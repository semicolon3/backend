package com.legalai.controller;

import com.legalai.common.ApiResponse;
import com.legalai.dto.request.GenerateDocumentRequest;
import com.legalai.dto.request.UpdateGeneratedDocumentRequest;
import com.legalai.dto.response.GeneratedDocumentResponse;
import com.legalai.security.UserPrincipal;
import com.legalai.service.GeneratedDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Generated Documents", description = "자동 문서 생성 API")
@RestController
@RequestMapping("/api/generated-documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class GeneratedDocumentController {

    private final GeneratedDocumentService generatedDocumentService;

    @Operation(summary = "문서 자동 생성", description = "템플릿 선택 후 필드 값 입력으로 문서 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<GeneratedDocumentResponse>> generate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateDocumentRequest request) {
        GeneratedDocumentResponse response = generatedDocumentService.generate(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("문서가 생성되었습니다.", response));
    }

    @Operation(summary = "생성된 문서 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<GeneratedDocumentResponse>>> getGeneratedDocuments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(generatedDocumentService.getGeneratedDocuments(principal.getUserId())));
    }

    @Operation(summary = "생성된 문서 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GeneratedDocumentResponse>> getGeneratedDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(generatedDocumentService.getGeneratedDocument(id, principal.getUserId())));
    }

    @Operation(summary = "문서 수정", description = "내용 수정 시 version 자동 증가")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<GeneratedDocumentResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateGeneratedDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("문서가 수정되었습니다.",
                generatedDocumentService.update(id, request, principal.getUserId())));
    }

    @Operation(summary = "PDF 다운로드")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        byte[] pdfBytes = generatedDocumentService.generatePdf(id, principal.getUserId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "document_" + id + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
