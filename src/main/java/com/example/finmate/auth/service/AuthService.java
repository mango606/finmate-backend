package com.example.finmate.auth.service;

import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.auth.domain.AuthTokenVO;
import com.example.finmate.auth.domain.LoginHistoryVO;
import com.example.finmate.auth.mapper.AuthMapper;
import com.example.finmate.common.service.CacheService;
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

    private static final int PASSWORD_RESET_TOKEN_EXPIRY = 24 * 60 * 60; // 24시간 (초)
    private static final int EMAIL_VERIFICATION_TOKEN_EXPIRY = 7 * 24 * 60 * 60; // 7일 (초)

    // 비밀번호 재설정 토큰 생성
    @Transactional
    public String generatePasswordResetToken(String userEmail) {
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
    }

    // 비밀번호 재설정
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
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
    }

    // 이메일 인증 토큰 생성
    @Transactional
    public String generateEmailVerificationToken(String userEmail) {
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
    }

    // 이메일 인증 확인
    @Transactional
    public boolean verifyEmail(String token) {
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
    }

    // 계정 잠금 해제
    @Transactional
    public boolean unlockAccount(String userId) {
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
    }

    // 로그인 이력 조회
    public Map<String, Object> getLoginHistory(String userId, int page, int size) {
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
    }

    // 로그인 성공 기록
    @Transactional
    public void recordLoginSuccess(String userId, String ipAddress, String userAgent) {
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
    }

    // 로그인 실패 기록
    @Transactional
    public void recordLoginFailure(String userId, String ipAddress, String userAgent, String failureReason) {
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
    }

    // 계정 보안 정보 조회
    public Map<String, Object> getAccountSecurity(String userId) {
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
            // 기본값 설정
            result.put("emailVerified", false);
            result.put("phoneVerified", false);
            result.put("twoFactorEnabled", false);
            result.put("accountLocked", false);
            result.put("loginFailCount", 0);
            result.put("lastLoginTime", null);
        }

        return result;
    }

    // 보안 설정 조회
    public Map<String, Object> getSecuritySettings(String userId) {
        AccountSecurityVO security = authMapper.getAccountSecurity(userId);

        Map<String, Object> settings = new HashMap<>();
        if (security != null) {
            settings.put("emailVerified", security.getEmailVerified());
            settings.put("phoneVerified", security.getPhoneVerified());
            settings.put("twoFactorEnabled", security.getTwoFactorEnabled());
            settings.put("loginNotificationEnabled", true); // 기본값
            settings.put("securityQuestionEnabled", false); // 기본값
        } else {
            settings.put("emailVerified", false);
            settings.put("phoneVerified", false);
            settings.put("twoFactorEnabled", false);
            settings.put("loginNotificationEnabled", true);
            settings.put("securityQuestionEnabled", false);
        }

        return settings;
    }

    // 2단계 인증 설정
    @Transactional
    public boolean updateTwoFactorAuth(String userId, boolean enabled) {
        int result = authMapper.updateTwoFactorStatus(userId, enabled);

        if (result > 0) {
            log.info("2단계 인증 설정 변경: {} - {}", userId, enabled);
            return true;
        }

        return false;
    }

    // 계정 활동 내역 조회
    public Map<String, Object> getActivityHistory(String userId, int page, int size) {
        // 로그인 이력과 보안 이벤트를 합쳐서 반환
        Map<String, Object> loginHistory = getLoginHistory(userId, page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("activities", loginHistory.get("histories"));
        result.put("totalCount", loginHistory.get("totalCount"));
        result.put("currentPage", page);
        result.put("pageSize", size);
        result.put("totalPages", loginHistory.get("totalPages"));

        return result;
    }

    // 보안 점검 수행
    public Map<String, Object> performSecurityCheck(String userId) {
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

        // 비밀번호 강도 (25점) - 추후 구현
        score += 20; // 기본 점수
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
    }

    // 보안 이벤트 기록
    @Transactional
    public void recordSecurityEvent(String userId, String eventType, String ipAddress) {
        LoginHistoryVO securityEvent = new LoginHistoryVO();
        securityEvent.setUserId(userId);
        securityEvent.setIpAddress(ipAddress);
        securityEvent.setLoginSuccess(true);
        securityEvent.setFailureReason(eventType);
        securityEvent.setLoginTime(LocalDateTime.now());

        authMapper.insertLoginHistory(securityEvent);

        log.info("보안 이벤트 기록: {} - {} from {}", userId, eventType, ipAddress);
    }

    // 세션 검증
    public boolean validateSession(String userId, String sessionId) {
        String cachedSessionId = cacheService.get("session_" + userId, String.class);
        return sessionId.equals(cachedSessionId);
    }

    // 세션 생성
    public void createSession(String userId, String sessionId) {
        cacheService.put("session_" + userId, sessionId, 1800); // 30분
    }

    // 세션 삭제
    public void deleteSession(String userId) {
        cacheService.remove("session_" + userId);
    }

    // 만료된 토큰 정리
    @Transactional
    public void cleanupExpiredTokens() {
        authMapper.deleteExpiredTokens(null, null);
        log.info("만료된 토큰 정리 완료");
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
}