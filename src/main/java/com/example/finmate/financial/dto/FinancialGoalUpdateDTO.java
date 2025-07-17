package com.example.finmate.financial.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public @Data
class FinancialGoalUpdateDTO {
    private Long goalId;

    @NotBlank(message = "목표명을 입력해주세요")
    @Size(min = 2, max = 50, message = "목표명은 2-50자 사이여야 합니다")
    private String goalName;

    @DecimalMin(value = "0.0", message = "현재 금액은 0 이상이어야 합니다")
    private BigDecimal currentAmount;

    @DecimalMin(value = "1.0", message = "월 적립액은 1원 이상이어야 합니다")
    private BigDecimal monthlyContribution;

    @Future(message = "목표 달성 일자는 미래 날짜여야 합니다")
    private LocalDate targetDate;

    @Pattern(regexp = "^(HIGH|MEDIUM|LOW)$", message = "올바른 우선순위를 선택해주세요")
    private String priority;

    @Pattern(regexp = "^(ACTIVE|COMPLETED|PAUSED|CANCELLED)$", message = "올바른 상태를 선택해주세요")
    private String status;

    @Size(max = 200, message = "설명은 200자 이하여야 합니다")
    private String description;
}