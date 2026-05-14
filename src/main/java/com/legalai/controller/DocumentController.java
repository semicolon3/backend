package com.legalai.controller;

import com.legalai.common.ApiResponse;
import com.legalai.domain.DocumentType;
import com.legalai.dto.response.DocumentAnalysisResponse;
import com.legalai.dto.response.DocumentResponse;
import com.legalai.security.UserPrincipal;
import com.legalai.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Documents", description = "문서 업로드/분석 API")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "문서 업로드", description = "계약서/카카오톡/영수증 파일 업로드 후 자동 분석 시작")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "OTHER") DocumentType documentType) {
        DocumentResponse response = documentService.upload(file, documentType, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("문서가 업로드되었습니다.", response));
    }

    @Operation(summary = "문서 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getDocuments(principal.getUserId())));
    }

    @Operation(summary = "문서 메타 + OCR 상태 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getDocument(id, principal.getUserId())));
    }

    @Operation(summary = "분석 결과 조회", description = "요약 + 엔티티 + 위험 조항 반환. 분석 중이면 status만 반환")
    @GetMapping("/{id}/analysis")
    public ResponseEntity<ApiResponse<DocumentAnalysisResponse>> getAnalysis(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getAnalysis(id, principal.getUserId())));
    }

    @Operation(summary = "문서 썸네일 조회", description = "PDF는 첫 페이지를 PNG로 변환, 이미지는 그대로 반환 (인증 불필요)")
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id) {
        DocumentService.ThumbnailResult result = documentService.getThumbnail(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.contentType()));
        return ResponseEntity.ok().headers(headers).body(result.data());
    }

    @Operation(summary = "문서 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        documentService.delete(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("문서가 삭제되었습니다."));
    }
}
