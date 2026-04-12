package com.example.common.transaction.annotation;

import java.lang.annotation.*;

/**
 * 标注在一个方法上，标记为TCC事务的Try阶段
 *
 * usage:
 * @TccTry
 * public boolean tryDeductStock(Long activityId, Long productId, int quantity) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TccTry {
    /** 事务参与者名称（对应Confirm/Cancel方法的服务名） */
    String participant() default "";

    /** 确认方法名（默认 = 方法名 + "Confirm"） */
    String confirmMethod() default "";

    /** 取消方法名（默认 = 方法名 + "Cancel"） */
    String cancelMethod() default "";

    /** 事务超时时间（秒），默认30秒 */
    int timeout() default 30;

    /** 是否异步执行Confirm/Cancel，默认false */
    boolean async() default false;
}