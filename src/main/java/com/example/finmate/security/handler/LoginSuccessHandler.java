package com.example.finmate.security.handler;

import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.mapper.MemberMapper;
import com.example.finmate.security.account.dto.AuthResultDTO;
import com.example.finmate.security.account.dto.UserInfoDTO;
import com.example.finmate.security.util.JsonResponse;
import com.example.finmate.security.util.JwtProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@Log4j2
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

        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setUserId(member.getUserId());
        userInfo.setUserName(member.getUserName());
        userInfo.setUserEmail(member.getUserEmail());
        userInfo.setUserPhone(member.getUserPhone());
        userInfo.setBirthDate(member.getBirthDate());
        userInfo.setGender(member.getGender());
        userInfo.setRegDate(member.getRegDate());
        userInfo.setAuthorities(user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList()));

        AuthResultDTO authResult = new AuthResultDTO(token, userInfo);

        JsonResponse.sendSuccess(response, authResult);
    }
}