package com.example.userservice.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SeckillProduct {
    private Long id;
    private Long activityId;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer limitPerUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SeckillProduct() {}

    public SeckillProduct(Long activityId, Long productId, BigDecimal seckillPrice, Integer stock, Integer limitPerUser) {
        this.activityId = activityId;
        this.productId = productId;
        this.seckillPrice = seckillPrice;
        this.stock = stock;
        this.limitPerUser = limitPerUser;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public BigDecimal getSeckillPrice() { return seckillPrice; }
    public void setSeckillPrice(BigDecimal seckillPrice) { this.seckillPrice = seckillPrice; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getLimitPerUser() { return limitPerUser; }
    public void setLimitPerUser(Integer limitPerUser) { this.limitPerUser = limitPerUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
