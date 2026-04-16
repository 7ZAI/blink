#!/bin/bash
#
# Git Hooks 激活脚本
# 将 sample 文件复制为实际 hook 文件
#
# @author binblink
# @since 2026-04-16
#

PROJECT_ROOT="$(git rev-parse --show-toplevel)"
HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

echo "激活 Git Hooks..."

# 创建 pre-commit hook
cat > "$HOOKS_DIR/pre-commit" << 'EOF'
#!/bin/bash
#
# Pre-commit hook - 执行快速检查
#

set -e

# 执行项目的快速检查脚本
SCRIPT_DIR="$(git rev-parse --show-toplevel)/scripts"
if [ -f "$SCRIPT_DIR/quick-check.sh" ]; then
    bash "$SCRIPT_DIR/quick-check.sh"
fi

# 检查敏感文件
SENSITIVE_FILES=".env .env.local credentials.json secrets.yaml application-secret.yml"
for file in $SENSITIVE_FILES; do
    if git diff --cached --name-only | grep -q "$file"; then
        echo "警告: 检测到敏感文件 $file"
        echo "请确认是否要提交此文件"
        read -p "继续提交? (y/N): " confirm
        if [ "$confirm" != "y" ]; then
            exit 1
        fi
    fi
done

exit 0
EOF

# 创建 pre-push hook（可选执行 CI）
cat > "$HOOKS_DIR/pre-push" << 'EOF'
#!/bin/bash
#
# Pre-push hook - 可选执行完整 CI 检查
#

set -e

echo "执行 pre-push 检查..."

# 可选择执行快速 CI 或跳过
read -p "执行完整 CI 检查? (y/N): " run_ci
if [ "$run_ci" = "y" ]; then
    SCRIPT_DIR="$(git rev-parse --show-toplevel)/scripts"
    if [ -f "$SCRIPT_DIR/ci-local.sh" ]; then
        bash "$SCRIPT_DIR/ci-local.sh" --quick
    fi
fi

exit 0
EOF

# 设置权限
chmod +x "$HOOKS_DIR/pre-commit"
chmod +x "$HOOKS_DIR/pre-push"
chmod +x "$PROJECT_ROOT/scripts/quick-check.sh"

echo "Git Hooks 已激活:"
echo "  - pre-commit: 执行快速检查"
echo "  - pre-push: 可选执行 CI 检查"
echo ""
echo "禁用方法:"
echo "  删除 $HOOKS_DIR/pre-commit"
echo "  或删除 $HOOKS_DIR/pre-push"