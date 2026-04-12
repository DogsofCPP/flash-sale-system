package com.example.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MQ订单消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;
    private String orderNo;
    private Long userId;
    private Long productId;
    private Long activityId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    public OrderMessage(Long userId, Long productId, Long activityId,
            String productName, BigDecimal productPrice,
            Integer quantity, BigDecimal totalAmount, String orderNo) {
        this.messageId = java.util.UUID.randomUUID().toString().replace("-", "");
        this.createdAt = LocalDateTime.now();
        this.userId = userId;
        this.productId = productId;
        this.activityId = activityId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.orderNo = orderNo;
    }
}
