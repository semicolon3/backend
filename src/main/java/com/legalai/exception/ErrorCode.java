package com.legalai.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 존재하지 않습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    USER_DELETED(HttpStatus.GONE, "탈퇴한 계정입니다."),

    // Document
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."),
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 문서에 접근 권한이 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    ANALYSIS_NOT_READY(HttpStatus.ACCEPTED, "분석이 아직 완료되지 않았습니다."),

    // Generated Document
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다."),
    GENERATED_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "생성된 문서를 찾을 수 없습니다."),
    GENERATED_DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 문서에 접근 권한이 없습니다."),
    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다."),

    // AI
    AI_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석에 실패했습니다."),
    AI_CHAT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 답변 생성에 실패했습니다."),

    // Conversation
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "대화방을 찾을 수 없습니다."),
    CONVERSATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 대화방에 접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
