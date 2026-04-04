package com.example.seckillservice.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SeckillOrder {

    private Long id;
    private Long userId;
    private Long activityId;
    private Long productId;
    private String orderNo;
    /** 0=排队中, 1=成功, 2=失败, 3=超时 */
    private Integer status;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
