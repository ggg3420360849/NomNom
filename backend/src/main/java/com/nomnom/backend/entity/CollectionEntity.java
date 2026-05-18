package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("collection")
public class CollectionEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer dishesId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getDishesId() { return dishesId; }
    public void setDishesId(Integer dishesId) { this.dishesId = dishesId; }
}
