<!-- src/components/BlinkTaskDialog/components/ResultPanel.vue -->
<template>
  <div class="blink-result-panel" :class="resultClass">
    <!-- 结果图标 -->
    <div class="blink-result-panel__icon">
      <el-icon :size="48">
        <component :is="resultIcon" />
      </el-icon>
    </div>

    <!-- 结果摘要 -->
    <div class="blink-result-panel__summary">
      {{ displaySummary }}
    </div>

    <!-- 错误信息 -->
    <div v-if="error" class="blink-result-panel__error">
      <el-icon :size="16">
        <WarningFilled />
      </el-icon>
      <span class="blink-result-panel__error-text">{{ error.message }}</span>
    </div>

    <!-- 结果数据详情（可选） -->
    <div v-if="result?.data && showDataDetail" class="blink-result-panel__data">
      {{ formatDataDetail }}
    </div>

    <!-- 操作按钮 -->
    <div v-if="result?.actions?.length" class="blink-result-panel__actions">
      <el-button
        v-for="action in result.actions"
        :key="action.label"
        :type="action.type === 'primary' ? 'primary' : 'default'"
        :link="action.type === 'link'"
        size="default"
        @click="handleAction(action)"
      >
        {{ action.label }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 结果面板子组件
 *
 * 用于展示任务执行结果，包括成功、失败、取消三种状态。
 * 不同状态使用不同的视觉样式和图标。
 * 支持显示结果摘要、错误信息、数据详情和操作按钮。
 *
 * @author binblink
 * @since 2026-04-25
 */
import { computed } from 'vue'
import { SuccessFilled, CircleCloseFilled, WarningFilled } from '@element-plus/icons-vue'
import type { TaskResult, ResultAction, TaskStatus } from '../types'

defineOptions({
  name: 'ResultPanel',
})

/**
 * 组件 Props 定义
 */
interface Props {
  /** 任务状态 */
  status: TaskStatus
  /** 任务结果 */
  result?: TaskResult | null
  /** 错误信息 */
  error?: Error | null
  /** 是否显示数据详情 */
  showDataDetail?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  result: null,
  error: null,
  showDataDetail: false,
})

/**
 * 组件 Emits 定义
 */
interface Emits {
  /** 点击操作按钮时触发 */
  (e: 'action', action: ResultAction): void
}

const emit = defineEmits<Emits>()

/**
 * 计算结果样式类
 * 根据任务状态返回对应的样式类名
 */
const resultClass = computed(() => {
  if (props.status === 'completed' && props.result?.success) {
    return 'blink-result-panel--success'
  }
  if (props.status === 'failed') {
    return 'blink-result-panel--error'
  }
  if (props.status === 'cancelled') {
    return 'blink-result-panel--warning'
  }
  return ''
})

/**
 * 计算结果图标组件
 * 根据任务状态返回对应的图标组件
 */
const resultIcon = computed(() => {
  if (props.status === 'completed' && props.result?.success) {
    return SuccessFilled
  }
  if (props.status === 'failed') {
    return CircleCloseFilled
  }
  // 取消或其他状态使用警告图标
  return WarningFilled
})

/**
 * 计算显示的摘要文本
 * 优先使用 result.summary，其次使用默认文本
 */
const displaySummary = computed(() => {
  if (props.result?.summary) {
    return props.result.summary
  }

  // 根据状态返回默认摘要
  if (props.status === 'completed' && props.result?.success) {
    return '任务执行成功'
  }
  if (props.status === 'failed') {
    return '任务执行失败'
  }
  if (props.status === 'cancelled') {
    return '任务已取消'
  }
  return '任务已完成'
})

/**
 * 计算数据详情格式化文本
 * 将 result.data 格式化为字符串显示
 */
const formatDataDetail = computed(() => {
  if (!props.result?.data) {
    return ''
  }

  const data = props.result.data
  if (typeof data === 'string') {
    return data
  }
  if (typeof data === 'number') {
    return `结果: ${data}`
  }
  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return '[数据无法显示]'
  }
})

/**
 * 处理操作按钮点击
 *
 * @param action 操作按钮配置
 */
const handleAction = (action: ResultAction) => {
  // 触发 action 事件，让父组件处理
  emit('action', action)
  // 如果 action 有 handler，也执行它
  if (action.handler) {
    action.handler()
  }
}
</script>

<style scoped lang="scss">
.blink-result-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px 16px;
  border-radius: 8px;
  text-align: center;
  transition: all 0.3s ease;

  // 结果图标
  &__icon {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  // 结果摘要
  &__summary {
    font-size: 16px;
    font-weight: 500;
    color: var(--text-color-primary);
    line-height: 1.5;
  }

  // 错误信息
  &__error {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background-color: rgba(var(--danger-color-rgb), 0.1);
    border-radius: 4px;
    max-width: 100%;
  }

  &__error-text {
    font-size: 14px;
    color: var(--danger-color);
    word-break: break-word;
  }

  // 数据详情
  &__data {
    font-size: 13px;
    color: var(--text-color-secondary);
    padding: 8px 12px;
    background-color: var(--fill-color-light);
    border-radius: 4px;
    max-width: 100%;
    word-break: break-word;
    white-space: pre-wrap;
  }

  // 操作按钮区域
  &__actions {
    display: flex;
    justify-content: center;
    gap: 12px;
    flex-wrap: wrap;
    margin-top: 8px;
  }

  // 成功状态样式
  &--success {
    background-color: rgba(var(--success-color-rgb), 0.08);

    .blink-result-panel__icon {
      color: var(--success-color);
    }

    .blink-result-panel__summary {
      color: var(--success-color);
    }
  }

  // 失败状态样式
  &--error {
    background-color: rgba(var(--danger-color-rgb), 0.08);

    .blink-result-panel__icon {
      color: var(--danger-color);
    }

    .blink-result-panel__summary {
      color: var(--danger-color);
    }
  }

  // 警告/取消状态样式
  &--warning {
    background-color: rgba(var(--warning-color-rgb), 0.08);

    .blink-result-panel__icon {
      color: var(--warning-color);
    }

    .blink-result-panel__summary {
      color: var(--warning-color);
    }
  }
}

// 无障碍访问：减少动画模式支持
@media (prefers-reduced-motion: reduce) {
  .blink-result-panel {
    transition: none;
  }
}
</style>