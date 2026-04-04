package com.example.common;

import lombok.Data;

/**
 * 秒杀下单请求
 */
@Data
public class SeckillRequest {

    /** 活动ID */
    private Long activityId;

    /** 商品ID */
    private Long productId;

    /** 购买数量（默认1） */
    private Integer quantity = 1;
}
