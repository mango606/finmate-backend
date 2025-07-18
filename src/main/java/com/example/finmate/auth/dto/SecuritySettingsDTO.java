package com.example.finmate.auth.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

@Data
public class SecuritySettingsDTO {
    private Boolean emailVerificationEnabled;
    private Boolean phoneVerificationEnabled;
    private Boolean twoFactorAuthEnabled;
    private Boolean loginNotificationEnabled;
    private Boolean securityQuestionEnabled;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String notificationEmail;

    @Pattern(regexp = "^01[0-9]-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    private String notificationPhone;
}