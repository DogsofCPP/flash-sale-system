package com.example.seckillservice.domain;

import java.math.BigDecimal;

public class SeckillProduct {

    private Long id;
    private Long activityId;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer soldCount;
    private Integer limitPerUser;

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
    public Integer getSoldCount() { return soldCount; }
    public void setSoldCount(Integer soldCount) { this.soldCount = soldCount; }
    public Integer getLimitPerUser() { return limitPerUser; }
    public void setLimitPerUser(Integer limitPerUser) { this.limitPerUser = limitPerUser; }
}
