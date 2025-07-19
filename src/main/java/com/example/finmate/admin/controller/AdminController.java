package com.example.finmate.admin.controller;

import com.example.finmate.admin.service.AdminService;
import com.example.finmate.common.dto.ApiResponse;
import com.example.finmate.common.service.CacheService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Api(tags = "관리자 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final CacheService cacheService;

    @ApiOperation(value = "시스템 상태 조회", notes = "전체 시스템의 상태 정보를 조회합니다.")
    @GetMapping("/system/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatus() {
        log.info("시스템 상태 조회 요청");

        Map<String, Object> systemInfo = adminService.getSystemInfo();
        return ResponseEntity.ok(ApiResponse.success(systemInfo));
    }

    @ApiOperation(value = "데이터베이스 상태 조회", notes = "데이터베이스 연결 및 성능 정보를 조회합니다.")
    @GetMapping("/database/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseStatus() {
        log.info("데이터베이스 상태 조회 요청");

        Map<String, Object> dbInfo = adminService.getDatabaseInfo();
        return ResponseEntity.ok(ApiResponse.success(dbInfo));
    }

    @ApiOperation(value = "회원 통계 조회", notes = "회원 가입 및 활동 통계를 조회합니다.")
    @GetMapping("/members/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberStatistics() {
        log.info("회원 통계 조회 요청");

        Map<String, Object> memberStats = adminService.getMemberStatistics();
        return ResponseEntity.ok(ApiResponse.success(memberStats));
    }

    @ApiOperation(value = "캐시 상태 조회", notes = "시스템 캐시 상태를 조회합니다.")
    @GetMapping("/cache/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStatus() {
        log.info("캐시 상태 조회 요청");

        CacheService.CacheStats cacheStats = cacheService.getStats();

        Map<String, Object> cacheInfo = new HashMap<>();
        cacheInfo.put("size", cacheStats.getSize());
        cacheInfo.put("expiredCount", cacheStats.getExpiredCount());
        cacheInfo.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(ApiResponse.success(cacheInfo));
    }

    @ApiOperation(value = "캐시 초기화", notes = "시스템 캐시를 모두 초기화합니다.")
    @DeleteMapping("/cache/clear")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        log.info("캐시 초기화 요청");

        cacheService.clear();
        return ResponseEntity.ok(ApiResponse.success("캐시가 초기화되었습니다.", null));
    }

    @ApiOperation(value = "회원 계정 상태 변경", notes = "특정 회원의 계정 활성화/비활성화를 변경합니다.")
    @PutMapping("/members/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateMemberStatus(
            @PathVariable String userId,
            @RequestParam boolean isActive) {

        log.info("회원 상태 변경 요청: {} -> {}", userId, isActive);

        try {
            boolean success = adminService.updateMemberStatus(userId, isActive);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("회원 상태가 변경되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("회원 상태 변경에 실패했습니다.", "UPDATE_FAILED"));
            }

        } catch (Exception e) {
            log.error("회원 상태 변경 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "UPDATE_ERROR"));
        }
    }

    @ApiOperation(value = "시스템 로그 조회", notes = "시스템 로그 파일의 최근 내용을 조회합니다.")
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<String>> getSystemLogs(
            @RequestParam(defaultValue = "100") int lines) {

        log.info("시스템 로그 조회 요청: {} 라인", lines);

        try {
            String logContent = adminService.getRecentLogs(lines);
            return ResponseEntity.ok(ApiResponse.success(logContent));

        } catch (Exception e) {
            log.error("로그 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("로그 조회에 실패했습니다.", "LOG_READ_ERROR"));
        }
    }
}