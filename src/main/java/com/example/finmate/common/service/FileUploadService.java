package com.example.finmate.common.service;

import com.example.finmate.common.util.StringUtils;
import com.example.finmate.common.util.UploadFiles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class FileUploadService {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.avatar.path}")
    private String avatarPath;

    @Value("${file.upload.max.size}")
    private long maxFileSize;

    @Value("${file.upload.max.size.per.file}")
    private long maxFileSizePerFile;

    // 허용되는 이미지 확장자
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    // 허용되는 문서 확장자
    private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"
    );

    // 프로필 이미지 업로드
    public String uploadProfileImage(String userId, MultipartFile file) throws IOException {
        validateImageFile(file);

        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String userDir = avatarPath + userId + "/";

        // 디렉토리 생성
        UploadFiles.makeDirectory(userDir);

        // 기존 프로필 이미지 삭제
        deleteExistingProfileImages(userDir);

        // 새 파일 저장
        Path filePath = Paths.get(userDir + fileName);
        Files.write(filePath, file.getBytes());

        log.info("프로필 이미지 업로드 완료: {} -> {}", userId, fileName);

        return fileName;
    }

    // 일반 파일 업로드
    public String uploadFile(String userId, MultipartFile file, String category) throws IOException {
        validateFile(file);

        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fullPath = uploadPath + category + "/" + dateDir + "/";

        // 디렉토리 생성
        UploadFiles.makeDirectory(fullPath);

        // 파일 저장
        Path filePath = Paths.get(fullPath + fileName);
        Files.write(filePath, file.getBytes());

        log.info("파일 업로드 완료: {} -> {}", userId, fileName);

        return category + "/" + dateDir + "/" + fileName;
    }

    // 파일 삭제
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(uploadPath + filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("파일 삭제 완료: {}", filePath);
                return true;
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", filePath, e);
        }
        return false;
    }

    // 이미지 파일 유효성 검사
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        if (file.getSize() > maxFileSizePerFile) {
            throw new IllegalArgumentException("파일 크기가 너무 큽니다. 최대 " + (maxFileSizePerFile / 1024 / 1024) + "MB");
        }

        String fileName = file.getOriginalFilename();
        if (!UploadFiles.isValidImageExtension(fileName)) {
            throw new IllegalArgumentException("지원되지 않는 이미지 형식입니다.");
        }
    }

    // 일반 파일 유효성 검사
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        if (file.getSize() > maxFileSizePerFile) {
            throw new IllegalArgumentException("파일 크기가 너무 큽니다. 최대 " + (maxFileSizePerFile / 1024 / 1024) + "MB");
        }

        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName);

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase()) &&
                !ALLOWED_DOCUMENT_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("지원되지 않는 파일 형식입니다.");
        }
    }

    // 고유한 파일명 생성
    private String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String baseName = getBaseName(originalFileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomString = StringUtils.generateRandomString(6);

        return baseName + "_" + timestamp + "_" + randomString + "." + extension;
    }

    // 파일 확장자 추출
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    // 파일명에서 확장자 제거
    private String getBaseName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    // 기존 프로필 이미지 삭제
    private void deleteExistingProfileImages(String userDir) {
        File dir = new File(userDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        }
    }
}