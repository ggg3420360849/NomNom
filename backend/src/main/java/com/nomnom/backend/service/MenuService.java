package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nomnom.backend.entity.DishesEntity;
import com.nomnom.backend.entity.DishesPackageEntity;
import com.nomnom.backend.entity.DishesTypeEntity;
import com.nomnom.backend.mapper.DishesMapper;
import com.nomnom.backend.mapper.DishesPackageMapper;
import com.nomnom.backend.mapper.DishesTypeMapper;
import com.nomnom.backend.mapper.OrdersItemMapper;
import com.nomnom.backend.model.MenuCategory;
import com.nomnom.backend.model.MenuItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final DishesMapper dishesMapper;
    private final DishesTypeMapper typeMapper;
    private final DishesPackageMapper packageMapper;
    private final OrdersItemMapper ordersItemMapper;
    private final ObjectMapper objectMapper;

    public MenuService(DishesMapper dishesMapper,
                       DishesTypeMapper typeMapper,
                       DishesPackageMapper packageMapper,
                       OrdersItemMapper ordersItemMapper,
                       ObjectMapper objectMapper) {
        this.dishesMapper = dishesMapper;
        this.typeMapper = typeMapper;
        this.packageMapper = packageMapper;
        this.ordersItemMapper = ordersItemMapper;
        this.objectMapper = objectMapper;
    }

    public List<MenuCategory> getCategories() {
        return typeMapper.selectList(null).stream()
                .map(t -> new MenuCategory(t.getId().toString(), t.getName(), null))
                .collect(Collectors.toList());
    }

    public List<MenuItem> getMenuItems(String categoryCode, String keyword) {
        LambdaQueryWrapper<DishesEntity> wrapper = new LambdaQueryWrapper<>();
        if (categoryCode != null && !categoryCode.isBlank()) {
            try {
                wrapper.eq(DishesEntity::getTypeId, Integer.parseInt(categoryCode));
            } catch (NumberFormatException ignored) {
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DishesEntity::getName, keyword)
                    .or()
                    .like(DishesEntity::getDetail, keyword));
        }
        wrapper.eq(DishesEntity::getStatus, 1).orderByDesc(DishesEntity::getCreateTime);

        Map<Integer, String> typeNames = typeMapper.selectList(null).stream()
                .collect(Collectors.toMap(DishesTypeEntity::getId, DishesTypeEntity::getName, (a, b) -> a));
        Map<Long, DishesPackageEntity> packageMap = packageMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(
                        DishesPackageEntity::getDishesId,
                        Collectors.collectingAndThen(
                                Collectors.minBy(Comparator.comparing(p -> p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO)),
                                opt -> opt.orElse(null))));
        Map<Long, Integer> salesMap = ordersItemMapper.selectList(null).stream()
                .filter(item -> item.getDishesPackageId() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getDishesPackageId(),
                        Collectors.summingInt(item -> item.getBuyNumber() != null ? item.getBuyNumber() : 0)));

        return dishesMapper.selectList(wrapper).stream().map(dish -> {
            DishesPackageEntity pkg = packageMap.get(dish.getId().longValue());
            MenuItem item = new MenuItem();
            item.setId(pkg != null ? pkg.getId() : dish.getId().longValue());
            item.setDishesId(dish.getId().longValue());
            item.setName(dish.getName());
            item.setCategory(dish.getTypeId() != null ? dish.getTypeId().toString() : "");
            item.setCategoryLabel(dish.getTypeId() != null ? typeNames.getOrDefault(dish.getTypeId(), "") : "");
            item.setDescription(dish.getDetail());
            item.setCoverUrl(dish.getCoverUrl());
            item.setPrice(pkg != null && pkg.getPrice() != null ? pkg.getPrice().doubleValue() : 0);
            item.setStatus(dish.getStock() == null || dish.getStock() > 0 ? "AVAILABLE" : "SOLD_OUT");
            item.setMonthlySales(pkg != null ? salesMap.getOrDefault(pkg.getId(), 0) : 0);
            item.setRecommend(dish.getIsRecommend() != null && dish.getIsRecommend() == 1);
            return item;
        }).collect(Collectors.toList());
    }

    public List<MenuItem> getRecommendItems(Long packageId) {
        DishesEntity dish = resolveDishFromPackage(packageId);
        if (dish == null || dish.getRecommendIds() == null || dish.getRecommendIds().isBlank()) {
            return List.of();
        }
        List<Long> recommendDishIds = parseRecommendIds(dish.getRecommendIds());
        if (recommendDishIds.isEmpty()) {
            return List.of();
        }
        Set<Long> recommendSet = Set.copyOf(recommendDishIds);
        return getMenuItems(null, null).stream()
                .filter(item -> item.getDishesId() != null && recommendSet.contains(item.getDishesId()))
                .sorted(Comparator.comparing(item -> recommendDishIds.indexOf(item.getDishesId())))
                .collect(Collectors.toList());
    }

    private DishesEntity resolveDishFromPackage(Long packageId) {
        if (packageId == null) {
            return null;
        }
        DishesPackageEntity pkg = packageMapper.selectById(packageId);
        if (pkg != null && pkg.getDishesId() != null) {
            return dishesMapper.selectById(pkg.getDishesId().intValue());
        }
        return dishesMapper.selectById(packageId.intValue());
    }

    private List<Long> parseRecommendIds(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<List<Long>>() {});
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }
}
