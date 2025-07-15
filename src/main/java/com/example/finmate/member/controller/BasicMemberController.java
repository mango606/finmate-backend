package com.example.finmate.member.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/member")
public class BasicMemberController {

    // 서버 상태 확인용 API
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "FinMate 서버가 정상 동작 중입니다.");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    // 사용자 ID 중복 체크 (기본 버전)
    @GetMapping("/checkUserId/{userId}")
    public ResponseEntity<Boolean> checkUserIdDuplicate(@PathVariable String userId) {
        log.info("사용자 ID 중복 체크: {}", userId);

        // 임시로 admin, user, test는 중복으로 처리
        boolean isDuplicate = "admin".equals(userId) || "user".equals(userId) || "test".equals(userId);

        return ResponseEntity.ok(isDuplicate);
    }

    // 이메일 중복 체크 (기본 버전)
    @GetMapping("/checkEmail")
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam String userEmail) {
        log.info("이메일 중복 체크: {}", userEmail);

        // 임시로 admin@finmate.com은 중복으로 처리
        boolean isDuplicate = "admin@finmate.com".equals(userEmail);

        return ResponseEntity.ok(isDuplicate);
    }

    // API 정보 조회
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "FinMate Backend API");
        info.put("version", "1.0.0");
        info.put("developer", "회원 관리 담당자");
        info.put("features", new String[]{"로그인", "로그아웃", "회원가입", "회원정보 수정"});

        return ResponseEntity.ok(info);
    }
}