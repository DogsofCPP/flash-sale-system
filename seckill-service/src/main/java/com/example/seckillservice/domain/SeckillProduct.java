package com.example.seckillservice.domain;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SeckillProduct {

    private Long id;
    private Long activityId;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer soldCount;
    private Integer limitPerUser;
}
