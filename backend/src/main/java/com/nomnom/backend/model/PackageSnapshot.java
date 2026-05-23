package com.nomnom.backend.model;

import java.math.BigDecimal;

public class PackageSnapshot {

    private Long packageId;
    private Long dishesId;
    private String dishName;
    private String packageName;
    private String specs;
    private String coverUrl;
    private BigDecimal price;
    private Integer dishStatus;
    private Integer stock;

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public Long getDishesId() {
        return dishesId;
    }

    public void setDishesId(Long dishesId) {
        this.dishesId = dishesId;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getSpecs() {
        return specs;
    }

    public void setSpecs(String specs) {
        this.specs = specs;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getDishStatus() {
        return dishStatus;
    }

    public void setDishStatus(Integer dishStatus) {
        this.dishStatus = dishStatus;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String resolveDisplayName() {
        StringBuilder builder = new StringBuilder();
        builder.append(dishName == null ? "" : dishName);
        if (packageName != null && !packageName.isBlank()) {
            builder.append(" - ").append(packageName);
        }
        if (specs != null && !specs.isBlank()) {
            builder.append(" / ").append(specs);
        }
        return builder.toString();
    }
}
