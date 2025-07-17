package com.example.finmate.financial.mapper;

import com.example.finmate.financial.domain.FinancialGoalVO;
import com.example.finmate.financial.domain.FinancialProfileVO;
import com.example.finmate.financial.domain.FinancialTransactionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FinancialMapper {

    // 금융 프로필 관련
    FinancialProfileVO getFinancialProfile(String userId);

    int insertFinancialProfile(FinancialProfileVO profile);

    int updateFinancialProfile(FinancialProfileVO profile);

    int deleteFinancialProfile(String userId);

    // 금융 목표 관련
    List<FinancialGoalVO> getFinancialGoals(@Param("userId") String userId,
                                            @Param("status") String status,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    int getFinancialGoalsCount(@Param("userId") String userId, @Param("status") String status);

    FinancialGoalVO getFinancialGoal(Long goalId);

    int insertFinancialGoal(FinancialGoalVO goal);

    int updateFinancialGoal(FinancialGoalVO goal);

    int deleteFinancialGoal(Long goalId);

    int updateGoalAmount(@Param("goalId") Long goalId, @Param("amount") java.math.BigDecimal amount);

    // 금융 거래 관련
    List<FinancialTransactionVO> getFinancialTransactions(@Param("userId") String userId,
                                                          @Param("type") String type,
                                                          @Param("startDate") String startDate,
                                                          @Param("endDate") String endDate,
                                                          @Param("offset") int offset,
                                                          @Param("limit") int limit);

    int getFinancialTransactionsCount(@Param("userId") String userId,
                                      @Param("type") String type,
                                      @Param("startDate") String startDate,
                                      @Param("endDate") String endDate);

    FinancialTransactionVO getFinancialTransaction(Long transactionId);

    int insertFinancialTransaction(FinancialTransactionVO transaction);

    int updateFinancialTransaction(FinancialTransactionVO transaction);

    int deleteFinancialTransaction(Long transactionId);

    // 통계 관련
    List<java.util.Map<String, Object>> getMonthlyGoalProgress(@Param("userId") String userId,
                                                               @Param("months") int months);

    List<java.util.Map<String, Object>> getCategoryExpenseStats(@Param("userId") String userId,
                                                                @Param("startDate") String startDate,
                                                                @Param("endDate") String endDate);

    List<java.util.Map<String, Object>> getAssetTrend(@Param("userId") String userId,
                                                      @Param("months") int months);

    // 대시보드 관련
    List<FinancialGoalVO> getRecentGoals(@Param("userId") String userId, @Param("limit") int limit);

    List<FinancialTransactionVO> getRecentTransactions(@Param("userId") String userId, @Param("limit") int limit);

    java.util.Map<String, Object> getFinancialSummary(String userId);
}