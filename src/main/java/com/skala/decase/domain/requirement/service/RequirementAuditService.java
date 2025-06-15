package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.requirement.controller.dto.response.RequirementAuditDTO;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementAuditResponse;
import com.skala.decase.domain.requirement.mapper.RequirementAuditMapper;
import com.skala.decase.domain.requirement.repository.RequirementAuditRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RequirementAuditService {

    private final RequirementAuditRepository requirementAuditRepository;
    private final RequirementAuditMapper requirementAuditMapper;

    public List<RequirementAuditResponse> findAllByProjectId(long projectId) {
        return requirementAuditRepository.getRequirementHistoryByProjectId(projectId)
                .stream().map(requirementAuditMapper::toResponse).toList();
    }

    public List<RequirementAuditResponse> findByProjectIdAndReqIdCode(long projectId, String reqIdCode) {
        return requirementAuditRepository.getRequirementHistoryByProjectId(projectId)
                .stream()
                .filter(requirement -> requirement.getRequirement().getReqIdCode().equals(reqIdCode))
                .sorted(Comparator.comparing(RequirementAuditDTO::getRevisionDate)) // 오름차순 정렬 추가
                .map(requirementAuditMapper::toResponse)
                .toList();
    }
}
