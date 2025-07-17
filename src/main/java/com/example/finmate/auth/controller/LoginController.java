package com.example.finmate.auth.controller;

import com.example.finmate.auth.service.AuthService;
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
import java.util.stream.Collectors;

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
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody MemberLoginDTO loginDTO,
            HttpServletRequest request) {

        String clientIP = IPUtils.getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("로그인 시도: {} from {}", loginDTO.getUserId(), IPUtils.maskIP(clientIP));

        Map<String, Object> response = new HashMap<>();

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

            // 사용자 정보 DTO 생성
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
                    .collect(Collectors.toList()));

            // 로그인 성공 기록
            authService.recordLoginSuccess(userId, clientIP, userAgent);

            // 응답 구성
            Map<String, Object> authResult = new HashMap<>();
            authResult.put("token", token);
            authResult.put("user", userInfo);

            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("data", authResult);

            log.info("로그인 성공: {} from {}", userId, IPUtils.maskIP(clientIP));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // 로그인 실패 기록
            authService.recordLoginFailure(loginDTO.getUserId(), clientIP, userAgent, e.getMessage());

            log.warn("로그인 실패: {} from {} - {}", loginDTO.getUserId(), IPUtils.maskIP(clientIP), e.getMessage());

            response.put("success", false);
            response.put("message", "로그인에 실패했습니다. 사용자 ID와 비밀번호를 확인해주세요.");
            response.put("error", "AUTHENTICATION_FAILED");

            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "로그인 상태 확인", notes = "현재 사용자의 로그인 상태를 확인합니다.")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getLoginStatus(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        Map<String, Object> response = new HashMap<>();

        if (token != null && jwtProcessor.validateToken(token)) {
            String userId = jwtProcessor.getUserIdFromToken(token);

            response.put("success", true);
            response.put("authenticated", true);
            response.put("userId", userId);
            response.put("tokenValid", true);
        } else {
            response.put("success", true);
            response.put("authenticated", false);
            response.put("tokenValid", false);
        }

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "토큰 갱신", notes = "유효한 토큰을 사용하여 새로운 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProcessor.getHeader());
        String token = jwtProcessor.extractTokenFromHeader(authHeader);

        Map<String, Object> response = new HashMap<>();

        if (token == null || !jwtProcessor.validateToken(token)) {
            response.put("success", false);
            response.put("message", "유효하지 않은 토큰입니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String userId = jwtProcessor.getUserIdFromToken(token);
            String newToken = jwtProcessor.generateToken(userId);

            response.put("success", true);
            response.put("message", "토큰이 갱신되었습니다.");
            response.put("token", newToken);

            log.info("토큰 갱신: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("토큰 갱신 실패", e);
            response.put("success", false);
            response.put("message", "토큰 갱신에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }
}