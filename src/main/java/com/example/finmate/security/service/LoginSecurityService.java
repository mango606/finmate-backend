package com.example.finmate.security.service;

import com.example.finmate.common.service.CacheService;
import com.example.finmate.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityService {

    private final CacheService cacheService;
    private final EmailService emailService;

    // 로그인 시도 제한 (IP 기반)
    public boolean isLoginAttemptAllowed(String ipAddress) {
        String key = "login_attempts_" + ipAddress;
        Integer attempts = cacheService.get(key, Integer.class);

        if (attempts == null) {
            attempts = 0;
        }

        boolean allowed = attempts < 5; // 5회 제한
        log.debug("로그인 시도 확인: {} - 시도 횟수: {}, 허용: {}", ipAddress, attempts, allowed);

        return allowed;
    }

    // 로그인 실패 시 시도 횟수 증가
    public void recordLoginFailure(String ipAddress) {
        String key = "login_attempts_" + ipAddress;
        Integer attempts = cacheService.get(key, Integer.class);

        if (attempts == null) {
            attempts = 0;
        }

        attempts++;
        cacheService.put(key, attempts, 1800); // 30분간 유지

        log.warn("로그인 실패 기록: {} - 시도 횟수: {}", ipAddress, attempts);

        if (attempts >= 5) {
            log.warn("IP 주소 로그인 시도 한도 초과: {} ({}회)", ipAddress, attempts);
            // 필요시 관리자에게 알림 이메일 발송
            notifyAdminOfSuspiciousActivity(ipAddress, attempts);
        }
    }

    // 로그인 성공 시 시도 횟수 초기화
    public void resetLoginAttempts(String ipAddress) {
        String key = "login_attempts_" + ipAddress;
        cacheService.remove(key);
        log.debug("로그인 시도 횟수 초기화: {}", ipAddress);
    }

    // 의심스러운 로그인 감지
    public boolean isSuspiciousLogin(String userId, String ipAddress, String userAgent) {
        // 최근 로그인 정보 확인
        String lastIpKey = "last_login_ip_" + userId;
        String lastUserAgentKey = "last_login_ua_" + userId;

        String lastIp = cacheService.get(lastIpKey, String.class);
        String lastUserAgent = cacheService.get(lastUserAgentKey, String.class);

        boolean suspicious = false;

        // IP 주소가 다른 경우
        if (lastIp != null && !lastIp.equals(ipAddress)) {
            suspicious = true;
            log.info("의심스러운 로그인 감지: {} - IP 변경 ({} -> {})", userId, lastIp, ipAddress);
        }

        // User Agent가 다른 경우
        if (lastUserAgent != null && !lastUserAgent.equals(userAgent)) {
            suspicious = true;
            log.info("의심스러운 로그인 감지: {} - User Agent 변경", userId);
        }

        // 정보 업데이트
        cacheService.put(lastIpKey, ipAddress, 86400); // 24시간
        cacheService.put(lastUserAgentKey, userAgent, 86400);

        return suspicious;
    }

    // 특정 IP 주소 차단
    public void blockIpAddress(String ipAddress, long blockDurationSeconds) {
        String key = "blocked_ip_" + ipAddress;
        cacheService.put(key, true, blockDurationSeconds);
        log.warn("IP 주소 차단: {} ({}초)", ipAddress, blockDurationSeconds);
    }

    // IP 주소 차단 여부 확인
    public boolean isIpBlocked(String ipAddress) {
        String key = "blocked_ip_" + ipAddress;
        Boolean blocked = cacheService.get(key, Boolean.class);
        return blocked != null && blocked;
    }

    // IP 주소 차단 해제
    public void unblockIpAddress(String ipAddress) {
        String key = "blocked_ip_" + ipAddress;
        cacheService.remove(key);
        log.info("IP 주소 차단 해제: {}", ipAddress);
    }

    // 로그인 시도 통계 조회
    public Map<String, Integer> getLoginAttemptStats() {
        Map<String, Integer> stats = new HashMap<>();

        // 캐시에서 로그인 시도 관련 키들을 찾아서 통계 생성
        // 실제 구현에서는 캐시의 키 패턴을 통해 통계를 생성할 수 있음

        return stats;
    }

    // 관리자에게 의심스러운 활동 알림
    private void notifyAdminOfSuspiciousActivity(String ipAddress, int attempts) {
        try {
            String adminEmail = "admin@finmate.com"; // 설정에서 가져오는 것이 좋음
            String subject = "FinMate 보안 알림: 의심스러운 로그인 시도";
            String message = String.format(
                    "IP 주소 %s에서 %d회의 로그인 실패가 발생했습니다.\n" +
                            "보안상 해당 IP를 일시적으로 차단했습니다.\n" +
                            "시간: %s",
                    ipAddress, attempts, java.time.LocalDateTime.now()
            );

            // 이메일 발송은 별도 스레드에서 처리하여 성능에 영향을 주지 않도록 함
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendEmail(adminEmail, subject, message);
                } catch (Exception e) {
                    log.error("관리자 알림 이메일 발송 실패", e);
                }
            });

        } catch (Exception e) {
            log.error("관리자 알림 처리 실패", e);
        }
    }

    // 로그인 보안 설정 조회
    public SecuritySettings getSecuritySettings() {
        return new SecuritySettings(
                5,    // 최대 로그인 시도 횟수
                1800, // 차단 시간 (30분)
                true, // 의심스러운 로그인 감지 활성화
                true  // 관리자 알림 활성화
        );
    }

    // 로그인 보안 설정 클래스
    public static class SecuritySettings {
        private final int maxLoginAttempts;
        private final long blockDurationSeconds;
        private final boolean suspiciousLoginDetection;
        private final boolean adminNotification;

        public SecuritySettings(int maxLoginAttempts, long blockDurationSeconds,
                                boolean suspiciousLoginDetection, boolean adminNotification) {
            this.maxLoginAttempts = maxLoginAttempts;
            this.blockDurationSeconds = blockDurationSeconds;
            this.suspiciousLoginDetection = suspiciousLoginDetection;
            this.adminNotification = adminNotification;
        }

        public int getMaxLoginAttempts() { return maxLoginAttempts; }
        public long getBlockDurationSeconds() { return blockDurationSeconds; }
        public boolean isSuspiciousLoginDetection() { return suspiciousLoginDetection; }
        public boolean isAdminNotification() { return adminNotification; }
    }
}