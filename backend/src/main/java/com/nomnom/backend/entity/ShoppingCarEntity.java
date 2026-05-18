package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("shopping_car")
public class ShoppingCarEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer dishesId;
    private Integer dishesPackageId;
    private Integer buyNumber;
    private Integer isSeleced;
    private LocalDateTime createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getDishesId() { return dishesId; }
    public void setDishesId(Integer dishesId) { this.dishesId = dishesId; }

    public Integer getDishesPackageId() { return dishesPackageId; }
    public void setDishesPackageId(Integer dishesPackageId) { this.dishesPackageId = dishesPackageId; }

    public Integer getBuyNumber() { return buyNumber; }
    public void setBuyNumber(Integer buyNumber) { this.buyNumber = buyNumber; }

    public Integer getIsSeleced() { return isSeleced; }
    public void setIsSeleced(Integer isSeleced) { this.isSeleced = isSeleced; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
