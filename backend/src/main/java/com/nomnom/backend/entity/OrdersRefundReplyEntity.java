package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("orders_refund_reply")
public class OrdersRefundReplyEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer ordersId;
    private Integer status;
    private String refundCause;
    private String rejectRefundCause;
    private LocalDateTime createTime;
    private LocalDateTime refunTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrdersId() { return ordersId; }
    public void setOrdersId(Integer ordersId) { this.ordersId = ordersId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getRefundCause() { return refundCause; }
    public void setRefundCause(String refundCause) { this.refundCause = refundCause; }

    public String getRejectRefundCause() { return rejectRefundCause; }
    public void setRejectRefundCause(String rejectRefundCause) { this.rejectRefundCause = rejectRefundCause; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getRefunTime() { return refunTime; }
    public void setRefunTime(LocalDateTime refunTime) { this.refunTime = refunTime; }
}
