package com.example.finmate.financial.dto;

import lombok.Data;

import java.util.List;

@Data
public class InvestmentCategoryDTO {
    private String userId;
    private List<String> categories; // STOCK, BOND, REAL_ESTATE, CRYPTO, FUND, SAVING 등
}