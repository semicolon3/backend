package com.legalai.service;

import com.legalai.exception.CustomException;
import com.legalai.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리 생성 실패", e);
        }
    }

    public String store(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID() + extension;
        Path targetPath = Paths.get(uploadDir).resolve(storedFileName);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return storedFileName;
    }

    public byte[] load(String storedFileName) {
        Path filePath = Paths.get(uploadDir).resolve(storedFileName);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    public void delete(String storedFileName) {
        Path filePath = Paths.get(uploadDir).resolve(storedFileName);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    public String getFilePath(String storedFileName) {
        return Paths.get(uploadDir).resolve(storedFileName).toAbsolutePath().toString();
    }
}
