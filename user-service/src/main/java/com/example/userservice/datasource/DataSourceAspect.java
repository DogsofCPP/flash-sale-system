package com.example.userservice.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DataSourceAspect {

    @Pointcut("execution(* com.example.userservice.mapper.*.*(..))")
    public void dataSourcePointcut() {}

    @Around("dataSourcePointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        String methodName = point.getSignature().getName();
        Class<?> declaringType = point.getSignature().getDeclaringType();

        try {
            java.lang.reflect.Method method = declaringType.getMethod(methodName,
                java.util.Arrays.stream(point.getArgs()).map(Object::getClass).toArray(Class[]::new));
            if (method != null && method.isAnnotationPresent(TargetDataSource.class)) {
                String ds = method.getAnnotation(TargetDataSource.class).value();
                DataSourceContextHolder.setDataSource(ds);
                try {
                    return point.proceed();
                } finally {
                    DataSourceContextHolder.clear();
                }
            }
        } catch (Exception ignored) {}

        if (methodName.startsWith("select") || methodName.startsWith("find")
            || methodName.startsWith("get") || methodName.startsWith("list")
            || methodName.startsWith("count") || methodName.startsWith("query")) {
            DataSourceContextHolder.setDataSource("slave");
        } else {
            DataSourceContextHolder.setDataSource("master");
        }

        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
