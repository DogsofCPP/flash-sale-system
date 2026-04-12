package com.example.common.transaction.annotation;

import java.lang.annotation.*;

/**
 * 标注在一个方法上，声明该方法参与TCC分布式事务
 * 方法内部会先执行本地业务，然后注册TCC事务
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TccTransaction {
    /** 业务类型，如 SECKILL_ORDER, PAYMENT */
    String bizType();

    /** 全局事务超时时间（秒），默认60秒 */
    int timeout() default 60;

    /** 事务失败是否自动回滚，默认true */
    boolean autoRollback() default true;

    /** 备注/描述 */
    String desc() default "";
}