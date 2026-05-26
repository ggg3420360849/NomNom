package com.nomnom.backend.model;

public class RefundRequest {

    private Long ordersId;
    private String refundCause;
    private String requestType;

    public Long getOrdersId() { return ordersId; }
    public void setOrdersId(Long ordersId) { this.ordersId = ordersId; }

    public String getRefundCause() { return refundCause; }
    public void setRefundCause(String refundCause) { this.refundCause = refundCause; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
}
