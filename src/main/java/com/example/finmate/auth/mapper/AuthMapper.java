package com.example.finmate.auth.mapper;

import com.example.finmate.auth.domain.AuthTokenVO;
import com.example.finmate.auth.domain.LoginHistoryVO;
import com.example.finmate.auth.domain.AccountSecurityVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthMapper {

    // 인증 토큰 관련
    int insertAuthToken(AuthTokenVO authToken);
    AuthTokenVO getAuthToken(@Param("token") String token, @Param("tokenType") String tokenType);
    int markTokenAsUsed(String token);
    int deleteExpiredTokens(@Param("userId") String userId, @Param("tokenType") String tokenType);

    // 로그인 이력 관련
    int insertLoginHistory(LoginHistoryVO loginHistory);
    List<LoginHistoryVO> getLoginHistories(@Param("userId") String userId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);
    int getLoginHistoryCount(String userId);

    // 계정 보안 관련
    AccountSecurityVO getAccountSecurity(String userId);
    int insertAccountSecurity(AccountSecurityVO accountSecurity);
    int updateAccountSecurity(AccountSecurityVO accountSecurity);
    int updateEmailVerificationStatus(@Param("userId") String userId, @Param("verified") boolean verified);
    int updatePhoneVerificationStatus(@Param("userId") String userId, @Param("verified") boolean verified);
    int updateTwoFactorStatus(@Param("userId") String userId, @Param("enabled") boolean enabled);
    int updateAccountLockStatus(@Param("userId") String userId, @Param("locked") boolean locked);
    int updateLastLoginTime(@Param("userId") String userId);
    int incrementLoginFailCount(String userId);
    int resetLoginFailCount(String userId);
    int getLoginFailCount(String userId);

    // 보안 관련 통계
    List<java.util.Map<String, Object>> getLoginStatistics(@Param("userId") String userId,
                                                           @Param("days") int days);
    List<java.util.Map<String, Object>> getSecurityEvents(@Param("userId") String userId,
                                                          @Param("days") int days);
}