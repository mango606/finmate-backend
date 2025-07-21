package com.example.finmate.auth.controller;

import com.example.finmate.auth.dto.RefreshTokenDTO;
import com.example.finmate.auth.service.AuthService;
import com.example.finmate.auth.service.RefreshTokenService;
import com.example.finmate.common.dto.ApiResponse;
import com.example.finmate.common.util.IPUtils;
import com.example.finmate.member.dto.MemberLoginDTO;
import com.example.finmate.member.domain.MemberVO;
import com.example.finmate.member.mapper.MemberMapper;
import com.example.finmate.security.util.JwtProcessor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Api(tags = "로그인/인증 API")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProcessor jwtProcessor;
    private final MemberMapper memberMapper;

    @ApiOperation(value = "사용자 로그인", notes = "사용자 ID와 비밀번호로 로그인하고 JWT 토큰 페어(Access + Refresh)를 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody MemberLoginDTO loginDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("로그인 시도: {} from {}", loginDTO.getUserId(), IPUtils.maskIP(clientIP));

        try {
            if (authService.isAccountLocked(loginDTO.getUserId())) {
                authService.recordLoginFailure(loginDTO.getUserId(), clientIP, userAgent, "ACCOUNT_LOCKED");
                return ResponseEntity.status(423) // 423 Locked
                        .body(ApiResponse.error("계정이 잠금 상태입니다. 30분 후 다시 시도해주세요.", "ACCOUNT_LOCKED"));
            }

            // Spring Security를 통한 인증
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginDTO.getUserId(), loginDTO.getUserPassword());

            Authentication authentication = authenticationManager.authenticate(authToken);
            User user = (User) authentication.getPrincipal();
            String userId = user.getUsername();

            // 토큰 페어 생성 (Access Token + Refresh Token)
            Map<String, Object> tokenPair = jwtProcessor.generateTokenPair(userId);
            String accessToken = (String) tokenPair.get("accessToken");
            String refreshTokenJwt = (String) tokenPair.get("refreshToken");

            // Refresh Token을 데이터베이스에 저장
            String refreshToken = refreshTokenService.generateRefreshToken(userId, clientIP, userAgent);

            // 사용자 정보 조회
            MemberVO member = memberMapper.getMemberByUserId(userId);

            // 응답 데이터 구성
            Map<String, Object> authResult = new HashMap<>();
            authResult.put("accessToken", accessToken);
            authResult.put("refreshToken", refreshToken);
            authResult.put("tokenType", "Bearer");
            authResult.put("expiresIn", tokenPair.get("expiresIn"));
            authResult.put("refreshExpiresIn", tokenPair.get("refreshExpiresIn"));

            // 사용자 정보
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", member.getUserId());
            userInfo.put("userName", member.getUserName());
            userInfo.put("userEmail", member.getUserEmail());
            userInfo.put("userPhone", member.getUserPhone());
            userInfo.put("birthDate", member.getBirthDate());
            userInfo.put("gender", member.getGender());
            userInfo.put("regDate", member.getRegDate());
            userInfo.put("authorities", user.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .collect(java.util.stream.Collectors.toList()));

            authResult.put("user", userInfo);

            // 로그인 성공 기록
            authService.recordLoginSuccess(userId, clientIP, userAgent);

            log.info("로그인 성공: {} from {}", userId, IPUtils.maskIP(clientIP));
            return ResponseEntity.ok(ApiResponse.success("로그인 성공", authResult));

        } catch (Exception e) {
            // 로그인 실패 기록
            authService.recordLoginFailure(loginDTO.getUserId(), clientIP, userAgent, e.getMessage());

            log.warn("로그인 실패: {} from {} - {}", loginDTO.getUserId(), IPUtils.maskIP(clientIP), e.getMessage());

            return ResponseEntity.status(401)
                    .body(ApiResponse.error("로그인에 실패했습니다. 사용자 ID와 비밀번호를 확인해주세요.", "AUTHENTICATION_FAILED"));
        }
    }

    @ApiOperation(value = "토큰 갱신", notes = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @Valid @RequestBody RefreshTokenDTO refreshTokenDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        String refreshToken = refreshTokenDTO.getRefreshToken();

        log.info("토큰 갱신 요청: {} from {}", refreshToken.substring(0, 10) + "...", IPUtils.maskIP(clientIP));

        try {
            // Refresh Token 검증
            if (!refreshTokenService.validateRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("유효하지 않은 Refresh Token입니다.", "INVALID_REFRESH_TOKEN"));
            }

            // 의심스러운 토큰 사용 감지
            if (refreshTokenService.isSuspiciousTokenUsage(refreshToken, clientIP)) {
                log.warn("의심스러운 Refresh Token 사용 감지: {}", IPUtils.maskIP(clientIP));

                // 의심스러운 경우 해당 토큰 무효화
                refreshTokenService.invalidateRefreshToken(refreshToken);

                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("보안상의 이유로 토큰이 무효화되었습니다. 다시 로그인해주세요.", "SUSPICIOUS_TOKEN_USAGE"));
            }

            // 사용자 ID 추출
            String userId = refreshTokenService.getUserIdFromRefreshToken(refreshToken);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("토큰에서 사용자 정보를 찾을 수 없습니다.", "TOKEN_USER_NOT_FOUND"));
            }

            // 새 Access Token 생성
            String newAccessToken = jwtProcessor.generateAccessToken(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("tokenType", "Bearer");
            result.put("expiresIn", jwtProcessor.getAccessTokenExpiration() / 1000);

            log.info("토큰 갱신 성공: {}", userId);
            return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", result));

        } catch (Exception e) {
            log.error("토큰 갱신 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("토큰 갱신에 실패했습니다.", "TOKEN_REFRESH_FAILED"));
        }
    }

    @ApiOperation(value = "로그인 상태 확인", notes = "현재 사용자의 로그인 상태를 확인합니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoginStatus(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        Map<String, Object> statusInfo = new HashMap<>();

        if (token != null && jwtProcessor.validateAccessTokenWithBlacklist(token)) {
            String userId = jwtProcessor.getUserIdFromToken(token);

            statusInfo.put("authenticated", true);
            statusInfo.put("userId", userId);
            statusInfo.put("tokenValid", true);
            statusInfo.put("tokenInfo", jwtProcessor.getTokenInfo(token));
        } else {
            statusInfo.put("authenticated", false);
            statusInfo.put("tokenValid", false);
        }

        return ResponseEntity.ok(ApiResponse.success("로그인 상태", statusInfo));
    }

    @ApiOperation(value = "토큰 유효성 검증", notes = "JWT 토큰의 유효성을 검증합니다.")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        Map<String, Object> validationInfo = new HashMap<>();

        if (token == null) {
            validationInfo.put("valid", false);
            validationInfo.put("reason", "토큰이 없습니다.");
            return ResponseEntity.ok(ApiResponse.success("토큰 검증 결과", validationInfo));
        }

        try {
            boolean isValid = jwtProcessor.validateAccessTokenWithBlacklist(token);
            validationInfo.put("valid", isValid);

            if (isValid) {
                String userId = jwtProcessor.getUserIdFromToken(token);
                validationInfo.put("userId", userId);
                validationInfo.put("tokenInfo", jwtProcessor.getTokenInfo(token));
            } else {
                validationInfo.put("reason", "토큰이 유효하지 않습니다.");
            }

            return ResponseEntity.ok(ApiResponse.success("토큰 검증 결과", validationInfo));

        } catch (Exception e) {
            log.warn("토큰 검증 중 오류: {}", e.getMessage());
            validationInfo.put("valid", false);
            validationInfo.put("reason", "토큰 검증 중 오류가 발생했습니다.");
            return ResponseEntity.ok(ApiResponse.success("토큰 검증 결과", validationInfo));
        }
    }

    @ApiOperation(value = "로그아웃", notes = "현재 사용자를 로그아웃하고 모든 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, String> requestBody) {

        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String accessToken = jwtProcessor.extractTokenFromHeader(authHeader);
        String clientIP = IPUtils.getClientIP(request);
        String refreshToken = null;

        // 요청 본문에서 Refresh Token 추출 (선택적)
        if (requestBody != null) {
            refreshToken = requestBody.get("refreshToken");
        }

        if (accessToken != null && jwtProcessor.validateAccessToken(accessToken)) {
            String userId = jwtProcessor.getUserIdFromToken(accessToken);

            // Access Token 블랙리스트 추가
            jwtProcessor.blacklistToken(accessToken);

            // 특정 Refresh Token이 제공된 경우 해당 토큰만 무효화
            if (refreshToken != null) {
                refreshTokenService.invalidateRefreshToken(refreshToken);
            } else {
                // 사용자의 모든 Refresh Token 무효화
                refreshTokenService.invalidateAllRefreshTokens(userId);
            }

            // 보안 이벤트 기록
            authService.recordSecurityEvent(userId, "LOGOUT", clientIP);

            log.info("로그아웃: {} from {}", userId, IPUtils.maskIP(clientIP));
        }

        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다.", null));
    }

    @ApiOperation(value = "토큰 폐기", notes = "현재 Access Token을 무효화합니다.")
    @PostMapping("/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeToken(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        if (token == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("토큰이 없습니다.", "MISSING_TOKEN"));
        }

        try {
            if (jwtProcessor.validateAccessToken(token)) {
                String userId = jwtProcessor.getUserIdFromToken(token);

                // 토큰을 블랙리스트에 추가
                jwtProcessor.blacklistToken(token);

                log.info("Access Token 폐기: {}", userId);
                return ResponseEntity.ok(ApiResponse.success("토큰이 폐기되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("유효하지 않은 토큰입니다.", "INVALID_TOKEN"));
            }
        } catch (Exception e) {
            log.error("토큰 폐기 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("토큰 폐기에 실패했습니다.", "TOKEN_REVOKE_FAILED"));
        }
    }

    @ApiOperation(value = "Refresh Token 폐기", notes = "특정 Refresh Token을 무효화합니다.")
    @PostMapping("/revoke-refresh")
    public ResponseEntity<ApiResponse<Void>> revokeRefreshToken(
            @Valid @RequestBody RefreshTokenDTO refreshTokenDTO,
            HttpServletRequest request) {

        String refreshToken = refreshTokenDTO.getRefreshToken();
        String clientIP = IPUtils.getClientIP(request);

        try {
            if (refreshTokenService.validateRefreshToken(refreshToken)) {
                String userId = refreshTokenService.getUserIdFromRefreshToken(refreshToken);

                // Refresh Token 무효화
                refreshTokenService.invalidateRefreshToken(refreshToken);

                // 보안 이벤트 기록
                if (userId != null) {
                    authService.recordSecurityEvent(userId, "REFRESH_TOKEN_REVOKED", clientIP);
                }

                log.info("Refresh Token 폐기: {}", userId);
                return ResponseEntity.ok(ApiResponse.success("Refresh Token이 폐기되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("유효하지 않은 Refresh Token입니다.", "INVALID_REFRESH_TOKEN"));
            }
        } catch (Exception e) {
            log.error("Refresh Token 폐기 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Refresh Token 폐기에 실패했습니다.", "REFRESH_TOKEN_REVOKE_FAILED"));
        }
    }

    @ApiOperation(value = "인증 정보 조회", notes = "현재 인증된 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        if (token == null || !jwtProcessor.validateAccessTokenWithBlacklist(token)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("유효하지 않은 토큰입니다.", "INVALID_TOKEN"));
        }

        try {
            String userId = jwtProcessor.getUserIdFromToken(token);
            MemberVO member = memberMapper.getMemberByUserId(userId);

            if (member == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"));
            }

            // 사용자 정보 구성
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", member.getUserId());
            userInfo.put("userName", member.getUserName());
            userInfo.put("userEmail", member.getUserEmail());
            userInfo.put("userPhone", member.getUserPhone());
            userInfo.put("birthDate", member.getBirthDate());
            userInfo.put("gender", member.getGender());
            userInfo.put("regDate", member.getRegDate());

            // Refresh Token 통계 추가
            Map<String, Object> tokenStats = refreshTokenService.getRefreshTokenStatistics(userId);
            userInfo.put("refreshTokenStats", tokenStats);

            return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", userInfo));

        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 정보 조회에 실패했습니다.", "USER_INFO_FAILED"));
        }
    }

    @ApiOperation(value = "활성 세션 조회", notes = "사용자의 활성 Refresh Token 목록을 조회합니다.")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveSessions(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        if (token == null || !jwtProcessor.validateAccessTokenWithBlacklist(token)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("유효하지 않은 토큰입니다.", "INVALID_TOKEN"));
        }

        try {
            String userId = jwtProcessor.getUserIdFromToken(token);
            Map<String, Object> sessionInfo = refreshTokenService.getRefreshTokenStatistics(userId);

            return ResponseEntity.ok(ApiResponse.success("활성 세션 조회 성공", sessionInfo));

        } catch (Exception e) {
            log.error("활성 세션 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("활성 세션 조회에 실패했습니다.", "SESSION_INFO_FAILED"));
        }
    }
}