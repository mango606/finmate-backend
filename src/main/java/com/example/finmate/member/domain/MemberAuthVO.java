package com.example.finmate.member.domain;

import lombok.Data;

@Data
public class MemberAuthVO {
    private String userId;  // 사용자 ID
    private String auth;    // 권한 (ROLE_USER, ROLE_ADMIN)
}