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

    // TODO: 회원가입 환영 메일
    public boolean sendWelcomeEmail(String userEmail, String userName) {
        log.info("환영 메일 발송 시뮬레이션: {} ({})", userName, userEmail);

        String subject = appName + " 회원가입을 환영합니다!";
        String content = createWelcomeEmailContent(userName);

        return sendEmail(userEmail, subject, content);
    }

    // TODO: 비밀번호 재설정 메일
    public boolean sendPasswordResetEmail(String userEmail, String resetToken) {
        log.info("비밀번호 재설정 메일 발송 시뮬레이션: {}", userEmail);

        String subject = appName + " 비밀번호 재설정 안내";
        String content = createPasswordResetEmailContent(resetToken);

        return sendEmail(userEmail, subject, content);
    }

    // TODO: 계정 활성화 메일
    public boolean sendActivationEmail(String userEmail, String activationToken) {
        log.info("계정 활성화 메일 발송 시뮬레이션: {}", userEmail);

        String subject = appName + " 계정 활성화 안내";
        String content = createActivationEmailContent(activationToken);

        return sendEmail(userEmail, subject, content);
    }

    // TODO: 실제 메일 발송
    private boolean sendEmail(String to, String subject, String content) {
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
}