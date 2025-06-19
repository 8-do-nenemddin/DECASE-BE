package com.skala.decase.domain.requirement.controller.dto.request;

import lombok.Getter;

@Getter
public class DeleteRequestDto {
	private String reason;
	private Long memberId;
}
