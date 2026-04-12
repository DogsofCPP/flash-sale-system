package com.example.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀订单创建事件
 * 用于MQ异步传递订单创建请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 幂等性Key
     */
    private String idempotencyKey;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 秒杀活动ID
     */
    private Long activityId;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createTime;

    /**
     * 生成幂等性Key
     */
    public static String generateIdempotencyKey(Long userId, Long productId, Long activityId) {
        return String.format("seckill:%d:%d:%d", userId, productId, activityId);
    }
}
