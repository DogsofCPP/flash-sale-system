package com.example.common;

/**
 * 秒杀订单状态枚举
 */
public class SeckillOrderStatus {

    public static final int QUEUING = 0;   // 排队中
    public static final int SUCCESS = 1;   // 成功
    public static final int FAILED = 2;    // 失败
    public static final int TIMEOUT = 3;   // 超时

    public static String getName(int status) {
        return switch (status) {
            case QUEUING -> "排队中";
            case SUCCESS -> "成功";
            case FAILED -> "失败";
            case TIMEOUT -> "超时";
            default -> "未知";
        };
    }
}
