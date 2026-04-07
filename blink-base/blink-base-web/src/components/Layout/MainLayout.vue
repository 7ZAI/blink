<template>
  <el-container class="layout-container">
    <slot
      v-if="showSidebar"
      name="sidebar"
      :logo="logo"
      :title="title"
      :menu-list="menuList"
      :resizable="resizable"
      :default-width="defaultSidebarWidth"
      :min-width="minSidebarWidth"
      :max-width="maxSidebarWidth"
      :sidebar-ref="sidebarRef"
      :toggle-sidebar="toggleSidebar"
    >
      <!-- 侧边栏 -->
      <Sidebar
        ref="sidebarRef"
        :logo="logo"
        :title="title"
        :menu-list="menuList"
        :resizable="resizable"
        :default-width="defaultSidebarWidth"
        :min-width="minSidebarWidth"
        :max-width="maxSidebarWidth"
      >
        <template v-if="slots['sidebar-logo']" #logo="slotProps">
          <slot name="sidebar-logo" v-bind="slotProps" />
        </template>
        <template v-if="slots['sidebar-menu']" #menu="slotProps">
          <slot name="sidebar-menu" v-bind="slotProps" />
        </template>
        <template v-if="slots['sidebar-collapse-trigger']" #collapse-trigger="slotProps">
          <slot name="sidebar-collapse-trigger" v-bind="slotProps" />
        </template>
      </Sidebar>
    </slot>

    <el-container direction="vertical">
      <slot
        v-if="showHeader"
        name="header"
        :user-info="userInfo"
        :theme="currentTheme"
        :current-language="currentLanguage"
        :show-fullscreen="showFullscreen"
        :show-theme-toggle="showThemeToggle"
        :show-language-switch="showLanguageSwitch"
        :show-theme-settings="showThemeSettings"
        :toggle-sidebar="toggleSidebar"
      >
        <!-- 头部 -->
        <Header
          :user-info="userInfo"
          :theme="currentTheme"
          :current-language="currentLanguage"
          :show-fullscreen="showFullscreen"
          :show-theme-toggle="showThemeToggle"
          :show-language-switch="showLanguageSwitch"
          :show-theme-settings="showThemeSettings"
          @theme-toggle="handleThemeToggle"
          @language-change="handleLanguageChange"
          @user-command="handleUserCommand"
        >
          <template #left>
            <slot name="header-left">
              <Breadcrumb v-if="showBreadcrumb" />
            </slot>
          </template>
          <template #right-before>
            <slot name="header-right-before"></slot>
          </template>
          <template #right>
            <slot name="header-right"></slot>
          </template>
          <template v-if="slots['dropdown-menu']" #dropdown-menu>
            <slot name="dropdown-menu"></slot>
          </template>
          <template v-if="slots['header-user-menu']" #user-menu="slotProps">
            <slot name="header-user-menu" v-bind="slotProps"></slot>
          </template>
        </Header>
      </slot>

      <slot
        v-if="showTabs"
        name="tabs"
        :tabs="tabs"
        :cached-views="cachedViews"
      >
        <!-- 标签页视图 -->
        <TabsView
          :tabs="tabs"
          :cached-views="cachedViews"
          @add-tab="handleAddTab"
          @close-tab="handleCloseTab"
          @close-other-tabs="handleCloseOtherTabs"
          @close-right-tabs="handleCloseRightTabs"
          @close-left-tabs="handleCloseLeftTabs"
          @close-all-tabs="handleCloseAllTabs"
          @del-cached-view="handleDelCachedView"
          @add-cached-view="handleAddCachedView"
        />
      </slot>

      <!-- 主内容区域 -->
      <el-main class="main-content transition-theme">
        <slot name="main">
          <slot>
            <router-view v-if="useRouterView" v-slot="{ Component, route }">
              <keep-alive v-if="enableKeepAlive" :include="cachedViews">
                <component :is="Component" :key="route.path" />
              </keep-alive>
              <component v-else :is="Component" :key="route.path" />
            </router-view>
          </slot>
        </slot>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, useSlots } from 'vue'
import Sidebar from './Sidebar/index.vue'
import Header, { type UserInfo } from './Header/index.vue'
import TabsView, { type TabItem } from './TabsView/index.vue'
import Breadcrumb from '../Breadcrumb/index.vue'
import type { MenuItem } from './Sidebar/SidebarMenu.vue'

/**
 * 主布局组件 Props
 */
interface Props {
  /** Logo SVG 字符串 */
  logo?: string
  /** 系统标题 */
  title?: string
  /** 菜单列表 */
  menuList?: MenuItem[]
  /** 用户信息 */
  userInfo?: UserInfo | null
  /** 当前主题 */
  currentTheme?: 'light' | 'dark'
  /** 当前语言 */
  currentLanguage?: string
  /** 标签页列表 */
  tabs?: TabItem[]
  /** 缓存的视图名称列表 */
  cachedViews?: string[]
  /** 是否可拖拽调整侧边栏宽度 */
  resizable?: boolean
  /** 侧边栏默认宽度 */
  defaultSidebarWidth?: number
  /** 侧边栏最小宽度 */
  minSidebarWidth?: number
  /** 侧边栏最大宽度 */
  maxSidebarWidth?: number
  /** 是否显示全屏按钮 */
  showFullscreen?: boolean
  /** 是否显示主题切换 */
  showThemeToggle?: boolean
  /** 是否显示语言切换 */
  showLanguageSwitch?: boolean
  /** 是否显示主题设置 */
  showThemeSettings?: boolean
  /** 是否显示侧边栏 */
  showSidebar?: boolean
  /** 是否显示头部 */
  showHeader?: boolean
  /** 是否显示标签页 */
  showTabs?: boolean
  /** 是否显示默认面包屑 */
  showBreadcrumb?: boolean
  /** 是否渲染默认 router-view */
  useRouterView?: boolean
  /** 是否启用 keep-alive */
  enableKeepAlive?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  logo: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>',
  title: 'Blink Admin',
  menuList: () => [],
  userInfo: null,
  currentTheme: 'light',
  currentLanguage: 'zh_cn',
  tabs: () => [],
  cachedViews: () => [],
  resizable: true,
  defaultSidebarWidth: 220,
  minSidebarWidth: 60,
  maxSidebarWidth: 400,
  showFullscreen: true,
  showThemeToggle: true,
  showLanguageSwitch: true,
  showThemeSettings: false,
  showSidebar: true,
  showHeader: true,
  showTabs: true,
  showBreadcrumb: true,
  useRouterView: true,
  enableKeepAlive: true,
})

const emit = defineEmits<{
  (e: 'theme-toggle'): void
  (e: 'language-change', lang: string): void
  (e: 'user-command', command: string): void
  (e: 'add-tab', tab: TabItem): void
  (e: 'close-tab', path: string): string | null
  (e: 'close-other-tabs', path: string): void
  (e: 'close-right-tabs', path: string): void
  (e: 'close-left-tabs', path: string): void
  (e: 'close-all-tabs'): void
  (e: 'del-cached-view', name: string): void
  (e: 'add-cached-view', name: string): void
}>()

const slots = useSlots()
const sidebarRef = ref<InstanceType<typeof Sidebar>>()

const toggleSidebar = () => {
  sidebarRef.value?.toggleSidebar()
}

const handleThemeToggle = () => {
  emit('theme-toggle')
}

const handleLanguageChange = (lang: string) => {
  emit('language-change', lang)
}

const handleUserCommand = async (command: string) => {
  emit('user-command', command)
}

const handleAddTab = (tab: TabItem) => {
  emit('add-tab', tab)
}

const handleCloseTab = (path: string): string | null => {
  emit('close-tab', path)
  return null
}

const handleCloseOtherTabs = (path: string) => {
  emit('close-other-tabs', path)
}

const handleCloseRightTabs = (path: string) => {
  emit('close-right-tabs', path)
}

const handleCloseLeftTabs = (path: string) => {
  emit('close-left-tabs', path)
}

const handleCloseAllTabs = () => {
  emit('close-all-tabs')
}

const handleDelCachedView = (name: string) => {
  emit('del-cached-view', name)
}

const handleAddCachedView = (name: string) => {
  emit('add-cached-view', name)
}

// 暴露方法供父组件调用
defineExpose({
  toggleSidebar,
})
</script>

<style scoped lang="scss">
/* 布局容器 */
.layout-container {
  @apply h-screen;
  background: var(--bg-color-page);
}

/* 主内容区域 */
.main-content {
  @apply p-4;
  background: var(--bg-color-page);
  transition: background-color var(--duration-normal) var(--ease-out-expo);
  position: relative;
  overflow-y: auto;
  overflow-x: hidden;

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
</style>
