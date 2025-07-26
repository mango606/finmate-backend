package com.example.finmate.financial.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinancialProfileInfoDTO {
    private Long profileId;
    private String userId;
    private String incomeRange;
    private String jobType;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal totalAssets;
    private BigDecimal totalDebt;
    private String riskProfile;
    private String investmentGoal;
    private Integer investmentPeriod;
    private BigDecimal emergencyFund;
    private String creditScore;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;

    private BigDecimal netWorth;        // 순자산 (자산 - 부채)
    private BigDecimal monthlySurplus;  // 월 여유자금 (소득 - 지출)
    private String financialHealthGrade; // 재정 건전성 등급
    private BigDecimal recommendedInvestmentAmount; // 권장 투자 금액
}