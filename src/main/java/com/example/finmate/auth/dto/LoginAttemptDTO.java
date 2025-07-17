package com.example.finmate.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginAttemptDTO {
    @NotBlank(message = "사용자 ID를 입력해주세요")
    private String userId;

    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;

    private String captchaToken;        // 캡차 토큰 (선택적)
    private String twoFactorCode;       // 2단계 인증 코드 (선택적)
    private Boolean rememberMe;         // 로그인 상태 유지 (선택적)
}