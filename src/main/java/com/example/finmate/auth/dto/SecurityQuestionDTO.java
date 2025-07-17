package com.example.finmate.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class SecurityQuestionDTO {
    @NotBlank(message = "보안 질문을 선택해주세요")
    private String question;

    @NotBlank(message = "답변을 입력해주세요")
    @Size(min = 1, max = 100, message = "답변은 1-100자 사이여야 합니다")
    private String answer;
}