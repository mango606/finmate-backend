package com.example.finmate.member.service;

import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.auth.mapper.AuthMapper;
import com.example.finmate.common.exception.DuplicateResourceException;
import com.example.finmate.common.exception.MemberNotFoundException;
import com.example.finmate.member.domain.MemberAuthVO;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.dto.MemberInfoDTO;
import com.example.finmate.member.dto.MemberJoinDTO;
import com.example.finmate.member.dto.MemberUpdateDTO;
import com.example.finmate.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    // AuthMapper는 선택적 의존성으로 처리 (null 체크 필요)
    @Autowired(required = false)
    private AuthMapper authMapper;

    // 회원 가입
    @Transactional
    public boolean insertMember(MemberJoinDTO joinDTO) {
        log.info("회원 가입 처리: {}", joinDTO.getUserId());

        try {
            // 입력 데이터 유효성 검증
            validateMemberJoinData(joinDTO);

            // 사용자 ID 중복 확인
            if (memberMapper.checkUserIdDuplicate(joinDTO.getUserId()) > 0) {
                throw new DuplicateResourceException("이미 사용 중인 사용자 ID입니다.");
            }

            // 이메일 중복 확인
            if (memberMapper.checkUserEmailDuplicate(joinDTO.getUserEmail()) > 0) {
                throw new DuplicateResourceException("이미 사용 중인 이메일입니다.");
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

            // 계정 보안 정보 초기화 (AuthMapper가 사용 가능한 경우에만)
            if (authMapper != null) {
                initializeAccountSecurity(joinDTO.getUserId());
            } else {
                log.warn("AuthMapper를 사용할 수 없어 계정 보안 정보 초기화를 건너뜁니다: {}", joinDTO.getUserId());
            }

            boolean success = memberResult > 0 && authResult > 0;
            log.info("회원 가입 결과: {} - {}", joinDTO.getUserId(), success);

            return success;
        } catch (DuplicateResourceException | IllegalArgumentException e) {
            // 비즈니스 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("회원 가입 실패: {}", joinDTO.getUserId(), e);
            throw new RuntimeException("회원 가입 처리 중 오류가 발생했습니다.", e);
        }
    }

    // 회원 정보 조회
    public MemberInfoDTO getMemberInfo(String userId) {
        log.info("회원 정보 조회: {}", userId);

        try {
            MemberVO member = memberMapper.getMemberByUserId(userId);
            if (member == null) {
                throw new MemberNotFoundException("회원을 찾을 수 없습니다: " + userId);
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
        } catch (MemberNotFoundException e) {
            // 커스텀 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("회원 정보 조회 실패: {}", userId, e);
            throw new RuntimeException("회원 정보 조회에 실패했습니다.", e);
        }
    }

    // 회원 정보 수정
    @Transactional
    public boolean updateMember(String userId, MemberUpdateDTO updateDTO) {
        log.info("회원 정보 수정: {}", userId);

        try {
            // 현재 회원 정보 확인
            MemberVO currentMember = memberMapper.getMemberByUserId(userId);
            if (currentMember == null) {
                throw new MemberNotFoundException("회원을 찾을 수 없습니다: " + userId);
            }

            // 입력 데이터 유효성 검증
            validateMemberUpdateData(updateDTO);

            // 이메일 중복 확인 (본인 제외)
            MemberVO emailCheck = memberMapper.getMemberByUserEmail(updateDTO.getUserEmail());
            if (emailCheck != null && !emailCheck.getUserId().equals(userId)) {
                throw new DuplicateResourceException("이미 사용 중인 이메일입니다.");
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
        } catch (MemberNotFoundException | DuplicateResourceException e) {
            // 커스텀 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("회원 정보 수정 실패: {}", userId, e);
            throw new RuntimeException("회원 정보 수정에 실패했습니다.", e);
        }
    }

    // 비밀번호 확인 (개인정보 수정용)
    public boolean verifyPassword(String userId, String password) {
        log.info("비밀번호 확인: {}", userId);

        try {
            MemberVO member = memberMapper.getMemberByUserId(userId);
            if (member == null) {
                throw new MemberNotFoundException("회원을 찾을 수 없습니다: " + userId);
            }

            boolean isValid = passwordEncoder.matches(password, member.getUserPassword());
            log.info("비밀번호 확인 결과: {} - {}", userId, isValid);

            return isValid;

        } catch (MemberNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("비밀번호 확인 실패: {}", userId, e);
            return false;
        }
    }

    // 비밀번호 변경
    @Transactional
    public boolean updateMemberPassword(String userId, String currentPassword, String newPassword) {
        log.info("비밀번호 변경: {}", userId);

        try {
            MemberVO member = memberMapper.getMemberByUserId(userId);
            if (member == null) {
                throw new MemberNotFoundException("회원을 찾을 수 없습니다: " + userId);
            }

            // 현재 비밀번호 확인
            if (!passwordEncoder.matches(currentPassword, member.getUserPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }

            // 새 비밀번호 유효성 검증
            validatePassword(newPassword);

            // 새 비밀번호와 현재 비밀번호가 같은지 확인
            if (passwordEncoder.matches(newPassword, member.getUserPassword())) {
                throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
            }

            String encodedPassword = passwordEncoder.encode(newPassword);
            int result = memberMapper.updateMemberPassword(userId, encodedPassword);
            boolean success = result > 0;

            log.info("비밀번호 변경 결과: {} - {}", userId, success);
            return success;
        } catch (MemberNotFoundException | IllegalArgumentException e) {
            // 커스텀 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("비밀번호 변경 실패: {}", userId, e);
            throw new RuntimeException("비밀번호 변경에 실패했습니다.", e);
        }
    }

    // 회원 탈퇴
    @Transactional
    public boolean deleteMember(String userId) {
        log.info("회원 탈퇴: {}", userId);

        try {
            MemberVO member = memberMapper.getMemberByUserId(userId);
            if (member == null) {
                throw new MemberNotFoundException("회원을 찾을 수 없습니다: " + userId);
            }

            // 계정 비활성화 (실제 삭제가 아닌 소프트 삭제)
            int result = memberMapper.deleteMember(userId);
            boolean success = result > 0;

            log.info("회원 탈퇴 결과: {} - {}", userId, success);
            return success;
        } catch (MemberNotFoundException e) {
            // 커스텀 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("회원 탈퇴 실패: {}", userId, e);
            throw new RuntimeException("회원 탈퇴에 실패했습니다.", e);
        }
    }

    // 사용자 ID 중복 확인
    public boolean checkUserIdDuplicate(String userId) {
        try {
            int count = memberMapper.checkUserIdDuplicate(userId);
            boolean isDuplicate = count > 0;

            log.info("사용자 ID 중복 확인: {} - {}", userId, isDuplicate);
            return isDuplicate;
        } catch (Exception e) {
            log.error("사용자 ID 중복 확인 실패: {}", userId, e);
            return true; // 오류 시 중복으로 처리하여 안전하게
        }
    }

    // 이메일 중복 확인
    public boolean checkUserEmailDuplicate(String userEmail) {
        try {
            int count = memberMapper.checkUserEmailDuplicate(userEmail);
            boolean isDuplicate = count > 0;

            log.info("이메일 중복 확인: {} - {}", userEmail, isDuplicate);
            return isDuplicate;
        } catch (Exception e) {
            log.error("이메일 중복 확인 실패: {}", userEmail, e);
            return true; // 오류 시 중복으로 처리하여 안전하게
        }
    }

    // 계정 보안 정보 초기화 (AuthMapper가 사용 가능한 경우에만)
    private void initializeAccountSecurity(String userId) {
        try {
            if (authMapper == null) {
                log.warn("AuthMapper를 사용할 수 없어 계정 보안 정보 초기화를 건너뜁니다: {}", userId);
                return;
            }

            AccountSecurityVO security = new AccountSecurityVO();
            security.setUserId(userId);
            security.setEmailVerified(false);
            security.setPhoneVerified(false);
            security.setTwoFactorEnabled(false);
            security.setAccountLocked(false);
            security.setLoginFailCount(0);
            security.setLastLoginTime(null);
            security.setLockTime(null);
            security.setLockReason(null);

            authMapper.insertAccountSecurity(security);
            log.info("계정 보안 정보 초기화 완료: {}", userId);
        } catch (Exception e) {
            log.warn("계정 보안 정보 초기화 실패: {} - {}", userId, e.getMessage());
            // 보안 정보 초기화 실패는 회원가입을 막지 않도록 함
        }
    }

    // 데이터베이스 연결 상태 확인
    public boolean checkDatabaseConnection() {
        try {
            // 간단한 쿼리로 DB 연결 확인
            int count = memberMapper.checkUserIdDuplicate("test_connection_check");
            return true;
        } catch (Exception e) {
            log.error("데이터베이스 연결 확인 실패", e);
            return false;
        }
    }

    // 회원 통계 조회
    public Map<String, Object> getMemberStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 기본 통계 - 실제 구현에서는 별도의 통계 쿼리 사용
            statistics.put("totalMembers", 0);
            statistics.put("activeMembers", 0);
            statistics.put("newMembersToday", 0);
            statistics.put("newMembersThisMonth", 0);

            return statistics;
        } catch (Exception e) {
            log.error("회원 통계 조회 실패", e);
            return statistics;
        }
    }

    // 회원 상태 변경 (관리자용)
    @Transactional
    public boolean updateMemberStatus(String userId, boolean isActive) {
        try {
            int result = memberMapper.updateMemberActive(userId, isActive);
            boolean success = result > 0;

            log.info("회원 상태 변경: {} - active: {}, 결과: {}", userId, isActive, success);
            return success;
        } catch (Exception e) {
            log.error("회원 상태 변경 실패: {}", userId, e);
            throw new RuntimeException("회원 상태 변경에 실패했습니다.", e);
        }
    }

    // 회원 검색 (관리자용)
    public List<MemberVO> searchMembers(String searchType, String searchValue, int page, int size) {
        try {
            // 실제 구현에서는 동적 쿼리를 사용하여 검색 조건에 따라 회원 조회
            // 현재는 기본 구현만 제공
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("회원 검색 실패: {} - {}", searchType, searchValue, e);
            return new java.util.ArrayList<>();
        }
    }

    // 비밀번호 재설정 (관리자용)
    @Transactional
    public boolean resetMemberPassword(String userId, String tempPassword) {
        try {
            MemberVO member = memberMapper.getMemberByUserId(userId);
            if (member == null) {
                throw new MemberNotFoundException("회원을 찾을 수 없습니다: " + userId);
            }

            String encodedPassword = passwordEncoder.encode(tempPassword);
            int result = memberMapper.updateMemberPassword(userId, encodedPassword);
            boolean success = result > 0;

            log.info("관리자 비밀번호 재설정: {} - {}", userId, success);
            return success;
        } catch (MemberNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("관리자 비밀번호 재설정 실패: {}", userId, e);
            throw new RuntimeException("비밀번호 재설정에 실패했습니다.", e);
        }
    }

    // 회원 가입 데이터 유효성 검증
    private void validateMemberJoinData(MemberJoinDTO joinDTO) {
        if (joinDTO.getUserId().length() < 4 || joinDTO.getUserId().length() > 20) {
            throw new IllegalArgumentException("사용자 ID는 4-20자 사이여야 합니다.");
        }

        if (!joinDTO.getUserId().matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("사용자 ID는 영문, 숫자, 언더스코어만 사용 가능합니다.");
        }

        if (joinDTO.getUserPassword().length() < 8 || joinDTO.getUserPassword().length() > 20) {
            throw new IllegalArgumentException("비밀번호는 8-20자 사이여야 합니다.");
        }

        if (!joinDTO.getUserPassword().matches("^(?=.*[a-zA-Z])(?=.*[\\d@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$")) {
            throw new IllegalArgumentException("비밀번호는 영문자와 숫자 또는 특수문자를 포함해야 합니다.");
        }

        if (joinDTO.getUserName().length() < 2 || joinDTO.getUserName().length() > 10) {
            throw new IllegalArgumentException("사용자 이름은 2-10자 사이여야 합니다.");
        }

        // 이메일 형식 검증
        if (!joinDTO.getUserEmail().matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }

        // 전화번호 형식 검증 (선택사항)
        if (joinDTO.getUserPhone() != null && !joinDTO.getUserPhone().isEmpty()) {
            if (!joinDTO.getUserPhone().matches("^01[0-9]-\\d{4}-\\d{4}$")) {
                throw new IllegalArgumentException("올바른 전화번호 형식이 아닙니다.");
            }
        }
    }

    // 회원 수정 데이터 유효성 검증
    private void validateMemberUpdateData(MemberUpdateDTO updateDTO) {
        if (updateDTO.getUserName().length() < 2 || updateDTO.getUserName().length() > 10) {
            throw new IllegalArgumentException("사용자 이름은 2-10자 사이여야 합니다.");
        }

        // 이메일 형식 검증
        if (!updateDTO.getUserEmail().matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }

        // 전화번호 형식 검증 (선택사항)
        if (updateDTO.getUserPhone() != null && !updateDTO.getUserPhone().isEmpty()) {
            if (!updateDTO.getUserPhone().matches("^01[0-9]-\\d{4}-\\d{4}$")) {
                throw new IllegalArgumentException("올바른 전화번호 형식이 아닙니다.");
            }
        }
    }

    // 비밀번호 유효성 검증
    private void validatePassword(String password) {
        if (password.length() < 8 || password.length() > 20) {
            throw new IllegalArgumentException("비밀번호는 8-20자 사이여야 합니다.");
        }

        if (!password.matches("^(?=.*[a-zA-Z])(?=.*[\\d@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$")) {
            throw new IllegalArgumentException("비밀번호는 영문자와 숫자 또는 특수문자를 포함해야 합니다.");
        }
    }
}