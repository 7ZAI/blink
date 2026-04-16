<template>
  <el-container class="layout-container" :class="layoutClass">
    <!-- ========== 侧边栏区域 ========== -->
    <slot v-if="showSidebar" name="sidebar" v-bind="sidebarSlotProps">
      <!-- 默认侧边栏 -->
      <Sidebar
        ref="sidebarRef"
        :logo="logo"
        :title="title"
        :menu-list="menuList"
        :resizable="resizable"
        :default-width="defaultSidebarWidth"
        :min-width="minSidebarWidth"
        :max-width="maxSidebarWidth"
        :storage-key="storageKey ? `${storageKey}-sidebar` : undefined"
        @collapse-change="handleSidebarCollapseChange"
        @width-change="handleSidebarWidthChange"
      >
        <!-- 透传 sidebar 子插槽 -->
        <template v-if="$slots['sidebar-logo']" #logo="slotProps">
          <slot name="sidebar-logo" v-bind="slotProps" />
        </template>
        <template v-if="$slots['sidebar-menu']" #menu="slotProps">
          <slot name="sidebar-menu" v-bind="slotProps" />
        </template>
        <template v-if="$slots['sidebar-top']" #top="slotProps">
          <slot name="sidebar-top" v-bind="slotProps" />
        </template>
        <template v-if="$slots['sidebar-bottom']" #bottom="slotProps">
          <slot name="sidebar-bottom" v-bind="slotProps" />
        </template>
        <template v-if="$slots['sidebar-collapse-trigger']" #collapse-trigger="slotProps">
          <slot name="sidebar-collapse-trigger" v-bind="slotProps" />
        </template>
      </Sidebar>
    </slot>

    <!-- ========== 主内容区域 ========== -->
    <el-container direction="vertical" class="main-container">
      <!-- ========== 头部区域 ========== -->
      <slot v-if="showHeader" name="header" v-bind="headerSlotProps">
        <!-- 默认头部 -->
        <Header
          :user-info="userInfo"
          :theme="currentTheme"
          :current-language="currentLanguage"
          :languages="languages"
          :show-fullscreen="showFullscreen"
          :show-theme-toggle="showThemeToggle"
          :show-language-switch="showLanguageSwitch"
          :show-theme-settings="showThemeSettings"
          :show-breadcrumb="showBreadcrumb"
          :show-action-labels="showActionLabels"
          :labels="labels"
          :avatar-resolver="avatarResolver"
          @theme-toggle="handleThemeToggle"
          @theme-change="handleThemeChange"
          @language-change="handleLanguageChange"
          @user-command="handleUserCommand"
          @fullscreen-change="handleFullscreenChange"
        >
          <!-- 透传 header 子插槽 -->
          <template v-if="$slots['header-left']" #left>
            <slot name="header-left" />
          </template>
          <template v-if="$slots['header-right-before']" #right-before>
            <slot name="header-right-before" />
          </template>
          <template v-if="$slots['header-right-middle']" #right-middle>
            <slot name="header-right-middle" />
          </template>
          <template v-if="$slots['header-right']" #right>
            <slot name="header-right" />
          </template>
          <template v-if="$slots['header-fullscreen']" #fullscreen="slotProps">
            <slot name="header-fullscreen" v-bind="slotProps" />
          </template>
          <template v-if="$slots['header-theme-toggle']" #theme-toggle="slotProps">
            <slot name="header-theme-toggle" v-bind="slotProps" />
          </template>
          <template v-if="$slots['header-language-switch']" #language-switch="slotProps">
            <slot name="header-language-switch" v-bind="slotProps" />
          </template>
          <template v-if="$slots['header-user-menu']" #user-menu="slotProps">
            <slot name="header-user-menu" v-bind="slotProps" />
          </template>
          <template v-if="$slots['dropdown-menu']" #dropdown-menu>
            <slot name="dropdown-menu" />
          </template>
        </Header>
      </slot>

      <!-- ========== 标签页区域 ========== -->
      <slot v-if="showTabs" name="tabs" v-bind="tabsSlotProps">
        <!-- 默认标签页 -->
        <TabsView
          ref="tabsViewRef"
          :tabs="tabs"
          :active-path="activePath"
          :show-context-menu="showTabsContextMenu"
          :max-tabs="maxTabs"
          :overflow-warning-threshold="overflowWarningThreshold"
          :min-tab-width="minTabWidth"
          :max-tab-width="maxTabWidth"
          @tab-click="handleTabClick"
          @close-tab="handleCloseTab"
          @close-other-tabs="handleCloseOtherTabs"
          @close-right-tabs="handleCloseRightTabs"
          @close-left-tabs="handleCloseLeftTabs"
          @close-all-tabs="handleCloseAllTabs"
          @refresh-tab="handleRefreshTab"
          @max-tabs-reached="handleMaxTabsReached"
        />
      </slot>

      <!-- ========== 主内容区域 ========== -->
      <el-main class="main-content">
        <slot name="main">
          <slot>
            <!-- 默认渲染 router-view -->
            <router-view v-if="useRouterView" v-slot="{ Component, route }">
              <keep-alive v-if="enableKeepAlive" :include="cachedViews">
                <component :is="Component" :key="route.path" />
              </keep-alive>
              <component v-else :is="Component" :key="route.path" />
            </router-view>
          </slot>
        </slot>
      </el-main>

      <!-- ========== 底部区域 ========== -->
      <slot v-if="$slots.footer" name="footer" />
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
/**
 * MainLayout 主布局组件
 *
 * 设计理念：
 * 1. 组合式布局 - 通过 slot 组合各个区域，完全可定制
 * 2. 状态解耦 - 状态通过 composables 管理，可外部复用
 * 3. 区域替换 - 每个区域都可完全替换，不局限于子插槽
 * 4. 样式可覆盖 - 使用 CSS 变量，支持主题定制
 *
 * 使用方式：
 * 1. 完全使用默认布局 - 直接使用，传入必要 props
 * 2. 替换某个区域 - 使用区域 slot (sidebar/header/tabs/main)
 * 3. 完全自定义 - 使用 default slot 完全控制布局
 */

import { ref, computed, useSlots, provide } from 'vue'
import Sidebar, { type MenuItem } from './Sidebar/index.vue'
import Header, { type UserInfo, type HeaderLabels } from './Header/index.vue'
import TabsView, { type TabItem } from './TabsView/index.vue'
import { LAYOUT_STATE_KEY } from '../../composables/useLayoutState'
import type { LanguageOption } from './LanguageSwitch/index.vue'

// ============================================
// 类型定义
// ============================================

/**
 * MainLayout Props
 */
export interface Props {
  // === 基础配置 ===
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
  /** 语言列表 */
  languages?: LanguageOption[]
  /** 文本标签配置 */
  labels?: HeaderLabels
  /** 头像解析函数 */
  avatarResolver?: (user: UserInfo) => string

  // === 标签页配置 ===
  /** 标签页列表 */
  tabs?: TabItem[]
  /** 当前激活路径 */
  activePath?: string
  /** 是否显示标签页右键菜单 */
  showTabsContextMenu?: boolean
  /** 最大标签数量，默认 20 */
  maxTabs?: number
  /** 标签溢出警告阈值 */
  overflowWarningThreshold?: number
  /** 标签最小宽度（像素） */
  minTabWidth?: number
  /** 标签最大宽度（像素） */
  maxTabWidth?: number

  // === 侧边栏配置 ===
  /** 是否显示侧边栏 */
  showSidebar?: boolean
  /** 是否可拖拽调整侧边栏宽度 */
  resizable?: boolean
  /** 侧边栏默认宽度 */
  defaultSidebarWidth?: number
  /** 侧边栏最小宽度 */
  minSidebarWidth?: number
  /** 侧边栏最大宽度 */
  maxSidebarWidth?: number
  /** 本地存储 key 前缀 */
  storageKey?: string

  // === 头部配置 ===
  /** 是否显示头部 */
  showHeader?: boolean
  /** 是否显示面包屑 */
  showBreadcrumb?: boolean
  /** 是否显示全屏按钮 */
  showFullscreen?: boolean
  /** 是否显示主题切换 */
  showThemeToggle?: boolean
  /** 是否显示语言切换 */
  showLanguageSwitch?: boolean
  /** 是否显示主题设置 */
  showThemeSettings?: boolean
  /** 是否显示操作按钮文本标签 */
  showActionLabels?: boolean

  // === 标签页配置 ===
  /** 是否显示标签页 */
  showTabs?: boolean

  // === 内容配置 ===
  /** 是否渲染默认 router-view */
  useRouterView?: boolean
  /** 是否启用 keep-alive */
  enableKeepAlive?: boolean
  /** 缓存的视图名称列表 */
  cachedViews?: string[]
}

/**
 * MainLayout Events
 */
export interface Emits {
  // === 主题和语言 ===
  (e: 'theme-toggle'): void
  (e: 'theme-change', theme: 'light' | 'dark'): void
  (e: 'language-change', lang: string): void
  (e: 'user-command', command: string): void
  (e: 'fullscreen-change', isFullscreen: boolean): void

  // === 侧边栏 ===
  (e: 'sidebar-collapse-change', collapsed: boolean): void
  (e: 'sidebar-width-change', width: number): void

  // === 标签页 ===
  (e: 'tab-click', tab: TabItem): void
  (e: 'close-tab', tab: TabItem): void
  (e: 'close-other-tabs', tab: TabItem): void
  (e: 'close-right-tabs', tab: TabItem): void
  (e: 'close-left-tabs', tab: TabItem): void
  (e: 'close-all-tabs'): void
  (e: 'refresh-tab', tab: TabItem): void
  (e: 'max-tabs-reached', currentCount: number, maxTabs: number): void
}

// ============================================
// Props 和 Emits 定义
// ============================================

const props = withDefaults(defineProps<Props>(), {
  logo: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>',
  title: 'Blink Admin',
  menuList: () => [],
  userInfo: null,
  currentTheme: 'light',
  currentLanguage: 'zh_cn',
  languages: () => [
    { code: 'zh_cn', label: '中文', nativeLabel: '简体中文' },
    { code: 'en_us', label: 'EN', nativeLabel: 'English' },
  ],
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
  tabs: () => [],
  activePath: '',
  showTabsContextMenu: true,
  maxTabs: 20,
  overflowWarningThreshold: 15,
  minTabWidth: 80,
  maxTabWidth: 160,
  showSidebar: true,
  resizable: true,
  defaultSidebarWidth: 220,
  minSidebarWidth: 60,
  maxSidebarWidth: 400,
  storageKey: 'blink-layout',
  showHeader: true,
  showBreadcrumb: true,
  showFullscreen: true,
  showThemeToggle: true,
  showLanguageSwitch: true,
  showThemeSettings: false,
  showActionLabels: false,
  showTabs: true,
  useRouterView: true,
  enableKeepAlive: true,
  cachedViews: () => [],
})

const emit = defineEmits<Emits>()
const slots = useSlots()

// ============================================
// 内部状态
// ============================================

const sidebarRef = ref<InstanceType<typeof Sidebar>>()
const tabsViewRef = ref<InstanceType<typeof TabsView>>()

// ============================================
// 插槽 Props
// ============================================

/** 侧边栏插槽 Props */
const sidebarSlotProps = computed(() => ({
  logo: props.logo,
  title: props.title,
  menuList: props.menuList,
  resizable: props.resizable,
  defaultWidth: props.defaultSidebarWidth,
  minWidth: props.minSidebarWidth,
  maxWidth: props.maxSidebarWidth,
  toggleSidebar: () => sidebarRef.value?.toggleSidebar(),
}))

/** 头部插槽 Props */
const headerSlotProps = computed(() => ({
  userInfo: props.userInfo,
  theme: props.currentTheme,
  currentLanguage: props.currentLanguage,
  languages: props.languages,
  showFullscreen: props.showFullscreen,
  showThemeToggle: props.showThemeToggle,
  showLanguageSwitch: props.showLanguageSwitch,
  showThemeSettings: props.showThemeSettings,
  avatarResolver: props.avatarResolver,
  toggleSidebar: () => sidebarRef.value?.toggleSidebar(),
}))

/** 标签页插槽 Props */
const tabsSlotProps = computed(() => ({
  tabs: props.tabs,
  activePath: props.activePath,
}))

// ============================================
// 布局类名
// ============================================

const layoutClass = computed(() => ({
  'layout-with-sidebar': props.showSidebar,
  'layout-without-sidebar': !props.showSidebar,
  'layout-with-header': props.showHeader,
  'layout-with-tabs': props.showTabs,
}))

// ============================================
// 事件处理
// ============================================

// 主题和语言
const handleThemeToggle = () => emit('theme-toggle')
const handleThemeChange = (theme: 'light' | 'dark') => emit('theme-change', theme)
const handleLanguageChange = (lang: string) => emit('language-change', lang)
const handleUserCommand = (command: string) => emit('user-command', command)
const handleFullscreenChange = (isFullscreen: boolean) => emit('fullscreen-change', isFullscreen)

// 侧边栏
const handleSidebarCollapseChange = (collapsed: boolean) =>
  emit('sidebar-collapse-change', collapsed)
const handleSidebarWidthChange = (width: number) => emit('sidebar-width-change', width)

// 标签页
const handleTabClick = (tab: TabItem) => emit('tab-click', tab)
const handleCloseTab = (tab: TabItem) => emit('close-tab', tab)
const handleCloseOtherTabs = (tab: TabItem) => emit('close-other-tabs', tab)
const handleCloseRightTabs = (tab: TabItem) => emit('close-right-tabs', tab)
const handleCloseLeftTabs = (tab: TabItem) => emit('close-left-tabs', tab)
const handleCloseAllTabs = () => emit('close-all-tabs')
const handleRefreshTab = (tab: TabItem) => emit('refresh-tab', tab)
const handleMaxTabsReached = (currentCount: number, maxTabs: number) =>
  emit('max-tabs-reached', currentCount, maxTabs)

// ============================================
// 全局状态注入
// ============================================

provide(LAYOUT_STATE_KEY, {
  showSidebar: computed(() => props.showSidebar),
  showHeader: computed(() => props.showHeader),
  showTabs: computed(() => props.showTabs),
  toggleSidebar: () => sidebarRef.value?.toggleSidebar(),
})

// ============================================
// 暴露方法
// ============================================

defineExpose({
  toggleSidebar: () => sidebarRef.value?.toggleSidebar(),
  sidebarRef,
  tabsViewRef,
  /**
   * 检查是否可以添加新标签
   * @returns true 表示可以继续添加，false 表示已达到上限
   */
  canAddTab: () => tabsViewRef.value?.checkMaxTabs() ?? true,
})
</script>

<style scoped lang="scss">
/* === 布局容器 === */
.layout-container {
  height: 100vh;
  background: var(--bg-color-page, #f5f7fa);
}

/* === 主内容容器 === */
.main-container {
  flex: 1;
  overflow: hidden;
}

/* === 主内容区域 === */
.main-content {
  padding: 16px;
  background: var(--bg-color-page, #f5f7fa);
  position: relative;
  overflow-y: auto;
  overflow-x: hidden;
  /* 修复 flex 子元素高度计算问题 */
  min-height: 0;
  flex: 1;

  > * {
    position: relative;
    z-index: 1;
  }
}

/* === 过渡动画 === */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
