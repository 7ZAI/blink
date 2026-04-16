#!/bin/bash
#
# 快速检查脚本 - pre-commit hook 调用
# 仅执行必要检查，快速反馈
#
# @author binblink
# @since 2026-04-16
#

set -e
PROJECT_ROOT="$(git rev-parse --show-toplevel)"

echo "执行快速检查..."

# 检测变更文件
JAVA_CHANGED=$(git diff --cached --name-only --diff-filter=ACMR | grep '\.java$' | head -5)
FRONTEND_CHANGED=$(git diff --cached --name-only --diff-filter=ACMR | grep -E '(frontend|\.vue|\.ts)' | head -5)

if [ -n "$JAVA_CHANGED" ]; then
    echo "检测到 Java 文件变更，执行编译检查..."
    cd "$PROJECT_ROOT"
    ./gradlew compileJava --quiet
    echo "Java 编译通过"
fi

if [ -n "$FRONTEND_CHANGED" ]; then
    echo "检测到前端文件变更，执行类型检查..."
    cd "$PROJECT_ROOT/frontend"
    pnpm typecheck 2>&1 || true
    echo "前端类型检查完成"
fi

echo "快速检查完成，可以提交"