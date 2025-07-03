package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.requirement.controller.dto.response.*;
import com.skala.decase.domain.requirement.domain.Reception;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.exception.RequirementException;
import com.skala.decase.domain.requirement.mapper.RequirementAuditMapper;
import com.skala.decase.domain.requirement.repository.RequirementAuditRepository;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.RevisionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RequirementAuditService {

    private final RequirementRepository requirementRepository;
    private final RequirementAuditRepository requirementAuditRepository;
    private final RequirementAuditMapper requirementAuditMapper;

    public List<RequirementAuditResponse> findAllByProjectId(long projectId) {
        return requirementAuditRepository.getRequirementHistoryByProjectId(projectId)
                .stream().map(requirementAuditMapper::toResponse).toList();
    }

    public List<RequirementAuditResponse> findByProjectIdAndReqIdCode(long projectId, String reqIdCode) {
        return requirementAuditRepository.getRequirementHistoryByProjectIdAndReqIdCode(projectId, reqIdCode)
                .stream()
                .map(requirementAuditMapper::toResponse)
                .toList();
    }

    public Map<String, List<String>> findModReasonByProjectIdAndReqIdCodes(long projectId, List<String> reqIdCodes) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Map<String, List<String>> results = new LinkedHashMap<>();

        int batchSize = 50;
        for (int i = 0; i < reqIdCodes.size(); i += batchSize) {
            int end = Math.min(i + batchSize, reqIdCodes.size());
            List<String> batch = reqIdCodes.subList(i, end);

            List<RequirementModReasonResponse> audits = requirementAuditRepository.findModReasonByProjectIdAndReqIdCodesNative(projectId, batch);

            Map<String, List<String>> batchMap = audits.stream()
                    .sorted(Comparator.comparing(RequirementModReasonResponse::revisionDate))
                    .collect(Collectors.groupingBy(
                            RequirementModReasonResponse::reqIdCode,
                            LinkedHashMap::new,
                            Collectors.mapping(
                                    dto -> {
                                        String date = dto.revisionDate().format(formatter);
                                        String reason = Optional.ofNullable(dto.ModReason()).orElse("-");
                                        return date + " : " + reason;
                                    },
                                    Collectors.toList()
                            )
                    ));
            results.putAll(batchMap);
        }
        return results;
    }

    public List<MatrixResponse> createMatrix(long projectId) {
        // Aud 테이블에서 ADD 인거 갖고 오기
        // 요구사항 정의서 테이블 데이터(최신) 가져오기
        // 맵핑 -> 리스트 만들기
        // 현재 요구사항에 있으면 수용, 없으면 미수용

        /*
            RevisionType.ADD        // 추가
            RevisionType.MOD        // 수정
            RevisionType.DEL        // 삭제
         */

        List<Requirement> requirements = requirementRepository.findByProjectId(projectId)
                .orElseThrow(() -> new RequirementException("프로젝트에 해당하는 요구사항을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

        List<RequirementAuditDTO> addRequirements = requirementAuditRepository.getRequirementHistoryByProjectId(projectId)
                .stream()
                .filter(requirement -> requirement.getRevisionType() == RevisionType.ADD)
                .toList();

        Map<String, MatrixResponse> matrixs = new HashMap<>();

        for (RequirementAuditDTO add : addRequirements) {
            String reqIdCode = add.getRequirement().getReqIdCode();
            if (matrixs.containsKey(reqIdCode)) {
                continue;
            }

            // 새로운 거에 있으면 그거 넣고 수용
            Requirement matchRequirement = requirements
                    .stream().filter(requirement -> requirement.getReqIdCode().equals(reqIdCode)).toList().get(0);
            if (matchRequirement != null) {
                MatrixResponse matrixResponse = new MatrixResponse(
                        reqIdCode,
                        matchRequirement.getLevel1(),
                        matchRequirement.getLevel2(),
                        matchRequirement.getLevel3(),
                        matchRequirement.getName(),
                        matchRequirement.getDescription(),
                        Reception.ACCEPTED
                );

                matrixs.put(reqIdCode, matrixResponse);
                continue;
            }

            // 없으면 delete 넣고 미수용
            RequirementAuditDTO deleteRequirement = requirementAuditRepository.getRequirementHistoryByProjectId(projectId)
                    .stream().filter(requirement -> requirement.getRequirement().getReqIdCode().equals(reqIdCode) && requirement.getRevisionType() == RevisionType.DEL)
                    .toList().get(0);

            MatrixResponse matrixResponse = new MatrixResponse(
                    reqIdCode,
                    deleteRequirement.getRequirement().getLevel1(),
                    deleteRequirement.getRequirement().getLevel2(),
                    deleteRequirement.getRequirement().getLevel3(),
                    deleteRequirement.getRequirement().getName(),
                    deleteRequirement.getRequirement().getDescription(),
                    Reception.UNACCEPTED
            );
            matrixs.put(reqIdCode, matrixResponse);
        }

        return matrixs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // key = reqIdCode 기준 오름차순 정렬
                .map(Map.Entry::getValue)
                .toList();
    }

    public List<RequirementResponse> findByProjectIdAndRevisionCount(Long projectId, int revisionCount) {
        return requirementAuditRepository.findByProjectIdAndRevisionCount(projectId, revisionCount);
    }
}
