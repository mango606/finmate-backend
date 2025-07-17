package com.example.finmate.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class TwoFactorAuthDTO {
    @NotBlank(message = "인증 코드를 입력해주세요")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다")
    private String authCode;
}