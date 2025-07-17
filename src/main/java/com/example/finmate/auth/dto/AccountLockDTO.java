package com.example.finmate.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AccountLockDTO {
    @NotBlank(message = "사용자 ID를 입력해주세요")
    private String userId;

    @NotBlank(message = "잠금 사유를 입력해주세요")
    private String lockReason;

    private Integer lockDurationHours;  // 잠금 기간 (시간)
}