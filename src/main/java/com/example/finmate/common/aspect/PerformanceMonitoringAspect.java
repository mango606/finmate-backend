package com.example.finmate.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PerformanceMonitoringAspect {

    // Micrometer 사용하지 않고 로그 기반 모니터링
    @Around("execution(* com.example.finmate.*.service.*.*(..))")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("메서드 실행 시작: {}.{}", className, methodName);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("메서드 실행 완료: {}.{} ({}ms)", className, methodName, executionTime);

            // 성능 임계값 체크 (1초 이상)
            if (executionTime > 1000) {
                log.warn("성능 경고: {}.{} 메서드가 {}ms 소요됨", className, methodName, executionTime);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("메서드 실행 실패: {}.{} ({}ms) - {}", className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    // 로그인 성공 이벤트 처리 (이벤트 클래스가 있는 경우)
    @EventListener
    public void handleLoginEvent(Object event) {
        if (event != null) {
            log.info("로그인 이벤트 처리: {}", event.getClass().getSimpleName());
        }
    }

    // 메서드 실행 시간 통계
    public static class PerformanceStats {
        private final String method;
        private final long executionTime;
        private final boolean success;

        public PerformanceStats(String method, long executionTime, boolean success) {
            this.method = method;
            this.executionTime = executionTime;
            this.success = success;
        }

        public String getMethod() { return method; }
        public long getExecutionTime() { return executionTime; }
        public boolean isSuccess() { return success; }
    }
}