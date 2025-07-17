package com.example.finmate.auth.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountSecurityVO {
    private String userId;              // 사용자 ID
    private Boolean emailVerified;      // 이메일 인증 여부
    private Boolean phoneVerified;      // 전화번호 인증 여부
    private Boolean twoFactorEnabled;   // 2단계 인증 활성화 여부
    private Boolean accountLocked;      // 계정 잠금 여부
    private Integer loginFailCount;     // 로그인 실패 횟수
    private LocalDateTime lastLoginTime; // 마지막 로그인 시간
    private LocalDateTime lockTime;     // 잠금 시간
    private String lockReason;          // 잠금 사유
    private LocalDateTime updateDate;   // 수정일
}