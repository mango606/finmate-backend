package com.example.finmate.member.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MemberInfoDTO {
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private LocalDate birthDate;
    private String gender;
    private LocalDateTime regDate;
    private List<String> authorities;
}