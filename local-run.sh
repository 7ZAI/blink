#!/bin/bash
# WSL 低内存环境本地开发启动脚本
# 用法: ./local-run.sh <模块名>
# 示例:
#   ./local-run.sh blink-base-app          # 后台运行，日志写入文件
#   ./local-run.sh base-admin-fe           # 前端后台运行
#   ./local-run.sh --all                   # 后台启动所有服务

LOG_DIR="logs"
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

# 创建日志目录
mkdir -p "$LOG_DIR"

# JVM 内存参数 (限制最大内存 512MB)
# 堆内存 256MB + 元空间 256MB + 直接内存 64MB ≈ 576MB
# 元空间需要足够大以加载 Spring Boot 框架类
JVM_OPTS="-Xmx256m -Xms128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -XX:MaxDirectMemorySize=64m"

# 启动单个服务的函数
start_service() {
    local MODULE=$1
    local LOG_FILE
    local GRADLE_MODULE
    local PORT
    local JAR_FILE
    local FRONTEND_DIR
    local IS_FRONTEND=false

    case $MODULE in
        # 后端服务
        "blink-base-app")
            LOG_FILE="$PROJECT_ROOT/$LOG_DIR/blink-base-app.log"
            GRADLE_MODULE=":blink-base:blink-base-app"
            JAR_FILE="$PROJECT_ROOT/blink-base/blink-base-app/build/libs/blink-base-app-1.0.0-SNAPSHOT.jar"
            PORT=8001
            IS_FRONTEND=false
            ;;
        "gateway-admin")
            LOG_FILE="$PROJECT_ROOT/$LOG_DIR/gateway-admin.log"
            GRADLE_MODULE=":blink-gateway:gateway-admin"
            JAR_FILE="$PROJECT_ROOT/blink-gateway/gateway-admin/build/libs/gateway-admin-1.0.0-SNAPSHOT.jar"
            PORT=8008
            IS_FRONTEND=false
            ;;
        "blink-gateway-reactive")
            LOG_FILE="$PROJECT_ROOT/$LOG_DIR/gateway-reactive.log"
            GRADLE_MODULE=":blink-gateway:blink-gateway-reactive"
            JAR_FILE="$PROJECT_ROOT/blink-gateway/blink-gateway-reactive/build/libs/blink-gateway-reactive-1.0.0-SNAPSHOT.jar"
            PORT=8002
            IS_FRONTEND=false
            ;;
        # 前端服务
        "base-admin-fe")
            LOG_FILE="$PROJECT_ROOT/$LOG_DIR/base-admin-fe.log"
            FRONTEND_DIR="$PROJECT_ROOT/frontend/packages/base-admin"
            IS_FRONTEND=true
            ;;
        "gateway-admin-fe")
            LOG_FILE="$PROJECT_ROOT/$LOG_DIR/gateway-admin-fe.log"
            FRONTEND_DIR="$PROJECT_ROOT/frontend/packages/gateway-admin"
            IS_FRONTEND=true
            ;;
        *)
            echo "未知模块: $MODULE"
            return 1
            ;;
    esac

    # 清空之前的日志文件
    echo "清空之前的日志文件: $LOG_FILE"
    > "$LOG_FILE"

    echo "========================================"
    echo "启动模块: $MODULE"
    if [ "$IS_FRONTEND" = false ]; then
        echo "端口: $PORT"
        echo "JVM 参数: $JVM_OPTS"
    fi
    echo "日志文件: $LOG_FILE"
    echo "========================================"
    echo ""
    echo "运行模式: 后台模式"
    echo ""

    if [ "$IS_FRONTEND" = true ]; then
        # 前端后台模式
        cd "$FRONTEND_DIR" && nohup npm run dev > "$LOG_FILE" 2>&1 &
    else
        # 后端后台模式：先构建再运行
        echo "正在构建..."
        ./gradlew "${GRADLE_MODULE}:build" -x test 2>&1 | tee "$LOG_FILE"
        # 运行日志追加到构建日志后面
        nohup java $JVM_OPTS -jar "$JAR_FILE" >> "$LOG_FILE" 2>&1 &
    fi

    # 保存 PID 到文件
    PID_FILE="$PROJECT_ROOT/$LOG_DIR/$MODULE.pid"
    echo $! > "$PID_FILE"

    echo "应用已在后台启动，PID: $(cat $PID_FILE)"
    echo ""
    echo "提示:"
    echo "  查看日志: tail -f $LOG_FILE"
    echo "  停止应用: ./local-stop.sh $MODULE"
    echo ""
    echo "等待应用启动..."
    sleep 2
    echo ""
    echo "最近日志:"
    echo "----------------------------------------"
    tail -n 15 "$LOG_FILE"
    echo "----------------------------------------"
}

# 启动所有后台服务
start_all() {
    echo "========================================"
    echo "后台启动所有服务"
    echo "========================================"
    echo ""

    # 按依赖顺序启动：先后端，再前端
    local BACKEND_MODULES=("blink-base-app" "gateway-admin" "blink-gateway-reactive")
    local FRONTEND_MODULES=("base-admin-fe" "gateway-admin-fe")

    echo "启动后端服务..."
    for MODULE in "${BACKEND_MODULES[@]}"; do
        echo ""
        start_service "$MODULE"
        sleep 2
    done

    echo ""
    echo "启动前端服务..."
    for MODULE in "${FRONTEND_MODULES[@]}"; do
        echo ""
        start_service "$MODULE"
        sleep 1
    done

    echo ""
    echo "========================================"
    echo "所有服务已后台启动"
    echo ""
    echo "服务列表:"
    echo "  blink-base-app         - RBAC 后台管理服务 (端口: 8001)"
    echo "  gateway-admin          - Gateway 管理服务 (端口: 8008)"
    echo "  blink-gateway-reactive - 响应式网关 (端口: 8080)"
    echo "  base-admin-fe          - Base 管理前端"
    echo "  gateway-admin-fe       - Gateway 管理前端"
    echo ""
    echo "日志文件位置: logs/"
    echo ""
    echo "操作命令:"
    echo "  查看所有日志: tail -f logs/*.log"
    echo "  停止所有服务: ./local-stop.sh --all"
    echo "========================================"
}

# 显示帮助信息
show_help() {
    echo "用法: ./local-run.sh <模块名>"
    echo "       ./local-run.sh --all"
    echo ""
    echo "可用模块:"
    echo "  后端服务:"
    echo "    blink-base-app         - RBAC 后台管理服务 (端口: 8001)"
    echo "    gateway-admin          - Gateway 管理服务 (端口: 8008)"
    echo "    blink-gateway-reactive - 响应式网关 (端口: 8002)"
    echo ""
    echo "  前端服务:"
    echo "    base-admin-fe          - Base 管理前端"
    echo "    gateway-admin-fe       - Gateway 管理前端"
    echo ""
    echo "  --all                   - 后台启动所有服务"
    echo ""
    echo "日志文件位置:"
    echo "  blink-base-app         -> logs/blink-base-app.log"
    echo "  gateway-admin          -> logs/gateway-admin.log"
    echo "  blink-gateway-reactive -> logs/gateway-reactive.log"
    echo "  base-admin-fe          -> logs/base-admin-fe.log"
    echo "  gateway-admin-fe       -> logs/gateway-admin-fe.log"
    exit 1
}

# 主逻辑
if [ -z "$1" ]; then
    show_help
fi

case "$1" in
    "--all")
        start_all
        ;;
    "blink-base-app"|"gateway-admin"|"blink-gateway-reactive"|"base-admin-fe"|"gateway-admin-fe")
        start_service "$1"
        ;;
    *)
        echo "未知模块: $1"
        echo ""
        show_help
        ;;
esac