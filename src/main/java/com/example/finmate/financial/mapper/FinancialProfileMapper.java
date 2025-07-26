package com.example.finmate.financial.mapper;

import com.example.finmate.financial.domain.FinancialProfileVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinancialProfileMapper {

    // 금융 프로필 등록
    int insertFinancialProfile(FinancialProfileVO financialProfile);

    // 사용자 ID로 금융 프로필 조회
    FinancialProfileVO getFinancialProfileByUserId(String userId);

    // 금융 프로필 수정
    int updateFinancialProfile(FinancialProfileVO financialProfile);

    // 금융 프로필 삭제
    int deleteFinancialProfile(String userId);

    // 금융 프로필 존재 여부 확인
    int checkFinancialProfileExists(String userId);

    // 투자 성향별 사용자 수 조회 (통계용)
    int getUserCountByRiskProfile(@Param("riskProfile") String riskProfile);

    // 소득 구간별 사용자 수 조회 (통계용)
    int getUserCountByIncomeRange(@Param("incomeRange") String incomeRange);
}