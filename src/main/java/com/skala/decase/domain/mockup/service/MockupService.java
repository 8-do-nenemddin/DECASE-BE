package com.skala.decase.domain.mockup.service;

import com.skala.decase.domain.mockup.domain.Mockup;
import com.skala.decase.domain.mockup.domain.dto.MockupExistDto;
import com.skala.decase.domain.mockup.exception.MockupException;
import com.skala.decase.domain.mockup.repository.MockupRepository;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.repository.ProjectRepository;
import com.skala.decase.domain.mockup.domain.dto.MockupUploadResponse;
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

@Service
@RequiredArgsConstructor
public class MockupService {

    // 로컬 파일 업로드 경로
    private static final String BASE_UPLOAD_PATH = "DECASE/mockups";

    private final MockupRepository mockupRepository;
    private final ProjectRepository projectRepository;

	// 프로젝트 ID 기준 모든 목업 리비전별로 그룹화된 정보 반환
	public Map<Integer, List<String>> getMockupsGroupedByRevision(Long projectId) {
		List<Mockup> mockups = mockupRepository.findAllByProject_ProjectId(projectId);
		if (mockups.isEmpty()) {
			throw new MockupException("해당 프로젝트에 대한 목업이 없습니다.", HttpStatus.NOT_FOUND);
		}

		Map<Integer, List<String>> revisionMap = new HashMap<>();
		for (Mockup mockup : mockups) {
			revisionMap
					.computeIfAbsent(mockup.getRevisionCount(), k -> new ArrayList<>())
					.add(Paths.get(mockup.getPath()).getFileName().toString());
		}

		return revisionMap;
	}

	// 단일 목업 코드 반환 (HTML 등)
	public ResponseEntity<Resource> getMockupCode(Long projectId, Integer revisionCount, String fileName) {
		Optional<Mockup> mockupOpt = mockupRepository.findByProject_ProjectIdAndRevisionCountAndName(projectId, revisionCount, fileName);

		if (mockupOpt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		Mockup mockup = mockupOpt.get();
		try {
			String code = Files.readString(Path.of(mockup.getPath()));
			ByteArrayResource resource = new ByteArrayResource(code.getBytes(StandardCharsets.UTF_8));

			return ResponseEntity.ok()
					.contentType(MediaType.TEXT_HTML)
					.body(resource);
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
			if (!Files.exists(filePath)) continue;

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
		Mockup mockup = mockupRepository.findByProject_ProjectIdAndRevisionCountAndName(projectId, revisionCount, fileName)
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
				zipOut.putNextEntry(new ZipEntry(fileName));
				try (InputStream inputStream = resource.getInputStream()) {
					byte[] buffer = new byte[1024];
					int length;
					while ((length = inputStream.read(buffer)) >= 0) {
						zipOut.write(buffer, 0, length);
					}
					zipOut.closeEntry();
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
    public List<MockupUploadResponse> uploadMockups(Long projectId, Integer revisionCount, List<MultipartFile> files) throws java.io.IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new MockupException("프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        List<MockupUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String filename = file.getOriginalFilename();
            java.nio.file.Path savePath = java.nio.file.Paths.get(BASE_UPLOAD_PATH, filename);
            java.nio.file.Files.copy(file.getInputStream(), savePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

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
