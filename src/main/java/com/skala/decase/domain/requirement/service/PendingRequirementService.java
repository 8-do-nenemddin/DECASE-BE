package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.requirement.controller.dto.response.PendingRequirementDto;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementDto;
import com.skala.decase.domain.requirement.repository.PendingRequirementRepository;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PendingRequirementService {
    private final PendingRequirementRepository pendingRequirementRepository;
	private final RequirementRepository requirementRepository;

    @Transactional(readOnly = true)
    public List<PendingRequirementDto> getPendingRequirementsList(Long projectId) {
        return pendingRequirementRepository.findAllByProject_ProjectId(projectId).stream()
            .map(pendingRequirement -> {
                // 동일한 reqIdCode, projectId로 원본 찾기
                var originalRequirement = requirementRepository
                        .findByProject_ProjectIdAndReqIdCode(
                                projectId, pendingRequirement.getReqIdCode()
                        ).orElseThrow(() -> new IllegalArgumentException("원본 요구사항을 찾을 수 없습니다."));

                return PendingRequirementDto.builder()
                    .original(RequirementDto.builder()
                        .id(originalRequirement.getReqPk())
                        .idCode(pendingRequirement.getReqIdCode())
                        .type(originalRequirement.getType().toString())
                        .name(originalRequirement.getName())
                        .description(originalRequirement.getDescription())
                        .category1(originalRequirement.getLevel1())
                        .category2(originalRequirement.getLevel2())
                        .category3(originalRequirement.getLevel3())
                        .priority(originalRequirement.getPriority().toString())
                        .difficulty(originalRequirement.getDifficulty().toString())
                        .modifiedDate(
                                Optional.ofNullable(originalRequirement.getModifiedDate())
                                        .map(LocalDateTime::toString)
                                        .orElse(null)
                        )
                        .modifier(originalRequirement.getCreatedBy().getName())
                        .reason(Optional.ofNullable(originalRequirement.getModReason()).orElse(null))
                        .build())
                    .proposed(RequirementDto.builder()
                        .id(pendingRequirement.getPendingPk())
                        .idCode(pendingRequirement.getReqIdCode())
                        .type(pendingRequirement.getType().toString())
                        .name(pendingRequirement.getName())
                        .description(pendingRequirement.getDescription())
                        .category1(pendingRequirement.getLevel1())
                        .category2(pendingRequirement.getLevel2())
                        .category3(pendingRequirement.getLevel3())
                        .priority(pendingRequirement.getPriority().toString())
                        .difficulty(pendingRequirement.getDifficulty().toString())
                        .modifiedDate(pendingRequirement.getModifiedDate().toString())
                        .modifier(pendingRequirement.getCreatedBy().getName())
                        .reason(pendingRequirement.getModReason())
                        .build())
                    .build();
            })
            .toList();
    }
}
