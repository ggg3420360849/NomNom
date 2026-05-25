package com.nomnom.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.DishesTableEntity;
import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.mapper.DishesTableMapper;
import com.nomnom.backend.mapper.OrdersMapper;
import com.nomnom.backend.model.EvaluationRequest;
import com.nomnom.backend.model.PackageSnapshot;
import com.nomnom.backend.service.DishesEvaluationsService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final DishesTableMapper tableMapper;
    private final OrdersMapper ordersMapper;
    private final DishesEvaluationsService evaluationsService;

    public CustomerController(DishesTableMapper tableMapper,
                              OrdersMapper ordersMapper,
                              DishesEvaluationsService evaluationsService) {
        this.tableMapper = tableMapper;
        this.ordersMapper = ordersMapper;
        this.evaluationsService = evaluationsService;
    }

    // 获取桌台
    @CrossOrigin
    @GetMapping("/tables")
    public ApiResponse<?> getTables() {
        return ApiResponse.ok(tableMapper.selectList(
                new LambdaQueryWrapper<DishesTableEntity>()
                        .orderByAsc(DishesTableEntity::getArea)
                        .orderByAsc(DishesTableEntity::getNumber)
        ));
    }

    // ======================
    // 获取订单中的套餐列表（用于评价弹窗）
    // ======================
    @CrossOrigin
    @GetMapping("/orders/{orderId}/packages")
    public ApiResponse<?> getOrderPackagesForEvaluation(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String roleCode = (String) request.getAttribute("roleCode");
        OrdersEntity order = ordersMapper.selectById(orderId);
        if (order == null) {
            return ApiResponse.fail("订单不存在");
        }
        if (!"MERCHANT".equals(roleCode) && (userId == null || !userId.equals(order.getUserId()))) {
            return ApiResponse.fail("无权查看该订单");
        }
        List<PackageSnapshot> packages = ordersMapper.getOrderPackagesForEvaluation(orderId);
        return ApiResponse.ok(packages);
    }

    // ======================
    // 批量提交评价（对每个菜品分别评价）
    // ======================
    @CrossOrigin
    @PostMapping("/comments/batch")
    public ApiResponse<?> submitBatchComments(@RequestBody List<EvaluationRequest> requests, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("请先登录");
        }
        if (requests == null || requests.isEmpty()) {
            return ApiResponse.fail("评价内容不能为空");
        }
        for (EvaluationRequest request : requests) {
            evaluationsService.createEvaluation(userId, request);
        }
        return ApiResponse.ok("评价成功");
    }

    // ======================
    // 单个评价接口（保留兼容）
    // ======================
    @CrossOrigin
    @PostMapping("/comment")
    public ApiResponse<?> submitComment(@RequestBody EvaluationRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("请先登录");
        }
        evaluationsService.createEvaluation(userId, request);
        return ApiResponse.ok("评价成功");
    }
}
