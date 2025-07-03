package com.skala.decase.domain.mockup.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.decase.domain.mockup.controller.dto.request.SpecGenerationRequest;
import com.skala.decase.domain.mockup.domain.Mockup;
import com.skala.decase.domain.mockup.exception.MockupException;
import com.skala.decase.domain.mockup.repository.MockupRepository;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.ProjectService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
//@Transactional(readOnly = true)
public class CreateScreenSpecService {

    private final WebClient webClient;
    private final ProjectService projectService;
    private final MockupRepository mockupRepository;

    @Value("${screen-spec.callback-url}")
    private String callbackUrl;
    @Value("${file.upload.mockup-path}")
    private String BASE_MOCKUP_PATH;
    @Value("${file.upload.spec-save-path}")
    private String BASE_MOCKUP_SAVE_PATH;

    /**
     * FastAPI 서버에 화면 정의서 생성 요청 (비동기, 콜백 URL 포함)
     */
    public void callFastApiScreenSpecAsync(long projectId, int revisionCount) {
        log.info("FastAPI 서버에 화면 정의서 생성 비동기 요청 시작. projectId={}, revisionCount={}", projectId, revisionCount);

        String mockupDir = Paths.get(BASE_MOCKUP_PATH, "project_" + projectId, "revision_" + revisionCount, "mockups")
                .toString();
        String outputDir = Paths.get(BASE_MOCKUP_PATH, "project_" + projectId, "revision_" + revisionCount,
                "screen_spec").toString();

        // 콜백 URL에서 {projectId} 플레이스홀더 치환
        String screenSpecCallbackUrl = callbackUrl.replace("{projectId}", String.valueOf(projectId));

        SpecGenerationRequest specGenerationRequest = new SpecGenerationRequest(mockupDir, outputDir);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> requestBody = objectMapper.convertValue(specGenerationRequest, Map.class);

        log.info("FastAPI 요청 정보: mockupDir={}, outputDir={}, callbackUrl={}", mockupDir, outputDir, screenSpecCallbackUrl);
        log.info("FastAPI 요청 바디: {}", requestBody);

        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/api/v1/mockup/specs/generate")
                        .queryParam("project_id", projectId)
                        .queryParam("revision_count", revisionCount)
                        .queryParam("callback_url", screenSpecCallbackUrl)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> {
                            log.info("FastAPI 화면정의서 생성 요청 성공. 응답 상태: {}", response.getStatusCode());
                        },
                        error -> {
                            log.error("FastAPI 화면정의서 생성 요청 중 에러 발생: {}", error.getMessage(), error);
                            // 에러가 발생해도 콜백 처리 자체는 실패하지 않도록 함
                        }
                );
    }

    /**
     * 화면 정의서 저장
     */
    @Transactional
    public void saveScreenSpec(Long projectId, Integer revisionCount, String status) {
        Project project = projectService.findByProjectId(projectId);

        Path projectDir = Paths.get(BASE_MOCKUP_SAVE_PATH, "project_" + project.getProjectId(),
                "revision_" + revisionCount , "screen_spec");

        if (!Files.exists(projectDir) || !Files.isDirectory(projectDir)) {
            throw new MockupException("화면 정의서 디렉토리가 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }

        try (Stream<Path> paths = Files.list(projectDir)) {
            paths.filter(Files::isRegularFile).forEach(filePath -> {
                String filename = filePath.getFileName().toString();

                Mockup mockup = Mockup.builder()
                        .name(filename)
                        .project(project)
                        .revisionCount(revisionCount)
                        .path(filePath.toString())
                        .build();

                mockupRepository.save(mockup);
            });
        } catch (IOException e) {
            throw new MockupException("화면 정의서 파일을 읽는 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
