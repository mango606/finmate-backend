package com.example.finmate.auth.service;

import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.auth.domain.AuthTokenVO;
import com.example.finmate.auth.domain.LoginHistoryVO;
import com.example.finmate.auth.dto.SecuritySettingsDTO;
import com.example.finmate.auth.mapper.AuthMapper;
import com.example.finmate.common.service.CacheService;
import com.example.finmate.common.util.IPUtils;
import com.example.finmate.common.util.StringUtils;
import com.example.finmate.common.util.ValidationUtils;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthMapper authMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final CacheService cacheService;

    private static final int PASSWORD_RESET_TOKEN_EXPIRY = 24 * 60 * 60; // 24시간 (초)
    private static final int EMAIL_VERIFICATION_TOKEN_EXPIRY = 7 * 24 * 60 * 60; // 7일 (초)

    // 비밀번호 재설정 토큰 생성
    @Transactional
    public String generatePasswordResetToken(String userEmail) {
        try {
            MemberVO member = memberMapper.getMemberByUserEmail(userEmail);
            if (member == null) {
                throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
            }

            if (!member.isActive()) {
                throw new IllegalArgumentException("비활성화된 계정입니다.");
            }

            // 기존 토큰 삭제
            authMapper.deleteExpiredTokens(member.getUserId(), "PASSWORD_RESET");

            // 새 토큰 생성
            String token = StringUtils.generateUUID();
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(24);

            AuthTokenVO authToken = new AuthTokenVO();
            authToken.setUserId(member.getUserId());
            authToken.setToken(token);
            authToken.setTokenType("PASSWORD_RESET");
            authToken.setExpiryTime(expiryTime);
            authToken.setIsUsed(false);

            authMapper.insertAuthToken(authToken);

            // 캐시에 토큰 저장
            cacheService.put("pwd_reset_" + token, member.getUserId(), PASSWORD_RESET_TOKEN_EXPIRY);

            log.info("비밀번호 재설정 토큰 생성: {}", member.getUserId());
            return token;
        } catch (Exception e) {
            log.error("비밀번호 재설정 토큰 생성 실패: {}", userEmail, e);
            throw new RuntimeException("비밀번호 재설정 토큰 생성에 실패했습니다.", e);
        }
    }

    // 비밀번호 재설정
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        try {
            // 캐시에서 토큰 확인
            String userId = cacheService.get("pwd_reset_" + token, String.class);
            if (userId == null) {
                throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
            }

            // DB에서 토큰 확인
            AuthTokenVO authToken = authMapper.getAuthToken(token, "PASSWORD_RESET");
            if (authToken == null || authToken.getIsUsed()) {
                throw new IllegalArgumentException("유효하지 않거나 이미 사용된 토큰입니다.");
            }

            if (authToken.getExpiryTime().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("만료된 토큰입니다.");
            }

            // 비밀번호 변경
            String encodedPassword = passwordEncoder.encode(newPassword);
            int result = memberMapper.updateMemberPassword(userId, encodedPassword);

            if (result > 0) {
                // 토큰 사용 처리
                authMapper.markTokenAsUsed(token);
                cacheService.remove("pwd_reset_" + token);

                log.info("비밀번호 재설정 완료: {}", userId);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("비밀번호 재설정 실패: {}", token, e);
            throw new RuntimeException("비밀번호 재설정에 실패했습니다.", e);
        }
    }

    // 이메일 인증 토큰 생성
    @Transactional
    public String generateEmailVerificationToken(String userEmail) {
        try {
            MemberVO member = memberMapper.getMemberByUserEmail(userEmail);
            if (member == null) {
                throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
            }

            // 기존 토큰 삭제
            authMapper.deleteExpiredTokens(member.getUserId(), "EMAIL_VERIFICATION");

            // 새 토큰 생성
            String token = StringUtils.generateUUID();
            LocalDateTime expiryTime = LocalDateTime.now().plusDays(7);

            AuthTokenVO authToken = new AuthTokenVO();
            authToken.setUserId(member.getUserId());
            authToken.setToken(token);
            authToken.setTokenType("EMAIL_VERIFICATION");
            authToken.setExpiryTime(expiryTime);
            authToken.setIsUsed(false);

            authMapper.insertAuthToken(authToken);

            // 캐시에 토큰 저장
            cacheService.put("email_verify_" + token, member.getUserId(), EMAIL_VERIFICATION_TOKEN_EXPIRY);

            log.info("이메일 인증 토큰 생성: {}", member.getUserId());
            return token;
        } catch (Exception e) {
            log.error("이메일 인증 토큰 생성 실패: {}", userEmail, e);
            throw new RuntimeException("이메일 인증 토큰 생성에 실패했습니다.", e);
        }
    }

    // 이메일 인증 확인
    @Transactional
    public boolean verifyEmail(String token) {
        try {
            // 캐시에서 토큰 확인
            String userId = cacheService.get("email_verify_" + token, String.class);
            if (userId == null) {
                throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
            }

            // DB에서 토큰 확인
            AuthTokenVO authToken = authMapper.getAuthToken(token, "EMAIL_VERIFICATION");
            if (authToken == null || authToken.getIsUsed()) {
                throw new IllegalArgumentException("유효하지 않거나 이미 사용된 토큰입니다.");
            }

            if (authToken.getExpiryTime().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("만료된 토큰입니다.");
            }

            // 이메일 인증 상태 업데이트
            int result = authMapper.updateEmailVerificationStatus(userId, true);

            if (result > 0) {
                // 토큰 사용 처리
                authMapper.markTokenAsUsed(token);
                cacheService.remove("email_verify_" + token);

                log.info("이메일 인증 완료: {}", userId);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("이메일 인증 실패: {}", token, e);
            throw new RuntimeException("이메일 인증에 실패했습니다.", e);
        }
    }

    // 계정 잠금 해제
    @Transactional
    public boolean unlockAccount(String userId) {
        try {
            MemberVO member = memberMapper.getMemberByUserId(userId);
            if (member == null) {
                throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
            }

            // 계정 잠금 해제
            int result = authMapper.updateAccountLockStatus(userId, false);

            if (result > 0) {
                // 로그인 실패 횟수 초기화
                authMapper.resetLoginFailCount(userId);

                log.info("계정 잠금 해제: {}", userId);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("계정 잠금 해제 실패: {}", userId, e);
            throw new RuntimeException("계정 잠금 해제에 실패했습니다.", e);
        }
    }

    // 로그인 이력 조회
    public Map<String, Object> getLoginHistory(String userId, int page, int size) {
        try {
            int offset = page * size;

            List<LoginHistoryVO> histories = authMapper.getLoginHistories(userId, offset, size);
            int totalCount = authMapper.getLoginHistoryCount(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("histories", histories);
            result.put("totalCount", totalCount);
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("totalPages", (totalCount + size - 1) / size);

            return result;
        } catch (Exception e) {
            log.error("로그인 이력 조회 실패: {}", userId, e);
            return getEmptyLoginHistory();
        }
    }

    // 로그인 성공 기록
    @Transactional
    public void recordLoginSuccess(String userId, String ipAddress, String userAgent) {
        try {
            LoginHistoryVO loginHistory = new LoginHistoryVO();
            loginHistory.setUserId(userId);
            loginHistory.setIpAddress(ipAddress);
            loginHistory.setUserAgent(userAgent);
            loginHistory.setLoginSuccess(true);
            loginHistory.setLoginTime(LocalDateTime.now());

            authMapper.insertLoginHistory(loginHistory);

            // 로그인 실패 횟수 초기화
            authMapper.resetLoginFailCount(userId);

            // 마지막 로그인 시간 업데이트
            authMapper.updateLastLoginTime(userId);

            log.info("로그인 성공 기록: {} from {}", userId, ipAddress);
        } catch (Exception e) {
            log.error("로그인 성공 기록 실패: {}", userId, e);
        }
    }

    // 로그인 실패 기록
    @Transactional
    public void recordLoginFailure(String userId, String ipAddress, String userAgent, String failureReason) {
        try {
            LoginHistoryVO loginHistory = new LoginHistoryVO();
            loginHistory.setUserId(userId);
            loginHistory.setIpAddress(ipAddress);
            loginHistory.setUserAgent(userAgent);
            loginHistory.setLoginSuccess(false);
            loginHistory.setFailureReason(failureReason);
            loginHistory.setLoginTime(LocalDateTime.now());

            authMapper.insertLoginHistory(loginHistory);

            // 로그인 실패 횟수 증가
            authMapper.incrementLoginFailCount(userId);

            // 실패 횟수가 5회 이상이면 계정 잠금
            int failCount = authMapper.getLoginFailCount(userId);
            if (failCount >= 5) {
                authMapper.updateAccountLockStatus(userId, true);
                log.warn("계정 잠금 - 로그인 실패 횟수 초과: {}", userId);
            }

            log.warn("로그인 실패 기록: {} from {} - {}", userId, ipAddress, failureReason);
        } catch (Exception e) {
            log.error("로그인 실패 기록 실패: {}", userId, e);
        }
    }

    // 계정 보안 정보 조회
    public Map<String, Object> getAccountSecurity(String userId) {
        try {
            AccountSecurityVO security = authMapper.getAccountSecurity(userId);

            Map<String, Object> result = new HashMap<>();
            if (security != null) {
                result.put("emailVerified", security.getEmailVerified());
                result.put("phoneVerified", security.getPhoneVerified());
                result.put("twoFactorEnabled", security.getTwoFactorEnabled());
                result.put("accountLocked", security.getAccountLocked());
                result.put("loginFailCount", security.getLoginFailCount());
                result.put("lastLoginTime", security.getLastLoginTime());
            } else {
                result = getDefaultSecurityInfo();
            }

            return result;
        } catch (Exception e) {
            log.error("계정 보안 정보 조회 실패: {}", userId, e);
            return getDefaultSecurityInfo();
        }
    }

    // 보안 설정 조회
    public Map<String, Object> getSecuritySettings(String userId) {
        try {
            AccountSecurityVO security = authMapper.getAccountSecurity(userId);

            Map<String, Object> settings = new HashMap<>();
            if (security != null) {
                settings.put("emailVerified", security.getEmailVerified());
                settings.put("phoneVerified", security.getPhoneVerified());
                settings.put("twoFactorEnabled", security.getTwoFactorEnabled());
                settings.put("loginNotificationEnabled", true);
                settings.put("securityQuestionEnabled", false);
            } else {
                settings = getDefaultSecuritySettings();
            }

            return settings;
        } catch (Exception e) {
            log.error("보안 설정 조회 실패: {}", userId, e);
            return getDefaultSecuritySettings();
        }
    }

    // 2단계 인증 설정
    @Transactional
    public boolean updateTwoFactorAuth(String userId, boolean enabled) {
        try {
            int result = authMapper.updateTwoFactorStatus(userId, enabled);

            if (result > 0) {
                log.info("2단계 인증 설정 변경: {} - {}", userId, enabled);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("2단계 인증 설정 실패: {}", userId, e);
            return false;
        }
    }

    // 보안 점검 수행
    public Map<String, Object> performSecurityCheck(String userId) {
        try {
            Map<String, Object> checkResult = new HashMap<>();
            int score = 0;

            AccountSecurityVO security = authMapper.getAccountSecurity(userId);
            MemberVO member = memberMapper.getMemberByUserId(userId);

            // 이메일 인증 상태 (20점)
            if (security != null && security.getEmailVerified()) {
                score += 20;
                checkResult.put("emailVerified", true);
            } else {
                checkResult.put("emailVerified", false);
            }

            // 2단계 인증 상태 (30점)
            if (security != null && security.getTwoFactorEnabled()) {
                score += 30;
                checkResult.put("twoFactorEnabled", true);
            } else {
                checkResult.put("twoFactorEnabled", false);
            }

            // 비밀번호 강도 (25점)
            score += 20;
            checkResult.put("passwordStrength", "GOOD");

            // 최근 로그인 활동 (25점)
            if (security != null && security.getLastLoginTime() != null) {
                LocalDateTime lastLogin = security.getLastLoginTime();
                if (lastLogin.isAfter(LocalDateTime.now().minusDays(30))) {
                    score += 25;
                    checkResult.put("recentActivity", true);
                } else {
                    checkResult.put("recentActivity", false);
                }
            } else {
                checkResult.put("recentActivity", false);
            }

            checkResult.put("totalScore", score);
            checkResult.put("grade", getSecurityGrade(score));
            checkResult.put("recommendations", getSecurityRecommendations(security));

            return checkResult;
        } catch (Exception e) {
            log.error("보안 점검 실패: {}", userId, e);
            return getDefaultSecurityCheck();
        }
    }

    // 보안 이벤트 기록
    @Transactional
    public void recordSecurityEvent(String userId, String eventType, String ipAddress) {
        try {
            LoginHistoryVO securityEvent = new LoginHistoryVO();
            securityEvent.setUserId(userId);
            securityEvent.setIpAddress(ipAddress);
            securityEvent.setLoginSuccess(true);
            securityEvent.setFailureReason(eventType);
            securityEvent.setLoginTime(LocalDateTime.now());

            authMapper.insertLoginHistory(securityEvent);

            log.info("보안 이벤트 기록: {} - {} from {}", userId, eventType, ipAddress);
        } catch (Exception e) {
            log.error("보안 이벤트 기록 실패: {}", userId, e);
        }
    }

    // 세션 검증
    public boolean validateSession(String userId, String sessionId) {
        try {
            String cachedSessionId = cacheService.get("session_" + userId, String.class);
            return sessionId.equals(cachedSessionId);
        } catch (Exception e) {
            log.error("세션 검증 실패: {}", userId, e);
            return false;
        }
    }

    // 세션 생성
    public void createSession(String userId, String sessionId) {
        try {
            cacheService.put("session_" + userId, sessionId, 1800); // 30분
        } catch (Exception e) {
            log.error("세션 생성 실패: {}", userId, e);
        }
    }

    // 세션 삭제
    public void deleteSession(String userId) {
        try {
            cacheService.remove("session_" + userId);
        } catch (Exception e) {
            log.error("세션 삭제 실패: {}", userId, e);
        }
    }

    // 만료된 토큰 정리
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            authMapper.deleteExpiredTokens(null, null);
            log.info("만료된 토큰 정리 완료");
        } catch (Exception e) {
            log.error("만료된 토큰 정리 실패", e);
        }
    }

    // 헬퍼 메서드들
    private String getSecurityGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 70) return "B";
        if (score >= 50) return "C";
        if (score >= 30) return "D";
        return "F";
    }

    private java.util.List<String> getSecurityRecommendations(AccountSecurityVO security) {
        java.util.List<String> recommendations = new java.util.ArrayList<>();

        if (security == null || !security.getEmailVerified()) {
            recommendations.add("이메일 인증을 완료해주세요.");
        }

        if (security == null || !security.getTwoFactorEnabled()) {
            recommendations.add("2단계 인증을 활성화해주세요.");
        }

        if (security == null || !security.getPhoneVerified()) {
            recommendations.add("휴대폰 인증을 완료해주세요.");
        }

        recommendations.add("정기적으로 비밀번호를 변경해주세요.");
        recommendations.add("의심스러운 활동이 있는지 정기적으로 확인해주세요.");

        return recommendations;
    }

    private Map<String, Object> getDefaultSecurityInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("emailVerified", false);
        result.put("phoneVerified", false);
        result.put("twoFactorEnabled", false);
        result.put("accountLocked", false);
        result.put("loginFailCount", 0);
        result.put("lastLoginTime", null);
        return result;
    }

    private Map<String, Object> getDefaultSecuritySettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("emailVerified", false);
        settings.put("phoneVerified", false);
        settings.put("twoFactorEnabled", false);
        settings.put("loginNotificationEnabled", true);
        settings.put("securityQuestionEnabled", false);
        return settings;
    }

    private Map<String, Object> getEmptyLoginHistory() {
        Map<String, Object> result = new HashMap<>();
        result.put("histories", new java.util.ArrayList<>());
        result.put("totalCount", 0);
        result.put("currentPage", 0);
        result.put("pageSize", 10);
        result.put("totalPages", 0);
        return result;
    }

    private Map<String, Object> getEmptyActivityHistory() {
        Map<String, Object> result = new HashMap<>();
        result.put("activities", new java.util.ArrayList<>());
        result.put("totalCount", 0);
        result.put("currentPage", 0);
        result.put("pageSize", 10);
        result.put("totalPages", 0);
        return result;
    }

    private Map<String, Object> getDefaultSecurityCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("emailVerified", false);
        result.put("twoFactorEnabled", false);
        result.put("passwordStrength", "UNKNOWN");
        result.put("recentActivity", false);
        result.put("totalScore", 0);
        result.put("grade", "F");
        result.put("recommendations", java.util.Arrays.asList("보안 점검을 다시 시도해주세요."));
        return result;
    }

    // 비밀번호 변경
    @Transactional
    public boolean changePassword(String userId, String currentPassword, String newPassword) {
        try {
            MemberVO member = memberMapper.getMemberByUserId(userId);
            if (member == null) {
                throw new IllegalArgumentException("회원을 찾을 수 없습니다: " + userId);
            }

            // 현재 비밀번호 확인
            if (!passwordEncoder.matches(currentPassword, member.getUserPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }

            // 새 비밀번호 유효성 검증
            if (!ValidationUtils.isValidPassword(newPassword)) {
                throw new IllegalArgumentException("새 비밀번호가 유효하지 않습니다.");
            }

            // 새 비밀번호와 현재 비밀번호가 같은지 확인
            if (passwordEncoder.matches(newPassword, member.getUserPassword())) {
                throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
            }

            String encodedPassword = passwordEncoder.encode(newPassword);
            int result = memberMapper.updateMemberPassword(userId, encodedPassword);

            log.info("비밀번호 변경 완료: {}", userId);
            return result > 0;

        } catch (Exception e) {
            log.error("비밀번호 변경 실패: {}", userId, e);
            throw new RuntimeException("비밀번호 변경에 실패했습니다.", e);
        }
    }

    // 보안 설정 업데이트
    @Transactional
    public boolean updateSecuritySettings(String userId, SecuritySettingsDTO settingsDTO) {
        try {
            AccountSecurityVO security = authMapper.getAccountSecurity(userId);

            if (security == null) {
                // 보안 정보가 없으면 새로 생성
                security = new AccountSecurityVO();
                security.setUserId(userId);
            }

            // 설정 업데이트
            if (settingsDTO.getEmailVerificationEnabled() != null) {
                security.setEmailVerified(settingsDTO.getEmailVerificationEnabled());
            }
            if (settingsDTO.getPhoneVerificationEnabled() != null) {
                security.setPhoneVerified(settingsDTO.getPhoneVerificationEnabled());
            }
            if (settingsDTO.getTwoFactorAuthEnabled() != null) {
                security.setTwoFactorEnabled(settingsDTO.getTwoFactorAuthEnabled());
            }

            int result = authMapper.updateAccountSecurity(security);

            log.info("보안 설정 업데이트 완료: {}", userId);
            return result > 0;

        } catch (Exception e) {
            log.error("보안 설정 업데이트 실패: {}", userId, e);
            return false;
        }
    }

    // 2단계 인증 설정 시작
    public Map<String, Object> setupTwoFactorAuth(String userId) {
        try {
            // QR 코드용 시크릿 키 생성 (실제로는 Google Authenticator 등과 연동)
            String secretKey = StringUtils.generateRandomString(32);

            // 임시로 캐시에 저장 (실제로는 임시 테이블이나 Redis 사용)
            cacheService.put("2fa_setup_" + userId, secretKey, 300); // 5분간 유효

            Map<String, Object> setupInfo = new HashMap<>();
            setupInfo.put("secretKey", secretKey);
            setupInfo.put("qrCodeUrl", generateQRCodeUrl(userId, secretKey));
            setupInfo.put("manualEntryKey", secretKey);
            setupInfo.put("backupCodes", generateBackupCodes());

            log.info("2단계 인증 설정 시작: {}", userId);
            return setupInfo;

        } catch (Exception e) {
            log.error("2단계 인증 설정 실패: {}", userId, e);
            throw new RuntimeException("2단계 인증 설정에 실패했습니다.", e);
        }
    }

    // 2단계 인증 코드 확인
    @Transactional
    public boolean verifyTwoFactorAuth(String userId, String authCode) {
        try {
            // 캐시에서 시크릿 키 확인
            String secretKey = cacheService.get("2fa_setup_" + userId, String.class);
            if (secretKey == null) {
                throw new IllegalArgumentException("2단계 인증 설정 세션이 만료되었습니다.");
            }

            // TOTP 코드 검증 (실제로는 Google Authenticator 라이브러리 사용)
            boolean isValid = verifyTOTPCode(secretKey, authCode);

            if (isValid) {
                // 2단계 인증 활성화
                authMapper.updateTwoFactorStatus(userId, true);

                // 시크릿 키를 안전한 곳에 저장 (실제로는 암호화해서 DB에 저장)
                cacheService.put("2fa_secret_" + userId, secretKey, 86400 * 365); // 1년간 보관

                // 설정용 임시 키는 삭제
                cacheService.remove("2fa_setup_" + userId);

                log.info("2단계 인증 활성화 완료: {}", userId);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("2단계 인증 확인 실패: {}", userId, e);
            return false;
        }
    }

    // QR 코드 URL 생성 (Google Authenticator 형식)
    private String generateQRCodeUrl(String userId, String secretKey) {
        String issuer = "FinMate";
        String account = userId + "@finmate.com";

        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, account, secretKey, issuer
        );
    }

    // 백업 코드 생성
    private List<String> generateBackupCodes() {
        List<String> backupCodes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            backupCodes.add(StringUtils.generateRandomString(8).toUpperCase());
        }
        return backupCodes;
    }

    // TOTP 코드 검증
    private boolean verifyTOTPCode(String secretKey, String code) {
        // TODO: TOTP 라이브러리 사용
        if (code == null || code.length() != 6) {
            return false;
        }

        // 현재 시간 기반으로 간단한 검증
        long currentTime = System.currentTimeMillis() / 30000; // 30초 간격
        String expectedCode = String.valueOf((secretKey.hashCode() + currentTime) % 1000000);
        expectedCode = String.format("%06d", Integer.parseInt(expectedCode));

        return expectedCode.equals(code);
    }

    // 활동 내역 조회 (로그인 + 보안 이벤트)
    public Map<String, Object> getActivityHistory(String userId, int page, int size) {
        try {
            Map<String, Object> loginHistory = getLoginHistory(userId, page, size);

            // 보안 이벤트도 함께 조회
            List<Map<String, Object>> securityEvents = authMapper.getSecurityEvents(userId, 30);

            Map<String, Object> result = new HashMap<>();
            result.put("activities", loginHistory.get("histories"));
            result.put("securityEvents", securityEvents);
            result.put("totalCount", loginHistory.get("totalCount"));
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("totalPages", loginHistory.get("totalPages"));

            return result;
        } catch (Exception e) {
            log.error("활동 내역 조회 실패: {}", userId, e);
            return getEmptyActivityHistory();
        }
    }

    // 계정 보안 점수 계산
    public Map<String, Object> calculateSecurityScore(String userId) {
        try {
            AccountSecurityVO security = authMapper.getAccountSecurity(userId);
            MemberVO member = memberMapper.getMemberByUserId(userId);

            int score = 0;
            List<String> recommendations = new ArrayList<>();

            // 기본 점수 (계정 존재)
            score += 20;

            // 이메일 인증 (20점)
            if (security != null && Boolean.TRUE.equals(security.getEmailVerified())) {
                score += 20;
            } else {
                recommendations.add("이메일 인증을 완료해주세요.");
            }

            // 2단계 인증 (30점)
            if (security != null && Boolean.TRUE.equals(security.getTwoFactorEnabled())) {
                score += 30;
            } else {
                recommendations.add("2단계 인증을 활성화해주세요.");
            }

            // 비밀번호 강도 (30점)
            // 실제로는 마지막 비밀번호 변경일 등을 고려
            score += 25; // 기본 점수

            Map<String, Object> result = new HashMap<>();
            result.put("score", Math.min(score, 100));
            result.put("grade", getSecurityGrade(score));
            result.put("recommendations", recommendations);
            result.put("lastUpdated", System.currentTimeMillis());

            return result;
        } catch (Exception e) {
            log.error("보안 점수 계산 실패: {}", userId, e);
            return getDefaultSecurityCheck();
        }
    }

    // 의심스러운 활동 감지 및 알림
    @Transactional
    public void detectSuspiciousActivity(String userId, String activityType, String clientIP) {
        try {
            // 최근 활동 패턴 분석
            List<LoginHistoryVO> recentLogins = authMapper.getLoginHistories(userId, 0, 10);

            boolean suspicious = false;
            String reason = "";

            // IP 주소 변경 감지
            if (!recentLogins.isEmpty()) {
                String lastIP = recentLogins.get(0).getIpAddress();
                if (!clientIP.equals(lastIP)) {
                    suspicious = true;
                    reason = "새로운 IP 주소에서의 접근";
                }
            }

            // 시간대 이상 감지 (예: 새벽 시간대 접근)
            int currentHour = java.time.LocalTime.now().getHour();
            if (currentHour >= 2 && currentHour <= 5) {
                suspicious = true;
                reason += (reason.isEmpty() ? "" : ", ") + "비정상적인 시간대 접근";
            }

            if (suspicious) {
                // 보안 이벤트 기록
                recordSecurityEvent(userId, "SUSPICIOUS_ACTIVITY_" + activityType, clientIP);

                // 사용자에게 알림 (이메일 등)
                MemberVO member = memberMapper.getMemberByUserId(userId);
                if (member != null) {
                    emailService.sendSecurityAlert(member.getUserEmail(), member.getUserName(), reason, clientIP);
                }

                log.warn("의심스러운 활동 감지: {} - {} from {}", userId, reason, IPUtils.maskIP(clientIP));
            }

        } catch (Exception e) {
            log.error("의심스러운 활동 감지 실패: {}", userId, e);
        }
    }
}