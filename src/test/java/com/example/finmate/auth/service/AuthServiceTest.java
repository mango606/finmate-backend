package com.example.finmate.auth.service;

import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.auth.domain.AuthTokenVO;
import com.example.finmate.auth.mapper.AuthMapper;
import com.example.finmate.common.service.CacheService;
import com.example.finmate.common.service.EmailService;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.mapper.MemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 서비스 테스트")
class AuthServiceTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CacheService cacheService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private MemberVO testMember;

    @BeforeEach
    void setUp() {
        testMember = new MemberVO();
        testMember.setUserId("testuser");
        testMember.setUserName("테스트사용자");
        testMember.setUserEmail("test@example.com");
        testMember.setActive(true);
    }

    @Test
    @DisplayName("비밀번호 재설정 토큰 생성 성공")
    void generatePasswordResetToken_Success() {
        // given
        when(memberMapper.getMemberByUserEmail("test@example.com")).thenReturn(testMember);
        doNothing().when(authMapper).deleteExpiredTokens(anyString(), anyString());
        when(authMapper.insertAuthToken(any(AuthTokenVO.class))).thenReturn(1);
        doNothing().when(cacheService).put(anyString(), anyString(), anyInt());

        // when
        String token = authService.generatePasswordResetToken("test@example.com");

        // then
        assertNotNull(token);
        verify(authMapper).insertAuthToken(any(AuthTokenVO.class));
        verify(cacheService).put(anyString(), eq("testuser"), anyInt());
    }

    @Test
    @DisplayName("비밀번호 재설정 토큰 생성 실패 - 존재하지 않는 이메일")
    void generatePasswordResetToken_EmailNotFound() {
        // given
        when(memberMapper.getMemberByUserEmail("nonexistent@example.com")).thenReturn(null);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            authService.generatePasswordResetToken("nonexistent@example.com");
        });
    }

    @Test
    @DisplayName("비밀번호 재설정 성공")
    void resetPassword_Success() {
        // given
        String token = "valid-token";
        String newPassword = "NewPass123!";

        when(cacheService.get("pwd_reset_" + token, String.class)).thenReturn("testuser");

        AuthTokenVO authToken = new AuthTokenVO();
        authToken.setUserId("testuser");
        authToken.setToken(token);
        authToken.setIsUsed(false);
        authToken.setExpiryTime(LocalDateTime.now().plusHours(1));
        when(authMapper.getAuthToken(token, "PASSWORD_RESET")).thenReturn(authToken);

        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");
        when(memberMapper.updateMemberPassword("testuser", "encodedPassword")).thenReturn(1);
        when(authMapper.markTokenAsUsed(token)).thenReturn(1);
        doNothing().when(cacheService).remove(anyString());

        // when
        boolean result = authService.resetPassword(token, newPassword);

        // then
        assertTrue(result);
        verify(memberMapper).updateMemberPassword("testuser", "encodedPassword");
        verify(authMapper).markTokenAsUsed(token);
        verify(cacheService).remove("pwd_reset_" + token);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 유효하지 않은 토큰")
    void resetPassword_InvalidToken() {
        // given
        String token = "invalid-token";
        when(cacheService.get("pwd_reset_" + token, String.class)).thenReturn(null);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            authService.resetPassword(token, "NewPass123!");
        });
    }

    @Test
    @DisplayName("이메일 인증 토큰 생성 성공")
    void generateEmailVerificationToken_Success() {
        // given
        when(memberMapper.getMemberByUserEmail("test@example.com")).thenReturn(testMember);
        doNothing().when(authMapper).deleteExpiredTokens(anyString(), anyString());
        when(authMapper.insertAuthToken(any(AuthTokenVO.class))).thenReturn(1);
        doNothing().when(cacheService).put(anyString(), anyString(), anyInt());

        // when
        String token = authService.generateEmailVerificationToken("test@example.com");

        // then
        assertNotNull(token);
        verify(authMapper).insertAuthToken(any(AuthTokenVO.class));
        verify(cacheService).put(anyString(), eq("testuser"), anyInt());
    }

    @Test
    @DisplayName("이메일 인증 확인 성공")
    void verifyEmail_Success() {
        // given
        String token = "valid-token";

        when(cacheService.get("email_verify_" + token, String.class)).thenReturn("testuser");

        AuthTokenVO authToken = new AuthTokenVO();
        authToken.setUserId("testuser");
        authToken.setToken(token);
        authToken.setIsUsed(false);
        authToken.setExpiryTime(LocalDateTime.now().plusDays(1));
        when(authMapper.getAuthToken(token, "EMAIL_VERIFICATION")).thenReturn(authToken);

        when(authMapper.updateEmailVerificationStatus("testuser", true)).thenReturn(1);
        when(authMapper.markTokenAsUsed(token)).thenReturn(1);
        doNothing().when(cacheService).remove(anyString());

        // when
        boolean result = authService.verifyEmail(token);

        // then
        assertTrue(result);
        verify(authMapper).updateEmailVerificationStatus("testuser", true);
        verify(authMapper).markTokenAsUsed(token);
        verify(cacheService).remove("email_verify_" + token);
    }

    @Test
    @DisplayName("계정 잠금 해제 성공")
    void unlockAccount_Success() {
        // given
        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);
        when(authMapper.updateAccountLockStatus("testuser", false)).thenReturn(1);
        when(authMapper.resetLoginFailCount("testuser")).thenReturn(1);

        // when
        boolean result = authService.unlockAccount("testuser");

        // then
        assertTrue(result);
        verify(authMapper).updateAccountLockStatus("testuser", false);
        verify(authMapper).resetLoginFailCount("testuser");
    }

    @Test
    @DisplayName("계정 보안 정보 조회")
    void getAccountSecurity() {
        // given
        AccountSecurityVO security = new AccountSecurityVO();
        security.setUserId("testuser");
        security.setEmailVerified(true);
        security.setPhoneVerified(false);
        security.setTwoFactorEnabled(false);
        security.setAccountLocked(false);
        security.setLoginFailCount(0);

        when(authMapper.getAccountSecurity("testuser")).thenReturn(security);

        // when
        Map<String, Object> result = authService.getAccountSecurity("testuser");

        // then
        assertNotNull(result);
        assertEquals(true, result.get("emailVerified"));
        assertEquals(false, result.get("phoneVerified"));
        assertEquals(false, result.get("twoFactorEnabled"));
        assertEquals(false, result.get("accountLocked"));
        assertEquals(0, result.get("loginFailCount"));
    }

    @Test
    @DisplayName("2단계 인증 설정 성공")
    void updateTwoFactorAuth_Success() {
        // given
        when(authMapper.updateTwoFactorStatus("testuser", true)).thenReturn(1);

        // when
        boolean result = authService.updateTwoFactorAuth("testuser", true);

        // then
        assertTrue(result);
        verify(authMapper).updateTwoFactorStatus("testuser", true);
    }

    @Test
    @DisplayName("보안 점검 수행")
    void performSecurityCheck() {
        // given
        AccountSecurityVO security = new AccountSecurityVO();
        security.setUserId("testuser");
        security.setEmailVerified(true);
        security.setTwoFactorEnabled(false);
        security.setLastLoginTime(LocalDateTime.now().minusDays(5));

        when(authMapper.getAccountSecurity("testuser")).thenReturn(security);
        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);

        // when
        Map<String, Object> result = authService.performSecurityCheck("testuser");

        // then
        assertNotNull(result);
        assertTrue((Boolean) result.get("emailVerified"));
        assertFalse((Boolean) result.get("twoFactorEnabled"));
        assertTrue((Boolean) result.get("recentActivity"));
        assertEquals("GOOD", result.get("passwordStrength"));
        assertTrue((Integer) result.get("totalScore") > 0);
        assertNotNull(result.get("grade"));
        assertNotNull(result.get("recommendations"));
    }
}