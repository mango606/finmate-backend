package com.example.finmate.common.util;

import java.security.SecureRandom;
import java.util.UUID;

public class ValidationUtils {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALPHANUMERIC = ALPHABET + NUMBERS;
    private static final SecureRandom random = new SecureRandom();

    // 이메일 정규식
    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    // 전화번호 정규식 (한국)
    private static final String PHONE_REGEX = "^01[0-9]-\\d{4}-\\d{4}$";

    // 사용자 ID 정규식 (영문, 숫자, 언더스코어)
    private static final String USER_ID_REGEX = "^[a-zA-Z0-9_]{4,20}$";

    // 이메일 유효성 검사
    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    // 전화번호 유효성 검사
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches(PHONE_REGEX);
    }

    // 사용자 ID 유효성 검사
    public static boolean isValidUserId(String userId) {
        return userId != null && userId.matches(USER_ID_REGEX);
    }

    // 비밀번호 유효성 검사 - 수정된 패턴 (영문자 + 숫자 또는 특수문자)
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 20) {
            return false;
        }

        // 영문자 포함 확인
        boolean hasLetter = false;
        boolean hasDigitOrSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c) || "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(String.valueOf(c))) {
                hasDigitOrSpecial = true;
            }
        }

        return hasLetter && hasDigitOrSpecial;
    }

    // 강력한 비밀번호 유효성 검사 (모든 요소 포함)
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 20) {
            return false;
        }

        // 각 조건 체크
        boolean hasLowerCase = false;
        boolean hasUpperCase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLowerCase = true;
            if (Character.isUpperCase(c)) hasUpperCase = true;
            if (Character.isDigit(c)) hasDigit = true;
            if ("!@#$%^&*()_+-=[]{}|;:,.<>?".contains(String.valueOf(c))) hasSpecial = true;
        }

        // 모든 조건을 만족해야 함
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

    // 비밀번호 강도 체크 (점수 기반)
    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) return 0;

        int score = 0;

        // 길이 점수 (최대 20점)
        if (password.length() >= 8) score += 10;
        if (password.length() >= 12) score += 10;

        // 문자 종류 체크
        boolean hasLowerCase = false;
        boolean hasUpperCase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLowerCase = true;
            if (Character.isUpperCase(c)) hasUpperCase = true;
            if (Character.isDigit(c)) hasDigit = true;
            if ("!@#$%^&*()_+-=[]{}|;:,.<>?".contains(String.valueOf(c))) hasSpecial = true;
        }

        // 문자 종류 점수
        if (hasLowerCase) score += 15;     // 소문자
        if (hasUpperCase) score += 15;     // 대문자
        if (hasDigit) score += 15;         // 숫자
        if (hasSpecial) score += 25;       // 특수문자

        return Math.min(score, 100);
    }

    // 비밀번호 강도 등급 반환
    public static String getPasswordStrengthGrade(String password) {
        int strength = getPasswordStrength(password);

        if (strength >= 80) return "STRONG";
        if (strength >= 60) return "MEDIUM";
        if (strength >= 40) return "WEAK";
        return "VERY_WEAK";
    }

    // 랜덤 문자열 생성
    public static String generateRandomString(int length) {
        return generateRandomString(length, ALPHANUMERIC);
    }

    public static String generateRandomString(int length, String characters) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }

    // UUID 생성
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    // UUID 생성 (하이픈 제거)
    public static String generateUUIDNoDash() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 임시 비밀번호 생성
    public static String generateTempPassword() {
        StringBuilder password = new StringBuilder();

        // 대문자 1개
        password.append(ALPHABET.charAt(random.nextInt(26)));
        // 소문자 2개
        password.append(ALPHABET.substring(26).charAt(random.nextInt(26)));
        password.append(ALPHABET.substring(26).charAt(random.nextInt(26)));
        // 숫자 2개
        password.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        password.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        // 특수문자 1개
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));
        // 나머지 2개는 영숫자
        password.append(generateRandomString(2, ALPHANUMERIC));

        // 문자열 섞기
        return shuffleString(password.toString());
    }

    // 문자열 섞기
    private static String shuffleString(String str) {
        char[] array = str.toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return new String(array);
    }
}