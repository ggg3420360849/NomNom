package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.CollectionEntity;
import com.nomnom.backend.mapper.CollectionMapper;
import com.nomnom.backend.mapper.CollectionReadMapper;
import com.nomnom.backend.model.CollectionItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CollectionService {

    private final CollectionMapper collectionMapper;
    private final CollectionReadMapper collectionReadMapper;

    public CollectionService(CollectionMapper collectionMapper, CollectionReadMapper collectionReadMapper) {
        this.collectionMapper = collectionMapper;
        this.collectionReadMapper = collectionReadMapper;
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
        CollectionEntity existing = collectionMapper.selectOne(
                new LambdaQueryWrapper<CollectionEntity>()
                        .eq(CollectionEntity::getUserId, userId.intValue())
                        .eq(CollectionEntity::getDishesId, dishesId.intValue())
                        .last("LIMIT 1"));
        if (existing != null) {
            collectionMapper.deleteById(existing.getId());
            return false;
        }
        CollectionEntity entity = new CollectionEntity();
        entity.setUserId(userId.intValue());
        entity.setDishesId(dishesId.intValue());
        collectionMapper.insert(entity);
        return true;
    }
}
