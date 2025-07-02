package com.skala.decase.domain.requirement.controller.dto.response;

public record SourceResponse(
        Long sourceId,
		String docId,
        String docName,  // 문서 이름으로 변경
        int pageNum,
        String relSentence
) {
}