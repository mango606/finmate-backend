package com.example.finmate.member.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class MemberJoinDTO {
    @NotBlank(message = "사용자 ID는 필수입니다")
    @Size(min = 4, max = 20, message = "사용자 ID는 4-20자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자 ID는 영문, 숫자, 언더스코어만 사용 가능합니다")
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
    // 영문자와 숫자+특수문자 중 하나 이상 포함 (더 유연한 패턴)
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[\\d@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
            message = "비밀번호는 영문자와 숫자 또는 특수문자를 포함해야 합니다")
    private String userPassword;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String passwordConfirm;

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

    // 비밀번호 확인 커스텀 검증 메서드
    public boolean isPasswordMatching() {
        return userPassword != null && userPassword.equals(passwordConfirm);
    }
}