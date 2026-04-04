package com.example.common.transaction.annotation;

import java.lang.annotation.*;

/**
 * 标注在类或方法上，标记为TCC事务参与者
 * 用于TCC框架扫描和注册
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TccParticipant {
    /** 事务确认方法 */
    String confirmMethod() default "";

    /** 事务取消方法 */
    String cancelMethod() default "";

    /** 参与者别名 */
    String alias() default "";
}