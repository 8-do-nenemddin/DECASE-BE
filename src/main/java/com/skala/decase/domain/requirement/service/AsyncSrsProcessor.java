package com.skala.decase.domain.requirement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.document.service.DocumentService;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.service.MemberService;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.mapper.RequirementServiceMapper;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import com.skala.decase.domain.requirement.service.dto.response.CreateRfpResponse;
import com.skala.decase.domain.requirement.service.dto.response.SrsAgentResponse;
import com.skala.decase.domain.source.domain.Source;
import com.skala.decase.domain.source.service.SourceRepository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncRfpProcessor {
    private final WebClient webClient;
    private final RequirementRepository requirementRepository;
    private final SourceRepository sourceRepository;
    private final RequirementServiceMapper requirementServiceMapper;

    private final ProjectService projectService;
    private final MemberService memberService;
    private final DocumentService documentService;

    /**
     * 요구사항 정의서 생성 agent 호출 fast-api post "/api/v1/process-rfp-file" 로부터 요구사항 정의서 생성 목록 받아와서 db에 저장
     *
     * @param file
     * @return
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> processRequirements(MultipartFile file, Long projectId, Long memberId,
                                                       String documentId) {
        try {
            log.info("요구사항 처리 시작 - 프로젝트: {}", projectId);

            // 새 트랜잭션에서 엔티티 재조회
            Project project = projectService.findByProjectId(projectId);
            Member member = memberService.findByMemberId(memberId);
            Document document = documentService.findByDocId(documentId);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());

            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/requirements/srs-agent/start")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofHours(1))
                    .block();

            String jobId = (String) response.get("job_id");
            log.info("요구사항 분석 Job ID 받음: {}", jobId);

            // 2단계: 상태만 주기적으로 확인 (진짜 폴링)
            SrsAgentResponse result = pollJobStatus(jobId);
            // 3단계: 결과 저장
            if (result != null && "COMPLETED".equals(result.getState()) &&
                    result.getRequirements() != null && !result.getRequirements().isEmpty()) {

                saveRequirements(result.getRequirements(), member, project, document);
                log.info("요구사항 {} 개 저장 완료", result.getRequirements().size());
            }

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("요구사항 처리 실패 - 프로젝트: {}, 에러: {}", projectId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 2단계: Job 상태만 주기적으로 확인 (진짜 폴링)
     */
    private SrsAgentResponse pollJobStatus(String jobId) {
        try {
            int maxAttempts = 120; // 최대 1시간 (30초 * 120)
            int attempts = 0;

            while (attempts < maxAttempts) {
                attempts++;

                try {
                    log.info("요구사항 상태 확인 중... jobId={}, attempt={}/{}", jobId, attempts, maxAttempts);

                    // 상태만 조회 (파일 업로드 없음)
                    SrsAgentResponse response = webClient.get()
                            .uri("/api/v1/requirements/srs-agent/{jobId}/status", jobId)
                            .retrieve()
                            .bodyToMono(SrsAgentResponse.class)
                            .timeout(Duration.ofSeconds(30))  // 30초 타임아웃
                            .block();

                    if (response != null) {
                        String state = response.getState();
                        log.info("요구사항 처리 상태: jobId={}, state={}, message={}",
                                jobId, state, response.getMessage());

                        if ("COMPLETED".equals(state)) {
                            log.info("요구사항 처리 완료! jobId={}", jobId);
                            return response;
                        } else if ("FAILED".equals(state)) {
                            log.error("요구사항 처리 실패: jobId={}, message={}", jobId, response.getMessage());
                            throw new RuntimeException("요구사항 처리 실패: " + response.getMessage());
                        }
                        // PROCESSING인 경우 계속 폴링
                    }

                } catch (Exception e) {
                    log.warn("요구사항 상태 확인 중 일시적 오류 (attempt={}): {}", attempts, e.getMessage());
                    // 일시적 오류는 무시하고 계속 시도
                }

                // 30초 대기 후 다시 상태 확인
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("요구사항 처리 중단됨", e);
                }
            }

            throw new RuntimeException("요구사항 처리 타임아웃: jobId=" + jobId);

        } catch (Exception e) {
            log.error("요구사항 상태 폴링 실패: jobId={}", jobId, e);
            throw new RuntimeException("요구사항 처리 실패", e);
        }
    }

    /**
     * as-is 분석 에이전트 호출 fast-api post "/api/v1/asis" 로부터 asis 보고서 파일 받아와서 DB에 저장
     *
     * @param rfpFile RFP 파일
     * @return
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> processASIS(Long projectId, Long memberId, MultipartFile rfpFile) {
        try {
            // 새 트랜잭션에서 엔티티 재조회
            Project project = projectService.findByProjectId(projectId);
            Member member = memberService.findByMemberId(memberId);

            log.info("ASIS 처리 시작 - 프로젝트: {}", project.getProjectId());

            // 1. FastAPI에 작업 요청 후 Job ID 받기
            return requestAsisProcessing(rfpFile)
                    .flatMap(jobId -> {
                        log.info("ASIS 처리 Job 시작: jobId={}", jobId);
                        return pollForAsisResult(jobId);
                    })
                    .flatMap(result -> {
                        if (result != null) {
                            String fileName = "asis_report_" + System.currentTimeMillis() + ".pdf";
                            documentService.uploadDocumentFromBytes(result, fileName, 8, project, member);
                            log.info("ASIS 문서 저장 완료");
                        }
                        return Mono.empty();
                    })
                    .then()
                    .toFuture();

        } catch (Exception e) {
            log.error("ASIS 처리 실패", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private Mono<String> requestAsisProcessing(MultipartFile rfpFile) {
        int maxRetries = 3;
        Duration timeout = Duration.ofHours(1);

        return Mono.defer(() -> {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", rfpFile.getResource());

            return webClient.post()
                    .uri("/api/v1/requirements/as-is/start")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .map(response -> {
                        if (response != null && response.containsKey("job_id")) {
                            return (String) response.get("job_id");
                        }
                        throw new RuntimeException("Invalid response format: job_id not found");
                    });
        })
        .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                .doBeforeRetry(signal -> 
                    log.warn("ASIS 처리 요청 재시도 (시도 {}/{}): {}", 
                            signal.totalRetries() + 1, maxRetries, signal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("ASIS 처리 요청 최종 실패: {}", retrySignal.failure().getMessage());
                    return new RuntimeException("ASIS 처리 요청 실패: " + retrySignal.failure().getMessage());
                }));
    }

    private Mono<byte[]> pollForAsisResult(String jobId) {
        int maxAttempts = 120; // 최대 1시간 (30초 * 120)
        Duration pollInterval = Duration.ofSeconds(30);
        Duration statusTimeout = Duration.ofSeconds(30);

        AtomicInteger attempts = new AtomicInteger(0);
        AtomicInteger consecutiveErrors = new AtomicInteger(0);
        int maxConsecutiveErrors = 3;

        return Mono.defer(() ->
                Mono.just(jobId)
                        .flatMap(id -> {
                            if (attempts.incrementAndGet() > maxAttempts) {
                                return Mono.error(new RuntimeException("ASIS 처리 타임아웃: jobId=" + jobId));
                            }
                            // 상태 조회 시 응답 타입을 확인하여 처리
                            return webClient.get()
                                    .uri("/api/v1/requirements/as-is/{jobId}/status", id)
                                    .retrieve()
                                    .toEntity(byte[].class)  // byte[]로 받아서 Content-Type 확인
                                    .timeout(statusTimeout)
                                    .flatMap(responseEntity -> {
                                        String contentType = responseEntity.getHeaders().getContentType() != null ?
                                                responseEntity.getHeaders().getContentType().toString() : "";
                                        byte[] responseBody = responseEntity.getBody();
                                        if (responseBody == null) {
                                            return Mono.error(new RuntimeException("Empty response body"));
                                        }
                                        // PDF 응답인 경우 - 작업 완료
                                        if (contentType.contains("application/pdf")) {
                                            consecutiveErrors.set(0);
                                            log.info("ASIS 처리 완료: jobId={}, PDF 결과 수신 (크기: {} bytes)",
                                                    id, responseBody.length);
                                            return Mono.just(responseBody);
                                        }
                                        // JSON 응답인 경우 - 상태 확인
                                        else if (contentType.contains("application/json")) {
                                            try {
                                                String jsonString = new String(responseBody, StandardCharsets.UTF_8);
                                                ObjectMapper mapper = new ObjectMapper();
                                                Map<String, Object> statusResponse = mapper.readValue(jsonString, Map.class);
                                                String status = (String) statusResponse.get("status");
                                                log.info("ASIS 처리 상태: jobId={}, status={}, attempt={}/{}",
                                                        id, status, attempts.get(), maxAttempts);
                                                if ("COMPLETED".equals(status) || "completed".equals(status)) {
                                                    consecutiveErrors.set(0);
                                                    // 별도 결과 엔드포인트에서 PDF 다운로드
                                                    return webClient.get()
                                                            .uri("/api/v1/requirements/as-is/{jobId}/result", id)
                                                            .retrieve()
                                                            .bodyToMono(byte[].class)
                                                            .timeout(Duration.ofMinutes(5))  // 결과 다운로드 타임아웃
                                                            .doOnNext(pdfBytes ->
                                                                    log.info("ASIS PDF 결과 다운로드 완료: jobId={}, 크기={} bytes",
                                                                            id, pdfBytes.length));
                                                } else if ("FAILED".equals(status) || "failed".equals(status)) {
                                                    String message = (String) statusResponse.get("message");
                                                    return Mono.error(new RuntimeException("ASIS 처리 실패: " + message));
                                                } else {
                                                    // PENDING, PROCESSING 등 - 계속 폴링
                                                    consecutiveErrors.set(0);
                                                    return Mono.empty();
                                                }
                                            } catch (Exception parseException) {
                                                log.warn("JSON 파싱 실패: {}", parseException.getMessage());
                                                return Mono.error(new RuntimeException("응답 파싱 실패: " + parseException.getMessage()));
                                            }
                                        }
                                        // 기타 응답 타입
                                        else {
                                            log.warn("예상하지 못한 응답 타입: {}", contentType);
                                            return Mono.error(new RuntimeException("지원하지 않는 응답 타입: " + contentType));
                                        }
                                    })
                                    .onErrorResume(e -> {
                                        int errors = consecutiveErrors.incrementAndGet();
                                        log.warn("ASIS 상태 확인 중 일시적 오류 (attempt={}, consecutive={}): {}",
                                                attempts.get(), errors, e.getMessage());
                                        if (errors >= maxConsecutiveErrors) {
                                            return Mono.error(new RuntimeException("연속적인 상태 확인 실패: " + e.getMessage()));
                                        }
                                        return Mono.empty();
                                    });
                        })
                        .repeatWhenEmpty(repeat -> repeat.delayElements(pollInterval))
                        .timeout(Duration.ofHours(1))  // 전체 폴링 시간을 1시간으로 제한
        );
    }

    private void saveRequirements(List<CreateRfpResponse> responses, Member member, Project project,
                                  Document document) {
        // 엔티티들을 현재 트랜잭션에 다시 attach
        member = memberService.findByMemberId(member.getMemberId());
        project = projectService.findByProjectId(project.getProjectId());
        document = documentService.findByDocId(document.getDocId());

        LocalDateTime now = LocalDateTime.now();
        for (CreateRfpResponse requirement : responses) {
            Requirement newReq = requirementServiceMapper.toREQEntity(requirement, member, project, now);
            requirementRepository.save(newReq);

            Source source = requirementServiceMapper.toSrcEntity(requirement, newReq, document);
            sourceRepository.save(source);
        }
    }

    private String extractFileName(HttpHeaders headers) {
        String contentDisposition = headers.getFirst("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            return contentDisposition.split("filename=")[1].replaceAll("\"", "");
        }
        return "asis_report_" + System.currentTimeMillis() + ".pdf";
    }

}
