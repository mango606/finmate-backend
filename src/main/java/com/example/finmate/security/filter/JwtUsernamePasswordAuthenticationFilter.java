package com.example.finmate.security.filter;

import com.example.finmate.member.dto.MemberLoginDTO;
import com.example.finmate.security.handler.LoginFailureHandler;
import com.example.finmate.security.handler.LoginSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class JwtUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager,
                                                   LoginSuccessHandler successHandler,
                                                   LoginFailureHandler failureHandler) {
        super(authenticationManager);
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        try {
            MemberLoginDTO loginDTO = objectMapper.readValue(request.getInputStream(), MemberLoginDTO.class);

            log.info("로그인 시도: {}", loginDTO.getUserId());

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginDTO.getUserId(), loginDTO.getUserPassword());

            return getAuthenticationManager().authenticate(authToken);

        } catch (IOException e) {
            log.error("로그인 요청 파싱 실패", e);
            throw new RuntimeException("로그인 요청을 처리할 수 없습니다", e);
        }
    }
}