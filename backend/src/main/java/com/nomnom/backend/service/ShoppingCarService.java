package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.ShoppingCarEntity;
import com.nomnom.backend.mapper.CartReadMapper;
import com.nomnom.backend.mapper.PackageReadMapper;
import com.nomnom.backend.mapper.ShoppingCarMapper;
import com.nomnom.backend.model.CartItemResponse;
import com.nomnom.backend.model.PackageSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCarService {

    private static final int MAX_CART_QUANTITY = 99;

    private final ShoppingCarMapper shoppingCarMapper;
    private final CartReadMapper cartReadMapper;
    private final PackageReadMapper packageReadMapper;

    public ShoppingCarService(ShoppingCarMapper shoppingCarMapper,
                              CartReadMapper cartReadMapper,
                              PackageReadMapper packageReadMapper) {
        this.shoppingCarMapper = shoppingCarMapper;
        this.cartReadMapper = cartReadMapper;
        this.packageReadMapper = packageReadMapper;
    }

    public List<CartItemResponse> getCartItems(Long userId) {
        validateUserId(userId);
        return cartReadMapper.selectCartItems(userId);
    }

    @Transactional
    public void addItem(Long userId, Long dishesId, Long packageId, Integer buyNumber) {
        validateUserId(userId);
        int quantity = normalizeQuantity(buyNumber);
        PackageSnapshot snapshot = resolvePackageSnapshot(packageId);
        validateDishMatch(dishesId, snapshot);

        ShoppingCarEntity existing = shoppingCarMapper.selectOne(
                new LambdaQueryWrapper<ShoppingCarEntity>()
                        .eq(ShoppingCarEntity::getUserId, userId.intValue())
                        .eq(ShoppingCarEntity::getDishesPackageId, packageId.intValue())
                        .last("LIMIT 1"));
        if (existing != null) {
            int nextQuantity = (existing.getBuyNumber() != null ? existing.getBuyNumber() : 0) + quantity;
            validateQuantity(nextQuantity);
            validateStock(snapshot, nextQuantity);
            existing.setDishesId(snapshot.getDishesId().intValue());
            existing.setBuyNumber(nextQuantity);
            shoppingCarMapper.updateById(existing);
            return;
        }
        validateStock(snapshot, quantity);
        ShoppingCarEntity entity = new ShoppingCarEntity();
        entity.setUserId(userId.intValue());
        entity.setDishesId(snapshot.getDishesId().intValue());
        entity.setDishesPackageId(packageId.intValue());
        entity.setBuyNumber(quantity);
        entity.setIsSeleced(1);
        entity.setCreateTime(LocalDateTime.now());
        shoppingCarMapper.insert(entity);
    }

    @Transactional
    public void updateQuantity(Long userId, Long id, Integer buyNumber) {
        ShoppingCarEntity entity = requireOwnedCartItem(userId, id);
        int quantity = normalizeQuantity(buyNumber);
        PackageSnapshot snapshot = resolvePackageSnapshot(entity.getDishesPackageId().longValue());
        validateStock(snapshot, quantity);
        entity.setBuyNumber(quantity);
        shoppingCarMapper.updateById(entity);
    }

    @Transactional
    public void toggleSelected(Long userId, Long id) {
        ShoppingCarEntity entity = requireOwnedCartItem(userId, id);
        entity.setIsSeleced(entity.getIsSeleced() != null && entity.getIsSeleced() == 1 ? 0 : 1);
        shoppingCarMapper.updateById(entity);
    }

    @Transactional
    public void removeItem(Long userId, Long id) {
        ShoppingCarEntity entity = requireOwnedCartItem(userId, id);
        shoppingCarMapper.deleteById(entity.getId());
    }

    @Transactional
    public void clearCart(Long userId) {
        validateUserId(userId);
        shoppingCarMapper.delete(new LambdaQueryWrapper<ShoppingCarEntity>()
                .eq(ShoppingCarEntity::getUserId, userId.intValue()));
    }

    private ShoppingCarEntity requireOwnedCartItem(Long userId, Long id) {
        validateUserId(userId);
        if (id == null) {
            throw new IllegalArgumentException("购物车项不能为空。");
        }
        ShoppingCarEntity entity = shoppingCarMapper.selectOne(new LambdaQueryWrapper<ShoppingCarEntity>()
                .eq(ShoppingCarEntity::getId, id.intValue())
                .eq(ShoppingCarEntity::getUserId, userId.intValue())
                .last("LIMIT 1"));
        if (entity == null) {
            throw new IllegalArgumentException("购物车项不存在或无权操作。");
        }
        return entity;
    }

    private PackageSnapshot resolvePackageSnapshot(Long packageId) {
        if (packageId == null) {
            throw new IllegalArgumentException("菜品套餐不能为空。");
        }
        List<PackageSnapshot> snapshots = packageReadMapper.selectPackageSnapshots(List.of(packageId));
        if (snapshots.isEmpty()) {
            throw new IllegalArgumentException("菜品套餐不存在。");
        }
        PackageSnapshot snapshot = snapshots.get(0);
        if (snapshot.getDishStatus() == null || snapshot.getDishStatus() != 1) {
            throw new IllegalArgumentException("菜品已下架，暂时无法加入购物车。");
        }
        if (snapshot.getDishesId() == null) {
            throw new IllegalArgumentException("菜品信息不完整，无法加入购物车。");
        }
        return snapshot;
    }

    private void validateDishMatch(Long dishesId, PackageSnapshot snapshot) {
        if (dishesId != null && !snapshot.getDishesId().equals(dishesId)) {
            throw new IllegalArgumentException("菜品和套餐不匹配。");
        }
    }

    private int normalizeQuantity(Integer buyNumber) {
        int quantity = buyNumber != null ? buyNumber : 1;
        validateQuantity(quantity);
        return quantity;
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("购买数量必须大于 0。");
        }
        if (quantity > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("单个菜品最多可加入 " + MAX_CART_QUANTITY + " 份。");
        }
    }

    private void validateStock(PackageSnapshot snapshot, int quantity) {
        if (snapshot.getStock() != null && snapshot.getStock() < quantity) {
            throw new IllegalArgumentException(snapshot.getDishName() + " 库存不足。");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录。");
        }
    }
}
