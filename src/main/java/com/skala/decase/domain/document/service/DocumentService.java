package com.skala.decase.domain.document.service;

import com.skala.decase.domain.document.controller.dto.DocumentDetailResponse;
import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.document.controller.dto.DocumentResponse;
import com.skala.decase.domain.document.exception.DocumentException;
import com.skala.decase.domain.document.repository.DocumentRepository;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.exception.MemberException;
import com.skala.decase.domain.member.repository.MemberRepository;
import com.skala.decase.domain.project.domain.Project;

import com.skala.decase.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
	private final ProjectRepository projectRepository;

	// 로컬 파일 업로드 경로
    private final static String BASE_UPLOAD_PATH = "DECASE/upload";

    // 문서 타입 매핑
    private static final Map<Integer, String> TYPE_PREFIX_MAP = Map.of(
            1, "RFP",
            2, "MOMV",
            3, "MOMD",
            4, "EXTRA",
            5, "REQ",
            6, "QFS",
            7, "MATRIX"
    );
    private final MemberRepository memberRepository;

    // Doc ID 찾기
    public String generateDocId(String typePrefix) {
        Optional<String> latestIdOpt = documentRepository.findLatestDocIdByPrefix(typePrefix);
        int nextNumber = 1;

        if (latestIdOpt.isPresent()) {
            String latestId = latestIdOpt.get();  // e.g. "RFP-000123"
            String[] parts = latestId.split("-");
            try {
                nextNumber = Integer.parseInt(parts[1]) + 1;
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid docId format: " + latestId);
            }
        }

        return String.format("%s-%06d", typePrefix, nextNumber);
    }

    // 사용자 업로드
    public List<DocumentResponse> uploadDocuments(Long projectId, Long memberId, List<MultipartFile> files, List<Integer> types) throws IOException {
        if (files.size() != types.size()) {
			throw new DocumentException("파일 수와 타입 수가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        List<DocumentResponse> responses = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            int iType = types.get(i);

            if (iType < 1 || iType > 7) {
                throw new DocumentException("유효하지 않은 문서 타입: " + iType, HttpStatus.BAD_REQUEST);
            }

			Project project = projectRepository.findById(projectId)
					.orElseThrow(() -> new DocumentException("유효하지 않은 프로젝트 ID: " + projectId, HttpStatus.NOT_FOUND));
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new MemberException("유효하지 않은 사용자 ID: " + memberId, HttpStatus.NOT_FOUND));
            // 파일 저장
            String fileName = System.currentTimeMillis() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
            Path uploadPath = Paths.get(BASE_UPLOAD_PATH);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Document 저장
            Document doc = new Document();
            doc.setDocId(generateDocId(TYPE_PREFIX_MAP.get(iType)));
            doc.setName(file.getOriginalFilename());
            doc.setPath(filePath.toString());
            doc.setCreatedDate(LocalDateTime.now());
            doc.setMemberUpload(true);
            doc.setCreatedBy(member);
            doc.setProject(project);

            documentRepository.save(doc);

            responses.add(new DocumentResponse(doc.getDocId(), doc.getName()));
        }

        return responses;
    }

    // 사용자 업로드 파일 삭제
    // 💡 삭제 왜 해야대..?
    // public void deleteDocument(Long docId) {
    //     documentRepository.deleteById(docId);
    // }

    // 사용자 업로드 파일 다운로드
    public ResponseEntity<byte[]> downloadDocument(String docId) throws IOException {
        Document doc = documentRepository.findById(docId)
				.orElseThrow(() -> new DocumentException("문서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Path path = Paths.get(doc.getPath());
        byte[] content = Files.readAllBytes(path);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getName() + "\"")
                .body(content);
    }

    // 파일 상세 정보 조회
    public ResponseEntity<DocumentDetailResponse> getDocument(String docId) throws IOException {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new DocumentException("문서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        DocumentDetailResponse docDetailResponse = DocumentDetailResponse.builder()
                .docId(doc.getDocId())
                .name(doc.getName())
                .createdDate(doc.getCreatedDate())
                .build();

        if (doc.isMemberUpload()) {
            docDetailResponse.setCreatedBy(doc.getCreatedBy().getName());
        } else {
            docDetailResponse.setCreatedBy("DECASE");
        }
        return new ResponseEntity<>(docDetailResponse, HttpStatus.OK);
    }
}