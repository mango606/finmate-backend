package com.example.finmate.admin.service;

import com.example.finmate.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final DataSource dataSource;
    private final MemberMapper memberMapper;

    // 시스템 정보 조회
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();

        // JVM 정보
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        Map<String, Object> memoryInfo = new HashMap<>();
        memoryInfo.put("total", formatBytes(totalMemory));
        memoryInfo.put("used", formatBytes(usedMemory));
        memoryInfo.put("free", formatBytes(freeMemory));
        memoryInfo.put("max", formatBytes(maxMemory));
        memoryInfo.put("usagePercent", Math.round((double) usedMemory / totalMemory * 100));

        // 시스템 속성
        Map<String, Object> systemProps = new HashMap<>();
        systemProps.put("javaVersion", System.getProperty("java.version"));
        systemProps.put("javaVendor", System.getProperty("java.vendor"));
        systemProps.put("osName", System.getProperty("os.name"));
        systemProps.put("osVersion", System.getProperty("os.version"));
        systemProps.put("osArch", System.getProperty("os.arch"));

        // 애플리케이션 정보
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("startTime", getApplicationStartTime());
        appInfo.put("uptime", getApplicationUptime());
        appInfo.put("currentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        systemInfo.put("memory", memoryInfo);
        systemInfo.put("system", systemProps);
        systemInfo.put("application", appInfo);

        return systemInfo;
    }

    // 데이터베이스 정보 조회
    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> dbInfo = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // 데이터베이스 기본 정보
            Map<String, Object> basicInfo = new HashMap<>();
            basicInfo.put("productName", metaData.getDatabaseProductName());
            basicInfo.put("productVersion", metaData.getDatabaseProductVersion());
            basicInfo.put("driverName", metaData.getDriverName());
            basicInfo.put("driverVersion", metaData.getDriverVersion());
            basicInfo.put("url", metaData.getURL());

            // 연결 정보
            Map<String, Object> connectionInfo = new HashMap<>();
            connectionInfo.put("maxConnections", metaData.getMaxConnections());
            connectionInfo.put("autoCommit", connection.getAutoCommit());
            connectionInfo.put("readOnly", connection.isReadOnly());

            // 테이블 정보
            Map<String, Object> tableInfo = new HashMap<>();
            try (ResultSet tables = metaData.getTables(null, null, "tbl_%", new String[]{"TABLE"})) {
                int tableCount = 0;
                while (tables.next()) {
                    tableCount++;
                }
                tableInfo.put("count", tableCount);
            }

            dbInfo.put("basic", basicInfo);
            dbInfo.put("connection", connectionInfo);
            dbInfo.put("tables", tableInfo);

        } catch (Exception e) {
            log.error("데이터베이스 정보 조회 실패", e);
            dbInfo.put("error", e.getMessage());
        }

        return dbInfo;
    }

    // 회원 통계 조회
    public Map<String, Object> getMemberStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // 전체 회원 수
            ResultSet rs1 = statement.executeQuery("SELECT COUNT(*) as total FROM tbl_member");
            if (rs1.next()) {
                stats.put("totalMembers", rs1.getInt("total"));
            }

            // 활성 회원 수
            ResultSet rs2 = statement.executeQuery("SELECT COUNT(*) as active FROM tbl_member WHERE is_active = true");
            if (rs2.next()) {
                stats.put("activeMembers", rs2.getInt("active"));
            }

            // 오늘 가입한 회원 수
            ResultSet rs3 = statement.executeQuery(
                    "SELECT COUNT(*) as today FROM tbl_member WHERE DATE(reg_date) = CURDATE()");
            if (rs3.next()) {
                stats.put("todayJoined", rs3.getInt("today"));
            }

            // 이번 달 가입한 회원 수
            ResultSet rs4 = statement.executeQuery(
                    "SELECT COUNT(*) as thisMonth FROM tbl_member WHERE YEAR(reg_date) = YEAR(NOW()) AND MONTH(reg_date) = MONTH(NOW())");
            if (rs4.next()) {
                stats.put("thisMonthJoined", rs4.getInt("thisMonth"));
            }

            // 성별 통계
            ResultSet rs5 = statement.executeQuery(
                    "SELECT gender, COUNT(*) as count FROM tbl_member WHERE gender IS NOT NULL GROUP BY gender");
            Map<String, Integer> genderStats = new HashMap<>();
            while (rs5.next()) {
                genderStats.put(rs5.getString("gender"), rs5.getInt("count"));
            }
            stats.put("genderStats", genderStats);

        } catch (Exception e) {
            log.error("회원 통계 조회 실패", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    // 회원 상태 변경
    public boolean updateMemberStatus(String userId, boolean isActive) {
        try {
            int result = memberMapper.updateMemberActive(userId, isActive);
            return result > 0;
        } catch (Exception e) {
            log.error("회원 상태 변경 실패: {}", userId, e);
            throw new RuntimeException("회원 상태 변경에 실패했습니다.", e);
        }
    }

    // 최근 로그 조회
    public String getRecentLogs(int lines) throws IOException {
        File logFile = new File("logs/finmate.log");
        if (!logFile.exists()) {
            return "로그 파일을 찾을 수 없습니다.";
        }

        LinkedList<String> lastLines = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastLines.add(line);
                if (lastLines.size() > lines) {
                    lastLines.removeFirst();
                }
            }
        }

        return String.join("\n", lastLines);
    }

    // 헬퍼 메서드들
    private String formatBytes(long bytes) {
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;

        if (gb >= 1) {
            return String.format("%.2f GB", gb);
        } else if (mb >= 1) {
            return String.format("%.2f MB", mb);
        } else {
            return String.format("%.2f KB", kb);
        }
    }

    private String getApplicationStartTime() {
        // TODO: 애플리케이션 시작 시간을 저장해야 함
        return LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String getApplicationUptime() {
        // TODO: 현재 시간 - 시작 시간으로 계산
        return "2시간 15분";
    }
}