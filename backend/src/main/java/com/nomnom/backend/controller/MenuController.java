package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.model.MenuCategory;
import com.nomnom.backend.model.MenuItem;
import com.nomnom.backend.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<MenuCategory>> getCategories() {
        return ApiResponse.ok(menuService.getCategories());
    }

    @GetMapping("/items")
    public ApiResponse<List<MenuItem>> getItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(menuService.getMenuItems(category, keyword));
    }

    @GetMapping("/recommend/{packageId}")
    public ApiResponse<List<MenuItem>> getRecommendItems(@PathVariable Long packageId) {
        return ApiResponse.ok(menuService.getRecommendItems(packageId));
    }
}
