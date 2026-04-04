package com.example.common;

/**
 * 订单状态枚举
 */
public class OrderStatus {

    public static final int PENDING_PAY = 0;      // 待支付
    public static final int PAID = 1;            // 已支付
    public static final int CANCELLED = 2;       // 已取消
    public static final int REFUNDED = 3;        // 已退款

    public static String getName(int status) {
        return switch (status) {
            case PENDING_PAY -> "待支付";
            case PAID -> "已支付";
            case CANCELLED -> "已取消";
            case REFUNDED -> "已退款";
            default -> "未知";
        };
    }
}
