package com.example.userservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrder {
    private Long id;
    private Long userId;
    private Long activityId;
    private Long productId;
    private String orderNo;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SeckillOrder(Long userId, Long activityId, Long productId, String orderNo) {
        this.userId = userId;
        this.activityId = activityId;
        this.productId = productId;
        this.orderNo = orderNo;
        this.status = 0;
    }
}
