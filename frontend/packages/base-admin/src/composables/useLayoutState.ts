/**
 * 布局状态管理 Composable
 */

import { ref, computed, provide, inject, type Ref, type ComputedRef } from 'vue'
import { useSidebarState, type UseSidebarStateOptions, type UseSidebarStateReturn } from './useSidebarState'
import { useTabsState, type UseTabsStateOptions, type UseTabsStateReturn, type TabItem } from './useTabsState'
import { useHeaderState, type UseHeaderStateOptions, type UseHeaderStateReturn } from './useHeaderState'

export interface UseLayoutStateOptions {
  sidebar?: UseSidebarStateOptions
  tabs?: UseTabsStateOptions
  header?: UseHeaderStateOptions
  showSidebar?: boolean
  showHeader?: boolean
  showTabs?: boolean
  showBreadcrumb?: boolean
  useRouterView?: boolean
  enableKeepAlive?: boolean
}

/**
 * 布局 Props 类型别名（用于外部组件扩展）
 */
export type LayoutProps = UseLayoutStateOptions

export interface UseLayoutStateReturn {
  sidebar: UseSidebarStateReturn
  tabs: UseTabsStateReturn
  header: UseHeaderStateReturn
  showSidebar: Ref<boolean>
  showHeader: Ref<boolean>
  showTabs: Ref<boolean>
  showBreadcrumb: Ref<boolean>
  useRouterView: Ref<boolean>
  enableKeepAlive: Ref<boolean>
  toggleShowSidebar: () => void
  toggleShowHeader: () => void
  toggleShowTabs: () => void
  setLayoutConfig: (config: Partial<UseLayoutStateOptions>) => void
  layoutConfig: ComputedRef<{
    showSidebar: boolean
    showHeader: boolean
    showTabs: boolean
    showBreadcrumb: boolean
    useRouterView: boolean
    enableKeepAlive: boolean
  }>
}

export const LAYOUT_STATE_KEY = 'blink-layout-state'

export function useLayoutState(options: UseLayoutStateOptions = {}): UseLayoutStateReturn {
  const {
    sidebar = {},
    tabs = {},
    header = {},
    showSidebar = true,
    showHeader = true,
    showTabs = true,
    showBreadcrumb = true,
    useRouterView = true,
    enableKeepAlive = true,
  } = options

  const sidebarState = useSidebarState(sidebar)
  const tabsState = useTabsState(tabs)
  const headerState = useHeaderState(header)

  const showSidebarRef = ref(showSidebar)
  const showHeaderRef = ref(showHeader)
  const showTabsRef = ref(showTabs)
  const showBreadcrumbRef = ref(showBreadcrumb)
  const useRouterViewRef = ref(useRouterView)
  const enableKeepAliveRef = ref(enableKeepAlive)

  const toggleShowSidebar = () => {
    showSidebarRef.value = !showSidebarRef.value
  }

  const toggleShowHeader = () => {
    showHeaderRef.value = !showHeaderRef.value
  }

  const toggleShowTabs = () => {
    showTabsRef.value = !showTabsRef.value
  }

  const setLayoutConfig = (config: Partial<UseLayoutStateOptions>) => {
    if (config.showSidebar !== undefined) showSidebarRef.value = config.showSidebar
    if (config.showHeader !== undefined) showHeaderRef.value = config.showHeader
    if (config.showTabs !== undefined) showTabsRef.value = config.showTabs
    if (config.showBreadcrumb !== undefined) showBreadcrumbRef.value = config.showBreadcrumb
    if (config.useRouterView !== undefined) useRouterViewRef.value = config.useRouterView
    if (config.enableKeepAlive !== undefined) enableKeepAliveRef.value = config.enableKeepAlive
  }

  const layoutConfig = computed(() => ({
    showSidebar: showSidebarRef.value,
    showHeader: showHeaderRef.value,
    showTabs: showTabsRef.value,
    showBreadcrumb: showBreadcrumbRef.value,
    useRouterView: useRouterViewRef.value,
    enableKeepAlive: enableKeepAliveRef.value,
  }))

  provide(LAYOUT_STATE_KEY, {
    sidebar: sidebarState,
    tabs: tabsState,
    header: headerState,
    showSidebar: showSidebarRef,
    showHeader: showHeaderRef,
    showTabs: showTabsRef,
    showBreadcrumb: showBreadcrumbRef,
    useRouterView: useRouterViewRef,
    enableKeepAlive: enableKeepAliveRef,
    layoutConfig,
  })

  return {
    sidebar: sidebarState,
    tabs: tabsState,
    header: headerState,
    showSidebar: showSidebarRef,
    showHeader: showHeaderRef,
    showTabs: showTabsRef,
    showBreadcrumb: showBreadcrumbRef,
    useRouterView: useRouterViewRef,
    enableKeepAlive: enableKeepAliveRef,
    toggleShowSidebar,
    toggleShowHeader,
    toggleShowTabs,
    setLayoutConfig,
    layoutConfig,
  }
}

export function useInjectLayoutState(): UseLayoutStateReturn | undefined {
  return inject<UseLayoutStateReturn>(LAYOUT_STATE_KEY)
}

export type { TabItem }