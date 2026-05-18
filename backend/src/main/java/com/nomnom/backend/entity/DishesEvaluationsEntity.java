package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("dishes_evaluations")
public class DishesEvaluationsEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer dishesId;
    private Integer userId;
    private Integer ordersId;
    private String content;
    private Integer ratingValue;
    private String replyContent;
    private LocalDateTime replyTime;
    private Integer replyStatus;
    private LocalDateTime createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getDishesId() { return dishesId; }
    public void setDishesId(Integer dishesId) { this.dishesId = dishesId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getOrdersId() { return ordersId; }
    public void setOrdersId(Integer ordersId) { this.ordersId = ordersId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getRatingValue() { return ratingValue; }
    public void setRatingValue(Integer ratingValue) { this.ratingValue = ratingValue; }

    public String getReplyContent() { return replyContent; }
    public void setReplyContent(String replyContent) { this.replyContent = replyContent; }

    public LocalDateTime getReplyTime() { return replyTime; }
    public void setReplyTime(LocalDateTime replyTime) { this.replyTime = replyTime; }

    public Integer getReplyStatus() { return replyStatus; }
    public void setReplyStatus(Integer replyStatus) { this.replyStatus = replyStatus; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
