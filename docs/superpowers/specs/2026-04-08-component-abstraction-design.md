# 通用组件抽象设计文档

## 概述

将 blink-base-web 项目中的主题设置、窗口弹窗、列表抽象为可复用的通用组件，要求低耦合、可自定义、方便复用。

## 设计原则

采用 **Props 驱动 + Slot 扩展** 模式：
- 简单场景通过 props 配置
- 复杂场景通过 slot 自定义
- 与 Element Plus 使用习惯一致
- 保持与现有代码风格统一

---

## 一、主题设置组件 (ThemeSettings)

### 1.1 组件定位

将现有 `ThemeEditor.vue` 重构为可复用的通用组件，支持完整的主题配置能力。

### 1.2 功能模块

| 模块 | 功能 |
|------|------|
| 预设主题 | 快速切换内置/自定义预设 |
| 颜色配置 | 主题色、成功色、警告色、危险色、信息色 |
| 字体配置 | 字体族、基础字号、大字号、小字号 |
| 动画开关 | 全局动画启用/禁用 |
| 系统配置 | 边框圆角、阴影强度、紧凑模式、内容宽度 |
| 自定义预设 | 保存/删除/应用自定义预设 |

### 1.3 文件结构

```
src/components/ThemeSettings/
├── index.vue                    # 主组件
├── components/
│   ├── PresetSelector.vue       # 预设主题选择器
│   ├── ColorSettings.vue        # 颜色配置面板
│   ├── FontSettings.vue         # 字体配置面板
│   ├── SystemSettings.vue       # 系统配置面板
│   └── CustomPresetList.vue     # 自定义预设列表
└── types.ts                     # 类型定义
```

### 1.4 接口定义

```typescript
// Props
interface ThemeSettingsProps {
  // 功能模块开关
  showPresets?: boolean          // 是否显示预设主题，默认 true
  showColors?: boolean           // 是否显示颜色设置，默认 true
  showFonts?: boolean            // 是否显示字体设置，默认 true
  showAnimations?: boolean       // 是否显示动画开关，默认 true
  showSystem?: boolean           // 是否显示系统配置，默认 true

  // 配置项
  presetThemes?: PresetTheme[]   // 自定义预设主题列表
  presetFonts?: FontOption[]     // 自定义字体选项
  maxCustomPresets?: number      // 最大自定义预设数量，默认 5

  // 状态控制
  modelValue?: ThemeConfig       // v-model 绑定当前主题配置
  readonly?: boolean             // 只读模式
}

// ThemeConfig 类型
interface ThemeConfig {
  presetId?: string
  colors: ThemeColors
  font: FontConfig
  animationsEnabled: boolean
  system: SystemConfig
}

interface ThemeColors {
  primary: string
  success: string
  warning: string
  danger: string
  info: string
}

interface FontConfig {
  family: string
  baseSize: number
  largeSize: number
  smallSize: number
}

interface SystemConfig {
  borderRadius: number           // 全局圆角 (0-24px)
  shadowIntensity: 'none' | 'light' | 'medium' | 'strong'
  compactMode: boolean           // 紧凑模式
  contentWidth: 'fluid' | 'fixed'
}

// Events
interface ThemeSettingsEmits {
  'update:modelValue': [value: ThemeConfig]
  'preset-change': [presetId: string]
  'color-change': [colors: ThemeColors]
  'font-change': [font: FontConfig]
  'animation-change': [enabled: boolean]
  'system-change': [config: SystemConfig]
  'preset-save': [preset: CustomPreset]
  'preset-delete': [presetId: string]
}

// Slots
interface ThemeSettingsSlots {
  'preset-footer': any           // 预设区域底部
  'color-footer': any            // 颜色区域底部
  'font-footer': any             // 字体区域底部
  'system-footer': any           // 系统配置区域底部
}
```

### 1.5 使用示例

```vue
<template>
  <ThemeSettings
    v-model="themeConfig"
    :show-system="true"
    :max-custom-presets="10"
    @preset-change="handlePresetChange"
    @preset-save="handleSavePreset"
  >
    <template #preset-footer>
      <el-button size="small" @click="handleImport">导入主题</el-button>
    </template>
  </ThemeSettings>
</template>
```

### 1.6 与现有代码关系

| 现有代码 | 处理方式 |
|---------|---------|
| `stores/theme.ts` | 保持不变，组件通过 v-model 解耦 |
| `config/themes.ts` | 扩展 SystemConfig 配置 |
| `ThemeEditor.vue` | 重构为新组件，原位置直接使用新组件替代 |

---

## 二、弹窗组件 (BlinkDialog)

### 2.1 组件定位

抽象统一的弹窗容器，处理标题、宽度、关闭逻辑、loading 状态、底部按钮等通用功能。

### 2.2 功能模块

| 功能 | 说明 |
|------|------|
| 基础配置 | 标题、宽度、显示控制 |
| 行为配置 | 点击遮罩关闭、ESC关闭、滚动锁定 |
| 状态管理 | Loading 状态、确认按钮 Loading |
| 底部按钮 | 取消/确认按钮配置、自定义底部 |
| 关闭确认 | 有修改时的关闭提示 |

### 2.3 文件结构

```
src/components/BlinkDialog/
├── index.vue                    # 主组件
└── types.ts                     # 类型定义
```

### 2.4 接口定义

```typescript
// Props
interface BlinkDialogProps {
  // 基础配置
  modelValue: boolean            // v-model 控制显示
  title?: string                 // 弹窗标题
  width?: string | number        // 弹窗宽度，默认 '500px'

  // 行为配置
  closeOnClickModal?: boolean    // 点击遮罩关闭，默认 false
  closeOnPressEscape?: boolean   // 按 ESC 关闭，默认 true
  showClose?: boolean            // 显示关闭按钮，默认 true
  lockScroll?: boolean           // 锁定滚动，默认 false

  // 状态
  loading?: boolean              // 加载状态
  confirmLoading?: boolean       // 确认按钮加载状态

  // 底部按钮
  showFooter?: boolean           // 显示底部，默认 true
  showCancel?: boolean           // 显示取消按钮，默认 true
  showConfirm?: boolean          // 显示确认按钮，默认 true
  cancelText?: string            // 取消按钮文本，默认 '取消'
  confirmText?: string           // 确认按钮文本，默认 '确定'
  confirmType?: 'primary' | 'success' | 'warning' | 'danger'

  // 关闭确认
  beforeClose?: (done: () => void) => void
  confirmOnClose?: boolean       // 关闭时是否需要确认

  // 样式
  customClass?: string
  destroyOnClose?: boolean       // 关闭时销毁内容，默认 false
}

// Events
interface BlinkDialogEmits {
  'update:modelValue': [value: boolean]
  'confirm': []
  'cancel': []
  'close': []
  'open': []
  'opened': []
  'closed': []
}

// Slots
interface BlinkDialogSlots {
  default: any      // 主内容区域
  header: any       // 自定义标题区域
  footer: any       // 自定义底部区域
}
```

### 2.5 使用示例

**基础用法**：
```vue
<BlinkDialog v-model="visible" title="新增用户" @confirm="handleSubmit">
  <el-form>
    <el-form-item label="用户名">
      <el-input v-model="form.name" />
    </el-form-item>
  </el-form>
</BlinkDialog>
```

**自定义底部**：
```vue
<BlinkDialog v-model="visible" title="分配角色">
  <RoleTransfer v-model="selectedRoles" />
  <template #footer>
    <el-button @click="visible = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="handleSubmit">
      确定分配
    </el-button>
  </template>
</BlinkDialog>
```

**关闭确认**：
```vue
<BlinkDialog v-model="visible" title="编辑用户" :before-close="handleBeforeClose">
  <UserForm ref="formRef" v-model="formData" @change="hasChanged = true" />
</BlinkDialog>
```

### 2.6 默认样式规范

```scss
.blink-dialog {
  border-radius: 12px;
  overflow: hidden;

  .el-dialog__header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-color-light);
    margin-right: 0;

    .el-dialog__title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color-primary);
    }
  }

  .el-dialog__body {
    padding: 20px;
    color: var(--text-color-regular);
  }

  .el-dialog__footer {
    padding: 12px 20px;
    border-top: 1px solid var(--border-color-light);
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}
```

### 2.7 与现有弹窗关系

| 现有弹窗 | 迁移方式 |
|---------|---------|
| `RoleFormDialog.vue` | 替换 `el-dialog` 为 `BlinkDialog` |
| `UserFormDialog.vue` | 同上 |
| 其他弹窗组件 | 逐步迁移 |

---

## 三、列表组件 (BlinkTable)

### 3.1 组件定位

抽象 el-table 的通用功能：列配置化、选择、排序、Loading、操作列、空数据状态、默认样式。

### 3.2 功能模块

| 功能 | 说明 |
|------|------|
| 列配置化 | JSON 配置列属性 |
| 选择功能 | 单选/多选、可选择性判断 |
| 排序 | 列排序、自定义排序 |
| Loading | 加载状态 |
| 操作列 | 配置化操作按钮、权限控制 |
| 空数据 | 统一空状态展示 |
| 默认样式 | 统一表格外观 |

### 3.3 文件结构

```
src/components/BlinkTable/
├── index.vue                    # 主组件
├── Column.vue                   # 列组件（slot 模式）
├── OperationColumn.vue          # 操作列组件
├── EmptyState.vue               # 空状态组件
└── types.ts                     # 类型定义
```

### 3.4 接口定义

```typescript
// Props
interface BlinkTableProps<T = any> {
  // 数据
  data: T[]
  rowKey?: string | ((row: T) => string)

  // 列配置（配置模式）
  columns?: TableColumn<T>[]

  // 选择功能
  selectable?: boolean
  selectedKeys?: (string | number)[]
  selectType?: 'checkbox' | 'radio'
  checkSelectable?: (row: T) => boolean

  // 排序
  sortable?: boolean
  defaultSort?: { prop: string; order: 'ascending' | 'descending' }

  // 状态
  loading?: boolean
  emptyText?: string

  // 样式
  height?: string | number
  maxHeight?: string | number
  stripe?: boolean
  border?: boolean
  showOverflowTooltip?: boolean

  // 操作列
  operations?: TableOperation<T>[]
  operationWidth?: number
  operationFixed?: 'left' | 'right' | boolean
}

// 列配置类型
interface TableColumn<T = any> {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right' | boolean

  // 特殊列类型
  type?: 'index' | 'selection' | 'expand' | 'tag' | 'image' | 'datetime'

  // 格式化
  formatter?: (row: T, column: any, cellValue: any) => string | VNode

  // 标签类型（type='tag' 时）
  tagType?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | ((row: T) => string)
  tagOptions?: { label: string; value: any; type?: string }[]

  // 图片配置（type='image' 时）
  imageWidth?: number
  imageHeight?: number

  // 排序
  sortable?: boolean | 'custom'

  // 插槽名
  slot?: string
}

// 操作按钮配置
interface TableOperation<T = any> {
  label: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  icon?: string
  link?: boolean
  visible?: (row: T) => boolean
  disabled?: (row: T) => boolean
  onClick: (row: T) => void
  permission?: string            // 权限标识
}

// Events
interface BlinkTableEmits<T = any> {
  'update:selectedKeys': [keys: (string | number)[]]
  'select': [selection: T[], row: T]
  'select-all': [selection: T[]]
  'selection-change': [selection: T[]]
  'sort-change': [{ prop: string; order: string }]
  'row-click': [row: T, column: any, event: Event]
  'row-dblclick': [row: T, column: any, event: Event]
}

// Expose
interface BlinkTableExpose {
  clearSelection: () => void
  toggleRowSelection: (row: any) => void
  toggleAllSelection: () => void
  setCurrentRow: (row: any) => void
  clearSort: () => void
  clearFilter: () => void
  doLayout: () => void
  scrollTo: (options: ScrollToOptions) => void
}
```

### 3.5 使用示例

**配置模式**：
```vue
<template>
  <BlinkTable
    :data="userList"
    :columns="columns"
    :loading="loading"
    :selectable="true"
    v-model:selected-keys="selectedIds"
    :operations="operations"
    operation-width="200"
    @selection-change="handleSelectionChange"
  />
</template>

<script setup lang="ts">
const columns: TableColumn[] = [
  { prop: 'loginName', label: '登录名', minWidth: 120 },
  { prop: 'username', label: '用户名', minWidth: 120 },
  {
    prop: 'sex',
    label: '性别',
    type: 'tag',
    tagOptions: [
      { label: '男', value: 1, type: 'primary' },
      { label: '女', value: 2, type: 'danger' },
    ]
  },
  { prop: 'phone', label: '电话', minWidth: 120 },
  { prop: 'createTime', label: '创建时间', type: 'datetime', width: 160 },
]

const operations: TableOperation[] = [
  { label: '编辑', type: 'primary', onClick: handleEdit },
  { label: '删除', type: 'danger', onClick: handleDelete, visible: (row) => row.roleId !== 1 },
]
</script>
```

**插槽模式**：
```vue
<template>
  <BlinkTable :data="userList" :loading="loading" :selectable="true">
    <BlinkTableColumn prop="loginName" label="登录名" />
    <BlinkTableColumn prop="avatar" label="头像">
      <template #default="{ row }">
        <el-avatar :src="getAvatarUrl(row.avatar)" />
      </template>
    </BlinkTableColumn>
    <BlinkTableColumn type="operation" width="280">
      <template #default="{ row }">
        <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
        <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
      </template>
    </BlinkTableColumn>
  </BlinkTable>
</template>
```

### 3.6 默认样式规范

```scss
.blink-table {
  background: var(--card-bg);
  border-radius: 8px;
  overflow: hidden;

  :deep(.el-table__header-wrapper) {
    th {
      background: var(--bg-color-page);
      color: var(--text-color-secondary);
      font-weight: 500;
      font-size: 13px;
    }
  }

  :deep(.el-table__row) {
    td {
      padding: 12px 0;
    }

    &:hover > td {
      background: var(--table-row-hover) !important;
    }
  }

  .operation-buttons {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
  }

  .empty-state {
    padding: 40px 0;
    text-align: center;
    color: var(--text-color-secondary);
  }
}
```

---

## 四、实现计划

### 4.1 开发顺序

三个组件可并行开发，建议顺序：

1. **BlinkDialog** - 最基础，其他组件可能依赖
2. **BlinkTable** - 使用频率最高，优先完成
3. **ThemeSettings** - 功能最复杂，最后完成

### 4.2 测试验证

每个组件完成后需验证：

- [ ] Props 正确响应
- [ ] Events 正确触发
- [ ] Slots 正确渲染
- [ ] 深色模式适配
- [ ] 国际化支持
- [ ] TypeScript 类型完整

### 4.3 文档更新

组件开发完成后需更新：

- [ ] `docs/rules/frontend-rules.md` 添加组件使用规范
- [ ] 组件目录下添加 README.md 使用说明