package com.example.finmate.financial.controller;

import com.example.finmate.common.exception.GlobalExceptionHandler;
import com.example.finmate.financial.dto.FinancialGoalDTO;
import com.example.finmate.financial.dto.FinancialProfileDTO;
import com.example.finmate.financial.service.FinancialService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("금융 컨트롤러 테스트")
class FinancialControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FinancialService financialService;

    @InjectMocks
    private FinancialController financialController;

    private ObjectMapper objectMapper;
    private FinancialProfileDTO validProfileDTO;
    private FinancialGoalDTO validGoalDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(financialController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 인증 컨텍스트 설정
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 테스트용 금융 프로필 DTO
        validProfileDTO = new FinancialProfileDTO();
        validProfileDTO.setIncomeRange("MEDIUM");
        validProfileDTO.setJobType("EMPLOYEE");
        validProfileDTO.setMonthlyIncome(new BigDecimal("5000000"));
        validProfileDTO.setMonthlyExpense(new BigDecimal("3000000"));
        validProfileDTO.setTotalAssets(new BigDecimal("100000000"));
        validProfileDTO.setTotalDebt(new BigDecimal("20000000"));
        validProfileDTO.setRiskProfile("MODERATE");
        validProfileDTO.setInvestmentGoal("RETIREMENT");
        validProfileDTO.setInvestmentPeriod(240);
        validProfileDTO.setEmergencyFund(new BigDecimal("10000000"));
        validProfileDTO.setCreditScore("GOOD");

        // 테스트용 금융 목표 DTO
        validGoalDTO = new FinancialGoalDTO();
        validGoalDTO.setGoalName("내 집 마련");
        validGoalDTO.setGoalType("HOUSE_PURCHASE");
        validGoalDTO.setTargetAmount(new BigDecimal("500000000"));
        validGoalDTO.setCurrentAmount(new BigDecimal("50000000"));
        validGoalDTO.setMonthlyContribution(new BigDecimal("2000000"));
        validGoalDTO.setTargetDate(LocalDate.now().plusYears(5));
        validGoalDTO.setStartDate(LocalDate.now());
        validGoalDTO.setPriority("HIGH");
        validGoalDTO.setDescription("5년 내 내 집 마련하기");
    }

    @Test
    @DisplayName("금융 프로필 조회 성공")
    void getFinancialProfile_Success() throws Exception {
        // given
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("hasProfile", true);
        profileData.put("profile", validProfileDTO);

        when(financialService.getFinancialProfile(anyString())).thenReturn(profileData);

        // when & then
        mockMvc.perform(get("/api/financial/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.hasProfile", is(true)));
    }

    @Test
    @DisplayName("금융 프로필 저장 성공")
    void saveFinancialProfile_Success() throws Exception {
        // given
        when(financialService.saveFinancialProfile(anyString(), any(FinancialProfileDTO.class)))
                .thenReturn(true);

        // when & then
        mockMvc.perform(post("/api/financial/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProfileDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("금융 프로필이 저장되었습니다.")));
    }

    @Test
    @DisplayName("금융 목표 등록 성공")
    void createFinancialGoal_Success() throws Exception {
        // given
        when(financialService.createFinancialGoal(anyString(), any(FinancialGoalDTO.class)))
                .thenReturn(true);

        // when & then
        mockMvc.perform(post("/api/financial/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validGoalDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("금융 목표가 등록되었습니다.")));
    }

    @Test
    @DisplayName("금융 대시보드 조회 성공")
    void getFinancialDashboard_Success() throws Exception {
        // given
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("profile", validProfileDTO);
        dashboardData.put("activeGoals", new java.util.ArrayList<>());

        when(financialService.getFinancialDashboard(anyString())).thenReturn(dashboardData);

        // when & then
        mockMvc.perform(get("/api/financial/dashboard"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)));
    }
}