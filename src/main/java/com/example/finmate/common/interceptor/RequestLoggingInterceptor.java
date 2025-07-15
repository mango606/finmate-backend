package com.example.finmate.common.interceptor;

import com.example.finmate.common.util.IPUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Log4j2
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIP = IPUtils.getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        // 정적 리소스는 로깅하지 않음
        if (uri.startsWith("/resources/") || uri.startsWith("/css/") ||
                uri.startsWith("/js/") || uri.startsWith("/images/") ||
                uri.endsWith(".ico") || uri.endsWith(".png") ||
                uri.endsWith(".jpg") || uri.endsWith(".css") || uri.endsWith(".js")) {
            return true;
        }

        log.info("HTTP 요청: {} {} from {} [{}]",
                method, uri, IPUtils.maskIP(clientIP),
                userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 100)) : "Unknown");

        // 요청 시작 시간 저장
        request.setAttribute("startTime", System.currentTimeMillis());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long endTime = System.currentTimeMillis();
            long executeTime = endTime - startTime;

            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            // 정적 리소스는 로깅하지 않음
            if (uri.startsWith("/resources/") || uri.startsWith("/css/") ||
                    uri.startsWith("/js/") || uri.startsWith("/images/") ||
                    uri.endsWith(".ico") || uri.endsWith(".png") ||
                    uri.endsWith(".jpg") || uri.endsWith(".css") || uri.endsWith(".js")) {
                return;
            }

            if (ex != null) {
                log.warn("HTTP 응답: {} {} - {} ({}ms) [예외: {}]",
                        method, uri, status, executeTime, ex.getMessage());
            } else {
                log.info("HTTP 응답: {} {} - {} ({}ms)", method, uri, status, executeTime);
            }
        }
    }
}