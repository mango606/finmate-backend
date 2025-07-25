package com.example.finmate;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.*;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class JettyLauncher {

    public static void main(String[] args) throws Exception {
        forceUtf8Encoding();

        // 서버 생성 (포트 8080)
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        connector.setHost("localhost");
        server.addConnector(connector);

        // 웹 애플리케이션 컨텍스트 설정
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");

        // 웹앱 디렉토리 설정 - 절대 경로로 설정
        String webappPath = System.getProperty("user.dir") + "/src/main/webapp";
        File webappDir = new File(webappPath);

        System.out.println("==========================================");
        System.out.println("🚀 FinMate 서버 시작 중...");
        System.out.println("==========================================");
        System.out.println("📂 웹앱 디렉토리: " + webappDir.getAbsolutePath());
        System.out.println("📂 웹앱 디렉토리 존재: " + webappDir.exists());
        System.out.println("🌏 시스템 인코딩: " + System.getProperty("file.encoding"));
        System.out.println("🕐 시스템 타임존: " + System.getProperty("user.timezone"));
        System.out.println("🌍 시스템 언어: " + System.getProperty("user.language"));
        System.out.println("🔤 기본 문자셋: " + java.nio.charset.Charset.defaultCharset());

        if (webappDir.exists()) {
            webapp.setWar(webappDir.getAbsolutePath());
            webapp.setResourceBase(webappDir.getAbsolutePath());
        } else {
            // 대체 경로
            webapp.setResourceBase("src/main/webapp");
        }

        // 클래스패스 설정
        String classPath = System.getProperty("user.dir") + "/build/classes/java/main;" +
                System.getProperty("user.dir") + "/build/resources/main";
        webapp.setExtraClasspath(classPath);

        // 설정 클래스들 추가
        webapp.setConfigurations(new Configuration[]{
                new AnnotationConfiguration(),
                new WebInfConfiguration(),
                new WebXmlConfiguration(),
                new MetaInfConfiguration(),
                new FragmentConfiguration(),
                new EnvConfiguration(),
                new PlusConfiguration()
        });

        // 개발 모드 설정
        webapp.setParentLoaderPriority(true);
        webapp.setThrowUnavailableOnStartupException(true);

        // 임시 디렉토리 설정
        File tempDir = new File("build/tmp/jetty");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        webapp.setTempDirectory(tempDir);

        // 정적 리소스 설정
        webapp.setWelcomeFiles(new String[]{"index.html"});
        webapp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        // 서버에 웹앱 추가
        server.setHandler(webapp);

        try {
            // 서버 시작
            server.start();

            System.out.println("==========================================");
            System.out.println("🎉 FinMate 서버가 성공적으로 시작되었습니다!");
            System.out.println("==========================================");
            System.out.println("📍 메인 URL: http://localhost:8080");
            System.out.println("👥 회원 페이지: http://localhost:8080/member.html");
            System.out.println("📖 API 문서: http://localhost:8080/swagger-ui/index.html");
            System.out.println("🏓 Ping 테스트: http://localhost:8080/ping");
            System.out.println("❤️ Health 체크: http://localhost:8080/api/member/health");
            System.out.println("==========================================");
            System.out.println("🔧 테스트 계정:");
            System.out.println("   관리자: admin / finmate123!");
            System.out.println("   일반사용자: testuser / finmate123!");
            System.out.println("==========================================");
            System.out.println("💾 데이터베이스 정보:");
            System.out.println("   URL: localhost:3306/finmate_db");
            System.out.println("   사용자: finmate / 1234");
            System.out.println("   관리도구: http://localhost:8081 (phpMyAdmin)");
            System.out.println("==========================================");
            System.out.println("🛑 서버를 중지하려면 Ctrl+C를 누르세요.");
            System.out.println("==========================================");
            System.out.println("🧪 한글 테스트: 안녕하세요! FinMate 서버입니다. 🚀");

            // 서버 대기
            server.join();
        } catch (Exception e) {
            System.err.println("❌ 서버 시작 실패: " + e.getMessage());
            e.printStackTrace();
            if (server.isStarted()) {
                server.stop();
            }
            System.exit(1);
        }
    }

    private static void forceUtf8Encoding() {
        try {
            // 시스템 프로퍼티 강제 설정
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("native.encoding", "UTF-8");
            System.setProperty("console.encoding", "UTF-8");
            System.setProperty("user.timezone", "Asia/Seoul");
            System.setProperty("java.awt.headless", "true");
            System.setProperty("user.language", "ko");
            System.setProperty("user.country", "KR");
            System.setProperty("user.variant", "");
            System.setProperty("sun.jnu.encoding", "UTF-8");
            System.setProperty("sun.stderr.encoding", "UTF-8");
            System.setProperty("sun.stdout.encoding", "UTF-8");
            System.setProperty("sun.io.useCanonPrefixCache", "false");

            // Java 내부 인코딩 설정
            System.setProperty("java.nio.charset.Charset.defaultCharset", "UTF-8");

            // 기본 문자셋을 UTF-8로 강제 설정
            java.lang.reflect.Field charsetField = java.nio.charset.Charset.class.getDeclaredField("defaultCharset");
            charsetField.setAccessible(true);
            charsetField.set(null, StandardCharsets.UTF_8);

            // 출력 스트림 UTF-8 설정 시도
            if (System.out != null) {
                try {
                    java.lang.reflect.Field outField = System.class.getDeclaredField("out");
                    outField.setAccessible(true);
                    // 콘솔 출력을 UTF-8로 재설정 시도
                } catch (Exception e) {
                    // 무시 - 선택적 설정
                }
            }

        } catch (Exception e) {
            System.err.println("⚠️ UTF-8 인코딩 강제 설정 중 일부 실패: " + e.getMessage());
        }

        // 환경 정보 출력
        System.out.println("🔤 현재 파일 인코딩: " + System.getProperty("file.encoding"));
        System.out.println("🔤 기본 문자셋: " + java.nio.charset.Charset.defaultCharset());
        System.out.println("🔤 JNU 인코딩: " + System.getProperty("sun.jnu.encoding", "설정되지 않음"));
        System.out.println("🔤 콘솔 인코딩: " + System.getProperty("console.encoding", "설정되지 않음"));
        System.out.println("🌍 언어 설정: " + System.getProperty("user.language") + "_" + System.getProperty("user.country"));

        try {
            String testText = "🧪 한글 테스트: 안녕하세요! FinMate 서버입니다. 🚀";
            System.out.println(testText);

            byte[] bytes = testText.getBytes(StandardCharsets.UTF_8);
            String restored = new String(bytes, StandardCharsets.UTF_8);
            if (testText.equals(restored)) {
                System.out.println("✅ UTF-8 인코딩 정상 작동 확인");
            } else {
                System.out.println("⚠️ UTF-8 인코딩 문제 감지");
            }
        } catch (Exception e) {
            System.out.println("⚠️ 한글 테스트 실패: " + e.getMessage());
        }
    }
}