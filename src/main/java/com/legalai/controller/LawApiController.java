package com.legalai.controller;

import com.legalai.service.LawApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/law")
@RequiredArgsConstructor
@Tag(name = "법령 API", description = "국가법령정보 API 연동")
public class LawApiController {

    private final LawApiService lawApiService;

    @GetMapping("/search")
    @Operation(summary = "법령 검색", description = "키워드로 법령을 검색합니다")
    public ResponseEntity<String> searchLaw(@RequestParam String query) {
        return ResponseEntity.ok(lawApiService.searchLaw(query));
    }

    @GetMapping("/detail")
    @Operation(summary = "법령 상세 조회", description = "법령 ID로 상세 내용을 조회합니다")
    public ResponseEntity<String> getLawDetail(@RequestParam String lawId) {
        return ResponseEntity.ok(lawApiService.getLawDetail(lawId));
    }
}