package com.nomnom.backend.model;

public class OrderSummary {

    private String orderNo;
    private String customerName;
    private String status;
    private double totalAmount;
    private int itemCount;

    public OrderSummary() {
    }

    public OrderSummary(String orderNo, String customerName, String status, double totalAmount, int itemCount) {
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
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
}
