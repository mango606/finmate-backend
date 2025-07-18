package com.example.finmate.financial.controller;

import com.example.finmate.common.dto.ApiResponse;
import com.example.finmate.common.util.SecurityUtils;
import com.example.finmate.financial.dto.FinancialGoalDTO;
import com.example.finmate.financial.dto.FinancialProfileDTO;
import com.example.finmate.financial.service.FinancialService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
@Api(tags = "금융 서비스 API")
@PreAuthorize("hasRole('USER')")
public class FinancialController {

    private final FinancialService financialService;

    @ApiOperation(value = "금융 프로필 조회", notes = "사용자의 금융 프로필 정보를 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinancialProfile() {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 프로필 조회: {}", userId);

        try {
            Map<String, Object> profile = financialService.getFinancialProfile(userId);
            return ResponseEntity.ok(ApiResponse.success(profile));

        } catch (Exception e) {
            log.error("금융 프로필 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("금융 프로필 조회에 실패했습니다.", "PROFILE_READ_ERROR"));
        }
    }

    @ApiOperation(value = "금융 프로필 등록/수정", notes = "사용자의 금융 프로필을 등록하거나 수정합니다.")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> saveFinancialProfile(
            @Valid @RequestBody FinancialProfileDTO profileDTO) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 프로필 저장: {}", userId);

        try {
            boolean success = financialService.saveFinancialProfile(userId, profileDTO);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("금융 프로필이 저장되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("금융 프로필 저장에 실패했습니다.", "PROFILE_SAVE_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            log.warn("금융 프로필 저장 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @ApiOperation(value = "금융 목표 목록 조회", notes = "사용자의 금융 목표 목록을 조회합니다.")
    @GetMapping("/goals")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinancialGoals(
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 목표 조회: {} (status: {})", userId, status);

        try {
            Map<String, Object> goals = financialService.getFinancialGoals(userId, status, page, size);
            return ResponseEntity.ok(ApiResponse.success(goals));

        } catch (Exception e) {
            log.error("금융 목표 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("금융 목표 조회에 실패했습니다.", "GOALS_READ_ERROR"));
        }
    }

    @ApiOperation(value = "금융 목표 등록", notes = "새로운 금융 목표를 등록합니다.")
    @PostMapping("/goals")
    public ResponseEntity<ApiResponse<Void>> createFinancialGoal(
            @Valid @RequestBody FinancialGoalDTO goalDTO) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 목표 등록: {}", userId);

        try {
            boolean success = financialService.createFinancialGoal(userId, goalDTO);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("금융 목표가 등록되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("금융 목표 등록에 실패했습니다.", "GOAL_CREATE_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            log.warn("금융 목표 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @ApiOperation(value = "금융 목표 수정", notes = "기존 금융 목표를 수정합니다.")
    @PutMapping("/goals/{goalId}")
    public ResponseEntity<ApiResponse<Void>> updateFinancialGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody FinancialGoalDTO goalDTO) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 목표 수정: {} (goalId: {})", userId, goalId);

        try {
            boolean success = financialService.updateFinancialGoal(userId, goalId, goalDTO);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("금융 목표가 수정되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("금융 목표 수정에 실패했습니다.", "GOAL_UPDATE_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            log.warn("금융 목표 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @ApiOperation(value = "금융 목표 삭제", notes = "금융 목표를 삭제합니다.")
    @DeleteMapping("/goals/{goalId}")
    public ResponseEntity<ApiResponse<Void>> deleteFinancialGoal(@PathVariable Long goalId) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 목표 삭제: {} (goalId: {})", userId, goalId);

        try {
            boolean success = financialService.deleteFinancialGoal(userId, goalId);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("금융 목표가 삭제되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("금융 목표 삭제에 실패했습니다.", "GOAL_DELETE_FAILED"));
            }

        } catch (IllegalArgumentException e) {
            log.warn("금융 목표 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @ApiOperation(value = "금융 대시보드", notes = "사용자의 금융 현황 대시보드를 조회합니다.")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinancialDashboard() {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 대시보드 조회: {}", userId);

        try {
            Map<String, Object> dashboard = financialService.getFinancialDashboard(userId);
            return ResponseEntity.ok(ApiResponse.success(dashboard));

        } catch (Exception e) {
            log.error("금융 대시보드 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("금융 대시보드 조회에 실패했습니다.", "DASHBOARD_ERROR"));
        }
    }

    @ApiOperation(value = "투자 성향 분석", notes = "사용자의 투자 성향을 분석합니다.")
    @GetMapping("/risk-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRiskAnalysis() {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("투자 성향 분석: {}", userId);

        try {
            Map<String, Object> analysis = financialService.analyzeRiskProfile(userId);
            return ResponseEntity.ok(ApiResponse.success(analysis));

        } catch (Exception e) {
            log.error("투자 성향 분석 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("투자 성향 분석에 실패했습니다.", "RISK_ANALYSIS_ERROR"));
        }
    }

    @ApiOperation(value = "포트폴리오 추천", notes = "사용자의 투자 성향에 맞는 포트폴리오를 추천합니다.")
    @GetMapping("/portfolio-recommendation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPortfolioRecommendation() {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("포트폴리오 추천: {}", userId);

        try {
            Map<String, Object> recommendation = financialService.getPortfolioRecommendation(userId);
            return ResponseEntity.ok(ApiResponse.success(recommendation));

        } catch (Exception e) {
            log.error("포트폴리오 추천 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("포트폴리오 추천에 실패했습니다.", "PORTFOLIO_ERROR"));
        }
    }

    @ApiOperation(value = "금융 통계", notes = "사용자의 금융 통계를 조회합니다.")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinancialStatistics(
            @RequestParam(defaultValue = "MONTHLY") String period) {

        String userId = SecurityUtils.getCurrentUserId();
        log.info("금융 통계 조회: {} (period: {})", userId, period);

        try {
            Map<String, Object> statistics = financialService.getFinancialStatistics(userId, period);
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            log.error("금융 통계 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("금융 통계 조회에 실패했습니다.", "STATISTICS_ERROR"));
        }
    }
}