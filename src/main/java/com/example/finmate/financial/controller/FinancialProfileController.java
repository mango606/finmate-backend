package com.example.finmate.financial.controller;

import com.example.finmate.common.dto.ApiResponse;
import com.example.finmate.common.util.IPUtils;
import com.example.finmate.common.util.SecurityUtils;
import com.example.finmate.financial.dto.FinancialProfileDTO;
import com.example.finmate.financial.dto.FinancialProfileInfoDTO;
import com.example.finmate.financial.dto.RiskAssessmentDTO;
import com.example.finmate.financial.service.FinancialProfileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
@Api(tags = "금융 프로필 관리 API")
public class FinancialProfileController {

    private final FinancialProfileService financialProfileService;

    @ApiOperation(value = "금융 프로필 조회", notes = "사용자의 금융 프로필 정보를 조회합니다.")
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<FinancialProfileInfoDTO>> getFinancialProfile(
            Authentication authentication) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 프로필 조회: {}", userId);

        try {
            FinancialProfileInfoDTO profile = financialProfileService.getFinancialProfile(userId);

            if (profile == null) {
                return ResponseEntity.ok(ApiResponse.success("등록된 금융 프로필이 없습니다.", null));
            }

            return ResponseEntity.ok(ApiResponse.success("금융 프로필 조회 성공", profile));

        } catch (Exception e) {
            log.error("금융 프로필 조회 실패: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("금융 프로필 조회에 실패했습니다.", "PROFILE_READ_ERROR"));
        }
    }

    @ApiOperation(value = "금융 프로필 등록/수정", notes = "사용자의 금융 프로필을 등록하거나 수정합니다.")
    @PostMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> saveFinancialProfile(
            @Valid @RequestBody FinancialProfileDTO profileDTO,
            Authentication authentication,
            HttpServletRequest request) {

        String userId = SecurityUtils.getCurrentUserId();
        String clientIP = IPUtils.getClientIP(request);
        log.info("금융 프로필 저장: {} from {}", userId, IPUtils.maskIP(clientIP));

        try {
            boolean success = financialProfileService.saveFinancialProfile(userId, profileDTO);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("금융 프로필이 저장되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("금융 프로필 저장에 실패했습니다.", "PROFILE_SAVE_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            log.warn("금융 프로필 저장 실패 - 유효성 검증: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
        } catch (Exception e) {
            log.error("금융 프로필 저장 실패: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("금융 프로필 저장에 실패했습니다.", "PROFILE_SAVE_ERROR"));
        }
    }

    @ApiOperation(value = "투자 성향 테스트", notes = "설문 답변을 바탕으로 투자 성향을 분석합니다.")
    @PostMapping("/risk-assessment")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<RiskAssessmentDTO>> assessRiskProfile(
            @RequestBody Map<String, Integer> answers,
            Authentication authentication) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("투자 성향 테스트: {}", userId);

        try {
            if (answers == null || answers.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("설문 답변을 입력해주세요.", "MISSING_ANSWERS"));
            }

            RiskAssessmentDTO result = financialProfileService.assessRiskProfile(answers);
            return ResponseEntity.ok(ApiResponse.success("투자 성향 분석 완료", result));

        } catch (Exception e) {
            log.error("투자 성향 테스트 실패: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("투자 성향 테스트에 실패했습니다.", "RISK_ASSESSMENT_ERROR"));
        }
    }

    @ApiOperation(value = "재정 건전성 분석", notes = "사용자의 재정 상태를 종합적으로 분석합니다.")
    @GetMapping("/financial-health")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeFinancialHealth(
            Authentication authentication) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("재정 건전성 분석: {}", userId);

        try {
            Map<String, Object> analysis = financialProfileService.analyzeFinancialHealth(userId);
            return ResponseEntity.ok(ApiResponse.success("재정 건전성 분석 완료", analysis));

        } catch (IllegalArgumentException e) {
            log.warn("재정 건전성 분석 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "PROFILE_NOT_FOUND"));
        } catch (Exception e) {
            log.error("재정 건전성 분석 실패: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("재정 건전성 분석에 실패했습니다.", "ANALYSIS_ERROR"));
        }
    }

    @ApiOperation(value = "금융 프로필 삭제", notes = "사용자의 금융 프로필을 삭제합니다.")
    @DeleteMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteFinancialProfile(
            Authentication authentication,
            HttpServletRequest request) {

        String userId = SecurityUtils.getCurrentUserId();
        String clientIP = IPUtils.getClientIP(request);
        log.info("금융 프로필 삭제: {} from {}", userId, IPUtils.maskIP(clientIP));

        try {
            boolean success = financialProfileService.deleteFinancialProfile(userId);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("금융 프로필이 삭제되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("금융 프로필 삭제에 실패했습니다.", "PROFILE_DELETE_FAILED"));
            }

        } catch (Exception e) {
            log.error("금융 프로필 삭제 실패: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("금융 프로필 삭제에 실패했습니다.", "PROFILE_DELETE_ERROR"));
        }
    }

    @ApiOperation(value = "투자 성향 테스트 질문 조회", notes = "투자 성향 테스트 질문 목록을 조회합니다.")
    @GetMapping("/risk-questions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRiskAssessmentQuestions() {
        log.info("투자 성향 테스트 질문 조회");

        try {
            Map<String, Object> questions = createRiskAssessmentQuestions();
            return ResponseEntity.ok(ApiResponse.success("투자 성향 테스트 질문 조회 성공", questions));

        } catch (Exception e) {
            log.error("투자 성향 테스트 질문 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("질문 조회에 실패했습니다.", "QUESTIONS_ERROR"));
        }
    }

    @ApiOperation(value = "금융 상품 추천", notes = "사용자의 투자 성향에 맞는 금융 상품을 추천합니다.")
    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductRecommendations(
            Authentication authentication) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 상품 추천: {}", userId);

        try {
            FinancialProfileInfoDTO profile = financialProfileService.getFinancialProfile(userId);

            if (profile == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("금융 프로필이 등록되지 않았습니다.", "PROFILE_NOT_FOUND"));
            }

            Map<String, Object> recommendations = generateProductRecommendations(profile);
            return ResponseEntity.ok(ApiResponse.success("금융 상품 추천 완료", recommendations));

        } catch (Exception e) {
            log.error("금융 상품 추천 실패: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("금융 상품 추천에 실패했습니다.", "RECOMMENDATION_ERROR"));
        }
    }

    // 투자 성향 테스트 질문 생성
    private Map<String, Object> createRiskAssessmentQuestions() {
        Map<String, Object> questionnaire = new HashMap<>();

        java.util.List<Map<String, Object>> questions = new java.util.ArrayList<>();

        // 질문 1
        Map<String, Object> q1 = new HashMap<>();
        q1.put("id", "q1");
        q1.put("question", "투자 경험이 어느 정도 되시나요?");
        q1.put("options", java.util.Arrays.asList(
                new HashMap<String, Object>() {{
                    put("text", "전혀 없음");
                    put("score", 1);
                }},
                new HashMap<String, Object>() {{
                    put("text", "1년 미만");
                    put("score", 2);
                }},
                new HashMap<String, Object>() {{
                    put("text", "1~3년");
                    put("score", 3);
                }},
                new HashMap<String, Object>() {{
                    put("text", "3~5년");
                    put("score", 4);
                }},
                new HashMap<String, Object>() {{
                    put("text", "5년 이상");
                    put("score", 5);
                }}
        ));
        questions.add(q1);

        // 질문 2
        Map<String, Object> q2 = new HashMap<>();
        q2.put("id", "q2");
        q2.put("question", "투자 목적은 무엇인가요?");
        q2.put("options", java.util.Arrays.asList(
                new HashMap<String, Object>() {{
                    put("text", "원금 보존");
                    put("score", 1);
                }},
                new HashMap<String, Object>() {{
                    put("text", "안정적 수익");
                    put("score", 2);
                }},
                new HashMap<String, Object>() {{
                    put("text", "인플레이션 대응");
                    put("score", 3);
                }},
                new HashMap<String, Object>() {{
                    put("text", "자산 증식");
                    put("score", 4);
                }},
                new HashMap<String, Object>() {{
                    put("text", "고수익 추구");
                    put("score", 5);
                }}
        ));
        questions.add(q2);

        // 질문 3
        Map<String, Object> q3 = new HashMap<>();
        q3.put("id", "q3");
        q3.put("question", "투자 기간은 얼마나 계획하고 계시나요?");
        q3.put("options", java.util.Arrays.asList(
                new HashMap<String, Object>() {{
                    put("text", "1년 미만");
                    put("score", 2);
                }},
                new HashMap<String, Object>() {{
                    put("text", "1~3년");
                    put("score", 3);
                }},
                new HashMap<String, Object>() {{
                    put("text", "3~5년");
                    put("score", 4);
                }},
                new HashMap<String, Object>() {{
                    put("text", "5~10년");
                    put("score", 5);
                }},
                new HashMap<String, Object>() {{
                    put("text", "10년 이상");
                    put("score", 6);
                }}
        ));
        questions.add(q3);

        // 질문 4
        Map<String, Object> q4 = new HashMap<>();
        q4.put("id", "q4");
        q4.put("question", "투자 원금이 30% 손실될 경우 어떻게 하시겠습니까?");
        q4.put("options", java.util.Arrays.asList(
                new HashMap<String, Object>() {{
                    put("text", "즉시 매도");
                    put("score", 1);
                }},
                new HashMap<String, Object>() {{
                    put("text", "일부 매도");
                    put("score", 2);
                }},
                new HashMap<String, Object>() {{
                    put("text", "관망");
                    put("score", 3);
                }},
                new HashMap<String, Object>() {{
                    put("text", "추가 매수");
                    put("score", 4);
                }},
                new HashMap<String, Object>() {{
                    put("text", "대량 추가 매수");
                    put("score", 5);
                }}
        ));
        questions.add(q4);

        // 질문 5
        Map<String, Object> q5 = new HashMap<>();
        q5.put("id", "q5");
        q5.put("question", "투자 가능한 여유 자금은 전체 자산의 몇 %인가요?");
        q5.put("options", java.util.Arrays.asList(
                new HashMap<String, Object>() {{
                    put("text", "10% 미만");
                    put("score", 1);
                }},
                new HashMap<String, Object>() {{
                    put("text", "10~30%");
                    put("score", 2);
                }},
                new HashMap<String, Object>() {{
                    put("text", "30~50%");
                    put("score", 3);
                }},
                new HashMap<String, Object>() {{
                    put("text", "50~70%");
                    put("score", 4);
                }},
                new HashMap<String, Object>() {{
                    put("text", "70% 이상");
                    put("score", 5);
                }}
        ));
        questions.add(q5);

        // 질문 6
        Map<String, Object> q6 = new HashMap<>();
        q6.put("id", "q6");
        q6.put("question", "현재 나이는?");
        q6.put("options", java.util.Arrays.asList(
                new HashMap<String, Object>() {{
                    put("text", "60세 이상");
                    put("score", 1);
                }},
                new HashMap<String, Object>() {{
                    put("text", "50~59세");
                    put("score", 2);
                }},
                new HashMap<String, Object>() {{
                    put("text", "40~49세");
                    put("score", 3);
                }},
                new HashMap<String, Object>() {{
                    put("text", "30~39세");
                    put("score", 4);
                }},
                new HashMap<String, Object>() {{
                    put("text", "20~29세");
                    put("score", 5);
                }}
        ));
        questions.add(q6);

        // 질문 7
        Map<String, Object> q7 = new HashMap<>();
        q7.put("id", "q7");
        q7.put("question", "수익률과 위험도 중 더 중요한 것은?");
        q7.put("options", java.util.Arrays.asList(
                new HashMap<String, Object>() {{
                    put("text", "안전성이 최우선");
                    put("score", 1);
                }},
                new HashMap<String, Object>() {{
                    put("text", "안전성 중심, 약간의 수익");
                    put("score", 2);
                }},
                new HashMap<String, Object>() {{
                    put("text", "안전성과 수익성 균형");
                    put("score", 3);
                }},
                new HashMap<String, Object>() {{
                    put("text", "수익성 중심, 약간의 위험");
                    put("score", 4);
                }},
                new HashMap<String, Object>() {{
                    put("text", "고수익 추구");
                    put("score", 5);
                }}
        ));
        questions.add(q7);

        // 질문 8
        Map<String, Object> q8 = new HashMap<>();
        q8.put("id", "q8");
        q8.put("question", "금융 지식 수준은?");
        q8.put("options", java.util.Arrays.asList(
                new HashMap<String, Object>() {{
                    put("text", "기초적");
                    put("score", 1);
                }},
                new HashMap<String, Object>() {{
                    put("text", "일반적");
                    put("score", 2);
                }},
                new HashMap<String, Object>() {{
                    put("text", "보통");
                    put("score", 3);
                }},
                new HashMap<String, Object>() {{
                    put("text", "상당한 수준");
                    put("score", 4);
                }},
                new HashMap<String, Object>() {{
                    put("text", "전문가 수준");
                    put("score", 5);
                }}
        ));
        questions.add(q8);

        questionnaire.put("questions", questions);
        questionnaire.put("totalQuestions", questions.size());
        questionnaire.put("instruction", "각 질문에 가장 적합한 답변을 선택해주세요. 모든 질문에 답변해야 정확한 투자 성향을 분석할 수 있습니다.");

        return questionnaire;
    }

    // 금융 상품 추천 생성
    private Map<String, Object> generateProductRecommendations(FinancialProfileInfoDTO profile) {
        Map<String, Object> recommendations = new HashMap<>();

        String riskProfile = profile.getRiskProfile();
        java.util.List<Map<String, Object>> products = new java.util.ArrayList<>();

        switch (riskProfile) {
            case "CONSERVATIVE":
                products.add(createProductInfo("적금", "안전한 목돈 마련", "2.5~3.5%", "낮음"));
                products.add(createProductInfo("국채", "국가 보증 안전 투자", "3.0~4.0%", "낮음"));
                products.add(createProductInfo("회사채(우량)", "안정적인 기업 채권", "3.5~4.5%", "낮음"));
                products.add(createProductInfo("MMF", "단기 자금 운용", "2.0~3.0%", "매우낮음"));
                break;

            case "MODERATE":
                products.add(createProductInfo("혼합형 펀드", "주식+채권 균형 투자", "4.0~8.0%", "보통"));
                products.add(createProductInfo("배당주", "안정적 배당 수익", "3.0~6.0%", "보통"));
                products.add(createProductInfo("리츠(REITs)", "부동산 간접 투자", "4.0~7.0%", "보통"));
                products.add(createProductInfo("인덱스 펀드", "시장 지수 추종", "5.0~10.0%", "보통"));
                break;

            case "AGGRESSIVE":
                products.add(createProductInfo("성장주", "고성장 기업 투자", "불확실", "높음"));
                products.add(createProductInfo("해외 주식", "글로벌 시장 투자", "불확실", "높음"));
                products.add(createProductInfo("섹터 ETF", "특정 산업 집중 투자", "불확실", "높음"));
                products.add(createProductInfo("암호화폐", "디지털 자산 투자", "불확실", "매우높음"));
                break;
        }

        recommendations.put("riskProfile", riskProfile);
        recommendations.put("products", products);
        recommendations.put("warning", "투자에는 원금 손실 위험이 있으며, 투자 전 충분한 검토가 필요합니다.");

        return recommendations;
    }

    // 상품 정보 생성
    private Map<String, Object> createProductInfo(String name, String description, String expectedReturn, String riskLevel) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("description", description);
        product.put("expectedReturn", expectedReturn);
        product.put("riskLevel", riskLevel);
        return product;
    }
}