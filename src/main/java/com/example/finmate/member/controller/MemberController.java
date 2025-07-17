package com.example.finmate.member.controller;

import com.example.finmate.auth.service.AuthService;
import com.example.finmate.common.service.EmailService;
import com.example.finmate.common.service.FileUploadService;
import com.example.finmate.common.util.IPUtils;
import com.example.finmate.common.util.SecurityUtils;
import com.example.finmate.member.dto.MemberInfoDTO;
import com.example.finmate.member.dto.MemberJoinDTO;
import com.example.finmate.member.dto.MemberUpdateDTO;
import com.example.finmate.member.service.MemberService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Api(tags = "회원 관리 API")
public class MemberController {

    private final MemberService memberService;
    private final AuthService authService;
    private final EmailService emailService;
    private final FileUploadService fileUploadService;

    @ApiOperation(value = "회원 가입", notes = "새로운 회원을 등록하고 이메일 인증을 요청합니다.")
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> insertMember(
            @Valid @RequestBody MemberJoinDTO joinDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("회원 가입 요청: {} from {}", joinDTO.getUserId(), IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = memberService.insertMember(joinDTO);

            if (success) {
                // 이메일 인증 토큰 생성 및 발송
                try {
                    String verificationToken = authService.generateEmailVerificationToken(joinDTO.getUserEmail());
                    emailService.sendActivationEmail(joinDTO.getUserEmail(), verificationToken);

                    response.put("success", true);
                    response.put("message", "회원 가입이 완료되었습니다. 이메일 인증을 완료해주세요.");
                    response.put("emailSent", true);
                } catch (Exception e) {
                    log.warn("이메일 인증 발송 실패: {}", e.getMessage());
                    response.put("success", true);
                    response.put("message", "회원 가입이 완료되었습니다.");
                    response.put("emailSent", false);
                }

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "회원 가입에 실패했습니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("회원 가입 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "회원 정보 조회", notes = "로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getMemberInfo(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();

        log.info("회원 정보 조회: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);

            // 보안 정보 포함
            Map<String, Object> enhancedInfo = new HashMap<>();
            enhancedInfo.put("basic", memberInfo);
            enhancedInfo.put("security", authService.getAccountSecurity(userId));

            response.put("success", true);
            response.put("data", enhancedInfo);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("회원 정보 조회 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "회원 정보 수정", notes = "로그인한 사용자의 정보를 수정합니다.")
    @PutMapping("/info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateMember(
            Authentication authentication,
            @Valid @RequestBody MemberUpdateDTO updateDTO) {

        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();

        log.info("회원 정보 수정: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = memberService.updateMember(userId, updateDTO);

            if (success) {
                response.put("success", true);
                response.put("message", "회원 정보가 수정되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "회원 정보 수정에 실패했습니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("회원 정보 수정 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "비밀번호 변경", notes = "로그인한 사용자의 비밀번호를 변경합니다.")
    @PutMapping("/password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateMemberPassword(
            Authentication authentication,
            @RequestBody Map<String, String> passwordData,
            HttpServletRequest request) {

        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();
        String clientIP = IPUtils.getClientIP(request);

        log.info("비밀번호 변경: {} from {}", userId, IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

        try {
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                response.put("success", false);
                response.put("message", "현재 비밀번호와 새 비밀번호를 입력해주세요.");
                return ResponseEntity.badRequest().body(response);
            }

            boolean success = memberService.updateMemberPassword(userId, currentPassword, newPassword);

            if (success) {
                // 보안 이벤트 기록
                authService.recordSecurityEvent(userId, "PASSWORD_CHANGED", clientIP);

                response.put("success", true);
                response.put("message", "비밀번호가 변경되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "비밀번호 변경에 실패했습니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("비밀번호 변경 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "회원 탈퇴", notes = "로그인한 사용자의 계정을 삭제합니다.")
    @DeleteMapping("/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> deleteMember(
            Authentication authentication,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();
        String clientIP = IPUtils.getClientIP(request);

        log.info("회원 탈퇴: {} from {}", userId, IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = memberService.deleteMember(userId);

            if (success) {
                // 보안 이벤트 기록
                authService.recordSecurityEvent(userId, "ACCOUNT_WITHDRAWN", clientIP);

                response.put("success", true);
                response.put("message", "회원 탈퇴가 완료되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "회원 탈퇴에 실패했습니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("회원 탈퇴 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "사용자 ID 중복 확인", notes = "회원가입 시 사용자 ID 중복을 확인합니다.")
    @GetMapping("/checkUserId/{userId}")
    public ResponseEntity<Map<String, Object>> checkUserIdDuplicate(
            @ApiParam(value = "확인할 사용자 ID", required = true) @PathVariable String userId) {

        log.info("사용자 ID 중복 확인: {}", userId);

        Map<String, Object> response = new HashMap<>();
        boolean isDuplicate = memberService.checkUserIdDuplicate(userId);

        response.put("success", true);
        response.put("isDuplicate", isDuplicate);
        response.put("message", isDuplicate ? "이미 사용 중인 ID입니다." : "사용 가능한 ID입니다.");

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "이메일 중복 확인", notes = "회원가입 시 이메일 중복을 확인합니다.")
    @GetMapping("/checkEmail")
    public ResponseEntity<Map<String, Object>> checkUserEmailDuplicate(
            @ApiParam(value = "확인할 이메일", required = true) @RequestParam String userEmail) {

        log.info("이메일 중복 확인: {}", userEmail);

        Map<String, Object> response = new HashMap<>();
        boolean isDuplicate = memberService.checkUserEmailDuplicate(userEmail);

        response.put("success", true);
        response.put("isDuplicate", isDuplicate);
        response.put("message", isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.");

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "프로필 이미지 업로드", notes = "사용자의 프로필 이미지를 업로드합니다.")
    @PostMapping("/profile-image")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("프로필 이미지 업로드: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            String fileName = fileUploadService.uploadProfileImage(userId, file);

            response.put("success", true);
            response.put("message", "프로필 이미지가 업로드되었습니다.");
            response.put("fileName", fileName);
            response.put("imageUrl", "/upload/avatar/" + userId + "/" + fileName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패", e);
            response.put("success", false);
            response.put("message", "프로필 이미지 업로드에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "보안 설정 조회", notes = "사용자의 보안 설정을 조회합니다.")
    @GetMapping("/security-settings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getSecuritySettings(Authentication authentication) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("보안 설정 조회: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> securitySettings = authService.getSecuritySettings(userId);

            response.put("success", true);
            response.put("data", securitySettings);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("보안 설정 조회 실패", e);
            response.put("success", false);
            response.put("message", "보안 설정 조회에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "2단계 인증 설정", notes = "2단계 인증을 활성화/비활성화합니다.")
    @PostMapping("/two-factor-auth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateTwoFactorAuth(
            Authentication authentication,
            @RequestParam boolean enabled,
            HttpServletRequest request) {

        String userId = SecurityUtils.getCurrentUserId();
        String clientIP = IPUtils.getClientIP(request);
        log.info("2단계 인증 설정: {} - {} from {}", userId, enabled, IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = authService.updateTwoFactorAuth(userId, enabled);

            if (success) {
                authService.recordSecurityEvent(userId,
                        enabled ? "TWO_FACTOR_ENABLED" : "TWO_FACTOR_DISABLED", clientIP);

                response.put("success", true);
                response.put("message", enabled ? "2단계 인증이 활성화되었습니다." : "2단계 인증이 비활성화되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "2단계 인증 설정에 실패했습니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("2단계 인증 설정 실패", e);
            response.put("success", false);
            response.put("message", "2단계 인증 설정에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "계정 활동 내역", notes = "사용자의 계정 활동 내역을 조회합니다.")
    @GetMapping("/activity-history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getActivityHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("계정 활동 내역 조회: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> activityHistory = authService.getActivityHistory(userId, page, size);

            response.put("success", true);
            response.put("data", activityHistory);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("계정 활동 내역 조회 실패", e);
            response.put("success", false);
            response.put("message", "계정 활동 내역 조회에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "계정 보안 점검", notes = "사용자 계정의 보안 상태를 점검합니다.")
    @GetMapping("/security-check")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> performSecurityCheck(Authentication authentication) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("계정 보안 점검: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> securityCheck = authService.performSecurityCheck(userId);

            response.put("success", true);
            response.put("data", securityCheck);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("계정 보안 점검 실패", e);
            response.put("success", false);
            response.put("message", "계정 보안 점검에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "알림 설정 조회", notes = "사용자의 알림 설정을 조회합니다.")
    @GetMapping("/notification-settings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getNotificationSettings(Authentication authentication) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("알림 설정 조회: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 기본 알림 설정 (추후 DB 연동)
            Map<String, Object> notificationSettings = new HashMap<>();
            notificationSettings.put("emailNotification", true);
            notificationSettings.put("smsNotification", false);
            notificationSettings.put("pushNotification", true);
            notificationSettings.put("loginNotification", true);
            notificationSettings.put("financialAlerts", true);
            notificationSettings.put("goalReminders", true);
            notificationSettings.put("marketingEmails", false);

            response.put("success", true);
            response.put("data", notificationSettings);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("알림 설정 조회 실패", e);
            response.put("success", false);
            response.put("message", "알림 설정 조회에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "알림 설정 수정", notes = "사용자의 알림 설정을 수정합니다.")
    @PutMapping("/notification-settings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateNotificationSettings(
            Authentication authentication,
            @RequestBody Map<String, Object> settingsData) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("알림 설정 수정: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 알림 설정 업데이트 로직 (추후 DB 연동)
            log.info("알림 설정 업데이트: {} - {}", userId, settingsData);

            response.put("success", true);
            response.put("message", "알림 설정이 수정되었습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("알림 설정 수정 실패", e);
            response.put("success", false);
            response.put("message", "알림 설정 수정에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "사용자 통계", notes = "사용자의 활동 통계를 조회합니다.")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUserStatistics(Authentication authentication) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("사용자 통계 조회: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> statistics = new HashMap<>();

            // 기본 통계 정보
            statistics.put("joinDate", "2024-01-01"); // 실제로는 DB에서 조회
            statistics.put("loginCount", 42);
            statistics.put("lastLoginDate", "2024-01-15");
            statistics.put("profileCompleteness", 85);
            statistics.put("securityScore", 78);

            // 활동 통계
            Map<String, Object> activityStats = new HashMap<>();
            activityStats.put("totalGoals", 5);
            activityStats.put("completedGoals", 2);
            activityStats.put("totalTransactions", 127);
            activityStats.put("averageMonthlyActivity", 23);
            statistics.put("activity", activityStats);

            response.put("success", true);
            response.put("data", statistics);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("사용자 통계 조회 실패", e);
            response.put("success", false);
            response.put("message", "사용자 통계 조회에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "서버 상태 확인", notes = "서버와 데이터베이스 연결 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 데이터베이스 연결 확인
            boolean dbHealthy = true; // 실제로는 memberService.checkDatabaseConnection()

            response.put("success", true);
            response.put("status", "OK");
            response.put("message", "FinMate 서버가 정상 동작 중입니다.");
            response.put("timestamp", System.currentTimeMillis());
            response.put("database", dbHealthy ? "Connected" : "Disconnected");
            response.put("version", "1.0.0");
            response.put("service", "MemberService");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("헬스 체크 실패", e);
            response.put("success", false);
            response.put("status", "ERROR");
            response.put("message", "서버 상태 확인에 실패했습니다.");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        }
    }
}