package com.example.finmate.security.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JwtProcessor {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    // JWT 토큰 생성
    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT 토큰에서 사용자 ID 추출
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (JwtException e) {
            log.warn("JWT 토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    // JWT 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException e) {
            log.warn("JWT 보안 오류: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT 형식 오류: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT 만료: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 클레임이 비어있음: {}", e.getMessage());
        }
        return false;
    }

    // 토큰 만료 시간 확인
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getExpiration();
        } catch (JwtException e) {
            log.warn("JWT 토큰에서 만료 시간 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }

    // HTTP 헤더에서 토큰 추출
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith(prefix)) {
            return authHeader.substring(prefix.length()).trim();
        }
        return null;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String getHeader() {
        return header;
    }

    public String getPrefix() {
        return prefix;
    }

    private final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    // 토큰 블랙리스트 추가
    public void blacklistToken(String token) {
        tokenBlacklist.add(token);
        log.info("토큰이 블랙리스트에 추가됨: {}", token.substring(0, 20) + "...");
    }

    // 토큰 블랙리스트 확인
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }

    // 토큰 유효성 검증 (블랙리스트 포함)
    public boolean validateTokenWithBlacklist(String token) {
        if (isTokenBlacklisted(token)) {
            log.warn("블랙리스트에 등록된 토큰 접근 시도: {}", token.substring(0, 20) + "...");
            return false;
        }
        return validateToken(token);
    }

    // 토큰 갱신 시 기존 토큰 블랙리스트 추가
    public String refreshTokenSafely(String oldToken) {
        if (!validateToken(oldToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        String userId = getUserIdFromToken(oldToken);
        String newToken = generateToken(userId);

        // 기존 토큰 블랙리스트 추가
        blacklistToken(oldToken);

        return newToken;
    }

    // 정기적으로 만료된 토큰을 블랙리스트에서 제거
    @Scheduled(fixedRate = 3600000) // 1시간마다
    public void cleanupExpiredTokensFromBlacklist() {
        tokenBlacklist.removeIf(token -> {
            try {
                Date expiration = getExpirationDateFromToken(token);
                return expiration != null && expiration.before(new Date());
            } catch (Exception e) {
                return true; // 파싱 불가능한 토큰은 제거
            }
        });
    }
}