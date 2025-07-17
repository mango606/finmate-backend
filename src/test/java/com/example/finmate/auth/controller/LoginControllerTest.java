package com.example.finmate.auth.controller;

import com.example.finmate.auth.service.AuthService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("로그인 컨트롤러 테스트")
class LoginControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthService authService;

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

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtProcessor.generateToken("testuser")).thenReturn("generated-jwt-token");
        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);
        doNothing().when(authService).recordLoginSuccess(anyString(), anyString(), anyString());

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("로그인 성공")))
                .andExpect(jsonPath("$.data.token", is("generated-jwt-token")))
                .andExpect(jsonPath("$.data.user.userId", is("testuser")))
                .andExpect(jsonPath("$.data.user.userName", is("테스트사용자")))
                .andExpect(jsonPath("$.data.user.userEmail", is("test@example.com")));

        verify(authService).recordLoginSuccess(eq("testuser"), anyString(), anyString());
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
                        .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("로그인에 실패했습니다. 사용자 ID와 비밀번호를 확인해주세요.")))
                .andExpect(jsonPath("$.error", is("AUTHENTICATION_FAILED")));

        verify(authService).recordLoginFailure(eq("testuser"), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 유효성 검증 오류")
    void login_ValidationError() throws Exception {
        // given - 잘못된 데이터
        MemberLoginDTO invalidDTO = new MemberLoginDTO();
        invalidDTO.setUserId(""); // 빈 사용자 ID
        invalidDTO.setUserPassword(""); // 빈 비밀번호

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("로그인 상태 확인 - 유효한 토큰")
    void getLoginStatus_ValidToken() throws Exception {
        // given
        when(jwtProcessor.getHeader()).thenReturn("Authorization");
        when(jwtProcessor.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtProcessor.validateToken("valid-token")).thenReturn(true);
        when(jwtProcessor.getUserIdFromToken("valid-token")).thenReturn("testuser");

        // when & then
        mockMvc.perform(get("/api/auth/status")
                        .header("Authorization", "Bearer valid-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.userId", is("testuser")))
                .andExpect(jsonPath("$.tokenValid", is(true)));
    }

    @Test
    @DisplayName("로그인 상태 확인 - 유효하지 않은 토큰")
    void getLoginStatus_InvalidToken() throws Exception {
        // given
        when(jwtProcessor.getHeader()).thenReturn("Authorization");
        when(jwtProcessor.extractTokenFromHeader("Bearer invalid-token")).thenReturn("invalid-token");
        when(jwtProcessor.validateToken("invalid-token")).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/auth/status")
                        .header("Authorization", "Bearer invalid-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.tokenValid", is(false)));
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshToken_Success() throws Exception {
        // given
        when(jwtProcessor.getHeader()).thenReturn("Authorization");
        when(jwtProcessor.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtProcessor.validateToken("valid-token")).thenReturn(true);
        when(jwtProcessor.getUserIdFromToken("valid-token")).thenReturn("testuser");
        when(jwtProcessor.generateToken("testuser")).thenReturn("new-jwt-token");

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer valid-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("토큰이 갱신되었습니다.")))
                .andExpect(jsonPath("$.token", is("new-jwt-token")));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰")
    void refreshToken_InvalidToken() throws Exception {
        // given
        when(jwtProcessor.getHeader()).thenReturn("Authorization");
        when(jwtProcessor.extractTokenFromHeader("Bearer invalid-token")).thenReturn("invalid-token");
        when(jwtProcessor.validateToken("invalid-token")).thenReturn(false);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer invalid-token"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("유효하지 않은 토큰입니다.")));
    }
}