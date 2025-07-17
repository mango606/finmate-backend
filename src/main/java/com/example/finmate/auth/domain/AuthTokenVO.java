package com.example.finmate.auth.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuthTokenVO {
    private Long tokenId;               // 토큰 ID
    private String userId;              // 사용자 ID
    private String token;               // 토큰 값
    private String tokenType;           // 토큰 유형 (PASSWORD_RESET, EMAIL_VERIFICATION, ACCESS_TOKEN)
    private LocalDateTime expiryTime;   // 만료 시간
    private Boolean isUsed;             // 사용 여부
    private LocalDateTime createdDate;  // 생성일
    private LocalDateTime usedDate;     // 사용일
}