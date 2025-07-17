package com.example.finmate.financial.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinancialProfileVO {
    private Long profileId;              // 프로필 ID
    private String userId;               // 사용자 ID
    private String incomeRange;          // 소득 구간 (LOW, MEDIUM, HIGH)
    private String jobType;              // 직업 유형 (EMPLOYEE, SELF_EMPLOYED, FREELANCER, STUDENT, RETIRED)
    private BigDecimal monthlyIncome;    // 월 소득
    private BigDecimal monthlyExpense;   // 월 지출
    private BigDecimal totalAssets;      // 총 자산
    private BigDecimal totalDebt;        // 총 부채
    private String riskProfile;          // 투자 성향 (CONSERVATIVE, MODERATE, AGGRESSIVE)
    private String investmentGoal;       // 투자 목표 (SAVING, RETIREMENT, EDUCATION, HOUSE, BUSINESS)
    private Integer investmentPeriod;    // 투자 기간 (개월)
    private BigDecimal emergencyFund;    // 비상금
    private String creditScore;          // 신용등급 (EXCELLENT, GOOD, FAIR, POOR)
    private LocalDateTime regDate;       // 등록일
    private LocalDateTime updateDate;    // 수정일
}