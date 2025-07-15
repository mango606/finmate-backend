package com.example.finmate.common.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> checks = new HashMap<>();

        // 애플리케이션 상태
        checks.put("application", createHealthCheck("UP", "Application is running"));

        // 데이터베이스 상태
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                checks.put("database", createHealthCheck("UP", "Database connection is healthy"));
            } else {
                checks.put("database", createHealthCheck("DOWN", "Database connection is invalid"));
            }
        } catch (Exception e) {
            checks.put("database", createHealthCheck("DOWN", "Database connection failed: " + e.getMessage()));
        }

        // 전체 상태 결정
        boolean allUp = checks.values().stream()
                .allMatch(check -> "UP".equals(((Map<?, ?>) check).get("status")));

        health.put("status", allUp ? "UP" : "DOWN");
        health.put("checks", checks);
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }

    private Map<String, Object> createHealthCheck(String status, String details) {
        Map<String, Object> check = new HashMap<>();
        check.put("status", status);
        check.put("details", details);
        return check;
    }
}