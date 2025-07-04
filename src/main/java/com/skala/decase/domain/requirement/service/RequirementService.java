package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.exception.MemberException;
import com.skala.decase.domain.member.repository.MemberProjectRepository;
import com.skala.decase.domain.member.repository.MemberRepository;
import com.skala.decase.domain.mockup.controller.dto.request.CreateMockUpRequest;
import com.skala.decase.domain.mockup.mapper.MockupMapper;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.domain.requirement.controller.dto.request.RequirementRevisionDto;
import com.skala.decase.domain.requirement.controller.dto.request.UpdateRequirementDto;
import com.skala.decase.domain.requirement.controller.dto.request.UpdateSrsAgentRequest;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementResponse;
import com.skala.decase.domain.requirement.domain.*;
import com.skala.decase.domain.requirement.exception.RequirementException;
import com.skala.decase.domain.requirement.mapper.RequirementServiceMapper;
import com.skala.decase.domain.requirement.mapper.RequirementUpdateServiceMapper;
import com.skala.decase.domain.requirement.repository.PendingRequirementRepository;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import com.skala.decase.domain.source.domain.Source;
import com.skala.decase.domain.source.service.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ProjectService projectService;
    private final SourceRepository sourceRepository;

    private final RequirementUpdateServiceMapper requirementUpdateServiceMapper;
    private final MockupMapper mockupMapper;

    private final MemberRepository memberRepository;
    private final PendingRequirementRepository pendingRequirementRepository;
    private final RequirementAuditService requirementAuditService;
    private final MemberProjectRepository memberProjectRepository;

    private final RequirementServiceMapper requirementServiceMapper;

    // 리버전 기본값 1로 받도록 하기 위함
    public List<RequirementResponse> getGeneratedRequirements(Long projectId) {
        return getGeneratedRequirements(projectId, 1);
    }

    /**
     * 특정 버전의 요구사항 정의서를 불러옵니다.
     *
     * @param projectId
     * @param revisionCount
     * @return
     */
    public List<RequirementResponse> getGeneratedRequirements(Long projectId, int revisionCount) {
        Project project = projectService.findByProjectId(projectId);

        // 버전 별로 가져올 수 있도록 수정해야함 -> aud에서 reqCodeId 별로 그룹핑해서 revisionCount보다 같거나 작은걸로 하면 됨
        List<RequirementResponse> requirements = requirementAuditService.findByProjectIdAndRevisionCount(projectId, revisionCount);

        if (requirements == null) {
            throw new RequirementException("요구사항이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        // 모든 reqIdCode 수집
        List<String> reqIdCodes = requirements.stream()
                .map(RequirementResponse::getReqIdCode)
                .distinct()
                .toList();

        // 한 번에 이력 조회 (Map 형태로 반환)
        Map<String, List<String>> reasonMap = requirementAuditService.findModReasonByProjectIdAndReqIdCodes(projectId, reqIdCodes);
        // 변환
        return requirements.stream()
                .peek(req -> {
                    List<String> reasons = reasonMap.getOrDefault(req.getReqIdCode(), List.of());
                    req.setModReason(reasons);
                })
                .sorted(Comparator.comparing(RequirementResponse::getType)
                        .thenComparing(RequirementResponse::getReqIdCode))
                .toList();
    }

    /**
     * 특정 버전의 요구사항 정의서를 불러옵니다. 출처 포함 x
     *
     * @param projectId
     * @param revisionCount
     * @return
     */
    public List<UpdateSrsAgentRequest> getRequirementsForUpdate(Long projectId, int revisionCount) {
        Project project = projectService.findByProjectId(projectId);

        //유효한 요구사항 리스트 조회
        List<Requirement> requirements = requirementRepository.findValidRequirementsByProjectAndRevision(
                projectId, revisionCount);

        //요구사항이 없는 경우
        if (requirements.isEmpty()) {
            return new ArrayList<>();
        }

        // 이 로직 수정 해야함 !!!
        // reqIdCode + revisionCount 조합별로 createdDate 기준 최신 요구사항만 필터링
        List<Requirement> latestRequirements = requirements.stream()
                .collect(Collectors.groupingBy(
                        req -> req.getReqIdCode() + "_" + req.getRevisionCount(),
                        Collectors.maxBy(Comparator.comparing(Requirement::getCreatedDate))
                ))
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return latestRequirements.stream()
                .map(requirementUpdateServiceMapper::toUpdateREQ)
                .toList();

    }

    /**
     * 특정 버전의 기능적 요구사항을 불러옵니다.
     *
     * @param projectId
     * @param revisionCount
     * @return
     */
    public List<CreateMockUpRequest> getFunctionalRequirements(Long projectId, int revisionCount) {
        Project project = projectService.findByProjectId(projectId);

        //유효한 기능적 요구사항 리스트 조회
        List<Requirement> requirements = requirementRepository.findValidFRsByProjectAndRevision(
                projectId, revisionCount);
        //요구사항이 없는 경우
        if (requirements.isEmpty()) {
            throw new RequirementException("기능적 요구사항이 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }
        // reqIdCode + revisionCount 조합별로 createdDate 기준 최신 요구사항만 필터링
        List<Requirement> latestRequirements = requirements.stream()
                .collect(Collectors.groupingBy(
                        req -> req.getReqIdCode() + "_" + req.getRevisionCount(),
                        Collectors.maxBy(Comparator.comparing(Requirement::getCreatedDate))
                ))
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        // 유효한 요구사항 PK 목록 추출
        List<Long> reqPks = latestRequirements.stream()
                .map(Requirement::getReqPk)
                .toList();

        // 해당 req_id_code 들의 특정 버전 이하 모든 요구사항 조회 -> source 용
        List<Requirement> allRelatedReq = requirementRepository
                .findRequirementsByReqPksAndRevision(reqPks, revisionCount);

        // 특정 버전 이하 모든 요구사항 PK 목록 추출
        List<Long> allRelatedReqPks = allRelatedReq.stream()
                .map(Requirement::getReqPk)
                .toList();

        // 해당 요구사항들의 Source 정보 일괄 조회
        List<Source> sources = sourceRepository.findByRequirementReqPks(allRelatedReqPks);

        // 요구사항별로 Source 그룹화
        Map<String, List<Source>> sourcesByReqIdCode = sources.stream()
                .collect(Collectors.groupingBy(Source::getReqIdCode));

        // 각 요구사항에 해당하는 Source 리스트 설정
        latestRequirements.forEach(requirement -> {
            List<Source> reqSources = sourcesByReqIdCode.getOrDefault(requirement.getReqIdCode(), new ArrayList<>());
            // 여기서는 기존 sources 리스트를 clear하고 새로 추가
            requirement.getSources().clear();
            requirement.getSources().addAll(reqSources);
        });

        return latestRequirements.stream()
                .map(mockupMapper::toCreateMockUpRequest)
                .toList();
    }

    public Map<String, List<String>> getRequirementCategory(Long projectId, int revisionCount) {

        Project project = projectService.findByProjectId(projectId);

        List<Requirement> requirements = requirementRepository.findValidRequirementsByProjectAndRevision(
                project.getProjectId(), revisionCount);

        Set<String> level1Set = new HashSet<>();
        Set<String> level2Set = new HashSet<>();
        Set<String> level3Set = new HashSet<>();

        for (Requirement req : requirements) {
            if (req.getLevel1() != null) {
                level1Set.add(req.getLevel1());
            }
            if (req.getLevel2() != null) {
                level2Set.add(req.getLevel2());
            }
            if (req.getLevel3() != null) {
                level3Set.add(req.getLevel3());
            }
        }

        Map<String, List<String>> categoryMap = new HashMap<>();
        categoryMap.put("대분류", new ArrayList<>(level1Set));
        categoryMap.put("중분류", new ArrayList<>(level2Set));
        categoryMap.put("소분류", new ArrayList<>(level3Set));

        return categoryMap;
    }

    public List<RequirementResponse> getFilteredRequirements(Long projectId, int revisionCount, String query,
                                                             String level1, String level2, String level3,
                                                             Integer type, Integer difficulty,
                                                             Integer priority,
                                                             List<String> docTypes) {

        Project project = projectService.findByProjectId(projectId);
        List<RequirementResponse> response = getGeneratedRequirements(projectId, revisionCount);

        // 스트림 필터링
        return response.stream()
                .filter(r -> query == null || r.getName().contains(query) || r.getDescription().contains(query))
                .filter(r -> level1 == null || level1.equals(r.getLevel1()))
                .filter(r -> level2 == null || level2.equals(r.getLevel2()))
                .filter(r -> level3 == null || level3.equals(r.getLevel3()))
                .filter(r -> type == null || r.getType().equals(RequirementType.fromOrdinal(type)))
                .filter(r -> difficulty == null || r.getDifficulty().equals(Difficulty.fromOrdinal(difficulty)))
                .filter(r -> priority == null || r.getPriority().equals(Priority.fromOrdinal(priority)))
                .filter(r -> docTypes == null || r.getSources().stream()
                        .anyMatch(rd -> {
                            String docId = rd.docId();
                            String prefix = docId.split("-")[0];
                            return docTypes.contains(prefix);
                        }))
                .toList();
    }

    // 요구사항 버전 별 조회
    public List<RequirementRevisionDto> getRequirementRevisions(Long projectId) {
        Project project = projectService.findByProjectId(projectId);

        int maxRevision = Optional.ofNullable(requirementRepository.getMaxRevisionCount(project)).orElse(0);

        String prefix = "요구사항 정의서_";
        int digitCount = String.valueOf(maxRevision).length();
        String format = "%0" + digitCount + "d";

        List<RequirementRevisionDto> versionList = new ArrayList<>();
        for (int i = maxRevision; i > 0; i--) {
            int finalI = i;
            Optional<Requirement> requirementOpt = requirementRepository.findFirstByProjectAndRevisionCount(project, i);
            requirementOpt.ifPresent(req -> {
                String label = prefix + String.format(format, finalI);
                String date = req.getCreatedDate().toLocalDate().toString();
                versionList.add(new RequirementRevisionDto(label, finalI, date));
            });
        }
        return versionList;
    }

    @Transactional
    public void updateRequirement(Long projectId, List<UpdateRequirementDto> dtoList) {
        Project project = projectService.findByProjectId(projectId);

        for (UpdateRequirementDto req : dtoList) {
            Member member = memberRepository.findById(req.getMemberId())
                    .orElseThrow(() -> new MemberException("존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND));

            Requirement requirement = requirementRepository.findById(req.getReqPk())
                    .orElseThrow(() -> new RequirementException("해당 요구사항이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

            // 프로젝트 소속 확인
            if (requirement.getProject().getProjectId() != projectId) {
                throw new RequirementException("해당 프로젝트의 요구사항이 아닙니다.", HttpStatus.BAD_REQUEST);
            }

            //admin일 경우 바로 반영
            if (memberProjectRepository.existsAdminPermission(project, member)) {
                updateRequirementByAdmin(req, requirement, member);
                continue;
            }

            System.out.println("=== PendingRequirement 생성 전 ===");
            PendingRequirement pendingRequirement = new PendingRequirement();

            System.out.println("=== createPendingRequirement 호출 전 ===");
            pendingRequirement.createPendingRequirement(req, requirement.getReqIdCode(), project, member); // 변경 사항 업데이트

            System.out.println("=== save 호출 전 ===");
            pendingRequirementRepository.save(pendingRequirement);
        }
    }

    private void updateRequirementByAdmin(UpdateRequirementDto req, Requirement requirement, Member member) {
        if (requirement.isDeleted()) {
            requirementRepository.delete(requirement);
            return;
        }
        requirement.update(req, member);
    }

    /**
     * 사용자가 직접 화면에서 요구사항 정의서 내용을 삭제할때 리비전 업데이트 x
     */
    @Transactional
    public void deleteRequirement(Long projectId, Long reqPk, String reason, Long memberId) {
        Project project = projectService.findByProjectId(projectId);

        Requirement requirement = requirementRepository.findById(reqPk)
                .orElseThrow(() -> new RequirementException("해당 요구사항이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("해당 멤버가 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        if (memberProjectRepository.existsAdminPermission(project, member)) {
            requirementRepository.delete(requirement);
        }

        PendingRequirement pendingRequirement = new PendingRequirement();
        pendingRequirement.createPendingRequirementDelete(requirement, reason, member); // 변경 사항 업데이트

        pendingRequirementRepository.save(pendingRequirement);
    }

}
