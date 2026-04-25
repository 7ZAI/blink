# BlinkTaskDialog 组件设计文档

**日期**: 2026-04-25
**类型**: 前端组件设计
**状态**: 待审批

---

## 1. 概述

### 1.1 组件定位

`BlinkTaskDialog` 是一个等待任务完成的弹窗组件，用于展示正在执行的任务状态和进度。支持多种进度信息格式（百分比进度、阶段状态、不确定时长），提供灵活的交互控制（取消任务、后台执行）。

### 1.2 使用场景

- 单任务等待场景（数据导出、部署流程、批量处理）
- 任务时长可预估或不可预估
- 用户需要等待或可后台执行

### 1.3 设计目标

- **简洁干净**的视觉风格，突出功能性
- **混合模式**支持多种进度数据格式
- **可配置**的用户交互行为
- **灵活**的任务执行与回调方式
- **符合项目规范**：使用 composables 模式、统一 CSS 变量、国际化支持

---

## 2. 架构设计

### 2.1 模块结构

```
src/components/BlinkTaskDialog/
├── index.vue              # 主组件
├── types.ts               # 类型定义
├── composables/
│   └── useTaskRunner.ts   # 核心任务状态管理
├── components/
│   ├── ProgressBar.vue    # 进度条组件
│   ├── StepsIndicator.vue # 步骤指示器组件
│   ├── Spinner.vue        # 加载动画组件
│   └── ResultPanel.vue    # 结果展示面板
└── locale/
    └ zh-cn.ts             # 中文国际化
    └ en-us.ts             # 英文国际化
```

### 2.2 核心架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      BlinkTaskDialog                         │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │   useTaskRunner │  │  BlinkTaskDialog│                   │
│  │   (Composable)  │──│    (Component)  │                   │
│  └─────────────────┘  └─────────────────┘                   │
│         │                     │                              │
│         │    state/methods    │                              │
│         ▼                     ▼                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    Task State                            ││
│  │  - visible, status, progress, message, result, error    ││
│  └─────────────────────────────────────────────────────────┘│
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                  Render Components                       ││
│  │  ProgressBar | StepsIndicator | Spinner | ResultPanel   ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘

便捷函数层:
┌─────────────────────────────────────────────────────────────┐
│  runTaskDialog(options)  →  快速启动任务弹窗                  │
│  showTaskDialog(options) →  带返回结果的函数式调用            │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 职责划分

| 模块 | 职责 |
|------|------|
| `useTaskRunner` | 任务状态管理、生命周期控制、进度追踪 |
| `BlinkTaskDialog` | UI 渲染、用户交互、样式处理 |
| `runTaskDialog` | 快速启动弹窗的便捷函数 |
| 子组件 | 具体渲染元素（进度条、步骤、结果） |

---

## 3. Composable API 设计

### 3.1 useTaskRunner

核心任务状态管理 Composable，负责任务执行的整个生命周期。

```typescript
import { useTaskRunner, TaskRunnerOptions, TaskRunnerReturn } from '@blink/components'

// 使用示例
const { state, start, cancel, complete, updateProgress } = useTaskRunner({
  onComplete: (result) => console.log('完成', result),
  onCancel: () => console.log('取消'),
  onError: (error) => console.error('错误', error)
})

// 启动任务
start({
  task: async (onProgress) => {
    // 任务执行逻辑
    onProgress({ percent: 30, message: '处理中...' })
    const result = await doSomething()
    return result
  },
  title: '正在导出数据',
  cancellable: true
})
```

### 3.2 类型定义

```typescript
// 任务状态
interface TaskState {
  visible: boolean              // 弹窗是否显示
  status: TaskStatus            // 任务状态
  progress: TaskProgress        // 进度信息
  title: string                 // 任务标题
  message: string               // 当前消息
  result: TaskResult | null     // 任务结果
  error: Error | null           // 错误信息
  elapsedTime: number           // 已耗时（毫秒）
  estimatedTime: number | null  // 预计剩余时间（毫秒）
}

// 任务状态枚举
enum TaskStatus {
  IDLE = 'idle',           // 空闲
  RUNNING = 'running',     // 执行中
  PAUSED = 'paused',       // 已暂停
  COMPLETED = 'completed', // 已完成
  FAILED = 'failed',       // 已失败
  CANCELLED = 'cancelled'  // 已取消
}

// 进度信息（混合模式支持）
interface TaskProgress {
  type: 'percent' | 'steps' | 'indeterminate'  // 进度类型
  value: number | null                          // 百分比进度值 (0-100)
  steps: StepInfo[] | null                      // 步骤信息
  currentStep: number | null                    // 当前步骤索引
}

interface StepInfo {
  name: string           // 步骤名称
  status: StepStatus     // 步骤状态
  message?: string       // 步骤消息
}

enum StepStatus {
  PENDING = 'pending',     // 待执行
  RUNNING = 'running',     // 执行中
  COMPLETED = 'completed', // 已完成
  FAILED = 'failed'        // 已失败
}

// 任务结果
interface TaskResult {
  success: boolean              // 是否成功
  data?: any                    // 结果数据
  summary?: string              // 结果摘要
  actions?: ResultAction[]      // 后续操作按钮
}

interface ResultAction {
  label: string         // 按钮文字
  type: 'primary' | 'default' | 'link'  // 按钮类型
  handler: () => void   // 点击处理
}

// 任务函数类型
type TaskFunction<T = any> = (
  onProgress: (progress: ProgressUpdate) => void
) => Promise<T>

interface ProgressUpdate {
  percent?: number              // 百分比进度
  message?: string              // 当前消息
  step?: number                 // 当前步骤索引
  stepMessage?: string          // 步骤消息
  estimatedTime?: number        // 预估剩余时间（毫秒）
}

// useTaskRunner 参数
interface TaskRunnerOptions {
  onComplete?: (result: any) => void      // 完成回调
  onCancel?: () => void                   // 取消回调
  onError?: (error: Error) => void        // 错误回调
  autoCloseDelay?: number                 // 成功后自动关闭延迟（毫秒）
  notifyOnComplete?: boolean              // 后台执行时是否通知
}

// useTaskRunner 返回值
interface TaskRunnerReturn {
  state: Ref<TaskState>                   // 任务状态（响应式）
  start: (options: StartOptions) => Promise<any>  // 启动任务
  cancel: () => void                      // 取消任务
  pause: () => void                       // 暂停任务（可选）
  resume: () => void                      // 继续任务（可选）
  updateProgress: (update: ProgressUpdate) => void  // 手动更新进度
  reset: () => void                       // 重置状态
}

// 启动参数
interface StartOptions {
  task: TaskFunction | (() => Promise<any>)  // 任务函数
  title?: string                              // 任务标题
  message?: string                            // 初始消息
  progressType?: 'percent' | 'steps' | 'indeterminate'  // 进度类型（可选，根据 onProgress 自动判断）
  steps?: string[]                            // 步骤名称列表（仅 steps 模式需要）
  cancellable?: boolean                       // 是否可取消（默认 false）
  backgroundable?: boolean                    // 是否可后台执行（默认 false）
  onCompleteBehavior?: 'auto-close' | 'show-result' | 'show-actions'  // 完成行为（默认 'auto-close'）
  autoCloseDelay?: number                     // 成功后自动关闭延迟（仅 auto-close 模式，默认 1500ms）
  resultActions?: ResultAction[]              // 完成后操作按钮（仅 show-actions 模式）
}

// progressType 自动检测规则：
// - 未指定时，根据 onProgress(update) 参数自动判断：
//   - update.percent 有值 → percent 模式
//   - update.step 有值 → steps 模式
//   - 无进度更新 → indeterminate 模式
```

---

## 4. 组件 API 设计

### 4.1 BlinkTaskDialog Props

```typescript
interface BlinkTaskDialogProps {
  // 状态控制
  modelValue: boolean                     // 弹窗显示状态
  status: TaskStatus                      // 任务状态
  progress: TaskProgress                  // 进度信息

  // 内容配置
  title?: string                          // 任务标题
  message?: string                        // 当前消息
  elapsedTime?: number                    // 已耗时（毫秒）
  estimatedTime?: number | null           // 预估剩余时间

  // 结果展示
  result?: TaskResult | null              // 任务结果
  error?: Error | null                    // 错误信息

  // 交互配置
  cancellable?: boolean                   // 是否可取消（默认 false）
  backgroundable?: boolean                // 是否可后台执行（默认 false）
  closeOnClickModal?: boolean             // 点击遮罩关闭（默认 false）
  showCloseButton?: boolean               // 显示关闭按钮（默认根据 backgroundable）

  // 样式配置
  width?: string | number                 // 弹窗宽度（默认 400px）
  customClass?: string                    // 自定义样式类
}
```

### 4.2 BlinkTaskDialog Events

```typescript
interface BlinkTaskDialogEmits {
  (e: 'update:modelValue', value: boolean): void  // 更新显示状态
  (e: 'cancel'): void                             // 取消任务
  (e: 'background'): void                         // 后台执行
  (e: 'close'): void                              // 关闭弹窗
  (e: 'action', action: ResultAction): void       // 点击结果操作按钮
}
```

### 4.3 BlinkTaskDialog Slots

```typescript
interface BlinkTaskDialogSlots {
  default?: () => any                   // 自定义内容区域
  header?: () => any                    // 自定义头部
  footer?: () => any                    // 自定义底部
  progress?: () => any                  // 自定义进度展示
  result?: (props: { result: TaskResult }) => any  // 自定义结果展示
}
```

---

## 5. 便捷函数设计

### 5.1 runTaskDialog

快速启动任务弹窗，返回 Promise。简化版本，适合简单场景。

```typescript
import { runTaskDialog } from '@blink/components'

// 基本使用
const result = await runTaskDialog({
  task: async (onProgress) => {
    onProgress({ percent: 50, message: '处理中...' })
    return await exportData(params)
  },
  title: '正在导出数据',
  cancellable: true
})

// 返回值
interface RunTaskDialogResult<T = any> {
  success: boolean
  data: T | null
  cancelled: boolean
  error: Error | null
}

// runTaskDialog 默认配置：
// - onCompleteBehavior: 'auto-close'
// - autoCloseDelay: 1500ms
// - cancellable: false
// - backgroundable: false
```

### 5.2 showTaskDialog

显示任务弹窗，支持完整配置选项。适合需要自定义完成行为的场景。

```typescript
import { showTaskDialog } from '@blink/components'

// 使用步骤进度
await showTaskDialog({
  task: async (onProgress) => {
    onProgress({ step: 0, stepMessage: '初始化...' })
    await init()
    onProgress({ step: 1, stepMessage: '处理数据...' })
    await processData()
    onProgress({ step: 2, stepMessage: '保存结果...' })
    return await save()
  },
  title: '批量处理',
  progressType: 'steps',
  steps: ['初始化', '处理数据', '保存结果'],
  onCompleteBehavior: 'show-actions',
  resultActions: [
    { label: '查看结果', type: 'primary', handler: () => goToResult() },
    { label: '关闭', type: 'default', handler: () => {} }
  ]
})

// 不确定时长任务
await showTaskDialog({
  task: async () => {
    // 无法预估进度的任务
    await waitForApproval()
  },
  title: '等待审批',
  progressType: 'indeterminate',
  backgroundable: true,
  notifyOnComplete: true
})
```

---

## 6. 使用示例

### 6.1 Composable 模式（精细控制）

```vue
<template>
  <BlinkTaskDialog
    v-model="state.visible"
    :status="state.status"
    :progress="state.progress"
    :title="state.title"
    :message="state.message"
    :result="state.result"
    :error="state.error"
    :elapsed-time="state.elapsedTime"
    :cancellable="true"
    @cancel="cancel"
  />
</template>

<script setup lang="ts">
import { BlinkTaskDialog, useTaskRunner } from '@blink/components'

const { state, start, cancel } = useTaskRunner({
  onComplete: (result) => {
    ElMessage.success('导出成功')
  }
})

const handleExport = async () => {
  await start({
    task: async (onProgress) => {
      const total = dataList.length
      for (let i = 0; i < total; i++) {
        await processItem(dataList[i])
        onProgress({
          percent: Math.round((i + 1) / total * 100),
          message: `处理第 ${i + 1}/${total} 条...`
        })
      }
      return { filename: 'export.xlsx', count: total }
    },
    title: '正在导出数据',
    progressType: 'percent'
  })
}
</script>
```

### 6.2 便捷函数模式（快速调用）

```vue
<script setup lang="ts">
import { runTaskDialog } from '@blink/components'

const handleExport = async () => {
  const result = await runTaskDialog({
    task: async (onProgress) => {
      // 任务逻辑
      return await exportAPI(params)
    },
    title: '正在导出数据'
  })

  if (result.success) {
    ElMessage.success(`已导出 ${result.data.count} 条数据`)
  }
}
</script>
```

### 6.3 手动控制模式（外部驱动进度）

```vue
<template>
  <BlinkTaskDialog ref="taskDialogRef" />
</template>

<script setup lang="ts">
import { BlinkTaskDialog } from '@blink/components'

const taskDialogRef = useTemplateRef('taskDialogRef')

const handleUpload = async () => {
  // 开始任务
  taskDialogRef.value?.start({
    title: '上传文件',
    progressType: 'percent'
  })

  try {
    // 使用第三方上传库（如 tus.js）
    uploader.on('progress', (progress) => {
      taskDialogRef.value?.updateProgress({
        percent: Math.round(progress * 100),
        message: `已上传 ${formatSize(progress.loaded)}/${formatSize(progress.total)}`
      })
    })

    await uploader.start()
    taskDialogRef.value?.complete({
      success: true,
      summary: '上传完成'
    })
  } catch (error) {
    taskDialogRef.value?.fail(error)
  }
}
</script>
```

---

## 7. 视觉设计

### 7.1 弹窗布局

```
┌────────────────────────────────────┐
│  ×                                  │  ← 关闭按钮（根据 backgroundable）
├────────────────────────────────────┤
│                                     │
│         ┌───────────────┐           │  ← 状态图标区域
│         │   Spinner     │           │     - 执行中：旋转动画
│         │   ✓ (成功)    │           │     - 完成：绿色勾
│         │   ✗ (失败)    │           │     - 失败：红色叉
│         └───────────────┘           │
│                                     │
│  正在导出数据...                     │  ← 标题
│                                     │
│  ┌────────────────────────────────┐ │  ← 进度展示区域
│  │ ████████████░░░░░░░░░░░░░░░░░ │ │     - 百分比进度条
│  │           45%                  │ │     - 或步骤指示器
│  └────────────────────────────────┘ │     - 或旋转动画
│                                     │
│  处理第 45 条数据                    │  ← 当前消息
│  已耗时: 2s  预计剩余: 3s           │  ← 时间信息（可选）
│                                     │
├────────────────────────────────────┤
│         [ 取消任务 ]                │  ← 操作按钮区（根据 cancellable）
└────────────────────────────────────┘
```

### 7.2 样式规范

遵循项目现有 CSS 变量系统：

```scss
.blink-task-dialog {
  --dialog-width: 400px;
  --icon-size: 48px;
  --progress-height: 6px;

  background: var(--card-bg);
  border: 1px solid var(--border-color-light);
  border-radius: 12px;
  box-shadow: var(--card-shadow);
}

// 进度条
.progress-bar {
  background: var(--border-color-light);
  border-radius: 3px;

  .progress-fill {
    background: var(--primary-color);
    border-radius: 3px;
    transition: width 0.3s ease;
  }
}

// 状态图标
.status-icon {
  color: var(--primary-color);   // 执行中
  color: var(--success-color);   // 成功
  color: var(--danger-color);    // 失败
}
```

### 7.3 深色模式适配

组件使用 CSS 变量，自动适配深色模式，无需额外配置。

---

## 8. 国际化支持

### 8.1 国际化文本

```typescript
// zh-cn.ts
export const zhCn = {
  taskDialog: {
    cancel: '取消任务',
    background: '后台执行',
    close: '关闭',
    running: '正在执行...',
    completed: '任务完成',
    failed: '任务失败',
    cancelled: '已取消',
    elapsedTime: '已耗时',
    estimatedTime: '预计剩余',
    progress: '进度',
    stepProgress: '步骤',
    waitProgress: '等待完成...',
    download: '下载',
    view: '查看',
    successSummary: '执行成功',
    errorSummary: '执行失败',
  }
}
```

组件自动从 `BlinkRequestContextHolder` 获取当前语言设置，或通过 props 传入 locale。

---

## 9. 错误处理

### 9.1 任务执行错误

```typescript
// useTaskRunner 内部错误处理
try {
  const result = await task(onProgress)
  handleComplete(result)
} catch (error) {
  handleError(error)
  // 自动更新状态为 FAILED
  // 显示错误信息
  // 调用 onError 回调
}
```

### 9.2 取消任务处理

```typescript
// 任务函数应支持 AbortSignal
const abortController = new AbortController()

start({
  task: async (onProgress, signal) => {
    // signal 是 AbortSignal
    const response = await fetch(url, { signal })
    return response.json()
  }
})

// 用户取消时
cancel()  // → abortController.abort()
```

### 9.3 边界情况

| 场景 | 处理方式 |
|------|----------|
| 任务函数抛出异常 | 状态变为 FAILED，显示错误信息 |
| 任务函数返回 undefined | 视为成功，result.data 为 null |
| 进度值超过 100% | 自动截断为 100% |
| 进度值小于 0% | 自动修正为 0% |
| 任务完成后再次 start | 重置状态后启动新任务 |
| 组件销毁时任务仍在执行 | 自动取消任务，清理资源 |

---

## 10. 测试策略

### 10.1 单元测试

- `useTaskRunner` 状态管理逻辑
- 进度计算和时间估算
- 取消和错误处理

### 10.2 组件测试

- 不同状态下的渲染输出
- 进度条动画效果
- 用户交互（取消、后台执行）
- 结果面板展示

### 10.3 集成测试

- 与实际 API 调用的集成
- 多任务并发测试
- 后台执行通知测试

---

## 11. 实现计划

### Phase 1: 核心实现
1. `useTaskRunner` Composable
2. `BlinkTaskDialog` 主组件
3. 基础进度条渲染
4. 百分比进度模式

### Phase 2: 功能完善
1. 步骤进度模式
2. 不确定时长模式
3. 取消和后台执行
4. 结果面板

### Phase 3: 便捷封装
1. `runTaskDialog` 函数
2. `showTaskDialog` 函数
3. 手动控制 API
4. 国际化集成

### Phase 4: 测试与文档
1. 单元测试
2. 组件测试
3. Demo 页面
4. 使用文档

---

## 12. API 清单

导出项：

```typescript
// 组件
export { default as BlinkTaskDialog } from './index.vue'

// Composable
export { useTaskRunner } from './composables/useTaskRunner'
export type {
  TaskState,
  TaskStatus,
  TaskProgress,
  TaskResult,
  TaskRunnerOptions,
  TaskRunnerReturn,
  StartOptions,
  ProgressUpdate,
} from './types'

// 便捷函数
export { runTaskDialog, showTaskDialog } from './functions'
export type { RunTaskDialogResult, ShowTaskDialogOptions } from './types'

// 子组件（可选导出）
export { default as ProgressBar } from './components/ProgressBar.vue'
export { default as StepsIndicator } from './components/StepsIndicator.vue'
export { default as Spinner } from './components/Spinner.vue'
export { default as ResultPanel } from './components/ResultPanel.vue'
```

---

## 13. 待确认事项

- [x] 进度信息获取方式：混合模式
- [x] 任务执行方式：支持多种模式
- [x] 用户交互行为：可配置
- [x] 视觉风格：简洁干净
- [x] 完成后行为：可配置
- [x] 架构方案：Hooks + 组件组合

---

**审批状态**: 待用户确认