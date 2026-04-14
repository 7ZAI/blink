#!/bin/bash
# WSL 低内存环境本地开发停止脚本
# 用法: ./local-stop.sh <模块名> [--all]
# 示例:
#   ./local-stop.sh blink-base-app   # 停止指定模块
#   ./local-stop.sh --all            # 停止所有后台运行的服务

LOG_DIR="logs"
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

# 偳止单个服务的函数
stop_service() {
    local MODULE=$1
    local PID_FILE="$PROJECT_ROOT/$LOG_DIR/$MODULE.pid"
    local LOG_FILE="$PROJECT_ROOT/$LOG_DIR/${MODULE}.log"

    if [ ! -f "$PID_FILE" ]; then
        echo "[WARN] $MODULE 的 PID 文件不存在: $PID_FILE"
        echo "       可能未在后台运行，或已手动停止"
        return 1
    fi

    local PID=$(cat "$PID_FILE")

    if [ -z "$PID" ]; then
        echo "[WARN] $MODULE PID 文件为空"
        rm -f "$PID_FILE"
        return 1
    fi

    # 检查进程是否存在
    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo "[WARN] $MODULE 进程已不存在 (PID: $PID)"
        rm -f "$PID_FILE"
        return 1
    fi

    echo "[INFO] 正在停止 $MODULE (PID: $PID)..."

    # 发送 SIGTERM 信号优雅停止
    kill "$PID" 2>/dev/null

    # 等待进程结束
    local WAIT_COUNT=0
    local MAX_WAIT=10

    while ps -p "$PID" > /dev/null 2>&1; do
        sleep 1
        WAIT_COUNT=$((WAIT_COUNT + 1))

        if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
            echo "[WARN] $MODULE 未在 $MAX_WAIT 秒内停止，强制终止..."
            kill -9 "$PID" 2>/dev/null
            sleep 1
            break
        fi
    done

    # 清理 PID 文件
    rm -f "$PID_FILE"

    # 对于后端服务，还需要清理可能残留的 Gradle daemon 进程
    case $MODULE in
        "blink-base-app"|"gateway-admin"|"blink-gateway-reactive")
            # 尝试停止该模块相关的 Gradle daemon
            echo "[INFO] 检查并清理 Gradle daemon..."
            ./gradlew --stop 2>/dev/null || true
            ;;
    esac

    echo "[OK] $MODULE 已停止"
    return 0
}

# 停止所有后台服务
stop_all() {
    echo "========================================"
    echo "停止所有后台运行的服务"
    echo "========================================"

    local MODULES=("blink-base-app" "gateway-admin" "blink-gateway-reactive" "base-admin-fe" "gateway-admin-fe")
    local STOPPED=0
    local FAILED=0

    for MODULE in "${MODULES[@]}"; do
        stop_service "$MODULE"
        if [ $? -eq 0 ]; then
            STOPPED=$((STOPPED + 1))
        else
            FAILED=$((FAILED + 1))
        fi
    done

    echo ""
    echo "========================================"
    echo "已停止: $STOPPED 个服务"
    echo "未运行: $FAILED 个服务"
    echo "========================================"
}

# 主逻辑
if [ "$1" == "--all" ]; then
    stop_all
elif [ -n "$1" ]; then
    MODULE=$1

    # 验证模块名
    case $MODULE in
        "blink-base-app"|"gateway-admin"|"blink-gateway-reactive"|"base-admin-fe"|"gateway-admin-fe")
            stop_service "$MODULE"
            ;;
        *)
            echo "未知模块: $MODULE"
            echo ""
            echo "可用模块:"
            echo "  后端服务:"
            echo "    blink-base-app         - RBAC 后台管理服务"
            echo "    gateway-admin          - Gateway 管理服务"
            echo "    blink-gateway-reactive - 响应式网关"
            echo ""
            echo "  前端服务:"
            echo "    base-admin-fe          - Base 管理前端"
            echo "    gateway-admin-fe       - Gateway 管理前端"
            echo ""
            echo "用法: ./local-stop.sh <模块名> [--all]"
            exit 1
            ;;
    esac
else
    echo "用法: ./local-stop.sh <模块名> [--all]"
    echo ""
    echo "可用模块:"
    echo "  后端服务:"
    echo "    blink-base-app         - RBAC 后台管理服务"
    echo "    gateway-admin          - Gateway 管理服务"
    echo "    blink-gateway-reactive - 响应式网关"
    echo ""
    echo "  前端服务:"
    echo "    base-admin-fe          - Base 管理前端"
    echo "    gateway-admin-fe       - Gateway 管理前端"
    echo ""
    echo "选项:"
    echo "  --all    停止所有后台运行的服务"
    echo ""
    echo "示例:"
    echo "  ./local-stop.sh blink-base-app   # 停止指定模块"
    echo "  ./local-stop.sh --all            # 停止所有服务"
    exit 1
fi