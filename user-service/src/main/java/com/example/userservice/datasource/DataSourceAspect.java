package com.example.userservice.datasource;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class DataSourceAspect {

    @Before("execution(* com.example.userservice.mapper.*.*(..))")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 判断是否使用注解指定数据源
        DataSource ds = method.getAnnotation(DataSource.class);
        if (ds != null) {
            DynamicDataSource.setDataSource(ds.value());
            return;
        }

        // 根据方法名自动选择: insert/update/delete -> master, select -> slave
        String methodName = method.getName();
        if (methodName.startsWith("insert") || methodName.startsWith("update")
            || methodName.startsWith("delete") || methodName.startsWith("add")
            || methodName.startsWith("save") || methodName.startsWith("remove")) {
            DynamicDataSource.useMaster();
        } else if (methodName.startsWith("select") || methodName.startsWith("get")
            || methodName.startsWith("find") || methodName.startsWith("list")
            || methodName.startsWith("count") || methodName.startsWith("query")) {
            DynamicDataSource.useSlave();
        } else {
            // 默认使用从库
            DynamicDataSource.useSlave();
        }
    }

    @After("execution(* com.example.userservice.mapper.*.*(..))")
    public void after() {
        DynamicDataSource.clearDataSource();
    }
}
