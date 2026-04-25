<!-- src/components/BlinkTaskDialog/components/Spinner.vue -->
<template>
  <div
    class="blink-spinner"
    role="status"
    aria-label="加载中"
  >
    <div
      class="blink-spinner__circle"
      :style="sizeStyle"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 加载动画子组件
 *
 * 用于展示旋转加载状态，支持配置尺寸。
 * 使用项目 CSS 变量实现主题适配，支持深色模式。
 *
 * @author binblink
 * @since 2026-04-25
 */
import { computed } from 'vue'

defineOptions({
  name: 'Spinner',
})

/**
 * 组件 Props 定义
 */
interface Props {
  /** 尺寸（像素），默认 48 */
  size?: number
}

const props = withDefaults(defineProps<Props>(), {
  size: 48,
})

/**
 * 计算尺寸样式
 * 动态设置圆形的宽度和高度
 */
const sizeStyle = computed(() => {
  const clampedSize = Math.max(16, Math.min(128, props.size))
  return {
    width: `${clampedSize}px`,
    height: `${clampedSize}px`,
  }
})
</script>

<style scoped lang="scss">
.blink-spinner {
  display: flex;
  justify-content: center;
  align-items: center;

  // 旋转圆形
  &__circle {
    border-radius: 50%;
    // 边框颜色：基础部分使用浅色边框，旋转部分使用主色调
    border: 3px solid var(--border-color-light);
    border-top-color: var(--primary-color);
    animation: spin 1s linear infinite;
    box-sizing: border-box;
  }
}

// 旋转动画
@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

// 无障碍访问：减少动画模式支持
@media (prefers-reduced-motion: reduce) {
  .blink-spinner__circle {
    animation: none;
    // 在减少动画模式下，使用静态指示器
    border-top-color: var(--primary-color);
    border-right-color: var(--primary-color-light-5);
  }
}
</style>