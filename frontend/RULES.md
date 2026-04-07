# 前端开发规范

适用 Vue 3 + TypeScript + Element Plus + pnpm Monorepo。

## 项目结构

```
frontend/
├── packages/
│   ├── components/      # @blink/components 共享组件库
│   ├── base-admin/      # Base Admin 应用
│   └── gateway-admin/   # Gateway Admin 应用
├── package.json
└── pnpm-workspace.yaml
```

## 开发命令

```bash
cd frontend
pnpm install                    # 安装依赖
pnpm dev:base                   # 启动 Base Admin (端口 4000)
pnpm dev:gateway                # 启动 Gateway Admin (端口 3001)
pnpm build:components           # 构建组件库
pnpm build                      # 构建所有项目
pnpm test                       # 运行测试
```

---

## 1. API 层规范

### 命名约定

| 类型 | 约定 | 示例 |
|------|------|------|
| 请求参数 | `XxxParams` / `XxxReq` | `QueryUserParams` |
| 响应数据 | `XxxRsp` / `XxxResult` | `PageResult` |
| 数据实体 | `XxxInfo` / `XxxDetail` | `UserInfo` |

### 规范要点

- 公共类型从 `@/types` 导入，禁止重复定义
- 统一使用 POST 方法，参数包裹在 body 中
- 分页响应使用 `PageResult<T>` 格式

---

## 2. 页面组件规范

### Script Setup 结构顺序

1. 导入：Vue 核心 → 第三方库 → 组件 → API → 工具
2. `defineOptions({ name: 'ComponentName' })`
3. i18n: `const { t } = useI18n()`
4. 响应式状态
5. 数据获取方法（try-finally 处理 loading）
6. 事件处理方法（handle 前缀）
7. 生命周期

---

## 3. 国际化规范

### 强制要求

- **所有用户可见文本必须使用 i18n**
- 路由 `meta.title` 使用 i18n key，禁止硬编码
- 表单验证规则使用 `computed` 包装

### i18n Key 命名规范

| 类型 | 前缀 | 示例 |
|------|------|------|
| 菜单 | `menu.` | `menu.userList` |
| 通用 | `common.` | `common.search` |
| 用户 | `user.` | `user.loginName` |
| 验证 | `validation.` | `validation.required` |
| 消息 | `message.` | `message.saveSuccess` |
| 头部 | `header.` | `header.profile` |

### 侧边栏菜单国际化

数据库 `menu_en_name` 字段（PascalCase）→ 转换为 camelCase → 作为 `menu.` 前缀的 i18n key

---

## 4. 样式规范

### CSS 变量（支持暗黑模式）

| 变量 | 用途 |
|------|------|
| `--card-bg` | 卡片背景 |
| `--bg-color` / `--bg-color-page` | 页面背景 |
| `--border-color-light` | 边框颜色 |
| `--text-color-primary` | 主要文本 |
| `--text-color-secondary` | 次要文本 |
| `--primary-color` | 主题色 |

### 规范要点

- 必须使用 CSS 变量，禁止硬编码颜色
- 深度选择器使用 `:deep()`

---

## 5. 组件拆分规范

```
views/system/user/
├── index.vue              # 主页面
└── components/
    ├── UserFormDialog.vue # 表单弹窗
    └── UserDetailDialog.vue # 详情弹窗
```

---

## 6. 错误处理规范

### 强制要求

- **禁止空 catch 块**
- 错误日志格式：`[组件名/模块名] 操作描述`

### HTTP 错误处理

`src/utils/request.ts` 已统一处理网络错误，业务代码只需记录日志。

---

## 7. 防止重复提交

使用 `useSubmitGuard` composable：

```typescript
const { isSubmitting, submitGuard } = useSubmitGuard()

const handleSubmit = async () => {
  await submitGuard(async () => {
    await saveData(params)
  })
}
```

---

## 8. 数据加载过渡

使用 `useTransition` 实现平滑过渡：

```typescript
const { transitionClass, startTransition, finishTransition } = useTransition()

const fetchData = async () => {
  startTransition()
  try {
    const res = await getData()
  } finally {
    finishTransition()
  }
}
```

---

## 9. 共享组件库 (@blink/components)

### Workspace 依赖

```json
{
  "dependencies": {
    "@blink/components": "workspace:*"
  }
}
```

### 主要导出

- **布局组件**: `MainLayout`, `Sidebar`, `Header`, `UserDropdown`, `TabsView`, `Breadcrumb`
- **功能组件**: `ThemeToggle`, `LanguageSwitch`, `FullscreenToggle`, `BlinkDialog`, `BlinkTable`, `BlinkTableColumn`, `ThemeSettings`
- **Composables**: `useSidebarState`, `useTabsState`, `useLayoutState`, `useThemeSettings`, `useSubmitGuard`
- **Directives**: `dataFadeDirective`, `rippleDirective`

---

## 10. 命名约定汇总

| 类型 | 约定 | 示例 |
|------|------|------|
| 页面组件 | PascalCase | `SystemUser` |
| 方法 | handleXxx / fetchXxx | `handleSearch`, `fetchUserList` |
| 状态变量 | camelCase | `searchForm`, `loading` |
| 接口(请求) | XxxParams / XxxReq | `QueryUserParams` |
| 接口(响应) | XxxRsp / XxxResult | `PageResult` |
| 数据实体 | XxxInfo / XxxDetail | `UserInfo` |

---

## 11. 开发检查清单

| 检查项 | 说明 |
|--------|------|
| i18n | 所有用户可见文本使用 `t()` |
| 暗黑模式 | 使用 CSS 变量，测试深色主题 |
| 错误处理 | catch 块包含日志记录 |
| 重复提交 | 关键操作使用 `useSubmitGuard` |
| 数据过渡 | 列表/树/图形使用 `useTransition` |
| 空值处理 | 使用 `?.` 和 `||` 安全访问 |
| 未使用导入 | 定期清理未使用的 import |