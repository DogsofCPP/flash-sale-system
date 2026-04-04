# Docker部署指南

## 1. 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| Docker | 20.10+ | 容器引擎 |
| Docker Compose | 2.0+ | 容器编排 |
| 内存 | 8GB+ | 推荐16GB |
| 磁盘 | 50GB+ | SSD推荐 |

## 2. 部署架构

```
docker-compose.yml（12个容器）
├── 基础设施层
│   ├── mysql-master (3307)      # 所有服务的写库
│   ├── mysql-slave  (3308)      # user-service的读库
│   ├── redis         (6379)      # 缓存与分布式锁
│   ├── rabbitmq      (5672/15672)# 消息队列
│   ├── elasticsearch (9200/9300) # 商品搜索引擎
│   ├── kibana        (5601)      # ES可视化
│   ├── nacos         (8848)      # 服务注册与配置中心
│   └── kafka         (9092)      # Kafka消息队列
│
├── 应用服务层
│   ├── gateway       (8080)      # Spring Cloud Gateway
│   ├── user-service-1/2 (8081) # 用户服务（×2实例）
│   ├── product-service (8083)     # 商品服务
│   ├── order-service  (8085)      # 订单服务
│   └── seckill-service-1/2 (8086)# 秒杀服务（×2实例）
│
└── 前端层
    └── nginx (80)                # 静态资源 + 反向代理Gateway
```

## 3. 快速部署

### 方式一：使用启动脚本

```bash
cd g:\sale_system\flash-sale-system
./docker-start.sh
```

### 方式二：手动部署

```bash
# 1. 构建并启动所有服务
docker-compose up -d

# 2. 查看服务状态
docker-compose ps

# 3. 查看秒杀服务日志
docker-compose logs -f seckill-service-1
```

## 4. 配置文件说明

### 4.1 docker-compose.yml

主配置文件，定义所有12个容器：

- **镜像服务**：MySQL、Redis、RabbitMQ、Elasticsearch、Kibana、Nacos、Kafka
- **构建服务**：Gateway、User-Service、Product-Service、Order-Service、Seckill-Service（Nginx）
- 所有微服务通过 `build: ./<service-name>` 自动构建镜像

### 4.2 环境变量（微服务）

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| SERVER_PORT | 服务端口 | 各服务不同 |
| MYSQL_HOST | MySQL Master地址 | mysql-master |
| MYSQL_PORT | MySQL端口 | 3306 |
| MYSQL_DATABASE | 数据库名 | 各服务不同 |
| MYSQL_USERNAME | 用户名 | root |
| MYSQL_PASSWORD | 密码 | 123456 |
| MYSQL_SLAVE_HOST | MySQL Slave地址 | mysql-slave |
| REDIS_HOST | Redis地址 | redis |
| REDIS_PORT | Redis端口 | 6379 |
| RABBITMQ_HOST | RabbitMQ地址 | rabbitmq |
| RABBITMQ_PORT | RabbitMQ端口 | 5672 |
| ELASTICSEARCH_HOST | ES地址 | elasticsearch |
| ELASTICSEARCH_PORT | ES端口 | 9200 |

## 5. 服务访问

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost | Nginx入口 |
| API统一入口 | http://localhost:8080 | Gateway统一入口 |
| Nacos控制台 | http://localhost:8848/nacos | 用户名/密码: nacos/nacos |
| Kibana | http://localhost:5601 | ES可视化 |
| RabbitMQ管理 | http://localhost:15672 | 用户名/密码: guest/guest |
| Elasticsearch | http://localhost:9200 | ES HTTP接口 |
| Kafka | localhost:9092 | Kafka broker |

## 6. 数据库初始化

### 6.1 自动初始化

Docker Compose启动MySQL Master时自动执行 `docker/init.sql`，创建：
- 5个数据库（flash_sale_user/product/order、flash_sale、nacos_config）
- 所有业务表（t_user、t_product、t_order、seckill_activity等）
- 测试数据（用户、商品、秒杀活动）

### 6.2 手动执行

```bash
docker exec -it flash-sale-mysql-master mysql -uroot -p123456
source /docker-entrypoint-initdb.d/init.sql
```

## 7. Docker网络

所有容器通过 `app` bridge网络互联，容器间通信使用服务名：

```yaml
# 正确写法
MYSQL_HOST: mysql-master      # 使用服务名
REDIS_HOST: redis             # 使用服务名

# 错误写法
MYSQL_HOST: localhost         # 不应在容器内使用localhost
MYSQL_HOST: 127.0.0.1        # 不应使用IP
```

## 8. 数据持久化

所有数据目录通过Docker Volume持久化：

```yaml
volumes:
  mysql_master_data:  # MySQL Master数据
  mysql_slave_data:   # MySQL Slave数据
  redis_data:        # Redis持久化
  es_data:           # Elasticsearch数据
  rabbitmq_data:    # RabbitMQ数据
  kafka_data:        # Kafka数据
```

## 9. 日志管理

```bash
# 查看所有日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f gateway
docker-compose logs -f seckill-service-1

# 查看最近100行
docker-compose logs --tail=100 seckill-service-1
```

## 10. 常见问题

### 10.1 镜像拉取失败

配置国内镜像加速器（Docker Desktop → Settings → Docker Engine）：

```json
{
    "registry-mirrors": [
        "https://docker.m.daocloud.io",
        "https://dockerproxy.cn"
    ]
}
```

### 10.2 端口冲突

检查并修改 `docker-compose.yml` 中的端口映射。

### 10.3 MySQL启动慢

MySQL首次初始化需要约30-60秒，脚本已配置健康检查自动等待。
