package com.example.finmate.auth.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class TwoFactorAuthDTO {
    @NotBlank(message = "인증 코드를 입력해주세요")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다")
    private String authCode;

    @Pattern(regexp = "^(SMS|EMAIL|AUTHENTICATOR)$", message = "올바른 인증 방법을 선택해주세요")
    private String authMethod; // 인증 방법 (선택적)

    private Boolean rememberDevice; // 기기 기억 여부 (선택적)
}