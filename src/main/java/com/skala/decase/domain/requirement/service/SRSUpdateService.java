package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.document.exception.DocumentException;
import com.skala.decase.domain.document.repository.DocumentRepository;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.service.MemberService;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.UpdateStatus;
import com.skala.decase.domain.requirement.exception.RequirementException;
import com.skala.decase.domain.requirement.mapper.RequirementUpdateServiceMapper;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import com.skala.decase.domain.requirement.service.dto.response.UpdateRfpResponse;
import com.skala.decase.domain.source.domain.Source;
import com.skala.decase.domain.source.service.SourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SRSUpdateService {

    private final WebClient webClient;
    private final RequirementUpdateServiceMapper requirementUpdateServiceMapper;

    private final RequirementRepository requirementRepository;
    private final DocumentRepository documentRepository;
    private final SourceRepository sourceRepository;

    private final ProjectService projectService;
    private final MemberService memberService;

    /**
     * fast-api post "/api/v1/process-rfp-file" 로부터 요구사항 정의서 수정 목록 받아옴
     *
     * @param file
     * @return
     */
    private List<UpdateRfpResponse> fetchUpdateRequirements(MultipartFile file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource());

        return webClient.post()
                .uri("/api/v1/process-rfp-file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToFlux(UpdateRfpResponse.class)
                .collectList()
                .block();

    }

    /**
     * RFP 수정 -> RFP가 아니라 SRS..
     *
     * @param projectId
     * @param memberId
     * @param docId
     * @param file
     */
//    @Transactional
//    public void updateRFP(Long projectId, Long memberId, String docId, MultipartFile file) {
//        Project project = projectService.findByProjectId(projectId);
//        Member member = memberService.findByMemberId(memberId);
//        Document document = documentRepository.findByDocId(docId)
//                .orElseThrow(() -> new DocumentException("문서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
//
//        //AI가 생성한 추가/업데이트/삭제 요구사항 받아옴
//        //TODO: ai쪽 완성되면 api url 수정 필요
//        List<UpdateRfpResponse> requirements = fetchUpdateRequirements(file);
//
//        if (requirements == null) {
//            // 여기서 왜 종료되었는지 로그 찍어야 함....
//            return;
//        }
//
//        //수정 이유 (mod reason), modified date 수정, revision 정보 업데이트
//        LocalDateTime modDate = LocalDateTime.now();  //수정 시각
//        int latestRevisionCount = requirementRepository.findMaxRevisionCountByProject(project)
//                .orElseThrow(() -> new RequirementException("수정할 요구사항 정의서가 없습니다.", HttpStatus.NOT_FOUND));
//
//        // fast api로부터 받아온 요구사항 정의서 목록들 반영
//        for (UpdateRfpResponse requirement : requirements) {
//            //추가: 요구사항 단순 추가
//            if (UpdateStatus.fromAI(requirement.status()).equals(UpdateStatus.CREATE)) {
//                Requirement updatedRequirement = requirementUpdateServiceMapper.toCreateREQEntity(requirement, member,
//                        project,
//                        modDate, latestRevisionCount + 1);
//                requirementRepository.save(updatedRequirement);
//                Source source = requirementUpdateServiceMapper.toSrcEntity(requirement, updatedRequirement, document);
//                sourceRepository.save(source);
//            }
//
//            //업데이트: 출처가 다른 경우 출처 추가 필요
//            if (UpdateStatus.fromAI(requirement.status()).equals(UpdateStatus.UPDATE)) {
//                // 기존 요구사항 isDeleted=true로 바꾸기
//                Requirement oldRequirement = requirementRepository.findByReqIdCodeAndIsDeletedFalse(requirement.id())
//                        .orElseThrow(() -> new RequirementException("업데이트할 요구사항을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
//                oldRequirement.softDelete(latestRevisionCount + 1);
//
//                // 새로운 업데이트된 요구사항 생성
//                Requirement updatedRequirement = requirementUpdateServiceMapper.toUpdateREQEntity(requirement, member,
//                        project,
//                        modDate, latestRevisionCount + 1);
//                requirementRepository.save(updatedRequirement);
//                // 새로운 출처 생성
//                Source source = requirementUpdateServiceMapper.toSrcEntity(requirement, updatedRequirement, document);
//                sourceRepository.save(source);
//            }
//
//            //삭제 : isdelete 필드 업데이트
//            if (UpdateStatus.fromAI(requirement.status()).equals(UpdateStatus.DELETE)) {
//                Requirement updatedRequirement = requirementRepository.findByReqIdCodeAndIsDeletedFalse(
//                                requirement.id())
//                        .orElseThrow(() -> new RequirementException("삭제할 요구사항을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
//                updatedRequirement.softDelete(latestRevisionCount + 1);
//            }
//
//        }
//    }

    @Transactional
    public void updateRFP(Long projectId, Long memberId, String docId, MultipartFile file) {
        Project project = projectService.findByProjectId(projectId);
        Member member = memberService.findByMemberId(memberId);
        Document document = documentRepository.findByDocId(docId)
                .orElseThrow(() -> new DocumentException("문서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        //AI가 생성한 추가/업데이트/삭제 요구사항 받아옴
        //TODO: ai쪽 완성되면 api url 수정 필요
        List<UpdateRfpResponse> requirements = fetchUpdateRequirements(file);
        int latestRevisionCount = requirementRepository.findMaxRevisionCountByProject(project)
                .orElseThrow(() -> new RequirementException("수정할 요구사항 정의서가 없습니다.", HttpStatus.NOT_FOUND));
        if (requirements == null) {
            // 여기서 왜 종료되었는지 로그 찍어야 함....
            return;
        }

        LocalDateTime modDate = LocalDateTime.now();
        for (UpdateRfpResponse response : requirements) {

            if (UpdateStatus.fromAI(response.status()).equals(UpdateStatus.CREATE)) {
                // 없으면 새로 생성해서 저장
                Requirement newRequirement = requirementUpdateServiceMapper.toCreateREQEntity(response, member, project, modDate, latestRevisionCount);
                requirementRepository.save(newRequirement);
            }

            if (UpdateStatus.fromAI(response.status()).equals(UpdateStatus.UPDATE)) {
                Requirement requirement = requirementRepository.findByReqIdCode(response.id())
                        .orElseThrow(() -> new RequirementException("요구사항이 존재하지 않습니다.", HttpStatus.BAD_REQUEST));
                if (requirement != null) {
                    // 기존 요구사항이 있으면 수정해서 저장
                    requirement.updateSRS(response, member);
                    Requirement updatedRequirement = requirementRepository.save(requirement);

                    Source source = requirementUpdateServiceMapper.toSrcEntity(response, updatedRequirement, document);
                    sourceRepository.save(source);
                }
            }

            if (UpdateStatus.fromAI(response.status()).equals(UpdateStatus.DELETE)) {
                Requirement requirement = requirementRepository.findByReqIdCode(response.id())
                        .orElseThrow(() -> new RequirementException("요구사항이 존재하지 않습니다.", HttpStatus.BAD_REQUEST));
                requirementRepository.delete(requirement);
            }
        }
    }
}
