package com.example.finmate.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class IPUtils {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    // 클라이언트 실제 IP 주소 추출
    public static String getClientIP(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 첫 번째 IP만 사용 (프록시 체인의 경우)
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    // IP 주소 유효성 검사
    public static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // 로컬 IP인지 확인
    public static boolean isLocalIP(String ip) {
        if (!isValidIP(ip)) {
            return false;
        }

        return ip.startsWith("127.") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                ip.equals("0:0:0:0:0:0:0:1") ||
                ip.equals("::1");
    }

    // IP 마스킹 (프라이버시 보호)
    public static String maskIP(String ip) {
        if (!isValidIP(ip)) {
            return ip;
        }

        String[] parts = ip.split("\\.");
        return parts[0] + "." + parts[1] + ".***." + parts[3];
    }
}