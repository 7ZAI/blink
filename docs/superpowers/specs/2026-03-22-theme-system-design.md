# 主题系统设计文档

## 概述

为 blink-base-web 添加完整的主题定制能力，包括：
- 6-8 套预设主题方案快速切换
- 自定义主题色、功能色配置
- 字体和字号配置
- 用户自定义预设保存（支持多个）
- 深色/浅色模式切换（已有）

## 技术方案

### 核心原理

使用 CSS 变量动态切换主题，Element Plus 组件会自动响应变量变化。

```typescript
// 动态修改 CSS 变量
document.documentElement.style.setProperty('--el-color-primary', '#8b5cf6')
```

### 架构设计

```
src/
├── config/
│   └── themes.ts              # 预设主题配置
├── stores/
│   └── theme.ts               # 扩展主题 Store
├── views/settings/
│   ├── index.vue              # 设置页面（扩展）
│   └── components/
│       └── ThemeEditor.vue    # 主题编辑器组件
└── utils/
    └── fontLoader.ts          # 字体加载工具
```

**注意：** 字体通过 Google Fonts CDN 按需加载，无需额外 CSS 文件。CSS 变量已在 `src/styles/index.scss` 中定义。

## 功能设计

### 1. 可配置的主题变量

#### 1.1 颜色变量

| 类别 | CSS 变量 | 说明 | 默认值 |
|------|----------|------|--------|
| 主题色 | `--el-color-primary` | 主色调 | `#3b82f6` |
| 成功色 | `--el-color-success` | 成功状态 | `#10b981` |
| 警告色 | `--el-color-warning` | 警告状态 | `#f59e0b` |
| 危险色 | `--el-color-danger` | 危险状态 | `#ef4444` |
| 信息色 | `--el-color-info` | 信息状态 | `#6366f1` |

#### 1.2 字体变量

| 类别 | CSS 变量 | 说明 | 默认值 |
|------|----------|------|--------|
| 字体族 | `--el-font-family` | 全局字体 | 系统默认 |
| 基础字号 | `--el-font-size-base` | 基础字号 | 14px |
| 大字号 | `--el-font-size-large` | 大字号 | 18px |
| 小字号 | `--el-font-size-small` | 小字号 | 12px |

### 2. 预设主题（8套）

| 名称 | Primary | Success | Warning | Danger | Info | 风格描述 |
|------|---------|---------|---------|--------|------|----------|
| 默认蓝 | `#3b82f6` | `#10b981` | `#f59e0b` | `#ef4444` | `#6366f1` | 经典稳重 |
| 科技紫 | `#8b5cf6` | `#06b6d4` | `#fbbf24` | `#f43f5e` | `#3b82f6` | 未来科技 |
| 活力橙 | `#f97316` | `#22c55e` | `#fbbf24` | `#ef4444` | `#0ea5e9` | 热情活力 |
| 清新绿 | `#10b981` | `#06b6d4` | `#f59e0b` | `#ef4444` | `#8b5cf6` | 自然清新 |
| 优雅红 | `#ef4444` | `#10b981` | `#f59e0b` | `#b91c1c` | `#6366f1` | 热烈醒目 |
| 赛博朋克 | `#06b6d4` | `#10b981` | `#f97316` | `#f43f5e` | `#8b5cf6` | 霓虹科技 |
| 极简灰 | `#64748b` | `#10b981` | `#f59e0b` | `#ef4444` | `#6366f1` | 简约商务 |
| 深邃夜 | `#1e293b` | `#10b981` | `#f59e0b` | `#ef4444` | `#6366f1` | 暗黑神秘 |

### 3. 字体预设

使用 Google Fonts 开源字体，按需加载：

| 字体名称 | Font Family | 来源 | 适合场景 |
|----------|-------------|------|----------|
| 系统默认 | 系统字体栈 | 本地 | 跨平台兼容 |
| 思源黑体 | `Noto Sans SC` | Google Fonts | 中文友好，清晰 |
| 思源宋体 | `Noto Serif SC` | Google Fonts | 正式文档 |
| 霞鹜文楷 | `LXGW WenKai` | Google Fonts | 优雅现代 |
| 等宽字体 | `JetBrains Mono` | Google Fonts | 代码展示 |

## 数据结构

### ThemeConfig 类型定义

```typescript
interface ThemeColors {
  primary: string
  success: string
  warning: string
  danger: string
  info: string
}

interface ThemeFont {
  family: string      // 字体名称
  baseSize: number    // 基础字号 (12-18)
  largeSize: number   // 大字号 (16-24)
  smallSize: number   // 小字号 (10-14)
}

interface ThemeConfig {
  name: string
  colors: ThemeColors
  font: ThemeFont
  isPreset: boolean    // 是否为预设主题
  createdAt?: number   // 创建时间（自定义主题）
}

// 预设主题
interface PresetTheme extends ThemeConfig {
  id: string
  isPreset: true
}

// 用户自定义预设
interface CustomTheme extends ThemeConfig {
  id: string
  isPreset: false
  createdAt: number
}
```

### Store 状态

```typescript
interface ThemeState {
  // 当前主题
  currentTheme: ThemeConfig

  // 深色/浅色模式
  mode: 'light' | 'dark'

  // 用户自定义预设列表
  customPresets: CustomTheme[]
}
```

### LocalStorage 结构

```json
{
  "theme": "dark",
  "themeConfig": {
    "name": "科技紫",
    "colors": { ... },
    "font": { ... }
  },
  "themeCustomPresets": [
    { "id": "xxx", "name": "我的主题", ... }
  ]
}
```

## 组件设计

### ThemeEditor.vue

主题编辑器组件，包含以下模块：

#### 预设主题选择器

- 8 套预设主题以色块卡片形式展示
- 点击即可应用
- 当前选中主题高亮显示

#### 颜色配置器

- 5 个颜色选择器（主题色、成功、警告、危险、信息）
- 实时预览颜色变化
- 支持手动输入 HEX 值

#### 字体配置器

- 字体下拉选择器（5 个预设字体）
- 字号滑块（基础、大、小三个档位）
- 字体预览区域

#### 操作按钮

- 「保存为预设」：弹出命名对话框，保存当前配置
- 「重置为默认」：恢复到默认蓝主题

#### 我的预设列表

- 展示用户保存的自定义预设
- 点击应用、支持删除
- 最多保存 10 个自定义预设

## API 设计

### ThemeStore Actions

```typescript
// 应用主题
applyTheme(config: ThemeConfig): void

// 保存为自定义预设
saveAsPreset(name: string): void

// 删除自定义预设
deletePreset(id: string): void

// 重置为默认主题
resetToDefault(): void

// 设置字体
setFont(font: ThemeFont): void

// 设置颜色
setColors(colors: ThemeColors): void
```

## 实现要点

### 1. CSS 变量映射

需要处理 Element Plus 的颜色派生变量，当主色改变时，自动生成浅色系列：

```typescript
// 生成颜色派生变量
function generateColorVars(color: string, name: string) {
  return {
    [`--el-color-${name}`]: color,
    [`--el-color-${name}-light-3`]: lighten(color, 0.3),
    [`--el-color-${name}-light-5`]: lighten(color, 0.5),
    [`--el-color-${name}-light-7`]: lighten(color, 0.7),
    [`--el-color-${name}-light-8`]: lighten(color, 0.8),
    [`--el-color-${name}-light-9`]: lighten(color, 0.9),
    [`--el-color-${name}-dark-2`]: darken(color, 0.2),
  }
}
```

### 2. 字体加载

使用 Web Font Loader 或动态创建 link 标签加载 Google Fonts：

```typescript
function loadFont(fontFamily: string) {
  const link = document.createElement('link')
  link.href = `https://fonts.googleapis.com/css2?family=${fontFamily}:wght@400;500;600;700&display=swap`
  link.rel = 'stylesheet'
  document.head.appendChild(link)
}
```

### 3. 深色模式兼容

主题色需要同时在深色/浅色模式下表现良好。预设主题已经过测试确保在两种模式下都可用。

### 4. 性能优化

- 字体文件按需加载
- CSS 变量修改使用批量更新
- 避免频繁触发重绘

## UI 原型

```
┌─────────────────────────────────────────────────────────┐
│ 设置 > 主题设置                                            │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ 预设主题                                                 │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐           │
│ │ 默认蓝│ │ 科技紫│ │ 活力橙│ │ 清新绿│ │ 优雅红│ ...      │
│ │ ████ │ │ ████ │ │ ████ │ │ ████ │ │ ████ │           │
│ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘           │
│                                                         │
│ ──────────────────────────────────────────────────────  │
│                                                         │
│ 颜色设置                                                 │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 主色调    [⬛ #3b82f6]  颜色选择器                    │ │
│ │ 成功色    [⬛ #10b981]                               │ │
│ │ 警告色    [⬛ #f59e0b]                               │ │
│ │ 危险色    [⬛ #ef4444]                               │ │
│ │ 信息色    [⬛ #6366f1]                               │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│ 字体设置                                                 │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 字体      [下拉: 系统默认 ▼]                         │ │
│ │                                                     │ │
│ │ 基础字号  [━━━━━━━●━━━━━] 14px                      │ │
│ │ 大字号    [━━━━━━━━━●━━━] 18px                      │ │
│ │ 小字号    [━━━●━━━━━━━━━] 12px                      │ │
│ │                                                     │ │
│ │ 预览: 这是一段示例文字 ABC abc 123                  │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│ [保存为预设]                        [重置为默认]          │
│                                                         │
│ ──────────────────────────────────────────────────────  │
│                                                         │
│ 我的预设                                                 │
│ ┌─────────────┐ ┌─────────────┐                         │
│ │ 我的主题     │ │ 项目配色     │                        │
│ │ 2024-03-22  │ │ 2024-03-21  │  [🗑]                   │
│ └─────────────┘ └─────────────┘                         │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## 文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/config/themes.ts` | 新建 | 预设主题配置 |
| `src/stores/theme.ts` | 修改 | 扩展主题管理 |
| `src/views/settings/index.vue` | 修改 | 集成主题编辑器 |
| `src/views/settings/components/ThemeEditor.vue` | 新建 | 主题编辑器组件 |
| `src/locales/zh-cn.ts` | 修改 | 中文国际化 |
| `src/locales/en-us.ts` | 修改 | 英文国际化 |

## 验收标准

1. 用户可以在设置页面选择预设主题，页面即时切换
2. 用户可以自定义主题色、功能色
3. 用户可以选择字体、调整字号
4. 用户可以将当前配置保存为自定义预设
5. 自定义预设可以删除
6. 主题配置在刷新页面后保持
7. 深色/浅色模式下主题表现正常