package com.example.finmate.common.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    // 이메일 정규식
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // 전화번호 정규식 (한국)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^01[0-9]-\\d{4}-\\d{4}$"
    );

    // 사용자 ID 정규식 (영문, 숫자, 언더스코어)
    private static final Pattern USER_ID_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{4,20}$"
    );

    // 이메일 유효성 검사
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    // 전화번호 유효성 검사
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    // 사용자 ID 유효성 검사
    public static boolean isValidUserId(String userId) {
        return userId != null && USER_ID_PATTERN.matcher(userId).matches();
    }

    // 비밀번호 유효성 검사
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 20) {
            return false;
        }

        // 각 조건 체크
        boolean hasLowerCase = password.matches(".*[a-z].*");     // 소문자 포함
        boolean hasUpperCase = password.matches(".*[A-Z].*");     // 대문자 포함
        boolean hasDigit = password.matches(".*\\d.*");           // 숫자 포함
        boolean hasSpecial = password.matches(".*[@$!%*?&].*");   // 특수문자 포함

        // 모든 조건을 만족해야 함 (대소문자, 숫자, 특수문자)
        return hasLowerCase && hasUpperCase && hasDigit && hasSpecial;
    }

    // 문자열 길이 검사
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) return false;
        int length = str.trim().length();
        return length >= minLength && length <= maxLength;
    }

    // 빈 문자열 검사
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    // 숫자인지 검사
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // 정수인지 검사
    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // 한글만 포함되어 있는지 검사
    public static boolean isKorean(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("^[가-힣\\s]+$");
    }

    // 영문만 포함되어 있는지 검사
    public static boolean isEnglish(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("^[a-zA-Z\\s]+$");
    }
}