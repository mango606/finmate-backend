package com.example.finmate.auth.service;

import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.auth.domain.AuthTokenVO;
import com.example.finmate.auth.domain.LoginHistoryVO;
import com.example.finmate.auth.mapper.AuthMapper;
import com.example.finmate.common.service.CacheService;
import com.example.finmate.common.service.EmailService;
import com.example.finmate.common.util.StringUtils;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    private static final int PASSWORD_RESET_TOKEN_EXPIRY = 24 * 60 * 60; // 24시간 (초)
    private static final int EMAIL_VERIFICATION_TOKEN_EXPIRY = 7 * 24 * 60 * 60; // 7일 (초)
    private static final int MAX_LOGIN_ATTEMPTS = 5; // 최대 로그인 시도 횟수
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30; // 계정 잠금 시간 (분)

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

                // 비밀번호 변경 시 모든 Refresh Token 무효화 (보안상)
                refreshTokenService.invalidateAllRefreshTokens(userId);

                // 로그인 실패 횟수 초기화
                authMapper.resetLoginFailCount(userId);

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

    // 계정 잠금 해제 (개선된 버전)
    @Transactional
    public boolean unlockAccount(String userId) {
        try {
            MemberVO member = memberMapper.getMemberByUserId(userId);
            if (member == null) {
                throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
            }

            // 계정 잠금 해제
            AccountSecurityVO security = new AccountSecurityVO();
            security.setUserId(userId);
            security.setAccountLocked(false);
            security.setLockTime(null);
            security.setLockReason(null);
            security.setLoginFailCount(0);

            int result = authMapper.updateAccountSecurity(security);
            if (result > 0) {
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

            // 의심스러운 로그인 감지
            detectSuspiciousLogin(userId, ipAddress, userAgent);

            log.info("로그인 성공 기록: {} from {}", userId, ipAddress);
        } catch (Exception e) {
            log.error("로그인 성공 기록 실패: {}", userId, e);
        }
    }

    // 로그인 실패 기록 (개선된 버전)
    @Transactional
    public void recordLoginFailure(String userId, String ipAddress, String userAgent, String failureReason) {
        try {
            // 로그인 이력 기록
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
            int failCount = authMapper.getLoginFailCount(userId);

            // 계정 잠금 처리
            if (failCount >= MAX_LOGIN_ATTEMPTS) {
                AccountSecurityVO security = new AccountSecurityVO();
                security.setUserId(userId);
                security.setAccountLocked(true);
                security.setLockTime(LocalDateTime.now());
                security.setLockReason(String.format("로그인 %d회 실패로 인한 자동 잠금", failCount));

                authMapper.updateAccountSecurity(security);

                // 모든 Refresh Token 무효화
                refreshTokenService.invalidateAllRefreshTokens(userId);

                // 계정 잠금 알림 이메일 발송
                try {
                    MemberVO member = memberMapper.getMemberByUserId(userId);
                    if (member != null) {
                        emailService.sendAccountLockNotification(member.getUserEmail(), member.getUserName());
                    }
                } catch (Exception e) {
                    log.warn("계정 잠금 알림 이메일 발송 실패: {}", userId, e);
                }

                log.warn("계정 잠금 - 로그인 실패 횟수 초과: {} ({}회)", userId, failCount);
            }

            log.warn("로그인 실패 기록: {} from {} - {} (실패 횟수: {})", userId, ipAddress, failureReason, failCount);
        } catch (Exception e) {
            log.error("로그인 실패 기록 실패: {}", userId, e);
        }
    }

    // 잠긴 계정 목록 조회 (관리자용)
    public List<AccountSecurityVO> getLockedAccounts() {
        try {
            return authMapper.getLockedAccounts();
        } catch (Exception e) {
            log.error("잠긴 계정 목록 조회 실패", e);
            return new java.util.ArrayList<>();
        }
    }

    // 계정 잠금 통계 조회
    public Map<String, Object> getAccountLockStatistics() {
        try {
            return authMapper.getAccountLockStatistics();
        } catch (Exception e) {
            log.error("계정 잠금 통계 조회 실패", e);
            return new HashMap<>();
        }
    }

    // 의심스러운 로그인 감지
    private void detectSuspiciousLogin(String userId, String ipAddress, String userAgent) {
        try {
            // 최근 로그인 정보 확인
            String lastIpKey = "last_login_ip_" + userId;
            String lastUserAgentKey = "last_login_ua_" + userId;

            String lastIp = cacheService.get(lastIpKey, String.class);
            String lastUserAgent = cacheService.get(lastUserAgentKey, String.class);

            boolean suspicious = false;
            StringBuilder suspiciousReason = new StringBuilder();

            // IP 주소가 다른 경우
            if (lastIp != null && !lastIp.equals(ipAddress)) {
                suspicious = true;
                suspiciousReason.append("새로운 IP 주소에서 로그인 (").append(lastIp).append(" -> ").append(ipAddress).append("); ");
            }

            // User Agent가 다른 경우
            if (lastUserAgent != null && !lastUserAgent.equals(userAgent)) {
                suspicious = true;
                suspiciousReason.append("새로운 기기/브라우저에서 로그인; ");
            }

            if (suspicious) {
                // 의심스러운 로그인 이벤트 기록
                recordSecurityEvent(userId, "SUSPICIOUS_LOGIN: " + suspiciousReason.toString(), ipAddress);

                // 의심스러운 로그인 알림 이메일 발송
                try {
                    MemberVO member = memberMapper.getMemberByUserId(userId);
                    if (member != null) {
                        sendSuspiciousLoginNotification(member, ipAddress, userAgent);
                    }
                } catch (Exception e) {
                    log.warn("의심스러운 로그인 알림 이메일 발송 실패: {}", userId, e);
                }

                log.info("의심스러운 로그인 감지: {} - {}", userId, suspiciousReason.toString());
            }

            // 정보 업데이트
            cacheService.put(lastIpKey, ipAddress, 86400); // 24시간
            cacheService.put(lastUserAgentKey, userAgent, 86400);

        } catch (Exception e) {
            log.error("의심스러운 로그인 감지 실패: {}", userId, e);
        }
    }

    // 의심스러운 로그인 알림 이메일 발송
    private void sendSuspiciousLoginNotification(MemberVO member, String ipAddress, String userAgent) {
        try {
            String subject = "FinMate 보안 알림: 새로운 기기에서 로그인";
            String content = String.format(
                    "안녕하세요 %s님,\n\n" +
                            "회원님의 계정에 새로운 기기에서 로그인이 감지되었습니다.\n\n" +
                            "로그인 정보:\n" +
                            "- IP 주소: %s\n" +
                            "- 기기/브라우저: %s\n" +
                            "- 시간: %s\n\n" +
                            "본인의 로그인이 아니라면 즉시 비밀번호를 변경하고 고객센터로 문의해주세요.\n\n" +
                            "감사합니다.\n" +
                            "FinMate 보안팀",
                    member.getUserName(),
                    ipAddress,
                    userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 100)) : "알 수 없음",
                    LocalDateTime.now()
            );

            emailService.sendEmail(member.getUserEmail(), subject, content);
        } catch (Exception e) {
            log.error("의심스러운 로그인 알림 이메일 발송 실패", e);
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
                result.put("lockReason", security.getLockReason());
            } else {
                result = getDefaultSecurityInfo();
            }

            // Refresh Token 통계 추가
            Map<String, Object> refreshTokenStats = refreshTokenService.getRefreshTokenStatistics(userId);
            result.put("refreshTokenStats", refreshTokenStats);

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
                settings.put("accountLocked", security.getAccountLocked());
                settings.put("loginFailCount", security.getLoginFailCount());
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
                // 2단계 인증 비활성화 시 기존 Refresh Token 모두 무효화 (보안상)
                if (!enabled) {
                    refreshTokenService.invalidateAllRefreshTokens(userId);
                }

                log.info("2단계 인증 설정 변경: {} - {}", userId, enabled);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("2단계 인증 설정 실패: {}", userId, e);
            return false;
        }
    }

    // 계정 활동 내역 조회
    public Map<String, Object> getActivityHistory(String userId, int page, int size) {
        try {
            Map<String, Object> loginHistory = getLoginHistory(userId, page, size);

            Map<String, Object> result = new HashMap<>();
            result.put("activities", loginHistory.get("histories"));
            result.put("totalCount", loginHistory.get("totalCount"));
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("totalPages", loginHistory.get("totalPages"));

            return result;
        } catch (Exception e) {
            log.error("계정 활동 내역 조회 실패: {}", userId, e);
            return getEmptyActivityHistory();
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
                    checkResult.put("recentActivity", false);
                }
            } else {
                checkResult.put("recentActivity", false);
            }

            // Refresh Token 보안 검사 (추가 5점)
            Map<String, Object> tokenStats = refreshTokenService.getRefreshTokenStatistics(userId);
            int activeTokenCount = (Integer) tokenStats.getOrDefault("activeTokenCount", 0);
            if (activeTokenCount <= 3) { // 적절한 토큰 개수 유지
                score += 5;
                checkResult.put("refreshTokenSecurity", true);
            } else {
                checkResult.put("refreshTokenSecurity", false);
            }

            checkResult.put("totalScore", score);
            checkResult.put("grade", getSecurityGrade(score));
            checkResult.put("recommendations", getSecurityRecommendations(security, activeTokenCount));
            checkResult.put("refreshTokenStats", tokenStats);

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

    private java.util.List<String> getSecurityRecommendations(AccountSecurityVO security, int activeTokenCount) {
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

        if (activeTokenCount > 5) {
            recommendations.add("사용하지 않는 기기의 로그인 세션을 정리해주세요.");
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
        result.put("refreshTokenStats", new HashMap<>());
        return result;
    }

    private Map<String, Object> getDefaultSecuritySettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("emailVerified", false);
        settings.put("phoneVerified", false);
        settings.put("twoFactorEnabled", false);
        settings.put("loginNotificationEnabled", true);
        settings.put("securityQuestionEnabled", false);
        settings.put("accountLocked", false);
        settings.put("loginFailCount", 0);
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
        result.put("refreshTokenSecurity", false);
        result.put("totalScore", 0);
        result.put("grade", "F");
        result.put("recommendations", java.util.Arrays.asList("보안 점검을 다시 시도해주세요."));
        result.put("refreshTokenStats", new HashMap<>());
        return result;
    }
}