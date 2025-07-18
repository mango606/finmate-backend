package com.example.finmate.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Value("${app.name}")
    private String appName;

    @Value("${app.environment}")
    private String environment;

    // 회원가입 환영 메일
    public boolean sendWelcomeEmail(String userEmail, String userName) {
        log.info("환영 메일 발송 시뮬레이션: {} ({})", userName, userEmail);

        String subject = appName + " 회원가입을 환영합니다!";
        String content = createWelcomeEmailContent(userName);

        return sendEmail(userEmail, subject, content);
    }

    // 비밀번호 재설정 메일
    public boolean sendPasswordResetEmail(String userEmail, String resetToken) {
        log.info("비밀번호 재설정 메일 발송 시뮬레이션: {}", userEmail);

        String subject = appName + " 비밀번호 재설정 안내";
        String content = createPasswordResetEmailContent(resetToken);

        return sendEmail(userEmail, subject, content);
    }

    // 계정 활성화 메일
    public boolean sendActivationEmail(String userEmail, String activationToken) {
        log.info("계정 활성화 메일 발송 시뮬레이션: {}", userEmail);

        String subject = appName + " 계정 활성화 안내";
        String content = createActivationEmailContent(activationToken);

        return sendEmail(userEmail, subject, content);
    }

    // 계정 잠금 알림 메일
    public boolean sendAccountLockNotification(String userEmail, String userName) {
        log.info("계정 잠금 알림 메일 발송: {}", userEmail);

        String subject = appName + " 계정 잠금 알림";
        String content = String.format(
                "안녕하세요 %s님,\n\n" +
                        "보안을 위해 계정이 일시적으로 잠금되었습니다.\n" +
                        "잠금 해제를 원하시면 고객센터로 문의해주세요.\n\n" +
                        "감사합니다.\n" +
                        "%s 팀",
                userName, appName
        );

        return sendEmail(userEmail, subject, content);
    }

    // 비밀번호 변경 알림 메일
    public boolean sendPasswordChangeNotification(String userEmail, String userName) {
        log.info("비밀번호 변경 알림 메일 발송: {}", userEmail);

        String subject = appName + " 비밀번호 변경 알림";
        String content = String.format(
                "안녕하세요 %s님,\n\n" +
                        "회원님의 비밀번호가 성공적으로 변경되었습니다.\n" +
                        "만약 본인이 변경하지 않았다면 즉시 고객센터로 문의해주세요.\n\n" +
                        "감사합니다.\n" +
                        "%s 팀",
                userName, appName
        );

        return sendEmail(userEmail, subject, content);
    }

    // 보안 알림 메일
    public boolean sendSecurityAlert(String userEmail, String userName, String reason, String clientIP) {
        log.info("보안 알림 메일 발송: {} - {}", userEmail, reason);

        String subject = appName + " 보안 알림 - 의심스러운 활동 감지";
        String content = createSecurityAlertContent(userName, reason, clientIP);

        return sendEmail(userEmail, subject, content);
    }

    // 로그인 알림 메일
    public boolean sendLoginNotification(String userEmail, String userName, String clientIP, String userAgent) {
        log.info("로그인 알림 메일 발송: {}", userEmail);

        String subject = appName + " 새로운 로그인 알림";
        String content = String.format(
                "안녕하세요 %s님,\n\n" +
                        "회원님의 계정에 새로운 로그인이 감지되었습니다.\n\n" +
                        "로그인 정보:\n" +
                        "- 시간: %s\n" +
                        "- IP 주소: %s\n" +
                        "- 브라우저: %s\n\n" +
                        "본인이 로그인한 것이 아니라면 즉시 비밀번호를 변경하고 고객센터로 문의해주세요.\n\n" +
                        "감사합니다.\n" +
                        "%s 팀",
                userName,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                maskIP(clientIP),
                userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 50)) + "..." : "알 수 없음",
                appName
        );

        return sendEmail(userEmail, subject, content);
    }

    // 2단계 인증 설정 완료 알림
    public boolean sendTwoFactorEnabledNotification(String userEmail, String userName) {
        log.info("2단계 인증 활성화 알림 메일 발송: {}", userEmail);

        String subject = appName + " 2단계 인증 활성화 완료";
        String content = String.format(
                "안녕하세요 %s님,\n\n" +
                        "회원님의 계정에 2단계 인증이 성공적으로 활성화되었습니다.\n" +
                        "이제 로그인 시 추가 인증 코드가 필요합니다.\n\n" +
                        "보안이 한층 강화되었습니다!\n\n" +
                        "감사합니다.\n" +
                        "%s 팀",
                userName, appName
        );

        return sendEmail(userEmail, subject, content);
    }

    // 계정 복구 알림
    public boolean sendAccountRecoveryNotification(String userEmail, String userName) {
        log.info("계정 복구 알림 메일 발송: {}", userEmail);

        String subject = appName + " 계정 복구 완료";
        String content = String.format(
                "안녕하세요 %s님,\n\n" +
                        "회원님의 계정이 성공적으로 복구되었습니다.\n" +
                        "이제 정상적으로 서비스를 이용하실 수 있습니다.\n\n" +
                        "보안을 위해 비밀번호를 변경하실 것을 권장합니다.\n\n" +
                        "감사합니다.\n" +
                        "%s 팀",
                userName, appName
        );

        return sendEmail(userEmail, subject, content);
    }

    // 메일 발송
    public boolean sendEmail(String to, String subject, String content) {
        if ("development".equals(environment)) {
            log.info("=== 메일 발송 시뮬레이션 ===");
            log.info("수신자: {}", to);
            log.info("제목: {}", subject);
            log.info("내용: {}", content);
            log.info("========================");
            return true;
        }

        // TODO: SMTP 설정을 통해 메일 발송
        // JavaMailSender 등을 사용하여 구현
        return true;
    }

    // 프라이빗 메서드들
    private String createWelcomeEmailContent(String userName) {
        return String.format(
                "안녕하세요 %s님,\n\n" +
                        "%s에 가입해 주셔서 감사합니다!\n\n" +
                        "이제 다양한 제테크 서비스를 이용하실 수 있습니다.\n" +
                        "궁금한 점이 있으시면 언제든 문의해 주세요.\n\n" +
                        "감사합니다.\n" +
                        "%s 팀",
                userName, appName, appName
        );
    }

    private String createPasswordResetEmailContent(String resetToken) {
        return String.format(
                "비밀번호 재설정을 요청하셨습니다.\n\n" +
                        "아래 링크를 클릭하여 새로운 비밀번호를 설정해 주세요:\n" +
                        "http://localhost:8080/reset-password?token=%s\n\n" +
                        "이 링크는 24시간 후 만료됩니다.\n" +
                        "만약 비밀번호 재설정을 요청하지 않으셨다면 이 메일을 무시해 주세요.\n\n" +
                        "%s 팀",
                resetToken, appName
        );
    }

    private String createActivationEmailContent(String activationToken) {
        return String.format(
                "계정 활성화를 위해 아래 링크를 클릭해 주세요:\n\n" +
                        "http://localhost:8080/activate?token=%s\n\n" +
                        "이 링크는 7일 후 만료됩니다.\n\n" +
                        "%s 팀",
                activationToken, appName
        );
    }

    private String createSecurityAlertContent(String userName, String reason, String clientIP) {
        return String.format(
                "안녕하세요 %s님,\n\n" +
                        "⚠️ 보안 알림: 회원님의 계정에서 의심스러운 활동이 감지되었습니다.\n\n" +
                        "감지된 활동:\n" +
                        "- 사유: %s\n" +
                        "- 시간: %s\n" +
                        "- IP 주소: %s\n\n" +
                        "본인의 활동이 맞다면 이 메일을 무시하셔도 됩니다.\n" +
                        "만약 본인의 활동이 아니라면:\n" +
                        "1. 즉시 비밀번호를 변경해주세요\n" +
                        "2. 2단계 인증을 활성화해주세요\n" +
                        "3. 고객센터로 문의해주세요\n\n" +
                        "계정 보안: http://localhost:8080/security-settings\n\n" +
                        "감사합니다.\n" +
                        "%s 보안팀",
                userName, reason,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                maskIP(clientIP), appName
        );
    }

    private String maskIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "알 수 없음";
        }

        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***." + parts[3];
        }

        return ip.substring(0, Math.min(ip.length(), 8)) + "***";
    }
}