package com.example.finmate.auth.controller;

import com.example.finmate.auth.service.AuthService;
import com.example.finmate.common.dto.ApiResponse;
import com.example.finmate.common.exception.AuthenticationFailedException;
import com.example.finmate.common.util.AuthResponseUtil;
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
    private final JwtProcessor jwtProcessor;
    private final MemberMapper memberMapper;

    @ApiOperation(value = "사용자 로그인", notes = "사용자 ID와 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody MemberLoginDTO loginDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("로그인 시도: {} from {}", loginDTO.getUserId(), IPUtils.maskIP(clientIP));

        try {
            // Spring Security를 통한 인증
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginDTO.getUserId(), loginDTO.getUserPassword());

            Authentication authentication = authenticationManager.authenticate(authToken);
            User user = (User) authentication.getPrincipal();
            String userId = user.getUsername();

            // JWT 토큰 생성
            String token = jwtProcessor.generateToken(userId);

            // 사용자 정보 조회
            MemberVO member = memberMapper.getMemberByUserId(userId);

            // 유틸리티를 사용하여 응답 구성
            Map<String, Object> authResult = AuthResponseUtil.createAuthResultMap(token, member, user);

            // 로그인 성공 기록
            authService.recordLoginSuccess(userId, clientIP, userAgent);

            log.info("로그인 성공: {} from {}", userId, IPUtils.maskIP(clientIP));
            return ResponseEntity.ok(ApiResponse.success("로그인 성공", authResult));

        } catch (Exception e) {
            // 로그인 실패 기록
            authService.recordLoginFailure(loginDTO.getUserId(), clientIP, userAgent, e.getMessage());

            log.warn("로그인 실패: {} from {} - {}", loginDTO.getUserId(), IPUtils.maskIP(clientIP), e.getMessage());

            throw new AuthenticationFailedException("로그인에 실패했습니다. 사용자 ID와 비밀번호를 확인해주세요.");
        }
    }

    @ApiOperation(value = "로그인 상태 확인", notes = "현재 사용자의 로그인 상태를 확인합니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoginStatus(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        Map<String, Object> statusInfo = new HashMap<>();

        if (token != null && jwtProcessor.validateToken(token)) {
            String userId = jwtProcessor.getUserIdFromToken(token);

            statusInfo.put("authenticated", true);
            statusInfo.put("userId", userId);
            statusInfo.put("tokenValid", true);
        } else {
            statusInfo.put("authenticated", false);
            statusInfo.put("tokenValid", false);
        }

        return ResponseEntity.ok(ApiResponse.success(statusInfo));
    }

    @ApiOperation(value = "토큰 갱신", notes = "유효한 토큰을 사용하여 새로운 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        if (token == null || !jwtProcessor.validateToken(token)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("유효하지 않은 토큰입니다.", "INVALID_TOKEN"));
        }

        try {
            String userId = jwtProcessor.getUserIdFromToken(token);
            String newToken = jwtProcessor.generateToken(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("token", newToken);

            log.info("토큰 갱신: {}", userId);
            return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", result));

        } catch (Exception e) {
            log.error("토큰 갱신 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("토큰 갱신에 실패했습니다.", "TOKEN_REFRESH_FAILED"));
        }
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
            boolean isValid = jwtProcessor.validateToken(token);
            validationInfo.put("valid", isValid);

            if (isValid) {
                String userId = jwtProcessor.getUserIdFromToken(token);
                validationInfo.put("userId", userId);
                validationInfo.put("expirationDate", jwtProcessor.getExpirationDateFromToken(token));
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

    @ApiOperation(value = "로그아웃", notes = "현재 사용자를 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);
        String clientIP = IPUtils.getClientIP(request);

        if (token != null && jwtProcessor.validateToken(token)) {
            String userId = jwtProcessor.getUserIdFromToken(token);

            // 토큰을 블랙리스트에 추가
            jwtProcessor.blacklistToken(token);

            // 보안 이벤트 기록
            authService.recordSecurityEvent(userId, "LOGOUT", clientIP);

            log.info("로그아웃: {} from {}", userId, IPUtils.maskIP(clientIP));
        }

        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다.", null));
    }

    @ApiOperation(value = "토큰 폐기", notes = "현재 토큰을 무효화합니다.")
    @PostMapping("/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeToken(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        if (token == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("토큰이 없습니다.", "MISSING_TOKEN"));
        }

        try {
            if (jwtProcessor.validateToken(token)) {
                String userId = jwtProcessor.getUserIdFromToken(token);

                // 토큰을 블랙리스트에 추가
                jwtProcessor.blacklistToken(token);

                log.info("토큰 폐기: {}", userId);
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

    @ApiOperation(value = "인증 정보 조회", notes = "현재 인증된 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        if (token == null || !jwtProcessor.validateToken(token)) {
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

            // 유틸리티를 사용하여 사용자 정보 생성
            Map<String, Object> userInfo = AuthResponseUtil.createUserInfoMap(member);

            return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", userInfo));

        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 정보 조회에 실패했습니다.", "USER_INFO_FAILED"));
        }
    }
}