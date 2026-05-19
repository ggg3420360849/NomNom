package com.nomnom.backend.model;

public class CommentRequest {
    private Long orderId;
    private Integer star;
    private String content;

    // Getter/Setter
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getStar() { return star; }
    public void setStar(Integer star) { this.star = star; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}