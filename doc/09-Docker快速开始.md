# Docker快速开始

> 本文档帮助你在5分钟内启动完整的分布式秒杀系统（12个容器）

## 前置要求

- Docker Desktop 已安装并运行
- 8GB+ 可用内存（推荐16GB）
- 国内网络正常（或已配置镜像加速器）

## 启动步骤

### 步骤1：配置镜像加速（可选但推荐）

打开 Docker Desktop → Settings → Docker Engine，添加：

```json
{
    "registry-mirrors": [
        "https://docker.m.daocloud.io",
        "https://dockerproxy.cn"
    ]
}
```

重启Docker Desktop使配置生效。

### 步骤2：一键启动

```bash
cd g:\sale_system\flash-sale-system

# Windows PowerShell
.\docker-start.sh

# Linux/Mac
bash ./docker-start.sh
```

> 首次构建约需5-10分钟（需编译Java项目为Docker镜像）

### 步骤3：等待服务就绪

```bash
docker-compose ps
```

所有服务状态变为 `healthy` 后即可访问（约1-2分钟）。

### 步骤4：验证部署

| 服务 | 验证地址 | 预期响应 |
|------|---------|---------|
| 前端页面 | http://localhost | 秒杀页面HTML |
| API统一入口 | http://localhost:8080/actuator/health | Gateway健康检查 |
| 用户服务 | http://localhost:8081/actuator/health | User服务健康检查 |
| 商品服务 | http://localhost:8083/actuator/health | Product服务健康检查 |
| 秒杀服务 | http://localhost:8086/actuator/health | Seckill服务健康检查 |
| RabbitMQ | http://localhost:15672 | 管理界面 |
| Nacos | http://localhost:8848/nacos | 登录页面 |
| ES | http://localhost:9200 | JSON响应 |

## 测试接口

### 通过Gateway（推荐）

```bash
# 用户登录
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test001","password":"123456"}'

# 商品列表
curl http://localhost:8080/api/products

# 秒杀活动列表
curl http://localhost:8080/api/seckill/activities

# 秒杀下单
curl -X POST http://localhost:8080/api/seckill/seckill?userId=1 \
  -H "Content-Type: application/json" \
  -d '{"activityId":1,"productId":1,"quantity":1}'

# 初始化秒杀库存（必须先执行）
curl -X POST http://localhost:8080/api/seckill/activities/init-all-stock

# ES搜索
curl "http://localhost:8080/api/search?q=iPhone"
```

## 停止服务

```bash
# 停止所有服务（保留数据）
docker-compose stop

# 停止并删除容器（保留数据卷）
docker-compose down

# 完全清理（包括数据卷）
docker-compose down -v
```

## 快速故障排查

| 问题 | 解决方案 |
|------|---------|
| 镜像拉取失败 | 配置镜像加速器 |
| 端口被占用 | 修改docker-compose.yml中的端口 |
| 服务启动失败 | 查看日志：`docker-compose logs [服务名]` |
| 数据库连接失败 | 等待MySQL完全启动后再试（约60秒）|
| 秒杀下单失败 | 先调用 `/api/seckill/activities/init-all-stock` 初始化Redis库存 |

## 文件说明

| 文件 | 用途 |
|------|------|
| `docker-start.sh` | 一键启动脚本（自动构建+启动+等待） |
| `docker-compose.yml` | 容器编排配置（12个服务） |
| `docker/init.sql` | 数据库初始化脚本（自动执行） |
| `docker/mysql/*.cnf` | MySQL Master/Slave配置 |
| `nginx/nginx.conf` | Nginx配置（静态资源+Gateway代理） |
| `front-end/sale.html` | 秒杀前端页面 |
| `gateway/` | API网关服务 |
| `user-service/` | 用户服务 |
| `product-service/` | 商品服务 |
| `order-service/` | 订单服务 |
| `seckill-service/` | 秒杀服务 |
