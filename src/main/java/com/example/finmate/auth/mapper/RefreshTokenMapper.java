package com.example.finmate.auth.mapper;

import com.example.finmate.auth.domain.RefreshTokenVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RefreshTokenMapper {

    // Refresh Token 생성
    int insertRefreshToken(RefreshTokenVO refreshToken);

    // Refresh Token 조회
    RefreshTokenVO getRefreshToken(@Param("refreshToken") String refreshToken);

    // 사용자의 Refresh Token 조회
    List<RefreshTokenVO> getRefreshTokensByUserId(@Param("userId") String userId);

    // Refresh Token 무효화
    int invalidateRefreshToken(@Param("refreshToken") String refreshToken);

    // 사용자의 모든 Refresh Token 무효화
    int invalidateAllRefreshTokensByUserId(@Param("userId") String userId);

    // 만료된 Refresh Token 삭제
    int deleteExpiredRefreshTokens();

    // Refresh Token 사용 시간 업데이트
    int updateLastUsedTime(@Param("refreshToken") String refreshToken);

    // 사용자별 활성 Refresh Token 개수 조회
    int getActiveTokenCount(@Param("userId") String userId);

    // 오래된 Refresh Token 삭제 (사용자당 최대 5개 유지)
    int deleteOldestTokensForUser(@Param("userId") String userId, @Param("keepCount") int keepCount);
}