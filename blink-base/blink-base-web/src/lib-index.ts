// src/lib-index.ts

// ============================================
// 组件导出
// ============================================

// 布局组件
export { default as MainLayout } from './components/Layout/MainLayout.vue'
export { default as Sidebar } from './components/Layout/Sidebar/index.vue'
export { default as Header } from './components/Layout/Header/index.vue'
export { default as UserDropdown } from './components/Layout/UserDropdown/index.vue'
export { default as TabsView } from './components/Layout/TabsView/index.vue'
export { default as Breadcrumb } from './components/Breadcrumb/index.vue'

// 选择器组件
export { default as IconSelector } from './components/IconSelector/index.vue'

// 功能组件
export { default as SkeletonLoader } from './components/SkeletonLoader/index.vue'
export { default as BlinkIcon } from './components/BlinkIcon/index.vue'

// ============================================
// 类型导出
// ============================================

// 布局组件类型
export type { MenuItem } from './components/Layout/Sidebar/SidebarMenu.vue'
export type { UserInfo } from './components/Layout/Header/index.vue'
export type { TabItem } from './components/Layout/TabsView/index.vue'
export type { BreadcrumbItem } from './components/Breadcrumb/index.vue'
export type { IconGroup } from './components/IconSelector/index.vue'

// ============================================
// Composables 导出
// ============================================

export { useTransition, useFadeIn } from './composables/useDataTransition'
export { useSubmitGuard } from './composables/useSubmitGuard'

// ============================================
// Directives 导出
// ============================================

export { dataFadeDirective, listFadeDirective, tableFadeDirective } from './directives/dataFade'
export { default as rippleDirective } from './directives/ripple'
