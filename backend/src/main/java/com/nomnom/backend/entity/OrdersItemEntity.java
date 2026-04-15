package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("orders_item")
public class OrdersItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ordersId;
    private Long dishesPackageId;
    private BigDecimal snapPrice;
    private String snapName;
    private String snapCover;
    private Integer buyNumber;
    private BigDecimal totalPrice;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrdersId() {
        return ordersId;
    }

    public void setOrdersId(Long ordersId) {
        this.ordersId = ordersId;
    }

    public Long getDishesPackageId() {
        return dishesPackageId;
    }

    public void setDishesPackageId(Long dishesPackageId) {
        this.dishesPackageId = dishesPackageId;
    }

    public BigDecimal getSnapPrice() {
        return snapPrice;
    }

    public void setSnapPrice(BigDecimal snapPrice) {
        this.snapPrice = snapPrice;
    }

    public String getSnapName() {
        return snapName;
    }

    public void setSnapName(String snapName) {
        this.snapName = snapName;
    }

    public String getSnapCover() {
        return snapCover;
    }

    public void setSnapCover(String snapCover) {
        this.snapCover = snapCover;
    }

    public Integer getBuyNumber() {
        return buyNumber;
    }

    public void setBuyNumber(Integer buyNumber) {
        this.buyNumber = buyNumber;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
