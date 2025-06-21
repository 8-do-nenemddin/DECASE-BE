package com.skala.decase.domain.mockup.mapper;

import com.skala.decase.domain.mockup.controller.dto.request.CreateMockUpRequest;
import com.skala.decase.domain.mockup.controller.dto.request.CreateMockUpSourceRequest;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementWithSourceResponse;
import com.skala.decase.domain.requirement.controller.dto.response.SourceResponse;
import com.skala.decase.domain.requirement.domain.RequirementType;
import com.skala.decase.domain.requirement.service.dto.response.CreateRfpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MockupMapper {
    /**
     * RequirementWithSourceResponse를 CreateRfpResponse로 변환
     */
    public CreateMockUpRequest toCreateMockUpRequest(RequirementWithSourceResponse requirement) {
        if (requirement == null) {
            return null;
        }

        String typeKor = RequirementType.toKorean(requirement.type());

        // SourceResponse -> CreateMockUpSourceRequest 변환
        List<CreateMockUpSourceRequest> sources = null;
        if (requirement.sources() != null) {
            sources = requirement.sources().stream()
                    .map(src -> new CreateMockUpSourceRequest(src.pageNum(), src.relSentence()))
                    .collect(Collectors.toList());
        }

        return new CreateMockUpRequest(
                requirement.name(), // requirement_name
                typeKor, // type
                sources, // sources
                requirement.description(), // description
                requirement.level1(), // category_large
                requirement.level2(), // category_medium
                requirement.level3(), // category_small
                requirement.priority(), // importance
                requirement.difficulty(), // difficulty
                requirement.reqIdCode() // requirement_id
        );
    }

    /**
     * RequirementWithSourceResponse 리스트를 CreateRfpResponse 리스트로 변환
     */
    public List<CreateMockUpRequest> toCreateMockUpRequestList(List<RequirementWithSourceResponse> requirements) {
        if (requirements == null) {
            return new ArrayList<>();
        }

        return requirements.stream()
                .map(this::toCreateMockUpRequest)
                .collect(Collectors.toList());
    }


    /**
     * RFP 페이지 번호 추출 (첫 번째 소스의 페이지 번호 사용)
     */
    private String extractRfpPage(List<SourceResponse> sources) {
        if (sources != null && !sources.isEmpty()) {
            SourceResponse firstSource = sources.get(0);
            return String.valueOf(firstSource.pageNum());
        }
        return "1"; // 기본값
    }

    /**
     * raw_text 추출 (소스들의 관련 문장을 조합)
     */
    private String extractRawText(List<SourceResponse> sources) {
        if (sources == null || sources.isEmpty()) {
            return "";
        }

        return sources.stream()
                .map(SourceResponse::relSentence)
                .filter(sentence -> sentence != null && !sentence.trim().isEmpty())
                .collect(Collectors.joining("\n "));
    }

    /**
     * CreateRfpResponse를 RequirementWithSourceResponse로 역변환 (필요시)
     */
    public RequirementWithSourceResponse toRequirementWithSourceResponse(CreateRfpResponse rfpResponse, Long reqPk) {
        if (rfpResponse == null) {
            return null;
        }

        // 기본값들로 RequirementWithSourceResponse 생성
        return new RequirementWithSourceResponse(
                reqPk != null ? reqPk : 0L,
                generateReqIdCode(reqPk), // REQ-001 형태로 생성
                1, // 기본 revision count
                rfpResponse.type(),
                "ACTIVE", // 기본 상태
                rfpResponse.category_large(),
                rfpResponse.category_medium(),
                rfpResponse.category_small(),
                rfpResponse.importance(),
                rfpResponse.difficulty(),
                rfpResponse.description_name(),
                rfpResponse.description_content(),
                java.time.LocalDateTime.now().toString(),
                false,
                0,
                List.of(), // 빈 수정 이유 목록
                createSourceFromRfpResponse(rfpResponse) // RFP 응답에서 소스 생성
        );
    }

    /**
     * reqPk 기반으로 REQ-ID 코드 생성
     */
    private String generateReqIdCode(Long reqPk) {
        if (reqPk == null) {
            return "REQ-001";
        }
        return String.format("REQ-%03d", reqPk);
    }

    /**
     * CreateRfpResponse에서 SourceResponse 목록 생성
     */
    private List<SourceResponse> createSourceFromRfpResponse(CreateRfpResponse rfpResponse) {
        if (rfpResponse.raw_text() == null || rfpResponse.raw_text().trim().isEmpty()) {
            return List.of();
        }

        // 페이지 번호 파싱
        int pageNum;
        try {
            pageNum = Integer.parseInt(rfpResponse.rfp_page());
        } catch (NumberFormatException e) {
            pageNum = 1;
        }

        SourceResponse source = new SourceResponse(
                1L, // 기본 소스 ID
                "RFP-DOC", // 기본 문서 ID
                pageNum,
                rfpResponse.raw_text()
        );

        return List.of(source);
    }
}
