package com.skala.decase.domain.document.controller.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentDetailResponse {
	String docId;
	String name;
	LocalDateTime createdDate;
	String createdBy;
}
