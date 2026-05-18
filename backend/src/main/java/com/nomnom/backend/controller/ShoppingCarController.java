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
        Long dishesId = Long.valueOf(body.get("dishes_id").toString());
        Long packageId = Long.valueOf(body.get("package_id").toString());
        Integer buyNumber = Integer.valueOf(body.getOrDefault("buy_number", 1).toString());
        shoppingCarService.addItem(userId, dishesId, packageId, buyNumber);
        return ApiResponse.ok("已加入购物车", null);
    }

    @PutMapping("/{id}/quantity")
    public ApiResponse<Void> updateQuantity(@PathVariable Long id, @RequestParam Integer buyNumber) {
        shoppingCarService.updateQuantity(id, buyNumber);
        return ApiResponse.ok("更新成功", null);
    }

    @PutMapping("/{id}/selected")
    public ApiResponse<Void> toggleSelected(@PathVariable Long id) {
        shoppingCarService.toggleSelected(id);
        return ApiResponse.ok("ok", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        shoppingCarService.removeItem(id);
        return ApiResponse.ok("已移除", null);
    }

    @DeleteMapping
    public ApiResponse<Void> clear(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        shoppingCarService.clearCart(userId);
        return ApiResponse.ok("已清空", null);
    }
}
