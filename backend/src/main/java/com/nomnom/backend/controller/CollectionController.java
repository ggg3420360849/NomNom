package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.model.CollectionItemResponse;
import com.nomnom.backend.service.CollectionService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    public ApiResponse<List<CollectionItemResponse>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(collectionService.getUserCollections(userId));
    }

    @GetMapping("/check")
    public ApiResponse<Boolean> check(@RequestParam Long dishesId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(collectionService.isCollected(userId, dishesId));
    }

    @PostMapping("/toggle")
    public ApiResponse<Boolean> toggle(@RequestParam Long dishesId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean isFav = collectionService.toggle(userId, dishesId);
        return ApiResponse.ok(isFav ? "已收藏" : "已取消收藏", isFav);
    }
}
