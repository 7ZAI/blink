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