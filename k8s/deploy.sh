#!/bin/bash
# K8s 部署脚本

echo "=========================================="
echo "  K8s 部署脚本"
echo "=========================================="
echo ""

# 切换到k8s目录
cd "$(dirname "$0")"

# 部署
kubectl apply -f namespace.yaml
kubectl apply -f namespace.yaml
kubectl apply -f app-deployment.yaml
kubectl apply -f mysql-statefulset.yaml
kubectl apply -f redis-deployment.yaml
kubectl apply -f hpa.yaml
kubectl apply -f ingress.yaml

echo ""
echo "=========================================="
echo "  部署完成"
echo "=========================================="
echo ""
echo "查看状态:"
echo "  kubectl get pods -n flash-sale"
echo "  kubectl get svc -n flash-sale"
echo "  kubectl get ingress -n flash-sale"
