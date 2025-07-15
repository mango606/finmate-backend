package com.example.finmate.member.domain;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString(exclude = "userPassword")
public class MemberVO {
    private String userId;           // 사용자 ID
    private String userPassword;     // 암호화된 비밀번호
    private String userName;         // 사용자 이름
    private String userEmail;        // 이메일
    private String userPhone;        // 전화번호
    private LocalDate birthDate;     // 생년월일
    private String gender;           // 성별 (M/F)
    private LocalDateTime regDate;   // 등록일
    private LocalDateTime updateDate; // 수정일
    private boolean isActive;        // 계정 활성화 상태

    // 권한 정보
    private List<MemberAuthVO> authList;
}