package com.example.finmate.common.dto;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class SystemConfigDTO {
    @NotBlank(message = "설정 키를 입력해주세요")
    @Size(min = 1, max = 100, message = "설정 키는 1-100자 사이여야 합니다")
    private String configKey;

    @NotBlank(message = "설정 값을 입력해주세요")
    @Size(min = 1, max = 1000, message = "설정 값은 1-1000자 사이여야 합니다")
    private String configValue;

    @Size(max = 200, message = "설명은 200자 이하여야 합니다")
    private String description;

    @Pattern(regexp = "^(STRING|INTEGER|BOOLEAN|DECIMAL|JSON)$",
            message = "올바른 데이터 타입을 선택해주세요")
    private String dataType = "STRING";

    @Pattern(regexp = "^(SYSTEM|USER|ADMIN)$",
            message = "올바른 설정 분류를 선택해주세요")
    private String category = "SYSTEM";

    private Boolean isEditable = true; // 수정 가능 여부
    private Boolean isActive = true;   // 활성화 여부
}