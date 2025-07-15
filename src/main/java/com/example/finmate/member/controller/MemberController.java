package com.example.finmate.member.controller;

import com.example.finmate.member.dto.MemberInfoDTO;
import com.example.finmate.member.dto.MemberJoinDTO;
import com.example.finmate.member.dto.MemberUpdateDTO;
import com.example.finmate.member.service.MemberService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Api(tags = "회원 관리 API")
public class MemberController {

    private final MemberService memberService;

    @ApiOperation(value = "회원 가입", notes = "새로운 회원을 등록합니다.")
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> insertMember(@Valid @RequestBody MemberJoinDTO joinDTO) {
        log.info("회원 가입 요청: {}", joinDTO.getUserId());

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = memberService.insertMember(joinDTO);

            if (success) {
                response.put("success", true);
                response.put("message", "회원 가입이 완료되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "회원 가입에 실패했습니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("회원 가입 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "회원 정보 조회", notes = "로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getMemberInfo(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();

        log.info("회원 정보 조회: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);

            response.put("success", true);
            response.put("data", memberInfo);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("회원 정보 조회 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "회원 정보 수정", notes = "로그인한 사용자의 정보를 수정합니다.")
    @PutMapping("/info")
    public ResponseEntity<Map<String, Object>> updateMember(
            Authentication authentication,
            @Valid @RequestBody MemberUpdateDTO updateDTO) {

        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();

        log.info("회원 정보 수정: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = memberService.updateMember(userId, updateDTO);

            if (success) {
                response.put("success", true);
                response.put("message", "회원 정보가 수정되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "회원 정보 수정에 실패했습니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("회원 정보 수정 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "비밀번호 변경", notes = "로그인한 사용자의 비밀번호를 변경합니다.")
    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> updateMemberPassword(
            Authentication authentication,
            @RequestBody Map<String, String> passwordData) {

        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();

        log.info("비밀번호 변경: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                response.put("success", false);
                response.put("message", "현재 비밀번호와 새 비밀번호를 입력해주세요.");
                return ResponseEntity.badRequest().body(response);
            }

            boolean success = memberService.updateMemberPassword(userId, currentPassword, newPassword);

            if (success) {
                response.put("success", true);
                response.put("message", "비밀번호가 변경되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "비밀번호 변경에 실패했습니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("비밀번호 변경 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "회원 탈퇴", notes = "로그인한 사용자의 계정을 삭제합니다.")
    @DeleteMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> deleteMember(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getUsername();

        log.info("회원 탈퇴: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = memberService.deleteMember(userId);

            if (success) {
                response.put("success", true);
                response.put("message", "회원 탈퇴가 완료되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "회원 탈퇴에 실패했습니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("회원 탈퇴 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @ApiOperation(value = "사용자 ID 중복 확인", notes = "회원가입 시 사용자 ID 중복을 확인합니다.")
    @GetMapping("/checkUserId/{userId}")
    public ResponseEntity<Map<String, Object>> checkUserIdDuplicate(
            @ApiParam(value = "확인할 사용자 ID", required = true) @PathVariable String userId) {

        log.info("사용자 ID 중복 확인: {}", userId);

        Map<String, Object> response = new HashMap<>();
        boolean isDuplicate = memberService.checkUserIdDuplicate(userId);

        response.put("success", true);
        response.put("isDuplicate", isDuplicate);
        response.put("message", isDuplicate ? "이미 사용 중인 ID입니다." : "사용 가능한 ID입니다.");

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "이메일 중복 확인", notes = "회원가입 시 이메일 중복을 확인합니다.")
    @GetMapping("/checkEmail")
    public ResponseEntity<Map<String, Object>> checkUserEmailDuplicate(
            @ApiParam(value = "확인할 이메일", required = true) @RequestParam String userEmail) {

        log.info("이메일 중복 확인: {}", userEmail);

        Map<String, Object> response = new HashMap<>();
        boolean isDuplicate = memberService.checkUserEmailDuplicate(userEmail);

        response.put("success", true);
        response.put("isDuplicate", isDuplicate);
        response.put("message", isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.");

        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "서버 상태 확인", notes = "서버와 데이터베이스 연결 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "OK");
        response.put("message", "FinMate 서버가 정상 동작 중입니다.");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}