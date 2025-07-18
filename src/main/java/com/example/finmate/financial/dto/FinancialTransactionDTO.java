package com.example.finmate.financial.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinancialTransactionDTO {
    @NotBlank(message = "거래 유형을 선택해주세요")
    @Pattern(regexp = "^(INCOME|EXPENSE|INVESTMENT|SAVING|LOAN_REPAYMENT)$",
            message = "올바른 거래 유형을 선택해주세요")
    private String transactionType;

    @NotBlank(message = "카테고리를 선택해주세요")
    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    private String category;

    @NotNull(message = "거래 금액을 입력해주세요")
    @DecimalMin(value = "0.1", message = "거래 금액은 0.1원 이상이어야 합니다")
    @DecimalMax(value = "999999999999.99", message = "거래 금액이 너무 큽니다")
    private BigDecimal amount;

    @Size(max = 200, message = "설명은 200자 이하여야 합니다")
    private String description;

    @NotNull(message = "거래 일시를 입력해주세요")
    private LocalDateTime transactionDate;

    @Pattern(regexp = "^(CARD|CASH|TRANSFER|INVESTMENT_ACCOUNT)$",
            message = "올바른 결제 수단을 선택해주세요")
    private String paymentMethod;

    @Size(max = 100, message = "거래처명은 100자 이하여야 합니다")
    private String merchant;

    private Long goalId; // 연관된 목표 ID (선택적)

    @Size(max = 200, message = "태그는 200자 이하여야 합니다")
    private String tags;

    @Size(max = 500, message = "메모는 500자 이하여야 합니다")
    private String memo;

    private Boolean isRecurring; // 정기 거래 여부

    @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY|YEARLY)$",
            message = "올바른 정기 거래 주기를 선택해주세요")
    private String recurringPeriod;
}