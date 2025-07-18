package com.example.finmate.member.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminMemberDTO {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotBlank(message = "사용자 이름은 필수입니다")
    @Size(min = 2, max = 10, message = "사용자 이름은 2-10자 사이여야 합니다")
    private String userName;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String userEmail;

    @Pattern(regexp = "^01[0-9]-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    private String userPhone;

    private LocalDate birthDate;

    @Pattern(regexp = "^[MF]$", message = "성별은 M 또는 F만 입력 가능합니다")
    private String gender;

    private Boolean isActive;

    private LocalDateTime regDate;
    private LocalDateTime updateDate;

    private List<String> authorities;

    // 관리자 전용 필드
    @Size(max = 500, message = "관리자 메모는 500자 이하여야 합니다")
    private String adminMemo;

    @Pattern(regexp = "^(NORMAL|WARNING|SUSPENDED|BANNED)$",
            message = "올바른 회원 등급을 선택해주세요")
    private String memberGrade;

    private LocalDateTime lastLoginDate;
    private Integer loginFailCount;
}