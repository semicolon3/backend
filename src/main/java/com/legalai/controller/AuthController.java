package com.legalai.controller;

import com.legalai.common.ApiResponse;
import com.legalai.dto.request.*;
import com.legalai.dto.response.TokenResponse;
import com.legalai.security.JwtTokenProvider;
import com.legalai.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(@Valid @RequestBody SignupRequest request) {
        TokenResponse tokens = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("회원가입이 완료되었습니다.", tokens));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("로그인이 완료되었습니다.", tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokens = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.substring(7);
        Long userId = jwtTokenProvider.getUserId(token);
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다."));
    }

    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<String>> findEmail(@Valid @RequestBody FindEmailRequest request) {
        String maskedEmail = authService.findEmail(request);
        return ResponseEntity.ok(ApiResponse.ok("이메일 찾기 성공", maskedEmail));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 재설정되었습니다."));
    }
}
