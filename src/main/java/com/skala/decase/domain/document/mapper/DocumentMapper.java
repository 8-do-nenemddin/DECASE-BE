package com.skala.decase.domain.document.mapper;

import com.skala.decase.domain.document.controller.dto.DocumentDetailResponse;
import com.skala.decase.domain.document.controller.dto.DocumentResponse;
import com.skala.decase.domain.document.domain.Document;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DocumentMapper {

    public DocumentDetailResponse toDetailResponse(Document document) {
        int lastUnderscore = document.getName().lastIndexOf("_");
        String docName = document.getName().substring(0, lastUnderscore);

        return DocumentDetailResponse.builder()
                .docId(document.getDocId())
                .name(docName)
                .docDescription(document.getDocDescription() == null ? "" : document.getDocDescription())
                .createdDate(document.getCreatedDate())
                .createdBy(document.getCreatedBy().getName())
                .build();
    }

    public DocumentResponse toResponse(Document document) {
        int lastUnderscore = document.getName().lastIndexOf("_");
        String docName = document.getName().substring(0, lastUnderscore);

        return new DocumentResponse(
                document.getDocId(),
                docName,
                document.getDocDescription() != null ? document.getDocDescription() : ""
        );
    }
}