package com.example.finmate.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("문자열 유틸리티 테스트")
class StringUtilsTest {

    @Test
    @DisplayName("빈 문자열 확인")
    void isEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertTrue(StringUtils.isEmpty("   "));
        assertFalse(StringUtils.isEmpty("test"));
        assertFalse(StringUtils.isEmpty("  test  "));
    }

    @Test
    @DisplayName("비어있지 않은 문자열 확인")
    void isNotEmpty() {
        assertFalse(StringUtils.isNotEmpty(null));
        assertFalse(StringUtils.isNotEmpty(""));
        assertFalse(StringUtils.isNotEmpty("   "));
        assertTrue(StringUtils.isNotEmpty("test"));
        assertTrue(StringUtils.isNotEmpty("  test  "));
    }

    @Test
    @DisplayName("이메일 마스킹")
    void maskEmail() {
        assertEquals("tes***@example.com", StringUtils.maskEmail("test@example.com"));
        assertEquals("t***@example.com", StringUtils.maskEmail("t@example.com"));
        assertEquals("lon***@example.com", StringUtils.maskEmail("longname@example.com"));
        assertEquals("invalid-email", StringUtils.maskEmail("invalid-email"));
        assertNull(StringUtils.maskEmail(null));
    }

    @Test
    @DisplayName("전화번호 마스킹")
    void maskPhone() {
        assertEquals("010-****-5678", StringUtils.maskPhone("010-1234-5678"));
        assertEquals("01012345678", StringUtils.maskPhone("01012345678")); // 형식이 맞지 않으면 그대로 반환
        assertNull(StringUtils.maskPhone(null));
        assertEquals("", StringUtils.maskPhone(""));
    }

    @Test
    @DisplayName("랜덤 문자열 생성")
    void generateRandomString() {
        String random1 = StringUtils.generateRandomString(10);
        String random2 = StringUtils.generateRandomString(10);

        assertNotNull(random1);
        assertNotNull(random2);
        assertEquals(10, random1.length());
        assertEquals(10, random2.length());
        assertNotEquals(random1, random2); // 매번 다른 문자열 생성
    }

    @Test
    @DisplayName("UUID 생성")
    void generateUUID() {
        String uuid1 = StringUtils.generateUUID();
        String uuid2 = StringUtils.generateUUID();

        assertNotNull(uuid1);
        assertNotNull(uuid2);
        assertTrue(uuid1.contains("-"));
        assertTrue(uuid2.contains("-"));
        assertNotEquals(uuid1, uuid2);
    }

    @Test
    @DisplayName("UUID 생성 - 하이픈 제거")
    void generateUUIDNoDash() {
        String uuid = StringUtils.generateUUIDNoDash();

        assertNotNull(uuid);
        assertFalse(uuid.contains("-"));
        assertEquals(32, uuid.length()); // 하이픈 제거시 32자
    }

    @Test
    @DisplayName("임시 비밀번호 생성")
    void generateTempPassword() {
        String tempPassword = StringUtils.generateTempPassword();

        assertNotNull(tempPassword);
        assertEquals(8, tempPassword.length());

        // 영문자 포함 확인
        assertTrue(tempPassword.matches(".*[a-zA-Z].*"));
        // 숫자 포함 확인
        assertTrue(tempPassword.matches(".*[0-9].*"));
        // 특수문자 포함 확인
        assertTrue(tempPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*"));
    }

    @Test
    @DisplayName("카멜케이스를 스네이크케이스로 변환")
    void camelToSnake() {
        assertEquals("user_name", StringUtils.camelToSnake("userName"));
        assertEquals("user_id", StringUtils.camelToSnake("userId"));
        assertEquals("my_test_variable", StringUtils.camelToSnake("myTestVariable"));
        assertEquals("test", StringUtils.camelToSnake("test"));
        assertNull(StringUtils.camelToSnake(null));
        assertEquals("", StringUtils.camelToSnake(""));
    }

    @Test
    @DisplayName("스네이크케이스를 카멜케이스로 변환")
    void snakeToCamel() {
        assertEquals("userName", StringUtils.snakeToCamel("user_name"));
        assertEquals("userId", StringUtils.snakeToCamel("user_id"));
        assertEquals("myTestVariable", StringUtils.snakeToCamel("my_test_variable"));
        assertEquals("test", StringUtils.snakeToCamel("test"));
        assertNull(StringUtils.snakeToCamel(null));
        assertEquals("", StringUtils.snakeToCamel(""));
    }

    @Test
    @DisplayName("문자열 앞뒤 공백 제거")
    void trim() {
        assertEquals("test", StringUtils.trim("  test  "));
        assertEquals("", StringUtils.trim("   "));
        assertEquals("test", StringUtils.trim("test"));
        assertNull(StringUtils.trim(null));
    }

    @Test
    @DisplayName("문자열 길이 제한")
    void truncate() {
        assertEquals("test...", StringUtils.truncate("test string", 4));
        assertEquals("test", StringUtils.truncate("test", 10));
        assertEquals("test", StringUtils.truncate("test", 4));
        assertNull(StringUtils.truncate(null, 5));
        assertEquals("", StringUtils.truncate("", 5));
    }
}