package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.DishesEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class MerchantAdminController {

    // --- 菜品管理 (使用你提供的 ok 和 fail 方法) ---
    @PostMapping("/dishes/add")
    public ApiResponse<String> addDish(@RequestBody DishesEntity dish) {
        System.out.println("收到前端传来的菜品: " + dish.getName() + "，图片地址: " + dish.getCoverUrl());
        return ApiResponse.ok("新增成功", null);
    }

    // --- 桌台管理 ---
    @GetMapping("/tables")
    public ApiResponse<List<Map<String, Object>>> getTables() {
        // 临时返回一个空列表，保证前端接口能通，不报错
        List<Map<String, Object>> mockTables = new ArrayList<>();
        return ApiResponse.ok("获取桌台成功", mockTables);
    }

    @PutMapping("/tables/status")
    public ApiResponse<String> updateTableStatus(@RequestParam Integer tableId, @RequestParam Integer status) {
        System.out.println("收到前端请求：更新桌台ID " + tableId + " 的状态为 " + status);
        return ApiResponse.ok("状态更新成功", null);
    }
}