package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.model.CartItemResponse;
import com.nomnom.backend.service.ShoppingCarService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class ShoppingCarController {

    private final ShoppingCarService shoppingCarService;

    public ShoppingCarController(ShoppingCarService shoppingCarService) {
        this.shoppingCarService = shoppingCarService;
    }

    @GetMapping
    public ApiResponse<List<CartItemResponse>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(shoppingCarService.getCartItems(userId));
    }

    @PostMapping
    public ApiResponse<Void> add(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long dishesId = parseRequiredLong(body, "dishes_id", "菜品不能为空");
        Long packageId = parseRequiredLong(body, "package_id", "菜品套餐不能为空");
        Integer buyNumber = parseOptionalInteger(body, "buy_number", 1, "购买数量不合法");
        shoppingCarService.addItem(userId, dishesId, packageId, buyNumber);
        return ApiResponse.ok("已加入购物车", null);
    }

    @PutMapping("/{id}/quantity")
    public ApiResponse<Void> updateQuantity(@PathVariable Long id,
                                            @RequestParam Integer buyNumber,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        shoppingCarService.updateQuantity(userId, id, buyNumber);
        return ApiResponse.ok("更新成功", null);
    }

    @PutMapping("/{id}/selected")
    public ApiResponse<Void> toggleSelected(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        shoppingCarService.toggleSelected(userId, id);
        return ApiResponse.ok("ok", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        shoppingCarService.removeItem(userId, id);
        return ApiResponse.ok("已移除", null);
    }

    @DeleteMapping
    public ApiResponse<Void> clear(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        shoppingCarService.clearCart(userId);
        return ApiResponse.ok("已清空", null);
    }

    private Long parseRequiredLong(Map<String, Object> body, String key, String message) {
        Object raw = body != null ? body.get(key) : null;
        if (raw == null || raw.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        try {
            return Long.valueOf(raw.toString().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private Integer parseOptionalInteger(Map<String, Object> body, String key, Integer fallback, String message) {
        Object raw = body != null ? body.get(key) : null;
        if (raw == null || raw.toString().trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.valueOf(raw.toString().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(message);
        }
    }
}
