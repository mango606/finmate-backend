package com.example.finmate.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("수정된 유효성 검사 유틸리티 테스트")
class ValidationUtilsTest {

    @Test
    @DisplayName("이메일 유효성 검사")
    void isValidEmail() {
        // 유효한 이메일
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name@domain.co.kr"));
        assertTrue(ValidationUtils.isValidEmail("test123+tag@gmail.com"));

        // 유효하지 않은 이메일
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(ValidationUtils.isValidEmail("@domain.com"));
        assertFalse(ValidationUtils.isValidEmail("test@"));
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test
    @DisplayName("전화번호 유효성 검사")
    void isValidPhone() {
        // 유효한 전화번호
        assertTrue(ValidationUtils.isValidPhone("010-1234-5678"));
        assertTrue(ValidationUtils.isValidPhone("011-9876-5432"));
        assertTrue(ValidationUtils.isValidPhone("016-1111-2222"));

        // 유효하지 않은 전화번호
        assertFalse(ValidationUtils.isValidPhone("010-123-5678"));
        assertFalse(ValidationUtils.isValidPhone("010-1234-567"));
        assertFalse(ValidationUtils.isValidPhone("020-1234-5678"));
        assertFalse(ValidationUtils.isValidPhone("01012345678"));
        assertFalse(ValidationUtils.isValidPhone(null));
    }

    @Test
    @DisplayName("사용자 ID 유효성 검사")
    void isValidUserId() {
        // 유효한 사용자 ID
        assertTrue(ValidationUtils.isValidUserId("user123"));
        assertTrue(ValidationUtils.isValidUserId("test_user"));
        assertTrue(ValidationUtils.isValidUserId("User_123"));

        // 유효하지 않은 사용자 ID
        assertFalse(ValidationUtils.isValidUserId("usr")); // 너무 짧음
        assertFalse(ValidationUtils.isValidUserId("user@123")); // 특수문자
        assertFalse(ValidationUtils.isValidUserId("한글사용자")); // 한글
        assertFalse(ValidationUtils.isValidUserId("user-name")); // 하이픈
        assertFalse(ValidationUtils.isValidUserId(null));
    }

    @Test
    @DisplayName("비밀번호 유효성 검사 - 수정된 패턴")
    void isValidPassword() {
        // 유효한 비밀번호 (영문자 + 숫자 또는 특수문자)
        assertTrue(ValidationUtils.isValidPassword("Test123!")); // 대소문자+숫자+특수문자
        assertTrue(ValidationUtils.isValidPassword("test123!")); // 소문자+숫자+특수문자
        assertTrue(ValidationUtils.isValidPassword("TEST123!")); // 대문자+숫자+특수문자
        assertTrue(ValidationUtils.isValidPassword("Test123")); // 대소문자+숫자
        assertTrue(ValidationUtils.isValidPassword("test123")); // 소문자+숫자
        assertTrue(ValidationUtils.isValidPassword("TEST123")); // 대문자+숫자
        assertTrue(ValidationUtils.isValidPassword("TestPass!")); // 대소문자+특수문자
        assertTrue(ValidationUtils.isValidPassword("testpass!")); // 소문자+특수문자
        assertTrue(ValidationUtils.isValidPassword("TESTPASS!")); // 대문자+특수문자

        // 유효하지 않은 비밀번호
        assertFalse(ValidationUtils.isValidPassword("TestPass")); // 영문자만
        assertFalse(ValidationUtils.isValidPassword("123456789")); // 숫자만
        assertFalse(ValidationUtils.isValidPassword("!@#$%^&*")); // 특수문자만
        assertFalse(ValidationUtils.isValidPassword("Test1!")); // 너무 짧음
        assertFalse(ValidationUtils.isValidPassword("Test123!@#$%^&*()Test123!@#$%^&*()")); // 너무 김
        assertFalse(ValidationUtils.isValidPassword(null));
    }

    @Test
    @DisplayName("강력한 비밀번호 유효성 검사")
    void isStrongPassword() {
        // 강력한 비밀번호 (모든 요소 포함)
        assertTrue(ValidationUtils.isStrongPassword("Test123!")); // 대소문자+숫자+특수문자
        assertTrue(ValidationUtils.isStrongPassword("MyPass123$")); // 대소문자+숫자+특수문자
        assertTrue(ValidationUtils.isStrongPassword("Secure*123")); // 대소문자+숫자+특수문자

        // 약한 비밀번호
        assertFalse(ValidationUtils.isStrongPassword("test123!")); // 대문자 없음
        assertFalse(ValidationUtils.isStrongPassword("TEST123!")); // 소문자 없음
        assertFalse(ValidationUtils.isStrongPassword("TestPass!")); // 숫자 없음
        assertFalse(ValidationUtils.isStrongPassword("Test123")); // 특수문자 없음
    }

    @Test
    @DisplayName("비밀번호 강도 점수 테스트")
    void getPasswordStrength() {
        // 강한 비밀번호
        assertTrue(ValidationUtils.getPasswordStrength("Test123!@#") >= 80);

        // 중간 비밀번호
        int mediumScore = ValidationUtils.getPasswordStrength("Test123");
        assertTrue(mediumScore >= 40 && mediumScore < 80);

        // 약한 비밀번호
        assertTrue(ValidationUtils.getPasswordStrength("test123") < 60);

        // 매우 약한 비밀번호
        assertTrue(ValidationUtils.getPasswordStrength("test") < 40);
    }

    @Test
    @DisplayName("비밀번호 강도 등급 테스트")
    void getPasswordStrengthGrade() {
        assertEquals("STRONG", ValidationUtils.getPasswordStrengthGrade("Test123!@#"));
        assertEquals("MEDIUM", ValidationUtils.getPasswordStrengthGrade("Test123"));
        assertEquals("WEAK", ValidationUtils.getPasswordStrengthGrade("test123"));
        assertEquals("VERY_WEAK", ValidationUtils.getPasswordStrengthGrade("test"));
    }

    @Test
    @DisplayName("문자열 길이 검사")
    void isValidLength() {
        assertTrue(ValidationUtils.isValidLength("test", 2, 10));
        assertTrue(ValidationUtils.isValidLength("hello world", 5, 20));

        assertFalse(ValidationUtils.isValidLength("a", 2, 10)); // 너무 짧음
        assertFalse(ValidationUtils.isValidLength("very long string", 2, 10)); // 너무 김
        assertFalse(ValidationUtils.isValidLength(null, 2, 10));
    }

    @Test
    @DisplayName("빈 문자열 검사")
    void isNotEmpty() {
        assertTrue(ValidationUtils.isNotEmpty("test"));
        assertTrue(ValidationUtils.isNotEmpty("  hello  "));

        assertFalse(ValidationUtils.isNotEmpty(""));
        assertFalse(ValidationUtils.isNotEmpty("   "));
        assertFalse(ValidationUtils.isNotEmpty(null));
    }

    @Test
    @DisplayName("숫자 검사")
    void isNumeric() {
        assertTrue(ValidationUtils.isNumeric("123"));
        assertTrue(ValidationUtils.isNumeric("123.45"));
        assertTrue(ValidationUtils.isNumeric("-123"));

        assertFalse(ValidationUtils.isNumeric("abc"));
        assertFalse(ValidationUtils.isNumeric("123abc"));
        assertFalse(ValidationUtils.isNumeric(""));
        assertFalse(ValidationUtils.isNumeric(null));
    }

    @Test
    @DisplayName("정수 검사")
    void isInteger() {
        assertTrue(ValidationUtils.isInteger("123"));
        assertTrue(ValidationUtils.isInteger("-123"));

        assertFalse(ValidationUtils.isInteger("123.45"));
        assertFalse(ValidationUtils.isInteger("abc"));
        assertFalse(ValidationUtils.isInteger(""));
        assertFalse(ValidationUtils.isInteger(null));
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + ", but was: " + actual);
        }
    }
}