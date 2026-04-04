# Kubernetes集群部署

## 1. 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                       │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │   Node1   │  │   Node2  │  │   Node3  │                  │
│  │ (Master)  │  │ (Worker) │  │ (Worker) │                  │
│  │           │  │           │  │           │                  │
│  │ ┌───────┐ │  │ ┌───────┐ │  │ ┌───────┐ │                  │
│  │ │Nginx  │ │  │ │Service│ │  │ │Service│ │                  │
│  │ └───────┘ │  │ └───────┘ │  │ └───────┘ │                  │
│  │ ┌───────┐ │  │ ┌───────┐ │  │ ┌───────┐ │                  │
│  │ │ MySQL │ │  │ │ Redis │ │  │ │  MQ   │ │                  │
│  │ └───────┘ │  │ └───────┘ │  │ └───────┘ │                  │
│  └──────────┘  └──────────┘  └──────────┘                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 2. 部署文件

所有K8s配置文件位于 `k8s/` 目录：

```
k8s/
├── namespace.yaml           # 命名空间定义
├── configmap.yaml           # 应用配置
├── secret.yaml              # 密钥配置
├── mysql-statefulset.yaml   # MySQL有状态部署
├── redis-deployment.yaml    # Redis部署
├── kafka-statefulset.yaml  # Kafka部署
├── app-deployment.yaml     # 应用Deployment
├── app-service.yaml        # 应用Service
├── hpa.yaml                 # 自动扩缩容
├── ingress.yaml             # Ingress配置
└── deploy.sh                # 部署脚本
```

## 3. 快速部署

```bash
cd k8s

# 1. 创建命名空间
kubectl apply -f namespace.yaml

# 2. 创建配置和密钥
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

# 3. 部署基础设施
kubectl apply -f mysql-statefulset.yaml
kubectl apply -f redis-deployment.yaml
kubectl apply -f kafka-statefulset.yaml

# 4. 部署应用
kubectl apply -f app-deployment.yaml
kubectl apply -f app-service.yaml

# 5. 配置Ingress
kubectl apply -f ingress.yaml

# 6. 配置自动扩缩容
kubectl apply -f hpa.yaml
```

## 4. 核心资源定义

### 4.1 Namespace

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: flash-sale
```

### 4.2 ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: seckill-config
  namespace: flash-sale
data:
  SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/flash_sale"
  REDIS_HOST: "redis"
  RABBITMQ_HOST: "rabbitmq"
```

### 4.3 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: seckill-service
  namespace: flash-sale
spec:
  replicas: 3
  selector:
    matchLabels:
      app: seckill-service
  template:
    spec:
      containers:
      - name: seckill-service
        image: seckill-service:latest
        ports:
        - containerPort: 8086
```

### 4.4 Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: seckill-service
  namespace: flash-sale
spec:
  selector:
    app: seckill-service
  ports:
  - port: 8086
    targetPort: 8086
  type: ClusterIP
```

### 4.5 HPA自动扩缩容

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: seckill-service-hpa
  namespace: flash-sale
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: seckill-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 4.6 Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: flash-sale-ingress
  namespace: flash-sale
spec:
  rules:
  - host: flash-sale.example.com
    http:
      paths:
      - path: /api
        backend:
          service:
            name: gateway
            port:
              number: 8080
```

## 5. 运维命令

```bash
# 查看Pod状态
kubectl get pods -n flash-sale

# 查看日志
kubectl logs -f deployment/seckill-service -n flash-sale

# 进入Pod
kubectl exec -it <pod-name> -n flash-sale -- /bin/bash

# 扩容
kubectl scale deployment seckill-service --replicas=5 -n flash-sale

# 更新镜像
kubectl set image deployment/seckill-service seckill-service=seckill-service:v2 -n flash-sale

# 查看资源使用
kubectl top pods -n flash-sale
```

## 6. 生产环境注意事项

1. **使用持久化存储**：生产环境应使用PV/PVC而非hostPath
2. **配置资源限制**：为每个容器设置CPU/内存限制
3. **健康检查**：配置livenessProbe和readinessProbe
4. **配置反亲和性**：将Pod分散到不同节点
5. **使用Secret**：数据库密码等敏感信息使用Secret
