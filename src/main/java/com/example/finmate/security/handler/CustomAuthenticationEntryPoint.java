package com.example.finmate.security.handler;

import com.example.finmate.security.util.JsonResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Log4j2
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.warn("인증 필요: {} - {}", request.getRequestURI(), authException.getMessage());

        JsonResponse.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다. 로그인 후 다시 시도해주세요.");
    }
}