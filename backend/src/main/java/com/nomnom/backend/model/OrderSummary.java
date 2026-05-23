package com.nomnom.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSummary {

    private Long id;
    private String orderNo;
    private String customerName;
    private Long tableId;
    private String tableNumber;
    private String status;
    private double totalAmount;
    private int itemCount;
    private LocalDateTime createTime;
    private List<OrderItemSummary> items = new ArrayList<>();

    public OrderSummary() {
    }

    public OrderSummary(String orderNo, String customerName, String status, double totalAmount, int itemCount) {
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<OrderItemSummary> getItems() {
        return items;
    }

    public void setItems(List<OrderItemSummary> items) {
        this.items = items;
    }
}
