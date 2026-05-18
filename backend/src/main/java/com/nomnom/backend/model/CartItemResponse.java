package com.nomnom.backend.model;

import java.math.BigDecimal;

public class CartItemResponse {
    private Long id;
    private Long userId;
    private Long dishesId;
    private Long packageId;
    private Integer buyNumber;
    private Integer isSelected;
    private String dishName;
    private String dishCover;
    private String packageName;
    private BigDecimal price;
    private String specs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDishesId() { return dishesId; }
    public void setDishesId(Long dishesId) { this.dishesId = dishesId; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Integer getBuyNumber() { return buyNumber; }
    public void setBuyNumber(Integer buyNumber) { this.buyNumber = buyNumber; }

    public Integer getIsSelected() { return isSelected; }
    public void setIsSelected(Integer isSelected) { this.isSelected = isSelected; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public String getDishCover() { return dishCover; }
    public void setDishCover(String dishCover) { this.dishCover = dishCover; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }
}
