package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.CollectionEntity;
import com.nomnom.backend.entity.DishesEntity;
import com.nomnom.backend.mapper.CollectionMapper;
import com.nomnom.backend.mapper.CollectionReadMapper;
import com.nomnom.backend.mapper.DishesMapper;
import com.nomnom.backend.model.CollectionItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CollectionService {

    private final CollectionMapper collectionMapper;
    private final CollectionReadMapper collectionReadMapper;
    private final DishesMapper dishesMapper;

    public CollectionService(CollectionMapper collectionMapper,
                             CollectionReadMapper collectionReadMapper,
                             DishesMapper dishesMapper) {
        this.collectionMapper = collectionMapper;
        this.collectionReadMapper = collectionReadMapper;
        this.dishesMapper = dishesMapper;
    }

    public List<CollectionItemResponse> getUserCollections(Long userId) {
        return collectionReadMapper.selectUserCollections(userId);
    }

    public boolean isCollected(Long userId, Long dishesId) {
        return collectionMapper.selectCount(new LambdaQueryWrapper<CollectionEntity>()
                .eq(CollectionEntity::getUserId, userId.intValue())
                .eq(CollectionEntity::getDishesId, dishesId.intValue())) > 0;
    }

    @Transactional
    public boolean toggle(Long userId, Long dishesId) {
        if (dishesId == null) {
            throw new IllegalArgumentException("菜品不能为空");
        }

        LambdaQueryWrapper<CollectionEntity> wrapper = new LambdaQueryWrapper<CollectionEntity>()
                .eq(CollectionEntity::getUserId, userId.intValue())
                .eq(CollectionEntity::getDishesId, dishesId.intValue());

        if (collectionMapper.selectCount(wrapper) > 0) {
            collectionMapper.delete(wrapper);
            return false;
        }

        DishesEntity dish = dishesMapper.selectById(dishesId.intValue());
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }
        if (dish.getStatus() == null || dish.getStatus() != 1) {
            throw new IllegalArgumentException("菜品已下架，暂时无法收藏");
        }

        CollectionEntity entity = new CollectionEntity();
        entity.setUserId(userId.intValue());
        entity.setDishesId(dishesId.intValue());
        collectionMapper.insert(entity);
        return true;
    }
}
