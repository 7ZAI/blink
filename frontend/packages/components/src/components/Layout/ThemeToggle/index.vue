<template>
  <div
    class="theme-toggle"
    :class="{ 'is-dark': isDark }"
    :title="title"
    @click="handleToggle"
  >
    <slot :is-dark="isDark" :toggle="handleToggle">
      <!-- 默认图标切换 -->
      <el-icon class="toggle-icon">
        <Moon v-if="!isDark" />
        <Sunny v-else />
      </el-icon>
      <span v-if="showLabel" class="toggle-label">
        {{ isDark ? labels.light : labels.dark }}
      </span>
    </slot>
  </div>
</template>

<script setup lang="ts">
/**
 * ThemeToggle 主题切换组件
 *
 * 特点：
 * - 支持自定义触发器
 * - 解耦 i18n，支持外部传入文本
 * - 支持显示/隐藏文本标签
 * - 完全可定制样式
 */

import { computed } from 'vue'
import { Moon, Sunny } from '@element-plus/icons-vue'

// ============================================
// 类型定义
// ============================================

export interface Props {
  /** 当前主题 */
  theme?: 'light' | 'dark'
  /** 是否显示文本标签 */
  showLabel?: boolean
  /** 文本标签映射 */
  labels?: {
    dark: string
    light: string
  }
  /** 自定义标题 */
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  theme: 'light',
  showLabel: false,
  labels: () => ({
    dark: '深色模式',
    light: '浅色模式',
  }),
  title: '',
})

const emit = defineEmits<{
  (e: 'toggle'): void
  (e: 'change', theme: 'light' | 'dark'): void
}>()

// ============================================
// 计算属性
// ============================================

const isDark = computed(() => props.theme === 'dark')

const title = computed(() => {
  if (props.title) return props.title
  return isDark.value ? props.labels.light : props.labels.dark
})

// ============================================
// 方法
// ============================================

const handleToggle = () => {
  emit('toggle')
  emit('change', isDark.value ? 'light' : 'dark')
}

// ============================================
// 暴露
// ============================================

defineExpose({
  isDark,
  handleToggle,
})
</script>

<style scoped lang="scss">
.theme-toggle {
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

  &.is-dark {
    .toggle-icon {
      color: #fbbf24;
    }
  }
}
</style>