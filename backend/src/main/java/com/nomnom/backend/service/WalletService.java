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

    @Transactional
    public void updateStatus(Long id, Integer status) {
        WalletEntity entity = walletMapper.selectById(id);
        if (entity != null) {
            entity.setStatus(status);
            walletMapper.updateById(entity);
        }
    }
}
