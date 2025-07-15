package com.example.finmate.security.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class SecurityProperties {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.header}")
    private String jwtHeader;

    @Value("${jwt.prefix}")
    private String jwtPrefix;

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

    @Value("${session.timeout}")
    private int sessionTimeout;
}