package com.skala.decase.domain.requirement.service;

import static com.skala.decase.domain.document.service.DocumentService.TYPE_PREFIX_MAP;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.document.exception.DocumentException;
import com.skala.decase.domain.document.repository.DocumentRepository;
import com.skala.decase.domain.document.service.DocumentService;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.service.MemberService;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.domain.requirement.controller.dto.request.SrsDeleteRequestDetail;
import com.skala.decase.domain.requirement.controller.dto.request.SrsUpdateRequest;
import com.skala.decase.domain.requirement.controller.dto.request.SrsUpdateRequestDetail;
import com.skala.decase.domain.requirement.controller.dto.request.UpdateSrsAgentRequest;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.exception.RequirementException;
import com.skala.decase.domain.requirement.mapper.RequirementUpdateServiceMapper;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import com.skala.decase.domain.source.domain.Source;
import com.skala.decase.domain.source.service.SourceRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class SrsUpdateService {

    private final WebClient webClient;
    private final RequirementUpdateServiceMapper requirementUpdateServiceMapper;

    private final RequirementRepository requirementRepository;
    private final SourceRepository sourceRepository;
    private final DocumentRepository documentRepository;

    private final ProjectService projectService;
    private final MemberService memberService;
    private final DocumentService documentService;
    private final RequirementService requirementService;

    private final EntityManager entityManager;

    @Value("${file.upload.upload-path}")
    private String BASE_UPLOAD_PATH;

    @Value("${srs-update.callback-url}")
    private String updateCallbackUrl;


    /**
     * 요구사항 정의서 업데이트 에이전트 호출 fast-api post "/api/v1/srs-agent/update"
     *
     * @param file 회의록 파일
     * @return
     */
    public void callFastApiUpdateProcess(List<UpdateSrsAgentRequest> srsRequests, Long projectId, Long memberId,
                                         String documentId,
                                         MultipartFile file, String callbackUrl, String fileSubject) {
        log.info("요구사항 업데이트 시작 - 프로젝트: {}", projectId);

        log.info("FastAPI 서버에 목업 생성 비동기 요청 시작. 요구사항 수: {}", srsRequests.size());
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        try {
            // MultipartFile을 byte[]로 읽어서 ByteArrayResource로 감싸서 전달
            byte[] fileBytes = file.getBytes();
            builder.part("extra_file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
        } catch (IOException e) {
            throw new RequirementException("파일 읽기 실패", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // builder.part("meeting_file", file.getResource());
        builder.part("project_id", projectId);
        builder.part("member_id", memberId);
        builder.part("document_id", documentId);
        builder.part("callback_url", callbackUrl);
        builder.part("file_subject", fileSubject);

        // srsRequests를 JSON 문자열로 변환하여 multipart에 추가
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String requirementsJson = objectMapper.writeValueAsString(srsRequests);
            builder.part("requirements_str", requirementsJson)
                    .header("Content-Type", "application/json"); // 명시적으로 타입 지정
        } catch (Exception e) {
            log.error("요구사항 리스트 JSON 변환 실패", e);
            throw new RequirementException("요구사항 리스트 JSON 변환 실패", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("FastAPI multipart 요청 준비 완료");

        webClient.post()
                .uri("/ai/api/v1/requirements/meeting-analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    /**
     * 업로드된 회의록 저장
     */
    @Transactional
    public Document uploadMOMDocument(Project project, Member member, String newFileName, MultipartFile file) {
        //파일 확장자 확인
        String originalFileName = file.getOriginalFilename();
        int idx = 3; // 기본값: 문서 MOMD
        if (originalFileName != null && originalFileName.toLowerCase().endsWith(".wav")) {
            idx = 2; // 음성 파일 MOMV
        }

        return saveDocument(BASE_UPLOAD_PATH, file, newFileName, idx, project, member, true);
    }

    @Transactional
    public Document saveDocument(String uploadPath, MultipartFile file, String newFileName, int docTypeIdx,
                                 Project project,
                                 Member member, boolean isMemberUpload) {
        //파일 이름 지정
        String extension = ""; //확장자 추출
        if (newFileName != null) {
            extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        }
        String fileName = newFileName == null
                ? System.currentTimeMillis() + "_" + StringUtils.cleanPath(file.getOriginalFilename())
                : System.currentTimeMillis() + "_" + newFileName + extension;

        Path path = Paths.get(uploadPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new DocumentException("파일 uploadPath를 만들 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        Path filePath = path.resolve(fileName);
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DocumentException("파일을 저장할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Document 엔티티 생성 및 저장
        Document doc = new Document(
                documentService.generateDocId(TYPE_PREFIX_MAP.get(docTypeIdx)),
                fileName,
                filePath.toString(),
                isMemberUpload,
                project,
                member
        );

        Document savedDocument = documentRepository.save(doc);
        entityManager.flush();
        return savedDocument;

    }

    /**
     * 요구사항 정의서 수정 MOMD: 회의록 문서 MOMV: 회의록 음성
     */
    @Transactional
    public void updateRFP(Long projectId, Long memberId, String fileName, MultipartFile file) {
        Project project = projectService.findByProjectId(projectId);
        Member member = memberService.findByMemberId(memberId);

        Document savedDocument = uploadMOMDocument(project, member, fileName, file);

        String formattedCallbackUrl = updateCallbackUrl.replace("{projectId}", projectId.toString());

        //요구사항 정의서 최신 버전 찾기
        Integer maxRevision = requirementRepository.findMaxRevisionCountByProject(project)
                .orElseThrow(() -> new RequirementException("업데이트 할 요구사항 정의서가 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        // 요구사항 업데이트시 사용할 요구사항 정의서 찾아오기
        List<UpdateSrsAgentRequest> srsRequests = requirementService.getRequirementsForUpdate(projectId, maxRevision);

        // 파일 이름에서 언더스코어(_) 뒤의 실제 파일 이름만 추출
        String originalFileName = savedDocument.getName();
        String fileSubject = originalFileName;
        int underscoreIdx = originalFileName.indexOf("_");
        if (underscoreIdx != -1 && underscoreIdx + 1 < originalFileName.length()) {
            fileSubject = originalFileName.substring(underscoreIdx + 1);
        }

        callFastApiUpdateProcess(srsRequests, project.getProjectId(), member.getMemberId(), savedDocument.getDocId(),
                file, formattedCallbackUrl, fileSubject);
    }


    /**
     * 요구사항 정의서 업데이트 콜백 요청 처리
     *
     * @param projectId
     * @param memberId
     * @param docId
     */
    @Transactional
    public void saveSRSUpdateAnalysis(Long projectId, Long memberId, String docId,
                                      String status, SrsUpdateRequest requirements) {
        Project project = projectService.findByProjectId(projectId);
        Member member = memberService.findByMemberId(memberId);
        Document document = documentService.findByDocId(docId);

        if (!status.equals("COMPLETED")) {
            documentRepository.delete(document);
            throw new RequirementException("요구사항 정의서 업데이트 실패. 상태: " + status + " - 프로젝트 ID: " + projectId,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        //수정 이유 (mod reason), modified date 수정, revision 정보 업데이트
        LocalDateTime modDate = LocalDateTime.now();  //수정 시각
        int latestRevisionCount = requirementRepository.findMaxRevisionCountByProject(project)
                .orElseThrow(() -> new RequirementException("수정할 요구사항 정의서가 없습니다.", HttpStatus.NOT_FOUND));

        // 1. 추가
        for (SrsUpdateRequestDetail addDetail : requirements.to_add()) {
            Requirement newReq = requirementUpdateServiceMapper.toCreateREQEntity(addDetail, member, project,
                    modDate, latestRevisionCount + 1);
            requirementRepository.save(newReq);
            Source source = requirementUpdateServiceMapper.toSrcEntity(addDetail, newReq, document);
            sourceRepository.save(source);
        }
        // 2. 수정
        for (SrsUpdateRequestDetail updateDetail : requirements.to_update()) {
            Requirement oldReq = requirementRepository.findByProjectAndReqIdCodeAndIsDeletedFalse(
                            project, updateDetail.requirement_id())
                    .orElseThrow(() -> new RequirementException("업데이트할 요구사항이 없습니다.", HttpStatus.NOT_FOUND));
            oldReq.updateSRS(updateDetail, member);
            requirementRepository.save(oldReq);
            //출처 추가
            Source source = requirementUpdateServiceMapper.toSrcEntity(updateDetail, oldReq, document);
            sourceRepository.save(source);
        }
        // 3. 삭제
        for (SrsDeleteRequestDetail deleteDetail : requirements.to_delete()) {
            Requirement reqToDelete = requirementRepository.findByProjectAndReqIdCodeAndIsDeletedFalse(project,
                            deleteDetail.requirement_id())
                    .orElseThrow(() -> new RequirementException("삭제할 요구사항이 없습니다.", HttpStatus.NOT_FOUND));
            reqToDelete.deleteSRS(deleteDetail, member);
            requirementRepository.save(reqToDelete);
        }
        log.info("요구사항 정의서 업데이트 완료");

    }
}
