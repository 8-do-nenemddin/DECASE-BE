package com.skala.decase.domain.document.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.decase.domain.document.controller.dto.DocumentDetailResponse;
import com.skala.decase.domain.document.controller.dto.DocumentResponse;
import com.skala.decase.domain.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Docs API", description = "문서 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "문서 업로드", description = "사용자가 문서를 업로드합니다.")
    @PostMapping(value = "/projects/{projectId}/documents/uploads", consumes = "multipart/form-data")
    public ResponseEntity<List<DocumentResponse>> uploadDocuments(
            @PathVariable Long projectId,
            @RequestParam Long memberId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("types") String typesJson
    ) throws Exception {
        List<Integer> types = objectMapper.readValue(typesJson, new TypeReference<>() {
        });
        return ResponseEntity.ok(documentService.uploadDocuments(projectId, memberId, files, types));
    }

    // @DeleteMapping("/{docId}")
    // public ResponseEntity<Void> deleteDocument(@PathVariable Long docId) {
    //     documentService.deleteDocument(docId);
    //     return ResponseEntity.noContent().build();
    // }

    @Operation(summary = "문서 다운로드", description = "docId에 해당하는 문서를 다운로드합니다.")
    @GetMapping("/documents/{docId}/downloads")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String docId) throws Exception {
        return documentService.downloadDocument(docId);
    }

    @Operation(summary = "문서 정보 상세보기", description = "docId에 해당하는 문서의 이름, 설명, 생성 날짜, 생성자를 반환합니다.")
    @GetMapping("/documents/{docId}")
    public ResponseEntity<DocumentDetailResponse> getDocumentDetail(@PathVariable String docId) {
        return documentService.getDocumentDetails(docId);
    }

    @Operation(summary = "문서 미리보기", description = "프로젝트의 사용자 업로드 문서 목록을 보여줍니다. docId, 이름, 설명을 반환합니다.")
    @GetMapping("/projects/{projectId}/document/uploads")
    public ResponseEntity<List<DocumentResponse>> getDocumentUploads(@PathVariable Long projectId) {
        return documentService.getDocumentUploads(projectId);
    }
}