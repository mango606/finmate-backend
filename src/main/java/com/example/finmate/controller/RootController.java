package com.example.finmate.controller;

import com.example.finmate.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class RootController {

    @GetMapping(value = "/", produces = "text/html; charset=UTF-8")
    public ResponseEntity<String> index() {
        try {
            // index.html 파일을 직접 읽어서 반환
            String indexPath = "src/main/webapp/index.html";
            if (Files.exists(Paths.get(indexPath))) {
                String content = new String(Files.readAllBytes(Paths.get(indexPath)), "UTF-8");
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .body(content);
            } else {
                // 파일이 없으면 기본 HTML 반환
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .body(getDefaultIndexHtml());
            }
        } catch (IOException e) {
            // 오류 시 기본 HTML 반환
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(getDefaultIndexHtml());
        }
    }

    @GetMapping(value = "/ping", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping() {
        log.info("서버 ping 요청");

        Map<String, Object> pingInfo = new HashMap<>();
        pingInfo.put("status", "OK");
        pingInfo.put("message", "FinMate 서버가 정상 동작 중입니다");
        pingInfo.put("timestamp", System.currentTimeMillis());
        pingInfo.put("server", "FinMate Backend v1.0.0");
        pingInfo.put("encoding", "UTF-8");

        return ResponseEntity.ok(ApiResponse.success("서버 응답 성공", pingInfo));
    }

    @GetMapping(value = "/hello", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ApiResponse<String>> hello() {
        log.info("Hello 요청");

        String message = "Hello FinMate! 안녕하세요! 서버가 정상 동작 중입니다. 🚀";
        return ResponseEntity.ok(ApiResponse.success("Hello 메시지", message));
    }

    private String getDefaultIndexHtml() {
        return """
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
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
        .status {
            display: flex;
            justify-content: space-around;
            margin: 30px 0;
            flex-wrap: wrap;
        }
        .status-item {
            padding: 20px;
            background: #f8f9fa;
            border-radius: 10px;
            border-left: 4px solid #007bff;
            margin: 10px;
            flex: 1;
            min-width: 200px;
        }
        .status-item h3 {
            margin: 0 0 10px 0;
            color: #007bff;
        }
        .status-item p {
            margin: 0;
            font-weight: bold;
        }
        .api-links {
            margin: 30px 0;
        }
        .api-link {
            display: inline-block;
            margin: 10px;
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
        .info {
            background: #e7f3ff;
            padding: 20px;
            border-radius: 10px;
            margin-top: 20px;
            border-left: 4px solid #007bff;
            text-align: left;
        }
        .info h3 {
            margin-top: 0;
            color: #007bff;
        }
        .server-status {
            color: #28a745;
            font-size: 20px;
            font-weight: bold;
            margin: 20px 0;
        }
        @media (max-width: 768px) {
            .status {
                flex-direction: column;
            }
            .status-item {
                margin: 5px 0;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <h1>🏦 FinMate</h1>
    <p class="subtitle">제테크 금융 서비스 백엔드 API</p>
    <p class="server-status">✅ 서버가 정상 동작 중입니다!</p>

    <div class="status">
        <div class="status-item">
            <h3>서버 상태</h3>
            <p id="server-status">🟢 정상 운영</p>
        </div>
        <div class="status-item">
            <h3>데이터베이스</h3>
            <p id="db-status">🔵 연결 확인 중...</p>
        </div>
        <div class="status-item">
            <h3>API 버전</h3>
            <p>v1.0.0</p>
        </div>
    </div>

    <div class="api-links">
        <a href="/hello" class="api-link">👋 Hello 테스트</a>
        <a href="/ping" class="api-link">🏓 서버 Ping</a>
        <a href="/api/member/health" class="api-link">❤️ Health 체크</a>
        <a href="/swagger-ui.html" class="api-link">📖 API 문서</a>
    </div>

    <div class="info">
        <h3>📋 시스템 정보</h3>
        <p><strong>프로젝트:</strong> FinMate Backend</p>
        <p><strong>기술 스택:</strong> Spring Legacy 5.x, MyBatis, MySQL, JWT</p>
        <p><strong>서버 포트:</strong> 8080 (Jetty)</p>
        <p><strong>데이터베이스:</strong> MySQL (localhost:3306/finmate_db)</p>
        <p><strong>현재 시간:</strong> <span id="current-time"></span></p>
    </div>

    <div class="info">
        <h3>🔧 개발 환경</h3>
        <p><strong>빌드 도구:</strong> Gradle 8.x</p>
        <p><strong>관리자 도구:</strong> <a href="http://localhost:8081" target="_blank">phpMyAdmin</a></p>
        <p><strong>파일 업로드:</strong> c:/upload/</p>
    </div>

    <div class="info">
        <h3>🧪 테스트 엔드포인트</h3>
        <p><strong>Hello:</strong> <a href="/hello" target="_blank">http://localhost:8080/hello</a></p>
        <p><strong>Ping:</strong> <a href="/ping" target="_blank">http://localhost:8080/ping</a></p>
        <p><strong>Health:</strong> <a href="/api/member/health" target="_blank">http://localhost:8080/api/health</a></p>
        <p><strong>API 문서:</strong> <a href="/swagger-ui.html" target="_blank">http://localhost:8080/swagger-ui.html</a></p>
    </div>
</div>

<script>
    // 현재 시간 업데이트
    function updateTime() {
        document.getElementById('current-time').textContent = new Date().toLocaleString('ko-KR');
    }
    updateTime();
    setInterval(updateTime, 1000);

    // 서버 연결 테스트
    fetch('/ping')
        .then(response => response.json())
        .then(data => {
            document.getElementById('db-status').innerHTML = '🟢 연결됨';
            console.log('✅ 서버 연결 성공:', data);
        })
        .catch(error => {
            document.getElementById('db-status').innerHTML = '🔴 연결 실패';
            console.error('❌ 서버 연결 실패:', error);
        });

    // Health 체크
    fetch('/api/member/health')
        .then(response => response.json())
        .then(data => {
            console.log('✅ Health 체크 성공:', data);
        })
        .catch(error => {
            console.warn('⚠️ Health 체크 실패:', error);
        });
</script>
</body>
</html>
                """;
    }
}