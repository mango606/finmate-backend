package com.example.finmate.common.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Data
public class ApiRequestLogDTO {
    @NotBlank(message = "요청 URL을 입력해주세요")
    @Size(max = 500, message = "요청 URL은 500자 이하여야 합니다")
    private String requestUrl;

    @NotBlank(message = "HTTP 메서드를 입력해주세요")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)$",
            message = "올바른 HTTP 메서드를 선택해주세요")
    private String httpMethod;

    @Size(max = 50, message = "사용자 ID는 50자 이하여야 합니다")
    private String userId;

    @NotBlank(message = "클라이언트 IP를 입력해주세요")
    @Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$",
            message = "올바른 IP 주소 형식이 아닙니다")
    private String clientIp;

    @Size(max = 500, message = "User Agent는 500자 이하여야 합니다")
    private String userAgent;

    @Min(value = 100, message = "응답 코드는 100 이상이어야 합니다")
    @Max(value = 599, message = "응답 코드는 599 이하여야 합니다")
    private Integer responseCode;

    @Min(value = 0, message = "응답 시간은 0 이상이어야 합니다")
    private Long responseTime; // 밀리초

    @Size(max = 2000, message = "요청 파라미터는 2000자 이하여야 합니다")
    private String requestParams;

    @Size(max = 1000, message = "에러 메시지는 1000자 이하여야 합니다")
    private String errorMessage;

    private LocalDateTime requestTime;
}