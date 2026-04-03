<template>
  <!-- 主布局容器 -->
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside
      :width="sidebarWidth + 'px'"
      class="sidebar transition-sidebar relative overflow-hidden select-none"
      :class="{ 'sidebar-collapsed': isCollapsed }"
    >
      <!-- Logo 区域 - 展开状态 -->
      <div class="logo h-[50px] flex items-center px-4 gap-2.5 border-b" v-show="!isCollapsed">
        <div class="w-3 h-7 shrink-0" v-html="systemConfigStore.systemLogo"></div>
        <span class="text-base font-semibold text-sidebar-active tracking-wider">{{ systemConfigStore.systemTitle }}</span>
      </div>
      <!-- Logo 区域 - 折叠状态 -->
      <div class="logo-mini h-[50px] flex items-center justify-center border-b" v-show="isCollapsed">
        <div class="w-2.5 h-6" v-html="systemConfigStore.systemLogo"></div>
      </div>
      <!-- 侧边栏菜单 -->
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        router
      >
        <template v-for="menu in menuList" :key="menu.menuId">
          <SidebarMenu :menu="menu" />
        </template>
      </el-menu>

      <!-- 拖拽调整宽度的手柄 -->
      <div
        class="resize-handle"
        :class="{ 'resizing': isResizing }"
        @mousedown="startResize"
      >
        <div class="resize-indicator"></div>
      </div>
    </el-aside>

    <!-- 折叠按钮 -->
    <div
      class="collapse-btn"
      :class="{ 'collapsed': isCollapsed }"
      @click="toggleSidebar"
    >
      <el-icon><ArrowLeft v-if="!isCollapsed" /><ArrowRight v-else /></el-icon>
    </div>

    <el-container>
      <!-- 头部 -->
      <el-header class="header h-[50px] flex items-center justify-between px-4 border-b transition-theme">
        <div class="header-left flex items-center flex-1 min-w-0 overflow-hidden">
          <Breadcrumb />
        </div>
        <div class="header-right flex items-center gap-1 shrink-0">
          <!-- 全屏按钮 -->
          <div class="header-item" @click="toggleFullscreen" :title="isFullscreen ? t('header.exitFullscreen') : t('header.fullscreen')">
            <el-icon v-if="isFullscreen"><Aim /></el-icon>
            <el-icon v-else><FullScreen /></el-icon>
          </div>
          <!-- 用户下拉菜单 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="header-item user-item">
              <el-avatar
                :src="getAvatarUrl(userStore.userInfo?.avatar, userStore.userInfo?.avatarStyle, userStore.userInfo?.loginName)"
              >
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.username || userStore.userInfo?.loginName }}</span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>{{ t('header.profile') }}
                </el-dropdown-item>
                <el-dropdown-item command="themeSettings">
                  <el-icon><Brush /></el-icon>{{ t('header.themeSettings') }}
                </el-dropdown-item>
                <el-dropdown-item divided command="toggleTheme">
                  <el-icon v-if="themeStore.theme === 'dark'"><Sunny /></el-icon>
                  <el-icon v-else><Moon /></el-icon>
                  {{ themeStore.theme === 'dark' ? t('header.lightMode') : t('header.darkMode') }}
                </el-dropdown-item>
                <el-dropdown-item command="toggleLocale">
                  <BlinkIcon icon="mdi:translate" size="14" />
                  {{ currentLocale === 'zh_cn' ? t('header.enUS') : t('header.zhCN') }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>{{ t('header.logout') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主题设置抽屉 -->
      <el-drawer
        v-model="themeDrawerVisible"
        :title="t('header.themeSettings')"
        direction="rtl"
        size="450px"
        :append-to-body="true"
      >
        <ThemeEditor />
      </el-drawer>

      <!-- 标签页视图 -->
      <TabsView />

      <!-- 主内容区域 -->
      <el-main class="main-content transition-theme">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, ArrowLeft, ArrowRight, ArrowDown, Setting, User, SwitchButton, Moon, Sunny, FullScreen, Aim, Brush } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { useThemeStore } from '@/stores/theme'
import { useSystemConfigStore } from '@/stores/systemConfig'
import { setLocale, getCurrentLocale } from '@/locales'
import { getAvatarUrl } from '@/utils/avatar'
import TabsView from '@/components/TabsView/index.vue'
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import SidebarMenu from './components/SidebarMenu.vue'
import ThemeEditor from '@/views/settings/components/ThemeEditor.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tabsStore = useTabsStore()
const themeStore = useThemeStore()
const systemConfigStore = useSystemConfigStore()

const activeMenu = computed(() => route.path)
const cachedViews = computed(() => tabsStore.getCachedViews)
const menuList = computed(() => userStore.menuTree || [])

const currentLocale = ref(getCurrentLocale())

const isFullscreen = ref(false)
const themeDrawerVisible = ref(false)

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

const toggleTheme = () => {
  themeStore.toggleTheme()
}

const toggleLocale = () => {
  const newLocale = currentLocale.value === 'zh_cn' ? 'en_us' : 'zh_cn'
  setLocale(newLocale)
  currentLocale.value = newLocale
}

const MIN_WIDTH = 60
const MAX_WIDTH = 400
const DEFAULT_WIDTH = 220
const COLLAPSE_THRESHOLD = 100

const sidebarWidth = ref(DEFAULT_WIDTH)
const isCollapsed = ref(false)
const isResizing = ref(false)

const startResize = (e: MouseEvent) => {
  isResizing.value = true
  const startX = e.clientX
  const startWidth = sidebarWidth.value

  const handleMouseMove = (e: MouseEvent) => {
    const delta = e.clientX - startX
    let newWidth = startWidth + delta

    if (newWidth < MIN_WIDTH) {
      newWidth = MIN_WIDTH
    } else if (newWidth > MAX_WIDTH) {
      newWidth = MAX_WIDTH
    }

    sidebarWidth.value = newWidth

    if (newWidth <= COLLAPSE_THRESHOLD) {
      isCollapsed.value = true
    } else {
      isCollapsed.value = false
    }
  }

  const handleMouseUp = () => {
    isResizing.value = false
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)

    localStorage.setItem('sidebarWidth', String(sidebarWidth.value))
    localStorage.setItem('sidebarCollapsed', String(isCollapsed.value))
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  if (isCollapsed.value) {
    sidebarWidth.value = MIN_WIDTH
  } else {
    sidebarWidth.value = DEFAULT_WIDTH
  }
  
  localStorage.setItem('sidebarWidth', String(sidebarWidth.value))
  localStorage.setItem('sidebarCollapsed', String(isCollapsed.value))
}

onMounted(() => {
  const savedWidth = localStorage.getItem('sidebarWidth')
  const savedCollapsed = localStorage.getItem('sidebarCollapsed')
  
  if (savedCollapsed !== null) {
    isCollapsed.value = savedCollapsed === 'true'
  }
  
  if (savedWidth !== null) {
    const width = parseInt(savedWidth)
    if (width >= MIN_WIDTH && width <= MAX_WIDTH) {
      sidebarWidth.value = width
    }
  }

  document.addEventListener('fullscreenchange', handleFullscreenChange)
  isFullscreen.value = !!document.fullscreenElement
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})

const handleCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'themeSettings':
      themeDrawerVisible.value = true
      break
    case 'toggleTheme':
      toggleTheme()
      break
    case 'toggleLocale':
      toggleLocale()
      break
    case 'logout':
      try {
        await ElMessageBox.confirm(t('header.logoutConfirm'), t('message.tips'), {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'warning',
        })
        await userStore.logout()
        tabsStore.closeAllTabs()
        ElMessage.success(t('header.logoutSuccess'))
        router.push('/login')
      } catch {
      }
      break
  }
}
</script>

<style scoped lang="scss">
/* 布局容器 */
.layout-container {
  @apply h-screen;
  background: var(--bg-color-page);
}

/* 侧边栏样式 */
.sidebar {
  position: relative;
  overflow: hidden;
  user-select: none;
  background: linear-gradient(180deg, var(--sidebar-bg) 0%, rgba(15, 23, 42, 0.98) 100%);
  transition: width var(--duration-normal) var(--ease-out-expo), background var(--duration-normal) var(--ease-out-expo);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);

  /* Logo 区域 */
  .logo {
    @apply h-[50px] flex items-center px-4 gap-2.5;
    border-bottom: 1px solid var(--sidebar-border);
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.08) 0%, transparent 100%);
    @apply overflow-hidden whitespace-nowrap;

    :deep(svg) {
      @apply w-auto h-7 shrink-0;
      fill: var(--primary-color);
      filter: drop-shadow(0 0 12px var(--primary-color));
      animation: float 3s ease-in-out infinite;
    }
  }

  /* Logo 文字 */
  .logo-text {
    @apply text-base font-semibold tracking-wider;
    color: var(--sidebar-text-active);
    text-shadow: 0 0 20px rgba(59, 130, 246, 0.5);
  }

  /* 折叠状态的 Logo */
  .logo-mini {
    @apply h-[50px] flex items-center justify-center;
    border-bottom: 1px solid var(--sidebar-border);
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.08) 0%, transparent 100%);

    :deep(svg) {
      @apply w-auto h-6;
      fill: var(--primary-color);
      filter: drop-shadow(0 0 12px var(--primary-color));
    }
  }

  /* 拖拽调整宽度的手柄 */
  .resize-handle {
    @apply absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize z-[100];

    .resize-indicator {
      @apply absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-0.5 h-10 rounded-sm opacity-0 transition-opacity;
      background: var(--gradient-cyber);
      box-shadow: var(--glow-primary);
    }

    &:hover {
      .resize-indicator {
        @apply opacity-100;
      }
    }

    &.resizing {
      background: linear-gradient(180deg, var(--primary-color) 0%, var(--neon-purple) 100%);
      box-shadow: var(--glow-primary);

      .resize-indicator {
        @apply opacity-100;
      }
    }
  }

  &.sidebar-collapsed {
    .resize-handle {
      @apply hidden;
    }
  }
}

/* 折叠按钮 */
.collapse-btn {
  @apply fixed top-1/2 -translate-y-1/2 w-5 h-[60px] rounded-r-lg flex items-center justify-center cursor-pointer z-[99] opacity-0 transition-all;
  border: 1px solid var(--sidebar-border);
  border-left: none;
  background: linear-gradient(180deg, var(--sidebar-bg) 0%, rgba(15, 23, 42, 0.95) 100%);
  left: v-bind('sidebarWidth + "px"');

  &:hover {
    @apply opacity-100;
    background: linear-gradient(180deg, var(--sidebar-bg-hover) 0%, var(--sidebar-bg) 100%);
    box-shadow: var(--glow-primary);
  }

  .el-icon {
    @apply text-xs;
    color: var(--sidebar-text);
    transition: all var(--duration-normal) var(--ease-out-expo);
  }

  &:hover .el-icon {
    color: var(--primary-color);
    filter: drop-shadow(0 0 6px var(--primary-color));
  }

  &.collapsed {
    @apply left-[60px];
  }
}

.sidebar:hover + .collapse-btn {
  @apply opacity-100;
}

/* 侧边栏菜单样式 */
.sidebar-menu {
  @apply border-r-0 h-[calc(100%-50px)] overflow-y-auto overflow-x-hidden;
  background: transparent !important;

  &:not(.el-menu--collapse) {
    @apply w-full;
  }

  :deep(.el-menu-item) {
    @apply h-11 leading-[44px] mx-2 my-1 rounded-lg transition-all;
    color: var(--sidebar-text);
    position: relative;

    &:hover {
      @apply translate-x-1;
      background: var(--sidebar-bg-hover) !important;
      color: var(--sidebar-text-active);
      box-shadow: inset 0 0 20px rgba(59, 130, 246, 0.1);
    }

    &.is-active {
      @apply font-medium;
      background: var(--sidebar-active-bg) !important;
      color: var(--sidebar-text-active) !important;
      box-shadow: inset 0 0 30px rgba(59, 130, 246, 0.15);

      &::before {
        content: '';
        @apply absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-5 rounded-r-sm;
        background: var(--gradient-cyber);
        box-shadow: 0 0 10px var(--primary-color);
      }
    }
  }

  :deep(.el-sub-menu__title) {
    @apply h-11 leading-[44px] mx-2 my-1 rounded-lg transition-all;
    color: var(--sidebar-text);

    &:hover {
      @apply translate-x-1;
      background: var(--sidebar-bg-hover) !important;
      color: var(--sidebar-text-active);
    }
  }

  :deep(.el-sub-menu) {
    .el-menu {
      background: transparent !important;
    }

    .el-menu-item {
      background: transparent !important;
      padding-left: 48px !important;

      &:hover {
        background: var(--sidebar-bg-hover) !important;
      }
    }

    // 三级菜单缩进
    .el-sub-menu {
      .el-menu-item {
        padding-left: 68px !important;
      }

      // 四级菜单缩进
      .el-sub-menu {
        .el-menu-item {
          padding-left: 88px !important;
        }
      }
    }

    &.is-active {
      > .el-sub-menu__title {
        @apply font-medium;
        color: var(--sidebar-text-active);
      }
    }
  }
}

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

/* 主题设置抽屉样式 */
:deep(.el-drawer) {
  border-radius: 12px 0 0 12px;

  .el-drawer__header {
    margin-bottom: 0;
    padding: 16px 20px;
    padding-right: 50px;
    border-bottom: 1px solid var(--border-color-light);
  }

  .el-drawer__close-btn {
    position: absolute;
    top: 16px;
    right: 16px;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--bg-color-page);
    border: 1px solid var(--border-color-light);
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    z-index: 10;

    &:hover {
      background: var(--primary-color);
      border-color: var(--primary-color);
      color: #fff;
      transform: scale(1.1);
    }

    .el-icon {
      font-size: 14px;
    }
  }
}

/* 主内容区域 */
.main-content {
  @apply p-4;
  background: var(--bg-color-page);
  transition: background-color var(--duration-normal) var(--ease-out-expo);
  position: relative;

  /* 添加微妙的网格背景 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
      radial-gradient(circle at 1px 1px, rgba(59, 130, 246, 0.03) 1px, transparent 0);
    background-size: 40px 40px;
    pointer-events: none;
    z-index: 0;
  }

  > * {
    position: relative;
    z-index: 1;
  }
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  @apply transition-opacity duration-300;
}

.fade-enter-from,
.fade-leave-to {
  @apply opacity-0;
}

/* 拖拽时的全局样式 */
:global(body.resizing) {
  @apply cursor-col-resize select-none;
}
</style>
