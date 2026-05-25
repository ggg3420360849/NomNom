package com.nomnom.backend.model;

import java.time.LocalDateTime;
import java.util.List;

public class EvaluationResponse {
    private Long id;
    private Long dishesId;
    private Long ordersId;
    private String orderNo;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String dishName;
    private String content;
    private Integer ratingValue;
    private String replyContent;
    private LocalDateTime replyTime;
    private Integer replyStatus;
    private LocalDateTime createTime;
    private List<String> imageUrls;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDishesId() { return dishesId; }
    public void setDishesId(Long dishesId) { this.dishesId = dishesId; }

    public Long getOrdersId() { return ordersId; }
    public void setOrdersId(Long ordersId) { this.ordersId = ordersId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

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

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
}
