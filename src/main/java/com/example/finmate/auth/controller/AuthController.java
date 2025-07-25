package com.example.finmate.auth.controller;

import com.example.finmate.auth.dto.EmailVerificationDTO;
import com.example.finmate.auth.dto.PasswordResetDTO;
import com.example.finmate.auth.dto.PasswordResetRequestDTO;
import com.example.finmate.auth.service.AuthService;
import com.example.finmate.common.dto.ApiResponse;
import com.example.finmate.common.service.EmailService;
import com.example.finmate.common.util.IPUtils;
import com.example.finmate.common.util.SecurityUtils;
import com.example.finmate.security.util.JwtProcessor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Api(tags = "인증 관리 API")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final JwtProcessor jwtProcessor;


    @ApiOperation(value = "토큰 유효성 확인", notes = "현재 토큰의 유효성을 확인합니다.")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyToken(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()) {
            result.put("valid", true);
            result.put("userId", authentication.getName());
            result.put("authorities", authentication.getAuthorities());
        } else {
            result.put("valid", false);
            result.put("message", "토큰이 유효하지 않습니다.");
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ApiOperation(value = "비밀번호 재설정 요청", notes = "이메일로 비밀번호 재설정 링크를 발송합니다.")
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO requestDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("비밀번호 재설정 요청: {} from {}", requestDTO.getUserEmail(), IPUtils.maskIP(clientIP));

        try {
            String resetToken = authService.generatePasswordResetToken(requestDTO.getUserEmail());
            boolean emailSent = emailService.sendPasswordResetEmail(requestDTO.getUserEmail(), resetToken);

            if (emailSent) {
                return ResponseEntity.ok(ApiResponse.success("비밀번호 재설정 링크가 이메일로 발송되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", "EMAIL_SEND_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        }
    }

    @ApiOperation(value = "비밀번호 재설정", notes = "토큰을 사용하여 비밀번호를 재설정합니다.")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetDTO resetDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("비밀번호 재설정 확인: {} from {}", resetDTO.getToken(), IPUtils.maskIP(clientIP));

        try {
            boolean success = authService.resetPassword(resetDTO.getToken(), resetDTO.getNewPassword());

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("비밀번호가 성공적으로 재설정되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("비밀번호 재설정에 실패했습니다.", "RESET_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        }
    }

    @ApiOperation(value = "이메일 인증 요청", notes = "회원가입 후 이메일 인증을 요청합니다.")
    @PostMapping("/email-verification/request")
    public ResponseEntity<ApiResponse<Void>> requestEmailVerification(
            @RequestParam String userEmail,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("이메일 인증 요청: {} from {}", userEmail, IPUtils.maskIP(clientIP));

        try {
            String verificationToken = authService.generateEmailVerificationToken(userEmail);
            boolean emailSent = emailService.sendActivationEmail(userEmail, verificationToken);

            if (emailSent) {
                return ResponseEntity.ok(ApiResponse.success("인증 링크가 이메일로 발송되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("이메일 발송에 실패했습니다.", "EMAIL_SEND_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        }
    }

    @ApiOperation(value = "이메일 인증 확인", notes = "이메일 인증 토큰을 확인합니다.")
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationDTO verificationDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("이메일 인증 확인: {} from {}", verificationDTO.getToken(), IPUtils.maskIP(clientIP));

        try {
            boolean success = authService.verifyEmail(verificationDTO.getToken());

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("이메일 인증에 실패했습니다.", "VERIFICATION_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        }
    }

    @ApiOperation(value = "계정 잠금 해제", notes = "관리자가 잠긴 계정을 해제합니다.")
    @PostMapping("/unlock-account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(
            @RequestParam String userId,
            Authentication authentication,
            HttpServletRequest request) {

        String currentUser = SecurityUtils.getCurrentUserId();
        String clientIP = IPUtils.getClientIP(request);
        log.info("계정 잠금 해제 요청: {} by {} from {}", userId, currentUser, IPUtils.maskIP(clientIP));

        try {
            boolean success = authService.unlockAccount(userId);

            if (success) {
                // 관리자 활동 기록
                authService.recordSecurityEvent(currentUser, "ACCOUNT_UNLOCKED_" + userId, clientIP);
                return ResponseEntity.ok(ApiResponse.success("계정 잠금이 해제되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("계정 잠금 해제에 실패했습니다.", "UNLOCK_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        }
    }

    @ApiOperation(value = "로그인 이력 조회", notes = "사용자의 로그인 이력을 조회합니다.")
    @GetMapping("/login-history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("로그인 이력 조회: {}", userId);

        try {
            Map<String, Object> loginHistory = authService.getLoginHistory(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success(loginHistory));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("로그인 이력 조회에 실패했습니다.", "HISTORY_READ_ERROR"));
        }
    }

    @ApiOperation(value = "현재 세션 정보", notes = "현재 사용자의 세션 정보를 조회합니다.")
    @GetMapping("/session-info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionInfo(
            Authentication authentication,
            HttpServletRequest request) {

        String userId = SecurityUtils.getCurrentUserId();
        String clientIP = IPUtils.getClientIP(request);

        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("userId", userId);
        sessionInfo.put("authorities", authentication.getAuthorities());
        sessionInfo.put("loginTime", System.currentTimeMillis());
        sessionInfo.put("isAuthenticated", SecurityUtils.isAuthenticated());
        sessionInfo.put("clientIP", IPUtils.maskIP(clientIP));
        sessionInfo.put("userAgent", request.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResponse.success(sessionInfo));
    }

    @ApiOperation(value = "보안 통계", notes = "사용자의 보안 관련 통계를 조회합니다.")
    @GetMapping("/security-statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityStatistics(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("보안 통계 조회: {} ({}일)", userId, days);

        try {
            Map<String, Object> statistics = new HashMap<>();

            // 로그인 이력 요약
            Map<String, Object> loginHistory = authService.getLoginHistory(userId, 0, days);
            statistics.put("loginHistory", loginHistory);

            // 보안 점검 결과
            Map<String, Object> securityCheck = authService.performSecurityCheck(userId);
            statistics.put("securityCheck", securityCheck);

            // 계정 보안 정보
            Map<String, Object> accountSecurity = authService.getAccountSecurity(userId);
            statistics.put("accountSecurity", accountSecurity);

            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            log.error("보안 통계 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("보안 통계 조회에 실패했습니다.", "STATISTICS_ERROR"));
        }
    }

    @ApiOperation(value = "인증 서비스 상태 확인", notes = "인증 서비스의 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> authHealthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();

        try {
            // 인증 서비스 상태 확인
            healthInfo.put("status", "OK");
            healthInfo.put("message", "인증 서비스가 정상 동작 중입니다.");
            healthInfo.put("timestamp", System.currentTimeMillis());
            healthInfo.put("service", "AuthService");

            return ResponseEntity.ok(ApiResponse.success(healthInfo));

        } catch (Exception e) {
            log.error("인증 서비스 헬스 체크 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("인증 서비스 상태 확인에 실패했습니다.", "HEALTH_CHECK_FAILED"));
        }
    }
}