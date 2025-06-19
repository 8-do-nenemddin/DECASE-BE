package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.requirement.exception.RequirementException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class SrsProcessor {
    private final WebClient webClient;

    /**
     * 요구사항 정의서 생성 agent 호출 fast-api post "/api/v1/process-rfp-file"
     *
     * @param file
     * @return
     */
    @Async
    public CompletableFuture<Map> processRequirements(MultipartFile file, Long projectId, Long memberId,
                                                       String documentId) {
        try {
            log.info("요구사항 처리 시작 - 프로젝트: {}", projectId);

            //file, projectId, memberId, documentId 전달
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());
            builder.part("project_id", projectId.toString());  // Form 데이터로 추가
            builder.part("member_id", memberId.toString());    // Form 데이터로 추가
            builder.part("document_id", documentId);           // Form 데이터로 추가

            return webClient.post()
                    .uri("/ai/api/v1/requirements/srs-agent/start")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))  // 즉시 응답받으므로 30초로 단축
                    .doOnSuccess(response -> {
                        String status = (String) response.get("status");
                        String message = (String) response.get("message");
                        log.info("요구사항 분석 시작, 상태: {}, 메시지: {}", status, message);
                    })
                    .doOnError(error -> {
                        log.error("요구사항 처리 실패 - 프로젝트: {}, 에러: {}", projectId, error.getMessage());
                    })
                    .toFuture();

        } catch (Exception e) {
            log.error("요구사항 처리 실패 - 프로젝트: {}, 에러: {}", projectId, e.getMessage(), e);
            throw new RequirementException("요구사항 정의서 생성 요청 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * as-is 분석 에이전트 호출 fast-api post "/api/v1/asis"
     *
     * @param rfpFile RFP 파일
     * @return
     */
    @Async
    public CompletableFuture<Void> processASIS(Long projectId, Long memberId, String documentId,
                                               MultipartFile rfpFile) {
        try {
            log.info("ASIS 처리 시작 - 프로젝트: {}", projectId);

            //file, projectId, memberId, documentId 전달
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", rfpFile.getResource());
            builder.part("project_id", projectId.toString());  // Form 데이터로 추가
            builder.part("member_id", memberId.toString());    // Form 데이터로 추가
            builder.part("document_id", documentId);           // Form 데이터로 추가

            Map<String, Object> response = webClient.post()
                    .uri("/ai/api/v1/requirements/as-is/start")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))  // 즉시 응답받으므로 30초로 단축
                    .block();

            String status = (String) response.get("status");
            String message = (String) response.get("message");

            log.info("현황 시스템 분석 시작, 상태: {}, 메시지: {}", status, message);
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            throw new RequirementException("요구사항 정의서 생성 요청 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
