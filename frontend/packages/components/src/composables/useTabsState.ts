/**
 * 标签页状态管理 Composable
 */

import { ref, computed, watch, type Ref, type ComputedRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export interface TabItem {
  path: string
  name: string
  title: string
  fullPath: string
  query?: Record<string, any>
  params?: Record<string, any>
  affix?: boolean
}

export interface UseTabsStateOptions {
  initialTabs?: TabItem[]
  initialCachedViews?: string[]
  autoAddCurrentRoute?: boolean
  closeFallbackPath?: string
  onTabChange?: (tabs: TabItem[]) => void
  onCachedViewChange?: (views: string[]) => void
  routeIntegration?: boolean
}

export interface UseTabsStateReturn {
  tabs: Ref<TabItem[]>
  cachedViews: Ref<string[]>
  activePath: ComputedRef<string>
  addTab: (tab: TabItem) => void
  closeTab: (path: string) => TabItem | null
  closeOtherTabs: (path: string) => void
  closeRightTabs: (path: string) => void
  closeLeftTabs: (path: string) => void
  closeAllTabs: () => void
  addCachedView: (name: string) => void
  delCachedView: (name: string) => void
  isActive: (tab: TabItem) => boolean
  hasTab: (path: string) => boolean
}

export function useTabsState(options: UseTabsStateOptions = {}): UseTabsStateReturn {
  const {
    initialTabs = [],
    initialCachedViews = [],
    autoAddCurrentRoute = true,
    closeFallbackPath = '/dashboard',
    onTabChange,
    onCachedViewChange,
    routeIntegration = true,
  } = options

  const tabs = ref<TabItem[]>(initialTabs)
  const cachedViews = ref<string[]>(initialCachedViews)

  const route = useRoute()
  const router = useRouter()

  const activePath = computed(() => route.path)

  const isActive = (tab: TabItem) => tab.path === activePath.value
  const hasTab = (path: string) => tabs.value.some((t) => t.path === path)

  const addTab = (tab: TabItem) => {
    if (hasTab(tab.path)) return
    tabs.value.push(tab)
    onTabChange?.(tabs.value)
  }

  const closeTab = (path: string): TabItem | null => {
    const index = tabs.value.findIndex((t) => t.path === path)
    if (index === -1) return null

    const [closedTab] = tabs.value.splice(index, 1)
    if (!closedTab) return null

    onTabChange?.(tabs.value)

    if (closedTab.name) {
      delCachedView(closedTab.name)
    }

    if (routeIntegration && path === activePath.value) {
      const nextTab = tabs.value[index] || tabs.value[index - 1]
      if (nextTab) {
        router.push({ path: nextTab.path, query: nextTab.query || {} })
      } else if (closeFallbackPath) {
        router.push(closeFallbackPath)
      }
    }

    return closedTab
  }

  const closeOtherTabs = (path: string) => {
    tabs.value = tabs.value.filter((t) => t.path === path || t.affix)
    cachedViews.value = cachedViews.value.filter((name) =>
      tabs.value.some((t) => t.name === name)
    )
    onTabChange?.(tabs.value)
    onCachedViewChange?.(cachedViews.value)
  }

  const closeRightTabs = (path: string) => {
    const index = tabs.value.findIndex((t) => t.path === path)
    if (index === -1) return
    tabs.value = tabs.value.filter((t, i) => i <= index || t.affix)
    cachedViews.value = cachedViews.value.filter((name) =>
      tabs.value.some((t) => t.name === name)
    )
    onTabChange?.(tabs.value)
    onCachedViewChange?.(cachedViews.value)
  }

  const closeLeftTabs = (path: string) => {
    const index = tabs.value.findIndex((t) => t.path === path)
    if (index === -1) return
    tabs.value = tabs.value.filter((t, i) => i >= index || t.affix)
    cachedViews.value = cachedViews.value.filter((name) =>
      tabs.value.some((t) => t.name === name)
    )
    onTabChange?.(tabs.value)
    onCachedViewChange?.(cachedViews.value)
  }

  const closeAllTabs = () => {
    tabs.value = tabs.value.filter((t) => t.affix)
    cachedViews.value = cachedViews.value.filter((name) =>
      tabs.value.some((t) => t.name === name)
    )
    onTabChange?.(tabs.value)
    onCachedViewChange?.(cachedViews.value)
  }

  const addCachedView = (name: string) => {
    if (!cachedViews.value.includes(name)) {
      cachedViews.value.push(name)
      onCachedViewChange?.(cachedViews.value)
    }
  }

  const delCachedView = (name: string) => {
    const index = cachedViews.value.indexOf(name)
    if (index > -1) {
      cachedViews.value.splice(index, 1)
      onCachedViewChange?.(cachedViews.value)
    }
  }

  watch(tabs, (value) => onTabChange?.(value), { deep: true })
  watch(cachedViews, (value) => onCachedViewChange?.(value), { deep: true })

  return {
    tabs,
    cachedViews,
    activePath,
    addTab,
    closeTab,
    closeOtherTabs,
    closeRightTabs,
    closeLeftTabs,
    closeAllTabs,
    addCachedView,
    delCachedView,
    isActive,
    hasTab,
  }
}