package com.example.finmate.security.handler;

import com.example.finmate.security.util.JsonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("접근 거부: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());

        JsonResponse.sendError(response, HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.");
    }
}