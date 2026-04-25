<!-- src/components/BlinkTaskDialog/index.vue -->
<template>
  <el-dialog
    v-model="visible"
    :title="displayTitle"
    :width="computedWidth"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="!isRunning"
    :show-close="showCloseButton"
    :lock-scroll="true"
    :class="['blink-task-dialog', customClass]"
    :destroy-on-close="true"
    @closed="handleClosed"
  >
    <!-- 内容区域 -->
    <div class="blink-task-dialog__body">
      <!-- 执行中状态：显示进度/加载动画 -->
      <div v-if="isRunning" class="blink-task-dialog__running">
        <!-- 消息区域 -->
        <div v-if="message" class="blink-task-dialog__message">
          {{ message }}
        </div>

        <!-- 进度显示区域：根据进度类型显示不同组件 -->
        <div class="blink-task-dialog__progress">
          <!-- 百分比进度：显示进度条 -->
          <ProgressBar
            v-if="progress.type === 'percent'"
            :percent="progress.value ?? 0"
            :estimated-time="estimatedTime"
          />

          <!-- 步骤进度：显示步骤指示器 -->
          <StepsIndicator
            v-else-if="progress.type === 'steps'"
            :steps="progress.steps ?? []"
          />

          <!-- 不确定进度：显示加载动画 -->
          <Spinner v-else :size="48" />
        </div>

        <!-- 时间显示区域 -->
        <div class="blink-task-dialog__time-info">
          <span v-if="elapsedTime > 0" class="blink-task-dialog__elapsed">
            已用时: {{ formatTime(elapsedTime) }}
          </span>
          <span v-if="estimatedTime && progress.type === 'percent'" class="blink-task-dialog__estimated">
            预计剩余: {{ formatTime(estimatedTime) }}
          </span>
        </div>
      </div>

      <!-- 非执行中状态：显示结果面板 -->
      <ResultPanel
        v-else
        :status="status"
        :result="result"
        :error="error"
        @action="handleAction"
      />
    </div>

    <!-- 底部按钮区域 -->
    <template #footer>
      <div class="blink-task-dialog__footer">
        <!-- 可取消 + 执行中：显示取消按钮 -->
        <el-button
          v-if="cancellable && isRunning"
          @click="handleCancel"
        >
          取消任务
        </el-button>

        <!-- 可后台执行 + 执行中：显示后台执行按钮 -->
        <el-button
          v-if="backgroundable && isRunning"
          type="primary"
          @click="handleBackground"
        >
          后台执行
        </el-button>

        <!-- 非执行中状态：显示关闭按钮 -->
        <el-button
          v-if="!isRunning"
          type="primary"
          @click="handleClose"
        >
          关闭
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * BlinkTaskDialog 主组件
 *
 * 任务执行进度弹窗组件，整合子组件实现完整的任务状态展示。
 * 支持三种进度展示模式：百分比进度条、步骤指示器、不确定进度动画。
 * 支持取消任务、后台执行、结果展示等功能。
 *
 * @author binblink
 * @since 2026-04-25
 */
import { computed } from 'vue'
import { ElDialog, ElButton } from 'element-plus'
import ProgressBar from './components/ProgressBar.vue'
import Spinner from './components/Spinner.vue'
import StepsIndicator from './components/StepsIndicator.vue'
import ResultPanel from './components/ResultPanel.vue'
import { TaskStatus } from './types'
import type { BlinkTaskDialogProps, BlinkTaskDialogEmits, TaskProgress, TaskResult, ResultAction } from './types'

defineOptions({
  name: 'BlinkTaskDialog',
})

/**
 * 组件 Props 定义
 */
const props = withDefaults(defineProps<BlinkTaskDialogProps>(), {
  modelValue: false,
  status: TaskStatus.IDLE,
  progress: () => ({ type: 'indeterminate', value: null, steps: null, currentStep: null }),
  title: '任务执行',
  message: '',
  elapsedTime: 0,
  estimatedTime: null,
  result: null,
  error: null,
  cancellable: false,
  backgroundable: false,
  closeOnClickModal: false,
  showCloseButton: false,
  width: '400px',
  customClass: '',
})

/**
 * 组件 Emits 定义
 */
const emit = defineEmits<BlinkTaskDialogEmits>()

/**
 * 双向绑定：弹窗显示状态
 */
const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val),
})

/**
 * 计算宽度值
 * 处理数字类型转换为像素单位
 */
const computedWidth = computed(() => {
  if (typeof props.width === 'number') {
    return `${props.width}px`
  }
  return props.width
})

/**
 * 判断是否处于执行中状态
 * 包括 RUNNING 和 PAUSED 状态
 */
const isRunning = computed(() => {
  return props.status === TaskStatus.RUNNING || props.status === TaskStatus.PAUSED
})

/**
 * 显示标题
 * 根据任务状态动态调整标题
 */
const displayTitle = computed(() => {
  if (!isRunning.value) {
    // 根据结果状态调整标题
    if (props.status === TaskStatus.COMPLETED && props.result?.success) {
      return `${props.title} - 完成`
    }
    if (props.status === TaskStatus.FAILED) {
      return `${props.title} - 失败`
    }
    if (props.status === TaskStatus.CANCELLED) {
      return `${props.title} - 已取消`
    }
  }
  return props.title
})

/**
 * 格式化时间显示
 * 将毫秒转换为友好的显示格式（秒/分钟）
 *
 * @param ms 毫秒数
 * @returns 格式化后的时间字符串
 */
const formatTime = (ms: number): string => {
  if (ms <= 0) {
    return '0秒'
  }

  // 大于 60 秒显示分钟
  if (ms >= 60000) {
    const minutes = Math.floor(ms / 60000)
    const seconds = Math.floor((ms % 60000) / 1000)
    if (seconds > 0) {
      return `${minutes}分${seconds}秒`
    }
    return `${minutes}分钟`
  }

  // 大于 1 秒显示秒数
  if (ms >= 1000) {
    const seconds = Math.floor(ms / 1000)
    return `${seconds}秒`
  }

  // 小于 1 秒显示毫秒
  return `${ms}毫秒`
}

/**
 * 处理取消任务按钮点击
 */
const handleCancel = () => {
  emit('cancel')
}

/**
 * 处理后台执行按钮点击
 */
const handleBackground = () => {
  emit('background')
  visible.value = false
}

/**
 * 处理关闭按钮点击
 */
const handleClose = () => {
  emit('close')
  visible.value = false
}

/**
 * 处理弹窗关闭完成事件
 */
const handleClosed = () => {
  // 弹窗关闭后可进行清理操作
}

/**
 * 处理结果操作按钮点击
 *
 * @param action 操作按钮配置
 */
const handleAction = (action: ResultAction) => {
  emit('action', action)
}
</script>

<style scoped lang="scss">
.blink-task-dialog {
  // 弹窗样式
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

  // 内容区域
  &__body {
    padding: 20px;
    min-height: 150px;
  }

  // 执行中状态容器
  &__running {
    display: flex;
    flex-direction: column;
    gap: 16px;
    align-items: center;
  }

  // 消息区域
  &__message {
    font-size: 14px;
    color: var(--text-color-regular);
    text-align: center;
    line-height: 1.5;
    max-width: 100%;
    word-break: break-word;
  }

  // 进度显示区域
  &__progress {
    width: 100%;
    display: flex;
    justify-content: center;
    padding: 8px 0;
  }

  // 时间显示区域
  &__time-info {
    display: flex;
    justify-content: center;
    gap: 16px;
    font-size: 13px;
    color: var(--text-color-secondary);
  }

  &__elapsed {
    // 已用时样式
  }

  &__estimated {
    // 预估剩余时间样式
  }

  // 底部区域
  :deep(.el-dialog__footer) {
    padding: 12px 20px;
    border-top: 1px solid var(--border-color-light);
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

// 深色模式适配
.dark .blink-task-dialog {
  :deep(.el-dialog__header) {
    border-bottom-color: var(--border-color-light);
  }

  :deep(.el-dialog__footer) {
    border-top-color: var(--border-color-light);
  }
}

// 无障碍访问：减少动画模式支持
@media (prefers-reduced-motion: reduce) {
  .blink-task-dialog {
    :deep(.el-dialog) {
      transition: none;
    }
  }
}
</style>