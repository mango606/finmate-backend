package com.example.finmate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${security.cors.allowed.origins}")
    private String allowedOrigins;

    @Value("${security.cors.allowed.methods}")
    private String allowedMethods;

    @Value("${security.cors.allowed.headers}")
    private String allowedHeaders;

    @Value("${security.cors.allow.credentials}")
    private boolean allowCredentials;

    @Value("${security.cors.max.age}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin 설정
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));

        // 허용할 헤더 설정
        if ("*".equals(allowedHeaders)) {
            configuration.setAllowedHeaders(Arrays.asList("*"));
        } else {
            configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }

        // 자격 증명 허용 설정
        configuration.setAllowCredentials(allowCredentials);

        // Preflight 요청 캐시 시간
        configuration.setMaxAge(maxAge);

        // 모든 경로에 대해 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}