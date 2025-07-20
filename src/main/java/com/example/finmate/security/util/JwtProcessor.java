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

    @Value("${jwt.expiration:1800000}") // 30분 기본값
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration:1209600000}") // 14일 기본값
    private long refreshTokenExpiration;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    // Access Token 생성
    public String generateAccessToken(String userId) {
        return generateToken(userId, accessTokenExpiration, "access");
    }

    // Refresh Token 생성 (JWT 형태)
    public String generateRefreshToken(String userId) {
        return generateToken(userId, refreshTokenExpiration, "refresh");
    }

    // 공통 토큰 생성 메서드
    private String generateToken(String userId, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", tokenType);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String generateToken(String userId) {
        return generateAccessToken(userId);
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

    // JWT 토큰에서 토큰 타입 추출
    public String getTokenTypeFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return (String) claims.get("tokenType");
        } catch (JwtException e) {
            log.warn("JWT 토큰에서 타입 추출 실패: {}", e.getMessage());
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

    // Access Token 검증 (토큰 타입 확인 포함)
    public boolean validateAccessToken(String token) {
        if (!validateToken(token)) {
            return false;
        }

        String tokenType = getTokenTypeFromToken(token);
        return "access".equals(tokenType);
    }

    // Refresh Token 검증 (토큰 타입 확인 포함)
    public boolean validateRefreshToken(String token) {
        if (!validateToken(token)) {
            return false;
        }

        String tokenType = getTokenTypeFromToken(token);
        return "refresh".equals(tokenType);
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

    // 토큰 발급 시간 확인
    public Date getIssuedAtFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getIssuedAt();
        } catch (JwtException e) {
            log.warn("JWT 토큰에서 발급 시간 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }

    // 토큰 만료까지 남은 시간 (밀리초)
    public long getTimeUntilExpiration(String token) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return 0;
        }
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
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

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    // 토큰 블랙리스트 관리
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

    // Access Token 유효성 검증 (블랙리스트 포함)
    public boolean validateAccessTokenWithBlacklist(String token) {
        if (isTokenBlacklisted(token)) {
            log.warn("블랙리스트에 등록된 Access Token 접근 시도: {}", token.substring(0, 20) + "...");
            return false;
        }
        return validateAccessToken(token);
    }

    // 토큰 페어 생성 (Access + Refresh)
    public Map<String, Object> generateTokenPair(String userId) {
        String accessToken = generateAccessToken(userId);
        String refreshToken = generateRefreshToken(userId);

        Map<String, Object> tokenPair = new HashMap<>();
        tokenPair.put("accessToken", accessToken);
        tokenPair.put("refreshToken", refreshToken);
        tokenPair.put("tokenType", "Bearer");
        tokenPair.put("expiresIn", accessTokenExpiration / 1000); // 초 단위
        tokenPair.put("refreshExpiresIn", refreshTokenExpiration / 1000); // 초 단위

        return tokenPair;
    }

    // 토큰 갱신 시 기존 토큰 블랙리스트 추가
    public String refreshAccessTokenSafely(String oldAccessToken) {
        if (!validateAccessToken(oldAccessToken)) {
            throw new IllegalArgumentException("유효하지 않은 Access Token입니다.");
        }

        String userId = getUserIdFromToken(oldAccessToken);
        String newAccessToken = generateAccessToken(userId);

        // 기존 토큰 블랙리스트 추가
        blacklistToken(oldAccessToken);

        return newAccessToken;
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

    // 토큰 정보 추출 (디버깅용)
    public Map<String, Object> getTokenInfo(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("subject", claims.getSubject());
            tokenInfo.put("issuedAt", claims.getIssuedAt());
            tokenInfo.put("expiration", claims.getExpiration());
            tokenInfo.put("tokenType", claims.get("tokenType"));
            tokenInfo.put("userId", claims.get("userId"));

            return tokenInfo;
        } catch (JwtException e) {
            log.warn("토큰 정보 추출 실패: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}