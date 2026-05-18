package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("wallet")
public class WalletEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private LocalDateTime createTime;
    private Integer status;
    private Integer type;
    private BigDecimal surplus;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public BigDecimal getSurplus() { return surplus; }
    public void setSurplus(BigDecimal surplus) { this.surplus = surplus; }
}
