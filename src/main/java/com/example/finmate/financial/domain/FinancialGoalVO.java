package com.example.finmate.financial.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FinancialGoalVO {
    private Long goalId;                    // 목표 ID
    private String userId;                  // 사용자 ID
    private String goalName;                // 목표명
    private String goalType;                // 목표 유형 (SAVING, INVESTMENT, DEBT_REPAYMENT, HOUSE_PURCHASE, RETIREMENT)
    private BigDecimal targetAmount;        // 목표 금액
    private BigDecimal currentAmount;       // 현재 금액
    private BigDecimal monthlyContribution; // 월 적립액
    private LocalDate targetDate;           // 목표 달성 일자
    private LocalDate startDate;            // 시작일
    private String priority;                // 우선순위 (HIGH, MEDIUM, LOW)
    private String status;                  // 상태 (ACTIVE, COMPLETED, PAUSED, CANCELLED)
    private String description;             // 설명
    private BigDecimal progressPercentage;  // 달성률
    private Integer expectedMonths;         // 예상 달성 기간 (개월)
    private LocalDateTime regDate;          // 등록일
    private LocalDateTime updateDate;       // 수정일
}