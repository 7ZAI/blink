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

// 功能组件
export { default as ThemeToggle } from './components/Layout/ThemeToggle/index.vue'
export { default as LanguageSwitch } from './components/Layout/LanguageSwitch/index.vue'
export { default as FullscreenToggle } from './components/Layout/FullscreenToggle/index.vue'
export { default as SkeletonLoader } from './components/SkeletonLoader/index.vue'
export { default as BlinkIcon } from './components/BlinkIcon/index.vue'

// 选择器组件
export { default as IconSelector } from './components/IconSelector/index.vue'

// ============================================
// 类型导出
// ============================================

// 布局组件类型
export type { MenuItem } from './components/Layout/Sidebar/SidebarMenu.vue'
export type { TabItem } from './components/Layout/TabsView/index.vue'
export type { BreadcrumbItem } from './components/Breadcrumb/index.vue'
export type { IconGroup } from './components/IconSelector/index.vue'

// Header 组件类型
export type { UserInfo } from './components/Layout/UserDropdown/index.vue'
export type { MenuItem as UserMenuItem } from './components/Layout/UserDropdown/index.vue'
export type { LanguageOption } from './components/Layout/LanguageSwitch/index.vue'
export type { HeaderLabels } from './components/Layout/Header/index.vue'

// Props 类型导出（用于外部组件扩展）
export type { Props as SidebarProps } from './components/Layout/Sidebar/index.vue'
export type { Props as HeaderProps } from './components/Layout/Header/index.vue'
export type { Props as MainLayoutProps } from './components/Layout/MainLayout.vue'
export type { Props as UserDropdownProps } from './components/Layout/UserDropdown/index.vue'
export type { Props as ThemeToggleProps } from './components/Layout/ThemeToggle/index.vue'
export type { Props as LanguageSwitchProps } from './components/Layout/LanguageSwitch/index.vue'
export type { Props as FullscreenToggleProps } from './components/Layout/FullscreenToggle/index.vue'

// ============================================
// Composables 导出
// ============================================

// 布局状态管理 Composables
export { useSidebarState } from './composables/useSidebarState'
export type { UseSidebarStateOptions, UseSidebarStateReturn } from './composables/useSidebarState'

export { useTabsState } from './composables/useTabsState'
export type { UseTabsStateOptions, UseTabsStateReturn, TabItem as TabItemState } from './composables/useTabsState'

export { useHeaderState } from './composables/useHeaderState'
export type { UseHeaderStateOptions, UseHeaderStateReturn } from './composables/useHeaderState'

export { useLayoutState, useInjectLayoutState, LAYOUT_STATE_KEY } from './composables/useLayoutState'
export type { UseLayoutStateOptions, UseLayoutStateReturn, LayoutProps } from './composables/useLayoutState'

export { useThemeSettings } from './composables/useThemeSettings'
export type {
  UseThemeSettingsOptions,
  UseThemeSettingsReturn,
  ThemeColors,
  FontConfig,
  ThemePreset,
} from './composables/useThemeSettings'

// 其他 Composables
export { useTransition, useFadeIn } from './composables/useDataTransition'
export { useSubmitGuard } from './composables/useSubmitGuard'

// ============================================
// Directives 导出
// ============================================

export { dataFadeDirective, listFadeDirective, tableFadeDirective } from './directives/dataFade'
export { default as rippleDirective } from './directives/ripple'