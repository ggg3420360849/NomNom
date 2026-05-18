package com.nomnom.backend.model;

import java.math.BigDecimal;

public class CollectionItemResponse {
    private Long id;
    private Long dishesId;
    private String dishName;
    private String dishCover;
    private BigDecimal price;
    private Integer dishStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDishesId() { return dishesId; }
    public void setDishesId(Long dishesId) { this.dishesId = dishesId; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public String getDishCover() { return dishCover; }
    public void setDishCover(String dishCover) { this.dishCover = dishCover; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getDishStatus() { return dishStatus; }
    public void setDishStatus(Integer dishStatus) { this.dishStatus = dishStatus; }
}
