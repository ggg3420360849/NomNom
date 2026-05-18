package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.ShoppingCarEntity;
import com.nomnom.backend.mapper.CartReadMapper;
import com.nomnom.backend.mapper.ShoppingCarMapper;
import com.nomnom.backend.model.CartItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCarService {

    private final ShoppingCarMapper shoppingCarMapper;
    private final CartReadMapper cartReadMapper;

    public ShoppingCarService(ShoppingCarMapper shoppingCarMapper, CartReadMapper cartReadMapper) {
        this.shoppingCarMapper = shoppingCarMapper;
        this.cartReadMapper = cartReadMapper;
    }

    public List<CartItemResponse> getCartItems(Long userId) {
        return cartReadMapper.selectCartItems(userId);
    }

    @Transactional
    public void addItem(Long userId, Long dishesId, Long packageId, Integer buyNumber) {
        ShoppingCarEntity existing = shoppingCarMapper.selectOne(
                new LambdaQueryWrapper<ShoppingCarEntity>()
                        .eq(ShoppingCarEntity::getUserId, userId.intValue())
                        .eq(ShoppingCarEntity::getDishesPackageId, packageId.intValue())
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setBuyNumber(existing.getBuyNumber() + buyNumber);
            shoppingCarMapper.updateById(existing);
            return;
        }
        ShoppingCarEntity entity = new ShoppingCarEntity();
        entity.setUserId(userId.intValue());
        entity.setDishesId(dishesId.intValue());
        entity.setDishesPackageId(packageId.intValue());
        entity.setBuyNumber(buyNumber);
        entity.setIsSeleced(1);
        entity.setCreateTime(LocalDateTime.now());
        shoppingCarMapper.insert(entity);
    }

    @Transactional
    public void updateQuantity(Long id, Integer buyNumber) {
        ShoppingCarEntity entity = shoppingCarMapper.selectById(id);
        if (entity != null) {
            entity.setBuyNumber(buyNumber);
            shoppingCarMapper.updateById(entity);
        }
    }

    @Transactional
    public void toggleSelected(Long id) {
        ShoppingCarEntity entity = shoppingCarMapper.selectById(id);
        if (entity != null) {
            entity.setIsSeleced(entity.getIsSeleced() != null && entity.getIsSeleced() == 1 ? 0 : 1);
            shoppingCarMapper.updateById(entity);
        }
    }

    @Transactional
    public void removeItem(Long id) {
        shoppingCarMapper.deleteById(id);
    }

    @Transactional
    public void clearCart(Long userId) {
        shoppingCarMapper.delete(new LambdaQueryWrapper<ShoppingCarEntity>()
                .eq(ShoppingCarEntity::getUserId, userId.intValue()));
    }
}
