package com.skala.decase.domain.document.service;

import com.skala.decase.domain.document.controller.dto.DocumentDetailResponse;
import com.skala.decase.domain.document.controller.dto.DocumentPreviewDto;
import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.document.exception.DocumentException;
import com.skala.decase.domain.document.mapper.DocumentMapper;
import com.skala.decase.domain.document.repository.DocumentRepository;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.ProjectService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsisService {

    private final ProjectService projectService;
    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final DocumentPreviewService documentPreviewService;

    /**
     * as-is 보고서 목록을 조회합니다.
     *
     * @param projectId
     * @return
     */
    public List<DocumentDetailResponse> getAsisDocumentList(Long projectId) {
        Project project = projectService.findByProjectId(projectId);

        // 해당 프로젝트의 모든 as-is 보고서 조회
        List<Document> documents = documentRepository.findByDocIdStartingWithASIS(project);

        return documents.stream()
                .map(documentMapper::toDetailResponse)
                .collect(Collectors.toList());

    }


    public DocumentPreviewDto getAsisHtmlPreview(String docId) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new DocumentException("문서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        String fileName = doc.getName();
        String fileExtension = documentPreviewService.getFileExtension(fileName).toLowerCase();

        Path filePath = Paths.get(doc.getPath());
        if (!Files.exists(filePath)) {
            throw new DocumentException("파일이 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }

        // HTML 파일만 허용
        if (!"html".equals(fileExtension)) {
            throw new DocumentException("HTML 파일만 미리보기가 지원됩니다.", HttpStatus.BAD_REQUEST);
        }

        // 파일명에서 확장자 제거
        String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
        System.out.println(fileNameWithoutExtension + ".pdf");
        // 프로젝트에서 해당 파일명으로 문서 찾기
        List<Document> documents = documentRepository.findByProjectAndName(doc.getProject(),
                fileNameWithoutExtension + ".pdf");

        if (documents.isEmpty()) {
            throw new DocumentException("해당 파일명의 문서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        try {
            String htmlContent = Files.readString(filePath, StandardCharsets.UTF_8);
            return DocumentPreviewDto.builder()
                    .fileType("html")
                    .fileName(fileName)
                    .previewUrl(documents.get(0).getDocId())
                    .htmlContent(htmlContent)
                    .build();
        } catch (IOException e) {
            throw new DocumentException("파일을 읽는 중 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}