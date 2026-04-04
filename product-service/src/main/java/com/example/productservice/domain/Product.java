package com.example.productservice.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    /** 1=上架, 0=下架 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
