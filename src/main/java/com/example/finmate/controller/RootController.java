package com.example.finmate.controller;

import com.example.finmate.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class RootController {

    @GetMapping(value = "/", produces = "text/html; charset=UTF-8")
    public ResponseEntity<String> index(HttpServletResponse response) {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            // index.html 파일을 직접 읽어서 반환
            String indexPath = "src/main/webapp/index.html";
            if (Files.exists(Paths.get(indexPath))) {
                String content = Files.readString(Paths.get(indexPath), StandardCharsets.UTF_8);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "text/html; charset=UTF-8");
                headers.set("Cache-Control", "no-cache");
                headers.set("Pragma", "no-cache");

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(content);
            } else {
                // 파일이 없으면 기본 HTML 반환
                return getUtf8HtmlResponse(getDefaultIndexHtml());
            }
        } catch (IOException e) {
            log.error("index.html 파일 읽기 실패", e);
            // 오류 시 기본 HTML 반환
            return getUtf8HtmlResponse(getDefaultIndexHtml());
        }
    }

    @GetMapping(value = "/ping", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping(HttpServletResponse response) {
        log.info("서버 ping 요청");

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> pingInfo = new HashMap<>();
        pingInfo.put("status", "OK");
        pingInfo.put("message", "FinMate 서버가 정상 동작 중입니다");
        pingInfo.put("koreanTest", "한글 테스트: 안녕하세요! 🚀");
        pingInfo.put("timestamp", System.currentTimeMillis());
        pingInfo.put("server", "FinMate Backend v1.0.0");
        pingInfo.put("encoding", "UTF-8");
        pingInfo.put("charset", StandardCharsets.UTF_8.name());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Cache-Control", "no-cache");

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success("서버 응답 성공", pingInfo));
    }

    @GetMapping(value = "/hello", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ApiResponse<String>> hello(HttpServletResponse response) {
        log.info("Hello 요청");

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String message = "Hello FinMate! 안녕하세요! 서버가 정상 동작 중입니다. 🚀";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success("Hello 메시지", message));
    }

    // member.html 직접 서빙
    @GetMapping(value = "/member.html", produces = "text/html; charset=UTF-8")
    public ResponseEntity<String> memberPage(HttpServletResponse response) {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            String memberPath = "src/main/webapp/member.html";
            if (Files.exists(Paths.get(memberPath))) {
                String content = Files.readString(Paths.get(memberPath), StandardCharsets.UTF_8);
                return getUtf8HtmlResponse(content);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("member.html 파일 읽기 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 테스트용 HTML 파일들 서빙
    @GetMapping(value = "/{filename:.+\\.html}", produces = "text/html; charset=UTF-8")
    public ResponseEntity<String> serveHtmlFile(javax.servlet.http.HttpServletRequest request,
                                                HttpServletResponse response) {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            String requestURI = request.getRequestURI();
            String filename = requestURI.substring(1); // 앞의 '/' 제거
            String filePath = "src/main/webapp/" + filename;

            if (Files.exists(Paths.get(filePath))) {
                String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
                return getUtf8HtmlResponse(content);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("HTML 파일 읽기 실패: {}", request.getRequestURI(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<String> getUtf8HtmlResponse(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/html; charset=UTF-8");
        headers.set("Cache-Control", "no-cache");
        headers.set("Pragma", "no-cache");

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    private String getDefaultIndexHtml() {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>FinMate - 제테크 금융 서비스</title>
                    <style>
                        body {
                            font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', Arial, sans-serif;
                            margin: 0;
                            padding: 0;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            min-height: 100vh;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 15px;
                            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                            text-align: center;
                            max-width: 700px;
                            width: 90%;
                        }
                        h1 {
                            color: #333;
                            margin-bottom: 10px;
                            font-size: 2.5em;
                        }
                        .subtitle {
                            color: #666;
                            margin-bottom: 30px;
                            font-size: 18px;
                        }
                        .api-links {
                            margin: 30px 0;
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                            gap: 15px;
                        }
                        .api-link {
                            display: block;
                            padding: 15px 25px;
                            background: #007bff;
                            color: white;
                            text-decoration: none;
                            border-radius: 8px;
                            transition: all 0.3s;
                            font-weight: bold;
                        }
                        .api-link:hover {
                            background: #0056b3;
                            transform: translateY(-2px);
                            box-shadow: 0 5px 15px rgba(0,123,255,0.3);
                        }
                        .api-link.primary {
                            background: linear-gradient(45deg, #667eea, #764ba2);
                        }
                        .api-link.primary:hover {
                            transform: translateY(-3px);
                            box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
                        }
                        .server-status {
                            color: #28a745;
                            font-size: 20px;
                            font-weight: bold;
                            margin: 20px 0;
                        }
                        .encoding-test {
                            background: #f8f9fa;
                            padding: 15px;
                            border-radius: 8px;
                            margin: 20px 0;
                            border-left: 4px solid #28a745;
                        }
                    </style>
                </head>
                <body>
                <div class="container">
                    <h1>🏦 FinMate</h1>
                    <p class="subtitle">제테크 금융 서비스 백엔드 API</p>
                    <p class="server-status">✅ 서버가 정상 동작 중입니다!</p>
                
                    <div class="encoding-test">
                        <h3>🔤 한글 인코딩 테스트</h3>
                        <p><strong>기본 테스트:</strong> 안녕하세요! FinMate입니다.</p>
                        <p><strong>특수문자:</strong> 🚀 🏦 👥 📖 🏓 ❤️</p>
                        <p><strong>UTF-8 확인:</strong> UTF-8 인코딩이 정상 작동 중입니다.</p>
                    </div>
                
                    <div class="api-links">
                        <a href="/member.html" class="api-link primary">👥 로그인</a>
                        <a href="/hello" class="api-link">👋 Hello 테스트</a>
                        <a href="/ping" class="api-link">🏓 서버 Ping</a>
                        <a href="/api/member/health" class="api-link">❤️ Health 체크</a>
                        <a href="/swagger-ui/index.html" class="api-link">📖 API 문서</a>
                    </div>
                
                    <div style="background: #e7f3ff; padding: 20px; border-radius: 10px; margin-top: 20px; text-align: left;">
                        <h3 style="margin-top: 0; color: #007bff;">📋 시스템 정보</h3>
                        <p><strong>프로젝트:</strong> FinMate Backend</p>
                        <p><strong>기술 스택:</strong> Spring Legacy 5.x, MyBatis, MySQL, JWT</p>
                        <p><strong>서버 포트:</strong> 8080 (Jetty)</p>
                        <p><strong>문자 인코딩:</strong> UTF-8</p>
                        <p><strong>현재 시간:</strong> <span id="current-time"></span></p>
                    </div>
                </div>
                
                <script>
                    function updateTime() {
                        document.getElementById('current-time').textContent = new Date().toLocaleString('ko-KR');
                    }
                    updateTime();
                    setInterval(updateTime, 1000);
                
                    // 인코딩 테스트
                    fetch('/ping')
                        .then(response => response.json())
                        .then(data => {
                            console.log('✅ 서버 연결 성공:', data);
                            if (data.data && data.data.koreanTest) {
                                console.log('✅ 한글 테스트:', data.data.koreanTest);
                            }
                        })
                        .catch(error => console.error('❌ 서버 연결 실패:', error));
                </script>
                </body>
                </html>
                """;
    }
}