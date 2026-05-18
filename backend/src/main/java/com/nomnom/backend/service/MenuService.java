package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.DishesEntity;
import com.nomnom.backend.entity.DishesPackageEntity;
import com.nomnom.backend.entity.DishesTypeEntity;
import com.nomnom.backend.mapper.DishesMapper;
import com.nomnom.backend.mapper.DishesPackageMapper;
import com.nomnom.backend.mapper.DishesTypeMapper;
import com.nomnom.backend.model.MenuCategory;
import com.nomnom.backend.model.MenuItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final DishesMapper dishesMapper;
    private final DishesTypeMapper typeMapper;
    private final DishesPackageMapper packageMapper;

    public MenuService(DishesMapper dishesMapper,
                       DishesTypeMapper typeMapper,
                       DishesPackageMapper packageMapper) {
        this.dishesMapper = dishesMapper;
        this.typeMapper = typeMapper;
        this.packageMapper = packageMapper;
    }

    // 获取分类
    public List<MenuCategory> getCategories() {
        return typeMapper.selectList(null).stream().map(t -> {
            return new MenuCategory(
                    t.getId().toString(),
                    t.getName(),
                    null
            );
        }).collect(Collectors.toList());
    }

    public List<MenuItem> getMenuItems(String categoryCode, String keyword) {
        LambdaQueryWrapper<DishesEntity> wrapper = new LambdaQueryWrapper<>();

        // 按分类ID筛选
        if (categoryCode != null && !categoryCode.isBlank()) {
            try {
                Integer typeId = Integer.parseInt(categoryCode);
                wrapper.eq(DishesEntity::getTypeId, typeId);
            } catch (Exception ignored) {}
        }

        // 关键词搜索
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DishesEntity::getName, keyword);
        }

        // 只查上架菜品
        wrapper.eq(DishesEntity::getStatus, 1);

        List<DishesEntity> dishes = dishesMapper.selectList(wrapper);

        // 菜品最低价格映射
        Map<Long, BigDecimal> priceMap = packageMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(
                        DishesPackageEntity::getDishesId,
                        Collectors.collectingAndThen(
                                Collectors.minBy(java.util.Comparator.comparing(DishesPackageEntity::getPrice)),
                                opt -> opt.map(DishesPackageEntity::getPrice).orElse(BigDecimal.ZERO)
                        )
                ));

        return dishes.stream().map(d -> {
            MenuItem item = new MenuItem();
            item.setId(d.getId().longValue());
            item.setName(d.getName());
            item.setDescription(d.getDetail());
            item.setCoverUrl(d.getCoverUrl());
            double price = priceMap.getOrDefault(d.getId().longValue(), BigDecimal.ZERO).doubleValue();
            item.setPrice(price);
            item.setStatus("AVAILABLE");
            return item;
        }).collect(Collectors.toList());
    }
}