#!/bin/bash
# ==========================================
# 编译检查脚本
# 检查项目编译状态和依赖
# ==========================================

echo "=========================================="
echo "  编译检查工具"
echo "=========================================="
echo ""

# 检查Java
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n1)
    echo -e "[OK] Java: $java_version"
else
    echo -e "[ERROR] Java 未安装"
fi

# 检查Maven
if command -v mvn &> /dev/null; then
    mvn_version=$(mvn -version 2>&1 | head -n1)
    echo -e "[OK] Maven: $mvn_version"
else
    echo -e "[ERROR] Maven 未安装"
fi

# 检查Docker
if command -v docker &> /dev/null; then
    docker_version=$(docker --version)
    echo -e "[OK] Docker: $docker_version"
else
    echo -e "[ERROR] Docker 未安装"
fi

# 检查docker-compose
if command -v docker-compose &> /dev/null; then
    compose_version=$(docker-compose --version)
    echo -e "[OK] Docker Compose: $compose_version"
else
    echo -e "[WARN] Docker Compose 未安装"
fi

echo ""

# 检查pom.xml
if [ -f "pom.xml" ]; then
    echo -e "[OK] pom.xml 存在"
    echo "    项目模块:"
    grep -oP '<artifactId>\K[^<]+' pom.xml | sed 's/^/      - /'
else
    echo -e "[WARN] pom.xml 不存在（单体架构）"
fi

echo ""

# 检查Dockerfile
if [ -f "Dockerfile" ]; then
    echo -e "[OK] Dockerfile 存在"
else
    echo -e "[WARN] Dockerfile 不存在"
fi

# 检查docker-compose.yml
if [ -f "docker-compose.yml" ]; then
    echo -e "[OK] docker-compose.yml 存在"
else
    echo -e "[WARN] docker-compose.yml 不存在"
fi

echo ""
echo "=========================================="
echo "  Maven编译测试"
echo "=========================================="
echo ""

# 尝试Maven编译
if command -v mvn &> /dev/null; then
    echo "执行编译检查..."
    mvn compile -q -DskipTests 2>&1 | tail -20

    if [ $? -eq 0 ]; then
        echo -e "[OK] 编译成功"
    else
        echo -e "[WARN] 编译存在警告或错误"
    fi
fi

echo ""
echo "=========================================="
echo "  检查完成"
echo "=========================================="
