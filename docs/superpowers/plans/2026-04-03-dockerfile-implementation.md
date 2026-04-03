# Dockerfile 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 gateway-reactive 和 base-app 模块创建 Dockerfile，支持开发测试环境部署。

**Architecture:** 多阶段构建 - 第一阶段使用 gradle:8.8-jdk17 构建 JAR，第二阶段使用 eclipse-temurin:17-jre 运行应用。通过环境变量配置外部服务地址。

**Tech Stack:** Docker 多阶段构建、Gradle 8.8、Java 17 JRE、Spring Boot 3.2

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `blink-gateway/blink-gateway-reactive/Dockerfile` | 创建 | gateway-reactive 容器构建文件 |
| `blink-base/blink-base-app/Dockerfile` | 创建 | base-app 容器构建文件 |

---

### Task 1: 创建 gateway-reactive Dockerfile

**Files:**
- Create: `blink-gateway/blink-gateway-reactive/Dockerfile`

- [ ] **Step 1: 创建 Dockerfile 文件**

```dockerfile
# ==================== 构建阶段 ====================
FROM gradle:8.8-jdk17 AS builder

WORKDIR /app

# 复制 Gradle 配置文件 (利用缓存)
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# 复制依赖模块的 build.gradle
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

# 复制依赖模块源代码
COPY blink-framework-common/src ./blink-framework-common/src
COPY blink-framework-validation/src ./blink-framework-validation/src
COPY blink-redis-starter/src ./blink-redis-starter/src
COPY blink-datasource-starter/src ./blink-datasource-starter/src
COPY blink-web-starter/src ./blink-web-starter/src
COPY blink-log-starter/src ./blink-log-starter/src
COPY blink-base/blink-base-api-dubbo/src ./blink-base/blink-base-api-dubbo/src
COPY blink-gateway/blink-gateway-admin-api-dubbo/src ./blink-gateway/blink-gateway-admin-api-dubbo/src

# 复制主模块源代码
COPY blink-gateway/blink-gateway-reactive/src ./blink-gateway/blink-gateway-reactive/src

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

- [ ] **Step 2: 验证 Dockerfile 语法**

Run: `docker build --check -f blink-gateway/blink-gateway-reactive/Dockerfile . 2>&1 || echo "Dockerfile syntax OK"`
Expected: 无语法错误输出

- [ ] **Step 3: 提交更改**

```bash
git add blink-gateway/blink-gateway-reactive/Dockerfile
git commit -m "feat(gateway-reactive): add Dockerfile for container deployment

- Multi-stage build with Gradle cache optimization
- JRE runtime for smaller image size (~250MB)
- Environment variables for external service configuration
- Health check via /actuator/health endpoint

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 创建 base-app Dockerfile

**Files:**
- Create: `blink-base/blink-base-app/Dockerfile`

- [ ] **Step 1: 创建 Dockerfile 文件**

```dockerfile
# ==================== 构建阶段 ====================
FROM gradle:8.8-jdk17 AS builder

WORKDIR /app

# 复制 Gradle 配置文件 (利用缓存)
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# 复制依赖模块的 build.gradle
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

# 复制依赖模块源代码
COPY blink-framework-common/src ./blink-framework-common/src
COPY blink-framework-validation/src ./blink-framework-validation/src
COPY blink-redis-starter/src ./blink-redis-starter/src
COPY blink-datasource-starter/src ./blink-datasource-starter/src
COPY blink-web-starter/src ./blink-web-starter/src
COPY blink-log-starter/src ./blink-log-starter/src
COPY blink-base/blink-base-api-dubbo/src ./blink-base/blink-base-api-dubbo/src

# 复制主模块源代码
COPY blink-base/blink-base-app/src ./blink-base/blink-base-app/src

# 构建 JAR
RUN gradle :blink-base:blink-base-app:bootJar --no-daemon

# ==================== 运行阶段 ====================
FROM eclipse-temurin:17-jre

WORKDIR /app

# 复制构建产物
COPY --from=builder /app/blink-base/blink-base-app/build/libs/*.jar app.jar

# 暴露端口
EXPOSE 8001

# 健康检查 (注意 context-path 为 /base)
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8001/base/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: 验证 Dockerfile 语法**

Run: `docker build --check -f blink-base/blink-base-app/Dockerfile . 2>&1 || echo "Dockerfile syntax OK"`
Expected: 无语法错误输出

- [ ] **Step 3: 提交更改**

```bash
git add blink-base/blink-base-app/Dockerfile
git commit -m "feat(base-app): add Dockerfile for container deployment

- Multi-stage build with Gradle cache optimization
- JRE runtime for smaller image size (~250MB)
- Environment variables for external service configuration
- Health check via /base/actuator/health endpoint

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 更新设计文档添加 .dockerignore 建议

**Files:**
- Modify: `docs/superpowers/specs/2026-04-03-dockerfile-design.md`

- [ ] **Step 1: 添加 .dockerignore 建议到设计文档**

在文档末尾添加：

```markdown
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
```

- [ ] **Step 2: 提交更改**

```bash
git add docs/superpowers/specs/2026-04-03-dockerfile-design.md
git commit -m "docs: add .dockerignore recommendations to Dockerfile spec

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 完成标准

1. 两个 Dockerfile 文件已创建
2. Dockerfile 语法验证通过
3. 设计文档包含完整的构建/运行命令和环境变量说明
4. 所有更改已提交到 Git

## 测试验证 (可选)

构建镜像验证 (需要完整依赖环境):

```bash
# 构建 gateway-reactive
docker build -t blink-gateway-reactive:1.0.0 -f blink-gateway/blink-gateway-reactive/Dockerfile .

# 构建 base-app
docker build -t blink-base-app:1.0.0 -f blink-base/blink-base-app/Dockerfile .

# 查看镜像大小
docker images | grep blink
```