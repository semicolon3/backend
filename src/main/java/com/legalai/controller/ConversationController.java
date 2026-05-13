package com.legalai.controller;

import com.legalai.common.ApiResponse;
import com.legalai.dto.request.CreateConversationRequest;
import com.legalai.dto.request.SendMessageRequest;
import com.legalai.dto.request.UpdateConversationRequest;
import com.legalai.dto.response.ConversationDetailResponse;
import com.legalai.dto.response.ConversationResponse;
import com.legalai.dto.response.MessageResponse;
import com.legalai.security.UserPrincipal;
import com.legalai.service.ConversationService;
import com.legalai.service.RagChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Conversations", description = "RAG 기반 법률 챗봇 — 대화방 + 메시지 + AI 답변")
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ConversationController {

    private final ConversationService conversationService;
    private final RagChatService ragChatService;

    @Operation(summary = "내 대화방 목록 (페이징)",
            description = "최근 업데이트 순. page는 0부터, size 기본 20")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(conversationService.list(principal.getUserId(), pageable)));
    }

    @Operation(summary = "새 대화방 시작",
            description = "법률 도메인을 선택해 대화방 생성. 제목 미지정 시 도메인 기반 기본 제목.")
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse response = conversationService.create(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("대화방이 생성되었습니다.", response));
    }

    @Operation(summary = "대화방 상세 (메시지 포함)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationDetailResponse>> getDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(conversationService.getDetail(id, principal.getUserId())));
    }

    @Operation(summary = "대화방 수정", description = "제목 변경 / 보관 처리")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateConversationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("대화방이 수정되었습니다.",
                conversationService.update(id, request, principal.getUserId())));
    }

    @Operation(summary = "대화방 삭제 (소프트 딜리트)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        conversationService.delete(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("대화방이 삭제되었습니다."));
    }

    @Operation(summary = "메시지 전송 → RAG 답변 ★",
            description = "사용자 메시지를 저장한 뒤 국가법령정보 검색 + OpenAI로 답변 생성. " +
                    "응답은 AI 메시지(인용 법령 포함). 호출 시간 5~15초.")
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = ragChatService.sendMessage(id, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("답변이 생성되었습니다.", response));
    }

    @Operation(summary = "대화방 메시지 목록", description = "시간 순. 인용 정보 포함.")
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(conversationService.getMessages(id, principal.getUserId())));
    }
}
