package com.nomnom.backend.model;

import java.math.BigDecimal;

public class PackageSnapshot {

    private Long packageId;
    private String dishName;
    private String packageName;
    private String specs;
    private String coverUrl;
    private BigDecimal price;
    private Integer dishStatus;

    // ========== 新增字段（用于评价功能）==========
    private Integer buyNumber;     // 购买数量
    private Integer ratingValue;   // 评分（1-5，前端绑定用）
    private String commentContent; // 评价内容（前端绑定用）
    // =========================================

    // 原有的 getter/setter
    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
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

    // ========== 新增的 getter/setter ==========
    public Integer getBuyNumber() {
        return buyNumber;
    }

    public void setBuyNumber(Integer buyNumber) {
        this.buyNumber = buyNumber;
    }

    public Integer getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(Integer ratingValue) {
        this.ratingValue = ratingValue;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }
    // =========================================

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