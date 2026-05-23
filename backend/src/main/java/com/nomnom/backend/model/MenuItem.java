package com.nomnom.backend.model;

public class MenuItem {

    private Long id;
    private Long dishesId;
    private String name;
    private String category;
    private String categoryLabel;
    private double price;
    private String description;
    private String coverUrl;
    private String status;
    private int monthlySales;
    private boolean recommend;

    public MenuItem() {
    }

    public MenuItem(Long id, String name, String category, String categoryLabel, double price, String description, String coverUrl, String status, int monthlySales) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.categoryLabel = categoryLabel;
        this.price = price;
        this.description = description;
        this.coverUrl = coverUrl;
        this.status = status;
        this.monthlySales = monthlySales;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDishesId() {
        return dishesId;
    }

    public void setDishesId(Long dishesId) {
        this.dishesId = dishesId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMonthlySales() {
        return monthlySales;
    }

    public void setMonthlySales(int monthlySales) {
        this.monthlySales = monthlySales;
    }

    public boolean isRecommend() {
        return recommend;
    }

    public void setRecommend(boolean recommend) {
        this.recommend = recommend;
    }
}
