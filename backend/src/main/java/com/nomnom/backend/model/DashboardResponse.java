package com.nomnom.backend.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardResponse {

    private int todayOrderCount;
    private BigDecimal todayRevenue;
    private int totalOrders;
    private int pendingCount;
    private int paidCount;
    private int preparingCount;
    private int completedCount;
    private List<Map<String, Object>> topDishes;

    public int getTodayOrderCount() { return todayOrderCount; }
    public void setTodayOrderCount(int todayOrderCount) { this.todayOrderCount = todayOrderCount; }

    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getPaidCount() { return paidCount; }
    public void setPaidCount(int paidCount) { this.paidCount = paidCount; }

    public int getPreparingCount() { return preparingCount; }
    public void setPreparingCount(int preparingCount) { this.preparingCount = preparingCount; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    public List<Map<String, Object>> getTopDishes() { return topDishes; }
    public void setTopDishes(List<Map<String, Object>> topDishes) { this.topDishes = topDishes; }
}
