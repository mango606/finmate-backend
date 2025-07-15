package com.example.finmate.common.scheduler;

import com.example.finmate.common.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final CacheService cacheService;

    // 매일 새벽 2시에 임시 파일 정리
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanTempFiles() {
        log.info("임시 파일 정리 작업 시작");

        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            long deletedCount = deleteOldFiles(tempDir, 7); // 7일 이상 된 파일 삭제

            log.info("임시 파일 정리 완료: {}개 파일 삭제", deletedCount);
        } catch (Exception e) {
            log.error("임시 파일 정리 실패", e);
        }
    }

    // 매주 일요일 새벽 3시에 로그 파일 정리
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanLogFiles() {
        log.info("로그 파일 정리 작업 시작");

        try {
            File logDir = new File("logs");
            if (logDir.exists()) {
                long deletedCount = deleteOldFiles(logDir, 30); // 30일 이상 된 로그 삭제
                log.info("로그 파일 정리 완료: {}개 파일 삭제", deletedCount);
            }
        } catch (Exception e) {
            log.error("로그 파일 정리 실패", e);
        }
    }

    // 매 시간마다 시스템 상태 체크
    @Scheduled(fixedRate = 3600000) // 1시간 = 3600000ms
    public void systemHealthCheck() {
        log.debug("시스템 상태 체크: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // 메모리 사용량 체크
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100;

        if (memoryUsagePercent > 80) {
            log.warn("메모리 사용량이 높습니다: {:.2f}%", memoryUsagePercent);
        }

        // 캐시 상태 체크
        CacheService.CacheStats cacheStats = cacheService.getStats();
        log.debug("캐시 상태 - 크기: {}, 만료된 항목: {}", cacheStats.getSize(), cacheStats.getExpiredCount());
    }

    // 오래된 파일 삭제 헬퍼 메서드
    private long deleteOldFiles(File directory, int daysOld) {
        long deletedCount = 0;
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                } else if (file.isDirectory()) {
                    deletedCount += deleteOldFiles(file, daysOld);
                }
            }
        }

        return deletedCount;
    }
}