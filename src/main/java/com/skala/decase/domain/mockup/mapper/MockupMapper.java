package com.skala.decase.domain.mockup.mapper;

import com.skala.decase.domain.mockup.controller.dto.request.CreateMockUpRequest;
import com.skala.decase.domain.mockup.controller.dto.request.CreateMockUpSourceRequest;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementResponse;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementWithSourceResponse;
import com.skala.decase.domain.requirement.controller.dto.response.SourceResponse;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.RequirementType;
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

    public CreateMockUpRequest toCreateMockUp(RequirementResponse response) {
        List<CreateMockUpSourceRequest> sources = null;
        if (response.getSources() != null) {
            sources = response.getSources().stream()
                    .map(src -> new CreateMockUpSourceRequest(src.pageNum(), src.relSentence()))
                    .toList();
        }

        return new CreateMockUpRequest(
                response.getName(),
                RequirementType.toKorean(response.getType()),
                sources,
                response.getDescription(),
                response.getLevel1(),
                response.getLevel2(),
                response.getLevel3(),
                response.getPriority(),
                response.getDifficulty(),
                response.getReqIdCode()
        );
    }

    /**
     * Requirement를 CreateMockUpRequest로 변환
     */
    public CreateMockUpRequest toCreateMockUpRequest(Requirement requirement) {
        if (requirement == null) {
            return null;
        }

        String typeKor = RequirementType.toKorean(requirement.getType().name());

        // Source -> CreateMockUpSourceRequest 변환
        List<CreateMockUpSourceRequest> sources = null;
        if (requirement.getSources() != null) {
            sources = requirement.getSources().stream()
                    .map(src -> new CreateMockUpSourceRequest(src.getPageNum(), src.getRelSentence()))
                    .collect(Collectors.toList());
        }

        return new CreateMockUpRequest(
                requirement.getName(),
                typeKor,
                sources,
                requirement.getDescription(),
                requirement.getLevel1(),
                requirement.getLevel2(),
                requirement.getLevel3(),
                requirement.getPriority().name(),
                requirement.getDifficulty().name(),
                requirement.getReqIdCode()
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


}
