package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/common")
public class FileUploadController {

    private static final String UPLOAD_PATH = "D:/CODE/projects/ordersystem/backend/uploads/";

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.fail("文件不能为空"); // 改为了你的 fail 方法
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = new File(UPLOAD_PATH + fileName);

        try {
            if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
            file.transferTo(dest);
            // 改为了你的 ok 方法
            return ApiResponse.ok("上传成功", "/uploads/" + fileName);
        } catch (IOException e) {
            return ApiResponse.fail("上传失败: " + e.getMessage());
        }
    }
}