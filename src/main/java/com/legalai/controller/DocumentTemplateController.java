package com.legalai.controller;

import com.legalai.common.ApiResponse;
import com.legalai.dto.response.DocumentTemplateResponse;
import com.legalai.service.GeneratedDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Document Templates", description = "문서 템플릿 API")
@RestController
@RequestMapping("/api/document-templates")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class DocumentTemplateController {

    private final GeneratedDocumentService generatedDocumentService;

    @Operation(summary = "템플릿 목록 조회", description = "내용증명/합의서 등 사용 가능한 템플릿 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentTemplateResponse>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.ok(generatedDocumentService.getTemplates()));
    }
}
