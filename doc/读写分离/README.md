# 数据库读写分离方案

## 1. 设计背景

在大规模电商系统中，读操作（商品查询、订单列表、用户信息）通常远多于写操作（下单、支付）。通过将读写流量分离到不同数据库节点，可以有效提升系统吞吐量。

本方案采用 **Spring AOP + ThreadLocal** 实现零侵入式的动态数据源切换。

## 2. 技术方案

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        应用层                                │
│  ┌─────────────────────────────────────────────────────┐  │
│  │              DataSourceInterceptor                   │  │
│  │         (基于方法名的自动路由切面)                      │  │
│  └─────────────────────────────────────────────────────┘  │
│                            │                               │
│  ┌─────────────────────────┴─────────────────────────────┐  │
│  │              DynamicDataSource                         │  │
│  │         (继承AbstractRoutingDataSource)                │  │
│  └─────────────────────────┬─────────────────────────────┘  │
└────────────────────────────┼────────────────────────────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
    ┌─────▼─────┐      ┌─────▼─────┐      ┌─────▼─────┐
    │   Master   │      │   Slave1  │      │   Slave2  │
    │   写库     │      │   读库    │      │   读库    │
    │  (3306)   │      │  (3307)   │      │  (3308)   │
    └───────────┘      └───────────┘      └───────────┘
```

### 2.2 核心组件

| 组件 | 职责 |
|------|------|
| `DataSourceContextHolder` | ThreadLocal存储当前线程的数据源标识 |
| `DynamicDataSource` | 动态数据源路由实现 |
| `TargetDataSourceAspect` | AOP切面自动识别读写操作 |
| `@TargetDataSource` | 注解式手动指定数据源 |

## 3. 关键实现

### 3.1 上下文持有者

```java
public class DataSourceContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void setDataSource(String name) {
        CONTEXT.set(name);
    }

    public static String getDataSource() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
```

### 3.2 动态路由数据源

```java
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String key = DataSourceContextHolder.getDataSource();
        return key != null ? key : "master";
    }
}
```

### 3.3 自动路由切面

```java
@Aspect
@Component
public class TargetDataSourceAspect {

    @Around("execution(* com.example..mapper.*.*(..))")
    public Object routeDataSource(ProceedingJoinPoint point) throws Throwable {
        String method = point.getSignature().getName();
        Class<?> clazz = point.getSignature().getDeclaringType();

        // 检查方法级注解
        try {
            Method m = clazz.getMethod(method,
                Arrays.stream(point.getArgs()).map(Object::getClass).toArray(Class[]::new));
            if (m != null && m.isAnnotationPresent(TargetDataSource.class)) {
                String ds = m.getAnnotation(TargetDataSource.class).value();
                DataSourceContextHolder.setDataSource(ds);
                return executeAndClear(point);
            }
        } catch (Exception ignored) {}

        // 根据方法名前缀自动判断
        if (isReadOperation(method)) {
            DataSourceContextHolder.setDataSource("slave");
        } else {
            DataSourceContextHolder.setDataSource("master");
        }
        return executeAndClear(point);
    }

    private boolean isReadOperation(String method) {
        return method.startsWith("select") || method.startsWith("find") ||
               method.startsWith("get")    || method.startsWith("list") ||
               method.startsWith("count")  || method.startsWith("query");
    }

    private Object executeAndClear(ProceedingJoinPoint point) throws Throwable {
        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
```

### 3.4 注解式路由

```java
// 强制使用主库
@TargetDataSource("master")
@Insert("INSERT INTO orders (...) VALUES (...)")
void insertOrder(Order order);

// 强制使用从库
@TargetDataSource("slave")
@Select("SELECT * FROM products WHERE id = #{id}")
Product findProduct(Long id);
```

## 4. MySQL主从配置

### 4.1 主库配置

```ini
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog-format = ROW
gtid-mode = ON
enforce-gtid-consistency = ON
sync-binlog = 1
```

### 4.2 从库配置

```ini
[mysqld]
server-id = 2
relay-log = relay-bin
read-only = ON
gtid-mode = ON
```

### 4.3 复制账号

```sql
CREATE USER 'sync_user'@'%' IDENTIFIED WITH mysql_native_password BY 'sync_pass';
GRANT REPLICATION SLAVE ON *.* TO 'sync_user'@'%';
FLUSH PRIVILEGES;
```

## 5. 性能测试

| 场景 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 商品查询QPS | 1,200 | 8,500 | 7x |
| 订单查询QPS | 1,800 | 9,200 | 5x |
| 写操作QPS | 800 | 750 | -6% |

> 注：写操作略有下降是因为增加了数据源路由开销

## 6. 使用约束

1. **事务一致性**：同一个事务内所有SQL必须在同一数据源
2. **延迟敏感**：写后立即读场景，建议显式使用 `@TargetDataSource("master")`
3. **连接管理**：主从连接池应独立配置，主库池稍大
4. **监控告警**：监控主从同步延迟，超过阈值自动告警

## 7. 验证方法

```bash
# 查看当前使用的数据源
curl http://localhost:8081/api/test/datasource

# 验证写操作走主库
curl http://localhost:8081/api/users/register \
  -X POST -d '{"username":"test","password":"123456"}'

# 验证读操作走从库
curl http://localhost:8081/api/users/1
```

## 8. 扩展阅读

- [分库分表方案](../消息队列与秒杀/02-雪花算法与分库分表.md)
- [缓存架构设计](../06-缓存架构.md)
