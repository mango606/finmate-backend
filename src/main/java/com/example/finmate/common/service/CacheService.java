package com.example.finmate.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CacheService {

    private final ConcurrentHashMap<String, CacheItem> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public CacheService() {
        // 만료된 캐시 정리 (1분마다)
        scheduler.scheduleAtFixedRate(this::cleanExpiredCache, 1, 1, TimeUnit.MINUTES);
    }

    // 캐시에 데이터 저장
    public void put(String key, Object value, long ttlSeconds) {
        long expirationTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        cache.put(key, new CacheItem(value, expirationTime));
        log.debug("캐시 저장: {} (TTL: {}초)", key, ttlSeconds);
    }

    // 캐시에서 데이터 조회
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        CacheItem item = cache.get(key);
        if (item == null) {
            log.debug("캐시 미스: {}", key);
            return null;
        }

        if (item.isExpired()) {
            cache.remove(key);
            log.debug("캐시 만료 제거: {}", key);
            return null;
        }

        log.debug("캐시 히트: {}", key);
        return (T) item.getValue();
    }

    // 캐시에서 데이터 제거
    public void remove(String key) {
        cache.remove(key);
        log.debug("캐시 제거: {}", key);
    }

    // 모든 캐시 제거
    public void clear() {
        cache.clear();
        log.info("모든 캐시 제거");
    }

    // 기본 캐시 통계
    public CacheStats getStats() {
        int size = cache.size();
        long expiredCount = cache.values().stream()
                .mapToLong(item -> item.isExpired() ? 1 : 0)
                .sum();

        return new CacheStats(size, expiredCount);
    }

    // 상세 캐시 통계
    public DetailedCacheStats getDetailedStats() {
        int totalSize = cache.size();
        int expiredCount = 0;
        int validCount = 0;
        long totalMemoryUsage = 0;

        for (Map.Entry<String, CacheItem> entry : cache.entrySet()) {
            CacheItem item = entry.getValue();
            if (item.isExpired()) {
                expiredCount++;
            } else {
                validCount++;
            }

            // 메모리 사용량 추정
            totalMemoryUsage += estimateObjectSize(item.getValue());
        }

        return new DetailedCacheStats(totalSize, expiredCount, validCount, totalMemoryUsage);
    }

    // 캐시 키 패턴별 조회
    public Map<String, Integer> getCacheStatsByPattern() {
        Map<String, Integer> stats = new HashMap<>();

        for (String key : cache.keySet()) {
            String pattern = extractPattern(key);
            stats.put(pattern, stats.getOrDefault(pattern, 0) + 1);
        }

        return stats;
    }

    // 캐시 워밍업
    public void warmUpCache() {
        log.info("캐시 워밍업 시작");

        // 자주 사용되는 데이터 미리 로드
        // 예: 시스템 설정, 코드 테이블 등

        log.info("캐시 워밍업 완료");
    }

    // 만료된 캐시 정리
    private void cleanExpiredCache() {
        int beforeSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int afterSize = cache.size();

        if (beforeSize != afterSize) {
            log.debug("만료된 캐시 정리: {} -> {} ({}개 제거)", beforeSize, afterSize, (beforeSize - afterSize));
        }
    }

    private long estimateObjectSize(Object obj) {
        // 객체 크기 추정 (간단한 구현)
        if (obj == null) return 0;
        if (obj instanceof String) return ((String) obj).length() * 2;
        return 100; // 기본값
    }

    private String extractPattern(String key) {
        // 키에서 패턴 추출 (예: "user_123" -> "user_*")
        String[] parts = key.split("_");
        if (parts.length > 1) {
            return parts[0] + "_*";
        }
        return key;
    }

    // 캐시 아이템 클래스
    private static class CacheItem {
        private final Object value;
        private final long expirationTime;

        public CacheItem(Object value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    // 기본 캐시 통계 클래스
    public static class CacheStats {
        private final int size;
        private final long expiredCount;

        public CacheStats(int size, long expiredCount) {
            this.size = size;
            this.expiredCount = expiredCount;
        }

        public int getSize() { return size; }
        public long getExpiredCount() { return expiredCount; }
    }

    // 상세 캐시 통계 클래스
    public static class DetailedCacheStats extends CacheStats {
        private final int validCount;
        private final long totalMemoryUsage;

        public DetailedCacheStats(int size, long expiredCount, int validCount, long totalMemoryUsage) {
            super(size, expiredCount);
            this.validCount = validCount;
            this.totalMemoryUsage = totalMemoryUsage;
        }

        public int getValidCount() { return validCount; }
        public long getTotalMemoryUsage() { return totalMemoryUsage; }
    }
}