#!/bin/bash
# K8s 部署脚本

cd "$(dirname "$0")"

echo "=========================================="
echo "  K8s 部署脚本"
echo "=========================================="
echo ""

echo "[1/5] 创建命名空间..."
kubectl apply -f namespace.yaml

echo "[2/5] 创建配置..."
kubectl apply -f configmap.yaml

echo "[3/5] 部署基础设施（MySQL/Redis/RabbitMQ/ES/Kafka/Nacos）..."
echo "提示：请先在基础设施节点部署，或使用 helm 安装"
echo "kubectl apply -f mysql-statefulset.yaml"
echo "kubectl apply -f redis-deployment.yaml"

echo "[4/5] 部署微服务..."
kubectl apply -f microservices.yaml

echo "[5/5] 配置自动扩缩容..."
kubectl apply -f hpa.yaml

echo ""
echo "=========================================="
echo "  部署完成"
echo "=========================================="
echo ""
echo "查看状态:"
echo "  kubectl get pods -n flash-sale"
echo "  kubectl get svc -n flash-sale"
echo "  kubectl get hpa -n flash-sale"
