package com.skala.decase.domain.document.service;

import com.skala.decase.domain.document.controller.dto.DocumentDetailResponse;
import com.skala.decase.domain.document.controller.dto.DocumentResponse;
import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.document.exception.DocumentException;
import com.skala.decase.domain.document.repository.DocumentRepository;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.exception.MemberException;
import com.skala.decase.domain.member.repository.MemberRepository;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.exception.ProjectException;
import com.skala.decase.domain.project.repository.ProjectRepository;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
            7, "MATRIX",
            8, "ASIS"
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
    public List<DocumentResponse> uploadDocuments(Long projectId, Long memberId, List<MultipartFile> files,
                                                  List<Integer> types) throws IOException {
        if (files.size() != types.size()) {
            throw new DocumentException("파일 수와 타입 수가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        List<DocumentResponse> responses = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            int iType = types.get(i);

            if (iType < 1 || iType > 8) {
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

    /**
     * 사용자 업로드 파일 다운로드
     */
    public ResponseEntity<Resource> downloadDocument(String docId) throws IOException {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new DocumentException("문서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Path path = Paths.get(doc.getPath());

        if (!Files.exists(path)) {
            throw new DocumentException("파일이 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }

        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new DocumentException("파일을 읽을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 파일명 인코딩 (한글 파일명 지원)
        String encodedFilename = URLEncoder.encode(doc.getName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        String contentType = determineContentType(doc.getName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.contentLength()))
                .body(resource);
    }

    private String determineContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }

        String extension = filename.toLowerCase();

        if (extension.endsWith(".pdf")) {
            return "application/pdf";
        } else if (extension.endsWith(".doc")) {
            return "application/msword";
        } else if (extension.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (extension.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (extension.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (extension.endsWith(".csv")) {
            return "text/csv; charset=UTF-8";
        } else if (extension.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        } else {
            return "application/octet-stream";
        }
    }

    // 파일 상세 정보 조회
    public ResponseEntity<DocumentDetailResponse> getDocumentDetails(String docId) {
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

    public ResponseEntity<List<DocumentResponse>> getDocumentUploads(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectException("프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 해당 프로젝트의 모든 문서 조회 (사용자 업로드 문서만 필터링)
        List<Document> documents = documentRepository.findAllByProjectAndIsMemberUploadTrue(project);

        List<DocumentResponse> responseList = documents.stream()
                .map(doc -> new DocumentResponse(doc.getDocId(), doc.getName()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

}