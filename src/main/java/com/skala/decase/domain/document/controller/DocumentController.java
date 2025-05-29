package com.skala.decase.domain.document.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.decase.domain.document.controller.dto.DocumentDetailResponse;
import com.skala.decase.domain.document.controller.dto.DocumentResponse;
import com.skala.decase.domain.document.service.DocumentService;

import com.skala.decase.domain.member.domain.Member;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Docs API", description = "문서 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/projects/{projectId}/documents/uploads", consumes = "multipart/form-data")
    public ResponseEntity<List<DocumentResponse>> uploadDocuments(
            @PathVariable Long projectId,
            @RequestParam Long memberId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("types") String typesJson
    ) throws Exception {
        List<Integer> types = objectMapper.readValue(typesJson, new TypeReference<>() {});
        return ResponseEntity.ok(documentService.uploadDocuments(projectId, memberId, files, types));
    }

    // @DeleteMapping("/{docId}")
    // public ResponseEntity<Void> deleteDocument(@PathVariable Long docId) {
    //     documentService.deleteDocument(docId);
    //     return ResponseEntity.noContent().build();
    // }

    @PostMapping("/documents/{docId}/downloads")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String docId) throws Exception {
        return documentService.downloadDocument(docId);
    }

    @GetMapping("/documents/{docId}")
    public ResponseEntity<DocumentDetailResponse> getDocument(@PathVariable String docId) throws Exception {
        return documentService.getDocument(docId);
    }
}