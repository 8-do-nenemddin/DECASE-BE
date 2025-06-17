package com.skala.decase.domain.requirement.controller.dto.response;

import com.skala.decase.domain.requirement.domain.Reception;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatrixResponse {

    String reqIdCode;
    String level1;
    String level2;
    String level3;
    String name;
    String description;
    Reception reception;
    String tableId;
    String uiId;
    String programId;
    String batchId;
    String unitTestId;
    String integrationTest;
    String acceptanceTest;

    public MatrixResponse(String reqIdCode, String level1, String level2, String level3, String name, String description, Reception reception) {
        this.reqIdCode = reqIdCode;
        this.level1 = level1;
        this.level2 = level2;
        this.level3 = level3;
        this.name = name;
        this.description = description;
        this.reception = reception;
        this.tableId = "";
        this.uiId = "";
        this.programId = "";
        this.batchId = "";
        this.unitTestId = "";
        this.integrationTest = "";
        this.acceptanceTest = "";
    }
}
