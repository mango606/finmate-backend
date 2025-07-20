package com.example.finmate.auth.service;

import com.example.finmate.auth.domain.RefreshTokenVO;
import com.example.finmate.auth.mapper.RefreshTokenMapper;
import com.example.finmate.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenMapper refreshTokenMapper;

    @Value("${jwt.refresh.expiration:1209600000}") // 14일 (밀리초)
    private long refreshTokenExpiration;

    // Refresh Token 생성
    @Transactional
    public String generateRefreshToken(String userId, String ipAddress, String userAgent) {
        try {
            // 기존 토큰 개수 확인 및 정리
            cleanupOldTokensForUser(userId);

            // 새 Refresh Token 생성
            String refreshToken = StringUtils.generateUUID();
            LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);

            RefreshTokenVO tokenVO = new RefreshTokenVO();
            tokenVO.setUserId(userId);
            tokenVO.setRefreshToken(refreshToken);
            tokenVO.setExpiryTime(expiryTime);
            tokenVO.setIsValid(true);
            tokenVO.setIpAddress(ipAddress);
            tokenVO.setUserAgent(userAgent);
            tokenVO.setCreatedDate(LocalDateTime.now());
            tokenVO.setLastUsedDate(LocalDateTime.now());

            int result = refreshTokenMapper.insertRefreshToken(tokenVO);

            if (result > 0) {
                log.info("Refresh Token 생성 완료: {}", userId);
                return refreshToken;
            } else {
                throw new RuntimeException("Refresh Token 생성에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("Refresh Token 생성 실패: {}", userId, e);
            throw new RuntimeException("Refresh Token 생성에 실패했습니다.", e);
        }
    }

    // Refresh Token 검증
    public boolean validateRefreshToken(String refreshToken) {
        try {
            RefreshTokenVO tokenVO = refreshTokenMapper.getRefreshToken(refreshToken);

            if (tokenVO == null) {
                log.warn("존재하지 않는 Refresh Token: {}", refreshToken.substring(0, 10) + "...");
                return false;
            }

            if (!tokenVO.getIsValid()) {
                log.warn("무효화된 Refresh Token: {}", refreshToken.substring(0, 10) + "...");
                return false;
            }

            if (tokenVO.getExpiryTime().isBefore(LocalDateTime.now())) {
                log.warn("만료된 Refresh Token: {}", refreshToken.substring(0, 10) + "...");
                return false;
            }

            // 사용 시간 업데이트
            refreshTokenMapper.updateLastUsedTime(refreshToken);

            return true;

        } catch (Exception e) {
            log.error("Refresh Token 검증 실패: {}", refreshToken.substring(0, 10) + "...", e);
            return false;
        }
    }

    // Refresh Token으로 사용자 ID 조회
    public String getUserIdFromRefreshToken(String refreshToken) {
        try {
            RefreshTokenVO tokenVO = refreshTokenMapper.getRefreshToken(refreshToken);
            return tokenVO != null ? tokenVO.getUserId() : null;
        } catch (Exception e) {
            log.error("Refresh Token에서 사용자 ID 조회 실패", e);
            return null;
        }
    }

    // Refresh Token 무효화
    @Transactional
    public void invalidateRefreshToken(String refreshToken) {
        try {
            int result = refreshTokenMapper.invalidateRefreshToken(refreshToken);
            if (result > 0) {
                log.info("Refresh Token 무효화 완료: {}", refreshToken.substring(0, 10) + "...");
            }
        } catch (Exception e) {
            log.error("Refresh Token 무효화 실패", e);
        }
    }

    // 사용자의 모든 Refresh Token 무효화 (로그아웃 시 사용)
    @Transactional
    public void invalidateAllRefreshTokens(String userId) {
        try {
            int result = refreshTokenMapper.invalidateAllRefreshTokensByUserId(userId);
            log.info("사용자의 모든 Refresh Token 무효화 완료: {} ({}개)", userId, result);
        } catch (Exception e) {
            log.error("사용자의 모든 Refresh Token 무효화 실패: {}", userId, e);
        }
    }

    // 사용자의 활성 Refresh Token 목록 조회
    public List<RefreshTokenVO> getActiveRefreshTokens(String userId) {
        try {
            return refreshTokenMapper.getRefreshTokensByUserId(userId);
        } catch (Exception e) {
            log.error("활성 Refresh Token 조회 실패: {}", userId, e);
            return new java.util.ArrayList<>();
        }
    }

    // 특정 Refresh Token 정보 조회
    public RefreshTokenVO getRefreshTokenInfo(String refreshToken) {
        try {
            return refreshTokenMapper.getRefreshToken(refreshToken);
        } catch (Exception e) {
            log.error("Refresh Token 정보 조회 실패", e);
            return null;
        }
    }

    // 사용자별 오래된 토큰 정리 (최대 5개 유지)
    @Transactional
    public void cleanupOldTokensForUser(String userId) {
        try {
            int maxTokens = 5;
            int activeCount = refreshTokenMapper.getActiveTokenCount(userId);

            if (activeCount >= maxTokens) {
                int deleteCount = refreshTokenMapper.deleteOldestTokensForUser(userId, maxTokens - 1);
                if (deleteCount > 0) {
                    log.info("사용자의 오래된 Refresh Token 정리: {} ({}개 삭제)", userId, deleteCount);
                }
            }
        } catch (Exception e) {
            log.error("사용자별 오래된 토큰 정리 실패: {}", userId, e);
        }
    }

    // 만료된 Refresh Token 정기 정리 (매일 새벽 2시)
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = refreshTokenMapper.deleteExpiredRefreshTokens();
            log.info("만료된 Refresh Token 정리 완료: {}개 삭제", deletedCount);
        } catch (Exception e) {
            log.error("만료된 Refresh Token 정리 실패", e);
        }
    }

    // Refresh Token 통계 조회
    public java.util.Map<String, Object> getRefreshTokenStatistics(String userId) {
        try {
            List<RefreshTokenVO> tokens = getActiveRefreshTokens(userId);

            java.util.Map<String, Object> statistics = new java.util.HashMap<>();
            statistics.put("activeTokenCount", tokens.size());
            statistics.put("tokens", tokens.stream().map(token -> {
                java.util.Map<String, Object> tokenInfo = new java.util.HashMap<>();
                tokenInfo.put("tokenId", token.getTokenId());
                tokenInfo.put("ipAddress", token.getIpAddress());
                tokenInfo.put("userAgent", token.getUserAgent());
                tokenInfo.put("createdDate", token.getCreatedDate());
                tokenInfo.put("lastUsedDate", token.getLastUsedDate());
                tokenInfo.put("expiryTime", token.getExpiryTime());
                return tokenInfo;
            }).collect(java.util.stream.Collectors.toList()));

            return statistics;

        } catch (Exception e) {
            log.error("Refresh Token 통계 조회 실패: {}", userId, e);
            return new java.util.HashMap<>();
        }
    }

    // 의심스러운 토큰 사용 감지
    public boolean isSuspiciousTokenUsage(String refreshToken, String currentIpAddress) {
        try {
            RefreshTokenVO tokenVO = refreshTokenMapper.getRefreshToken(refreshToken);

            if (tokenVO == null) {
                return true; // 존재하지 않는 토큰은 의심스러움
            }

            // IP 주소가 다른 경우 의심스러운 사용으로 판단
            if (!tokenVO.getIpAddress().equals(currentIpAddress)) {
                log.warn("의심스러운 Refresh Token 사용 감지: {} -> {}",
                        tokenVO.getIpAddress(), currentIpAddress);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("의심스러운 토큰 사용 감지 실패", e);
            return true; // 에러 시 안전하게 의심스러운 것으로 처리
        }
    }
}