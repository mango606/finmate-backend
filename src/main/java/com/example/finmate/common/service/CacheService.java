package com.example.finmate.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    // 캐시 통계
    public CacheStats getStats() {
        int size = cache.size();
        long expiredCount = cache.values().stream()
                .mapToLong(item -> item.isExpired() ? 1 : 0)
                .sum();

        return new CacheStats(size, expiredCount);
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

    // 캐시 통계 클래스
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
}