package com.nomnom.backend.entity;

import java.time.LocalDateTime;

public class CommentEntity {
    private Long id;
    private Long orderId;
    private Integer star;
    private String content;
    private LocalDateTime createTime;

    // Getter/Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getStar() { return star; }
    public void setStar(Integer star) { this.star = star; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}