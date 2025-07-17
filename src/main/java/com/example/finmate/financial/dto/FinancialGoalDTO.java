package com.example.finmate.financial.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinancialGoalDTO {
    @NotBlank(message = "목표명을 입력해주세요")
    @Size(min = 2, max = 50, message = "목표명은 2-50자 사이여야 합니다")
    private String goalName;

    @NotBlank(message = "목표 유형을 선택해주세요")
    @Pattern(regexp = "^(SAVING|INVESTMENT|DEBT_REPAYMENT|HOUSE_PURCHASE|RETIREMENT)$", message = "올바른 목표 유형을 선택해주세요")
    private String goalType;

    @NotNull(message = "목표 금액을 입력해주세요")
    @DecimalMin(value = "1.0", message = "목표 금액은 1원 이상이어야 합니다")
    @DecimalMax(value = "999999999999.99", message = "목표 금액이 너무 큽니다")
    private BigDecimal targetAmount;

    @DecimalMin(value = "0.0", message = "현재 금액은 0 이상이어야 합니다")
    private BigDecimal currentAmount;

    @NotNull(message = "월 적립액을 입력해주세요")
    @DecimalMin(value = "1.0", message = "월 적립액은 1원 이상이어야 합니다")
    private BigDecimal monthlyContribution;

    @NotNull(message = "목표 달성 일자를 입력해주세요")
    @Future(message = "목표 달성 일자는 미래 날짜여야 합니다")
    private LocalDate targetDate;

    @NotNull(message = "시작일을 입력해주세요")
    private LocalDate startDate;

    @NotBlank(message = "우선순위를 선택해주세요")
    @Pattern(regexp = "^(HIGH|MEDIUM|LOW)$", message = "올바른 우선순위를 선택해주세요")
    private String priority;

    @Size(max = 200, message = "설명은 200자 이하여야 합니다")
    private String description;
}