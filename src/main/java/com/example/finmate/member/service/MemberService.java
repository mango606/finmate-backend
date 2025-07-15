package com.example.finmate.member.service;

import com.example.finmate.member.domain.MemberAuthVO;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.dto.MemberInfoDTO;
import com.example.finmate.member.dto.MemberJoinDTO;
import com.example.finmate.member.dto.MemberUpdateDTO;
import com.example.finmate.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    // 회원 가입
    @Transactional
    public boolean insertMember(MemberJoinDTO joinDTO) {
        log.info("회원 가입 처리: {}", joinDTO.getUserId());

        // 사용자 ID 중복 확인
        if (memberMapper.checkUserIdDuplicate(joinDTO.getUserId()) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 사용자 ID입니다.");
        }

        // 이메일 중복 확인
        if (memberMapper.checkUserEmailDuplicate(joinDTO.getUserEmail()) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 확인
        if (!joinDTO.getUserPassword().equals(joinDTO.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 회원 정보 생성
        MemberVO member = new MemberVO();
        member.setUserId(joinDTO.getUserId());
        member.setUserPassword(passwordEncoder.encode(joinDTO.getUserPassword()));
        member.setUserName(joinDTO.getUserName());
        member.setUserEmail(joinDTO.getUserEmail());
        member.setUserPhone(joinDTO.getUserPhone());
        member.setBirthDate(joinDTO.getBirthDate());
        member.setGender(joinDTO.getGender());
        member.setActive(true);

        // 회원 등록
        int memberResult = memberMapper.insertMember(member);

        // 기본 권한 등록 (ROLE_USER)
        MemberAuthVO auth = new MemberAuthVO();
        auth.setUserId(joinDTO.getUserId());
        auth.setAuth("ROLE_USER");
        int authResult = memberMapper.insertMemberAuth(auth);

        boolean success = memberResult > 0 && authResult > 0;
        log.info("회원 가입 결과: {} - {}", joinDTO.getUserId(), success);

        return success;
    }

    // 회원 정보 조회
    public MemberInfoDTO getMemberInfo(String userId) {
        log.info("회원 정보 조회: {}", userId);

        MemberVO member = memberMapper.getMemberByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        }

        List<MemberAuthVO> authList = memberMapper.getMemberAuthByUserId(userId);
        List<String> authorities = authList.stream()
                .map(MemberAuthVO::getAuth)
                .collect(Collectors.toList());

        MemberInfoDTO memberInfo = new MemberInfoDTO();
        memberInfo.setUserId(member.getUserId());
        memberInfo.setUserName(member.getUserName());
        memberInfo.setUserEmail(member.getUserEmail());
        memberInfo.setUserPhone(member.getUserPhone());
        memberInfo.setBirthDate(member.getBirthDate());
        memberInfo.setGender(member.getGender());
        memberInfo.setRegDate(member.getRegDate());
        memberInfo.setAuthorities(authorities);

        return memberInfo;
    }

    // 회원 정보 수정
    @Transactional
    public boolean updateMember(String userId, MemberUpdateDTO updateDTO) {
        log.info("회원 정보 수정: {}", userId);

        // 현재 회원 정보 확인
        MemberVO currentMember = memberMapper.getMemberByUserId(userId);
        if (currentMember == null) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        }

        // 이메일 중복 확인 (본인 제외)
        MemberVO emailCheck = memberMapper.getMemberByUserEmail(updateDTO.getUserEmail());
        if (emailCheck != null && !emailCheck.getUserId().equals(userId)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        MemberVO member = new MemberVO();
        member.setUserId(userId);
        member.setUserName(updateDTO.getUserName());
        member.setUserEmail(updateDTO.getUserEmail());
        member.setUserPhone(updateDTO.getUserPhone());
        member.setBirthDate(updateDTO.getBirthDate());
        member.setGender(updateDTO.getGender());

        int result = memberMapper.updateMember(member);
        boolean success = result > 0;

        log.info("회원 정보 수정 결과: {} - {}", userId, success);
        return success;
    }

    // 비밀번호 변경
    @Transactional
    public boolean updateMemberPassword(String userId, String currentPassword, String newPassword) {
        log.info("비밀번호 변경: {}", userId);

        MemberVO member = memberMapper.getMemberByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, member.getUserPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        int result = memberMapper.updateMemberPassword(userId, encodedPassword);
        boolean success = result > 0;

        log.info("비밀번호 변경 결과: {} - {}", userId, success);
        return success;
    }

    // 회원 탈퇴
    @Transactional
    public boolean deleteMember(String userId) {
        log.info("회원 탈퇴: {}", userId);

        int result = memberMapper.deleteMember(userId);
        boolean success = result > 0;

        log.info("회원 탈퇴 결과: {} - {}", userId, success);
        return success;
    }

    // 사용자 ID 중복 확인
    public boolean checkUserIdDuplicate(String userId) {
        int count = memberMapper.checkUserIdDuplicate(userId);
        boolean isDuplicate = count > 0;

        log.info("사용자 ID 중복 확인: {} - {}", userId, isDuplicate);
        return isDuplicate;
    }

    // 이메일 중복 확인
    public boolean checkUserEmailDuplicate(String userEmail) {
        int count = memberMapper.checkUserEmailDuplicate(userEmail);
        boolean isDuplicate = count > 0;

        log.info("이메일 중복 확인: {} - {}", userEmail, isDuplicate);
        return isDuplicate;
    }
}