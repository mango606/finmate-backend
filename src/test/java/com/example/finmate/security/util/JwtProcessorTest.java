package com.example.finmate.security.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 프로세서 테스트")
class JwtProcessorTest {

    private JwtProcessor jwtProcessor;

    @BeforeEach
    void setUp() {
        jwtProcessor = new JwtProcessor();

        // 테스트용 설정값 주입
        ReflectionTestUtils.setField(jwtProcessor, "secretKey",
                "test-secret-key-for-jwt-token-generation-must-be-long-enough-minimum-256-bits");
        ReflectionTestUtils.setField(jwtProcessor, "expiration", 3600000L); // 1시간
        ReflectionTestUtils.setField(jwtProcessor, "header", "Authorization");
        ReflectionTestUtils.setField(jwtProcessor, "prefix", "Bearer ");
    }

    @Test
    @DisplayName("JWT 토큰 생성")
    void generateToken() {
        // when
        String token = jwtProcessor.generateToken("testuser");

        // then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("JWT 토큰에서 사용자 ID 추출")
    void getUserIdFromToken() {
        // given
        String token = jwtProcessor.generateToken("testuser");

        // when
        String userId = jwtProcessor.getUserIdFromToken(token);

        // then
        assertEquals("testuser", userId);
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 유효한 토큰")
    void validateToken_ValidToken() {
        // given
        String token = jwtProcessor.generateToken("testuser");

        // when
        boolean isValid = jwtProcessor.validateToken(token);

        // then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 유효하지 않은 토큰")
    void validateToken_InvalidToken() {
        // when
        boolean isValid = jwtProcessor.validateToken("invalid.token.here");

        // then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("HTTP 헤더에서 토큰 추출")
    void extractTokenFromHeader() {
        // when
        String token = jwtProcessor.extractTokenFromHeader("Bearer test-token");

        // then
        assertEquals("test-token", token);
    }

    @Test
    @DisplayName("HTTP 헤더에서 토큰 추출 - 잘못된 형식")
    void extractTokenFromHeader_InvalidFormat() {
        // when
        String token = jwtProcessor.extractTokenFromHeader("Invalid-Header");

        // then
        assertNull(token);
    }

    @Test
    @DisplayName("토큰 만료 시간 확인")
    void getExpirationDateFromToken() {
        // given
        String token = jwtProcessor.generateToken("testuser");

        // when
        Date expirationDate = jwtProcessor.getExpirationDateFromToken(token);

        // then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    @DisplayName("토큰 블랙리스트 추가 및 확인")
    void blacklistToken() {
        // given
        String token = jwtProcessor.generateToken("testuser");

        // when
        jwtProcessor.blacklistToken(token);

        // then
        assertTrue(jwtProcessor.isTokenBlacklisted(token));
        assertFalse(jwtProcessor.validateTokenWithBlacklist(token));
    }

    @Test
    @DisplayName("토큰 안전 갱신")
    void refreshTokenSafely() {
        // given
        String oldToken = jwtProcessor.generateToken("testuser");

        // when
        String newToken = jwtProcessor.refreshTokenSafely(oldToken);

        // then
        assertNotNull(newToken);
        assertNotEquals(oldToken, newToken);
        assertTrue(jwtProcessor.isTokenBlacklisted(oldToken));
        assertTrue(jwtProcessor.validateToken(newToken));
    }
}