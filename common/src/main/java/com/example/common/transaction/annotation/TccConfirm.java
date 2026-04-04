package com.example.common.transaction.annotation;

import java.lang.annotation.*;

/**
 * 标注在一个方法上，标记为TCC事务的Confirm阶段
 * 该方法无业务含义，仅供TCC协调器调用
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TccConfirm {
}