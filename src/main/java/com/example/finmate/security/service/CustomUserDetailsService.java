package com.example.finmate.security.service;

import com.example.finmate.auth.domain.AccountSecurityVO;
import com.example.finmate.auth.mapper.AuthMapper;
import com.example.finmate.common.exception.AccountLockedException;
import com.example.finmate.member.domain.MemberAuthVO;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("customUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberMapper memberMapper;

    @Autowired(required = false)
    private AuthMapper authMapper;

    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.info("사용자 정보 로드: {}", userId);

        MemberVO member = memberMapper.getMemberByUserId(userId);
        if (member == null) {
            log.warn("사용자를 찾을 수 없음: {}", userId);
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }

        if (!member.isActive()) {
            log.warn("비활성화된 계정: {}", userId);
            throw new UsernameNotFoundException("비활성화된 계정입니다: " + userId);
        }

        // 계정 잠금 상태 확인 및 자동 해제 처리
        checkAndHandleAccountLock(userId);

        List<MemberAuthVO> authList = memberMapper.getMemberAuthByUserId(userId);
        List<GrantedAuthority> authorities = authList.stream()
                .map(auth -> new SimpleGrantedAuthority(auth.getAuth()))
                .collect(Collectors.toList());

        log.info("사용자 권한: {}", authorities);

        return User.builder()
                .username(member.getUserId())
                .password(member.getUserPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false) // 이미 위에서 검증했으므로 false
                .credentialsExpired(false)
                .disabled(!member.isActive())
                .build();
    }

    private void checkAndHandleAccountLock(String userId) {
        if (authMapper == null) {
            log.debug("AuthMapper를 사용할 수 없어 계정 잠금 확인을 건너뜁니다: {}", userId);
            return;
        }

        try {
            AccountSecurityVO security = authMapper.getAccountSecurity(userId);

            if (security == null) {
                // 계정 보안 정보가 없으면 기본값으로 생성
                initializeAccountSecurity(userId);
                return;
            }

            if (!Boolean.TRUE.equals(security.getAccountLocked())) {
                // 계정이 잠겨있지 않으면 통과
                return;
            }

            if (security.getLockTime() == null) {
                // 잠금 시간이 설정되지 않은 영구 잠금 (관리자 잠금)
                log.warn("영구 잠긴 계정 로그인 시도: {}", userId);
                throw new AccountLockedException("계정이 잠금 상태입니다. 관리자에게 문의하세요.");
            }

            // 자동 해제 시간 계산
            LocalDateTime unlockTime = security.getLockTime().plusMinutes(ACCOUNT_LOCK_DURATION_MINUTES);

            if (LocalDateTime.now().isAfter(unlockTime)) {
                // 잠금 시간이 지났으므로 자동 해제
                autoUnlockAccount(userId);
                log.info("자동 계정 잠금 해제: {}", userId);
            } else {
                // 아직 잠금 시간이 남음
                String unlockTimeStr = unlockTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                log.warn("잠긴 계정 로그인 시도: {} (해제 시간: {})", userId, unlockTimeStr);
                throw new AccountLockedException(
                        String.format("계정이 잠금 상태입니다. %s 이후 다시 시도해주세요.", unlockTimeStr)
                );
            }

        } catch (AccountLockedException e) {
            throw e; // 계정 잠금 예외는 다시 던짐
        } catch (Exception e) {
            log.warn("계정 보안 정보 조회/처리 실패: {} - {}", userId, e.getMessage());
            // 보안 정보 조회 실패는 로그인을 막지 않도록 함
        }
    }

    private void initializeAccountSecurity(String userId) {
        try {
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
        }
    }

    private void autoUnlockAccount(String userId) {
        try {
            authMapper.updateAccountLockStatus(userId, false);
            authMapper.resetLoginFailCount(userId);
            log.info("계정 자동 잠금 해제 완료: {}", userId);
        } catch (Exception e) {
            log.error("계정 자동 잠금 해제 실패: {} - {}", userId, e.getMessage());
            throw new RuntimeException("계정 잠금 해제 처리 중 오류가 발생했습니다.", e);
        }
    }

    public boolean isAccountLocked(String userId) {
        if (authMapper == null) {
            return false;
        }

        try {
            AccountSecurityVO security = authMapper.getAccountSecurity(userId);
            if (security == null || !Boolean.TRUE.equals(security.getAccountLocked())) {
                return false;
            }

            if (security.getLockTime() == null) {
                return true; // 영구 잠금
            }

            // 자동 해제 시간 확인
            LocalDateTime unlockTime = security.getLockTime().plusMinutes(ACCOUNT_LOCK_DURATION_MINUTES);
            return LocalDateTime.now().isBefore(unlockTime);

        } catch (Exception e) {
            log.warn("계정 잠금 상태 확인 실패: {} - {}", userId, e.getMessage());
            return false;
        }
    }

    public long getMinutesUntilUnlock(String userId) {
        if (authMapper == null) {
            return 0;
        }

        try {
            AccountSecurityVO security = authMapper.getAccountSecurity(userId);
            if (security == null || !Boolean.TRUE.equals(security.getAccountLocked()) || security.getLockTime() == null) {
                return 0;
            }

            LocalDateTime unlockTime = security.getLockTime().plusMinutes(ACCOUNT_LOCK_DURATION_MINUTES);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(unlockTime)) {
                return 0; // 이미 해제 시간이 지남
            }

            return java.time.Duration.between(now, unlockTime).toMinutes();

        } catch (Exception e) {
            log.warn("계정 잠금 해제 시간 계산 실패: {} - {}", userId, e.getMessage());
            return 0;
        }
    }
}