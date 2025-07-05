package com.skala.decase.domain.mockup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.decase.domain.mockup.domain.Mockup;
import com.skala.decase.domain.mockup.domain.dto.MockupExistDto;
import com.skala.decase.domain.mockup.exception.MockupException;
import com.skala.decase.domain.mockup.repository.MockupRepository;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.repository.ProjectRepository;
import com.skala.decase.domain.mockup.domain.dto.MockupUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class MockupService {

    private static final Logger log = LoggerFactory.getLogger(MockupService.class);

    // 로컬 파일 업로드 경로
    private static final String TEST_BASE_UPLOAD_PATH = "DECASE/mockups";

    private final MockupRepository mockupRepository;
    private final ProjectRepository projectRepository;

    @Value("${file.upload.mockup-path}")
    private String MOCKUP_UPLOAD_PATH;

    // 프로젝트 ID 기준 모든 목업 리비전별로 그룹화된 정보 반환
    public Map<Integer, Map<String, List<String>>> getMockupsGroupedByRevision(Long projectId) {
        List<Mockup> mockups = mockupRepository.findAllByProject_ProjectId(projectId);
        if (mockups.isEmpty()) {
            throw new MockupException("해당 프로젝트에 대한 목업이 없습니다.", HttpStatus.NOT_FOUND);
        }

        Map<Integer, Map<String, List<String>>> revisionMap = new HashMap<>();
        for (Mockup mockup : mockups) {
            Map<String, List<String>> revisionData = revisionMap
                    .computeIfAbsent(mockup.getRevisionCount(), k -> new HashMap<>());

            if (mockup.getName().endsWith("_spec.html")) { // _spec.html로 끝나는 파일(화면 정의서)은 spec 리스트에
                revisionData.computeIfAbsent("spec", k -> new ArrayList<>()).add(mockup.getName());
            } else if (mockup.getName().endsWith(".html")) { //.html로 끝나는 파일(목업)은 mock 리스트에 추가
                revisionData.computeIfAbsent("mock", k -> new ArrayList<>()).add(mockup.getName());
            }
            //.png 파일은 사이드바에 보여주지 않음
        }

        // 각 리비전에 대해 spec과 mock 리스트가 없으면 빈 리스트로 초기화
        for (Map<String, List<String>> revisionData : revisionMap.values()) {
            revisionData.putIfAbsent("spec", new ArrayList<>());
            revisionData.putIfAbsent("mock", new ArrayList<>());
        }

        return revisionMap;
    }

    // 단일 목업 코드 반환 (HTML 등)
    public ResponseEntity<Map<String, Object>> getMockupCode(Long projectId, Integer revisionCount, String fileName) {
        Optional<Mockup> mockupOpt = mockupRepository.findByProject_ProjectIdAndRevisionCountAndName(projectId,
                revisionCount, fileName);

        if (mockupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Mockup mockup = mockupOpt.get();
        try {
            // HTML 파일 읽기
            String code = Files.readString(Path.of(mockup.getPath()));

            // 같은 projectId + revisionCount + .json 확장자를 가진 mockup 중 하나 찾기
            Optional<Mockup> jsonMetaOpt = mockupRepository.findAllByProject_ProjectIdAndRevisionCount(projectId,
                            revisionCount)
                    .stream()
                    .filter(m -> m.getName().toLowerCase().endsWith(".json"))
                    .findFirst();

            List<Map<String, Object>> sourceRequirements = new ArrayList<>();

            if (jsonMetaOpt.isPresent()) {
                Path jsonPath = Path.of(jsonMetaOpt.get().getPath());
                String jsonContent = Files.readString(jsonPath);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(jsonContent);
                JsonNode pages = root.path("page_mapping");

                for (JsonNode page : pages) {
                    if (fileName.equals(page.path("generated_file").asText())) {
                        JsonNode sources = page.path("source_requirements");
                        if (sources.isArray()) {
                            for (JsonNode src : sources) {
                                Map<String, Object> entry = new HashMap<>();
                                entry.put("id", src.path("id").asText());
                                entry.put("description", src.path("description").asText());
                                sourceRequirements.add(entry);
                            }
                        }
                        break;
                    }
                }
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("html", code);
            responseBody.put("sourceRequirements", sourceRequirements);

            // html 파일과 같은 경로, 같은 이름의 .png 파일이 있으면 base64로 인코딩하여 image 필드로 추가
            String pngFilePath = mockup.getPath().replaceAll("\\_spec.html?$", ".png");
            Path pngPath = Path.of(pngFilePath);
            if (Files.exists(pngPath)) {
                try {
                    byte[] imageBytes = Files.readAllBytes(pngPath);
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    responseBody.put("imageName", pngPath.getFileName().toString());
                    responseBody.put("image", base64Image);
                } catch (IOException e) {
                    // 이미지 읽기 실패 시 무시 (image 필드 추가하지 않음)
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 목업 불러오기
    private List<Resource> getMockupsByRevision(Long projectId, Integer revisionCount) {
        List<Mockup> mockups = mockupRepository.findAllByProject_ProjectIdAndRevisionCount(projectId, revisionCount);
        if (mockups.isEmpty()) {
            throw new MockupException("해당 조건에 맞는 목업이 없습니다.", HttpStatus.NOT_FOUND);
        }

        List<Resource> resources = new ArrayList<>();
        for (Mockup mockup : mockups) {
            Path filePath = Paths.get(mockup.getPath());
            if (!Files.exists(filePath)) {
                continue;
            }

            try {
                resources.add(new UrlResource(filePath.toUri()));
            } catch (MalformedURLException e) {
                throw new MockupException("파일 경로 오류: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return resources;
    }

    // 수정된 목업 코드 저장
    public void saveMockupCode(Long projectId, Integer revisionCount, String fileName, String newCode) {
        Mockup mockup = mockupRepository.findByProject_ProjectIdAndRevisionCountAndName(projectId, revisionCount,
                        fileName)
                .orElseThrow(() -> new MockupException("파일을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        try {
            Files.writeString(Path.of(mockup.getPath()), newCode, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MockupException("파일 저장 중 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 목업 다운로드
    public ResponseEntity<Resource> downloadMockups(Long projectId, Integer revisionCount) {
        List<Resource> mockupResources = getMockupsByRevision(projectId, revisionCount);
        if (mockupResources.isEmpty()) {
            throw new MockupException("압축할 목업 파일이 없습니다.", HttpStatus.NOT_FOUND);
        }

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(byteStream)) {

            for (Resource resource : mockupResources) {
                String fileName = resource.getFilename();
                String zipEntryPath;

                // _spec.html로 끝나는 파일은 screen_spec 폴더에, 나머지는 mockup 폴더에 넣기
                if (fileName != null && fileName.endsWith("_spec.html")) {
                    zipEntryPath = "screen_spec/" + fileName;
                } else {
                    zipEntryPath = "mockup/" + fileName;
                }

                zipOut.putNextEntry(new ZipEntry(zipEntryPath));
                try (InputStream inputStream = resource.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) >= 0) {
                        zipOut.write(buffer, 0, length);
                    }
                    zipOut.closeEntry();
                }
            }

            // 1. 기존 mockupResources 압축 코드 위에 추가
            Path pngDirPath = Paths.get(MOCKUP_UPLOAD_PATH, "project_" + projectId, "revision_" + revisionCount,
                    "screen_spec");
            if (Files.exists(pngDirPath) && Files.isDirectory(pngDirPath)) {
                try {
                    // 디렉터리 내 모든 파일 로그 출력
                    log.info("[PNG 디렉터리] {} 내 파일 목록:", pngDirPath);
                    Files.list(pngDirPath).forEach(path -> log.info("  - {}", path.getFileName()));

                    // PNG 파일만 필터링
                    Files.list(pngDirPath)
                            .filter(path -> path.toString().toLowerCase().endsWith(".png"))
                            .forEach(pngPath -> {
                                log.info("[PNG 추가 시도] 파일: {}", pngPath.getFileName());
                                try (InputStream inputStream = Files.newInputStream(pngPath)) {
                                    String fileName = pngPath.getFileName().toString();
                                    String zipEntryPath = "screen_spec/" + fileName;
                                    zipOut.putNextEntry(new ZipEntry(zipEntryPath));
                                    byte[] buffer = new byte[1024];
                                    int length;
                                    while ((length = inputStream.read(buffer)) >= 0) {
                                        zipOut.write(buffer, 0, length);
                                    }
                                    zipOut.closeEntry();
                                    log.info("[PNG 추가 성공] {} -> {}", fileName, zipEntryPath);
                                } catch (IOException e) {
                                    log.error("[PNG 추가 실패] {} - {}", pngPath, e.getMessage(), e);
                                }
                            });
                } catch (IOException e) {
                    log.error("PNG 디렉터리 접근 실패: {} - {}", pngDirPath, e.getMessage(), e);
                }
            }

            zipOut.finish();
            byte[] zipBytes = byteStream.toByteArray();
            ByteArrayResource resource = new ByteArrayResource(zipBytes);
            return ResponseEntity.ok()
                    .contentLength(zipBytes.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mockups.zip\"")
                    .body(resource);

        } catch (IOException e) {
            throw new MockupException("ZIP 파일 생성 중 오류 발생: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 테스트용 - 목업 파일 업로드 메서드
    public List<MockupUploadResponse> uploadMockups(Long projectId, Integer revisionCount, List<MultipartFile> files)
            throws java.io.IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new MockupException("프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        List<MockupUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String filename = file.getOriginalFilename();
            java.nio.file.Path savePath = java.nio.file.Paths.get(TEST_BASE_UPLOAD_PATH, filename);
            java.nio.file.Files.copy(file.getInputStream(), savePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            Mockup mockup = Mockup
                    .builder()
                    .name(filename)
                    .project(project)
                    .revisionCount(revisionCount)
                    .path(savePath.toString())
                    .build();
            mockupRepository.save(mockup);

            responses.add(new MockupUploadResponse(file.getOriginalFilename(), savePath.toString()));
        }

        return responses;
    }

    // 해당 리비전에 목업 유무 반환
    public MockupExistDto mockupExists(Long projectId, Integer revisionCount) {
        boolean mockupExist = mockupRepository.existsByProject_ProjectIdAndRevisionCount(projectId, revisionCount);
        return (new MockupExistDto(mockupExist));
    }
}
