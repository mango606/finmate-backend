package com.example.finmate.auth.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginHistoryVO {
    private Long historyId;             // 이력 ID
    private String userId;              // 사용자 ID
    private String ipAddress;           // IP 주소
    private String userAgent;           // 사용자 에이전트
    private Boolean loginSuccess;       // 로그인 성공 여부
    private String failureReason;       // 실패 사유
    private LocalDateTime loginTime;    // 로그인 시간
    private String location;            // 접속 위치 (선택적)
    private String deviceType;          // 기기 유형 (선택적)
}