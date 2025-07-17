package com.example.finmate.financial.service;

import com.example.finmate.financial.domain.FinancialGoalVO;
import com.example.finmate.financial.domain.FinancialProfileVO;
import com.example.finmate.financial.dto.FinancialGoalDTO;
import com.example.finmate.financial.dto.FinancialProfileDTO;
import com.example.finmate.financial.mapper.FinancialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final FinancialMapper financialMapper;

    // 금융 프로필 조회
    public Map<String, Object> getFinancialProfile(String userId) {
        FinancialProfileVO profile = financialMapper.getFinancialProfile(userId);

        Map<String, Object> result = new HashMap<>();

        if (profile == null) {
            result.put("hasProfile", false);
            result.put("profile", null);
        } else {
            result.put("hasProfile", true);
            result.put("profile", profile);
            result.put("analysis", analyzeFinancialHealth(profile));
        }

        return result;
    }

    // 금융 프로필 저장
    @Transactional
    public boolean saveFinancialProfile(String userId, FinancialProfileDTO profileDTO) {
        validateFinancialProfile(profileDTO);

        FinancialProfileVO existingProfile = financialMapper.getFinancialProfile(userId);

        FinancialProfileVO profile = new FinancialProfileVO();
        profile.setUserId(userId);
        profile.setIncomeRange(profileDTO.getIncomeRange());
        profile.setJobType(profileDTO.getJobType());
        profile.setMonthlyIncome(profileDTO.getMonthlyIncome());
        profile.setMonthlyExpense(profileDTO.getMonthlyExpense());
        profile.setTotalAssets(profileDTO.getTotalAssets());
        profile.setTotalDebt(profileDTO.getTotalDebt());
        profile.setRiskProfile(profileDTO.getRiskProfile());
        profile.setInvestmentGoal(profileDTO.getInvestmentGoal());
        profile.setInvestmentPeriod(profileDTO.getInvestmentPeriod());
        profile.setEmergencyFund(profileDTO.getEmergencyFund());
        profile.setCreditScore(profileDTO.getCreditScore());

        int result;
        if (existingProfile == null) {
            result = financialMapper.insertFinancialProfile(profile);
        } else {
            profile.setProfileId(existingProfile.getProfileId());
            result = financialMapper.updateFinancialProfile(profile);
        }

        return result > 0;
    }

    // 금융 목표 조회
    public Map<String, Object> getFinancialGoals(String userId, String status, int page, int size) {
        int offset = page * size;

        List<FinancialGoalVO> goals = financialMapper.getFinancialGoals(userId, status, offset, size);
        int totalCount = financialMapper.getFinancialGoalsCount(userId, status);

        // 각 목표의 달성률 계산
        for (FinancialGoalVO goal : goals) {
            calculateGoalProgress(goal);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("goals", goals);
        result.put("totalCount", totalCount);
        result.put("currentPage", page);
        result.put("pageSize", size);
        result.put("totalPages", (totalCount + size - 1) / size);

        return result;
    }

    // 금융 목표 생성
    @Transactional
    public boolean createFinancialGoal(String userId, FinancialGoalDTO goalDTO) {
        validateFinancialGoal(goalDTO);

        FinancialGoalVO goal = new FinancialGoalVO();
        goal.setUserId(userId);
        goal.setGoalName(goalDTO.getGoalName());
        goal.setGoalType(goalDTO.getGoalType());
        goal.setTargetAmount(goalDTO.getTargetAmount());
        goal.setCurrentAmount(goalDTO.getCurrentAmount() != null ? goalDTO.getCurrentAmount() : BigDecimal.ZERO);
        goal.setMonthlyContribution(goalDTO.getMonthlyContribution());
        goal.setTargetDate(goalDTO.getTargetDate());
        goal.setStartDate(goalDTO.getStartDate());
        goal.setPriority(goalDTO.getPriority());
        goal.setStatus("ACTIVE");
        goal.setDescription(goalDTO.getDescription());

        // 예상 달성 기간 계산
        goal.setExpectedMonths(calculateExpectedMonths(goal));

        int result = financialMapper.insertFinancialGoal(goal);
        return result > 0;
    }

    // 금융 목표 수정
    @Transactional
    public boolean updateFinancialGoal(String userId, Long goalId, FinancialGoalDTO goalDTO) {
        FinancialGoalVO existingGoal = financialMapper.getFinancialGoal(goalId);

        if (existingGoal == null || !existingGoal.getUserId().equals(userId)) {
            throw new IllegalArgumentException("존재하지 않는 목표이거나 접근 권한이 없습니다.");
        }

        validateFinancialGoal(goalDTO);

        FinancialGoalVO goal = new FinancialGoalVO();
        goal.setGoalId(goalId);
        goal.setUserId(userId);
        goal.setGoalName(goalDTO.getGoalName());
        goal.setGoalType(goalDTO.getGoalType());
        goal.setTargetAmount(goalDTO.getTargetAmount());
        goal.setCurrentAmount(goalDTO.getCurrentAmount() != null ? goalDTO.getCurrentAmount() : existingGoal.getCurrentAmount());
        goal.setMonthlyContribution(goalDTO.getMonthlyContribution());
        goal.setTargetDate(goalDTO.getTargetDate());
        goal.setStartDate(goalDTO.getStartDate());
        goal.setPriority(goalDTO.getPriority());
        goal.setDescription(goalDTO.getDescription());

        // 예상 달성 기간 재계산
        goal.setExpectedMonths(calculateExpectedMonths(goal));

        int result = financialMapper.updateFinancialGoal(goal);
        return result > 0;
    }

    // 금융 목표 삭제
    @Transactional
    public boolean deleteFinancialGoal(String userId, Long goalId) {
        FinancialGoalVO existingGoal = financialMapper.getFinancialGoal(goalId);

        if (existingGoal == null || !existingGoal.getUserId().equals(userId)) {
            throw new IllegalArgumentException("존재하지 않는 목표이거나 접근 권한이 없습니다.");
        }

        int result = financialMapper.deleteFinancialGoal(goalId);
        return result > 0;
    }

    // 금융 대시보드
    public Map<String, Object> getFinancialDashboard(String userId) {
        Map<String, Object> dashboard = new HashMap<>();

        // 금융 프로필 조회
        FinancialProfileVO profile = financialMapper.getFinancialProfile(userId);
        dashboard.put("profile", profile);

        // 활성 목표 조회
        List<FinancialGoalVO> activeGoals = financialMapper.getFinancialGoals(userId, "ACTIVE", 0, 5);
        for (FinancialGoalVO goal : activeGoals) {
            calculateGoalProgress(goal);
        }
        dashboard.put("activeGoals", activeGoals);

        // 목표 통계
        Map<String, Object> goalStats = new HashMap<>();
        goalStats.put("totalGoals", financialMapper.getFinancialGoalsCount(userId, null));
        goalStats.put("activeGoals", financialMapper.getFinancialGoalsCount(userId, "ACTIVE"));
        goalStats.put("completedGoals", financialMapper.getFinancialGoalsCount(userId, "COMPLETED"));
        dashboard.put("goalStats", goalStats);

        // 월별 진행 상황
        dashboard.put("monthlyProgress", getMonthlyProgress(userId));

        // 재정 건강도 분석
        if (profile != null) {
            dashboard.put("financialHealth", analyzeFinancialHealth(profile));
        }

        return dashboard;
    }

    // 투자 성향 분석
    public Map<String, Object> analyzeRiskProfile(String userId) {
        FinancialProfileVO profile = financialMapper.getFinancialProfile(userId);

        if (profile == null) {
            throw new IllegalArgumentException("금융 프로필이 등록되지 않았습니다.");
        }

        Map<String, Object> analysis = new HashMap<>();

        // 기본 위험 성향
        analysis.put("riskProfile", profile.getRiskProfile());

        // 위험 성향별 특징
        Map<String, Object> characteristics = new HashMap<>();
        switch (profile.getRiskProfile()) {
            case "CONSERVATIVE":
                characteristics.put("description", "안정적인 투자를 선호하며 원금 보장을 중시합니다.");
                characteristics.put("recommendedAssets", Arrays.asList("예금", "적금", "국채", "안전한 펀드"));
                characteristics.put("riskLevel", "낮음");
                characteristics.put("expectedReturn", "3-5%");
                break;
            case "MODERATE":
                characteristics.put("description", "적절한 위험을 감수하여 안정적인 수익을 추구합니다.");
                characteristics.put("recommendedAssets", Arrays.asList("혼합형 펀드", "주식형 펀드", "ETF", "리츠"));
                characteristics.put("riskLevel", "보통");
                characteristics.put("expectedReturn", "5-8%");
                break;
            case "AGGRESSIVE":
                characteristics.put("description", "높은 위험을 감수하여 높은 수익을 추구합니다.");
                characteristics.put("recommendedAssets", Arrays.asList("주식", "고수익 펀드", "파생상품", "해외투자"));
                characteristics.put("riskLevel", "높음");
                characteristics.put("expectedReturn", "8-15%");
                break;
        }
        analysis.put("characteristics", characteristics);

        // 포트폴리오 추천 비율
        analysis.put("recommendedPortfolio", getRecommendedPortfolio(profile.getRiskProfile()));

        return analysis;
    }

    // 포트폴리오 추천
    public Map<String, Object> getPortfolioRecommendation(String userId) {
        FinancialProfileVO profile = financialMapper.getFinancialProfile(userId);

        if (profile == null) {
            throw new IllegalArgumentException("금융 프로필이 등록되지 않았습니다.");
        }

        Map<String, Object> recommendation = new HashMap<>();

        // 위험 성향별 포트폴리오
        Map<String, BigDecimal> portfolio = getRecommendedPortfolio(profile.getRiskProfile());
        recommendation.put("portfolio", portfolio);

        // 월 적립액 기준 투자 금액 계산
        BigDecimal monthlyInvestment = calculateMonthlyInvestment(profile);
        recommendation.put("monthlyInvestment", monthlyInvestment);

        // 자산별 월 투자 금액
        Map<String, BigDecimal> monthlyAllocation = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : portfolio.entrySet()) {
            BigDecimal amount = monthlyInvestment.multiply(entry.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            monthlyAllocation.put(entry.getKey(), amount);
        }
        recommendation.put("monthlyAllocation", monthlyAllocation);

        // 투자 목표별 추천
        recommendation.put("goalBasedRecommendation", getGoalBasedRecommendation(profile));

        return recommendation;
    }

    // 금융 통계
    public Map<String, Object> getFinancialStatistics(String userId, String period) {
        Map<String, Object> statistics = new HashMap<>();

        // 기간별 목표 달성률
        statistics.put("goalProgress", getGoalProgressByPeriod(userId, period));

        // 자산 증감 추이
        statistics.put("assetTrend", getAssetTrend(userId, period));

        // 카테고리별 통계
        statistics.put("categoryStats", getCategoryStatistics(userId, period));

        return statistics;
    }

    // 헬퍼 메서드들
    private void validateFinancialProfile(FinancialProfileDTO profileDTO) {
        if (profileDTO.getMonthlyIncome().compareTo(profileDTO.getMonthlyExpense()) < 0) {
            throw new IllegalArgumentException("월 지출이 월 소득보다 클 수 없습니다.");
        }

        if (profileDTO.getTotalAssets() != null && profileDTO.getTotalDebt() != null) {
            if (profileDTO.getTotalDebt().compareTo(profileDTO.getTotalAssets()) > 0) {
                log.warn("총 부채가 총 자산보다 큽니다: {}", profileDTO);
            }
        }
    }

    private void validateFinancialGoal(FinancialGoalDTO goalDTO) {
        if (goalDTO.getTargetDate().isBefore(goalDTO.getStartDate())) {
            throw new IllegalArgumentException("목표 달성 일자는 시작일보다 이후여야 합니다.");
        }

        if (goalDTO.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("시작일은 현재 날짜 이후여야 합니다.");
        }

        if (goalDTO.getCurrentAmount() != null && goalDTO.getCurrentAmount().compareTo(goalDTO.getTargetAmount()) > 0) {
            throw new IllegalArgumentException("현재 금액이 목표 금액보다 클 수 없습니다.");
        }
    }

    private void calculateGoalProgress(FinancialGoalVO goal) {
        if (goal.getCurrentAmount() != null && goal.getTargetAmount() != null) {
            BigDecimal progress = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            goal.setProgressPercentage(progress);
        }
    }

    private Integer calculateExpectedMonths(FinancialGoalVO goal) {
        if (goal.getMonthlyContribution() == null || goal.getMonthlyContribution().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal remainingAmount = goal.getTargetAmount().subtract(
                goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO);

        return remainingAmount.divide(goal.getMonthlyContribution(), 0, RoundingMode.CEILING).intValue();
    }

    private Map<String, Object> analyzeFinancialHealth(FinancialProfileVO profile) {
        Map<String, Object> health = new HashMap<>();

        // 저축률 계산
        BigDecimal savingsRate = profile.getMonthlyIncome().subtract(profile.getMonthlyExpense())
                .divide(profile.getMonthlyIncome(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        health.put("savingsRate", savingsRate);

        // 부채비율 계산
        if (profile.getTotalAssets() != null && profile.getTotalDebt() != null) {
            BigDecimal debtRatio = profile.getTotalDebt()
                    .divide(profile.getTotalAssets(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            health.put("debtRatio", debtRatio);
        }

        // 비상금 적정성 평가
        if (profile.getEmergencyFund() != null) {
            BigDecimal emergencyFundMonths = profile.getEmergencyFund()
                    .divide(profile.getMonthlyExpense(), 2, RoundingMode.HALF_UP);
            health.put("emergencyFundMonths", emergencyFundMonths);
        }

        // 종합 점수 계산
        int totalScore = calculateFinancialScore(profile);
        health.put("totalScore", totalScore);
        health.put("grade", getFinancialGrade(totalScore));

        return health;
    }

    private Map<String, BigDecimal> getRecommendedPortfolio(String riskProfile) {
        Map<String, BigDecimal> portfolio = new HashMap<>();

        switch (riskProfile) {
            case "CONSERVATIVE":
                portfolio.put("예금/적금", BigDecimal.valueOf(40));
                portfolio.put("국채/회사채", BigDecimal.valueOf(30));
                portfolio.put("안전한 펀드", BigDecimal.valueOf(20));
                portfolio.put("주식", BigDecimal.valueOf(10));
                break;
            case "MODERATE":
                portfolio.put("예금/적금", BigDecimal.valueOf(20));
                portfolio.put("채권", BigDecimal.valueOf(30));
                portfolio.put("혼합형 펀드", BigDecimal.valueOf(30));
                portfolio.put("주식", BigDecimal.valueOf(20));
                break;
            case "AGGRESSIVE":
                portfolio.put("예금/적금", BigDecimal.valueOf(10));
                portfolio.put("채권", BigDecimal.valueOf(20));
                portfolio.put("주식형 펀드", BigDecimal.valueOf(40));
                portfolio.put("개별 주식", BigDecimal.valueOf(30));
                break;
        }

        return portfolio;
    }

    private BigDecimal calculateMonthlyInvestment(FinancialProfileVO profile) {
        BigDecimal surplus = profile.getMonthlyIncome().subtract(profile.getMonthlyExpense());
        // 월 여유 자금의 70%를 투자 권장
        return surplus.multiply(BigDecimal.valueOf(0.7)).setScale(0, RoundingMode.HALF_UP);
    }

    private Map<String, Object> getGoalBasedRecommendation(FinancialProfileVO profile) {
        Map<String, Object> recommendation = new HashMap<>();

        switch (profile.getInvestmentGoal()) {
            case "RETIREMENT":
                recommendation.put("strategy", "장기 투자");
                recommendation.put("recommendedProducts", Arrays.asList("연금저축", "IRP", "주식형 펀드"));
                recommendation.put("tips", Arrays.asList("조기 시작", "복리 효과 활용", "세제 혜택 상품 활용"));
                break;
            case "HOUSE":
                recommendation.put("strategy", "목표 기간 내 안정적 수익");
                recommendation.put("recommendedProducts", Arrays.asList("주택청약종합저축", "혼합형 펀드", "적금"));
                recommendation.put("tips", Arrays.asList("주택 관련 정책 확인", "대출 조건 미리 확인"));
                break;
            case "EDUCATION":
                recommendation.put("strategy", "교육비 준비");
                recommendation.put("recommendedProducts", Arrays.asList("교육비 적금", "안정적 펀드", "보험"));
                recommendation.put("tips", Arrays.asList("교육비 상승률 고려", "분할 투자"));
                break;
        }

        return recommendation;
    }

    private Map<String, Object> getMonthlyProgress(String userId) {
        // 월별 진행 상황 조회 로직
        return new HashMap<>();
    }

    private Map<String, Object> getGoalProgressByPeriod(String userId, String period) {
        // 기간별 목표 달성률 조회 로직
        return new HashMap<>();
    }

    private Map<String, Object> getAssetTrend(String userId, String period) {
        // 자산 증감 추이 조회 로직
        return new HashMap<>();
    }

    private Map<String, Object> getCategoryStatistics(String userId, String period) {
        // 카테고리별 통계 조회 로직
        return new HashMap<>();
    }

    private int calculateFinancialScore(FinancialProfileVO profile) {
        // 금융 점수 계산 로직
        return 75; // 예시 점수
    }

    private String getFinancialGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}