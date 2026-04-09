#!/bin/bash
# WSL 低内存环境本地开发启动脚本
# 用法: ./local-run.sh <模块名>
# 示例: ./local-run.sh blink-base-app

MODULE=$1

if [ -z "$MODULE" ]; then
    echo "用法: ./local-run.sh <模块名>"
    echo "可用模块:"
    echo "  blink-base-app       - RBAC 后台管理服务"
    echo "  gateway-admin        - Gateway 管理服务"
    echo "  blink-gateway-reactive - 响应式网关"
    exit 1
fi

# JVM 内存参数 (适合 WSL 低内存环境)
# -Xmx384m: 最大堆内存 384MB
# -Xms128m: 初始堆内存 128MB
# -XX:MaxMetaspaceSize=128m: 最大元空间 128MB
# -XX:+UseG1GC: 使用 G1 垃圾收集器 (内存效率更高)
JVM_OPTS="-Xmx384m -Xms128m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

echo "========================================"
echo "WSL 低内存模式启动: $MODULE"
echo "JVM 参数: $JVM_OPTS"
echo "========================================"

case $MODULE in
    "blink-base-app")
        ./gradlew :blink-base:blink-base-app:bootRun --no-daemon -Dorg.gradle.jvmargs="-Xmx256m" $JVM_OPTS
        ;;
    "gateway-admin")
        ./gradlew :blink-gateway:gateway-admin:bootRun --no-daemon -Dorg.gradle.jvmargs="-Xmx256m" $JVM_OPTS
        ;;
    "blink-gateway-reactive")
        ./gradlew :blink-gateway:blink-gateway-reactive:bootRun --no-daemon -Dorg.gradle.jvmargs="-Xmx256m" $JVM_OPTS
        ;;
    *)
        echo "未知模块: $MODULE"
        exit 1
        ;;
esac