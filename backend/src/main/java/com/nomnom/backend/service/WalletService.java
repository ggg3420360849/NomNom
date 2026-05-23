package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.WalletEntity;
import com.nomnom.backend.entity.WalletInfoEntity;
import com.nomnom.backend.mapper.WalletInfoMapper;
import com.nomnom.backend.mapper.WalletMapper;
import com.nomnom.backend.model.WalletDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private final WalletMapper walletMapper;
    private final WalletInfoMapper walletInfoMapper;

    public WalletService(WalletMapper walletMapper, WalletInfoMapper walletInfoMapper) {
        this.walletMapper = walletMapper;
        this.walletInfoMapper = walletInfoMapper;
    }

    public WalletEntity getWallet(Long userId) {
        return walletMapper.selectOne(
                new LambdaQueryWrapper<WalletEntity>()
                        .eq(WalletEntity::getUserId, userId.intValue())
                        .last("LIMIT 1"));
    }

    public List<WalletDetailResponse> getWalletInfo(Long walletId) {
        return walletInfoMapper.selectList(
                new LambdaQueryWrapper<WalletInfoEntity>()
                        .eq(WalletInfoEntity::getWalletId, walletId.intValue())
                        .orderByDesc(WalletInfoEntity::getCreateTime))
                .stream()
                .map(w -> {
                    WalletDetailResponse r = new WalletDetailResponse();
                    r.setId(w.getId().longValue());
                    r.setDetail(w.getDetail());
                    r.setSurplusMoney(w.getSurplusMoney());
                    r.setCreateTime(w.getCreateTime());
                    return r;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public WalletEntity recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("充值金额必须大于 0。");
        }
        WalletEntity wallet = createWallet(userId, 1);
        BigDecimal current = wallet.getSurplus() != null ? wallet.getSurplus() : BigDecimal.ZERO;
        wallet.setSurplus(current.add(amount));
        walletMapper.updateById(wallet);
        recordWalletInfo(wallet.getId(), "钱包充值", amount);
        return wallet;
    }

    @Transactional
    public WalletEntity pay(Long userId, BigDecimal amount, String orderCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("支付金额必须大于 0。");
        }
        WalletEntity wallet = createWallet(userId, 1);
        if (wallet.getStatus() != null && wallet.getStatus() == 0) {
            throw new IllegalArgumentException("钱包已冻结，暂不能支付。");
        }
        BigDecimal current = wallet.getSurplus() != null ? wallet.getSurplus() : BigDecimal.ZERO;
        if (current.compareTo(amount) < 0) {
            throw new IllegalArgumentException("钱包余额不足，请先充值。");
        }
        wallet.setSurplus(current.subtract(amount));
        walletMapper.updateById(wallet);
        recordWalletInfo(wallet.getId(), "订单支付" + (orderCode != null ? "：" + orderCode : ""), amount.negate());
        return wallet;
    }

    @Transactional
    public WalletEntity createWallet(Long userId, Integer type) {
        WalletEntity existing = getWallet(userId);
        if (existing != null) {
            return existing;
        }
        WalletEntity entity = new WalletEntity();
        entity.setUserId(userId.intValue());
        entity.setCreateTime(LocalDateTime.now());
        entity.setStatus(1);
        entity.setType(type);
        entity.setSurplus(BigDecimal.ZERO);
        walletMapper.insert(entity);
        return entity;
    }

    private void recordWalletInfo(Integer walletId, String detail, BigDecimal amount) {
        WalletInfoEntity info = new WalletInfoEntity();
        info.setWalletId(walletId);
        info.setDetail(detail);
        info.setSurplusMoney(amount);
        info.setCreateTime(LocalDateTime.now());
        walletInfoMapper.insert(info);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        WalletEntity entity = walletMapper.selectById(id);
        if (entity != null) {
            entity.setStatus(status);
            walletMapper.updateById(entity);
        }
    }
}
