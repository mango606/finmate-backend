package com.example.finmate.financial.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinancialTransactionVO {
    private Long transactionId;         // 거래 ID
    private String userId;              // 사용자 ID
    private String transactionType;     // 거래 유형 (INCOME, EXPENSE, INVESTMENT, SAVING, LOAN_REPAYMENT)
    private String category;            // 카테고리 (FOOD, TRANSPORT, HOUSING, ENTERTAINMENT, SALARY, BONUS, STOCK, BOND, FUND 등)
    private BigDecimal amount;          // 거래 금액
    private String description;         // 설명
    private LocalDateTime transactionDate; // 거래 일시
    private String paymentMethod;       // 결제 수단 (CARD, CASH, TRANSFER, INVESTMENT_ACCOUNT)
    private String merchant;            // 거래처/상호
    private Long goalId;                // 연관된 목표 ID (선택적)
    private String tags;                // 태그 (콤마로 구분)
    private String memo;                // 메모
    private Boolean isRecurring;        // 정기 거래 여부
    private String recurringPeriod;     // 정기 거래 주기 (DAILY, WEEKLY, MONTHLY, YEARLY)
    private LocalDateTime regDate;      // 등록일
    private LocalDateTime updateDate;   // 수정일
}