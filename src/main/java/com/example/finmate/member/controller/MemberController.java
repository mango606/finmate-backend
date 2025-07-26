package com.example.finmate.member.controller;

import com.example.finmate.auth.service.AuthService;
import com.example.finmate.common.dto.ApiResponse;
import com.example.finmate.common.exception.DuplicateResourceException;
import com.example.finmate.common.exception.MemberNotFoundException;
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> insertMember(
            @Valid @RequestBody MemberJoinDTO joinDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        log.info("회원 가입 요청: {} from {}", joinDTO.getUserId(), IPUtils.maskIP(clientIP));

        try {
            boolean success = memberService.insertMember(joinDTO);

            if (!success) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("회원 가입에 실패했습니다.", "MEMBER_JOIN_FAILED"));
            }

            // 환영 이메일 발송
            Map<String, Object> result = new HashMap<>();
            try {
                emailService.sendWelcomeEmail(joinDTO.getUserEmail(), joinDTO.getUserName());

                // 이메일 인증 토큰 생성 및 발송
                String verificationToken = authService.generateEmailVerificationToken(joinDTO.getUserEmail());
                emailService.sendActivationEmail(joinDTO.getUserEmail(), verificationToken);
                result.put("emailSent", true);
            } catch (Exception e) {
                log.warn("이메일 발송 실패: {}", e.getMessage());
                result.put("emailSent", false);
            }

            return ResponseEntity.ok(ApiResponse.success("회원 가입이 완료되었습니다. 이메일 인증을 완료해주세요.", result));

        } catch (DuplicateResourceException e) {
            log.warn("회원 가입 실패 - 중복: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "DUPLICATE_RESOURCE"));
        } catch (IllegalArgumentException e) {
            log.warn("회원 가입 실패 - 유효성 검증: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @ApiOperation(value = "회원 정보 조회", notes = "로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberInfo(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();

        log.info("회원 정보 조회: {}", userId);

        try {
            MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);

            // 보안 정보 포함
            Map<String, Object> enhancedInfo = new HashMap<>();
            enhancedInfo.put("basic", memberInfo);
            enhancedInfo.put("security", authService.getAccountSecurity(userId));

            return ResponseEntity.ok(ApiResponse.success("회원 정보 조회 성공", enhancedInfo));

        } catch (MemberNotFoundException e) {
            log.warn("회원 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "MEMBER_NOT_FOUND"));
        }
    }

    @ApiOperation(value = "회원 정보 수정", notes = "로그인한 사용자의 정보를 수정합니다.")
    @PutMapping("/info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> updateMember(
            Authentication authentication,
            @Valid @RequestBody MemberUpdateDTO updateDTO) {

        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();

        log.info("회원 정보 수정: {}", userId);

        try {
            boolean success = memberService.updateMember(userId, updateDTO);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("회원 정보가 수정되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("회원 정보 수정에 실패했습니다.", "UPDATE_FAILED"));
            }

        } catch (DuplicateResourceException e) {
            log.warn("회원 정보 수정 실패 - 중복: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "DUPLICATE_RESOURCE"));
        } catch (IllegalArgumentException e) {
            log.warn("회원 정보 수정 실패 - 유효성 검증: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @ApiOperation(value = "비밀번호 확인", notes = "개인정보 수정 시 현재 비밀번호를 확인합니다.")
    @PostMapping("/verify-password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> verifyPassword(
            @RequestBody Map<String, String> passwordData,
            Authentication authentication) {

        String userId = SecurityUtils.getCurrentUserId();
        String password = passwordData.get("password");

        log.info("비밀번호 확인: {}", userId);

        try {
            boolean isValid = memberService.verifyPassword(userId, password);

            if (isValid) {
                return ResponseEntity.ok(ApiResponse.success("비밀번호가 확인되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("비밀번호가 일치하지 않습니다.", "PASSWORD_MISMATCH"));
            }

        } catch (Exception e) {
            log.error("비밀번호 확인 실패: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("비밀번호 확인에 실패했습니다.", "PASSWORD_VERIFY_ERROR"));
        }
    }

    @ApiOperation(value = "비밀번호 변경", notes = "로그인한 사용자의 비밀번호를 변경합니다.")
    @PutMapping("/password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> updateMemberPassword(
            Authentication authentication,
            @RequestBody Map<String, String> passwordData,
            HttpServletRequest request) {

        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();
        String clientIP = IPUtils.getClientIP(request);

        log.info("비밀번호 변경: {} from {}", userId, IPUtils.maskIP(clientIP));

        try {
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("현재 비밀번호와 새 비밀번호를 입력해주세요.", "MISSING_PARAMETERS"));
            }

            boolean success = memberService.updateMemberPassword(userId, currentPassword, newPassword);

            if (success) {
                // 보안 이벤트 기록
                authService.recordSecurityEvent(userId, "PASSWORD_CHANGED", clientIP);
                return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("비밀번호 변경에 실패했습니다.", "PASSWORD_CHANGE_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            log.warn("비밀번호 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @ApiOperation(value = "회원 탈퇴", notes = "로그인한 사용자의 계정을 삭제합니다.")
    @DeleteMapping("/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            Authentication authentication,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();
        String clientIP = IPUtils.getClientIP(request);

        log.info("회원 탈퇴: {} from {}", userId, IPUtils.maskIP(clientIP));

        try {
            boolean success = memberService.deleteMember(userId);

            if (success) {
                // 보안 이벤트 기록
                authService.recordSecurityEvent(userId, "ACCOUNT_WITHDRAWN", clientIP);
                return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("회원 탈퇴에 실패했습니다.", "WITHDRAWAL_FAILED"));
            }

        } catch (MemberNotFoundException e) {
            log.warn("회원 탈퇴 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "MEMBER_NOT_FOUND"));
        }
    }

    @ApiOperation(value = "사용자 ID 중복 확인", notes = "회원가입 시 사용자 ID 중복을 확인합니다.")
    @GetMapping("/checkUserId/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkUserIdDuplicate(
            @ApiParam(value = "확인할 사용자 ID", required = true) @PathVariable String userId) {

        log.info("사용자 ID 중복 확인: {}", userId);

        boolean isDuplicate = memberService.checkUserIdDuplicate(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("isDuplicate", isDuplicate);
        result.put("available", !isDuplicate);

        String message = isDuplicate ? "이미 사용 중인 ID입니다." : "사용 가능한 ID입니다.";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @ApiOperation(value = "이메일 중복 확인", notes = "회원가입 시 이메일 중복을 확인합니다.")
    @GetMapping("/checkEmail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkUserEmailDuplicate(
            @ApiParam(value = "확인할 이메일", required = true) @RequestParam String userEmail) {

        log.info("이메일 중복 확인: {}", userEmail);

        boolean isDuplicate = memberService.checkUserEmailDuplicate(userEmail);

        Map<String, Object> result = new HashMap<>();
        result.put("isDuplicate", isDuplicate);
        result.put("available", !isDuplicate);

        String message = isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @ApiOperation(value = "프로필 이미지 업로드", notes = "사용자의 프로필 이미지를 업로드합니다.")
    @PostMapping("/profile-image")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("프로필 이미지 업로드: {}", userId);

        try {
            String fileName = fileUploadService.uploadProfileImage(userId, file);

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("imageUrl", "/upload/avatar/" + userId + "/" + fileName);

            return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 업로드되었습니다.", result));

        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("프로필 이미지 업로드에 실패했습니다.", "UPLOAD_FAILED"));
        }
    }

    @ApiOperation(value = "서버 상태 확인", notes = "서버와 데이터베이스 연결 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        try {
            // 데이터베이스 연결 확인
            boolean dbHealthy = memberService.checkDatabaseConnection();

            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "OK");
            healthInfo.put("database", dbHealthy ? "Connected" : "Disconnected");
            healthInfo.put("version", "1.0.0");
            healthInfo.put("service", "MemberService");
            healthInfo.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponse.success("FinMate 서버가 정상 동작 중입니다.", healthInfo));

        } catch (Exception e) {
            log.error("헬스 체크 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("서버 상태 확인에 실패했습니다.", "HEALTH_CHECK_FAILED"));
        }
    }

    @ApiOperation(value = "보안 설정 조회", notes = "사용자의 보안 설정을 조회합니다.")
    @GetMapping("/security-settings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecuritySettings(Authentication authentication) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("보안 설정 조회: {}", userId);

        try {
            Map<String, Object> securitySettings = authService.getSecuritySettings(userId);
            return ResponseEntity.ok(ApiResponse.success("보안 설정 조회 성공", securitySettings));

        } catch (Exception e) {
            log.error("보안 설정 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("보안 설정 조회에 실패했습니다.", "SECURITY_SETTINGS_ERROR"));
        }
    }

    @ApiOperation(value = "2단계 인증 설정", notes = "2단계 인증을 활성화/비활성화합니다.")
    @PostMapping("/two-factor-auth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> updateTwoFactorAuth(
            Authentication authentication,
            @RequestParam boolean enabled,
            HttpServletRequest request) {

        String userId = SecurityUtils.getCurrentUserId();
        String clientIP = IPUtils.getClientIP(request);
        log.info("2단계 인증 설정: {} - {} from {}", userId, enabled, IPUtils.maskIP(clientIP));

        try {
            boolean success = authService.updateTwoFactorAuth(userId, enabled);

            if (success) {
                authService.recordSecurityEvent(userId,
                        enabled ? "TWO_FACTOR_ENABLED" : "TWO_FACTOR_DISABLED", clientIP);

                String message = enabled ? "2단계 인증이 활성화되었습니다." : "2단계 인증이 비활성화되었습니다.";
                return ResponseEntity.ok(ApiResponse.success(message, null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("2단계 인증 설정에 실패했습니다.", "TWO_FACTOR_FAILED"));
            }

        } catch (Exception e) {
            log.error("2단계 인증 설정 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("2단계 인증 설정에 실패했습니다.", "TWO_FACTOR_ERROR"));
        }
    }
}