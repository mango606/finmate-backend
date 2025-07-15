package com.example.finmate.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {

    private static final SecureRandom secureRandom = new SecureRandom();

    // 현재 인증된 사용자 ID 가져오기
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {

            if (authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                return user.getUsername();
            } else {
                return authentication.getName();
            }
        }
        return null;
    }

    // 현재 사용자가 인증되었는지 확인
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

    // 현재 사용자가 특정 권한을 가지고 있는지 확인
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role) ||
                            authority.getAuthority().equals(role));
        }
        return false;
    }

    // SHA-256 해시 생성
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 해시 생성 실패", e);
        }
    }

    // 랜덤 솔트 생성
    public static String generateSalt() {
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // 안전한 랜덤 토큰 생성
    public static String generateSecureToken(int length) {
        byte[] token = new byte[length];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    // XSS 방지를 위한 HTML 이스케이프
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }

        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    // SQL 인젝션 방지를 위한 문자열 검증
    public static boolean containsSqlInjection(String input) {
        if (input == null) {
            return false;
        }

        String lowerInput = input.toLowerCase();
        String[] sqlKeywords = {
                "select", "insert", "update", "delete", "drop", "create", "alter",
                "union", "script", "javascript", "vbscript", "onload", "onerror"
        };

        for (String keyword : sqlKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}