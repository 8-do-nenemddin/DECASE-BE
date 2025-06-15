package com.skala.decase.domain.requirement.controller;

import com.skala.decase.domain.requirement.service.SrsProcessingService;
import com.skala.decase.domain.requirement.service.SrsUpdateService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Software Requirements Specification API", description = "요구사항 정의서 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/")
public class SrsProcessingController {

    private final SrsProcessingService srsProcessingService;
    private final SrsUpdateService srsUpdateService;

    /**
     * 요구사항 정의서 생성 업로드된 RFP DB에 저장 as-is, 요구사항 도출 fast api 각각 호출
     *
     * @param projectId 프로젝트 id
     * @param memberId  멤버 id
     * @param file      사용자 업로드 파일
     * @return
     */
    @Operation(summary = "요구사항 정의서 생성", description = "업로드된 RFP DB에 저장 후 as-is, 요구사항 도출 fast api를 병렬적으로 호출합니다.")
    @PostMapping(path = "/{projectId}/requirement-documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> processRfpFile(@PathVariable Long projectId,
                                                              @RequestParam("memberId") Long memberId,
                                                              @RequestPart("file") MultipartFile file) {
        // post: /api/v1/process-rfp-file에서 생성된 요구사항 정의서 리스트를 받아옴.
        String docId = srsProcessingService.createRequirementsSpecification(projectId, memberId, file);
        return ResponseEntity.ok().body(ApiResponse.success(docId + "의 요구사항 정의서 생성 요청"));
    }

    /**
     * 추가적인 문서를 받아서 요구사항 정의서 수정사항에 반영
     *
     * @param projectId 프로젝트 id
     * @param memberId  멤버 id
     * @param docId     문서 id
     * @param file      사용자 업로드 파일
     * @return
     */
    @Operation(summary = "추가적인 문서를 받아서 요구사항 정의서 수정사항에 반영", description = "추가적인 문서를 받아서 요구사항 정의서 수정사항에 반영합니다.")
    @PostMapping(path = "/{projectId}/requirement-documents/update",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> updateRfpFile(@PathVariable Long projectId,
                                                             @RequestParam("memberId") Long memberId,
                                                             @RequestParam("docId") String docId,
                                                             @RequestPart("file") MultipartFile file) {
        // post: /api/v1/process-rfp-file에서 생성된 요구사항 정의서 리스트를 받아옴.
        srsUpdateService.updateRFP(projectId, memberId, docId, file);
        return ResponseEntity.ok().body(ApiResponse.success("요구사항 정의서 수정 완료"));
    }

}
