package com.example.finmate.common.scheduler;

import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.auth.mapper.AuthMapper;
import com.example.finmate.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountMaintenanceScheduler {

    private final AuthMapper authMapper;
    private final AuthService authService;

    // 5분마다 만료된 계정 잠금 자동 해제
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void autoUnlockExpiredAccounts() {
        try {
            List<AccountSecurityVO> expiredLockedAccounts = authMapper.getExpiredLockedAccounts(30);

            for (AccountSecurityVO account : expiredLockedAccounts) {
                if (account.getLockTime() != null) {
                    LocalDateTime unlockTime = account.getLockTime().plusMinutes(30);

                    if (LocalDateTime.now().isAfter(unlockTime)) {
                        boolean success = authService.unlockAccount(account.getUserId());
                        if (success) {
                            log.info("자동 계정 잠금 해제: {}", account.getUserId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("자동 계정 잠금 해제 실패", e);
        }
    }

    // 매일 새벽 3시에 오래된 로그인 이력 정리
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldLoginHistory() {
        try {
            // 90일 이상 된 로그인 이력 삭제 (실제 구현에서는 별도 메서드 필요)
            log.info("오래된 로그인 이력 정리 시작");
            // authMapper.deleteOldLoginHistory(90);
            log.info("오래된 로그인 이력 정리 완료");
        } catch (Exception e) {
            log.error("오래된 로그인 이력 정리 실패", e);
        }
    }

    // 매일 자정에 보안 통계 업데이트
    @Scheduled(cron = "0 0 0 * * ?")
    public void updateSecurityStatistics() {
        try {
            log.info("보안 통계 업데이트 시작");
            // 계정 잠금 통계, 로그인 실패 통계 등 업데이트
            // authMapper.updateDailySecurityStats();
            log.info("보안 통계 업데이트 완료");
        } catch (Exception e) {
            log.error("보안 통계 업데이트 실패", e);
        }
    }
}