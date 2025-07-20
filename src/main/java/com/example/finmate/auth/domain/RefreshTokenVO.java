package com.example.finmate.auth.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RefreshTokenVO {
    private Long tokenId;               // 토큰 ID
    private String userId;              // 사용자 ID
    private String refreshToken;        // Refresh Token 값
    private LocalDateTime expiryTime;   // 만료 시간
    private Boolean isValid;            // 유효 여부
    private String ipAddress;           // 발급 IP 주소
    private String userAgent;           // 사용자 에이전트
    private LocalDateTime createdDate;  // 생성일
    private LocalDateTime lastUsedDate; // 마지막 사용일
}