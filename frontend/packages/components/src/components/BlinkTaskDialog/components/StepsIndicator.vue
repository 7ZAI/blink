<!-- src/components/BlinkTaskDialog/components/StepsIndicator.vue -->
<template>
  <div
    class="blink-steps-indicator"
    role="list"
    aria-label="任务步骤"
  >
    <div
      v-for="(step, index) in steps"
      :key="index"
      class="blink-steps-indicator__step"
      :class="`blink-steps-indicator__step--${step.status}`"
      role="listitem"
      :aria-current="step.status === StepStatus.RUNNING ? 'step' : undefined"
    >
      <!-- 步骤图标 -->
      <div
        class="blink-steps-indicator__icon"
        :class="`blink-steps-indicator__icon--${step.status}`"
      >
        <!-- 已完成：对勾 -->
        <span
          v-if="step.status === StepStatus.COMPLETED"
          class="blink-steps-indicator__icon-check"
          aria-hidden="true"
        >
          &#10003;
        </span>

        <!-- 已失败：叉号 -->
        <span
          v-else-if="step.status === StepStatus.FAILED"
          class="blink-steps-indicator__icon-cross"
          aria-hidden="true"
        >
          &#10005;
        </span>

        <!-- 执行中：加载动画 -->
        <Spinner
          v-else-if="step.status === StepStatus.RUNNING"
          :size="16"
        />

        <!-- 待执行：步骤序号 -->
        <span
          v-else
          class="blink-steps-indicator__icon-number"
          aria-hidden="true"
        >
          {{ index + 1 }}
        </span>
      </div>

      <!-- 步骤内容区域 -->
      <div class="blink-steps-indicator__content">
        <!-- 步骤名称 -->
        <div class="blink-steps-indicator__name">
          {{ step.name }}
          <span
            v-if="step.status === StepStatus.RUNNING"
            class="blink-steps-indicator__status-label"
          >
            执行中
          </span>
        </div>

        <!-- 步骤消息 -->
        <div
          v-if="step.message"
          class="blink-steps-indicator__message"
        >
          {{ step.message }}
        </div>
      </div>

      <!-- 连接线 -->
      <div
        v-if="index < steps.length - 1"
        class="blink-steps-indicator__line"
        :class="getLineStyle(step.status, steps[index + 1]?.status)"
        aria-hidden="true"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 步骤指示器子组件
 *
 * 以垂直列表形式展示任务步骤，支持四种状态：
 * - pending: 待执行，显示步骤序号
 * - running: 执行中，显示加载动画
 * - completed: 已完成，显示对勾
 * - failed: 已失败，显示叉号
 *
 * 支持步骤间连接线的状态显示，符合无障碍访问规范。
 *
 * @author binblink
 * @since 2026-04-25
 */
import { computed } from 'vue'
import Spinner from './Spinner.vue'
import { StepStatus } from '../types'
import type { StepInfo } from '../types'

defineOptions({
  name: 'StepsIndicator',
})

/**
 * 组件 Props 定义
 */
interface Props {
  /** 步骤信息列表 */
  steps: StepInfo[]
}

const props = defineProps<Props>()

/**
 * 获取连接线样式
 * 根据当前步骤和下一步骤状态确定连接线显示样式
 *
 * @param currentStatus 当前步骤状态
 * @param nextStatus 下一步骤状态
 * @returns 连接线样式类名
 */
const getLineStyle = (currentStatus: StepStatus, nextStatus?: StepStatus): string => {
  // 当前步骤已完成，连接线显示为完成色
  if (currentStatus === StepStatus.COMPLETED) {
    return 'blink-steps-indicator__line--completed'
  }

  // 当前步骤失败，连接线显示为失败色
  if (currentStatus === StepStatus.FAILED) {
    return 'blink-steps-indicator__line--failed'
  }

  // 默认显示为待执行色
  return 'blink-steps-indicator__line--pending'
}
</script>

<style scoped lang="scss">
.blink-steps-indicator {
  display: flex;
  flex-direction: column;

  // 步骤项
  &__step {
    display: flex;
    align-items: flex-start;
    position: relative;
    padding-bottom: 16px;

    &:last-child {
      padding-bottom: 0;
    }

    // 状态样式
    &--running {
      .blink-steps-indicator__name {
        font-weight: 600;
        color: var(--primary-color);
      }
    }

    &--completed {
      .blink-steps-indicator__name {
        color: var(--text-color-regular);
      }
    }

    &--failed {
      .blink-steps-indicator__name {
        color: var(--danger-color);
      }
    }

    &--pending {
      .blink-steps-indicator__name {
        color: var(--text-color-secondary);
      }
    }
  }

  // 步骤图标
  &__icon {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
    font-size: 12px;
    font-weight: 600;
    transition: all 0.3s ease;

    // 待执行状态
    &--pending {
      border: 2px solid var(--border-color-light);
      background-color: transparent;
      color: var(--text-color-secondary);
    }

    // 执行中状态
    &--running {
      border: 2px solid var(--primary-color);
      background-color: var(--primary-color-light-9);
    }

    // 已完成状态
    &--completed {
      border: 2px solid var(--success-color);
      background-color: var(--success-color);
      color: #fff;
    }

    // 已失败状态
    &--failed {
      border: 2px solid var(--danger-color);
      background-color: var(--danger-color);
      color: #fff;
    }
  }

  // 图标：对勾
  &__icon-check {
    font-size: 14px;
    line-height: 1;
  }

  // 图标：叉号
  &__icon-cross {
    font-size: 14px;
    line-height: 1;
  }

  // 图标：序号
  &__icon-number {
    line-height: 1;
  }

  // 步骤内容区域
  &__content {
    flex: 1;
    min-width: 0;
    padding-top: 2px;
  }

  // 步骤名称
  &__name {
    font-size: 14px;
    line-height: 1.4;
    transition: color 0.3s ease;
  }

  // 状态标签（执行中时显示）
  &__status-label {
    display: inline-block;
    margin-left: 8px;
    padding: 1px 6px;
    font-size: 12px;
    font-weight: 500;
    color: var(--primary-color);
    background-color: var(--primary-color-light-9);
    border-radius: 4px;
    vertical-align: middle;
  }

  // 步骤消息
  &__message {
    margin-top: 4px;
    font-size: 13px;
    color: var(--text-color-secondary);
    line-height: 1.4;
    word-break: break-word;
  }

  // 连接线
  &__line {
    position: absolute;
    left: 11px;
    top: 24px;
    width: 2px;
    height: calc(100% - 24px);
    transition: background-color 0.3s ease;

    // 待执行状态
    &--pending {
      background-color: var(--border-color-light);
    }

    // 已完成状态
    &--completed {
      background-color: var(--success-color);
    }

    // 已失败状态
    &--failed {
      background-color: var(--danger-color);
    }
  }
}

// 无障碍访问：减少动画模式支持
@media (prefers-reduced-motion: reduce) {
  .blink-steps-indicator__icon,
  .blink-steps-indicator__line {
    transition: none;
  }
}
</style>