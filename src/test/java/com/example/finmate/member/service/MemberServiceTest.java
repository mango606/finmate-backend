package com.example.finmate.member.service;

import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.dto.MemberJoinDTO;
import com.example.finmate.member.mapper.MemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 서비스 테스트")
class MemberServiceTest {

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private MemberJoinDTO validJoinDTO;

    @BeforeEach
    void setUp() {
        validJoinDTO = new MemberJoinDTO();
        validJoinDTO.setUserId("testuser");
        validJoinDTO.setUserPassword("Test123!@#");
        validJoinDTO.setPasswordConfirm("Test123!@#");
        validJoinDTO.setUserName("테스트사용자");
        validJoinDTO.setUserEmail("test@example.com");
        validJoinDTO.setUserPhone("010-1234-5678");
        validJoinDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        validJoinDTO.setGender("M");
    }

    @Test
    @DisplayName("정상적인 회원가입")
    void insertMember_Success() {
        // given
        when(memberMapper.checkUserIdDuplicate(anyString())).thenReturn(0);
        when(memberMapper.checkUserEmailDuplicate(anyString())).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(memberMapper.insertMember(any(MemberVO.class))).thenReturn(1);
        when(memberMapper.insertMemberAuth(any())).thenReturn(1);

        // when
        boolean result = memberService.insertMember(validJoinDTO);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("중복된 사용자 ID로 회원가입 실패")
    void insertMember_DuplicateUserId() {
        // given
        when(memberMapper.checkUserIdDuplicate(anyString())).thenReturn(1);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.insertMember(validJoinDTO);
        });
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 실패")
    void insertMember_DuplicateEmail() {
        // given
        when(memberMapper.checkUserIdDuplicate(anyString())).thenReturn(0);
        when(memberMapper.checkUserEmailDuplicate(anyString())).thenReturn(1);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.insertMember(validJoinDTO);
        });
    }

    @Test
    @DisplayName("비밀번호 불일치로 회원가입 실패")
    void insertMember_PasswordMismatch() {
        // given
        validJoinDTO.setPasswordConfirm("DifferentPassword");
        when(memberMapper.checkUserIdDuplicate(anyString())).thenReturn(0);
        when(memberMapper.checkUserEmailDuplicate(anyString())).thenReturn(0);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.insertMember(validJoinDTO);
        });
    }

    @Test
    @DisplayName("사용자 ID 중복 확인")
    void checkUserIdDuplicate() {
        // given
        when(memberMapper.checkUserIdDuplicate("existingUser")).thenReturn(1);
        when(memberMapper.checkUserIdDuplicate("newUser")).thenReturn(0);

        // when & then
        assertTrue(memberService.checkUserIdDuplicate("existingUser"));
        assertFalse(memberService.checkUserIdDuplicate("newUser"));
    }

    @Test
    @DisplayName("이메일 중복 확인")
    void checkUserEmailDuplicate() {
        // given
        when(memberMapper.checkUserEmailDuplicate("existing@test.com")).thenReturn(1);
        when(memberMapper.checkUserEmailDuplicate("new@test.com")).thenReturn(0);

        // when & then
        assertTrue(memberService.checkUserEmailDuplicate("existing@test.com"));
        assertFalse(memberService.checkUserEmailDuplicate("new@test.com"));
    }
}