package com.example.finmate.auth.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

@Data
public class AccountLockDTO {
    @NotBlank(message = "사용자 ID를 입력해주세요")
    private String userId;

    @NotBlank(message = "잠금 사유를 입력해주세요")
    private String lockReason;

    @Min(value = 1, message = "잠금 기간은 최소 1시간 이상이어야 합니다")
    @Max(value = 8760, message = "잠금 기간은 최대 1년(8760시간)을 초과할 수 없습니다")
    private Integer lockDurationHours;  // 잠금 기간 (시간)
}