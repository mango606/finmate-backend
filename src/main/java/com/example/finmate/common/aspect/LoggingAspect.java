package com.example.finmate.common.aspect;

import com.example.finmate.common.util.IPUtils;
import com.example.finmate.common.util.SecurityUtils;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Log4j2
@Aspect
@Component
public class LoggingAspect {

    // 컨트롤러 메서드 실행 로깅
    @Around("execution(* com.example.finmate.*.controller.*.*(..))")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // 요청 정보 추출
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String userInfo = "Anonymous";
        String clientIP = "Unknown";

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            clientIP = IPUtils.getClientIP(request);

            String currentUser = SecurityUtils.getCurrentUserId();
            if (currentUser != null) {
                userInfo = currentUser;
            }
        }

        log.info("=== API 호출 시작 ===");
        log.info("메서드: {}.{}", className, methodName);
        log.info("사용자: {}", userInfo);
        log.info("클라이언트 IP: {}", IPUtils.maskIP(clientIP));

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();

            log.info("=== API 호출 완료 ===");
            log.info("실행 시간: {}ms", (endTime - startTime));

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();

            log.error("=== API 호출 실패 ===");
            log.error("실행 시간: {}ms", (endTime - startTime));
            log.error("오류: {}", e.getMessage(), e);

            throw e;
        }
    }

    // 서비스 메서드 실행 로깅
    @Around("execution(* com.example.finmate.*.service.*.*(..))")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.debug("서비스 메서드 실행: {}.{}", className, methodName);

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error("서비스 메서드 실패: {}.{} - {}", className, methodName, e.getMessage());
            throw e;
        }
    }

    // 데이터베이스 접근 로깅
    @Before("execution(* com.example.finmate.*.mapper.*.*(..))")
    public void logMapper(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.debug("DB 접근: {}.{}", className, methodName);
    }

    // 예외 발생 로깅
    @AfterThrowing(pointcut = "execution(* com.example.finmate..*.*(..))", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.error("예외 발생: {}.{}", className, methodName);
        log.error("예외 내용: {}", exception.getMessage(), exception);
    }
}