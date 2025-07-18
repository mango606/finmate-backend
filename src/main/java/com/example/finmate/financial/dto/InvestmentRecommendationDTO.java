package com.example.finmate.financial.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class InvestmentRecommendationDTO {
    @NotBlank(message = "투자 성향을 선택해주세요")
    @Pattern(regexp = "^(CONSERVATIVE|MODERATE|AGGRESSIVE)$",
            message = "올바른 투자 성향을 선택해주세요")
    private String riskProfile;

    @NotNull(message = "투자 가능 금액을 입력해주세요")
    @DecimalMin(value = "1.0", message = "투자 가능 금액은 1원 이상이어야 합니다")
    private BigDecimal investmentAmount;

    @NotNull(message = "투자 기간을 입력해주세요")
    @Min(value = 1, message = "투자 기간은 1개월 이상이어야 합니다")
    @Max(value = 600, message = "투자 기간은 600개월 이하여야 합니다")
    private Integer investmentPeriod;

    @NotBlank(message = "투자 목적을 선택해주세요")
    @Pattern(regexp = "^(SAVING|RETIREMENT|EDUCATION|HOUSE|BUSINESS)$",
            message = "올바른 투자 목적을 선택해주세요")
    private String investmentGoal;

    @DecimalMin(value = "0.0", message = "목표 수익률은 0% 이상이어야 합니다")
    @DecimalMax(value = "100.0", message = "목표 수익률은 100% 이하여야 합니다")
    private BigDecimal targetReturn;
}