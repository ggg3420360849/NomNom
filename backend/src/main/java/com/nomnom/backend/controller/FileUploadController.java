package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = Paths.get(uploadPath).toAbsolutePath().resolve(fileName).toFile();

        try {
            if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
            file.transferTo(dest);
            return ApiResponse.ok("上传成功", "/uploads/" + fileName);
        } catch (IOException e) {
            return ApiResponse.fail("上传失败: " + e.getMessage());
        }
    }
}