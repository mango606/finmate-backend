package com.example.finmate.financial.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class FinancialProfileDTO {
    @NotBlank(message = "소득 구간을 선택해주세요")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$", message = "올바른 소득 구간을 선택해주세요")
    private String incomeRange;

    @NotBlank(message = "직업 유형을 선택해주세요")
    @Pattern(regexp = "^(EMPLOYEE|SELF_EMPLOYED|FREELANCER|STUDENT|RETIRED)$", message = "올바른 직업 유형을 선택해주세요")
    private String jobType;

    @NotNull(message = "월 소득을 입력해주세요")
    @DecimalMin(value = "0.0", message = "월 소득은 0 이상이어야 합니다")
    private BigDecimal monthlyIncome;

    @NotNull(message = "월 지출을 입력해주세요")
    @DecimalMin(value = "0.0", message = "월 지출은 0 이상이어야 합니다")
    private BigDecimal monthlyExpense;

    @DecimalMin(value = "0.0", message = "총 자산은 0 이상이어야 합니다")
    private BigDecimal totalAssets;

    @DecimalMin(value = "0.0", message = "총 부채는 0 이상이어야 합니다")
    private BigDecimal totalDebt;

    @NotBlank(message = "투자 성향을 선택해주세요")
    @Pattern(regexp = "^(CONSERVATIVE|MODERATE|AGGRESSIVE)$", message = "올바른 투자 성향을 선택해주세요")
    private String riskProfile;

    @NotBlank(message = "투자 목표를 선택해주세요")
    @Pattern(regexp = "^(SAVING|RETIREMENT|EDUCATION|HOUSE|BUSINESS)$", message = "올바른 투자 목표를 선택해주세요")
    private String investmentGoal;

    @NotNull(message = "투자 기간을 입력해주세요")
    @Min(value = 1, message = "투자 기간은 1개월 이상이어야 합니다")
    @Max(value = 600, message = "투자 기간은 600개월 이하여야 합니다")
    private Integer investmentPeriod;

    @DecimalMin(value = "0.0", message = "비상금은 0 이상이어야 합니다")
    private BigDecimal emergencyFund;

    @Pattern(regexp = "^(EXCELLENT|GOOD|FAIR|POOR)$", message = "올바른 신용등급을 선택해주세요")
    private String creditScore;
}