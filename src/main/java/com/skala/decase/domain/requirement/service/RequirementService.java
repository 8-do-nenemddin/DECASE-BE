package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.exception.MemberException;
import com.skala.decase.domain.member.repository.MemberRepository;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.domain.requirement.controller.dto.request.RequirementDto;
import com.skala.decase.domain.requirement.controller.dto.request.RequirementRevisionDto;
import com.skala.decase.domain.requirement.controller.dto.request.UpdateRequirementDto;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementWithSourceResponse;
import com.skala.decase.domain.requirement.domain.PendingRequirement;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.RequirementType;
import com.skala.decase.domain.requirement.exception.RequirementException;
import com.skala.decase.domain.requirement.mapper.RequirementServiceMapper;
import com.skala.decase.domain.requirement.repository.PendingRequirementRepository;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import com.skala.decase.domain.source.domain.Source;
import com.skala.decase.domain.source.service.SourceRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ProjectService projectService;
    private final SourceRepository sourceRepository;
    private final MemberRepository memberRepository;
    private final PendingRequirementRepository pendingRequirementRepository;

    // 리버전 기본값 1로 받도록 하기 위함
    public List<RequirementWithSourceResponse> getGeneratedRequirements(Long projectId) {
        return getGeneratedRequirements(projectId, 1);
    }

    /**
     * 특정 버전의 요구사항 정의서를 불러옵니다.
     *
     * @param projectId
     * @param revisionCount
     * @return
     */
    public List<RequirementWithSourceResponse> getGeneratedRequirements(Long projectId, int revisionCount) {
        Project project = projectService.findByProjectId(projectId);

        //유효한 요구사항 리스트 조회
        List<Requirement> requirements = requirementRepository.findValidRequirementsByProjectAndRevision(
                projectId, revisionCount);
//		System.out.println("조회된 요구사항 개수: " + requirements.size());
        //요구사항이 없는 경우
        if (requirements.isEmpty()) {
            return new ArrayList<>();
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
//		System.out.println("ReqPks: " + reqPks);

        // 해당 req_id_code 들의 특정 버전 이하 모든 요구사항 조회 -> source 용
        List<Requirement> allRelatedReq = requirementRepository
                .findRequirementsByReqPksAndRevision(reqPks, revisionCount);
//		System.out.println("조회된 관련 요구사항 개수: " + allRelatedReq.size());

        // req_id_code별로 allRelatedReq 그룹화하여 modReason 추출
        Map<String, List<String>> modReasonsByReqIdCode = allRelatedReq.stream()
                .collect(Collectors.groupingBy(
                        Requirement::getReqIdCode,
                        Collectors.mapping(
                                req -> req.getModReason() != null ? req.getModReason() : "",
                                Collectors.toList()
                        )
                ));

        // 특정 버전 이하 모든 요구사항 PK 목록 추출
        List<Long> allRelatedReqPks = allRelatedReq.stream()
                .map(Requirement::getReqPk)
                .toList();

        // 해당 요구사항들의 Source 정보 일괄 조회
        List<Source> sources = sourceRepository.findByRequirementReqPks(allRelatedReqPks);
//		System.out.println("조회된 Source 개수: " + sources.size());
//		sources.forEach(source ->
//				System.out.println("SourceId: " + source.getSourceId() + ", ReqPk: " + source.getRequirement().getReqPk() +
//						", ReqIdCode: " + source.getRequirement().getReqIdCode() + ", DocId: " +
//						(source.getDocument() != null ? source.getDocument().getDocId() : "null"))
//		);

        // 요구사항별로 Source 그룹화
        Map<String, List<Source>> sourcesByReqPk = sources.stream()
                .collect(Collectors.groupingBy(Source::getReqIdCode));
//		System.out.println("=== 5단계: Source 그룹화 결과 ===");
//		sourcesByReqPk.forEach((reqPk, sourceList) ->
//				System.out.println("ReqPk: " + reqPk + " -> Source 개수: " + sourceList.size())
//		);

        // 각 요구사항에 해당하는 Source 리스트 설정
        latestRequirements.forEach(requirement -> {
            List<Source> reqSources = sourcesByReqPk.getOrDefault(requirement.getReqIdCode(), new ArrayList<>());
            // 여기서는 기존 sources 리스트를 clear하고 새로 추가
            requirement.getSources().clear();
            requirement.getSources().addAll(reqSources);
        });
//		System.out.println("=== 최종 결과 ===");
//		System.out.println("반환할 요구사항 개수: " + requirements.size());
//		requirements.forEach(req ->
//				System.out.println("ReqPk: " + req.getReqPk() + ", ReqIdCode: " + req.getReqIdCode() +
//						", Sources 개수: " + req.getSources().size())
//		);

        return latestRequirements.stream()
                .map(requirement -> {
                    List<String> modReasons = modReasonsByReqIdCode.getOrDefault(requirement.getReqIdCode(),
                            new ArrayList<>());
                    return RequirementServiceMapper.toReqWithSrcResponse(requirement, modReasons, revisionCount);
                })
                .sorted(Comparator
                        .comparing(RequirementWithSourceResponse::type)
                        .thenComparing(RequirementWithSourceResponse::reqIdCode))
                .toList();

    }

    public Map<String, List<String>> getRequirementCategory(Long projectId, int revisionCount) {

        Project project = projectService.findByProjectId(projectId);

        List<Requirement> requirements = requirementRepository.findValidRequirementsByProjectAndRevision(project.getProjectId(),revisionCount);

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

    public List<RequirementDto> getFilteredRequirements(Long projectId, String query,
                                                        String level1, String level2, String level3,
                                                        Integer type, Integer difficulty, Integer priority,
                                                        List<String> docTypes) {

        Project project = projectService.findByProjectId(projectId);

        List<Requirement> requirements = requirementRepository.findByProject_AndIsDeletedFalse(project);

        // 스트림 필터링
        return requirements.stream()
                .filter(r -> query == null || r.getName().contains(query) || r.getDescription().contains(query))
                .filter(r -> level1 == null || level1.equals(r.getLevel1()))
                .filter(r -> level2 == null || level2.equals(r.getLevel2()))
                .filter(r -> level3 == null || level3.equals(r.getLevel3()))
                .filter(r -> type == null || r.getType().ordinal() == type)
                .filter(r -> difficulty == null || r.getDifficulty().ordinal() == difficulty)
                .filter(r -> priority == null || r.getPriority().ordinal() == priority)
                .filter(r -> docTypes == null || r.getSources().stream()
                        .anyMatch(rd -> {
                            String docId = rd.getDocument().getDocId();
                            String prefix = docId.split("-")[0];
                            return docTypes.contains(prefix);
                        }))
                .map(RequirementDto::fromEntity)
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
        for (int i = 1; i <= maxRevision; i++) {
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

    /**
     * 사용자가 직접 화면에서 요구사항 정의서 내용을 수정할때 리비전 업데이트 x
     */
//    @Transactional
//    public void updateRequirement(Long projectId, int revisionCount, List<UpdateRequirementDto> dtoList) {
//        Project project = projectService.findByProjectId(projectId);
//
//        for (UpdateRequirementDto req : dtoList) {
//            Member member = memberRepository.findById(req.getMemberId())
//                    .orElseThrow(() -> new MemberException("존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND));
//
//            Requirement requirement = requirementRepository.findById(req.getReqPk())
//                    .orElseThrow(() -> new RequirementException("해당 요구사항이 존재하지 않습니다.", HttpStatus.NOT_FOUND));
//
//            // 프로젝트 소속 확인
//            if (requirement.getProject().getProjectId() != projectId) {
//                throw new IllegalArgumentException("해당 프로젝트의 요구사항이 아닙니다.");
//            }
//
//            // 업데이트 된 요구사항 저장
//            Requirement updatedReq = req.toEntity(project, requirement.getReqIdCode(), revisionCount, member);
//            Requirement savedReq = requirementRepository.save(updatedReq);
//
//            // 기존 Source를 새로운 요구사항과 연결
//            List<Source> sources = sourceRepository.findAllByRequirement(requirement);
//            List<Source> newSources = sources.stream()
//                    .map(oldSource -> {
//                        // 기존 Source 정보를 바탕으로 새로운 Source 생성
//                        Source newSource = new Source();
//                        newSource.createSource(
//                                savedReq,  // 새로운 요구사항과 연결
//                                oldSource.getDocument(),  // 기존 문서 정보 유지
//                                oldSource.getPageNum(),   // 기존 페이지 번호 유지
//                                oldSource.getRelSentence()  // 기존 관련 문장 유지
//                        );
//                        return newSource;
//                    })
//                    .toList();
//
//            sourceRepository.saveAll(newSources);
//
//            // 기존 요구사항 soft delete
//            requirement.setDeleted(true);
//            requirement.setDeletedRevision(revisionCount);
//            requirementRepository.save(requirement);
//        }
//    }

    @Transactional
    public void updateRequirement(Long projectId, int revisionCount, List<UpdateRequirementDto> dtoList) {
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

            PendingRequirement pendingRequirement = new PendingRequirement();
            pendingRequirement.createPendingRequirement(req,requirement.getReqIdCode(), project, member); // 변경 사항 업데이트

            pendingRequirementRepository.save(pendingRequirement);

            // 기존 Source를 새로운 요구사항과 연결
//            List<Source> sources = sourceRepository.findAllByRequirement(requirement);
//            List<Source> newSources = sources.stream()
//                    .map(oldSource -> {
//                        // 기존 Source 정보를 바탕으로 새로운 Source 생성
//                        Source newSource = new Source();
//                        newSource.createSource(
//                                savedReq,  // 새로운 요구사항과 연결
//                                oldSource.getDocument(),  // 기존 문서 정보 유지
//                                oldSource.getPageNum(),   // 기존 페이지 번호 유지
//                                oldSource.getRelSentence()  // 기존 관련 문장 유지
//                        );
//                        return newSource;
//                    })
//                    .toList();
//
//            sourceRepository.saveAll(newSources);
        }
    }

    /**
     * 사용자가 직접 화면에서 요구사항 정의서 내용을 삭제할때 리비전 업데이트 x
     */
    @Transactional
    public String deleteRequirement(Long projectId, Long reqPk) {
        Project project = projectService.findByProjectId(projectId);

        Requirement requirement = requirementRepository.findById(reqPk)
                .orElseThrow(() -> new RequirementException("해당 요구사항이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        int maxRevision = Optional.ofNullable(requirementRepository.getMaxRevisionCount(project)).orElse(0);

        // 기존 요구사항 soft delete 정보 입력
        requirement.setDeleted(true);
        requirement.setDeletedRevision(maxRevision);

        return "요구사항이 삭제되었습니다.";
    }
}

