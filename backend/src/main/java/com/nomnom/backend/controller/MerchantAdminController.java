package com.nomnom.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.DishesEntity;
import com.nomnom.backend.entity.DishesPackageEntity;
import com.nomnom.backend.entity.DishesTableEntity;
import com.nomnom.backend.entity.DishesTypeEntity;
import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.entity.OrdersItemEntity;
import com.nomnom.backend.mapper.DishesMapper;
import com.nomnom.backend.mapper.DishesPackageMapper;
import com.nomnom.backend.mapper.DishesTableMapper;
import com.nomnom.backend.mapper.DishesTypeMapper;
import com.nomnom.backend.mapper.OrdersItemMapper;
import com.nomnom.backend.mapper.OrdersMapper;
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
    private final ObjectMapper objectMapper;

    public MerchantAdminController(DishesMapper dishesMapper, DishesTableMapper tableMapper,
                                   DishesPackageMapper packageMapper, DishesTypeMapper typeMapper,
                                   OrdersMapper ordersMapper, OrdersItemMapper ordersItemMapper,
                                   ObjectMapper objectMapper) {
        this.dishesMapper = dishesMapper;
        this.tableMapper = tableMapper;
        this.packageMapper = packageMapper;
        this.typeMapper = typeMapper;
        this.ordersMapper = ordersMapper;
        this.ordersItemMapper = ordersItemMapper;
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
        typeMapper.insert(category);
        return ApiResponse.ok("新增成功", null);
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Void> updateCategory(@PathVariable Integer id, @RequestBody DishesTypeEntity category) {
        DishesTypeEntity existing = typeMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("分类不存在");
        }
        existing.setName(category.getName());
        existing.setIconUri(category.getIconUri());
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

    // ==================== 内部方法 ====================

    private void applyRequest(DishesEntity dish, AddDishRequest request) {
        dish.setTypeId(request.getTypeId());
        dish.setName(request.getName());
        dish.setDetail(request.getDetail());
        dish.setCoverUrl(request.getCoverUrl());
        dish.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        dish.setStock(request.getStock() != null ? request.getStock() : 999);
        dish.setMemberPrice(request.getMemberPrice());
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
        d.setCreateTime(dish.getCreateTime());
        d.setPackages(packages);
        return d;
    }
}
