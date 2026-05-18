package com.nomnom.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletDetailResponse {

    private Long id;
    private String detail;
    private BigDecimal surplusMoney;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public BigDecimal getSurplusMoney() { return surplusMoney; }
    public void setSurplusMoney(BigDecimal surplusMoney) { this.surplusMoney = surplusMoney; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
