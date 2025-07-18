package com.example.finmate.auth.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class LoginAttemptDTO {
    @NotBlank(message = "사용자 ID를 입력해주세요")
    @Size(min = 4, max = 20, message = "사용자 ID는 4-20자 사이여야 합니다")
    private String userId;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
    private String password;

    @Size(max = 500, message = "캡차 토큰이 너무 깁니다")
    private String captchaToken;        // 캡차 토큰 (선택적)

    @Size(min = 6, max = 6, message = "2단계 인증 코드는 6자리여야 합니다")
    private String twoFactorCode;       // 2단계 인증 코드 (선택적)

    private Boolean rememberMe;         // 로그인 상태 유지 (선택적)
}