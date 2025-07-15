package com.example.finmate.member.controller;

import com.example.finmate.common.exception.GlobalExceptionHandler;
import com.example.finmate.member.dto.MemberJoinDTO;
import com.example.finmate.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 컨트롤러 테스트")
class MemberControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    private ObjectMapper objectMapper;
    private MemberJoinDTO validJoinDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validJoinDTO = new MemberJoinDTO();
        validJoinDTO.setUserId("testuser");
        validJoinDTO.setUserPassword("Test123!"); // 수정된 패스워드 (영문자+숫자+특수문자)
        validJoinDTO.setPasswordConfirm("Test123!");
        validJoinDTO.setUserName("테스트사용자");
        validJoinDTO.setUserEmail("test@example.com");
        validJoinDTO.setUserPhone("010-1234-5678");
        validJoinDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        validJoinDTO.setGender("M");
    }

    @Test
    @DisplayName("정상적인 회원가입 요청")
    void insertMember_Success() throws Exception {
        // given
        when(memberService.insertMember(any(MemberJoinDTO.class))).thenReturn(true);

        // when & then
        mockMvc.perform(post("/api/member/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJoinDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("회원 가입이 완료되었습니다.")));
    }

    @Test
    @DisplayName("회원가입 실패 - 서비스 레이어 오류")
    void insertMember_ServiceFailure() throws Exception {
        // given
        when(memberService.insertMember(any(MemberJoinDTO.class)))
                .thenThrow(new IllegalArgumentException("이미 사용 중인 사용자 ID입니다."));

        // when & then
        mockMvc.perform(post("/api/member/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJoinDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("이미 사용 중인 사용자 ID입니다.")));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검증 오류")
    void insertMember_ValidationFailure() throws Exception {
        // given - 잘못된 데이터
        MemberJoinDTO invalidDTO = new MemberJoinDTO();
        invalidDTO.setUserId("ab"); // 너무 짧음
        invalidDTO.setUserPassword("weak"); // 약한 비밀번호
        invalidDTO.setPasswordConfirm("weak");
        invalidDTO.setUserName(""); // 빈 이름
        invalidDTO.setUserEmail("invalid-email"); // 잘못된 이메일 형식

        // when & then
        mockMvc.perform(post("/api/member/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("사용자 ID 중복 확인 - 중복 없음")
    void checkUserIdDuplicate_NotDuplicated() throws Exception {
        // given
        when(memberService.checkUserIdDuplicate("testuser")).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/member/checkUserId/testuser"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.isDuplicate", is(false)))
                .andExpect(jsonPath("$.message", is("사용 가능한 ID입니다.")));
    }

    @Test
    @DisplayName("사용자 ID 중복 확인 - 중복 있음")
    void checkUserIdDuplicate_Duplicated() throws Exception {
        // given
        when(memberService.checkUserIdDuplicate("admin")).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/member/checkUserId/admin"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.isDuplicate", is(true)))
                .andExpect(jsonPath("$.message", is("이미 사용 중인 ID입니다.")));
    }

    @Test
    @DisplayName("이메일 중복 확인")
    void checkUserEmailDuplicate() throws Exception {
        // given
        when(memberService.checkUserEmailDuplicate(anyString())).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/member/checkEmail")
                        .param("userEmail", "test@example.com"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.isDuplicate", is(false)))
                .andExpect(jsonPath("$.message", is("사용 가능한 이메일입니다.")));
    }

    @Test
    @DisplayName("서버 상태 확인")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/member/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status", is("OK")))
                .andExpect(jsonPath("$.message", is("FinMate 서버가 정상 동작 중입니다.")));
    }
}