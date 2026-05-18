package com.nomnom.backend.model;

public class EvaluationRequest {

    private Long dishesId;
    private Long ordersId;
    private Integer ratingValue;
    private String content;
    private java.util.List<String> imageUrls;

    public Long getDishesId() { return dishesId; }
    public void setDishesId(Long dishesId) { this.dishesId = dishesId; }

    public Long getOrdersId() { return ordersId; }
    public void setOrdersId(Long ordersId) { this.ordersId = ordersId; }

    public Integer getRatingValue() { return ratingValue; }
    public void setRatingValue(Integer ratingValue) { this.ratingValue = ratingValue; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public java.util.List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(java.util.List<String> imageUrls) { this.imageUrls = imageUrls; }
}
