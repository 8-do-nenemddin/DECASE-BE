package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.requirement.controller.dto.request.ApproveDto;
import com.skala.decase.domain.requirement.controller.dto.response.PendingRequirementDto;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementDto;
import com.skala.decase.domain.requirement.domain.PendingRequirement;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.exception.PendingRequirementException;
import com.skala.decase.domain.requirement.exception.RequirementException;
import com.skala.decase.domain.requirement.repository.PendingRequirementRepository;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
        return pendingRequirementRepository.findAllByProject_ProjectIdAndStatusFalse(projectId).stream()
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

    @Transactional
    public String approveRequests(Long projectId, List<ApproveDto> dtoList) {
        for (ApproveDto dto : dtoList) {
            // 1) PendingRequirement 조회
            PendingRequirement pendingRequirement = pendingRequirementRepository.findById(dto.getPendingPk())
                    .orElseThrow(() -> new PendingRequirementException("해당 요청이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

            // 2) 원본 Requirement 조회
            Requirement originalRequirement = requirementRepository.findByProject_ProjectIdAndReqIdCode(
                    projectId, pendingRequirement.getReqIdCode()
            ).orElseThrow(() -> new RequirementException("원본 요구사항을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

            if (dto.getStatus() == 2) {
                // 승인 처리: Pending 값으로 원본 Requirement 필드 덮어쓰기
                originalRequirement.updateFromPending(
                        pendingRequirement.getType(),
                        pendingRequirement.getLevel1(),
                        pendingRequirement.getLevel2(),
                        pendingRequirement.getLevel3(),
                        pendingRequirement.getName(),
                        pendingRequirement.getDescription(),
                        pendingRequirement.getPriority(),
                        pendingRequirement.getDifficulty(),
                        pendingRequirement.getModReason(),
                        pendingRequirement.getCreatedBy()
                );

                // 원본 Requirement 저장
                requirementRepository.save(originalRequirement);

                // Pending 삭제
                pendingRequirementRepository.delete(pendingRequirement);

            } else if (dto.getStatus() == 1) {
                // 반려 처리: Pending 삭제
                pendingRequirement.setStatus(true);
                pendingRequirementRepository.save(pendingRequirement);
            } else {
                throw new PendingRequirementException("유효하지 않은 상태 값입니다. status는 1(승인) 또는 2(반려)이어야 합니다.", HttpStatus.BAD_REQUEST);
            }
        }
        return "요청된 요구사항이 정상적으로 처리되었습니다.";
    }

    @Transactional
    public String approveRequest(Long projectId, ApproveDto dto) {
        // 1) PendingRequirement 조회
        PendingRequirement pendingRequirement = pendingRequirementRepository.findById(dto.getPendingPk())
                .orElseThrow(() -> new PendingRequirementException("해당 요청이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        // 2) 원본 Requirement 조회
        Requirement originalRequirement = requirementRepository.findByProject_ProjectIdAndReqIdCode(
                projectId, pendingRequirement.getReqIdCode()
        ).orElseThrow(() -> new RequirementException("원본 요구사항을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (dto.getStatus() == 2) {
            // 승인 처리: Pending 값으로 원본 Requirement 필드 덮어쓰기
            originalRequirement.updateFromPending(
                    pendingRequirement.getType(),
                    pendingRequirement.getLevel1(),
                    pendingRequirement.getLevel2(),
                    pendingRequirement.getLevel3(),
                    pendingRequirement.getName(),
                    pendingRequirement.getDescription(),
                    pendingRequirement.getPriority(),
                    pendingRequirement.getDifficulty(),
                    pendingRequirement.getModReason(),
                    pendingRequirement.getCreatedBy()
            );

            // 원본 Requirement 저장
            requirementRepository.save(originalRequirement);

            // Pending 삭제
            pendingRequirementRepository.delete(pendingRequirement);

        } else if (dto.getStatus() == 1) {
            // 반려 처리: Pending 상태만 true로 변경
            pendingRequirement.setStatus(true);
            pendingRequirementRepository.save(pendingRequirement);
        } else {
            throw new PendingRequirementException("유효하지 않은 상태 값입니다. status는 1(반려) 또는 2(승인)이어야 합니다.", HttpStatus.BAD_REQUEST);
        }

        return "요청된 요구사항이 정상적으로 처리되었습니다.";
    }
}
