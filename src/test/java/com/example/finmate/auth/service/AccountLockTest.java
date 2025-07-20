package com.example.finmate.auth.service;

import com.example.finmate.auth.controller.LoginController;
import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.auth.mapper.AuthMapper;
import com.example.finmate.common.exception.AccountLockedException;
import com.example.finmate.common.exception.GlobalExceptionHandler;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.dto.MemberLoginDTO;
import com.example.finmate.member.mapper.MemberMapper;
import com.example.finmate.security.service.CustomUserDetailsService;
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

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
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

    private MockMvc mockMvc;

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

    @Mock
    private AuthMapper authMapper;

    @Mock
    private CustomUserDetailsService userDetailsService;

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

        mockMvc = MockMvcBuilders.standaloneSetup(loginController)
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

        // Mock 설정: 처음 4번은 일반 인증 실패
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // 처음 4번의 실패
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO))
                            .header("User-Agent", "TestAgent")
                            .header("X-Forwarded-For", "127.0.0.1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        // 5번째 시도에서 AccountLockedException 발생하도록 설정
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AccountLockedException("계정이 잠금 상태입니다. 15:30 이후 다시 시도해주세요."));

        // 5번째 로그인 시도 (계정 잠금)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("계정이 잠금 상태입니다")))
                .andExpect(jsonPath("$.errorCode", is("AUTHENTICATION_FAILED")));

        // AuthService의 recordLoginFailure가 5번 호출되었는지 확인
        verify(authService, times(5)).recordLoginFailure(eq(userId), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 계정 잠금 해제")
    void unlockAccount_ByAdmin() throws Exception {
        String lockedUserId = "testuser";

        // AuthService mock 설정
        when(authService.unlockAccount(lockedUserId)).thenReturn(true);
        doNothing().when(authService).recordSecurityEvent(anyString(), anyString(), anyString());

        // 현재 사용자를 admin으로 설정하는 Mock Authentication
        Authentication mockAuth = mock(Authentication.class);
        User adminUser = new User("admin", "password", Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(mockAuth.getPrincipal()).thenReturn(adminUser);
        when(mockAuth.getName()).thenReturn("admin");

        mockMvc.perform(post("/api/auth/unlock-account")
                        .param("userId", lockedUserId)
                        .header("Authorization", "Bearer admin-token")
                        .principal(mockAuth))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("계정 잠금이 해제되었습니다.")));

        verify(authService).unlockAccount(lockedUserId);
    }

    @Test
    @DisplayName("계정 잠금 상태에서 올바른 비밀번호로도 로그인 불가")
    void lockedAccount_CannotLoginWithCorrectPassword() throws Exception {
        String userId = "testuser";
        String correctPassword = "correctpassword";

        // 계정이 잠긴 상태에서 AccountLockedException 발생
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AccountLockedException("계정이 잠금 상태입니다. 관리자에게 문의하세요."));

        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword(correctPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("계정이 잠금 상태입니다")));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 계정 잠금 해제 시도")
    void unlockAccount_NonExistentUser() throws Exception {
        String nonExistentUserId = "nonexistent";

        // 존재하지 않는 사용자에 대한 예외 발생
        when(authService.unlockAccount(nonExistentUserId))
                .thenThrow(new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Authentication mockAuth = mock(Authentication.class);
        User adminUser = new User("admin", "password", Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(mockAuth.getPrincipal()).thenReturn(adminUser);
        when(mockAuth.getName()).thenReturn("admin");

        mockMvc.perform(post("/api/auth/unlock-account")
                        .param("userId", nonExistentUserId)
                        .header("Authorization", "Bearer admin-token")
                        .principal(mockAuth))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("존재하지 않는 사용자입니다.")))
                .andExpect(jsonPath("$.errorCode", is("INVALID_REQUEST")));
    }

    @Test
    @DisplayName("계정 잠금 자동 해제 테스트")
    void autoUnlockAccount_After30Minutes() throws Exception {
        String userId = "testuser";
        String password = "password";

        // 잠긴 계정 정보 설정 (30분 전에 잠김)
        AccountSecurityVO lockedAccount = new AccountSecurityVO();
        lockedAccount.setUserId(userId);
        lockedAccount.setAccountLocked(true);
        lockedAccount.setLockTime(LocalDateTime.now().minusMinutes(31)); // 31분 전 잠금

        // Mock 사용자 정보
        MemberVO member = new MemberVO();
        member.setUserId(userId);
        member.setActive(true);

        // 첫 번째 호출에서는 잠긴 상태, 두 번째 호출에서는 자동 해제됨을 시뮬레이션
        when(authMapper.getAccountSecurity(userId))
                .thenReturn(lockedAccount)
                .thenReturn(null); // 해제 후에는 잠금 정보 없음

        // CustomUserDetailsService에서 자동 해제 로직이 작동하도록 설정
        when(userDetailsService.loadUserByUsername(userId))
                .thenAnswer(invocation -> {
                    // 자동 해제 로직 시뮬레이션
                    doNothing().when(authMapper).updateAccountLockStatus(userId, false);
                    doNothing().when(authMapper).resetLoginFailCount(userId);

                    return User.builder()
                            .username(userId)
                            .password("encodedPassword")
                            .authorities("ROLE_USER")
                            .build();
                });

        // 성공적인 인증을 위한 Mock 설정
        Authentication mockAuth = mock(Authentication.class);
        User authenticatedUser = new User(userId, "password", Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        when(mockAuth.getPrincipal()).thenReturn(authenticatedUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        // JWT 토큰 생성 Mock
        when(jwtProcessor.generateTokenPair(userId)).thenReturn(java.util.Map.of(
                "accessToken", "access-token",
                "refreshToken", "refresh-token-jwt",
                "expiresIn", 1800L,
                "refreshExpiresIn", 1209600L
        ));
        when(refreshTokenService.generateRefreshToken(eq(userId), anyString(), anyString()))
                .thenReturn("database-refresh-token");

        // 회원 정보 조회 Mock
        when(memberMapper.getMemberByUserId(userId)).thenReturn(member);

        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword(password);

        // 로그인 시도 (자동 해제 후 성공)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("로그인 성공")));

        // 자동 해제 관련 메서드들이 호출되었는지 확인
        verify(authMapper).updateAccountLockStatus(userId, false);
        verify(authMapper).resetLoginFailCount(userId);
    }
}