#!/bin/bash
#
# 本地 CI 脚本 - 前后端统一执行
# 用法: ./scripts/ci-local.sh [--quick] [--backend] [--frontend]
#
# @author binblink
# @since 2026-04-16
#

set -e
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_DIR="$PROJECT_ROOT/logs/ci"
LOG_FILE="$LOG_DIR/ci_$TIMESTAMP.log"

mkdir -p "$LOG_DIR"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} $1" | tee -a "$LOG_FILE"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"; }

# 参数解析
QUICK_MODE=false
BACKEND_ONLY=false
FRONTEND_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --quick) QUICK_MODE=true; shift ;;
        --backend) BACKEND_ONLY=true; shift ;;
        --frontend) FRONTEND_ONLY=true; shift ;;
        -h|--help)
            echo "用法: ./scripts/ci-local.sh [选项]"
            echo "选项:"
            echo "  --quick     快速模式（仅关键检查）"
            echo "  --backend   仅执行后端 CI"
            echo "  --frontend  仅执行前端 CI"
            echo "  -h, --help  显示帮助信息"
            exit 0
            ;;
        *) shift ;;
    esac
done

# ==================== 后端 CI ====================
run_backend_ci() {
    log "========== 后端 CI 流程开始 =========="

    cd "$PROJECT_ROOT"

    # 1. 编译检查
    log "Step 1: 编译检查..."
    ./gradlew compileJava compileTestJava --parallel 2>&1 | tee -a "$LOG_FILE"
    log_success "编译通过"

    # 2. 测试
    log "Step 2: 测试执行..."
    if [ "$QUICK_MODE" = true ]; then
        # 快速模式：仅测试关键模块
        ./gradlew :blink-framework-common:test :blink-web-starter:test 2>&1 | tee -a "$LOG_FILE"
    else
        ./gradlew test --parallel 2>&1 | tee -a "$LOG_FILE"
    fi
    log_success "测试通过"

    # 3. 覆盖率报告
    if [ "$QUICK_MODE" = false ]; then
        log "Step 3: 生成覆盖率报告..."
        ./gradlew jacocoRootReport 2>&1 | tee -a "$LOG_FILE" || log_warn "覆盖率报告生成失败"
        log_success "覆盖率报告: $PROJECT_ROOT/build/reports/jacoco/html/index.html"
    fi

    log_success "========== 后端 CI 完成 =========="
}

# ==================== 前端 CI ====================
run_frontend_ci() {
    log "========== 前端 CI 流程开始 =========="

    cd "$PROJECT_ROOT/frontend"

    # 1. 类型检查
    log "Step 1: TypeScript 类型检查..."
    pnpm typecheck 2>&1 | tee -a "$LOG_FILE" || log_warn "类型检查发现问题"
    log_success "类型检查完成"

    # 2. 测试
    log "Step 2: 前端测试..."
    if [ "$QUICK_MODE" = true ]; then
        pnpm test 2>&1 | tee -a "$LOG_FILE"
    else
        pnpm test:coverage 2>&1 | tee -a "$LOG_FILE"
    fi
    log_success "测试通过"

    # 3. Lint 检查
    if [ "$QUICK_MODE" = false ]; then
        log "Step 3: ESLint 检查..."
        pnpm lint 2>&1 | tee -a "$LOG_FILE" || log_warn "Lint 发现问题"
    fi

    log_success "========== 前端 CI 完成 =========="
    log "覆盖率报告: $PROJECT_ROOT/frontend/packages/*/coverage/index.html"
}

# ==================== 执行 CI ====================
trap 'log_error "CI 流程异常中断"; exit 1' ERR

log "本地 CI 开始运行 - $TIMESTAMP"
log "项目根目录: $PROJECT_ROOT"
log "日志文件: $LOG_FILE"

if [ "$BACKEND_ONLY" = true ]; then
    run_backend_ci
elif [ "$FRONTEND_ONLY" = true ]; then
    run_frontend_ci
else
    run_backend_ci
    run_frontend_ci
fi

log_success "========== CI 流程全部完成 =========="
echo ""
echo "报告位置:"
echo "  后端覆盖率: $PROJECT_ROOT/build/reports/jacoco/html/index.html"
echo "  前端覆盖率: $PROJECT_ROOT/frontend/packages/*/coverage/index.html"
echo "  日志文件:   $LOG_FILE"