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

    public List<MenuCategory> getCategories() {
        return typeMapper.selectList(null).stream()
                .filter(t -> t.getId() != null)
                .map(t -> new MenuCategory(t.getId().toString(), t.getName() != null ? t.getName() : "", null))
                .collect(Collectors.toList());
    }

    public List<MenuItem> getMenuItems(String categoryCode, String keyword) {
        LambdaQueryWrapper<DishesEntity> wrapper = new LambdaQueryWrapper<>();

        if (categoryCode != null && !categoryCode.isBlank()) {
            try {
                Integer typeId = Integer.parseInt(categoryCode);
                wrapper.eq(DishesEntity::getTypeId, typeId);
            } catch (Exception ignored) {}
        }

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DishesEntity::getName, keyword);
        }

        wrapper.eq(DishesEntity::getStatus, 1);
        List<DishesEntity> dishes = dishesMapper.selectList(wrapper);

        Map<Long, BigDecimal> priceMap = packageMapper.selectList(null).stream()
                .filter(pkg -> pkg.getDishesId() != null)
                .collect(Collectors.groupingBy(
                        DishesPackageEntity::getDishesId,
                        Collectors.collectingAndThen(
                                Collectors.minBy(java.util.Comparator.comparing(DishesPackageEntity::getPrice)),
                                opt -> opt.map(DishesPackageEntity::getPrice).orElse(BigDecimal.ZERO)
                        )
                ));

        return dishes.stream()
                .filter(d -> d.getId() != null)
                .map(d -> {
                    MenuItem item = new MenuItem();
                    item.setId(d.getId().longValue());
                    item.setName(d.getName() != null ? d.getName() : "");
                    item.setDescription(d.getDetail() != null ? d.getDetail() : "");
                    item.setCoverUrl(d.getCoverUrl() != null ? d.getCoverUrl() : "");
                    item.setPrice(priceMap.getOrDefault(d.getId().longValue(), BigDecimal.ZERO).doubleValue());
                    item.setStatus("AVAILABLE");
                    return item;
                })
                .collect(Collectors.toList());
    }
}