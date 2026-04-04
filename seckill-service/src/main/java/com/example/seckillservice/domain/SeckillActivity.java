package com.example.seckillservice.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SeckillActivity {

    private Long id;
    private String name;
    /** 0=未开始, 1=进行中, 2=已结束 */
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
