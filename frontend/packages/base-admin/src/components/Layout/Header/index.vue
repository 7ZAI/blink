<template>
  <el-header
    class="header-container"
    :class="{ 'header-fixed': fixed }"
    :style="containerStyle"
  >
    <!-- 左侧区域 Slot -->
    <div class="header-left">
      <slot name="left">
        <Breadcrumb v-if="showBreadcrumb" />
      </slot>
    </div>

    <!-- 右侧区域 -->
    <div class="header-right">
      <!-- 右侧前置内容 Slot -->
      <slot name="right-before"></slot>

      <!-- 全屏按钮 -->
      <slot name="fullscreen" :is-fullscreen="isFullscreen" :toggle="toggleFullscreen">
        <FullscreenToggle
          v-if="showFullscreen"
          :show-label="showActionLabels"
          :labels="labels.fullscreen"
          @change="handleFullscreenChange"
        />
      </slot>

      <!-- 主题切换按钮 -->
      <slot name="theme-toggle" :theme="theme" :toggle="handleThemeToggle">
        <ThemeToggle
          v-if="showThemeToggle"
          :theme="theme"
          :show-label="showActionLabels"
          :labels="labels.theme"
          @change="handleThemeChange"
        />
      </slot>

      <!-- 语言切换 -->
      <slot name="language-switch" :language="currentLanguage" :change="handleLanguageChange">
        <LanguageSwitch
          v-if="showLanguageSwitch"
          :current-language="currentLanguage"
          :languages="languages"
          @change="handleLanguageChange"
        />
      </slot>

      <!-- 右侧中间内容 Slot -->
      <slot name="right-middle"></slot>

      <!-- 用户菜单 Slot -->
      <slot name="user-menu" :user-info="userInfo" :handle-command="handleUserCommand">
        <UserDropdown
          v-if="userInfo"
          :user-info="userInfo"
          :show-theme-settings="showThemeSettings"
          :labels="labels.user"
          :menu-items="userMenuItems"
          :avatar-resolver="avatarResolver"
          @command="handleUserCommand"
        >
          <template v-if="$slots['dropdown-menu']" #menu="slotProps">
            <slot name="dropdown-menu" v-bind="slotProps" />
          </template>
        </UserDropdown>
      </slot>

      <!-- 右侧后置内容 Slot -->
      <slot name="right"></slot>
    </div>
  </el-header>
</template>

<script setup lang="ts">
/**
 * Header 头部组件
 *
 * 特点：
 * - 完全可定制的插槽系统，可替换任何区域
 * - 解耦 i18n，通过 labels prop 传入文本
 * - 状态通过 composable 管理，可外部复用
 * - 样式可完全覆盖
 * - 每个功能区域都有独立组件，可单独使用
 */

import { computed, ref, onMounted, onUnmounted, useSlots } from 'vue'
import Breadcrumb from '../../Breadcrumb/index.vue'
import UserDropdown from '../UserDropdown/index.vue'
import type { UserInfo, MenuItem as UserMenuItem } from '../UserDropdown/index.vue'
import ThemeToggle from '../ThemeToggle/index.vue'
import LanguageSwitch, { type LanguageOption } from '../LanguageSwitch/index.vue'
import FullscreenToggle from '../FullscreenToggle/index.vue'

// Re-export types for external use
export type { UserInfo } from '../UserDropdown/index.vue'
export type { LanguageOption } from '../LanguageSwitch/index.vue'

// ============================================
// 类型定义
// ============================================

/**
 * 标签文本配置
 */
export interface HeaderLabels {
  fullscreen?: { enter: string; exit: string }
  theme?: { dark: string; light: string }
  user?: {
    profile?: string
    themeSettings?: string
    logout?: string
  }
}

export interface Props {
  /** 用户信息 */
  userInfo?: UserInfo | null
  /** 当前主题 */
  theme?: 'light' | 'dark'
  /** 当前语言 */
  currentLanguage?: string
  /** 语言列表 */
  languages?: LanguageOption[]
  /** 是否显示全屏按钮 */
  showFullscreen?: boolean
  /** 是否显示主题切换 */
  showThemeToggle?: boolean
  /** 是否显示语言切换 */
  showLanguageSwitch?: boolean
  /** 是否显示主题设置 */
  showThemeSettings?: boolean
  /** 是否显示面包屑 */
  showBreadcrumb?: boolean
  /** 是否显示操作按钮文本标签 */
  showActionLabels?: boolean
  /** 是否固定头部 */
  fixed?: boolean
  /** 头部高度 */
  height?: number
  /** 文本标签配置 */
  labels?: HeaderLabels
  /** 头像解析函数 */
  avatarResolver?: (user: UserInfo) => string

  // === 样式配置 ===
  /** 自定义容器样式 */
  customStyle?: Record<string, string>
  /** 自定义类名 */
  customClass?: string
}

// ============================================
// Props 定义
// ============================================

const props = withDefaults(defineProps<Props>(), {
  userInfo: null,
  theme: 'light',
  currentLanguage: 'zh_cn',
  languages: () => [
    { code: 'zh_cn', label: '中文', nativeLabel: '简体中文' },
    { code: 'en_us', label: 'EN', nativeLabel: 'English' },
  ],
  showFullscreen: true,
  showThemeToggle: true,
  showLanguageSwitch: true,
  showThemeSettings: false,
  showBreadcrumb: true,
  showActionLabels: false,
  fixed: false,
  height: 50,
  labels: () => ({
    fullscreen: { enter: '全屏', exit: '退出全屏' },
    theme: { dark: '深色模式', light: '浅色模式' },
    user: {
      profile: '个人中心',
      themeSettings: '主题设置',
      logout: '退出登录',
    },
  }),
  avatarResolver: undefined,
  customStyle: () => ({}),
  customClass: '',
})

// ============================================
// Emits 定义
// ============================================

export interface Emits {
  (e: 'theme-toggle'): void
  (e: 'theme-change', theme: 'light' | 'dark'): void
  (e: 'language-change', lang: string): void
  (e: 'user-command', command: string): void
  (e: 'fullscreen-change', isFullscreen: boolean): void
}

const emit = defineEmits<Emits>()
const slots = useSlots()

// ============================================
// 状态
// ============================================

const isFullscreen = ref(false)

// ============================================
// 计算属性
// ============================================

const containerStyle = computed(() => ({
  height: `${props.height}px`,
  ...props.customStyle,
}))

const userMenuItems = computed<UserMenuItem[]>(() => {
  const items: UserMenuItem[] = []

  // 个人中心
  items.push({
    command: 'profile',
    label: props.labels.user?.profile || '个人中心',
  })

  // 主题设置
  if (props.showThemeSettings) {
    items.push({
      command: 'themeSettings',
      label: props.labels.user?.themeSettings || '主题设置',
    })
  }

  // 退出登录
  items.push({
    command: 'logout',
    label: props.labels.user?.logout || '退出登录',
    divided: true,
  })

  return items
})

// ============================================
// 方法
// ============================================

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

const handleFullscreenChange = (fullscreen: boolean) => {
  isFullscreen.value = fullscreen
  emit('fullscreen-change', fullscreen)
}

const handleThemeToggle = () => {
  emit('theme-toggle')
}

const handleThemeChange = (newTheme: 'light' | 'dark') => {
  emit('theme-change', newTheme)
}

const handleLanguageChange = (lang: string) => {
  emit('language-change', lang)
}

const handleUserCommand = (command: string) => {
  emit('user-command', command)
}

// ============================================
// 生命周期
// ============================================

onMounted(() => {
  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement
  })
  isFullscreen.value = !!document.fullscreenElement
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement
  })
})

// ============================================
// 暴露
// ============================================

defineExpose({
  toggleFullscreen,
  handleThemeToggle,
  handleLanguageChange,
  handleUserCommand,
  isFullscreen,
})
</script>

<style scoped lang="scss">
/* === 头部容器 === */
.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-color-light, #e5e7eb);
  background: var(--header-bg, #ffffff);
  transition: all var(--duration-normal, 0.3s) var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1));

  &.header-fixed {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;
  }
}

/* === 左侧区域 === */
.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* === 右侧区域 === */
.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
</style>