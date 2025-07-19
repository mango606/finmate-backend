package com.example.finmate.common.health;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Component
public class DatabaseHealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5초 타임아웃
        } catch (SQLException e) {
            return false;
        }
    }

    public Map<String, Object> getHealthDetails() {
        Map<String, Object> details = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            details.put("database", metaData.getDatabaseProductName());
            details.put("version", metaData.getDatabaseProductVersion());
            details.put("url", metaData.getURL());
            details.put("readOnly", connection.isReadOnly());
            details.put("autoCommit", connection.getAutoCommit());
            details.put("status", "UP");

        } catch (SQLException e) {
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
        }

        return details;
    }
}