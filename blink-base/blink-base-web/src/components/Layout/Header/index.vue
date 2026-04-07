<template>
  <el-header class="header h-[50px] flex items-center justify-between px-4 border-b transition-theme">
    <div class="header-left flex items-center flex-1 min-w-0 overflow-hidden">
      <slot name="left">
        <Breadcrumb />
      </slot>
    </div>
    <div class="header-right flex items-center gap-1 shrink-0">
      <!-- 自定义右侧内容（在全屏按钮之前） -->
      <slot name="right-before"></slot>

      <!-- 全屏按钮 -->
      <div
        v-if="showFullscreen"
        class="header-item"
        @click="toggleFullscreen"
        :title="isFullscreen ? t('header.exitFullscreen') : t('header.fullscreen')"
      >
        <el-icon v-if="isFullscreen"><Aim /></el-icon>
        <el-icon v-else><FullScreen /></el-icon>
      </div>

      <!-- 主题切换 -->
      <div
        v-if="showThemeToggle"
        class="header-item"
        :title="theme === 'light' ? t('header.darkMode') : t('header.lightMode')"
        @click="handleThemeToggle"
      >
        <el-icon v-if="theme === 'light'"><Moon /></el-icon>
        <el-icon v-else><Sunny /></el-icon>
      </div>

      <!-- 语言切换 -->
      <el-dropdown v-if="showLanguageSwitch" @command="handleLanguageChange">
        <div class="header-item">
          {{ currentLanguage === 'zh_cn' ? '中文' : 'EN' }}
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="zh_cn">中文</el-dropdown-item>
            <el-dropdown-item command="en_us">English</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <slot name="user-menu" :user-info="userInfo">
        <UserDropdown
          :user-info="userInfo"
          :show-theme-settings="showThemeSettings"
          @command="handleUserCommand"
        >
          <template v-if="slots['dropdown-menu']" #menu>
            <slot name="dropdown-menu"></slot>
          </template>
        </UserDropdown>
      </slot>

      <!-- 自定义右侧内容 -->
      <slot name="right"></slot>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, useSlots } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Moon,
  Sunny,
  FullScreen,
  Aim,
} from '@element-plus/icons-vue'
import Breadcrumb from '../../Breadcrumb/index.vue'
import UserDropdown, { type UserInfo } from '../UserDropdown/index.vue'

/**
 * 用户信息接口
 */
export type { UserInfo } from '../UserDropdown/index.vue'

interface Props {
  /** 用户信息 */
  userInfo?: UserInfo | null
  /** 当前主题 */
  theme?: 'light' | 'dark'
  /** 当前语言 */
  currentLanguage?: string
  /** 是否显示全屏按钮 */
  showFullscreen?: boolean
  /** 是否显示主题切换 */
  showThemeToggle?: boolean
  /** 是否显示语言切换 */
  showLanguageSwitch?: boolean
  /** 是否显示主题设置 */
  showThemeSettings?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  userInfo: null,
  theme: 'light',
  currentLanguage: 'zh_cn',
  showFullscreen: true,
  showThemeToggle: true,
  showLanguageSwitch: true,
  showThemeSettings: false,
})

const emit = defineEmits<{
  (e: 'theme-toggle'): void
  (e: 'language-change', lang: string): void
  (e: 'user-command', command: string): void
}>()

const slots = useSlots()
const { t } = useI18n()

const isFullscreen = ref(false)

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

const handleThemeToggle = () => {
  emit('theme-toggle')
}

const handleLanguageChange = (lang: string) => {
  emit('language-change', lang)
}

const handleUserCommand = (command: string) => {
  emit('user-command', command)
}

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  isFullscreen.value = !!document.fullscreenElement
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<style scoped lang="scss">
/* 头部样式 */
.header {
  @apply flex items-center justify-between px-4 border-b h-[50px];
  background: var(--header-bg);
  backdrop-filter: blur(var(--glass-blur));
  box-shadow: var(--header-shadow);
  border-color: var(--border-color-light);
  transition: all var(--duration-normal) var(--ease-out-expo);
}

.header-left {
  @apply flex items-center flex-1 min-w-0 overflow-hidden;
}

.header-right {
  @apply flex items-center gap-1 shrink-0;
}

/* 头部项 */
.header-item {
  @apply inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg cursor-pointer transition-all text-[13px] whitespace-nowrap shrink-0;
  color: var(--text-color-regular);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: var(--gradient-primary);
    opacity: 0;
    transition: opacity var(--duration-normal) var(--ease-out-expo);
  }

  &:hover {
    @apply text-primary;
    &::before {
      opacity: 0.1;
    }
    box-shadow: inset 0 0 20px rgba(59, 130, 246, 0.1);
  }

  .el-icon {
    @apply text-lg shrink-0;
  }
}

</style>
