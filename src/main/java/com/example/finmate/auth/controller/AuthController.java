package com.example.finmate.auth.controller;

import com.example.finmate.auth.dto.PasswordResetRequestDTO;
import com.example.finmate.auth.dto.PasswordResetDTO;
import com.example.finmate.auth.dto.EmailVerificationDTO;
import com.example.finmate.auth.service.AuthService;
import com.example.finmate.common.service.EmailService;
import com.example.finmate.common.util.IPUtils;
import com.example.finmate.common.util.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @ApiOperation(value = "로그아웃", notes = "현재 로그인한 사용자를 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            Authentication authentication,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);

        if (authentication != null) {
            String userId = authentication.getName();
            log.info("로그아웃: {} from {}", userId, IPUtils.maskIP(clientIP));

            // 보안 이벤트 기록
            authService.recordSecurityEvent(userId, "LOGOUT", clientIP);

            // 세션 삭제
            authService.deleteSession(userId);
        }

        // JWT 토큰 기반이므로 서버에서는 단순히 클라이언트에게 토큰 삭제를 알림
        SecurityContextHolder.clearContext();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그아웃되었습니다.");

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "토큰 유효성 확인", notes = "현재 토큰의 유효성을 확인합니다.")
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()) {
            response.put("success", true);
            response.put("valid", true);
            response.put("userId", authentication.getName());
            response.put("authorities", authentication.getAuthorities());
        } else {
            response.put("success", true);
            response.put("valid", false);
            response.put("message", "토큰이 유효하지 않습니다.");
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "비밀번호 재설정 요청", notes = "이메일로 비밀번호 재설정 링크를 발송합니다.")
    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO requestDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("비밀번호 재설정 요청: {} from {}", requestDTO.getUserEmail(), IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

        try {
            String resetToken = authService.generatePasswordResetToken(requestDTO.getUserEmail());
            boolean emailSent = emailService.sendPasswordResetEmail(requestDTO.getUserEmail(), resetToken);

            if (emailSent) {
                response.put("success", true);
                response.put("message", "비밀번호 재설정 링크가 이메일로 발송되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "비밀번호 재설정", notes = "토큰을 사용하여 비밀번호를 재설정합니다.")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Map<String, Object>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetDTO resetDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("비밀번호 재설정 확인: {} from {}", resetDTO.getToken(), IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = authService.resetPassword(resetDTO.getToken(), resetDTO.getNewPassword());

            if (success) {
                response.put("success", true);
                response.put("message", "비밀번호가 성공적으로 재설정되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "비밀번호 재설정에 실패했습니다.");
            }

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "이메일 인증 요청", notes = "회원가입 후 이메일 인증을 요청합니다.")
    @PostMapping("/email-verification/request")
    public ResponseEntity<Map<String, Object>> requestEmailVerification(
            @RequestParam String userEmail,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("이메일 인증 요청: {} from {}", userEmail, IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

        try {
            String verificationToken = authService.generateEmailVerificationToken(userEmail);
            boolean emailSent = emailService.sendActivationEmail(userEmail, verificationToken);

            if (emailSent) {
                response.put("success", true);
                response.put("message", "인증 링크가 이메일로 발송되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "이메일 발송에 실패했습니다.");
            }

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "이메일 인증 확인", notes = "이메일 인증 토큰을 확인합니다.")
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Map<String, Object>> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationDTO verificationDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("이메일 인증 확인: {} from {}", verificationDTO.getToken(), IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = authService.verifyEmail(verificationDTO.getToken());

            if (success) {
                response.put("success", true);
                response.put("message", "이메일 인증이 완료되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "이메일 인증에 실패했습니다.");
            }

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "계정 잠금 해제", notes = "관리자가 잠긴 계정을 해제합니다.")
    @PostMapping("/unlock-account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> unlockAccount(
            @RequestParam String userId,
            Authentication authentication,
            HttpServletRequest request) {

        String currentUser = SecurityUtils.getCurrentUserId();
        String clientIP = IPUtils.getClientIP(request);
        log.info("계정 잠금 해제 요청: {} by {} from {}", userId, currentUser, IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = authService.unlockAccount(userId);

            if (success) {
                // 관리자 활동 기록
                authService.recordSecurityEvent(currentUser, "ACCOUNT_UNLOCKED_" + userId, clientIP);

                response.put("success", true);
                response.put("message", "계정 잠금이 해제되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "계정 잠금 해제에 실패했습니다.");
            }

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "로그인 이력 조회", notes = "사용자의 로그인 이력을 조회합니다.")
    @GetMapping("/login-history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getLoginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("로그인 이력 조회: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> loginHistory = authService.getLoginHistory(userId, page, size);

            response.put("success", true);
            response.put("data", loginHistory);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "로그인 이력 조회에 실패했습니다.");
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "현재 세션 정보", notes = "현재 사용자의 세션 정보를 조회합니다.")
    @GetMapping("/session-info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getSessionInfo(
            Authentication authentication,
            HttpServletRequest request) {

        String userId = SecurityUtils.getCurrentUserId();
        String clientIP = IPUtils.getClientIP(request);

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> sessionInfo = new HashMap<>();

        sessionInfo.put("userId", userId);
        sessionInfo.put("authorities", authentication.getAuthorities());
        sessionInfo.put("loginTime", System.currentTimeMillis());
        sessionInfo.put("isAuthenticated", SecurityUtils.isAuthenticated());
        sessionInfo.put("clientIP", IPUtils.maskIP(clientIP));
        sessionInfo.put("userAgent", request.getHeader("User-Agent"));

        response.put("success", true);
        response.put("data", sessionInfo);

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "보안 통계", notes = "사용자의 보안 관련 통계를 조회합니다.")
    @GetMapping("/security-statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getSecurityStatistics(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("보안 통계 조회: {} ({}일)", userId, days);

        Map<String, Object> response = new HashMap<>();

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

            response.put("success", true);
            response.put("data", statistics);

        } catch (Exception e) {
            log.error("보안 통계 조회 실패", e);
            response.put("success", false);
            response.put("message", "보안 통계 조회에 실패했습니다.");
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "인증 서비스 상태 확인", notes = "인증 서비스의 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> authHealthCheck() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 인증 서비스 상태 확인
            response.put("success", true);
            response.put("status", "OK");
            response.put("message", "인증 서비스가 정상 동작 중입니다.");
            response.put("timestamp", System.currentTimeMillis());
            response.put("service", "AuthService");

        } catch (Exception e) {
            log.error("인증 서비스 헬스 체크 실패", e);
            response.put("success", false);
            response.put("status", "ERROR");
            response.put("message", "인증 서비스 상태 확인에 실패했습니다.");
        }

        return ResponseEntity.ok(response);
    }
}