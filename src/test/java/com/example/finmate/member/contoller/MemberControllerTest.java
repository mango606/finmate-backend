package com.example.finmate.member.controller;

import com.example.finmate.member.dto.MemberJoinDTO;
import com.example.finmate.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@DisplayName("회원 컨트롤러 테스트")
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @Autowired
    private ObjectMapper objectMapper;

    private MemberJoinDTO validJoinDTO;

    @BeforeEach
    void setUp() {
        validJoinDTO = new MemberJoinDTO();
        validJoinDTO.setUserId("testuser");
        validJoinDTO.setUserPassword("Test123!@#");
        validJoinDTO.setPasswordConfirm("Test123!@#");
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJoinDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원 가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("유효하지 않은 데이터로 회원가입 요청")
    void insertMember_InvalidData() throws Exception {
        // given
        validJoinDTO.setUserId(""); // 빈 사용자 ID

        // when & then
        mockMvc.perform(post("/api/member/join")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJoinDTO)))
                .andExpected(status().isBadRequest());
    }

    @Test
    @DisplayName("사용자 ID 중복 확인")
    void checkUserIdDuplicate() throws Exception {
        // given
        when(memberService.checkUserIdDuplicate("testuser")).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/member/checkUserId/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isDuplicate").value(false));
    }

    @Test
    @DisplayName("이메일 중복 확인")
    void checkUserEmailDuplicate() throws Exception {
        // given
        when(memberService.checkUserEmailDuplicate(anyString())).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/member/checkEmail")
                        .param("userEmail", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isDuplicate").value(false));
    }

    @Test
    @DisplayName("서버 상태 확인")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/member/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("OK"));
    }
}