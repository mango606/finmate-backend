package com.example.finmate.auth.controller;

import com.example.finmate.auth.dto.RefreshTokenDTO;
import com.example.finmate.auth.service.AuthService;
import com.example.finmate.auth.service.RefreshTokenService;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("로그인 컨트롤러 테스트")
@ActiveProfiles("test")
class LoginControllerTest {

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

    @InjectMocks
    private LoginController loginController;

    private ObjectMapper objectMapper;
    private MemberLoginDTO validLoginDTO;
    private MemberVO testMember;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(loginController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        // 테스트용 로그인 DTO
        validLoginDTO = new MemberLoginDTO();
        validLoginDTO.setUserId("testuser");
        validLoginDTO.setUserPassword("Test123!");

        // 테스트용 회원 정보
        testMember = new MemberVO();
        testMember.setUserId("testuser");
        testMember.setUserName("테스트사용자");
        testMember.setUserEmail("test@example.com");
        testMember.setUserPhone("010-1234-5678");
        testMember.setBirthDate(LocalDate.of(1990, 1, 1));
        testMember.setGender("M");
        testMember.setRegDate(LocalDateTime.now());
        testMember.setActive(true);
    }

    @Test
    @DisplayName("정상적인 로그인 성공")
    void login_Success() throws Exception {
        // given
        Collection<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        User userDetails = new User("testuser", "encodedPassword", authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, authorities);

        // JWT 토큰 페어 생성 모킹
        Map<String, Object> tokenPair = new HashMap<>();
        tokenPair.put("accessToken", "generated-access-token");
        tokenPair.put("refreshToken", "generated-refresh-token-jwt");
        tokenPair.put("expiresIn", 1800L);
        tokenPair.put("refreshExpiresIn", 1209600L);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtProcessor.generateTokenPair("testuser")).thenReturn(tokenPair);
        when(refreshTokenService.generateRefreshToken(eq("testuser"), anyString(), anyString()))
                .thenReturn("database-refresh-token");
        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);

        doNothing().when(authService).recordLoginSuccess(anyString(), anyString(), anyString());

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO))
                        .header("User-Agent", "JUnitTest")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("로그인 성공")))
                .andExpect(jsonPath("$.data.accessToken", is("generated-access-token")))
                .andExpect(jsonPath("$.data.refreshToken", is("database-refresh-token")))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.userId", is("testuser")))
                .andExpect(jsonPath("$.data.user.userName", is("테스트사용자")))
                .andExpect(jsonPath("$.data.user.userEmail", is("test@example.com")));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 인증 정보")
    void login_BadCredentials() throws Exception {
        // given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        doNothing().when(authService).recordLoginFailure(anyString(), anyString(), anyString(), anyString());

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO))
                        .header("User-Agent", "JUnitTest")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("로그인에 실패했습니다. 사용자 ID와 비밀번호를 확인해주세요.")))
                .andExpect(jsonPath("$.errorCode", is("AUTHENTICATION_FAILED")));
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshToken_Success() throws Exception {
        // given
        RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
        refreshTokenDTO.setRefreshToken("valid-refresh-token");

        when(refreshTokenService.validateRefreshToken("valid-refresh-token")).thenReturn(true);
        when(refreshTokenService.isSuspiciousTokenUsage(eq("valid-refresh-token"), anyString())).thenReturn(false);
        when(refreshTokenService.getUserIdFromRefreshToken("valid-refresh-token")).thenReturn("testuser");
        when(jwtProcessor.generateAccessToken("testuser")).thenReturn("new-access-token");
        when(jwtProcessor.getAccessTokenExpiration()).thenReturn(1800000L);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("토큰이 갱신되었습니다.")))
                .andExpect(jsonPath("$.data.accessToken", is("new-access-token")))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.expiresIn", is(1800)));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰")
    void refreshToken_InvalidToken() throws Exception {
        // given
        RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
        refreshTokenDTO.setRefreshToken("invalid-refresh-token");

        when(refreshTokenService.validateRefreshToken("invalid-refresh-token")).thenReturn(false);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("유효하지 않은 Refresh Token입니다.")))
                .andExpect(jsonPath("$.errorCode", is("INVALID_REFRESH_TOKEN")));
    }

    @Test
    @DisplayName("로그인 상태 확인 - 유효한 토큰")
    void getLoginStatus_ValidToken() throws Exception {
        // given
        when(jwtProcessor.getHeader()).thenReturn("Authorization");
        when(jwtProcessor.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtProcessor.validateAccessTokenWithBlacklist("valid-token")).thenReturn(true);
        when(jwtProcessor.getUserIdFromToken("valid-token")).thenReturn("testuser");

        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("userId", "testuser");
        tokenInfo.put("exp", System.currentTimeMillis() + 1800000);
        when(jwtProcessor.getTokenInfo("valid-token")).thenReturn(tokenInfo);

        // when & then
        mockMvc.perform(get("/api/auth/status")
                        .header("Authorization", "Bearer valid-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.authenticated", is(true)))
                .andExpect(jsonPath("$.data.userId", is("testuser")))
                .andExpect(jsonPath("$.data.tokenValid", is(true)))
                .andExpect(jsonPath("$.data.tokenInfo", notNullValue()));
    }

    @Test
    @DisplayName("로그인 상태 확인 - 유효하지 않은 토큰")
    void getLoginStatus_InvalidToken() throws Exception {
        // given
        when(jwtProcessor.getHeader()).thenReturn("Authorization");
        when(jwtProcessor.extractTokenFromHeader("Bearer invalid-token")).thenReturn("invalid-token");
        when(jwtProcessor.validateAccessTokenWithBlacklist("invalid-token")).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/auth/status")
                        .header("Authorization", "Bearer invalid-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.authenticated", is(false)))
                .andExpect(jsonPath("$.data.tokenValid", is(false)));
    }

    @Test
    @DisplayName("토큰 검증")
    void validateToken_ValidToken() throws Exception {
        // given
        when(jwtProcessor.getHeader()).thenReturn("Authorization");
        when(jwtProcessor.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtProcessor.validateAccessTokenWithBlacklist("valid-token")).thenReturn(true);
        when(jwtProcessor.getUserIdFromToken("valid-token")).thenReturn("testuser");

        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("userId", "testuser");
        tokenInfo.put("exp", System.currentTimeMillis() + 1800000);
        when(jwtProcessor.getTokenInfo("valid-token")).thenReturn(tokenInfo);

        // when & then
        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer valid-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.userId", is("testuser")))
                .andExpect(jsonPath("$.data.tokenInfo", notNullValue()));
    }

    @Test
    @DisplayName("로그아웃")
    void logout_Success() throws Exception {
        // given
        when(jwtProcessor.getHeader()).thenReturn("Authorization");
        when(jwtProcessor.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtProcessor.validateAccessToken("valid-token")).thenReturn(true);
        when(jwtProcessor.getUserIdFromToken("valid-token")).thenReturn("testuser");

        doNothing().when(jwtProcessor).blacklistToken("valid-token");
        doNothing().when(refreshTokenService).invalidateAllRefreshTokens("testuser");
        doNothing().when(authService).recordSecurityEvent(eq("testuser"), eq("LOGOUT"), anyString());

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("로그아웃되었습니다.")));
    }
}