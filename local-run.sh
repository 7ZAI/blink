#!/bin/bash
# WSL 低内存环境本地开发启动脚本
# 用法: ./local-run.sh <模块名> [--console]
# 示例:
#   ./local-run.sh blink-base-app          # 后台运行，日志写入文件
#   ./local-run.sh blink-base-app --console # 终端运行，日志同时输出到终端和文件
#   ./local-run.sh base-admin-fe           # 前端后台运行
#   ./local-run.sh base-admin-fe --console # 前端终端运行

MODULE=$1
MODE=$2

if [ -z "$MODULE" ]; then
    echo "用法: ./local-run.sh <模块名> [--console]"
    echo ""
    echo "可用模块:"
    echo "  后端服务:"
    echo "    blink-base-app         - RBAC 后台管理服务 (端口: 8001)"
    echo "    gateway-admin          - Gateway 管理服务 (端口: 8008)"
    echo "    blink-gateway-reactive - 响应式网关 (端口: 8080)"
    echo ""
    echo "  前端服务:"
    echo "    base-admin-fe          - Base 管理前端"
    echo "    gateway-admin-fe       - Gateway 管理前端"
    echo ""
    echo "运行模式:"
    echo "  默认        - 后台运行，日志仅写入文件"
    echo "  --console   - 终端运行，日志同时输出到终端和文件"
    echo ""
    echo "日志文件位置:"
    echo "  blink-base-app         -> logs/blink-base-app.log"
    echo "  gateway-admin          -> logs/gateway-admin.log"
    echo "  blink-gateway-reactive -> logs/gateway-reactive.log"
    echo "  base-admin-fe          -> logs/base-admin-fe.log"
    echo "  gateway-admin-fe       -> logs/gateway-admin-fe.log"
    exit 1
fi

# 创建日志目录
LOG_DIR="logs"
mkdir -p "$LOG_DIR"

# 获取项目根目录的绝对路径
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

# 根据模块名确定日志文件路径和启动命令
case $MODULE in
    # 后端服务
    "blink-base-app")
        LOG_FILE="$PROJECT_ROOT/$LOG_DIR/blink-base-app.log"
        GRADLE_MODULE=":blink-base:blink-base-app:bootRun"
        PORT=8001
        IS_FRONTEND=false
        ;;
    "gateway-admin")
        LOG_FILE="$PROJECT_ROOT/$LOG_DIR/gateway-admin.log"
        GRADLE_MODULE=":blink-gateway:gateway-admin:bootRun"
        PORT=8008
        IS_FRONTEND=false
        ;;
    "blink-gateway-reactive")
        LOG_FILE="$PROJECT_ROOT/$LOG_DIR/gateway-reactive.log"
        GRADLE_MODULE=":blink-gateway:blink-gateway-reactive:bootRun"
        PORT=8080
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
        exit 1
        ;;
esac

# 清空之前的日志文件
echo "清空之前的日志文件: $LOG_FILE"
> "$LOG_FILE"

# JVM 内存参数 (适合 WSL 低内存环境)
JVM_OPTS="-Xmx384m -Xms128m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

echo "========================================"
echo "启动模块: $MODULE"
if [ "$IS_FRONTEND" = false ]; then
    echo "端口: $PORT"
    echo "JVM 参数: $JVM_OPTS"
fi
echo "日志文件: $LOG_FILE"
echo "========================================"

# 判断运行模式
if [ "$MODE" == "--console" ]; then
    # 终端模式：日志同时输出到终端和文件
    echo ""
    echo "运行模式: 终端模式 (Ctrl+C 停止)"
    echo ""
    if [ "$IS_FRONTEND" = true ]; then
        # 前端终端模式
        cd "$FRONTEND_DIR" && npm run dev 2>&1 | tee "$LOG_FILE"
    else
        # 后端终端模式
        ./gradlew "$GRADLE_MODULE" --no-daemon -Dorg.gradle.jvmargs="-Xmx256m" --args="$JVM_OPTS" 2>&1 | tee "$LOG_FILE"
    fi
else
    # 后台模式：日志仅写入文件
    echo ""
    echo "运行模式: 后台模式"
    echo ""
    echo "提示:"
    echo "  查看日志: tail -f $LOG_FILE"
    echo "  停止应用: kill \$(cat logs/$MODULE.pid)"
    echo ""

    if [ "$IS_FRONTEND" = true ]; then
        # 前端后台模式
        cd "$FRONTEND_DIR" && nohup npm run dev > "$LOG_FILE" 2>&1 &
    else
        # 后端后台模式
        nohup ./gradlew "$GRADLE_MODULE" --no-daemon -Dorg.gradle.jvmargs="-Xmx256m" --args="$JVM_OPTS" > "$LOG_FILE" 2>&1 &
    fi

    # 保存 PID 到文件
    PID_FILE="$PROJECT_ROOT/$LOG_DIR/$MODULE.pid"
    echo $! > "$PID_FILE"

    echo "应用已在后台启动，PID: $(cat $PID_FILE)"
    echo ""
    echo "等待应用启动..."

    # 等待并显示启动状态
    sleep 3
    echo ""
    echo "最近日志:"
    echo "----------------------------------------"
    tail -n 20 "$LOG_FILE"
    echo "----------------------------------------"
fi