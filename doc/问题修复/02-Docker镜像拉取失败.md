# Docker镜像拉取失败

## 1. 问题描述

执行 `docker-compose up -d` 时，部分镜像拉取失败：

```
Error failed to resolve reference "docker.io/confluentinc/cp-kafka:7.5.0"
```

## 2. 原因分析

- Docker Hub访问受限（国内网络）
- 镜像仓库地址被墙
- DNS解析失败

## 3. 解决方案

### 方案一：配置国内镜像加速器（推荐）

#### Windows (Docker Desktop)

1. 点击系统托盘Docker图标 → Settings
2. 选择 Docker Engine
3. 编辑 JSON 配置：

```json
{
    "builder": {
        "gc": {
            "defaultKeepStorage": "20GB",
            "enabled": true
        }
    },
    "experimental": false,
    "features": {
        "buildkit": true
    },
    "registry-mirrors": [
        "https://docker.m.daocloud.io",
        "https://dockerproxy.cn",
        "https://mirror.ccs.tencentyun.com"
    ]
}
```

4. 点击 Apply & Restart

#### 验证配置

```powershell
docker info | Select-String "Registry Mirrors"
```

### 方案二：手动拉取镜像

```powershell
# 预先拉取可能失败的镜像
docker pull confluentinc/cp-kafka:7.5.0
docker pull confluentinc/cp-zookeeper:7.5.0
docker pull elasticsearch:8.12.0
docker pull kibana:8.12.0
docker pull nacos/nacos-server:v2.2.3
```

### 方案三：使用代理

在Docker Desktop配置代理：
```json
{
    "proxies": {
        "default": {
            "httpProxy": "http://your-proxy:port",
            "httpsProxy": "http://your-proxy:port"
        }
    }
}
```

### 方案四：使用阿里云镜像加速器

1. 登录阿里云容器镜像服务
2. 获取专属镜像加速地址
3. 配置到Docker Daemon

## 4. 常见镜像加速地址

| 提供商 | 地址 |
|--------|------|
| DaoCloud | https://docker.m.daocloud.io |
| DockerProxy | https://dockerproxy.cn |
| 腾讯云 | https://mirror.ccs.tencentyun.com |
| 阿里云 | <你的专属地址> |
| 华为云 | https://<your-id>.mirror.swr.myhuaweicloud.com |

## 5. 验证

重新执行：
```powershell
cd g:\sale_system\flash-sale-system
docker-compose up -d
```
