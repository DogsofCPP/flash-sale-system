package com.example.common;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 通用分页请求参数
 */
@Data
public class PageRequest {

    private Integer page = 1;

    private Integer size = 10;

    public int getOffset() {
        return (page - 1) * size;
    }
}
