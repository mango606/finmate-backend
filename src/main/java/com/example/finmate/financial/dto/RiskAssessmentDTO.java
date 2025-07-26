package com.example.finmate.financial.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class RiskAssessmentDTO {
    @NotNull(message = "위험 성향 테스트 답변을 입력해주세요")
    private Map<String, Integer> answers; // 질문ID -> 답변점수 매핑

    private Integer totalScore;
    private String riskProfile;
    private String description;
    private String recommendedProducts;
}