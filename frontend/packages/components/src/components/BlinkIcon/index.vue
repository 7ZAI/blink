<template>
  <component
    :is="wrapperTag"
    class="blink-icon"
    :class="[customClass, { 'blink-icon--menu': menuMode }]"
    :style="iconStyle"
  >
    <component v-if="resolvedElementIcon" :is="resolvedElementIcon" />
    <Icon v-else-if="icon" :icon="icon" />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Icon } from '@iconify/vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

/**
 * BlinkIcon 组件
 *
 * 统一的图标组件，支持 Element Plus 图标和 Iconify 图标
 *
 * @example
 * <!-- Element Plus 图标 -->
 * <BlinkIcon icon="Setting" :size="18" />
 *
 * <!-- Iconify 图标 -->
 * <BlinkIcon icon="mdi:cog" :size="18" />
 *
 * <!-- 菜单模式（自动添加间距） -->
 * <BlinkIcon icon="Setting" :size="18" menu-mode />
 */
export interface Props {
  /** 图标名称，Element Plus 图标直接使用名称，Iconify 图标使用 prefix:name 格式 */
  icon?: string
  /** 图标大小，数字为 px，字符串可以是任意 CSS 单位 */
  size?: number | string
  /** 图标颜色 */
  color?: string
  /** 自定义类名 */
  class?: string
  /**
   * 菜单模式
   * - true: 自动添加右边距，适用于菜单项图标
   * - false: 不添加间距
   */
  menuMode?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  icon: '',
  size: undefined,
  color: undefined,
  class: '',
  menuMode: false,
})

/**
 * 解析 Element Plus 图标
 * - 不包含 ':' 的图标名视为 Element Plus 图标
 * - 包含 ':' 的视为 Iconify 图标
 */
const resolvedElementIcon = computed(() => {
  if (!props.icon || props.icon.includes(':')) {
    return null
  }
  return ElementPlusIconsVue[props.icon as keyof typeof ElementPlusIconsVue] || null
})

/**
 * 包装标签
 * - Element Plus 图标使用 el-icon 包裹，享受 Element Plus 内置样式
 * - Iconify 图标使用 span 包裹
 */
const wrapperTag = computed(() => (resolvedElementIcon.value ? 'el-icon' : 'span'))

const customClass = computed(() => props.class)

const iconStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.size) {
    style.fontSize = typeof props.size === 'number' ? `${props.size}px` : props.size
  }
  if (props.color) {
    style.color = props.color
  }
  return style
})
</script>

<style scoped lang="scss">
.blink-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  flex-shrink: 0;

  /* 菜单模式：自动添加右边距 */
  &.blink-icon--menu {
    margin-right: 8px;
  }
}
</style>