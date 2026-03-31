package com.example.userservice.domain;

import java.time.LocalDateTime;

public class SeckillOrder {
    private Long id;
    private Long userId;
    private Long activityId;
    private Long productId;
    private String orderNo;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SeckillOrder() {}

    public SeckillOrder(Long userId, Long activityId, Long productId, String orderNo) {
        this.userId = userId;
        this.activityId = activityId;
        this.productId = productId;
        this.orderNo = orderNo;
        this.status = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
