package com.example.finmate;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class JettyLauncher {

    public static void main(String[] args) throws Exception {
        // UTF-8 시스템 프로퍼티 설정
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.timezone", "Asia/Seoul");
        System.setProperty("console.encoding", "UTF-8");
        System.setProperty("java.awt.headless", "true");
        System.setProperty("user.language", "ko");
        System.setProperty("user.country", "KR");

        // JVM 인코딩 강제 설정
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("file.encoding.pkg", "sun.io");

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

        System.out.println("웹앱 디렉토리: " + webappDir.getAbsolutePath());
        System.out.println("웹앱 디렉토리 존재: " + webappDir.exists());

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

        // 서버에 웹앱 추가
        server.setHandler(webapp);

        try {
            // 서버 시작
            server.start();

            System.out.println("=================================");
            System.out.println("🚀 FinMate 서버가 시작되었습니다!");
            System.out.println("📍 URL: http://localhost:8080");
            System.out.println("👥 회원 페이지: http://localhost:8080/member.html");
            System.out.println("📖 API 문서: http://localhost:8080/swagger-ui.html");
            System.out.println("🔧 관리자 계정: admin / finmate123!");
            System.out.println("👤 테스트 계정: testuser / finmate123!");
            System.out.println("=================================");
            System.out.println("서버를 중지하려면 Ctrl+C를 누르세요.");

            // 서버 대기
            server.join();
        } catch (Exception e) {
            System.err.println("❌ 서버 시작 실패: " + e.getMessage());
            e.printStackTrace();
            if (server.isStarted()) {
                server.stop();
            }
        }
    }
}