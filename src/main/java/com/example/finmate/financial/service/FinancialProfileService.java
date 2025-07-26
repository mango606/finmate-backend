package com.example.finmate.financial.service;

import com.example.finmate.financial.domain.FinancialProfileVO;
import com.example.finmate.financial.dto.FinancialProfileDTO;
import com.example.finmate.financial.dto.FinancialProfileInfoDTO;
import com.example.finmate.financial.dto.RiskAssessmentDTO;
import com.example.finmate.financial.mapper.FinancialProfileMapper;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialProfileService {

    private final FinancialProfileMapper financialProfileMapper;
    private final MemberMapper memberMapper;

    // 금융 프로필 등록/수정
    @Transactional
    public boolean saveFinancialProfile(String userId, FinancialProfileDTO profileDTO) {
        log.info("금융 프로필 저장: {}", userId);

        try {
            // 사용자 존재 여부 확인
            MemberVO member = memberMapper.getMemberByUserId(userId);
            if (member == null) {
                throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
            }

            // 입력값 유효성 검증
            validateFinancialProfile(profileDTO);

            // VO 객체 생성
            FinancialProfileVO profile = createFinancialProfileVO(userId, profileDTO);

            int result;
            // 기존 프로필이 있는지 확인
            if (financialProfileMapper.checkFinancialProfileExists(userId) > 0) {
                // 기존 프로필 수정
                result = financialProfileMapper.updateFinancialProfile(profile);
                log.info("금융 프로필 수정 완료: {}", userId);
            } else {
                // 새 프로필 등록
                result = financialProfileMapper.insertFinancialProfile(profile);
                log.info("금융 프로필 등록 완료: {}", userId);
            }

            return result > 0;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("금융 프로필 저장 실패: {}", userId, e);
            throw new RuntimeException("금융 프로필 저장에 실패했습니다.", e);
        }
    }

    // 금융 프로필 조회
    public FinancialProfileInfoDTO getFinancialProfile(String userId) {
        log.info("금융 프로필 조회: {}", userId);

        try {
            FinancialProfileVO profile = financialProfileMapper.getFinancialProfileByUserId(userId);

            if (profile == null) {
                return null; // 프로필이 없는 경우
            }

            return createFinancialProfileInfoDTO(profile);

        } catch (Exception e) {
            log.error("금융 프로필 조회 실패: {}", userId, e);
            throw new RuntimeException("금융 프로필 조회에 실패했습니다.", e);
        }
    }

    // 투자 성향 테스트
    public RiskAssessmentDTO assessRiskProfile(Map<String, Integer> answers) {
        log.info("투자 성향 테스트 실행");

        try {
            // 답변 점수 합계 계산
            int totalScore = answers.values().stream().mapToInt(Integer::intValue).sum();

            // 위험 성향 결정
            String riskProfile = determineRiskProfile(totalScore);
            String description = getRiskProfileDescription(riskProfile);
            String recommendedProducts = getRecommendedProducts(riskProfile);

            RiskAssessmentDTO result = new RiskAssessmentDTO();
            result.setAnswers(answers);
            result.setTotalScore(totalScore);
            result.setRiskProfile(riskProfile);
            result.setDescription(description);
            result.setRecommendedProducts(recommendedProducts);

            log.info("투자 성향 테스트 완료: {} (점수: {})", riskProfile, totalScore);
            return result;

        } catch (Exception e) {
            log.error("투자 성향 테스트 실패", e);
            throw new RuntimeException("투자 성향 테스트에 실패했습니다.", e);
        }
    }

    // 재정 건전성 분석
    public Map<String, Object> analyzeFinancialHealth(String userId) {
        log.info("재정 건전성 분석: {}", userId);

        try {
            FinancialProfileVO profile = financialProfileMapper.getFinancialProfileByUserId(userId);
            if (profile == null) {
                throw new IllegalArgumentException("금융 프로필이 등록되지 않았습니다.");
            }

            Map<String, Object> analysis = new HashMap<>();

            // 순자산 계산
            BigDecimal netWorth = profile.getTotalAssets().subtract(profile.getTotalDebt());
            analysis.put("netWorth", netWorth);

            // 월 여유자금 계산
            BigDecimal monthlySurplus = profile.getMonthlyIncome().subtract(profile.getMonthlyExpense());
            analysis.put("monthlySurplus", monthlySurplus);

            // 부채비율 계산 (부채/자산 * 100)
            BigDecimal debtRatio = BigDecimal.ZERO;
            if (profile.getTotalAssets().compareTo(BigDecimal.ZERO) > 0) {
                debtRatio = profile.getTotalDebt()
                        .divide(profile.getTotalAssets(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            analysis.put("debtRatio", debtRatio);

            // 저축률 계산 (여유자금/소득 * 100)
            BigDecimal savingRate = BigDecimal.ZERO;
            if (profile.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {
                savingRate = monthlySurplus
                        .divide(profile.getMonthlyIncome(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            analysis.put("savingRate", savingRate);

            // 비상금 적정성 계산 (비상금/월지출)
            BigDecimal emergencyFundMonths = BigDecimal.ZERO;
            if (profile.getMonthlyExpense().compareTo(BigDecimal.ZERO) > 0) {
                emergencyFundMonths = profile.getEmergencyFund()
                        .divide(profile.getMonthlyExpense(), 2, RoundingMode.HALF_UP);
            }
            analysis.put("emergencyFundMonths", emergencyFundMonths);

            // 재정 건전성 등급 결정
            String healthGrade = determineFinancialHealthGrade(debtRatio, savingRate, emergencyFundMonths);
            analysis.put("healthGrade", healthGrade);

            // 권장 투자 금액 계산
            BigDecimal recommendedInvestment = calculateRecommendedInvestment(monthlySurplus, profile.getRiskProfile());
            analysis.put("recommendedInvestment", recommendedInvestment);

            // 개선 제안사항
            analysis.put("recommendations", generateRecommendations(debtRatio, savingRate, emergencyFundMonths));

            return analysis;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("재정 건전성 분석 실패: {}", userId, e);
            throw new RuntimeException("재정 건전성 분석에 실패했습니다.", e);
        }
    }

    // 금융 프로필 삭제
    @Transactional
    public boolean deleteFinancialProfile(String userId) {
        log.info("금융 프로필 삭제: {}", userId);

        try {
            int result = financialProfileMapper.deleteFinancialProfile(userId);
            boolean success = result > 0;

            log.info("금융 프로필 삭제 결과: {} - {}", userId, success);
            return success;

        } catch (Exception e) {
            log.error("금융 프로필 삭제 실패: {}", userId, e);
            throw new RuntimeException("금융 프로필 삭제에 실패했습니다.", e);
        }
    }

    // VO 객체 생성
    private FinancialProfileVO createFinancialProfileVO(String userId, FinancialProfileDTO dto) {
        FinancialProfileVO profile = new FinancialProfileVO();
        profile.setUserId(userId);
        profile.setIncomeRange(dto.getIncomeRange());
        profile.setJobType(dto.getJobType());
        profile.setMonthlyIncome(dto.getMonthlyIncome());
        profile.setMonthlyExpense(dto.getMonthlyExpense());
        profile.setTotalAssets(dto.getTotalAssets());
        profile.setTotalDebt(dto.getTotalDebt());
        profile.setRiskProfile(dto.getRiskProfile());
        profile.setInvestmentGoal(dto.getInvestmentGoal());
        profile.setInvestmentPeriod(dto.getInvestmentPeriod());
        profile.setEmergencyFund(dto.getEmergencyFund());
        profile.setCreditScore(dto.getCreditScore());
        return profile;
    }

    // DTO 객체 생성
    private FinancialProfileInfoDTO createFinancialProfileInfoDTO(FinancialProfileVO profile) {
        FinancialProfileInfoDTO dto = new FinancialProfileInfoDTO();
        dto.setProfileId(profile.getProfileId());
        dto.setUserId(profile.getUserId());
        dto.setIncomeRange(profile.getIncomeRange());
        dto.setJobType(profile.getJobType());
        dto.setMonthlyIncome(profile.getMonthlyIncome());
        dto.setMonthlyExpense(profile.getMonthlyExpense());
        dto.setTotalAssets(profile.getTotalAssets());
        dto.setTotalDebt(profile.getTotalDebt());
        dto.setRiskProfile(profile.getRiskProfile());
        dto.setInvestmentGoal(profile.getInvestmentGoal());
        dto.setInvestmentPeriod(profile.getInvestmentPeriod());
        dto.setEmergencyFund(profile.getEmergencyFund());
        dto.setCreditScore(profile.getCreditScore());
        dto.setRegDate(profile.getRegDate());
        dto.setUpdateDate(profile.getUpdateDate());

        // 계산된 필드들 설정
        BigDecimal netWorth = profile.getTotalAssets().subtract(profile.getTotalDebt());
        dto.setNetWorth(netWorth);

        BigDecimal monthlySurplus = profile.getMonthlyIncome().subtract(profile.getMonthlyExpense());
        dto.setMonthlySurplus(monthlySurplus);

        // 간단한 재정 건전성 등급
        BigDecimal debtRatio = BigDecimal.ZERO;
        if (profile.getTotalAssets().compareTo(BigDecimal.ZERO) > 0) {
            debtRatio = profile.getTotalDebt()
                    .divide(profile.getTotalAssets(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal savingRate = BigDecimal.ZERO;
        if (profile.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {
            savingRate = monthlySurplus
                    .divide(profile.getMonthlyIncome(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal emergencyFundMonths = BigDecimal.ZERO;
        if (profile.getMonthlyExpense().compareTo(BigDecimal.ZERO) > 0) {
            emergencyFundMonths = profile.getEmergencyFund()
                    .divide(profile.getMonthlyExpense(), 2, RoundingMode.HALF_UP);
        }

        String healthGrade = determineFinancialHealthGrade(debtRatio, savingRate, emergencyFundMonths);
        dto.setFinancialHealthGrade(healthGrade);

        BigDecimal recommendedInvestment = calculateRecommendedInvestment(monthlySurplus, profile.getRiskProfile());
        dto.setRecommendedInvestmentAmount(recommendedInvestment);

        return dto;
    }

    // 투자 성향 결정
    private String determineRiskProfile(int totalScore) {
        if (totalScore <= 20) {
            return "CONSERVATIVE"; // 보수적
        } else if (totalScore <= 35) {
            return "MODERATE"; // 중간
        } else {
            return "AGGRESSIVE"; // 공격적
        }
    }

    // 투자 성향 설명
    private String getRiskProfileDescription(String riskProfile) {
        switch (riskProfile) {
            case "CONSERVATIVE":
                return "안전성을 중시하며, 원금 손실을 최소화하려는 보수적 투자 성향입니다.";
            case "MODERATE":
                return "적정 수준의 위험을 감수하며, 안정성과 수익성의 균형을 추구하는 중간 투자 성향입니다.";
            case "AGGRESSIVE":
                return "높은 수익을 위해 높은 위험을 감수할 수 있는 공격적 투자 성향입니다.";
            default:
                return "투자 성향을 분석 중입니다.";
        }
    }

    // 추천 투자 상품
    private String getRecommendedProducts(String riskProfile) {
        switch (riskProfile) {
            case "CONSERVATIVE":
                return "예금, 적금, 국채, 회사채(우량), MMF";
            case "MODERATE":
                return "혼합형 펀드, 배당주, 리츠, 인덱스 펀드";
            case "AGGRESSIVE":
                return "성장주, 해외 주식, 섹터 ETF, 암호화폐";
            default:
                return "개별 상담을 통한 맞춤 상품 추천";
        }
    }

    // 재정 건전성 등급 결정
    private String determineFinancialHealthGrade(BigDecimal debtRatio, BigDecimal savingRate, BigDecimal emergencyFundMonths) {
        int score = 0;

        // 부채 비율 점수 (30%)
        if (debtRatio.compareTo(BigDecimal.valueOf(20)) <= 0) score += 30;
        else if (debtRatio.compareTo(BigDecimal.valueOf(40)) <= 0) score += 20;
        else if (debtRatio.compareTo(BigDecimal.valueOf(60)) <= 0) score += 10;

        // 저축률 점수 (40%)
        if (savingRate.compareTo(BigDecimal.valueOf(30)) >= 0) score += 40;
        else if (savingRate.compareTo(BigDecimal.valueOf(20)) >= 0) score += 30;
        else if (savingRate.compareTo(BigDecimal.valueOf(10)) >= 0) score += 20;
        else if (savingRate.compareTo(BigDecimal.valueOf(0)) >= 0) score += 10;

        // 비상금 적정성 점수 (30%)
        if (emergencyFundMonths.compareTo(BigDecimal.valueOf(6)) >= 0) score += 30;
        else if (emergencyFundMonths.compareTo(BigDecimal.valueOf(3)) >= 0) score += 20;
        else if (emergencyFundMonths.compareTo(BigDecimal.valueOf(1)) >= 0) score += 10;

        if (score >= 80) return "A";
        else if (score >= 60) return "B";
        else if (score >= 40) return "C";
        else if (score >= 20) return "D";
        else return "F";
    }

    // 권장 투자 금액 계산
    private BigDecimal calculateRecommendedInvestment(BigDecimal monthlySurplus, String riskProfile) {
        if (monthlySurplus.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal recommendationRate;
        switch (riskProfile) {
            case "CONSERVATIVE":
                recommendationRate = BigDecimal.valueOf(0.3); // 30%
                break;
            case "MODERATE":
                recommendationRate = BigDecimal.valueOf(0.5); // 50%
                break;
            case "AGGRESSIVE":
                recommendationRate = BigDecimal.valueOf(0.7); // 70%
                break;
            default:
                recommendationRate = BigDecimal.valueOf(0.3);
        }

        return monthlySurplus.multiply(recommendationRate).setScale(0, RoundingMode.DOWN);
    }

    // 개선 제안사항 생성
    private java.util.List<String> generateRecommendations(BigDecimal debtRatio, BigDecimal savingRate, BigDecimal emergencyFundMonths) {
        java.util.List<String> recommendations = new java.util.ArrayList<>();

        if (debtRatio.compareTo(BigDecimal.valueOf(40)) > 0) {
            recommendations.add("부채 비율이 높습니다. 부채 줄이기를 우선적으로 고려해보세요.");
        }

        if (savingRate.compareTo(BigDecimal.valueOf(10)) < 0) {
            recommendations.add("저축률이 낮습니다. 월 지출을 재검토하여 저축을 늘려보세요.");
        }

        if (emergencyFundMonths.compareTo(BigDecimal.valueOf(3)) < 0) {
            recommendations.add("비상금이 부족합니다. 월 지출의 3-6개월분을 준비하는 것이 좋습니다.");
        }

        if (savingRate.compareTo(BigDecimal.valueOf(20)) >= 0 && debtRatio.compareTo(BigDecimal.valueOf(30)) <= 0) {
            recommendations.add("재정 상태가 양호합니다. 투자를 통한 자산 증식을 고려해보세요.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("현재 재정 상태를 잘 유지하고 계십니다. 꾸준한 관리를 계속해보세요.");
        }

        return recommendations;
    }

    // 금융 프로필 유효성 검증
    private void validateFinancialProfile(FinancialProfileDTO profile) {
        // 월 지출이 월 소득보다 많은 경우 경고
        if (profile.getMonthlyExpense().compareTo(profile.getMonthlyIncome()) > 0) {
            log.warn("월 지출이 월 소득보다 많습니다: 소득={}, 지출={}",
                    profile.getMonthlyIncome(), profile.getMonthlyExpense());
        }

        // 부채가 자산보다 많은 경우 경고
        if (profile.getTotalDebt().compareTo(profile.getTotalAssets()) > 0) {
            log.warn("총 부채가 총 자산보다 많습니다: 자산={}, 부채={}",
                    profile.getTotalAssets(), profile.getTotalDebt());
        }

        // 투자 기간 검증
        if (profile.getInvestmentPeriod() != null && profile.getInvestmentPeriod() > 600) {
            throw new IllegalArgumentException("투자 기간은 600개월(50년)을 초과할 수 없습니다.");
        }
    }
}