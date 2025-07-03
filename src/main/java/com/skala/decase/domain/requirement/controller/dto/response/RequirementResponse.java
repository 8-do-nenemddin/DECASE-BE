package com.skala.decase.domain.requirement.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.skala.decase.domain.requirement.domain.Reception;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RequirementResponse {

    private long reqPk;
    private String reqIdCode;
    private int revisionCount;
    private String type;
    private String status;
    private String level1;
    private String level2;
    private String level3;
    private String priority;
    private String difficulty;
    private String name;
    private String description;
    private String revType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime modifiedDate;
    private List<String> modReason;
    private List<SourceResponse> sources;

    public RequirementResponse(long reqPk, String reqIdCode, int revisionCount,
                               String type, String status, String level1, String level2,
                               String level3, String priority, String difficulty, String name, String description, String revType,
                               LocalDateTime createdDate, LocalDateTime modifiedDate, List<String> modReason, List<SourceResponse> sources) {
        this.reqPk = reqPk;
        this.reqIdCode = reqIdCode;
        this.revisionCount = revisionCount;
        this.type = type;
        this.status = Reception.fromReception(status);
        this.level1 = level1;
        this.level2 = level2;
        this.level3 = level3;
        this.priority = priority;
        this.difficulty = difficulty;
        this.name = name;
        this.description = description;
        this.revType = revType;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.modReason = modReason;
        this.sources = sources;
    }
}
