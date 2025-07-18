package com.example.finmate.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("수정된 유효성 검사 유틸리티 테스트")
class ValidationUtilsTest {

    @Test
    @DisplayName("이메일 유효성 검사")
    void isValidEmail() {
        // 유효한 이메일
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name@domain.co.kr"));
        assertTrue(ValidationUtils.isValidEmail("test123@gmail.com"));

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
        assertTrue(ValidationUtils.isValidPhone("019-1111-2222"));

        // 유효하지 않은 전화번호
        assertFalse(ValidationUtils.isValidPhone("010-12345-678"));
        assertFalse(ValidationUtils.isValidPhone("010-1234-567"));
        assertFalse(ValidationUtils.isValidPhone("02-1234-5678"));
        assertFalse(ValidationUtils.isValidPhone(null));
    }

    @Test
    @DisplayName("사용자 ID 유효성 검사")
    void isValidUserId() {
        // 유효한 사용자 ID
        assertTrue(ValidationUtils.isValidUserId("testuser"));
        assertTrue(ValidationUtils.isValidUserId("user123"));
        assertTrue(ValidationUtils.isValidUserId("test_user"));

        // 유효하지 않은 사용자 ID
        assertFalse(ValidationUtils.isValidUserId("usr")); // 너무 짧음
        assertFalse(ValidationUtils.isValidUserId("verylongusernamethatexceedslimit")); // 너무 김
        assertFalse(ValidationUtils.isValidUserId("test-user")); // 하이픈 불가
        assertFalse(ValidationUtils.isValidUserId("test user")); // 공백 불가
        assertFalse(ValidationUtils.isValidUserId(null));
    }

    @Test
    @DisplayName("비밀번호 유효성 검사 - 수정된 패턴")
    void isValidPassword() {
        // 유효한 비밀번호 (영문자 + 숫자 또는 특수문자)
        assertTrue(ValidationUtils.isValidPassword("Test123"));      // 영문자 + 숫자
        assertTrue(ValidationUtils.isValidPassword("Test123!"));     // 영문자 + 숫자 + 특수문자
        assertTrue(ValidationUtils.isValidPassword("Password!"));    // 영문자 + 특수문자
        assertTrue(ValidationUtils.isValidPassword("MyPass123"));    // 영문자 + 숫자
        assertTrue(ValidationUtils.isValidPassword("SecurePass!"));  // 영문자 + 특수문자

        // 유효하지 않은 비밀번호
        assertFalse(ValidationUtils.isValidPassword("password"));     // 영문자만
        assertFalse(ValidationUtils.isValidPassword("12345678"));     // 숫자만
        assertFalse(ValidationUtils.isValidPassword("!@#$%^&*"));     // 특수문자만
        assertFalse(ValidationUtils.isValidPassword("Test1"));        // 너무 짧음
        assertFalse(ValidationUtils.isValidPassword("VeryLongPasswordThatExceedsTheMaximumLength123!")); // 너무 김
        assertFalse(ValidationUtils.isValidPassword(null));
    }

    @Test
    @DisplayName("강력한 비밀번호 유효성 검사")
    void isStrongPassword() {
        // 강력한 비밀번호 (모든 조건 포함)
        assertTrue(ValidationUtils.isStrongPassword("MyPass123!"));
        assertTrue(ValidationUtils.isStrongPassword("SecureP@ss1"));

        // 약한 비밀번호
        assertFalse(ValidationUtils.isStrongPassword("Test123")); // 특수문자 없음
        assertFalse(ValidationUtils.isStrongPassword("test123!")); // 대문자 없음
        assertFalse(ValidationUtils.isStrongPassword("TEST123!")); // 소문자 없음
        assertFalse(ValidationUtils.isStrongPassword("TestPass!")); // 숫자 없음
    }

    @Test
    @DisplayName("문자열 길이 검사")
    void isValidLength() {
        assertTrue(ValidationUtils.isValidLength("hello", 3, 10));
        assertTrue(ValidationUtils.isValidLength("test", 4, 4));

        assertFalse(ValidationUtils.isValidLength("hi", 3, 10)); // 너무 짧음
        assertFalse(ValidationUtils.isValidLength("verylongstring", 3, 10)); // 너무 김
        assertFalse(ValidationUtils.isValidLength(null, 3, 10));
    }

    @Test
    @DisplayName("빈 문자열 검사")
    void isNotEmpty() {
        assertTrue(ValidationUtils.isNotEmpty("hello"));
        assertTrue(ValidationUtils.isNotEmpty("  test  ")); // 공백 제거 후 체크

        assertFalse(ValidationUtils.isNotEmpty(""));
        assertFalse(ValidationUtils.isNotEmpty("   ")); // 공백만
        assertFalse(ValidationUtils.isNotEmpty(null));
    }

    @Test
    @DisplayName("숫자 검사")
    void isNumeric() {
        assertTrue(ValidationUtils.isNumeric("123"));
        assertTrue(ValidationUtils.isNumeric("123.45"));
        assertTrue(ValidationUtils.isNumeric("-123.45"));

        assertFalse(ValidationUtils.isNumeric("123abc"));
        assertFalse(ValidationUtils.isNumeric("abc"));
        assertFalse(ValidationUtils.isNumeric(""));
        assertFalse(ValidationUtils.isNumeric(null));
    }

    @Test
    @DisplayName("정수 검사")
    void isInteger() {
        assertTrue(ValidationUtils.isInteger("123"));
        assertTrue(ValidationUtils.isInteger("-123"));

        assertFalse(ValidationUtils.isInteger("123.45"));
        assertFalse(ValidationUtils.isInteger("123abc"));
        assertFalse(ValidationUtils.isInteger(""));
        assertFalse(ValidationUtils.isInteger(null));
    }

    @Test
    @DisplayName("비밀번호 강도 점수 테스트")
    void getPasswordStrength() {
        // 강력한 비밀번호는 높은 점수
        int strongScore = ValidationUtils.getPasswordStrength("MyPass123!");
        assertTrue(strongScore >= 80, "강력한 비밀번호는 80점 이상이어야 함: " + strongScore);

        // 약한 비밀번호는 낮은 점수
        int weakScore = ValidationUtils.getPasswordStrength("password");
        assertTrue(weakScore < 50, "약한 비밀번호는 50점 미만이어야 함: " + weakScore);
    }

    @Test
    @DisplayName("비밀번호 강도 등급 테스트")
    void getPasswordStrengthGrade() {
        // 강력한 비밀번호
        assertEquals("STRONG", ValidationUtils.getPasswordStrengthGrade("MyPass123!@"));

        // 보통 비밀번호
        assertEquals("MEDIUM", ValidationUtils.getPasswordStrengthGrade("MyPass123"));

        // 약한 비밀번호
        assertEquals("WEAK", ValidationUtils.getPasswordStrengthGrade("Test123"));

        // 매우 약한 비밀번호
        assertEquals("VERY_WEAK", ValidationUtils.getPasswordStrengthGrade("password"));
    }
}