package com.example.finmate.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("보안 유틸리티 테스트")
class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        // 테스트 전 SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 SecurityContext 정리
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("현재 사용자 ID 조회 - 인증된 사용자")
    void getCurrentUserId_AuthenticatedUser() {
        // given
        User user = new User("testuser", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        String userId = SecurityUtils.getCurrentUserId();

        // then
        assertEquals("testuser", userId);
    }

    @Test
    @DisplayName("현재 사용자 ID 조회 - 인증되지 않은 사용자")
    void getCurrentUserId_UnauthenticatedUser() {
        // when
        String userId = SecurityUtils.getCurrentUserId();

        // then
        assertNull(userId);
    }

    @Test
    @DisplayName("현재 사용자 ID 조회 - 익명 사용자")
    void getCurrentUserId_AnonymousUser() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        String userId = SecurityUtils.getCurrentUserId();

        // then
        assertNull(userId);
    }

    @Test
    @DisplayName("인증 상태 확인")
    void isAuthenticated() {
        // given - 인증되지 않은 상태
        assertFalse(SecurityUtils.isAuthenticated());

        // given - 인증된 상태
        User user = new User("testuser", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when & then
        assertTrue(SecurityUtils.isAuthenticated());
    }

    @Test
    @DisplayName("권한 확인 - USER 권한")
    void hasRole_UserRole() {
        // given
        User user = new User("testuser", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when & then
        assertTrue(SecurityUtils.hasRole("USER"));
        assertTrue(SecurityUtils.hasRole("ROLE_USER"));
        assertFalse(SecurityUtils.hasRole("ADMIN"));
        assertFalse(SecurityUtils.hasRole("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("권한 확인 - ADMIN 권한")
    void hasRole_AdminRole() {
        // given
        User user = new User("admin", "password",
                Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                ));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when & then
        assertTrue(SecurityUtils.hasRole("USER"));
        assertTrue(SecurityUtils.hasRole("ADMIN"));
        assertTrue(SecurityUtils.hasRole("ROLE_USER"));
        assertTrue(SecurityUtils.hasRole("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("권한 확인 - 인증되지 않은 사용자")
    void hasRole_UnauthenticatedUser() {
        // when & then
        assertFalse(SecurityUtils.hasRole("USER"));
        assertFalse(SecurityUtils.hasRole("ADMIN"));
    }

    @Test
    @DisplayName("현재 사용자 권한 목록 조회")
    void getCurrentUserAuthorities() {
        // given
        User user = new User("testuser", "password",
                Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                ));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        List<String> authorities = SecurityUtils.getCurrentUserAuthorities();

        // then
        assertEquals(2, authorities.size());
        assertTrue(authorities.contains("ROLE_USER"));
        assertTrue(authorities.contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("현재 사용자 권한 목록 조회 - 인증되지 않은 사용자")
    void getCurrentUserAuthorities_UnauthenticatedUser() {
        // when
        List<String> authorities = SecurityUtils.getCurrentUserAuthorities();

        // then
        assertTrue(authorities.isEmpty());
    }

    @Test
    @DisplayName("관리자 권한 확인")
    void isAdmin() {
        // given - 관리자 권한 없음
        User user = new User("testuser", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertFalse(SecurityUtils.isAdmin());

        // given - 관리자 권한 있음
        User admin = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UsernamePasswordAuthenticationToken adminAuth =
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        assertTrue(SecurityUtils.isAdmin());
    }

    @Test
    @DisplayName("사용자 권한 확인")
    void isUser() {
        // given
        User user = new User("testuser", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when & then
        assertTrue(SecurityUtils.isUser());

        // given - USER 권한 없음
        SecurityContextHolder.clearContext();
        assertFalse(SecurityUtils.isUser());
    }

    @Test
    @DisplayName("HTML 이스케이프")
    void escapeHtml() {
        // 기본적인 HTML 태그 이스케이프
        assertEquals("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;&#x2F;script&gt;",
                SecurityUtils.escapeHtml("<script>alert('xss')</script>"));

        // 다양한 특수문자 이스케이프
        assertEquals("&quot;test&quot; &amp; &gt;",
                SecurityUtils.escapeHtml("\"test\" & >"));

        // null 입력
        assertNull(SecurityUtils.escapeHtml(null));

        // 빈 문자열
        assertEquals("", SecurityUtils.escapeHtml(""));

        // 일반 텍스트
        assertEquals("안전한 텍스트", SecurityUtils.escapeHtml("안전한 텍스트"));

        // 모든 특수문자 테스트
        assertEquals("&amp;&lt;&gt;&quot;&#x27;&#x2F;",
                SecurityUtils.escapeHtml("&<>\"'/"));
    }

    @Test
    @DisplayName("SQL 인젝션 탐지")
    void containsSqlInjection() {
        // SQL 키워드 포함된 경우
        assertTrue(SecurityUtils.containsSqlInjection("SELECT * FROM users"));
        assertTrue(SecurityUtils.containsSqlInjection("'; DROP TABLE users; --"));
        assertTrue(SecurityUtils.containsSqlInjection("UNION SELECT password"));
        assertTrue(SecurityUtils.containsSqlInjection("INSERT INTO users"));
        assertTrue(SecurityUtils.containsSqlInjection("UPDATE users SET"));
        assertTrue(SecurityUtils.containsSqlInjection("DELETE FROM users"));

        // 대소문자 구분하지 않음
        assertTrue(SecurityUtils.containsSqlInjection("select * from users"));
        assertTrue(SecurityUtils.containsSqlInjection("Select * From Users"));

        // 스크립트 태그 포함된 경우
        assertTrue(SecurityUtils.containsSqlInjection("<script>alert('xss')</script>"));
        assertTrue(SecurityUtils.containsSqlInjection("javascript:alert('xss')"));
        assertTrue(SecurityUtils.containsSqlInjection("vbscript:alert('xss')"));
        assertTrue(SecurityUtils.containsSqlInjection("onload=alert('xss')"));
        assertTrue(SecurityUtils.containsSqlInjection("onerror=alert('xss')"));

        // 안전한 텍스트
        assertFalse(SecurityUtils.containsSqlInjection("정상적인 텍스트"));
        assertFalse(SecurityUtils.containsSqlInjection("test@example.com"));
        assertFalse(SecurityUtils.containsSqlInjection("사용자 이름"));
        assertFalse(SecurityUtils.containsSqlInjection("1234567890"));

        // null 입력
        assertFalse(SecurityUtils.containsSqlInjection(null));

        // 빈 문자열
        assertFalse(SecurityUtils.containsSqlInjection(""));
    }

    @Test
    @DisplayName("SHA-256 해시 생성")
    void sha256() {
        String input = "test123";
        String hash = SecurityUtils.sha256(input);

        // 해시가 생성되었는지 확인
        assertNotNull(hash);
        assertNotEquals(input, hash);

        // 같은 입력에 대해 같은 해시 생성
        assertEquals(hash, SecurityUtils.sha256(input));

        // 다른 입력에 대해 다른 해시 생성
        assertNotEquals(hash, SecurityUtils.sha256("different"));

        // 빈 문자열도 해시 생성 가능
        assertNotNull(SecurityUtils.sha256(""));

        // Base64 인코딩된 문자열인지 확인 (간단한 체크)
        assertTrue(hash.length() > 0);
        assertTrue(hash.matches("^[A-Za-z0-9+/=]+$"));
    }

    @Test
    @DisplayName("랜덤 솔트 생성")
    void generateSalt() {
        String salt1 = SecurityUtils.generateSalt();
        String salt2 = SecurityUtils.generateSalt();

        // 솔트가 생성되었는지 확인
        assertNotNull(salt1);
        assertNotNull(salt2);

        // 매번 다른 솔트 생성
        assertNotEquals(salt1, salt2);

        // Base64 인코딩된 문자열인지 확인
        assertTrue(salt1.matches("^[A-Za-z0-9+/=]+$"));
        assertTrue(salt2.matches("^[A-Za-z0-9+/=]+$"));

        // 충분한 길이인지 확인
        assertTrue(salt1.length() > 10);
        assertTrue(salt2.length() > 10);
    }

    @Test
    @DisplayName("보안 토큰 생성")
    void generateSecureToken() {
        // 32바이트 토큰 생성
        String token1 = SecurityUtils.generateSecureToken(32);
        String token2 = SecurityUtils.generateSecureToken(32);

        assertNotNull(token1);
        assertNotNull(token2);

        // 매번 다른 토큰 생성
        assertNotEquals(token1, token2);

        // URL-safe Base64 인코딩된 문자열인지 확인
        assertTrue(token1.matches("^[A-Za-z0-9_-]+$"));
        assertTrue(token2.matches("^[A-Za-z0-9_-]+$"));

        // 다른 길이의 토큰 생성
        String shortToken = SecurityUtils.generateSecureToken(16);
        String longToken = SecurityUtils.generateSecureToken(64);

        assertNotNull(shortToken);
        assertNotNull(longToken);
        assertNotEquals(shortToken.length(), longToken.length());
    }

    @Test
    @DisplayName("세션 타임아웃 확인")
    void isSessionValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 세션이 없는 경우
        assertFalse(SecurityUtils.isSessionValid(request));

        // 새로운 세션인 경우
        request.getSession(true); // 새 세션 생성
        assertFalse(SecurityUtils.isSessionValid(request));

        // 기존 세션이 있는 경우 (모킹이 필요하므로 간단히 테스트)
        MockHttpServletRequest requestWithSession = new MockHttpServletRequest();
        requestWithSession.getSession(true);
        // MockHttpServletRequest에서는 isNew()가 항상 true를 반환하므로
        // 실제 환경에서는 false가 반환될 것임
        assertFalse(SecurityUtils.isSessionValid(requestWithSession));
    }

    @Test
    @DisplayName("SHA-256 해시 생성 - 예외 처리")
    void sha256_ExceptionHandling() {
        // null 입력시 예외 발생하는지 확인
        assertThrows(RuntimeException.class, () -> {
            SecurityUtils.sha256(null);
        });
    }

    @Test
    @DisplayName("보안 토큰 생성 - 잘못된 길이")
    void generateSecureToken_InvalidLength() {
        // 0 또는 음수 길이
        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtils.generateSecureToken(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtils.generateSecureToken(-1);
        });
    }

    @Test
    @DisplayName("HTML 이스케이프 - 복잡한 케이스")
    void escapeHtml_ComplexCases() {
        // 중첩된 태그
        String complexHtml = "<div onclick=\"alert('test')\">Hello <b>World</b></div>";
        String escaped = SecurityUtils.escapeHtml(complexHtml);
        assertFalse(escaped.contains("<"));
        assertFalse(escaped.contains(">"));
        assertFalse(escaped.contains("\""));
        assertFalse(escaped.contains("'"));

        // 한글과 특수문자 혼합
        String mixedText = "안녕하세요 <script>alert('안녕');</script>";
        String escapedMixed = SecurityUtils.escapeHtml(mixedText);
        assertTrue(escapedMixed.contains("안녕하세요"));
        assertFalse(escapedMixed.contains("<script>"));
    }

    @Test
    @DisplayName("SQL 인젝션 탐지 - 복잡한 케이스")
    void containsSqlInjection_ComplexCases() {
        // SQL 키워드가 단어 중간에 포함된 경우
        assertTrue(SecurityUtils.containsSqlInjection("please select this"));
        assertTrue(SecurityUtils.containsSqlInjection("I will insert this"));

        // 정상적인 단어에 SQL 키워드가 포함된 경우도 탐지 (보수적 접근)
        assertTrue(SecurityUtils.containsSqlInjection("selection process"));

        // 실제 악성 코드 패턴
        assertTrue(SecurityUtils.containsSqlInjection("1' OR '1'='1"));
        assertTrue(SecurityUtils.containsSqlInjection("admin'--"));
        assertTrue(SecurityUtils.containsSqlInjection("'; DROP TABLE users; --"));
    }
}