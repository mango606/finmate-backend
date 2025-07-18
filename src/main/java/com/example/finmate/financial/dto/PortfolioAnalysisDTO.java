package com.example.finmate.financial.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PortfolioAnalysisDTO {
    @NotNull(message = "분석 시작일을 입력해주세요")
    @PastOrPresent(message = "분석 시작일은 과거 또는 현재 날짜여야 합니다")
    private LocalDate startDate;

    @NotNull(message = "분석 종료일을 입력해주세요")
    @PastOrPresent(message = "분석 종료일은 과거 또는 현재 날짜여야 합니다")
    private LocalDate endDate;

    @Pattern(regexp = "^(PERFORMANCE|RISK|ALLOCATION|REBALANCING)$",
            message = "올바른 분석 유형을 선택해주세요")
    private String analysisType;

    @DecimalMin(value = "0.0", message = "벤치마크 수익률은 0% 이상이어야 합니다")
    private BigDecimal benchmarkReturn;

    private Boolean includeDetail = false; // 상세 분석 포함 여부

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    private Integer page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다")
    private Integer size = 10;
}