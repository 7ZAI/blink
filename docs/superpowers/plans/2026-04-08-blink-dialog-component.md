# BlinkDialog 组件实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建统一的弹窗容器组件，封装标题、宽度、关闭逻辑、loading状态、底部按钮等通用功能。

**Architecture:** 基于 Element Plus 的 el-dialog 进行二次封装，通过 props 控制行为，通过 slots 支持自定义内容。组件与业务逻辑解耦，仅负责弹窗容器的通用功能。

**Tech Stack:** Vue 3 + TypeScript + Element Plus

---

## 文件结构

```
src/components/BlinkDialog/
├── index.vue          # 主组件
└── types.ts           # 类型定义
```

---

### Task 1: 创建类型定义文件

**Files:**
- Create: `src/components/BlinkDialog/types.ts`

- [ ] **Step 1: 创建 types.ts 文件，定义 Props、Emits、Slots 类型**

```typescript
// src/components/BlinkDialog/types.ts

/**
 * BlinkDialog 组件 Props 类型
 */
export interface BlinkDialogProps {
  // 基础配置
  modelValue: boolean
  title?: string
  width?: string | number

  // 行为配置
  closeOnClickModal?: boolean
  closeOnPressEscape?: boolean
  showClose?: boolean
  lockScroll?: boolean

  // 状态
  loading?: boolean
  confirmLoading?: boolean

  // 底部按钮
  showFooter?: boolean
  showCancel?: boolean
  showConfirm?: boolean
  cancelText?: string
  confirmText?: string
  confirmType?: 'primary' | 'success' | 'warning' | 'danger'

  // 关闭确认
  beforeClose?: (done: () => void) => void

  // 样式
  customClass?: string
  destroyOnClose?: boolean
}

/**
 * BlinkDialog 组件 Emits 类型
 */
export interface BlinkDialogEmits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
  (e: 'close'): void
  (e: 'open'): void
  (e: 'opened'): void
  (e: 'closed'): void
}
```

- [ ] **Step 2: 提交类型定义文件**

```bash
git add src/components/BlinkDialog/types.ts
git commit -m "feat(BlinkDialog): add type definitions

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 创建主组件

**Files:**
- Create: `src/components/BlinkDialog/index.vue`

- [ ] **Step 1: 创建 index.vue 组件骨架**

```vue
<!-- src/components/BlinkDialog/index.vue -->
<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="computedWidth"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    :show-close="showClose"
    :lock-scroll="lockScroll"
    :before-close="handleBeforeClose"
    :class="['blink-dialog', customClass]"
    :destroy-on-close="destroyOnClose"
    @open="emit('open')"
    @opened="emit('opened')"
    @closed="handleClosed"
  >
    <!-- 内容区域 -->
    <div v-loading="loading" class="blink-dialog__body">
      <slot />
    </div>

    <!-- 底部区域 -->
    <template v-if="showFooter" #footer>
      <slot name="footer">
        <div class="blink-dialog__footer">
          <el-button v-if="showCancel" @click="handleCancel">
            {{ cancelText }}
          </el-button>
          <el-button
            v-if="showConfirm"
            :type="confirmType"
            :loading="confirmLoading"
            @click="handleConfirm"
          >
            {{ confirmText }}
          </el-button>
        </div>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BlinkDialogProps, BlinkDialogEmits } from './types'

defineOptions({
  name: 'BlinkDialog',
})

const props = withDefaults(defineProps<BlinkDialogProps>(), {
  title: '',
  width: '500px',
  closeOnClickModal: false,
  closeOnPressEscape: true,
  showClose: true,
  lockScroll: false,
  loading: false,
  confirmLoading: false,
  showFooter: true,
  showCancel: true,
  showConfirm: true,
  cancelText: '取消',
  confirmText: '确定',
  confirmType: 'primary',
  customClass: '',
  destroyOnClose: false,
})

const emit = defineEmits<BlinkDialogEmits>()

// 双向绑定
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// 宽度处理
const computedWidth = computed(() => {
  if (typeof props.width === 'number') {
    return `${props.width}px`
  }
  return props.width
})

// 关闭前处理
const handleBeforeClose = (done: () => void) => {
  if (props.beforeClose) {
    props.beforeClose(done)
  } else {
    done()
  }
}

// 取消按钮
const handleCancel = () => {
  emit('cancel')
  visible.value = false
}

// 确认按钮
const handleConfirm = () => {
  emit('confirm')
}

// 关闭完成
const handleClosed = () => {
  emit('close')
  emit('closed')
}

// Slots 定义
defineSlots<{
  default?: () => any
  footer?: () => any
}>()
</script>
```

- [ ] **Step 2: 添加组件样式**

在 `<style>` 部分添加：

```scss
<style scoped lang="scss">
.blink-dialog {
  :deep(.el-dialog) {
    border-radius: 12px;
    overflow: hidden;
  }

  :deep(.el-dialog__header) {
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-color-light);
    margin-right: 0;

    .el-dialog__title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color-primary);
    }
  }

  &__body {
    padding: 20px;
    color: var(--text-color-regular);
    min-height: 50px;
  }

  :deep(.el-dialog__footer) {
    padding: 12px 20px;
    border-top: 1px solid var(--border-color-light);
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

// 深色模式适配
.dark .blink-dialog {
  :deep(.el-dialog__header) {
    border-bottom-color: var(--border-color-light);
  }

  :deep(.el-dialog__footer) {
    border-top-color: var(--border-color-light);
  }
}
</style>
```

- [ ] **Step 3: 提交主组件文件**

```bash
git add src/components/BlinkDialog/index.vue
git commit -m "feat(BlinkDialog): implement main component

- Props-driven configuration
- Support v-model binding
- Support custom footer via slot
- Dark mode compatible

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 添加组件导出

**Files:**
- Modify: `src/components/index.ts` (如果存在)
- Create: `src/components/index.ts` (如果不存在)

- [ ] **Step 1: 检查并更新组件导出文件**

先检查 `src/components/index.ts` 是否存在：

```bash
ls -la src/components/index.ts
```

如果存在，读取并添加导出；如果不存在，创建新文件：

```typescript
// src/components/index.ts
export { default as BlinkDialog } from './BlinkDialog/index.vue'
export * from './BlinkDialog/types'
```

- [ ] **Step 2: 提交导出配置**

```bash
git add src/components/index.ts
git commit -m "feat: add BlinkDialog component export

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: 更新前端规范文档

**Files:**
- Modify: `docs/rules/frontend-rules.md`

- [ ] **Step 1: 在 frontend-rules.md 末尾添加 BlinkDialog 组件使用规范**

```markdown
## 18. BlinkDialog 弹窗组件规范

### 18.1 基本用法

使用 `BlinkDialog` 替代直接使用 `el-dialog`，统一弹窗风格：

```vue
<template>
  <BlinkDialog
    v-model="visible"
    title="新增用户"
    @confirm="handleSubmit"
  >
    <el-form>
      <!-- 表单内容 -->
    </el-form>
  </BlinkDialog>
</template>
```

### 18.2 Props 配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modelValue | boolean | - | 控制显示，支持 v-model |
| title | string | '' | 弹窗标题 |
| width | string \| number | '500px' | 弹窗宽度 |
| loading | boolean | false | 内容区域 loading |
| confirmLoading | boolean | false | 确认按钮 loading |
| showFooter | boolean | true | 是否显示底部 |
| showCancel | boolean | true | 是否显示取消按钮 |
| showConfirm | boolean | true | 是否显示确认按钮 |
| cancelText | string | '取消' | 取消按钮文本 |
| confirmText | string | '确定' | 确认按钮文本 |
| confirmType | string | 'primary' | 确认按钮类型 |
| closeOnClickModal | boolean | false | 点击遮罩关闭 |
| beforeClose | function | - | 关闭前回调 |

### 18.3 自定义底部

使用 `#footer` 插槽自定义底部内容：

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

### 18.4 关闭确认

使用 `beforeClose` 实现关闭确认：

```vue
<script setup lang="ts">
const hasChanged = ref(false)

const handleBeforeClose = (done: () => void) => {
  if (hasChanged.value) {
    ElMessageBox.confirm('有未保存的更改，确定关闭吗？', '提示', {
      type: 'warning'
    }).then(() => {
      done()
    }).catch(() => {})
  } else {
    done()
  }
}
</script>

<template>
  <BlinkDialog v-model="visible" :before-close="handleBeforeClose">
    <!-- 内容 -->
  </BlinkDialog>
</template>
```

### 18.5 Events

| 事件 | 说明 |
|------|------|
| confirm | 点击确认按钮 |
| cancel | 点击取消按钮 |
| open | 弹窗打开 |
| opened | 弹窗打开动画结束 |
| close | 弹窗关闭 |
| closed | 弹窗关闭动画结束 |
```

- [ ] **Step 2: 提交文档更新**

```bash
git add docs/rules/frontend-rules.md
git commit -m "docs: add BlinkDialog component usage guide

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: 验证组件功能

- [ ] **Step 1: 创建测试页面验证组件**

在开发服务器中访问任意包含弹窗的页面，将现有 `el-dialog` 临时替换为 `BlinkDialog` 验证功能：

1. 基础显示/隐藏
2. 标题显示
3. 底部按钮点击
4. Loading 状态
5. 深色模式适配

- [ ] **Step 2: 确认组件正常工作后，完成最终提交**

```bash
git status
git log --oneline -5
```

---

## 完成标准

- [ ] 组件类型定义完整
- [ ] 组件 Props 响应正常
- [ ] 组件 Events 触发正常
- [ ] 组件 Slots 渲染正常
- [ ] 深色模式适配
- [ ] 文档更新完成
- [ ] 代码已提交到 git