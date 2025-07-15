package com.example.finmate.common.util;

import java.security.SecureRandom;
import java.util.UUID;

public class StringUtils {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALPHANUMERIC = ALPHABET + NUMBERS;
    private static final SecureRandom random = new SecureRandom();

    // 빈 문자열 확인
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // 비어있지 않은 문자열 확인
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    // 문자열 마스킹 (이메일)
    public static String maskEmail(String email) {
        if (isEmpty(email) || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.length() <= 3) {
            return localPart.charAt(0) + "***@" + domainPart;
        } else {
            return localPart.substring(0, 3) + "***@" + domainPart;
        }
    }

    // 문자열 마스킹 (전화번호)
    public static String maskPhone(String phone) {
        if (isEmpty(phone)) {
            return phone;
        }

        if (phone.length() == 13 && phone.contains("-")) {
            // 010-1234-5678 -> 010-****-5678
            return phone.substring(0, 4) + "****" + phone.substring(8);
        }

        return phone;
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

    // 카멜케이스를 스네이크케이스로 변환
    public static String camelToSnake(String camelCase) {
        if (isEmpty(camelCase)) {
            return camelCase;
        }

        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    // 스네이크케이스를 카멜케이스로 변환
    public static String snakeToCamel(String snakeCase) {
        if (isEmpty(snakeCase)) {
            return snakeCase;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = snakeCase.split("_");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                result.append(part.toLowerCase());
            } else {
                result.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    // 문자열 앞뒤 공백 제거 (null safe)
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    // 문자열 길이 제한
    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}