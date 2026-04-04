#!/bin/bash
# Docker Swarm 集群初始化脚本

echo "=========================================="
echo "  Docker Swarm 集群初始化"
echo "=========================================="
echo ""

# 检查是否是Swarm manager
is_manager=$(docker inspect --format='{{.ManagerStatus.Leader}}' $(hostname) 2>/dev/null)

if [ -z "$is_manager" ]; then
    echo "此节点不是Swarm Manager"
    echo "请在Manager节点运行此脚本"
    exit 1
fi

echo "初始化Swarm集群..."
docker swarm init --advertise-addr $(hostname -I | awk '{print $1}')

echo ""
echo "=========================================="
echo "  集群初始化完成"
echo "=========================================="
echo ""
echo "添加Worker节点："
echo "  docker swarm join-token worker"
echo ""
echo "查看节点："
echo "  docker node ls"
echo ""
