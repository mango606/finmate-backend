package com.example.finmate.member.controller;

import com.example.finmate.member.dto.MemberJoinDTO;
import com.example.finmate.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        mockMvc = MockMvcBuilders.standaloneSetup(memberController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJoinDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원 가입이 완료되었습니다."));
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