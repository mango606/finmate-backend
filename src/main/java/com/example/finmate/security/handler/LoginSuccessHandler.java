package com.example.finmate.security.handler;

import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.mapper.MemberMapper;
import com.example.finmate.security.util.JsonResponse;
import com.example.finmate.security.util.JwtProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProcessor jwtProcessor;
    private final MemberMapper memberMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();

        log.info("로그인 성공: {}", userId);

        // JWT 토큰 생성
        String token = jwtProcessor.generateToken(userId);

        // 사용자 정보 조회
        MemberVO member = memberMapper.getMemberByUserId(userId);

        // 사용자 정보를 Map으로 구성
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", member.getUserId());
        userInfo.put("userName", member.getUserName());
        userInfo.put("userEmail", member.getUserEmail());
        userInfo.put("userPhone", member.getUserPhone());
        userInfo.put("birthDate", member.getBirthDate());
        userInfo.put("gender", member.getGender());
        userInfo.put("regDate", member.getRegDate());
        userInfo.put("authorities", user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList()));

        // 인증 결과를 Map으로 구성
        Map<String, Object> authResult = new HashMap<>();
        authResult.put("token", token);
        authResult.put("user", userInfo);

        JsonResponse.sendSuccess(response, authResult);
    }
}