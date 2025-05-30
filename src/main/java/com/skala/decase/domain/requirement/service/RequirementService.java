package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.exception.ProjectException;
import com.skala.decase.domain.project.repository.ProjectRepository;
import com.skala.decase.domain.requirement.controller.dto.RequirementDto;
import com.skala.decase.domain.requirement.controller.dto.RequirementRevisionDto;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RequirementService {

	private final RequirementRepository requirementRepository;
	private final ProjectRepository projectRepository;

	// 기본값 1로 받도록 하기 위함
	public List<Requirement> getGeneratedRequirements(Long projectId) {
		return getGeneratedRequirements(projectId, 1);
	}

	public List<Requirement> getGeneratedRequirements(Long projectId, int revisionCount) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ProjectException("존재하지 않는 프로젝트 입니다.", HttpStatus.NOT_FOUND));
		return requirementRepository.findByProject_AndRevisionCountAndIsDeletedFalse(project, revisionCount);
	}

	public Map<String, List<String>> getRequirementCategory(Long projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ProjectException("존재하지 않는 프로젝트 입니다.", HttpStatus.NOT_FOUND));

		List<Requirement> requirements = requirementRepository.findByProject_AndIsDeletedFalse(project);

		Set<String> level1Set = new HashSet<>();
		Set<String> level2Set = new HashSet<>();
		Set<String> level3Set = new HashSet<>();

		for (Requirement req : requirements) {
			if (req.getLevel1() != null) level1Set.add(req.getLevel1());
			if (req.getLevel2() != null) level2Set.add(req.getLevel2());
			if (req.getLevel3() != null) level3Set.add(req.getLevel3());
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

		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ProjectException("존재하지 않는 프로젝트입니다.", HttpStatus.NOT_FOUND));

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
				.filter(r -> docTypes == null || r.getRequirementDocuments().stream()
						.anyMatch(rd -> {
							String docId= rd.getDocument().getDocId();
							String prefix = docId.split("-")[0];
							return docTypes.contains(prefix);
						}))
				.map(RequirementDto::fromEntity)
				.toList();
	}

	// 요구사항 버전 조회
	public List<RequirementRevisionDto> getRequirementRevisions(Long projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ProjectException("존재하지 않는 프로젝트입니다.", HttpStatus.NOT_FOUND));

		Integer maxRevision = requirementRepository.getMaxRevisionCount(project);

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
}
