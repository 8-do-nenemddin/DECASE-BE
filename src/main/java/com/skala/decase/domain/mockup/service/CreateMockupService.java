package com.skala.decase.domain.mockup.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.decase.domain.job.domain.JobName;
import com.skala.decase.domain.mockup.controller.dto.request.CreateMockUpRequest;
import com.skala.decase.domain.mockup.domain.Mockup;
import com.skala.decase.domain.mockup.exception.MockupException;
import com.skala.decase.domain.mockup.repository.MockupRepository;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.AIMailService;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.domain.requirement.service.RequirementService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
//@Transactional(readOnly = true)
public class CreateMockupService {

    private final WebClient webClient;
    // 로컬 파일 업로드 경로
    @Value("${file.upload.mockup-path}")
    private String BASE_UPLOAD_PATH;

    @Value("${mockup.callback-url}")
    private String callbackUrl;

    private final ProjectService projectService;
    private final RequirementService requirementService;
    private final MockupRepository mockupRepository;
    private final AIMailService aiMailService;

    /**
     * 목업 생성 - fast api 서버에서 생성한 html/css 파일들을 받아옵니다.
     */
    public void createMockUpAsync(Long projectId, Integer revisionCount) {
        Project project = projectService.findByProjectId(projectId);

        // 목업 생성시 사용할 요구사항 정의서 찾아오기
        List<CreateMockUpRequest> srsRequests = requirementService.getFunctionalRequirements(projectId,
                revisionCount);

        // 요구사항 리스트는 내부에서 생성하므로 null 또는 빈 리스트 전달 가능
        callFastApiMockupGenerationAsync(srsRequests, project.getName(), projectId, revisionCount);
    }

    /**
     * FastAPI 서버에 목업 생성 요청 (비동기, 콜백 URL 포함)
     */
    public void callFastApiMockupGenerationAsync(List<CreateMockUpRequest> srsRequests, String outputFolderName,
                                                 Long projectId, Integer revisionCount) {

        String mockupCallbackUrl = callbackUrl.replace("{projectId}", String.valueOf(projectId));

        log.info("FastAPI 서버에 목업 생성 비동기 요청 시작. 요구사항 수: {}", srsRequests.size());
        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("project_id", projectId);
        requestBody.put("revision_count", revisionCount);
        requestBody.put("callback_url", mockupCallbackUrl);
        requestBody.put("output_folder_name", outputFolderName);

        // CreateMockUpRequest 객체 리스트를 Map 리스트로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map> requirementsList = srsRequests.stream()
                .map(dto -> objectMapper.convertValue(dto, Map.class))
                .toList();

        requestBody.put("requirements", requirementsList);

        log.info("FastAPI 요청 바디: {}", requestBody);

        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/api/v1/mockup/generate-mockup")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        unused -> {
                        }, // onNext (성공 시)
                        error -> {
                            log.error("FastAPI 목업 생성 요청 중 에러 발생", error);
                        }
                );
    }

    /**
     * 콜백 수신 후 생성된 목업 저장
     */
    @Transactional
    public void saveMockUp(Long projectId, int revisionCount, MultipartFile mockupZip, String status) {
        Project project = projectService.findByProjectId(projectId);
        if (status.equals("FAILED")) {
            throw new MockupException("목업 생성 요청이 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Resource resource = new InputStreamResource(mockupZip);

        // ZIP 파일 압축 해제 및 저장
        extractAndSaveMockupFiles(resource, project, revisionCount);

        // 프로젝트에 참여하는 모든 멤버에게 메일 전송
        if (project.getMembersProjects() != null) {
            for (var memberProject : project.getMembersProjects()) {
                if (memberProject.getMember() != null) {
                    aiMailService.sendMail(JobName.MOCKUP, memberProject.getMember(), status, project, 1);
                }
            }
        }
    }

    /**
     * ZIP 파일 압축 해제 및 저장
     */
    @Transactional
    public void extractAndSaveMockupFiles(Resource zipResource, Project project, Integer revisionCount) {
        // 프로젝트별 디렉토리 생성
        Path projectDir = Paths.get(BASE_UPLOAD_PATH, "project_" + project.getProjectId(), "revision_" + revisionCount);
        Path mockupsDir = projectDir.resolve("mockups");
        Path screenSpecDir = projectDir.resolve("screen_spec");
        try {
            Files.createDirectories(mockupsDir);
            Files.createDirectories(screenSpecDir);
        } catch (Exception e) {
            throw new MockupException("프로젝트별 디렉토리 생성 실패", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // ZIP 파일 압축 해제
        try (ZipInputStream zipInputStream = new ZipInputStream(zipResource.getInputStream())) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    // 디렉토리 생성 (mockups 하위에 생성)
                    Path dirPath = mockupsDir.resolve(entry.getName());
                    Files.createDirectories(dirPath);
                    continue;
                }

                // 파일 저장 (mockups 하위에 저장)
                String filename = entry.getName();
                Path filePath = mockupsDir.resolve(filename);

                // 상위 디렉토리가 없으면 생성
                Files.createDirectories(filePath.getParent());

                // 파일 복사
                Files.copy(zipInputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

                // Mockup 엔티티 생성 및 저장
                Mockup mockup = Mockup.builder()
                        .name(filename)
                        .project(project)
                        .revisionCount(revisionCount)
                        .path(filePath.toString())
                        .build();

                mockupRepository.save(mockup);

                log.info("목업 파일 저장 완료: {}", filename);
            }
        } catch (Exception e) {
            throw new MockupException("목업 파일 저장 실패", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
