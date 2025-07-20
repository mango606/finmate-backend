package com.example.finmate.common.exception;

import com.example.finmate.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 계정 잠금 예외 처리
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLockedException(AccountLockedException e) {
        log.warn("계정 잠금 예외: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage(), "ACCOUNT_LOCKED"));
    }

    // 일반적인 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("예상치 못한 오류 발생", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
    }

    // 회원을 찾을 수 없는 경우
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberNotFoundException(MemberNotFoundException e) {
        log.warn("회원을 찾을 수 없음: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage(), "MEMBER_NOT_FOUND"));
    }

    // 중복 리소스 예외 처리
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException e) {
        log.warn("중복 리소스: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage(), "DUPLICATE_RESOURCE"));
    }

    // 인증 실패 예외 처리
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailedException(AuthenticationFailedException e) {
        log.warn("인증 실패: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage(), "AUTHENTICATION_FAILED"));
    }

    // 요소를 찾을 수 없는 경우
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoSuchElementException(NoSuchElementException e) {
        log.warn("요소를 찾을 수 없음: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("요청한 리소스를 찾을 수 없습니다.", "RESOURCE_NOT_FOUND"));
    }

    // Bean Validation 오류 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("유효성 검증 실패: {}", e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });

        // 첫 번째 오류 메시지를 주 메시지로 사용
        String mainMessage = fieldErrors.isEmpty() ? "입력값이 올바르지 않습니다."
                : fieldErrors.values().iterator().next();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message(mainMessage)
                        .data(fieldErrors)
                        .errorCode("VALIDATION_ERROR")
                        .timestamp(System.currentTimeMillis())
                        .traceId(java.util.UUID.randomUUID().toString())
                        .build());
    }

    // Bind Exception 처리
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException e) {
        log.warn("바인딩 오류: {}", e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("입력값 형식이 올바르지 않습니다.")
                        .data(fieldErrors)
                        .errorCode("BINDING_ERROR")
                        .timestamp(System.currentTimeMillis())
                        .traceId(java.util.UUID.randomUUID().toString())
                        .build());
    }

    // Constraint Violation Exception 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("제약 조건 위반: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("입력값이 제약 조건을 위반했습니다.", "CONSTRAINT_VIOLATION"));
    }

    // 잘못된 인수 예외 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 인수: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), "INVALID_ARGUMENT"));
    }

    // 런타임 예외 처리
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("런타임 오류 발생", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("처리 중 오류가 발생했습니다.", "RUNTIME_ERROR"));
    }

    // 접근 거부 예외 처리
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        log.warn("접근 거부: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("접근 권한이 없습니다.", "ACCESS_DENIED"));
    }

    // 인증이 필요한 리소스에 접근한 경우
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(org.springframework.security.core.AuthenticationException e) {
        log.warn("인증 필요: {}", e.getMessage());

        if (e instanceof AccountLockedException) {
            return handleAccountLockedException((AccountLockedException) e);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("인증이 필요합니다.", "AUTHENTICATION_REQUIRED"));
    }

    // HTTP 메서드가 지원되지 않는 경우
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        log.warn("지원되지 않는 HTTP 메서드: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("지원되지 않는 HTTP 메서드입니다.", "METHOD_NOT_ALLOWED"));
    }

    // 미디어 타입이 지원되지 않는 경우
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupportedException(org.springframework.web.HttpMediaTypeNotSupportedException e) {
        log.warn("지원되지 않는 미디어 타입: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("지원되지 않는 미디어 타입입니다.", "UNSUPPORTED_MEDIA_TYPE"));
    }

    // 파일 업로드 크기 초과
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        log.warn("파일 크기 초과: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("업로드 파일 크기가 너무 큽니다.", "FILE_SIZE_EXCEEDED"));
    }

    // SQL 관련 예외 처리
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException e) {
        log.error("데이터 무결성 위반: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("데이터 무결성 제약 조건을 위반했습니다.", "DATA_INTEGRITY_VIOLATION"));
    }

    // 데이터베이스 접근 예외 처리
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(org.springframework.dao.DataAccessException e) {
        log.error("데이터베이스 접근 오류: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("데이터베이스 접근 중 오류가 발생했습니다.", "DATABASE_ACCESS_ERROR"));
    }

    // 트랜잭션 예외 처리
    @ExceptionHandler(org.springframework.transaction.TransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionException(org.springframework.transaction.TransactionException e) {
        log.error("트랜잭션 오류: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("트랜잭션 처리 중 오류가 발생했습니다.", "TRANSACTION_ERROR"));
    }

    // JSON 파싱 예외 처리
    @ExceptionHandler(com.fasterxml.jackson.core.JsonProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleJsonProcessingException(com.fasterxml.jackson.core.JsonProcessingException e) {
        log.warn("JSON 파싱 오류: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("JSON 형식이 올바르지 않습니다.", "JSON_PARSING_ERROR"));
    }

    // JWT 관련 예외 처리
    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(io.jsonwebtoken.JwtException e) {
        log.warn("JWT 오류: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("토큰이 유효하지 않습니다.", "INVALID_TOKEN"));
    }

    // 타임아웃 예외 처리
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleTimeoutException(java.util.concurrent.TimeoutException e) {
        log.error("요청 타임아웃: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(ApiResponse.error("요청 처리 시간이 초과되었습니다.", "REQUEST_TIMEOUT"));
    }
}