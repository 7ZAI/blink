# BlinkTable 组件实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建可配置的表格组件，封装列配置、选择、排序、Loading、操作列、空数据状态、默认样式。

**Architecture:** 基于 Element Plus 的 el-table 进行二次封装，支持两种使用模式：
1. **配置模式**：通过 `columns` prop 配置列
2. **插槽模式**：通过 `BlinkTableColumn` 子组件自定义列

**Tech Stack:** Vue 3 + TypeScript + Element Plus

---

## 文件结构

```
src/components/BlinkTable/
├── index.vue          # 主组件
├── Column.vue         # 列组件（slot 模式）
├── types.ts           # 类型定义
└── styles.scss        # 样式文件
```

---

### Task 1: 创建类型定义文件

**Files:**
- Create: `src/components/BlinkTable/types.ts`

- [ ] **Step 1: 创建 types.ts 文件，定义所有类型**

```typescript
// src/components/BlinkTable/types.ts

/**
 * 列配置类型
 */
export interface TableColumn<T = any> {
  /** 字段名 */
  prop: string
  /** 列标题 */
  label: string
  /** 列宽 */
  width?: string | number
  /** 最小列宽 */
  minWidth?: string | number
  /** 对齐方式 */
  align?: 'left' | 'center' | 'right'
  /** 固定列 */
  fixed?: 'left' | 'right' | boolean

  /** 特殊列类型 */
  type?: 'index' | 'selection' | 'expand' | 'tag' | 'image' | 'datetime'
  /** 格式化函数 */
  formatter?: (row: T, column: any, cellValue: any) => string

  /** 标签类型（type='tag' 时） */
  tagType?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | ((row: T) => string)
  /** 标签选项映射 */
  tagOptions?: { label: string; value: any; type?: string }[]

  /** 图片宽度（type='image' 时） */
  imageWidth?: number
  /** 图片高度（type='image' 时） */
  imageHeight?: number

  /** 排序 */
  sortable?: boolean | 'custom'
  /** 是否显示溢出 tooltip */
  showOverflowTooltip?: boolean

  /** 自定义插槽名 */
  slot?: string
}

/**
 * 操作按钮配置
 */
export interface TableOperation<T = any> {
  /** 按钮文本 */
  label: string
  /** 按钮类型 */
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  /** 图标 */
  icon?: string
  /** 是否为链接样式 */
  link?: boolean
  /** 是否显示 */
  visible?: (row: T) => boolean
  /** 是否禁用 */
  disabled?: (row: T) => boolean
  /** 点击回调 */
  onClick: (row: T) => void
  /** 权限标识 */
  permission?: string
}

/**
 * BlinkTable Props 类型
 */
export interface BlinkTableProps<T = any> {
  /** 表格数据 */
  data: T[]
  /** 行唯一标识 */
  rowKey?: string | ((row: T) => string)

  /** 列配置（配置模式） */
  columns?: TableColumn<T>[]

  /** 是否可选择 */
  selectable?: boolean
  /** 选中的行 keys */
  selectedKeys?: (string | number)[]
  /** 选择类型 */
  selectType?: 'checkbox' | 'radio'
  /** 判断行是否可选 */
  checkSelectable?: (row: T) => boolean

  /** 是否启用排序 */
  sortable?: boolean
  /** 默认排序 */
  defaultSort?: { prop: string; order: 'ascending' | 'descending' }

  /** 加载状态 */
  loading?: boolean
  /** 空数据提示 */
  emptyText?: string

  /** 表格高度 */
  height?: string | number
  /** 最大高度 */
  maxHeight?: string | number
  /** 斑马纹 */
  stripe?: boolean
  /** 边框 */
  border?: boolean
  /** 内容溢出 tooltip */
  showOverflowTooltip?: boolean

  /** 操作列配置 */
  operations?: TableOperation<T>[]
  /** 操作列宽度 */
  operationWidth?: number
  /** 操作列固定 */
  operationFixed?: 'left' | 'right' | boolean
}

/**
 * BlinkTable Emits 类型
 */
export interface BlinkTableEmits<T = any> {
  (e: 'update:selectedKeys', keys: (string | number)[]): void
  (e: 'select', selection: T[], row: T): void
  (e: 'select-all', selection: T[]): void
  (e: 'selection-change', selection: T[]): void
  (e: 'sort-change', sort: { prop: string; order: string }): void
  (e: 'row-click', row: T, column: any, event: Event): void
  (e: 'row-dblclick', row: T, column: any, event: Event): void
}

/**
 * BlinkTable Expose 方法
 */
export interface BlinkTableExpose {
  clearSelection: () => void
  toggleRowSelection: (row: any) => void
  toggleAllSelection: () => void
  setCurrentRow: (row: any) => void
  clearSort: () => void
  clearFilter: () => void
  doLayout: () => void
}
```

- [ ] **Step 2: 提交类型定义文件**

```bash
git add src/components/BlinkTable/types.ts
git commit -m "feat(BlinkTable): add type definitions

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 创建主组件

**Files:**
- Create: `src/components/BlinkTable/index.vue`

- [ ] **Step 1: 创建 index.vue 组件**

```vue
<!-- src/components/BlinkTable/index.vue -->
<template>
  <div class="blink-table">
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="data"
      :row-key="rowKey"
      :height="height"
      :max-height="maxHeight"
      :stripe="stripe"
      :border="border"
      :default-sort="defaultSort"
      :empty-text="emptyText"
      @selection-change="handleSelectionChange"
      @select="handleSelect"
      @select-all="handleSelectAll"
      @sort-change="handleSortChange"
      @row-click="handleRowClick"
      @row-dblclick="handleRowDblclick"
    >
      <!-- 插槽模式：使用默认插槽 -->
      <template v-if="$slots.default">
        <slot />
      </template>

      <!-- 配置模式：根据 columns 配置渲染 -->
      <template v-else-if="columns?.length">
        <!-- 选择列 -->
        <el-table-column
          v-if="selectable && selectType === 'checkbox'"
          type="selection"
          width="55"
          align="center"
          :selectable="checkSelectable"
        />

        <!-- 数据列 -->
        <template v-for="col in columns" :key="col.prop">
          <el-table-column
            :prop="col.prop"
            :label="col.label"
            :width="col.width"
            :min-width="col.minWidth"
            :align="col.align"
            :fixed="col.fixed"
            :sortable="col.sortable"
            :show-overflow-tooltip="col.showOverflowTooltip ?? showOverflowTooltip"
          >
            <template #default="{ row, $index }">
              <!-- 索引列 -->
              <template v-if="col.type === 'index'">
                {{ $index + 1 }}
              </template>

              <!-- 标签列 -->
              <template v-else-if="col.type === 'tag'">
                <el-tag :type="getTagType(col, row)">
                  {{ getTagLabel(col, row) }}
                </el-tag>
              </template>

              <!-- 图片列 -->
              <template v-else-if="col.type === 'image'">
                <el-image
                  :src="row[col.prop]"
                  :style="{
                    width: `${col.imageWidth || 40}px`,
                    height: `${col.imageHeight || 40}px`
                  }"
                  fit="cover"
                />
              </template>

              <!-- 日期时间列 -->
              <template v-else-if="col.type === 'datetime'">
                {{ row[col.prop] || '-' }}
              </template>

              <!-- 自定义格式化 -->
              <template v-else-if="col.formatter">
                {{ col.formatter(row, col, row[col.prop]) }}
              </template>

              <!-- 默认显示 -->
              <template v-else>
                {{ row[col.prop] ?? '-' }}
              </template>
            </template>
          </el-table-column>
        </template>
      </template>

      <!-- 操作列（配置模式） -->
      <el-table-column
        v-if="operations?.length && !$slots.default"
        :label="$t('common.operation')"
        :width="operationWidth || 200"
        :fixed="operationFixed || 'right'"
      >
        <template #default="{ row }">
          <div class="operation-buttons">
            <template v-for="(op, index) in operations" :key="index">
              <el-button
                v-if="op.visible ? op.visible(row) : true"
                :type="op.type || 'primary'"
                :link="op.link ?? true"
                size="small"
                :disabled="op.disabled ? op.disabled(row) : false"
                @click="op.onClick(row)"
              >
                {{ op.label }}
              </el-button>
            </template>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { TableColumn, TableOperation, BlinkTableProps, BlinkTableEmits, BlinkTableExpose } from './types'

defineOptions({
  name: 'BlinkTable',
})

const props = withDefaults(defineProps<BlinkTableProps>(), {
  rowKey: 'id',
  selectable: false,
  selectType: 'checkbox',
  stripe: true,
  border: true,
  showOverflowTooltip: true,
  loading: false,
  emptyText: '暂无数据',
})

const emit = defineEmits<BlinkTableEmits>()

const tableRef = ref()

// 选择变化
const handleSelectionChange = (selection: any[]) => {
  emit('selection-change', selection)
  const keys = selection.map(item => {
    if (typeof props.rowKey === 'function') {
      return props.rowKey(item)
    }
    return item[props.rowKey as string]
  })
  emit('update:selectedKeys', keys)
}

const handleSelect = (selection: any[], row: any) => {
  emit('select', selection, row)
}

const handleSelectAll = (selection: any[]) => {
  emit('select-all', selection)
}

const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
  emit('sort-change', { prop, order: order || '' })
}

const handleRowClick = (row: any, column: any, event: Event) => {
  emit('row-click', row, column, event)
}

const handleRowDblclick = (row: any, column: any, event: Event) => {
  emit('row-dblclick', row, column, event)
}

// 获取标签类型
const getTagType = (col: TableColumn, row: any): string => {
  if (typeof col.tagType === 'function') {
    return col.tagType(row)
  }
  if (col.tagOptions) {
    const option = col.tagOptions.find(opt => opt.value === row[col.prop])
    return option?.type || 'info'
  }
  return col.tagType || 'info'
}

// 获取标签文本
const getTagLabel = (col: TableColumn, row: any): string => {
  if (col.tagOptions) {
    const option = col.tagOptions.find(opt => opt.value === row[col.prop])
    return option?.label ?? row[col.prop]
  }
  return row[col.prop]
}

// Expose 方法
const clearSelection = () => tableRef.value?.clearSelection()
const toggleRowSelection = (row: any) => tableRef.value?.toggleRowSelection(row)
const toggleAllSelection = () => tableRef.value?.toggleAllSelection()
const setCurrentRow = (row: any) => tableRef.value?.setCurrentRow(row)
const clearSort = () => tableRef.value?.clearSort()
const clearFilter = () => tableRef.value?.clearFilter()
const doLayout = () => tableRef.value?.doLayout()

defineExpose<BlinkTableExpose>({
  clearSelection,
  toggleRowSelection,
  toggleAllSelection,
  setCurrentRow,
  clearSort,
  clearFilter,
  doLayout,
})
</script>
```

- [ ] **Step 2: 添加组件样式**

```scss
<style scoped lang="scss">
.blink-table {
  :deep(.el-table) {
    background: var(--card-bg);
    border-radius: 8px;
    overflow: hidden;

    // 表头样式
    .el-table__header-wrapper {
      th {
        background: var(--bg-color-page);
        color: var(--text-color-secondary);
        font-weight: 500;
        font-size: 13px;
      }
    }

    // 表格行样式
    .el-table__row {
      td {
        padding: 12px 0;
      }

      &:hover > td {
        background: var(--table-row-hover) !important;
      }
    }

    // 斑马纹
    &--striped .el-table__row--striped td {
      background: var(--bg-color-page);
    }
  }

  .operation-buttons {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;

    .el-button + .el-button {
      margin-left: 0;
    }
  }
}
</style>
```

- [ ] **Step 3: 提交主组件文件**

```bash
git add src/components/BlinkTable/index.vue
git commit -m "feat(BlinkTable): implement main component

- Support columns config mode
- Support slot mode
- Support selection, sorting
- Support operation column
- Dark mode compatible

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 创建列组件（插槽模式）

**Files:**
- Create: `src/components/BlinkTable/Column.vue`

- [ ] **Step 1: 创建 Column.vue 组件**

```vue
<!-- src/components/BlinkTable/Column.vue -->
<template>
  <el-table-column
    :prop="prop"
    :label="label"
    :width="width"
    :min-width="minWidth"
    :align="align"
    :fixed="fixed"
    :sortable="sortable"
    :type="type"
    :show-overflow-tooltip="showOverflowTooltip"
  >
    <template v-if="$slots.default" #default="scope">
      <slot v-bind="scope" />
    </template>

    <template v-if="$slots.header" #header="scope">
      <slot name="header" v-bind="scope" />
    </template>
  </el-table-column>
</template>

<script setup lang="ts">
defineOptions({
  name: 'BlinkTableColumn',
})

interface Props {
  prop?: string
  label?: string
  width?: string | number
  minWidth?: string | number
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right' | boolean
  sortable?: boolean | 'custom'
  type?: 'index' | 'selection' | 'expand'
  showOverflowTooltip?: boolean
}

withDefaults(defineProps<Props>(), {
  showOverflowTooltip: true,
})
</script>
```

- [ ] **Step 2: 提交列组件**

```bash
git add src/components/BlinkTable/Column.vue
git commit -m "feat(BlinkTable): add Column component for slot mode

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: 添加组件导出

**Files:**
- Create: `src/components/index.ts`

- [ ] **Step 1: 创建或更新组件导出文件**

```typescript
// src/components/index.ts
export { default as BlinkDialog } from './BlinkDialog/index.vue'
export * from './BlinkDialog/types'

export { default as BlinkTable } from './BlinkTable/index.vue'
export { default as BlinkTableColumn } from './BlinkTable/Column.vue'
export * from './BlinkTable/types'
```

- [ ] **Step 2: 提交导出配置**

```bash
git add src/components/index.ts
git commit -m "feat: add BlinkTable component exports

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: 更新前端规范文档

**Files:**
- Modify: `docs/rules/frontend-rules.md`

- [ ] **Step 1: 在文档末尾添加 BlinkTable 组件使用规范**

```markdown
## 19. BlinkTable 表格组件规范

### 19.1 基本用法

使用 `BlinkTable` 替代直接使用 `el-table`，统一表格风格：

```vue
<template>
  <BlinkTable
    :data="userList"
    :columns="columns"
    :loading="loading"
    :selectable="true"
    v-model:selected-keys="selectedIds"
    @selection-change="handleSelectionChange"
  />
</template>
```

### 19.2 列配置（配置模式）

```typescript
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
  { prop: 'createTime', label: '创建时间', type: 'datetime', width: 160 },
]
```

### 19.3 操作列配置

```typescript
const operations: TableOperation[] = [
  { label: '编辑', type: 'primary', onClick: handleEdit },
  {
    label: '删除',
    type: 'danger',
    onClick: handleDelete,
    visible: (row) => row.roleId !== 1  // 超级管理员不显示删除按钮
  },
]
```

### 19.4 插槽模式（复杂场景）

```vue
<template>
  <BlinkTable :data="userList" :loading="loading">
    <BlinkTableColumn prop="loginName" label="登录名" />
    <BlinkTableColumn prop="avatar" label="头像">
      <template #default="{ row }">
        <el-avatar :src="getAvatarUrl(row.avatar)" />
      </template>
    </BlinkTableColumn>
    <BlinkTableColumn prop="status" label="状态" type="tag" :tag-type="getStatusType" />
  </BlinkTable>
</template>
```

### 19.5 选择功能

```vue
<template>
  <BlinkTable
    :data="userList"
    :selectable="true"
    v-model:selected-keys="selectedIds"
    :check-selectable="checkSelectable"
    @selection-change="handleSelectionChange"
  />
</template>

<script setup lang="ts">
// 判断行是否可选
const checkSelectable = (row: UserInfo) => {
  return row.superFlag !== 1  // 超级管理员不可选
}
</script>
```

### 19.6 Expose 方法

```vue
<script setup lang="ts">
const tableRef = ref()

// 清空选择
const clearSelection = () => {
  tableRef.value?.clearSelection()
}
</script>

<template>
  <BlinkTable ref="tableRef" :data="userList" :selectable="true" />
</template>
```
```

- [ ] **Step 2: 提交文档更新**

```bash
git add docs/rules/frontend-rules.md
git commit -m "docs: add BlinkTable component usage guide

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: 验证组件功能

- [ ] **Step 1: 验证配置模式功能**

1. 表格数据正常渲染
2. 列配置生效
3. 选择功能正常
4. 排序功能正常
5. 操作列按钮正常显示和点击

- [ ] **Step 2: 验证插槽模式功能**

1. 自定义列内容正常渲染
2. 操作列插槽正常

- [ ] **Step 3: 验证深色模式**

切换深色模式，检查表格样式适配

- [ ] **Step 4: 确认完成后查看提交记录**

```bash
git log --oneline -10
```

---

## 完成标准

- [ ] 组件类型定义完整
- [ ] 配置模式正常工作
- [ ] 插槽模式正常工作
- [ ] 选择功能正常
- [ ] 操作列功能正常
- [ ] Expose 方法可用
- [ ] 深色模式适配
- [ ] 文档更新完成
- [ ] 代码已提交到 git