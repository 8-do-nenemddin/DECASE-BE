package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.document.exception.DocumentException;
import com.skala.decase.domain.document.service.DocumentService;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.service.MemberService;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.domain.requirement.exception.RequirementException;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SrsProcessingService {

    private final SrsProcessor srsProcessor;

    private final ProjectService projectService;
    private final MemberService memberService;
    private final DocumentService documentService;
    private final EntityManager entityManager;

    /**
     * 요구사항 정의서 최초 생성
     *
     * @param projectId
     * @param memberId
     * @param file      RFP 문서
     */
    @Transactional
    public String createRequirementsSpecification(Long projectId, Long memberId, MultipartFile file) {
        Project project = projectService.findByProjectId(projectId);
        Member member = memberService.findByMemberId(memberId);
        Document document = documentService.uploadRFP(project, member, file);  //RFP 파일 db에 저장
        entityManager.flush();

        processInParallel(file, projectId, memberId, document.getDocId());
        return document.getDocId();
    }

    /**
     * 병렬처리
     * 요구사항 정의서 생성, as-is 분석 에이전트 호출
     */
    public void processInParallel(MultipartFile file, Long projectId, Long memberId, String rfpDocId) {
        log.info("병렬 처리 시작 - 프로젝트: {}", projectId);

        CompletableFuture<Void> asisFuture = srsProcessor.processASIS(projectId, memberId, rfpDocId, file);
        CompletableFuture<Map> requirementsFuture = srsProcessor.processRequirements(file, projectId, memberId,
                rfpDocId);

        try {
            CompletableFuture.allOf(requirementsFuture, asisFuture).get();
        } catch (Exception e) {
            log.error("병렬 처리 실패 - 프로젝트: {}", projectId, e);
            throw new RequirementException("요구사항 정의서 생성 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 즉시 반환 (블로킹하지 않음)
        log.info("병렬 처리 요청 완료 - 프로젝트: {} (백그라운드에서 계속 진행)", projectId);
    }
}
