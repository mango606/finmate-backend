package com.example.finmate.member.mapper;

import com.example.finmate.member.domain.MemberAuthVO;
import com.example.finmate.member.domain.MemberVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberMapper {
    // 회원 등록
    int insertMember(MemberVO memberVO);

    // 회원 권한 등록
    int insertMemberAuth(MemberAuthVO memberAuthVO);

    // 사용자 ID로 회원 조회 (로그인용)
    MemberVO getMemberByUserId(String userId);

    // 사용자 ID로 권한 조회
    List<MemberAuthVO> getMemberAuthByUserId(String userId);

    // 이메일로 회원 조회
    MemberVO getMemberByUserEmail(String userEmail);

    // 회원 정보 수정
    int updateMember(MemberVO memberVO);

    // 비밀번호 변경
    int updateMemberPassword(@Param("userId") String userId, @Param("userPassword") String userPassword);

    // 회원 삭제 (실제로는 비활성화)
    int deleteMember(String userId);

    // 회원 활성화/비활성화
    int updateMemberActive(@Param("userId") String userId, @Param("isActive") boolean isActive);

    // 사용자 ID 중복 확인
    int checkUserIdDuplicate(String userId);

    // 이메일 중복 확인
    int checkUserEmailDuplicate(String userEmail);
}