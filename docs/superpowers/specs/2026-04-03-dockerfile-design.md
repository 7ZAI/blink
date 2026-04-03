# Dockerfile 设计文档

## 概述

为 `gateway-reactive` 和 `base-app` 模块添加 Dockerfile，用于开发测试环境部署。

## 目标

- 支持快速迭代部署
- 利用 Gradle 缓存优化构建速度
- 最小化运行时镜像体积
- 通过环境变量灵活配置外部服务地址

## 架构设计

### 多阶段构建

```
┌─────────────────────────────────────────────────────────────┐
│  阶段1: 构建阶段 (gradle:8.8-jdk17)                          │
│  - 复制 Gradle 配置文件                                      │
│  - 下载依赖 (独立缓存层)                                     │
│  - 执行 ./gradlew bootJar                                   │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│  阶段2: 运行阶段 (eclipse-temurin:17-jre)                    │
│  - 仅包含 JRE，体积更小                                      │
│  - 复制构建产物 (JAR)                                        │
│  - 通过环境变量配置外部服务                                  │
│  - 暴露端口，启动应用                                        │
└─────────────────────────────────────────────────────────────┘
```

### 网络配置

- 默认桥接网络模式
- 通过环境变量传入 Nacos/MySQL/Redis/Dubbo 地址
- 支持在不同环境（开发/测试/生产）中动态配置

## Dockerfile 文件位置

| 模块 | 文件路径 | 暴露端口 |
|------|----------|----------|
| gateway-reactive | `blink-gateway/blink-gateway-reactive/Dockerfile` | 8002 |
| base-app | `blink-base/blink-base-app/Dockerfile` | 8001 |

## 环境变量配置

### Spring Boot 环境变量命名规则

Spring Boot 自动将环境变量映射到配置属性：
- 属性名转大写
- 点号(.)和横线(-)转为下划线(_)
- 例: `spring.cloud.nacos.config.server-addr` → `SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR`

### gateway-reactive 环境变量

| 环境变量 | 对应配置 | 默认值 | 说明 |
|----------|----------|--------|------|
| `SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR` | `spring.cloud.nacos.config.server-addr` | `127.0.0.1:8848` | Nacos 配置中心地址 |
| `SPRING_CLOUD_NACOS_CONFIG_NAMESPACE` | `spring.cloud.nacos.config.namespace` | `94984ad7-b510-4ca4-bdcb-b6cdbd437dfb` | Nacos 命名空间 |
| `SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR` | `spring.cloud.nacos.discovery.server-addr` | `127.0.0.1:8848` | Nacos 服务发现地址 |
| `SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE` | `spring.cloud.nacos.discovery.namespace` | `94984ad7-b510-4ca4-bdcb-b6cdbd437dfb` | Nacos 命名空间 |
| `SPRING_DATA_REDIS_HOST` | `spring.data.redis.host` | `127.0.0.1` | Redis 地址 |
| `SPRING_DATA_REDIS_PORT` | `spring.data.redis.port` | `6379` | Redis 端口 |
| `SPRING_DATA_REDIS_PASSWORD` | `spring.data.redis.password` | `123456` | Redis 密码 |
| `DUBBO_REGISTRY_ADDRESS` | `dubbo.registry.address` | `nacos://127.0.0.1:8848` | Dubbo 注册中心地址 |
| `DUBBO_REGISTRY_NAMESPACE` | `dubbo.registry.namespace` | `94984ad7-b510-4ca4-bdcb-b6cdbd437dfb` | Dubbo 命名空间 |

### base-app 环境变量

| 环境变量 | 对应配置 | 默认值 | 说明 |
|----------|----------|--------|------|
| `SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR` | `spring.cloud.nacos.config.server-addr` | `127.0.0.1:8848` | Nacos 配置中心地址 |
| `SPRING_CLOUD_NACOS_CONFIG_NAMESPACE` | `spring.cloud.nacos.config.namespace` | `94984ad7-b510-4ca4-bdcb-b6cdbd437dfb` | Nacos 命名空间 |
| `SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR` | `spring.cloud.nacos.discovery.server-addr` | `127.0.0.1:8848` | Nacos 服务发现地址 |
| `SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE` | `spring.cloud.nacos.discovery.namespace` | `94984ad7-b510-4ca4-bdcb-b6cdbd437dfb` | Nacos 命名空间 |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:mysql://localhost:3306/blink...` | MySQL 连接地址 |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `root` | MySQL 用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `123456` | MySQL 密码 |
| `SPRING_DATA_REDIS_HOST` | `spring.data.redis.host` | `127.0.0.1` | Redis 地址 |
| `SPRING_DATA_REDIS_PORT` | `spring.data.redis.port` | `6379` | Redis 端口 |
| `SPRING_DATA_REDIS_PASSWORD` | `spring.data.redis.password` | `123456` | Redis 密码 |
| `DUBBO_REGISTRY_ADDRESS` | `dubbo.registry.address` | `nacos://127.0.0.1:8848` | Dubbo 注册中心地址 |
| `DUBBO_REGISTRY_NAMESPACE` | `dubbo.registry.namespace` | `94984ad7-b510-4ca4-bdcb-b6cdbd437dfb` | Dubbo 命名空间 |

## Dockerfile 模板

### gateway-reactive

```dockerfile
# ==================== 构建阶段 ====================
FROM gradle:8.8-jdk17 AS builder

WORKDIR /app

# 复制 Gradle 配置文件 (利用缓存)
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# 复制所有模块的 build.gradle
COPY blink-gateway/blink-gateway-reactive/build.gradle ./blink-gateway/blink-gateway-reactive/
COPY blink-framework-common/build.gradle ./blink-framework-common/
COPY blink-framework-validation/build.gradle ./blink-framework-validation/
COPY blink-redis-starter/build.gradle ./blink-redis-starter/
COPY blink-datasource-starter/build.gradle ./blink-datasource-starter/
COPY blink-web-starter/build.gradle ./blink-web-starter/
COPY blink-log-starter/build.gradle ./blink-log-starter/
COPY blink-base/blink-base-api-dubbo/build.gradle ./blink-base/blink-base-api-dubbo/
COPY blink-gateway/blink-gateway-admin-api-dubbo/build.gradle ./blink-gateway/blink-gateway-admin-api-dubbo/

# 下载依赖 (独立层，代码变动时复用)
RUN gradle dependencies --no-daemon --parallel || return 0

# 复制源代码
COPY blink-gateway/blink-gateway-reactive/src ./blink-gateway/blink-gateway-reactive/src
COPY blink-framework-common/src ./blink-framework-common/src
COPY blink-framework-validation/src ./blink-framework-validation/src
COPY blink-redis-starter/src ./blink-redis-starter/src
COPY blink-datasource-starter/src ./blink-datasource-starter/src
COPY blink-web-starter/src ./blink-web-starter/src
COPY blink-log-starter/src ./blink-log-starter/src
COPY blink-base/blink-base-api-dubbo/src ./blink-base/blink-base-api-dubbo/src
COPY blink-gateway/blink-gateway-admin-api-dubbo/src ./blink-gateway/blink-gateway-admin-api-dubbo/src

# 构建 JAR
RUN gradle :blink-gateway:blink-gateway-reactive:bootJar --no-daemon

# ==================== 运行阶段 ====================
FROM eclipse-temurin:17-jre

WORKDIR /app

# 复制构建产物
COPY --from=builder /app/blink-gateway/blink-gateway-reactive/build/libs/*.jar app.jar

# 暴露端口
EXPOSE 8002

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8002/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### base-app

```dockerfile
# ==================== 构建阶段 ====================
FROM gradle:8.8-jdk17 AS builder

WORKDIR /app

# 复制 Gradle 配置文件 (利用缓存)
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# 复制所有模块的 build.gradle
COPY blink-base/blink-base-app/build.gradle ./blink-base/blink-base-app/
COPY blink-framework-common/build.gradle ./blink-framework-common/
COPY blink-framework-validation/build.gradle ./blink-framework-validation/
COPY blink-redis-starter/build.gradle ./blink-redis-starter/
COPY blink-datasource-starter/build.gradle ./blink-datasource-starter/
COPY blink-web-starter/build.gradle ./blink-web-starter/
COPY blink-log-starter/build.gradle ./blink-log-starter/
COPY blink-base/blink-base-api-dubbo/build.gradle ./blink-base/blink-base-api-dubbo/

# 下载依赖 (独立层，代码变动时复用)
RUN gradle dependencies --no-daemon --parallel || return 0

# 复制源代码
COPY blink-base/blink-base-app/src ./blink-base/blink-base-app/src
COPY blink-framework-common/src ./blink-framework-common/src
COPY blink-framework-validation/src ./blink-framework-validation/src
COPY blink-redis-starter/src ./blink-redis-starter/src
COPY blink-datasource-starter/src ./blink-datasource-starter/src
COPY blink-web-starter/src ./blink-web-starter/src
COPY blink-log-starter/src ./blink-log-starter/src
COPY blink-base/blink-base-api-dubbo/src ./blink-base/blink-base-api-dubbo/src

# 构建 JAR
RUN gradle :blink-base:blink-base-app:bootJar --no-daemon

# ==================== 运行阶段 ====================
FROM eclipse-temurin:17-jre

WORKDIR /app

# 复制构建产物
COPY --from=builder /app/blink-base/blink-base-app/build/libs/*.jar app.jar

# 暴露端口
EXPOSE 8001

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8001/base/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 构建命令

```bash
# 构建 gateway-reactive 镜像 (在项目根目录执行)
docker build -t blink-gateway-reactive:1.0.0 -f blink-gateway/blink-gateway-reactive/Dockerfile .

# 构建 base-app 镜像 (在项目根目录执行)
docker build -t blink-base-app:1.0.0 -f blink-base/blink-base-app/Dockerfile .
```

## 运行命令

### gateway-reactive

```bash
docker run -d --name gateway-reactive \
  -p 8002:8002 \
  -e SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=192.168.1.100:8848 \
  -e SPRING_CLOUD_NACOS_CONFIG_NAMESPACE=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb \
  -e SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=192.168.1.100:8848 \
  -e SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb \
  -e SPRING_DATA_REDIS_HOST=192.168.1.100 \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e SPRING_DATA_REDIS_PASSWORD=yourpassword \
  -e DUBBO_REGISTRY_ADDRESS=nacos://192.168.1.100:8848 \
  -e DUBBO_REGISTRY_NAMESPACE=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb \
  blink-gateway-reactive:1.0.0
```

### base-app

```bash
docker run -d --name base-app \
  -p 8001:8001 \
  -e SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=192.168.1.100:8848 \
  -e SPRING_CLOUD_NACOS_CONFIG_NAMESPACE=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb \
  -e SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=192.168.1.100:8848 \
  -e SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://192.168.1.100:3306/blink?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=yourpassword \
  -e SPRING_DATA_REDIS_HOST=192.168.1.100 \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e SPRING_DATA_REDIS_PASSWORD=yourpassword \
  -e DUBBO_REGISTRY_ADDRESS=nacos://192.168.1.100:8848 \
  -e DUBBO_REGISTRY_NAMESPACE=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb \
  blink-base-app:1.0.0
```

## 预估镜像大小

| 阶段 | 大小 |
|------|------|
| 构建阶段 (builder) | ~800MB |
| 运行阶段 (最终镜像) | ~250MB |

## 依赖模块清单

### gateway-reactive 依赖模块
- blink-framework-common
- blink-framework-validation
- blink-redis-starter
- blink-datasource-starter (间接依赖)
- blink-web-starter
- blink-log-starter
- blink-base-api-dubbo
- blink-gateway-admin-api-dubbo

### base-app 依赖模块
- blink-framework-common
- blink-framework-validation
- blink-redis-starter
- blink-datasource-starter
- blink-web-starter
- blink-log-starter
- blink-base-api-dubbo

## 注意事项

1. Dockerfile 必须在项目根目录执行 (`docker build -f ... .`)
2. `gradle dependencies` 命令允许失败 (`|| return 0`)，因为部分模块可能缺少源码
3. 健康检查使用 `/actuator/health` 端点，两个模块都已配置 Spring Boot Actuator
4. 环境变量会覆盖配置文件中的默认值，无需修改 application.yml
5. 命名空间 ID 需要与 Nacos 中的实际配置一致

## 可选优化: .dockerignore 文件

建议在项目根目录创建 `.dockerignore` 文件以减少构建上下文大小：

```dockerignore
# Git
.git
.gitignore

# IDE
.idea
*.iml
.vscode

# Build outputs (不需要复制到构建上下文)
**/build
**/dist
**/target
**/out

# Logs
**/logs
*.log

# Node modules (前端项目)
**/node_modules

# Temporary files
*.tmp
*.temp
*.swp
*.bak

# OS files
.DS_Store
Thumbs.db

# Claude/Superpowers
.claude
.superpowers

# Documentation (不需要构建)
docs
*.md
!README.md
```

此优化可显著减少 Docker 构建上下文传输时间。