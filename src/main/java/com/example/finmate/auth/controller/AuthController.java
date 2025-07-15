package com.example.finmate.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Api(tags = "인증 관리 API")
public class AuthController {

    @ApiOperation(value = "로그아웃", notes = "현재 로그인한 사용자를 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(Authentication authentication) {
        if (authentication != null) {
            String userId = authentication.getName();
            log.info("로그아웃: {}", userId);
        }

        // JWT 토큰 기반이므로 서버에서는 단순히 클라이언트에게 토큰 삭제를 알림
        SecurityContextHolder.clearContext();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그아웃되었습니다.");

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "토큰 유효성 확인", notes = "현재 토큰의 유효성을 확인합니다.")
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()) {
            response.put("success", true);
            response.put("valid", true);
            response.put("userId", authentication.getName());
            response.put("authorities", authentication.getAuthorities());
        } else {
            response.put("success", true);
            response.put("valid", false);
            response.put("message", "토큰이 유효하지 않습니다.");
        }

        return ResponseEntity.ok(response);
    }
}