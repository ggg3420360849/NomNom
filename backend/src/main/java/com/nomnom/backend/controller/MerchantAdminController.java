package com.nomnom.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.DishesEntity;
import com.nomnom.backend.entity.DishesEvaluationsEntity;
import com.nomnom.backend.entity.DishesPackageEntity;
import com.nomnom.backend.entity.DishesTableEntity;
import com.nomnom.backend.entity.DishesTypeEntity;
import com.nomnom.backend.entity.ImagesEntity;
import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.entity.OrdersItemEntity;
import com.nomnom.backend.entity.OrdersRefundReplyEntity;
import com.nomnom.backend.entity.UserEntity;
import com.nomnom.backend.mapper.DishesEvaluationsMapper;
import com.nomnom.backend.mapper.DishesMapper;
import com.nomnom.backend.mapper.DishesPackageMapper;
import com.nomnom.backend.mapper.DishesTableMapper;
import com.nomnom.backend.mapper.DishesTypeMapper;
import com.nomnom.backend.mapper.ImagesMapper;
import com.nomnom.backend.mapper.OrdersItemMapper;
import com.nomnom.backend.mapper.OrdersMapper;
import com.nomnom.backend.mapper.OrdersRefundReplyMapper;
import com.nomnom.backend.mapper.UserMapper;
import com.nomnom.backend.model.AddDishRequest;
import com.nomnom.backend.model.DishDetailResponse;
import com.nomnom.backend.model.DashboardResponse;
import com.nomnom.backend.model.DishDetailResponse;
import com.nomnom.backend.model.DishListResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class MerchantAdminController {

    private final DishesMapper dishesMapper;
    private final DishesTableMapper tableMapper;
    private final DishesPackageMapper packageMapper;
    private final DishesTypeMapper typeMapper;
    private final OrdersMapper ordersMapper;
    private final OrdersItemMapper ordersItemMapper;
    private final OrdersRefundReplyMapper refundMapper;
    private final DishesEvaluationsMapper evaluationsMapper;
    private final UserMapper userMapper;
    private final ImagesMapper imagesMapper;
    private final ObjectMapper objectMapper;

    public MerchantAdminController(DishesMapper dishesMapper, DishesTableMapper tableMapper,
                                   DishesPackageMapper packageMapper, DishesTypeMapper typeMapper,
                                   OrdersMapper ordersMapper, OrdersItemMapper ordersItemMapper,
                                   OrdersRefundReplyMapper refundMapper,
                                   DishesEvaluationsMapper evaluationsMapper,
                                   UserMapper userMapper, ImagesMapper imagesMapper,
                                   ObjectMapper objectMapper) {
        this.dishesMapper = dishesMapper;
        this.tableMapper = tableMapper;
        this.packageMapper = packageMapper;
        this.typeMapper = typeMapper;
        this.ordersMapper = ordersMapper;
        this.ordersItemMapper = ordersItemMapper;
        this.refundMapper = refundMapper;
        this.evaluationsMapper = evaluationsMapper;
        this.userMapper = userMapper;
        this.imagesMapper = imagesMapper;
        this.objectMapper = objectMapper;
    }

    // ==================== 菜品管理 ====================

    @GetMapping("/dishes")
    public ApiResponse<List<DishListResponse>> listDishes() {
        List<DishesEntity> dishes = dishesMapper.selectList(
                new LambdaQueryWrapper<DishesEntity>()
                        .orderByDesc(DishesEntity::getCreateTime));

        Map<Integer, String> typeMap = typeMapper.selectList(null).stream()
                .collect(Collectors.toMap(DishesTypeEntity::getId, t -> t.getName() != null ? t.getName() : ""));

        List<Long> dishIds = dishes.stream().map(d -> d.getId().longValue()).collect(Collectors.toList());
        Map<Long, BigDecimal> priceMap = dishIds.isEmpty() ? Map.of() :
                packageMapper.selectList(new LambdaQueryWrapper<DishesPackageEntity>()
                        .in(DishesPackageEntity::getDishesId, dishIds))
                        .stream()
                        .collect(Collectors.groupingBy(DishesPackageEntity::getDishesId,
                                Collectors.collectingAndThen(
                                        Collectors.minBy(java.util.Comparator.comparing(DishesPackageEntity::getPrice)),
                                        opt -> opt.map(DishesPackageEntity::getPrice).orElse(BigDecimal.ZERO))));

        List<DishListResponse> result = dishes.stream().map(d -> {
            DishListResponse r = new DishListResponse();
            r.setId(d.getId());
            r.setTypeId(d.getTypeId());
            r.setTypeName(d.getTypeId() != null ? typeMap.getOrDefault(d.getTypeId(), "") : "");
            r.setName(d.getName());
            r.setDetail(d.getDetail());
            r.setCoverUrl(d.getCoverUrl());
            r.setStatus(d.getStatus());
            r.setStock(d.getStock());
            r.setPrice(priceMap.getOrDefault(d.getId().longValue(), BigDecimal.ZERO));
            r.setMemberPrice(d.getMemberPrice());
            r.setSpecs(d.getSpecs());
            r.setIsRecommend(d.getIsRecommend());
            r.setRecommendIds(d.getRecommendIds());
            r.setCreateTime(d.getCreateTime());
            return r;
        }).collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    @GetMapping("/dishes/{id}")
    public ApiResponse<DishDetailResponse> getDish(@PathVariable Integer id) {
        DishesEntity dish = dishesMapper.selectById(id);
        if (dish == null) {
            return ApiResponse.fail("菜品不存在");
        }
        List<DishesPackageEntity> packages = packageMapper.selectList(
                new LambdaQueryWrapper<DishesPackageEntity>()
                        .eq(DishesPackageEntity::getDishesId, id.longValue()));
        return ApiResponse.ok(toDetail(dish, packages));
    }

    @PostMapping("/dishes/add")
    public ApiResponse<Void> addDish(@RequestBody AddDishRequest request) {
        DishesEntity dish = new DishesEntity();
        applyRequest(dish, request);
        dish.setCreateTime(new Date());
        dishesMapper.insert(dish);

        DishesPackageEntity pkg = new DishesPackageEntity();
        pkg.setDishesId(dish.getId().longValue());
        pkg.setName("标准份");
        pkg.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
        pkg.setCreateTime(LocalDateTime.now());
        packageMapper.insert(pkg);

        return ApiResponse.ok("新增成功", null);
    }

    @PutMapping("/dishes/{id}")
    public ApiResponse<Void> updateDish(@PathVariable Integer id, @RequestBody AddDishRequest request) {
        DishesEntity dish = dishesMapper.selectById(id);
        if (dish == null) {
            return ApiResponse.fail("菜品不存在");
        }
        applyRequest(dish, request);
        dishesMapper.updateById(dish);

        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            List<DishesPackageEntity> packages = packageMapper.selectList(
                    new LambdaQueryWrapper<DishesPackageEntity>()
                            .eq(DishesPackageEntity::getDishesId, id.longValue())
                            .orderByAsc(DishesPackageEntity::getPrice)
                            .last("LIMIT 1"));
            if (!packages.isEmpty()) {
                DishesPackageEntity pkg = packages.get(0);
                pkg.setPrice(request.getPrice());
                packageMapper.updateById(pkg);
            }
        }
        return ApiResponse.ok("更新成功", null);
    }

    @PutMapping("/dishes/{id}/status")
    public ApiResponse<Void> toggleDishStatus(@PathVariable Integer id, @RequestParam Integer status) {
        DishesEntity dish = dishesMapper.selectById(id);
        if (dish == null) {
            return ApiResponse.fail("菜品不存在");
        }
        dish.setStatus(status);
        dishesMapper.updateById(dish);
        return ApiResponse.ok(status == 1 ? "已上架" : "已下架", null);
    }

    @Transactional
    @DeleteMapping("/dishes/{id}")
    public ApiResponse<Void> deleteDish(@PathVariable Integer id) {
        DishesEntity dish = dishesMapper.selectById(id);
        if (dish == null) {
            return ApiResponse.fail("菜品不存在");
        }
        packageMapper.delete(new LambdaQueryWrapper<DishesPackageEntity>()
                .eq(DishesPackageEntity::getDishesId, id.longValue()));
        dishesMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ==================== 分类管理 ====================

    @GetMapping("/categories")
    public ApiResponse<List<DishesTypeEntity>> listCategories(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<DishesTypeEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(DishesTypeEntity::getName, keyword.trim());
        }
        wrapper.orderByAsc(DishesTypeEntity::getId);
        return ApiResponse.ok(typeMapper.selectList(wrapper));
    }

    @PostMapping("/categories")
    public ApiResponse<Void> addCategory(@RequestBody DishesTypeEntity category) {
        String name = normalizeRequiredText(category != null ? category.getName() : null);
        if (name.isEmpty()) {
            return ApiResponse.fail("分类名称不能为空");
        }
        if (category == null) {
            category = new DishesTypeEntity();
        }
        category.setName(name);
        category.setIconUri(normalizeOptionalText(category.getIconUri()));
        typeMapper.insert(category);
        return ApiResponse.ok("新增成功", null);
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Void> updateCategory(@PathVariable Integer id, @RequestBody DishesTypeEntity category) {
        DishesTypeEntity existing = typeMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("分类不存在");
        }
        String name = normalizeRequiredText(category != null ? category.getName() : null);
        if (name.isEmpty()) {
            return ApiResponse.fail("分类名称不能为空");
        }
        existing.setName(name);
        existing.setIconUri(normalizeOptionalText(category.getIconUri()));
        typeMapper.updateById(existing);
        return ApiResponse.ok("更新成功", null);
    }

    @Transactional
    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Integer id) {
        if (typeMapper.selectById(id) == null) {
            return ApiResponse.fail("分类不存在");
        }
        typeMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ==================== 仪表盘 ====================

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        List<OrdersEntity> allOrders = ordersMapper.selectList(null);
        List<OrdersItemEntity> allItems = ordersItemMapper.selectList(null);

        DashboardResponse d = new DashboardResponse();
        d.setTotalOrders(allOrders.size());

        java.time.LocalDate today = java.time.LocalDate.now();
        java.math.BigDecimal todayRevenue = java.math.BigDecimal.ZERO;
        int todayCount = 0;
        int p1 = 0, p2 = 0, p3 = 0, p4 = 0;
        for (OrdersEntity o : allOrders) {
            if (o.getStatus() != null) {
                switch (o.getStatus()) {
                    case 1: p1++; break;
                    case 2: p2++; break;
                    case 3: p3++; break;
                    case 4: p4++; break;
                }
            }
            if (o.getCreateTime() != null && o.getCreateTime().toLocalDate().equals(today)) {
                todayCount++;
                todayRevenue = todayRevenue.add(o.getTotalPrice() != null ? o.getTotalPrice() : java.math.BigDecimal.ZERO);
            }
        }
        d.setPendingCount(p1);
        d.setPaidCount(p2);
        d.setPreparingCount(p3);
        d.setCompletedCount(p4);
        d.setTodayOrderCount(todayCount);
        d.setTodayRevenue(todayRevenue);

        java.util.Map<String, Integer> dishSales = new java.util.LinkedHashMap<>();
        for (OrdersItemEntity item : allItems) {
            String name = item.getSnapName() != null ? item.getSnapName() : "未知";
            dishSales.merge(name.split(" - ")[0], item.getBuyNumber() != null ? item.getBuyNumber() : 0, Integer::sum);
        }
        d.setTopDishes(dishSales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> { java.util.Map<String, Object> m = new java.util.LinkedHashMap<>(); m.put("name", e.getKey()); m.put("sales", e.getValue()); return m; })
                .collect(java.util.stream.Collectors.toList()));

        return ApiResponse.ok(d);
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    public ApiResponse<List<OrdersEntity>> listOrders(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<OrdersEntity> wrapper = new LambdaQueryWrapper<OrdersEntity>()
                .orderByDesc(OrdersEntity::getCreateTime);
        if (status != null) {
            wrapper.eq(OrdersEntity::getStatus, status);
        }
        return ApiResponse.ok(ordersMapper.selectList(wrapper));
    }

    @PutMapping("/orders/{id}/status")
    public ApiResponse<Void> updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        OrdersEntity order = ordersMapper.selectById(id);
        if (order == null) {
            return ApiResponse.fail("订单不存在");
        }
        order.setStatus(status);
        ordersMapper.updateById(order);
        return ApiResponse.ok("状态更新成功", null);
    }

    // ==================== 桌台管理 ====================

    @GetMapping("/tables")
    public ApiResponse<List<DishesTableEntity>> getTables() {
        List<DishesTableEntity> tables = tableMapper.selectList(
                new LambdaQueryWrapper<DishesTableEntity>()
                        .orderByAsc(DishesTableEntity::getArea)
                        .orderByAsc(DishesTableEntity::getNumber));
        return ApiResponse.ok("获取桌台成功", tables);
    }

    @PutMapping("/tables/status")
    public ApiResponse<Void> updateTableStatus(@RequestParam Integer tableId, @RequestParam Integer status) {
        DishesTableEntity table = tableMapper.selectById(tableId);
        if (table == null) {
            return ApiResponse.fail("桌台不存在");
        }
        table.setStatus(status);
        if (status == 0) {
            table.setAmount(BigDecimal.ZERO);
        }
        tableMapper.updateById(table);
        return ApiResponse.ok("状态更新成功", null);
    }

    @PostMapping("/tables")
    public ApiResponse<Void> addTable(@RequestBody DishesTableEntity table) {
        tableMapper.insert(table);
        return ApiResponse.ok("新增成功", null);
    }

    @PutMapping("/tables/{id}")
    public ApiResponse<Void> updateTable(@PathVariable Long id, @RequestBody DishesTableEntity request) {
        DishesTableEntity table = tableMapper.selectById(id);
        if (table == null) {
            return ApiResponse.fail("桌台不存在");
        }
        table.setNumber(request.getNumber());
        table.setPersonNumber(request.getPersonNumber());
        table.setArea(request.getArea());
        table.setMinSpend(request.getMinSpend());
        tableMapper.updateById(table);
        return ApiResponse.ok("更新成功", null);
    }

    @DeleteMapping("/tables/{id}")
    public ApiResponse<Void> deleteTable(@PathVariable Long id) {
        if (tableMapper.selectById(id) == null) {
            return ApiResponse.fail("桌台不存在");
        }
        tableMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ==================== 审核管理 ====================

    @GetMapping("/audits")
    public ApiResponse<List<Map<String, Object>>> listAudits() {
        List<OrdersRefundReplyEntity> refunds = refundMapper.selectList(
                new LambdaQueryWrapper<OrdersRefundReplyEntity>()
                        .eq(OrdersRefundReplyEntity::getStatus, 2)
                        .orderByDesc(OrdersRefundReplyEntity::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (OrdersRefundReplyEntity r : refunds) {
            OrdersEntity order = ordersMapper.selectById(r.getOrdersId());
            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("id", r.getId());
            audit.put("type", "REFUND");
            audit.put("desc", "订单#" + (order != null ? order.getCode() : r.getOrdersId())
                    + " " + (r.getRefundCause() != null ? r.getRefundCause() : ""));
            audit.put("status", "PENDING");
            result.add(audit);
        }
        return ApiResponse.ok(result);
    }

    @PutMapping("/audit/{id}")
    public ApiResponse<Void> auditAction(@PathVariable Long id, @RequestParam Boolean approved) {
        OrdersRefundReplyEntity entity = refundMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.fail("审核项不存在");
        }
        if (approved) {
            entity.setStatus(3);
            entity.setRefunTime(LocalDateTime.now());
            refundMapper.updateById(entity);
            return ApiResponse.ok("审批已通过", null);
        } else {
            entity.setStatus(4);
            entity.setRefunTime(LocalDateTime.now());
            refundMapper.updateById(entity);
            return ApiResponse.ok("申请已驳回", null);
        }
    }

    // ==================== 财务对账 ====================

    @GetMapping("/finance/reconciliation")
    public ApiResponse<List<Map<String, Object>>> reconciliation() {
        List<OrdersEntity> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<OrdersEntity>()
                        .orderByDesc(OrdersEntity::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (OrdersEntity o : orders) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("id", o.getId());
            rec.put("orderAmt", o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO);
            rec.put("flowAmt", o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO);
            result.add(rec);
        }
        return ApiResponse.ok(result);
    }

    // ==================== 评价管理 ====================

    @GetMapping("/reviews")
    public ApiResponse<List<Map<String, Object>>> listReviews() {
        List<DishesEvaluationsEntity> evaluations = evaluationsMapper.selectList(
                new LambdaQueryWrapper<DishesEvaluationsEntity>()
                        .orderByDesc(DishesEvaluationsEntity::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (DishesEvaluationsEntity ev : evaluations) {
            UserEntity user = userMapper.selectById(ev.getUserId());
            DishesEntity dish = dishesMapper.selectById(ev.getDishesId());
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", ev.getId());
            r.put("user", user != null ? (user.getUsername() != null ? user.getUsername() : user.getAccount()) : "未知用户");
            r.put("dishName", dish != null ? dish.getName() : "未知菜品");
            r.put("rating", ev.getRatingValue());
            r.put("content", ev.getContent());
            r.put("reply", ev.getReplyContent());
            result.add(r);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/reviews/{id}/reply")
    public ApiResponse<Void> replyToReview(@PathVariable Long id, @RequestBody Map<String, String> body) {
        DishesEvaluationsEntity entity = evaluationsMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.fail("评价不存在");
        }
        String replyContent = body.get("reply");
        if (replyContent == null || replyContent.trim().isEmpty()) {
            return ApiResponse.fail("回复内容不能为空");
        }
        entity.setReplyContent(replyContent.trim());
        entity.setReplyTime(LocalDateTime.now());
        entity.setReplyStatus(1);
        evaluationsMapper.updateById(entity);
        return ApiResponse.ok("回复成功", null);
    }

    @Transactional
    @DeleteMapping("/reviews/{id}")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        if (evaluationsMapper.selectById(id) == null) {
            return ApiResponse.fail("评价不存在");
        }
        imagesMapper.delete(new LambdaQueryWrapper<ImagesEntity>()
                .eq(ImagesEntity::getDishesEvaluationsId, id.intValue()));
        evaluationsMapper.deleteById(id);
        return ApiResponse.ok("评价已删除", null);
    }

    // ==================== 统计分析 ====================

    @GetMapping("/stats")
    public ApiResponse<List<Map<String, Object>>> stats() {
        List<OrdersItemEntity> allItems = ordersItemMapper.selectList(null);

        Map<String, Map<String, Object>> dishStats = new LinkedHashMap<>();
        for (OrdersItemEntity item : allItems) {
            String name = item.getSnapName() != null ? item.getSnapName().split(" - ")[0] : "未知";
            Map<String, Object> entry = dishStats.computeIfAbsent(name, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", k);
                m.put("sales", 0);
                m.put("revenue", BigDecimal.ZERO);
                return m;
            });
            int qty = item.getBuyNumber() != null ? item.getBuyNumber() : 0;
            entry.put("sales", (int) entry.get("sales") + qty);
            BigDecimal itemRevenue = (item.getSnapPrice() != null ? item.getSnapPrice() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(qty));
            entry.put("revenue", ((BigDecimal) entry.get("revenue")).add(itemRevenue));
        }

        List<Map<String, Object>> result = dishStats.values().stream()
                .sorted((a, b) -> Integer.compare((int) b.get("sales"), (int) a.get("sales")))
                .collect(Collectors.toList());

        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("rank", i + 1);
        }
        return ApiResponse.ok(result);
    }

    // ==================== 内部方法 ====================

    private String normalizeRequiredText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyRequest(DishesEntity dish, AddDishRequest request) {
        dish.setTypeId(request.getTypeId());
        dish.setName(request.getName());
        dish.setDetail(request.getDetail());
        dish.setCoverUrl(request.getCoverUrl());
        dish.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        dish.setStock(request.getStock() != null ? request.getStock() : 999);
        dish.setMemberPrice(request.getMemberPrice());
        dish.setIsRecommend(request.getIsRecommend());
        dish.setRecommendIds(request.getRecommendIds());
        dish.setSpecs(buildSpecsJson(request));
    }

    private String buildSpecsJson(AddDishRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (request.getSpecs() != null && !request.getSpecs().isEmpty()) {
            map.put("sizes", request.getSpecs());
        }
        if (request.getTastes() != null && !request.getTastes().isEmpty()) {
            map.put("spiciness", request.getTastes());
        }
        try {
            return map.isEmpty() ? null : objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private DishDetailResponse toDetail(DishesEntity dish, List<DishesPackageEntity> packages) {
        DishDetailResponse d = new DishDetailResponse();
        d.setId(dish.getId());
        d.setTypeId(dish.getTypeId());
        d.setName(dish.getName());
        d.setDetail(dish.getDetail());
        d.setCoverUrl(dish.getCoverUrl());
        d.setStatus(dish.getStatus());
        d.setStock(dish.getStock());
        d.setMemberPrice(dish.getMemberPrice());
        d.setSpecs(dish.getSpecs());
        d.setIsRecommend(dish.getIsRecommend());
        d.setRecommendIds(dish.getRecommendIds());
        d.setCreateTime(dish.getCreateTime());
        d.setPackages(packages);
        return d;
    }
}
