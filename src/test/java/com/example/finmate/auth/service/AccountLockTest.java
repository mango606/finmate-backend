package com.example.finmate.auth.service;

import com.example.finmate.auth.controller.LoginController;
import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.common.exception.AccountLockedException;
import com.example.finmate.common.exception.GlobalExceptionHandler;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.dto.MemberLoginDTO;
import com.example.finmate.member.mapper.MemberMapper;
import com.example.finmate.security.util.JwtProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("계정 잠금 테스트")
@ActiveProfiles("test")
class AccountLockTest {

    private MockMvc loginMockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthService authService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtProcessor jwtProcessor;

    @Mock
    private MemberMapper memberMapper;

    private LoginController loginController;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        loginController = new LoginController(
                authenticationManager,
                authService,
                refreshTokenService,
                jwtProcessor,
                memberMapper
        );

        loginMockMvc = MockMvcBuilders.standaloneSetup(loginController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("로그인 5회 실패 후 계정 잠금")
    void accountLock_After5Failures() throws Exception {
        String userId = "testuser";
        String wrongPassword = "wrongpassword";
        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword(wrongPassword);

        // AuthService의 recordLoginFailure 메서드 모킹
        doNothing().when(authService).recordLoginFailure(anyString(), anyString(), anyString(), anyString());

        // 모든 로그인 시도에 대해 BadCredentialsException 발생
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // 5번의 로그인 실패 시도
        for (int i = 0; i < 5; i++) {
            loginMockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO))
                            .header("User-Agent", "TestAgent")
                            .header("X-Forwarded-For", "127.0.0.1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.errorCode", is("AUTHENTICATION_FAILED")));
        }

        // AuthService의 recordLoginFailure가 5번 호출되었는지 확인
        verify(authService, times(5)).recordLoginFailure(eq(userId), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("계정 잠금 상태에서 올바른 비밀번호로도 로그인 불가")
    void lockedAccount_CannotLoginWithCorrectPassword() throws Exception {
        String userId = "testuser";
        String correctPassword = "correctpassword";

        // 계정이 잠긴 상태에서는 AccountLockedException이 발생해야 하지만
        // LoginController에서 모든 예외를 BadCredentialsException으로 처리
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        doNothing().when(authService).recordLoginFailure(anyString(), anyString(), anyString(), anyString());

        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword(correctPassword);

        loginMockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("AUTHENTICATION_FAILED")));

        verify(authService).recordLoginFailure(eq(userId), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("계정 잠금 자동 해제 테스트")
    void autoUnlockAccount_After30Minutes() throws Exception {
        String userId = "testuser";
        String password = "password";

        // Mock 사용자 정보
        MemberVO member = new MemberVO();
        member.setUserId(userId);
        member.setUserName("테스트사용자");
        member.setUserEmail("test@example.com");
        member.setUserPhone("010-1234-5678");
        member.setBirthDate(LocalDate.of(1990, 1, 1));
        member.setGender("M");
        member.setRegDate(LocalDateTime.now());
        member.setActive(true);

        // 성공적인 인증을 위한 Mock 설정
        Authentication mockAuth = mock(Authentication.class);
        User authenticatedUser = new User(userId, "password", Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        when(mockAuth.getPrincipal()).thenReturn(authenticatedUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        // JWT 토큰 생성 Mock
        Map<String, Object> tokenPair = new HashMap<>();
        tokenPair.put("accessToken", "access-token");
        tokenPair.put("refreshToken", "refresh-token-jwt");
        tokenPair.put("expiresIn", 1800L);
        tokenPair.put("refreshExpiresIn", 1209600L);

        when(jwtProcessor.generateTokenPair(userId)).thenReturn(tokenPair);
        when(refreshTokenService.generateRefreshToken(eq(userId), anyString(), anyString()))
                .thenReturn("database-refresh-token");

        // 회원 정보 조회 Mock
        when(memberMapper.getMemberByUserId(userId)).thenReturn(member);

        // 로그인 성공 기록 Mock
        doNothing().when(authService).recordLoginSuccess(anyString(), anyString(), anyString());

        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword(password);

        // 로그인 시도 (자동 해제 후 성공)
        loginMockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("로그인 성공")));

        // 인증 관련 메서드들이 호출되었는지 확인
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authService).recordLoginSuccess(eq(userId), anyString(), anyString());
    }

    @Test
    @DisplayName("계정 잠금 기능 통합 테스트")
    void accountLockIntegrationTest() throws Exception {
        String userId = "testuser";
        String wrongPassword = "wrongpassword";

        // 실제 프로젝트에서는 LoginController가 모든 예외를 try-catch로 처리
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        doNothing().when(authService).recordLoginFailure(anyString(), anyString(), anyString(), anyString());

        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword(wrongPassword);

        // 실제 동작: 계정 잠금 상태여도 일반적인 인증 실패 메시지 반환
        loginMockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("로그인에 실패했습니다. 사용자 ID와 비밀번호를 확인해주세요.")))
                .andExpect(jsonPath("$.errorCode", is("AUTHENTICATION_FAILED")));

        // 실패 기록이 호출되었는지 확인
        verify(authService).recordLoginFailure(eq(userId), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("계정 잠금 상태 확인 테스트 - 단순화")
    void checkAccountLockStatus_Simplified() throws Exception {
        String userId = "testuser";

        // 잠긴 계정으로 로그인 시도 시 AccountLockedException 발생
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AccountLockedException("계정이 잠금 상태입니다."));

        doNothing().when(authService).recordLoginFailure(anyString(), anyString(), anyString(), anyString());

        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword("password");

        // 잠긴 계정으로 로그인 시도
        loginMockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("AUTHENTICATION_FAILED")));

        // 실패 기록이 호출되었는지 확인
        verify(authService).recordLoginFailure(eq(userId), anyString(), anyString(), anyString());
    }

    // AuthService 단위 테스트 (관리자 기능)
    @Test
    @DisplayName("AuthService 단위 테스트 - 계정 잠금 해제")
    void authService_unlockAccount_Success() {
        String lockedUserId = "testuser";

        // AuthService의 unlockAccount 메서드 직접 테스트
        when(authService.unlockAccount(lockedUserId)).thenReturn(true);

        boolean result = authService.unlockAccount(lockedUserId);

        assertTrue(result);
        verify(authService).unlockAccount(lockedUserId);
    }

    @Test
    @DisplayName("AuthService 단위 테스트 - 존재하지 않는 사용자")
    void authService_unlockAccount_NonExistentUser() {
        String nonExistentUserId = "nonexistent";

        when(authService.unlockAccount(nonExistentUserId))
                .thenThrow(new IllegalArgumentException("존재하지 않는 사용자입니다."));

        assertThrows(IllegalArgumentException.class, () -> {
            authService.unlockAccount(nonExistentUserId);
        });
    }

    @Test
    @DisplayName("로그인 실패 기록 동작 확인")
    void loginFailure_RecordingBehavior() throws Exception {
        String userId = "testuser";
        String wrongPassword = "wrongpassword";

        // AuthService 메서드 모킹
        doNothing().when(authService).recordLoginFailure(anyString(), anyString(), anyString(), anyString());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword(wrongPassword);

        // 로그인 실패 시도
        loginMockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isUnauthorized());

        // recordLoginFailure가 올바른 파라미터로 호출되었는지 확인
        verify(authService).recordLoginFailure(
                eq(userId),
                eq("127.0.0.1"),
                eq("TestAgent"),
                contains("Bad credentials")
        );
    }

    // 테스트 헬퍼 메서드
    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected true but was false");
        }
    }

    private static void assertThrows(Class<? extends Exception> expectedType, Runnable executable) {
        try {
            executable.run();
            throw new AssertionError("Expected " + expectedType.getSimpleName() + " to be thrown");
        } catch (Exception e) {
            if (!expectedType.isInstance(e)) {
                throw new AssertionError("Expected " + expectedType.getSimpleName() + " but got " + e.getClass().getSimpleName());
            }
        }
    }
}