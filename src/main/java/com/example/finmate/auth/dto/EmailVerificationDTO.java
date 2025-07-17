package com.example.finmate.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class EmailVerificationDTO {
    @NotBlank(message = "인증 토큰을 입력해주세요")
    private String token;
}