package com.nomnom.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.CommentEntity;
import com.nomnom.backend.entity.DishesTableEntity;
import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.entity.OrdersItemEntity;
import com.nomnom.backend.mapper.CommentMapper;
import com.nomnom.backend.mapper.DishesTableMapper;
import com.nomnom.backend.mapper.OrdersItemMapper;
import com.nomnom.backend.mapper.OrdersMapper;
import com.nomnom.backend.model.EvaluationRequest;
import com.nomnom.backend.model.PackageSnapshot;
import com.nomnom.backend.service.DishesEvaluationsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final DishesTableMapper tableMapper;
    private final OrdersMapper ordersMapper;
    private final DishesEvaluationsService evaluationsService;
    private final OrdersItemMapper ordersItemMapper;

    @Resource
    private CommentMapper commentMapper;

    public CustomerController(DishesTableMapper tableMapper,
                              OrdersMapper ordersMapper,
                              DishesEvaluationsService evaluationsService,
                              OrdersItemMapper ordersItemMapper) {
        this.tableMapper = tableMapper;
        this.ordersMapper = ordersMapper;
        this.evaluationsService = evaluationsService;
        this.ordersItemMapper = ordersItemMapper;
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

    // 顾客下单（返回订单ID）
    @CrossOrigin
    @Transactional
    @PostMapping("/orders")
    public ApiResponse<?> createOrder(@RequestBody CustomerOrderRequest request) {
        System.out.println("=== 收到下单请求 ===");
        System.out.println("tableId: " + request.getTableId());
        System.out.println("totalPrice: " + request.getTotalPrice());
        System.out.println("items数量: " + (request.getItems() == null ? 0 : request.getItems().size()));

        DishesTableEntity table = tableMapper.selectById(request.getTableId());
        if (table == null) {
            return ApiResponse.fail("桌台不存在");
        }

        table.setStatus(1);
        tableMapper.updateById(table);

        OrdersEntity order = new OrdersEntity();
        order.setCode(UUID.randomUUID().toString());
        order.setUserId(1L);
        order.setStatus(1);
        order.setTotalPrice(request.getTotalPrice());
        order.setCreateTime(LocalDateTime.now());
        ordersMapper.insert(order);

        Long orderId = order.getId();
        System.out.println("订单创建成功，orderId: " + orderId);

        // ========== 保存订单明细 ==========
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CustomerOrderItem item : request.getItems()) {
                OrdersItemEntity orderItem = new OrdersItemEntity();
                orderItem.setOrdersId(orderId);  // Long 直接赋值
                orderItem.setDishesPackageId(item.getPackageId());
                orderItem.setBuyNumber(item.getQuantity());
                orderItem.setSnapPrice(item.getPrice());
                orderItem.setSnapName(item.getName());
                orderItem.setSnapCover(item.getCoverUrl());
                orderItem.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                orderItem.setCreateTime(LocalDateTime.now());
                ordersItemMapper.insert(orderItem);
                System.out.println("订单明细保存成功: " + item.getName() + " x" + item.getQuantity());
            }
        } else {
            System.out.println("警告：订单没有明细商品！");
        }
        // =================================

        return ApiResponse.ok(orderId);
    }

    // 下单请求类
    public static class CustomerOrderRequest {
        private Integer tableId;
        private BigDecimal totalPrice;
        private List<CustomerOrderItem> items;

        public Integer getTableId() { return tableId; }
        public void setTableId(Integer tableId) { this.tableId = tableId; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
        public List<CustomerOrderItem> getItems() { return items; }
        public void setItems(List<CustomerOrderItem> items) { this.items = items; }
    }

    // 订单商品项请求类
    public static class CustomerOrderItem {
        private Long packageId;      // 套餐ID (dishes_package_id)
        private String name;         // 商品名称
        private BigDecimal price;    // 单价
        private Integer quantity;    // 数量
        private String coverUrl;     // 图片URL

        public Long getPackageId() { return packageId; }
        public void setPackageId(Long packageId) { this.packageId = packageId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    }

    // ======================
    // 获取订单中的套餐列表（用于评价弹窗）
    // ======================
    @CrossOrigin
    @GetMapping("/orders/{orderId}/packages")
    public ApiResponse<?> getOrderPackagesForEvaluation(@PathVariable Long orderId) {
        System.out.println("=== 获取订单套餐列表，orderId: " + orderId);
        try {
            List<PackageSnapshot> packages = ordersMapper.getOrderPackagesForEvaluation(orderId);
            System.out.println("查询到套餐数量: " + packages.size());
            return ApiResponse.ok(packages);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail("获取失败：" + e.getMessage());
        }
    }

    // ======================
    // 批量提交评价（对每个菜品分别评价）
    // ======================
    @CrossOrigin
    @PostMapping("/comments/batch")
    public ApiResponse<?> submitBatchComments(@RequestBody List<EvaluationRequest> requests, HttpServletRequest httpRequest) {
        Long userId = 1L;
        Object userIdAttr = httpRequest.getAttribute("userId");
        if (userIdAttr != null) {
            userId = (Long) userIdAttr;
        }

        System.out.println("=== 批量提交评价，数量: " + requests.size());

        try {
            for (EvaluationRequest request : requests) {
                evaluationsService.createEvaluation(userId, request);
            }
            System.out.println("批量评价成功！");
            return ApiResponse.ok("评价成功");
        } catch (Exception e) {
            System.err.println("批量评价失败：" + e.getMessage());
            e.printStackTrace();
            return ApiResponse.fail("评价失败：" + e.getMessage());
        }
    }

    // ======================
    // 单个评价接口（保留兼容）
    // ======================
    @CrossOrigin
    @PostMapping("/comment")
    public ApiResponse<?> submitComment(@RequestBody EvaluationRequest request, HttpServletRequest httpRequest) {
        Long userId = 1L;
        Object userIdAttr = httpRequest.getAttribute("userId");
        if (userIdAttr != null) {
            userId = (Long) userIdAttr;
        }

        System.out.println("=== 收到评价请求 ===");
        System.out.println("ordersId: " + request.getOrdersId());
        System.out.println("dishesId: " + request.getDishesId());
        System.out.println("ratingValue: " + request.getRatingValue());
        System.out.println("content: " + request.getContent());

        try {
            evaluationsService.createEvaluation(userId, request);
            System.out.println("评价保存成功！");
            return ApiResponse.ok("评价成功");
        } catch (Exception e) {
            System.err.println("评价接口执行失败！");
            e.printStackTrace();
            return ApiResponse.fail("评价失败：" + e.getMessage());
        }
    }
}