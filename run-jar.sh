#!/bin/bash
# WSL 低内存环境运行已构建的 jar 文件
# 用法: ./run-jar.sh <jar路径> [端口]
# 示例: ./run-jar.sh blink-base/blink-base-app/build/libs/blink-base-app.jar 8080

JAR_PATH=$1
PORT=${2:-8080}

if [ -z "$JAR_PATH" ]; then
    echo "用法: ./run-jar.sh <jar路径> [端口]"
    echo "示例: ./run-jar.sh blink-base/blink-base-app/build/libs/blink-base-app.jar"
    exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
    echo "JAR 文件不存在: $JAR_PATH"
    echo "请先构建项目: ./gradlew :blink-base:blink-base-app:build -x test"
    exit 1
fi

# JVM 内存参数 (适合 WSL 低内存环境)
JVM_OPTS="-Xmx384m -Xms128m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

echo "========================================"
echo "WSL 低内存模式运行 JAR"
echo "JVM 参数: $JVM_OPTS"
echo "端口: $PORT"
echo "========================================"

java $JVM_OPTS -Dserver.port=$PORT -jar "$JAR_PATH"