package com.example.finmate.financial.dto;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class FinancialStatisticsDTO {
    @NotBlank(message = "통계 기간을 선택해주세요")
    @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY|QUARTERLY|YEARLY)$",
            message = "올바른 통계 기간을 선택해주세요")
    private String period;

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    private Integer page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private Integer size = 10;

    @Pattern(regexp = "^(INCOME|EXPENSE|INVESTMENT|SAVING|ALL)$",
            message = "올바른 거래 유형을 선택해주세요")
    private String transactionType;

    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    private String category;
}