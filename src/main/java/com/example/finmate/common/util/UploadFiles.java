package com.example.finmate.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileCopyUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;

@Slf4j
public class UploadFiles {

    // 이미지 파일 다운로드 (프로필 이미지 등)
    public static void downloadImage(HttpServletResponse response, File file) {
        try {
            if (!file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "image/png";  // 기본값
            }

            response.setContentType(contentType);
            response.setContentLength((int) file.length());

            // 캐시 헤더 설정
            response.setHeader("Cache-Control", "max-age=3600");

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {

                FileCopyUtils.copy(fis, os);
                os.flush();
            }

        } catch (IOException e) {
            log.error("파일 다운로드 실패: {}", file.getPath(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // 일반 파일 다운로드
    public static void downloadFile(HttpServletResponse response, File file, String originalFileName) {
        try {
            if (!file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setContentType("application/octet-stream");
            response.setContentLength((int) file.length());

            String encodedFileName = new String(originalFileName.getBytes("UTF-8"), "ISO-8859-1");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {

                FileCopyUtils.copy(fis, os);
                os.flush();
            }

        } catch (IOException e) {
            log.error("파일 다운로드 실패: {}", file.getPath(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // 디렉토리 생성
    public static boolean makeDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }

    // 파일 확장자 검증
    public static boolean isValidImageExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return extension.matches("^(jpg|jpeg|png|gif|bmp)$");
    }

    // 파일 크기 검증 (바이트 단위)
    public static boolean isValidFileSize(long fileSize, long maxSize) {
        return fileSize > 0 && fileSize <= maxSize;
    }
}