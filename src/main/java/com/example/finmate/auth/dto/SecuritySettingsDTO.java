package com.example.finmate.auth.dto;

import lombok.Data;

@Data
public class SecuritySettingsDTO {
    private Boolean emailVerificationEnabled;
    private Boolean phoneVerificationEnabled;
    private Boolean twoFactorAuthEnabled;
    private Boolean loginNotificationEnabled;
    private Boolean securityQuestionEnabled;
    private String notificationEmail;
    private String notificationPhone;
}