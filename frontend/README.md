# Blink Frontend Monorepo

基于 pnpm workspace 的前端 Monorepo 架构，包含共享组件库和两个管理应用。

## 项目结构

```
frontend/
├── packages/
│   ├── components/      # @blink/components - 共享组件库
│   │   ├── src/
│   │   │   ├── components/     # 共享组件
│   │   │   ├── composables/    # 共享 Composables
│   │   │   ├── directives/     # 共享指令
│   │   │   └── lib-index.ts    # 组件库入口
│   │   └── package.json
│   │
│   ├── base-admin/      # Base Admin 应用 (端口 4000)
│   │   ├── src/
│   │   │   ├── views/          # 业务页面
│   │   │   ├── api/            # API 接口
│   │   │   └── ...
│   │   └── package.json
│   │
│   └── gateway-admin/   # Gateway Admin 应用 (端口 3001)
│       ├── src/
│       │   ├── views/          # 业务页面
│       │   ├── api/            # API 接口
│       │   └── ...
│       └── package.json
│
├── .vscode/             # IDE 配置（推荐扩展、调试配置）
├── package.json         # Monorepo 根配置
├── pnpm-workspace.yaml  # pnpm workspace 配置
├── RULES.md             # 开发规范
└── pnpm-lock.yaml
```

## 快速开始

### 环境要求

- Node.js >= 18.0.0
- pnpm >= 8.0.0

### 安装依赖

```bash
cd frontend
pnpm install
```

### 开发模式

```bash
# 启动 Base Admin (端口 4000)
pnpm dev:base

# 启动 Gateway Admin (端口 3001)
pnpm dev:gateway

# 启动组件库开发服务
pnpm dev:components
```

### 构建

```bash
# 构建组件库
pnpm build:components

# 构建 Base Admin
pnpm build:base

# 构建 Gateway Admin
pnpm build:gateway

# 构建所有项目
pnpm build
```

### 测试

```bash
# 运行组件库测试
pnpm test

# 测试覆盖率
pnpm test:coverage
```

## Workspace 依赖

两个应用均使用 `workspace:*` 协议引用共享组件库：

```json
{
  "dependencies": {
    "@blink/components": "workspace:*"
  }
}
```

**优势：**
- 开发时实时链接，无需打包
- 修改组件库代码后立即生效
- 统一版本管理，类型自动同步

## 共享组件库

`@blink/components` 主要导出：

| 类别 | 组件/功能 |
|------|----------|
| 布局 | `MainLayout`, `Sidebar`, `Header`, `UserDropdown`, `TabsView`, `Breadcrumb` |
| 功能 | `ThemeToggle`, `LanguageSwitch`, `FullscreenToggle`, `BlinkDialog`, `BlinkTable`, `BlinkTableColumn`, `ThemeSettings`, `BlinkIcon` |
| Composables | `useSidebarState`, `useTabsState`, `useLayoutState`, `useThemeSettings`, `useSubmitGuard`, `useTransition` |
| Directives | `dataFadeDirective`, `rippleDirective` |

## 常用命令

| 命令 | 说明 |
|------|------|
| `pnpm install` | 安装所有依赖 |
| `pnpm dev:base` | 启动 Base Admin (4000) |
| `pnpm dev:gateway` | 启动 Gateway Admin (3001) |
| `pnpm build` | 构建所有项目 |
| `pnpm test` | 运行测试 |
| `pnpm -r <cmd>` | 在所有包中执行命令 |
| `pnpm --filter <pkg> <cmd>` | 在指定包中执行命令 |

## 开发规范

详见 [RULES.md](./RULES.md)，涵盖：
- API 层命名约定
- 页面组件结构
- 国际化规范
- 样式规范（暗黑模式支持）
- 组件拆分规范
- 错误处理规范
- 防重复提交 / 数据过渡

## IDE 配置

`.vscode/` 目录包含团队共享配置：
- `settings.json` - 编辑器设置、TypeScript/Vue 配置
- `extensions.json` - 推荐扩展（Volar、ESLint、Prettier、Tailwind CSS）
- `launch.json` - Chrome 调试配置

## 技术栈

| 类别 | 版本 |
|------|------|
| 构建工具 | Vite 7 |
| 框架 | Vue 3.5 |
| UI 库 | Element Plus 2.13 |
| 状态管理 | Pinia 3 |
| 路由 | Vue Router 4 |
| 国际化 | Vue I18n 11 |
| 测试 | Vitest 4 + @vue/test-utils |
| 类型 | TypeScript 5.9 |
| 包管理 | pnpm 9 |