<!-- src/components/BlinkTaskDialog/components/ProgressBar.vue -->
<template>
  <div class="blink-progress-bar">
    <!-- 进度条轨道 -->
    <div class="blink-progress-bar__track">
      <div
        class="blink-progress-bar__fill"
        :style="{ width: `${clampedPercent}%` }"
      />
    </div>

    <!-- 进度文字 -->
    <div class="blink-progress-bar__text">
      <span class="blink-progress-bar__percent">{{ clampedPercent }}%</span>
      <span v-if="estimatedTime" class="blink-progress-bar__estimated">
        预计剩余 {{ formatTime(estimatedTime) }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 进度条子组件
 *
 * 用于展示任务的百分比进度，支持显示预估剩余时间。
 * 进度值会自动限制在 0-100 范围内。
 *
 * @author binblink
 * @since 2024-01-xx
 */
import { computed } from 'vue'

defineOptions({
  name: 'ProgressBar',
})

/**
 * 组件 Props 定义
 */
interface Props {
  /** 进度百分比 (0-100) */
  percent: number
  /** 预估剩余时间（毫秒），可选 */
  estimatedTime?: number
}

const props = withDefaults(defineProps<Props>(), {
  percent: 0,
  estimatedTime: undefined,
})

/**
 * 计算限制后的进度值（确保在 0-100 范围内）
 */
const clampedPercent = computed(() => {
  return Math.max(0, Math.min(100, Math.round(props.percent)))
})

/**
 * 格式化时间显示
 * 将毫秒转换为更友好的显示格式（秒/分钟）
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
</script>

<style scoped lang="scss">
.blink-progress-bar {
  display: flex;
  flex-direction: column;
  gap: 8px;

  // 进度条轨道
  &__track {
    height: 8px;
    background-color: var(--border-color-light);
    border-radius: 4px;
    overflow: hidden;
    position: relative;
  }

  // 进度条填充
  &__fill {
    height: 100%;
    background-color: var(--primary-color);
    border-radius: 4px;
    transition: width 0.3s ease-out;
    position: relative;

    // 可选：添加渐变效果增强视觉
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(
        90deg,
        transparent 0%,
        rgba(255, 255, 255, 0.2) 50%,
        transparent 100%
      );
      animation: shimmer 2s infinite;
    }
  }

  // 进度文字区域
  &__text {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
  }

  // 百分比文字
  &__percent {
    color: var(--text-color-regular);
    font-weight: 500;
  }

  // 预估时间文字
  &__estimated {
    color: var(--text-color-secondary);
    font-size: 13px;
  }
}

// shimmer 动画效果
@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

// 深色模式适配
.dark .blink-progress-bar {
  &__track {
    background-color: var(--border-color-light);
  }

  &__fill {
    background-color: var(--primary-color);
  }

  &__percent {
    color: var(--text-color-regular);
  }

  &__estimated {
    color: var(--text-color-secondary);
  }
}
</style>