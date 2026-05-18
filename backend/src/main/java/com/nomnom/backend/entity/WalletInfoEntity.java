package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("wallet_info")
public class WalletInfoEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer walletId;
    private String detail;
    private BigDecimal surplusMoney;
    private LocalDateTime createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getWalletId() { return walletId; }
    public void setWalletId(Integer walletId) { this.walletId = walletId; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public BigDecimal getSurplusMoney() { return surplusMoney; }
    public void setSurplusMoney(BigDecimal surplusMoney) { this.surplusMoney = surplusMoney; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
