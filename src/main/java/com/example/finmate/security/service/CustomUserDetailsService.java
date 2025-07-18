package com.example.finmate.security.service;

import com.example.finmate.member.domain.MemberAuthVO;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("customUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberMapper memberMapper;

    @Override
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
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!member.isActive())
                .build();
    }
}