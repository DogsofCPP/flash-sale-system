package com.example.userservice.domain;

import lombok.Data;

@Data
public class User {

    private Long id;

    private String username;

    private String passwordHash;

    private String phone;

    /**
     * 0 = 禁用, 1 = 正常
     */
    private Integer status;
}

