#!/bin/bash
# WSL 低内存环境本地开发启动脚本
# 用法: ./local-run.sh <模块名> [--console]
# 示例:
#   ./local-run.sh blink-base-app          # 后台运行，日志写入文件
#   ./local-run.sh blink-base-app --console # 终端运行，日志同时输出到终端和文件

MODULE=$1
MODE=$2

if [ -z "$MODULE" ]; then
    echo "用法: ./local-run.sh <模块名> [--console]"
    echo ""
    echo "可用模块:"
    echo "  blink-base-app         - RBAC 后台管理服务"
    echo "  gateway-admin          - Gateway 管理服务"
    echo "  blink-gateway-reactive - 响应式网关"
    echo ""
    echo "运行模式:"
    echo "  默认        - 后台运行，日志仅写入文件"
    echo "  --console   - 终端运行，日志同时输出到终端和文件"
    echo ""
    echo "日志文件位置:"
    echo "  blink-base-app         -> logs/blink-base-app.log"
    echo "  gateway-admin          -> logs/gateway-admin.log"
    echo "  blink-gateway-reactive -> logs/gateway-reactive.log"
    exit 1
fi

# 创建日志目录
LOG_DIR="logs"
mkdir -p "$LOG_DIR"

# 根据模块名确定日志文件路径和 Gradle 模块路径
case $MODULE in
    "blink-base-app")
        LOG_FILE="$LOG_DIR/blink-base-app.log"
        GRADLE_MODULE=":blink-base:blink-base-app:bootRun"
        PORT=8001
        ;;
    "gateway-admin")
        LOG_FILE="$LOG_DIR/gateway-admin.log"
        GRADLE_MODULE=":blink-gateway:gateway-admin:bootRun"
        PORT=8008
        ;;
    "blink-gateway-reactive")
        LOG_FILE="$LOG_DIR/gateway-reactive.log"
        GRADLE_MODULE=":blink-gateway:blink-gateway-reactive:bootRun"
        PORT=8080
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
echo "端口: $PORT"
echo "JVM 参数: $JVM_OPTS"
echo "日志文件: $LOG_FILE"
echo "========================================"

# 判断运行模式
if [ "$MODE" == "--console" ]; then
    # 终端模式：日志同时输出到终端和文件
    echo ""
    echo "运行模式: 终端模式 (Ctrl+C 停止)"
    echo ""
    ./gradlew "$GRADLE_MODULE" --no-daemon -Dorg.gradle.jvmargs="-Xmx256m" --args="$JVM_OPTS" 2>&1 | tee "$LOG_FILE"
else
    # 后台模式：日志仅写入文件
    echo ""
    echo "运行模式: 后台模式"
    echo ""
    echo "提示:"
    echo "  查看日志: tail -f $LOG_FILE"
    echo "  停止应用: kill \$(cat logs/$MODULE.pid)"
    echo ""

    # 启动应用并记录 PID
    nohup ./gradlew "$GRADLE_MODULE" --no-daemon -Dorg.gradle.jvmargs="-Xmx256m" --args="$JVM_OPTS" > "$LOG_FILE" 2>&1 &

    # 保存 PID 到文件
    PID_FILE="$LOG_DIR/$MODULE.pid"
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