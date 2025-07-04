package com.skala.decase.domain.requirement.controller.dto.request;

import com.skala.decase.domain.requirement.domain.Difficulty;
import com.skala.decase.domain.requirement.domain.Priority;
import com.skala.decase.domain.requirement.domain.Reception;
import com.skala.decase.domain.requirement.domain.RequirementType;
import lombok.Data;

@Data
public class UpdateRequirementDto {
    private Long memberId; // createdBy  변경
    private Long reqPk; // 변경 X
    private RequirementType type; // 변경
    private String level1; // 변경
    private String level2; // 변경
    private String level3; // 변경
    private Priority priority; // 변경
    private Difficulty difficulty; // 변경
    private Reception reception;
    private String status;
    private String name; // 변경
    private String description; // 변경
    private boolean isDeleted;
    private String modReason; // 변경
}