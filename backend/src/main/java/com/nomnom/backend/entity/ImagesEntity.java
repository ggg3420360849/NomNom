package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("images")
public class ImagesEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer dishesEvaluationsId;
    private String pictureUrl;
    private Integer number;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getDishesEvaluationsId() { return dishesEvaluationsId; }
    public void setDishesEvaluationsId(Integer dishesEvaluationsId) { this.dishesEvaluationsId = dishesEvaluationsId; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }
}
