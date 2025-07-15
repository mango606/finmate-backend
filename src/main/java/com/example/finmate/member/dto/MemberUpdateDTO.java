package com.example.finmate.member.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class MemberUpdateDTO {
    @NotBlank(message = "사용자 이름은 필수입니다")
    @Size(min = 2, max = 10, message = "사용자 이름은 2-10자 사이여야 합니다")
    private String userName;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String userEmail;

    @Pattern(regexp = "^01[0-9]-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    private String userPhone;

    @Past(message = "생년월일은 과거 날짜여야 합니다")
    private LocalDate birthDate;

    @Pattern(regexp = "^[MF]$", message = "성별은 M 또는 F만 입력 가능합니다")
    private String gender;
}