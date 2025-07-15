package com.example.finmate.security.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private LocalDate birthDate;
    private String gender;
    private LocalDateTime regDate;
    private List<String> authorities;
}