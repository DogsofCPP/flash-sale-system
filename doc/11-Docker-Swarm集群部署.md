# Docker Swarm集群部署

## 1. Swarm架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Swarm Manager                      │
│                         (Leader)                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Orchestration & Scheduling                │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
          │                        │
          ▼                        ▼
┌──────────────────┐      ┌──────────────────┐
│  Worker Node 1   │      │  Worker Node 2   │
│                  │      │                  │
│  ┌────────────┐  │      │  ┌────────────┐  │
│  │  Service   │  │      │  │  Service   │  │
│  │ Replicas=3 │  │      │  │ Replicas=3 │  │
│  └────────────┘  │      │  └────────────┘  │
└──────────────────┘      └──────────────────┘
```

## 2. 集群初始化

```bash
# 在Manager节点初始化Swarm
docker swarm init --advertise-addr <MANAGER-IP>

# 在Worker节点加入集群
docker swarm join --token <TOKEN> <MANAGER-IP>:2377

# 查看节点状态
docker node ls
```

## 3. 部署服务

### 3.1 使用Stack部署

```bash
# 部署整个应用栈
docker stack deploy -c docker-compose.swarm.yml flash-sale

# 查看服务列表
docker service ls

# 查看服务状态
docker service ps flash-sale_seckill-service
```

### 3.2 docker-compose.swarm.yml

```yaml
version: "3.9"

services:
  seckill-service:
    image: seckill-service:latest
    ports:
      - "8086:8086"
    environment:
      - SPRING_PROFILES_ACTIVE=swarm
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: "1.0"
          memory: 1G
        reservations:
          cpus: "0.5"
          memory: 512M
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
    networks:
      - flash-sale-net

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    deploy:
      replicas: 2
    networks:
      - flash-sale-net

networks:
  flash-sale-net:
    driver: overlay
```

## 4. Swarm特有配置

### 4.1 全局服务

```yaml
nginx:
  image: nginx:alpine
  deploy:
    mode: global  # 每个节点运行一个实例
```

### 4.2 滚动更新

```yaml
seckill-service:
  image: seckill-service:v1
  deploy:
    update_config:
      parallelism: 1        # 每次更新1个副本
      delay: 10s            # 更新间隔
      failure_action: rollback
      monitor: 5s
```

### 4.3 健康检查

```yaml
seckill-service:
  image: seckill-service:v1
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8086/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
```

### 4.4 标签约束

```yaml
seckill-service:
  image: seckill-service:v1
  deploy:
    placement:
      constraints:
        - node.role == worker
        - node.labels.zone == production
```

## 5. 运维命令

```bash
# 扩容服务
docker service scale flash-sale_seckill-service=5

# 更新服务镜像
docker service update --image seckill-service:v2 flash-sale_seckill-service

# 回滚服务
docker service rollback flash-sale_seckill-service

# 查看服务日志
docker service logs flash-sale_seckill-service

# 查看节点任务
docker service ps flash-sale_seckill-service

# 移除整个栈
docker stack rm flash-sale
```

## 6. Swarm vs Kubernetes

| 特性 | Docker Swarm | Kubernetes |
|------|-------------|------------|
| 学习曲线 | 简单 | 陡峭 |
| 功能丰富度 | 基础 | 强大 |
| 社区生态 | 一般 | 庞大 |
| 适合场景 | 小规模/简单 | 大规模/复杂 |
| YAML语法 | 兼容Compose | K8s原生 |
| 自动扩缩容 | 手动或第三方 | 原生支持 |
| Ingress | Labels | Ingress Controller |
| 持久化存储 | Volumes | PV/PVC |
