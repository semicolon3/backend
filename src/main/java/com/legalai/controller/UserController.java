package com.legalai.controller;

import com.legalai.common.ApiResponse;
import com.legalai.dto.request.ChangePasswordRequest;
import com.legalai.dto.request.UpdateUserRequest;
import com.legalai.dto.response.UserResponse;
import com.legalai.security.JwtTokenProvider;
import com.legalai.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@RequestHeader("Authorization") String bearerToken) {
        Long userId = extractUserId(bearerToken);
        return ResponseEntity.ok(ApiResponse.ok(userService.getMe(userId)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(@RequestHeader("Authorization") String bearerToken,
                                                               @RequestBody UpdateUserRequest request) {
        Long userId = extractUserId(bearerToken);
        return ResponseEntity.ok(ApiResponse.ok("정보가 수정되었습니다.", userService.updateMe(userId, request)));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestHeader("Authorization") String bearerToken,
                                                             @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = extractUserId(bearerToken);
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다."));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe(@RequestHeader("Authorization") String bearerToken) {
        Long userId = extractUserId(bearerToken);
        userService.deleteMe(userId);
        return ResponseEntity.ok(ApiResponse.ok("회원 탈퇴가 완료되었습니다."));
    }

    private Long extractUserId(String bearerToken) {
        return jwtTokenProvider.getUserId(bearerToken.substring(7));
    }
}
