package com.example.finmate.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Log4j2
public class JsonResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // JSON 응답 전송
    public static void send(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        try (PrintWriter out = response.getWriter()) {
            String jsonString = objectMapper.writeValueAsString(data);
            out.print(jsonString);
            out.flush();
            log.debug("JSON 응답 전송: {}", jsonString);
        }
    }

    // 에러 응답 전송
    public static void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);

        ErrorResponse errorResponse = new ErrorResponse(false, message);

        try (PrintWriter out = response.getWriter()) {
            String jsonString = objectMapper.writeValueAsString(errorResponse);
            out.print(jsonString);
            out.flush();
            log.warn("에러 응답 전송: {}", jsonString);
        }
    }

    // 성공 응답 전송
    public static void sendSuccess(HttpServletResponse response, Object data) throws IOException {
        SuccessResponse successResponse = new SuccessResponse(true, data);
        send(response, successResponse);
    }

    // 성공 응답 DTO
    public static class SuccessResponse {
        public boolean success;
        public Object data;

        public SuccessResponse(boolean success, Object data) {
            this.success = success;
            this.data = data;
        }
    }

    // 에러 응답 DTO
    public static class ErrorResponse {
        public boolean success;
        public String message;

        public ErrorResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}