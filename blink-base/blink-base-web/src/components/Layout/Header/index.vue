<template>
  <el-header class="header h-[50px] flex items-center justify-between px-4 border-b transition-theme">
    <div class="header-left flex items-center flex-1 min-w-0 overflow-hidden">
      <slot name="left">
        <Breadcrumb />
      </slot>
    </div>
    <div class="header-right flex items-center gap-1 shrink-0">
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

      <!-- 用户下拉菜单 -->
      <el-dropdown v-if="userInfo" class="user-dropdown" @command="handleUserCommand">
        <div class="header-item user-item">
          <el-avatar :src="userInfo.avatar" :size="32">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <span class="user-name">{{ userInfo.username || userInfo.loginName }}</span>
          <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <!-- 自定义下拉菜单内容 -->
            <slot name="dropdown-menu">
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>{{ t('header.profile') }}
              </el-dropdown-item>
              <el-dropdown-item command="themeSettings" v-if="showThemeSettings">
                <el-icon><Brush /></el-icon>{{ t('header.themeSettings') }}
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>{{ t('header.logout') }}
              </el-dropdown-item>
            </slot>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- 自定义右侧内容 -->
      <slot name="right"></slot>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  UserFilled,
  ArrowDown,
  User,
  SwitchButton,
  Moon,
  Sunny,
  FullScreen,
  Aim,
  Brush,
} from '@element-plus/icons-vue'
import Breadcrumb from '../../Breadcrumb/index.vue'
import BlinkIcon from '../../BlinkIcon/index.vue'

/**
 * 用户信息接口
 */
export interface UserInfo {
  username?: string
  loginName?: string
  avatar?: string
  avatarStyle?: string
}

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

/* 用户项 */
.user-item {
  @apply flex items-center gap-2.5 px-3.5;

  .el-avatar {
    @apply cursor-pointer border-2 border-transparent transition-all w-9 h-9;

    &:hover {
      @apply border-primary;
      box-shadow: var(--glow-primary);
    }
  }

  .user-name {
    @apply text-[15px] font-medium max-w-[120px] overflow-hidden text-ellipsis whitespace-nowrap;
    color: var(--text-color-primary);
  }

  .dropdown-arrow {
    @apply text-[13px] transition-transform;
    color: var(--text-color-secondary);
  }

  &:hover .dropdown-arrow {
    @apply rotate-180;
  }
}

/* 下拉菜单项 */
:deep(.el-dropdown-menu__item) {
  @apply flex items-center gap-2 px-4 py-2.5 text-sm;

  .el-icon {
    @apply text-base;
  }

  &:hover {
    @apply text-primary;
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.1) 0%, transparent 100%);
  }
}
</style>