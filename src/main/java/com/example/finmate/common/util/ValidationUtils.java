package com.example.finmate.common.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    // 이메일 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // 전화번호 패턴 (010-1234-5678 형식)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^01[0-9]-\\d{4}-\\d{4}$"
    );

    // 사용자 ID 패턴 (4-20자, 영문/숫자/언더스코어)
    private static final Pattern USER_ID_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{4,20}$"
    );

    // 비밀번호 패턴 (8-20자, 영문자+숫자 또는 영문자+숫자+특수문자)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-zA-Z])(?=.*[\\d@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$"
    );

    // 강력한 비밀번호 패턴 (대문자+소문자+숫자+특수문자)
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$"
    );

    // 숫자 패턴
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "^-?\\d+(\\.\\d+)?$"
    );

    // 정수 패턴
    private static final Pattern INTEGER_PATTERN = Pattern.compile(
            "^-?\\d+$"
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
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    // 강력한 비밀번호 유효성 검사
    public static boolean isStrongPassword(String password) {
        return password != null && STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    // 문자열 길이 검사
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) return false;
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    // 빈 문자열 검사 (null, 빈 문자열, 공백만 있는 문자열 체크)
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    // 숫자 검사
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        return NUMERIC_PATTERN.matcher(str).matches();
    }

    // 정수 검사
    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) return false;
        return INTEGER_PATTERN.matcher(str).matches();
    }

    // 비밀번호 강도 점수 계산 (0-100점)
    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // 길이 점수 (최대 20점)
        if (password.length() >= 8) {
            score += Math.min(20, password.length() * 2);
        }

        // 대문자 포함 (15점)
        if (password.matches(".*[A-Z].*")) {
            score += 15;
        }

        // 소문자 포함 (15점)
        if (password.matches(".*[a-z].*")) {
            score += 15;
        }

        // 숫자 포함 (15점)
        if (password.matches(".*\\d.*")) {
            score += 15;
        }

        // 특수문자 포함 (25점)
        if (password.matches(".*[@$!%*?&].*")) {
            score += 25;
        }

        // 다양한 문자 조합 보너스 (10점)
        int charTypes = 0;
        if (password.matches(".*[A-Z].*")) charTypes++;
        if (password.matches(".*[a-z].*")) charTypes++;
        if (password.matches(".*\\d.*")) charTypes++;
        if (password.matches(".*[@$!%*?&].*")) charTypes++;

        if (charTypes >= 3) {
            score += 10;
        }

        return Math.min(100, score);
    }

    // 비밀번호 강도 등급 반환
    public static String getPasswordStrengthGrade(String password) {
        int score = getPasswordStrength(password);

        if (score >= 80) {
            return "STRONG";
        } else if (score >= 60) {
            return "MEDIUM";
        } else if (score >= 40) {
            return "WEAK";
        } else {
            return "VERY_WEAK";
        }
    }
}