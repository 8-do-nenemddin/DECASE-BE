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
import com.skala.decase.domain.source.service.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PendingRequirementService {
    private final PendingRequirementRepository pendingRequirementRepository;
    private final RequirementRepository requirementRepository;
    private final SourceRepository sourceRepository;

    @Transactional
    public List<PendingRequirementDto> getPendingRequirementsList(Long projectId) {
        List<PendingRequirement> pendingList = pendingRequirementRepository.findAllByProject_ProjectIdAndStatusFalse(projectId);

        List<PendingRequirementDto> result = new ArrayList<>();

        for (PendingRequirement pendingRequirement : pendingList) {
            Optional<Requirement> originalOpt = requirementRepository
                    .findByProject_ProjectIdAndReqIdCode(projectId, pendingRequirement.getReqIdCode());

            if (originalOpt.isEmpty()) {
                // 원본 요구사항 없으면 해당 pending 삭제
                pendingRequirementRepository.delete(pendingRequirement);
                continue;
            }

            Requirement original = originalOpt.get();

            PendingRequirementDto dto = PendingRequirementDto.builder()
                    .original(RequirementDto.builder()
                            .id(original.getReqPk())
                            .idCode(pendingRequirement.getReqIdCode())
                            .type(original.getType().toString())
                            .name(original.getName())
                            .description(original.getDescription())
                            .category1(original.getLevel1())
                            .category2(original.getLevel2())
                            .category3(original.getLevel3())
                            .priority(original.getPriority().toString())
                            .difficulty(original.getDifficulty().toString())
                            .reception(original.getReception().toString())
                            .modifiedDate(
                                    Optional.ofNullable(original.getModifiedDate())
                                            .map(LocalDateTime::toString)
                                            .orElse(null)
                            )
                            .modifier(original.getCreatedBy().getName())
                            .reason(original.getModReason())
                            .isDelete(false)
                            .build())
                    .proposed(RequirementDto.builder()
                            .id(pendingRequirement.getPendingPk())
                            .idCode(pendingRequirement.getReqIdCode())
                            .type(pendingRequirement.getType() != null ? pendingRequirement.getType().toString() : original.getType().toString())
                            .name(pendingRequirement.getName() != null ? pendingRequirement.getName() : original.getName())
                            .description(pendingRequirement.getDescription() != null ? pendingRequirement.getDescription() : original.getDescription())
                            .category1(pendingRequirement.getLevel1() != null ? pendingRequirement.getLevel1() : original.getLevel1())
                            .category2(pendingRequirement.getLevel2() != null ? pendingRequirement.getLevel2() : original.getLevel2())
                            .category3(pendingRequirement.getLevel3() != null ? pendingRequirement.getLevel3() : original.getLevel3())
                            .priority(pendingRequirement.getPriority() != null ? pendingRequirement.getPriority().toString() : original.getPriority().toString())
                            .difficulty(pendingRequirement.getDifficulty() != null ? pendingRequirement.getDifficulty().toString() : original.getDifficulty().toString())
                            .reception(pendingRequirement.getReception() != null ? pendingRequirement.getReception().toString() : original.getReception().toString())
                            .modifiedDate(pendingRequirement.getModifiedDate().toString())
                            .modifier(pendingRequirement.getCreatedBy().getName())
                            .reason(pendingRequirement.getModReason())
                            .isDelete(pendingRequirement.getIsDelete())
                            .build())
                    .build();

            result.add(dto);
        }

        return result;
    }

    public void approveRequest(Long projectId, ApproveDto approveDto) {
        // 1) PendingRequirement 조회
        PendingRequirement pendingRequirement = pendingRequirementRepository.findById(approveDto.getPendingPk())
                .orElseThrow(() -> new PendingRequirementException("해당 요청이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        // 2) 원본 Requirement 조회
        Requirement originalRequirement = requirementRepository.findByProject_ProjectIdAndReqIdCode(
                projectId, pendingRequirement.getReqIdCode()
        ).orElseThrow(() -> new RequirementException("원본 요구사항을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (approveDto.getStatus() == 2) {
            if (pendingRequirement.getIsDelete()) {
                originalRequirement.setModifiedDate(pendingRequirement.getModifiedDate());
                originalRequirement.setModReason(pendingRequirement.getModReason());
                originalRequirement.setModifiedBy(pendingRequirement.getCreatedBy());

                List<PendingRequirement> pendinglist = pendingRequirementRepository.findAllPendingRequirementByReqIdCode(pendingRequirement.getReqIdCode());

                // ✅ Envers 스냅샷 찍기 전에 연관 객체 강제 초기화
                Hibernate.initialize(originalRequirement.getCreatedBy());
                Hibernate.initialize(originalRequirement.getModifiedBy());

                System.out.println("createdBy initialized? " + Hibernate.isInitialized(originalRequirement.getCreatedBy()));
                System.out.println("modifiedBy initialized? " + Hibernate.isInitialized(originalRequirement.getModifiedBy()));

                requirementRepository.save(originalRequirement);
                requirementRepository.flush(); // 이 시점에서 초기화된 필드까지 Envers가 추적
                requirementRepository.delete(originalRequirement);
                pendingRequirementRepository.deleteAll(pendinglist);
            } else {
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
                        pendingRequirement.getReception(),
                        pendingRequirement.getModReason(),
                        pendingRequirement.getCreatedBy()
                );
                System.out.println("반영 안됨 " + originalRequirement.getReception());
                // 원본 Requirement 저장
                requirementRepository.save(originalRequirement);
            }
            // Pending 삭제
            pendingRequirementRepository.delete(pendingRequirement);
        } else if (approveDto.getStatus() == 1) {
            // 반려 처리: Pending 삭제
            pendingRequirement.setStatus(true);
            pendingRequirementRepository.save(pendingRequirement);
        } else {
            throw new PendingRequirementException("유효하지 않은 상태 값입니다. status는 1(승인) 또는 2(반려)이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public String approveRequests(Long projectId, List<ApproveDto> dtoList) {
        for (ApproveDto dto : dtoList) {
            approveRequest(projectId, dto);
        }
        return "요청된 요구사항이 정상적으로 처리되었습니다.";
    }
}
