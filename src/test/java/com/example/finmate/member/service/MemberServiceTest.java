package com.example.finmate.member.service;

import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.auth.mapper.AuthMapper;
import com.example.finmate.common.exception.DuplicateResourceException;
import com.example.finmate.common.exception.MemberNotFoundException;
import com.example.finmate.member.domain.MemberAuthVO;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.dto.MemberJoinDTO;
import com.example.finmate.member.dto.MemberInfoDTO;
import com.example.finmate.member.dto.MemberUpdateDTO;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 서비스 테스트")
class MemberServiceTest {

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private MemberService memberService;

    private MemberJoinDTO validJoinDTO;
    private MemberVO testMember;
    private MemberUpdateDTO validUpdateDTO;

    @BeforeEach
    void setUp() {
        validJoinDTO = new MemberJoinDTO();
        validJoinDTO.setUserId("testuser");
        validJoinDTO.setUserPassword("Test123!");
        validJoinDTO.setPasswordConfirm("Test123!");
        validJoinDTO.setUserName("테스트사용자");
        validJoinDTO.setUserEmail("test@example.com");
        validJoinDTO.setUserPhone("010-1234-5678");
        validJoinDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        validJoinDTO.setGender("M");

        testMember = new MemberVO();
        testMember.setUserId("testuser");
        testMember.setUserName("테스트사용자");
        testMember.setUserEmail("test@example.com");
        testMember.setUserPhone("010-1234-5678");
        testMember.setBirthDate(LocalDate.of(1990, 1, 1));
        testMember.setGender("M");
        testMember.setRegDate(LocalDateTime.now());
        testMember.setActive(true);

        validUpdateDTO = new MemberUpdateDTO();
        validUpdateDTO.setUserName("수정된이름");
        validUpdateDTO.setUserEmail("updated@example.com");
        validUpdateDTO.setUserPhone("010-9876-5432");
        validUpdateDTO.setBirthDate(LocalDate.of(1991, 1, 1));
        validUpdateDTO.setGender("F");
    }

    @Test
    @DisplayName("정상적인 회원가입")
    void insertMember_Success() {
        // given
        when(memberMapper.checkUserIdDuplicate(anyString())).thenReturn(0);
        when(memberMapper.checkUserEmailDuplicate(anyString())).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(memberMapper.insertMember(any(MemberVO.class))).thenReturn(1);
        when(memberMapper.insertMemberAuth(any(MemberAuthVO.class))).thenReturn(1);
        when(authMapper.insertAccountSecurity(any(AccountSecurityVO.class))).thenReturn(1);

        // when
        boolean result = memberService.insertMember(validJoinDTO);

        // then
        assertTrue(result);
        verify(memberMapper).insertMember(any(MemberVO.class));
        verify(memberMapper).insertMemberAuth(any(MemberAuthVO.class));
    }

    @Test
    @DisplayName("중복된 사용자 ID로 회원가입 실패")
    void insertMember_DuplicateUserId() {
        // given
        when(memberMapper.checkUserIdDuplicate(anyString())).thenReturn(1);

        // when & then
        assertThrows(DuplicateResourceException.class, () -> {
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
        assertThrows(DuplicateResourceException.class, () -> {
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
    @DisplayName("회원 정보 조회 성공")
    void getMemberInfo_Success() {
        // given
        List<MemberAuthVO> authList = Arrays.asList(
                createAuth("testuser", "ROLE_USER")
        );

        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);
        when(memberMapper.getMemberAuthByUserId("testuser")).thenReturn(authList);

        // when
        MemberInfoDTO result = memberService.getMemberInfo("testuser");

        // then
        assertNotNull(result);
        assertEquals("testuser", result.getUserId());
        assertEquals("테스트사용자", result.getUserName());
        assertEquals("test@example.com", result.getUserEmail());
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains("ROLE_USER"));
    }

    @Test
    @DisplayName("회원 정보 조회 실패 - 존재하지 않는 회원")
    void getMemberInfo_MemberNotFound() {
        // given
        when(memberMapper.getMemberByUserId("nonexistent")).thenReturn(null);

        // when & then
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.getMemberInfo("nonexistent");
        });
    }

    @Test
    @DisplayName("회원 정보 수정 성공")
    void updateMember_Success() {
        // given
        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);
        when(memberMapper.getMemberByUserEmail("updated@example.com")).thenReturn(null);
        when(memberMapper.updateMember(any(MemberVO.class))).thenReturn(1);

        // when
        boolean result = memberService.updateMember("testuser", validUpdateDTO);

        // then
        assertTrue(result);
        verify(memberMapper).updateMember(any(MemberVO.class));
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 이메일 중복")
    void updateMember_DuplicateEmail() {
        // given
        MemberVO anotherMember = new MemberVO();
        anotherMember.setUserId("anothuser");
        anotherMember.setUserEmail("updated@example.com");

        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);
        when(memberMapper.getMemberByUserEmail("updated@example.com")).thenReturn(anotherMember);

        // when & then
        assertThrows(DuplicateResourceException.class, () -> {
            memberService.updateMember("testuser", validUpdateDTO);
        });
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updateMemberPassword_Success() {
        // given
        testMember.setUserPassword("encodedOldPassword");

        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123!", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123!")).thenReturn("encodedNewPassword");
        when(memberMapper.updateMemberPassword("testuser", "encodedNewPassword")).thenReturn(1);

        // when
        boolean result = memberService.updateMemberPassword("testuser", "oldPassword", "newPassword123!");

        // then
        assertTrue(result);
        verify(memberMapper).updateMemberPassword("testuser", "encodedNewPassword");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void updateMemberPassword_WrongCurrentPassword() {
        // given
        testMember.setUserPassword("encodedOldPassword");

        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);
        when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.updateMemberPassword("testuser", "wrongPassword", "newPassword123!");
        });
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteMember_Success() {
        // given
        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);
        when(memberMapper.deleteMember("testuser")).thenReturn(1);

        // when
        boolean result = memberService.deleteMember("testuser");

        // then
        assertTrue(result);
        verify(memberMapper).deleteMember("testuser");
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

    @Test
    @DisplayName("데이터베이스 연결 확인")
    void checkDatabaseConnection() {
        // given
        when(memberMapper.checkUserIdDuplicate("test_connection_check")).thenReturn(0);

        // when
        boolean result = memberService.checkDatabaseConnection();

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("데이터베이스 연결 확인 실패")
    void checkDatabaseConnection_Failed() {
        // given
        when(memberMapper.checkUserIdDuplicate("test_connection_check")).thenThrow(new RuntimeException("DB Error"));

        // when
        boolean result = memberService.checkDatabaseConnection();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("회원 상태 변경 성공")
    void updateMemberStatus_Success() {
        // given
        when(memberMapper.updateMemberActive("testuser", false)).thenReturn(1);

        // when
        boolean result = memberService.updateMemberStatus("testuser", false);

        // then
        assertTrue(result);
        verify(memberMapper).updateMemberActive("testuser", false);
    }

    @Test
    @DisplayName("관리자 비밀번호 재설정 성공")
    void resetMemberPassword_Success() {
        // given
        when(memberMapper.getMemberByUserId("testuser")).thenReturn(testMember);
        when(passwordEncoder.encode("tempPassword")).thenReturn("encodedTempPassword");
        when(memberMapper.updateMemberPassword("testuser", "encodedTempPassword")).thenReturn(1);

        // when
        boolean result = memberService.resetMemberPassword("testuser", "tempPassword");

        // then
        assertTrue(result);
        verify(memberMapper).updateMemberPassword("testuser", "encodedTempPassword");
    }

    @Test
    @DisplayName("유효성 검증 실패 - 짧은 사용자 ID")
    void validateMemberJoinData_ShortUserId() {
        // given
        validJoinDTO.setUserId("ab");

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.insertMember(validJoinDTO);
        });
    }

    @Test
    @DisplayName("유효성 검증 실패 - 약한 비밀번호")
    void validateMemberJoinData_WeakPassword() {
        // given
        validJoinDTO.setUserPassword("weak");
        validJoinDTO.setPasswordConfirm("weak");

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.insertMember(validJoinDTO);
        });
    }

    @Test
    @DisplayName("유효성 검증 실패 - 잘못된 이메일 형식")
    void validateMemberJoinData_InvalidEmail() {
        // given
        validJoinDTO.setUserEmail("invalid-email");

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.insertMember(validJoinDTO);
        });
    }

    @Test
    @DisplayName("유효성 검증 실패 - 잘못된 전화번호 형식")
    void validateMemberJoinData_InvalidPhone() {
        // given
        validJoinDTO.setUserPhone("invalid-phone");

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.insertMember(validJoinDTO);
        });
    }

    private MemberAuthVO createAuth(String userId, String auth) {
        MemberAuthVO authVO = new MemberAuthVO();
        authVO.setUserId(userId);
        authVO.setAuth(auth);
        return authVO;
    }
}