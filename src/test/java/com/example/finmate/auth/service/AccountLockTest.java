package com.example.finmate.auth.controller;

import com.example.finmate.auth.service.AuthService;
import com.example.finmate.common.dto.ApiResponse;
import com.example.finmate.common.exception.AccountLockedException;
import com.example.finmate.common.exception.GlobalExceptionHandler;
import com.example.finmate.member.dto.MemberLoginDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;

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

    private LoginController loginController;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        loginController = new LoginController(authenticationManager, authService, null, null, null);
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

        // 처음 4회는 일반적인 인증 실패
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // 5회 실패 시뮬레이션
        for (int i = 0; i < 4; i++) {
            MemberLoginDTO loginDTO = new MemberLoginDTO();
            loginDTO.setUserId(userId);
            loginDTO.setUserPassword(wrongPassword);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isUnauthorized());

            verify(authService, times(i + 1)).recordLoginFailure(eq(userId), anyString(), anyString(), anyString());
        }

        // 5번째 시도에서 계정 잠금 예외 발생
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AccountLockedException("계정이 잠금 상태입니다. 15:30 이후 다시 시도해주세요."));

        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword(wrongPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("계정이 잠금 상태입니다")));
    }

    @Test
    @DisplayName("관리자 계정 잠금 해제")
    void unlockAccount_ByAdmin() throws Exception {
        String lockedUserId = "testuser";

        when(authService.unlockAccount(lockedUserId)).thenReturn(true);

        mockMvc.perform(post("/api/auth/unlock-account")
                        .param("userId", lockedUserId)
                        .header("Authorization", "Bearer admin-token"))
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

        // 계정이 잠긴 상태
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AccountLockedException("계정이 잠금 상태입니다. 관리자에게 문의하세요."));

        MemberLoginDTO loginDTO = new MemberLoginDTO();
        loginDTO.setUserId(userId);
        loginDTO.setUserPassword(correctPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("계정이 잠금 상태입니다")));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 계정 잠금 해제 시도")
    void unlockAccount_NonExistentUser() throws Exception {
        String nonExistentUserId = "nonexistent";

        when(authService.unlockAccount(nonExistentUserId))
                .thenThrow(new IllegalArgumentException("존재하지 않는 사용자입니다."));

        mockMvc.perform(post("/api/auth/unlock-account")
                        .param("userId", nonExistentUserId)
                        .header("Authorization", "Bearer admin-token"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("존재하지 않는 사용자입니다.")));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLockedException(AccountLockedException e) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage(), "ACCOUNT_LOCKED"));
    }
}