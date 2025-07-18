package com.example.finmate.common.util;

import com.example.finmate.member.domain.MemberVO;
import org.springframework.security.core.userdetails.User;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AuthResponseUtil {

    // MemberVO와 User 정보를 사용하여 사용자 정보 Map을 생성
    public static Map<String, Object> createUserInfoMap(MemberVO member, User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", member.getUserId());
        userInfo.put("userName", member.getUserName());
        userInfo.put("userEmail", member.getUserEmail());
        userInfo.put("userPhone", member.getUserPhone());
        userInfo.put("birthDate", member.getBirthDate());
        userInfo.put("gender", member.getGender());
        userInfo.put("regDate", member.getRegDate());

        if (user != null) {
            userInfo.put("authorities", user.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .collect(Collectors.toList()));
        }

        return userInfo;
    }

    // MemberVO만 사용하여 사용자 정보 Map을 생성
    public static Map<String, Object> createUserInfoMap(MemberVO member) {
        return createUserInfoMap(member, null);
    }

    // 인증 결과 Map을 생성 (토큰 + 사용자 정보)
    public static Map<String, Object> createAuthResultMap(String token, Map<String, Object> userInfo) {
        Map<String, Object> authResult = new HashMap<>();
        authResult.put("token", token);
        authResult.put("user", userInfo);
        return authResult;
    }

    // 완전한 인증 결과 Map을 생성
    public static Map<String, Object> createAuthResultMap(String token, MemberVO member, User user) {
        Map<String, Object> userInfo = createUserInfoMap(member, user);
        return createAuthResultMap(token, userInfo);
    }
}