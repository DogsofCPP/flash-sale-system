package com.example.orderservice.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private Long activityId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    /** 0=待支付, 1=已支付, 2=已取消, 3=已退款 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
    private LocalDateTime cancelTime;
}
