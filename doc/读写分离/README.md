# 读写分离

## 1. 概述

系统采用MySQL主从复制架构，实现读写分离，将读请求分发到从库，写请求路由到主库。

## 2. 架构

```
                    ┌───────────────┐
                    │  Application  │
                    └───────┬───────┘
                            │
                    ┌───────▼───────┐
                    │  Dynamic      │
                    │  DataSource   │
                    │  (AOP路由)    │
                    └───────┬───────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
       ┌──────▼──────┐             ┌──────▼──────┐
       │   Master    │             │    Slave    │
       │   (写)       │             │    (读)      │
       │   3306       │             │    3307      │
       └─────────────┘             └─────────────┘
```

## 3. 实现原理

### 3.1 DynamicDataSource

通过继承`AbstractRoutingDataSource`，使用ThreadLocal存储当前线程使用的数据源key：

```java
public class DynamicDataSource extends AbstractRoutingDataSource {
    private static final ThreadLocal<String> DATA_SOURCE_KEY =
        new ThreadLocal<>();

    public static void setDataSource(String dataSource) {
        DATA_SOURCE_KEY.set(dataSource);
    }

    public static String getDataSource() {
        return DATA_SOURCE_KEY.get();
    }

    public static void clear() {
        DATA_SOURCE_KEY.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return getDataSource();
    }
}
```

### 3.2 DataSourceAspect切面

通过AOP自动识别方法类型并切换数据源：

```java
@Aspect
@Component
public class DataSourceAspect {
    @Before("execution(* com.example..mapper.*.*(..))")
    public void before(JoinPoint point) {
        String methodName = point.getSignature().getName();
        if (isReadMethod(methodName)) {
            DataSourceContextHolder.setDataSource("slaveDataSource");
        } else {
            DataSourceContextHolder.setDataSource("masterDataSource");
        }
    }

    private boolean isReadMethod(String methodName) {
        return methodName.startsWith("select") ||
               methodName.startsWith("find") ||
               methodName.startsWith("get") ||
               methodName.startsWith("list") ||
               methodName.startsWith("count") ||
               methodName.startsWith("query");
    }
}
```

### 3.3 @DataSource注解

手动指定数据源：

```java
@DataSource("master")
public void createOrder() { }

@DataSource("slave")
public Product findById(Long id) { }
```

## 4. 主从复制配置

### 4.1 Master配置 (docker/mysql/master.cnf)

```ini
[mysqld]
server-id=1
log-bin=mysql-bin
binlog-format=ROW
gtid-mode=ON
enforce-gtid-consistency=ON
sync-binlog=1
innodb_flush_log_at_trx_commit=1
```

### 4.2 Slave配置 (docker/mysql/slave.cnf)

```ini
[mysqld]
server-id=2
relay-log=relay-bin
read-only=1
gtid-mode=ON
replica-skip-errors=1062
```

### 4.3 初始化复制用户 (docker/init.sql)

```sql
CREATE USER 'replica'@'%' IDENTIFIED BY 'replica123';
GRANT REPLICATION SLAVE ON *.* TO 'replica'@'%';
FLUSH PRIVILEGES;
```

## 5. 测试验证

### 5.1 测试接口

```bash
# 测试数据源连接
curl http://localhost:8086/api/test/datasource

# 测试读写分离
curl http://localhost:8086/api/test/read-write
```

### 5.2 预期输出

主库测试应返回"master"，从库测试应返回"slave"。

## 6. 注意事项

1. **主从延迟**：从库可能有轻微延迟，对实时性要求高的场景应强制使用主库
2. **数据一致性**：写操作后立即读取建议使用主库
3. **事务处理**：事务内所有操作必须在同一数据源
4. **连接池配置**：主从库连接池大小应分别配置
