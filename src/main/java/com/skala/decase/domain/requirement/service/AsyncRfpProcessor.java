package com.skala.decase.domain.requirement.service;

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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

            SrsAgentResponse response = webClient.post()
                    .uri("/api/v1/requirements/srs-agent")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(SrsAgentResponse.class)
                    .timeout(Duration.ofMinutes(3))
                    .block();

            if (response != null && response.getRequirements() != null && !response.getRequirements().isEmpty()) {
                saveRequirements(response.getRequirements(), member, project, document);
                log.info("요구사항 {} 개 저장 완료", response.getRequirements().size());
            }

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("요구사항 처리 실패", e);
            return CompletableFuture.failedFuture(e);
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

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", rfpFile.getResource());

            ResponseEntity<byte[]> response = webClient.post()
                    .uri("/api/v1/requirements/as-is")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .toEntity(byte[].class)
                    .timeout(Duration.ofMinutes(3))
                    .block();

            if (response.getBody() != null) {
                String fileName = extractFileName(response.getHeaders());
                documentService.uploadDocumentFromBytes(response.getBody(), fileName, 8, project, member);
                log.info("ASIS 문서 저장 완료");
            }

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("ASIS 처리 실패", e);
            return CompletableFuture.failedFuture(e);
        }
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

    private String extractFileName(HttpHeaders headers)
    {
        String contentDisposition = headers.getFirst("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            return contentDisposition.split("filename=")[1].replaceAll("\"", "");
        }
        return "asis_report_" + System.currentTimeMillis() + ".pdf";
    }

}
