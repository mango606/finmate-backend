package com.example.finmate.member.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MemberLoginDTO {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String userPassword;
}