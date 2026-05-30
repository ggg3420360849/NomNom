package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/common")
public class FileUploadController {

    @Value("${nomnom.upload-path:uploads}")
    private String uploadPath;

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.fail("文件不能为空");
        }

        Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        String fileName = UUID.randomUUID() + "_" + sanitizeFileName(file.getOriginalFilename());
        Path dest = uploadRoot.resolve(fileName).normalize();
        if (!dest.startsWith(uploadRoot)) {
            return ApiResponse.fail("文件名不合法");
        }

        try {
            Files.createDirectories(uploadRoot);
            file.transferTo(dest.toFile());
            return ApiResponse.ok("上传成功", "/uploads/" + fileName);
        } catch (IOException e) {
            return ApiResponse.fail("上传失败: " + e.getMessage());
        }
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "upload";
        }
        String normalized = originalFilename.replace("\\", "/");
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        String safeName = baseName.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}._-]", "_");
        if (safeName.isEmpty() || ".".equals(safeName) || "..".equals(safeName)) {
            return "upload";
        }
        return safeName.length() > 80 ? safeName.substring(0, 80) : safeName;
    }
}
