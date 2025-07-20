
package com.example.finmate.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RefreshTokenDTO {
    @NotBlank(message = "Refresh Token을 입력해주세요")
    private String refreshToken;
}