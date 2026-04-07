<template>
  <div class="fullscreen-toggle" :title="title" @click="handleToggle">
    <slot :is-fullscreen="isFullscreen" :toggle="handleToggle">
      <el-icon class="toggle-icon">
        <Aim v-if="isFullscreen" />
        <FullScreen v-else />
      </el-icon>
      <span v-if="showLabel" class="toggle-label">
        {{ isFullscreen ? labels.exit : labels.enter }}
      </span>
    </slot>
  </div>
</template>

<script setup lang="ts">
/**
 * FullscreenToggle 全屏切换组件
 *
 * 特点：
 * - 自动管理全屏状态
 * - 支持自定义触发器
 * - 解耦，可独立使用
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { FullScreen, Aim } from '@element-plus/icons-vue'

// ============================================
// 类型定义
// ============================================

export interface Props {
  /** 是否显示文本标签 */
  showLabel?: boolean
  /** 文本标签映射 */
  labels?: {
    enter: string
    exit: string
  }
  /** 自定义标题 */
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  showLabel: false,
  labels: () => ({
    enter: '全屏',
    exit: '退出全屏',
  }),
  title: '',
})

const emit = defineEmits<{
  (e: 'toggle'): void
  (e: 'change', isFullscreen: boolean): void
}>()

// ============================================
// 状态
// ============================================

const isFullscreen = ref(false)

// ============================================
// 计算属性
// ============================================

const title = computed(() => {
  if (props.title) return props.title
  return isFullscreen.value ? props.labels.exit : props.labels.enter
})

// ============================================
// 方法
// ============================================

const handleToggle = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
  emit('toggle')
  emit('change', isFullscreen.value)
}

const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

// ============================================
// 生命周期
// ============================================

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  isFullscreen.value = !!document.fullscreenElement
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})

// ============================================
// 暴露
// ============================================

defineExpose({
  isFullscreen,
  handleToggle,
})
</script>

<style scoped lang="scss">
.fullscreen-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-color-regular, #606266);

  &:hover {
    color: var(--primary-color, #3b82f6);
    background: rgba(59, 130, 246, 0.1);
  }

  .toggle-icon {
    font-size: 18px;
  }

  .toggle-label {
    font-size: 13px;
    white-space: nowrap;
  }
}
</style>