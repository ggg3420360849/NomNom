package com.nomnom.backend.model;

public class RefundRequest {

    private Long ordersId;
    private String refundCause;

    public Long getOrdersId() { return ordersId; }
    public void setOrdersId(Long ordersId) { this.ordersId = ordersId; }

    public String getRefundCause() { return refundCause; }
    public void setRefundCause(String refundCause) { this.refundCause = refundCause; }
}
