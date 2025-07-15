package com.example.finmate.admin.controller;

import com.example.finmate.admin.service.AdminService;
import com.example.finmate.common.service.CacheService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Log4j2
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
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        log.info("시스템 상태 조회 요청");

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> systemInfo = adminService.getSystemInfo();

        response.put("success", true);
        response.put("data", systemInfo);

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "데이터베이스 상태 조회", notes = "데이터베이스 연결 및 성능 정보를 조회합니다.")
    @GetMapping("/database/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        log.info("데이터베이스 상태 조회 요청");

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> dbInfo = adminService.getDatabaseInfo();

        response.put("success", true);
        response.put("data", dbInfo);

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "회원 통계 조회", notes = "회원 가입 및 활동 통계를 조회합니다.")
    @GetMapping("/members/statistics")
    public ResponseEntity<Map<String, Object>> getMemberStatistics() {
        log.info("회원 통계 조회 요청");

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> memberStats = adminService.getMemberStatistics();

        response.put("success", true);
        response.put("data", memberStats);

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "캐시 상태 조회", notes = "시스템 캐시 상태를 조회합니다.")
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        log.info("캐시 상태 조회 요청");

        Map<String, Object> response = new HashMap<>();
        CacheService.CacheStats cacheStats = cacheService.getStats();

        Map<String, Object> cacheInfo = new HashMap<>();
        cacheInfo.put("size", cacheStats.getSize());
        cacheInfo.put("expiredCount", cacheStats.getExpiredCount());
        cacheInfo.put("timestamp", System.currentTimeMillis());

        response.put("success", true);
        response.put("data", cacheInfo);

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "캐시 초기화", notes = "시스템 캐시를 모두 초기화합니다.")
    @DeleteMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        log.info("캐시 초기화 요청");

        cacheService.clear();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "캐시가 초기화되었습니다.");

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "회원 계정 상태 변경", notes = "특정 회원의 계정 활성화/비활성화를 변경합니다.")
    @PutMapping("/members/{userId}/status")
    public ResponseEntity<Map<String, Object>> updateMemberStatus(
            @PathVariable String userId,
            @RequestParam boolean isActive) {

        log.info("회원 상태 변경 요청: {} -> {}", userId, isActive);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = adminService.updateMemberStatus(userId, isActive);

            if (success) {
                response.put("success", true);
                response.put("message", "회원 상태가 변경되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "회원 상태 변경에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("회원 상태 변경 실패", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "시스템 로그 조회", notes = "시스템 로그 파일의 최근 내용을 조회합니다.")
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getSystemLogs(
            @RequestParam(defaultValue = "100") int lines) {

        log.info("시스템 로그 조회 요청: {} 라인", lines);

        Map<String, Object> response = new HashMap<>();

        try {
            String logContent = adminService.getRecentLogs(lines);

            response.put("success", true);
            response.put("data", logContent);

        } catch (Exception e) {
            log.error("로그 조회 실패", e);
            response.put("success", false);
            response.put("message", "로그 조회에 실패했습니다.");
        }

        return ResponseEntity.ok(response);
    }
}