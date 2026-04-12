package com.example.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀下单响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResponse {

    /** 订单编号 */
    private String orderNo;

    /** 商品名称 */
    private String productName;

    /** 购买数量 */
    private Integer quantity;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单过期时间 */
    private LocalDateTime expireTime;

    public static SeckillResponse ok(String orderNo, String productName,
            Integer quantity, BigDecimal totalAmount, LocalDateTime expireTime) {
        return new SeckillResponse(orderNo, productName, quantity, totalAmount, expireTime);
    }
}
