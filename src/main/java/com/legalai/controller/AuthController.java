package com.legalai.controller;

import com.legalai.common.ApiResponse;
import com.legalai.dto.request.*;
import com.legalai.dto.response.TokenResponse;
import com.legalai.security.UserPrincipal;
import com.legalai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일/비밀번호로 즉시 가입 후 JWT 발급")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("회원가입이 완료되었습니다.", authService.signup(request)));
    }

    @Operation(summary = "로그인", description = "이메일+비밀번호 로그인 → Access/Refresh Token 발급")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("로그인이 완료되었습니다.", authService.login(request)));
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access/Refresh Token 발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @Operation(summary = "로그아웃", description = "Refresh Token 무효화", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다."));
    }

    @Operation(summary = "이메일 찾기", description = "이름+전화번호로 가입된 이메일 조회 (마스킹 처리)")
    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<String>> findEmail(@Valid @RequestBody FindEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("이메일 찾기 성공", authService.findEmail(request)));
    }

    @Operation(summary = "비밀번호 재설정")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 재설정되었습니다."));
    }
}
