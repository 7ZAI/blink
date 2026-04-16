import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

/**
 * 标签状态类型
 */
export type TabStatus = 'normal' | 'loading' | 'error' | 'modified'

/**
 * 标签页项接口（扩展版）
 */
export interface TabItem {
  path: string
  name: string
  title: string
  fullPath: string
  query?: Record<string, string>
  params?: Record<string, string>
  affix?: boolean
  closable?: boolean
  icon?: string
  status?: TabStatus
  badge?: string | number
  tooltip?: string
  isNew?: boolean
  dynamicTitle?: string
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<TabItem[]>([])
  const activeTabPath = ref('')
  const cachedViews = ref<string[]>([])

  const getTabs = computed(() => tabs.value)
  const getActiveTabPath = computed(() => activeTabPath.value)
  const getCachedViews = computed(() => cachedViews.value)

  /**
   * 初始化固定标签
   */
  const initAffixTabs = (routes: RouteLocationNormalized[]) => {
    routes.forEach((route) => {
      if (route.meta?.affix && route.name) {
        addTab({
          path: route.path,
          name: route.name as string,
          title: (route.meta?.title as string) || (route.name as string),
          fullPath: route.fullPath,
          affix: true,
        })
      }
    })
  }

  /**
   * 添加标签
   * 使用 fullPath 作为唯一标识（包含 query 参数）
   */
  const addTab = (tab: TabItem) => {
    // 使用 fullPath 判断唯一性，支持带 query 的详情页打开多个标签
    const uniqueKey = tab.fullPath || tab.path
    const exists = tabs.value.some((t) => (t.fullPath || t.path) === uniqueKey)
    if (!exists) {
      tabs.value.push(tab)
    }
    activeTabPath.value = uniqueKey
    // 缓存视图使用 name + query 后缀，确保不同详情页独立缓存
    const cacheKey = getCacheKey(tab)
    if (tab.name && !cachedViews.value.includes(cacheKey)) {
      cachedViews.value.push(cacheKey)
    }
  }

  /**
   * 获取缓存视图的 key
   * 对于详情页，使用 name + instanceId 作为唯一缓存 key
   */
  const getCacheKey = (tab?: TabItem): string => {
    if (!tab) return ''
    if (tab.query?.id) {
      return `${tab.name}_${tab.query.id}`
    }
    if (tab.query?.instanceId) {
      return `${tab.name}_${tab.query.instanceId}`
    }
    return tab.name || ''
  }

  /**
   * 关闭标签
   */
  const closeTab = (path: string): string | null => {
    // 支持 fullPath 匹配
    const index = tabs.value.findIndex((t) => (t.fullPath || t.path) === path)
    if (index === -1) return null

    const closedTab = tabs.value[index]
    if (closedTab?.affix) return null

    const cacheKey = getCacheKey(closedTab)
    tabs.value.splice(index, 1)

    if (cacheKey) {
      const cacheIndex = cachedViews.value.indexOf(cacheKey)
      if (cacheIndex > -1) {
        cachedViews.value.splice(cacheIndex, 1)
      }
    }

    if (tabs.value.length === 0) {
      activeTabPath.value = ''
      return '/dashboard'
    }

    if (activeTabPath.value === path) {
      const nextTab = tabs.value[Math.min(index, tabs.value.length - 1)]
      if (nextTab) {
        activeTabPath.value = nextTab.fullPath || nextTab.path
        return nextTab.fullPath || nextTab.path
      }
    }

    return null
  }

  /**
   * 关闭其他标签
   */
  const closeOtherTabs = (path: string) => {
    tabs.value = tabs.value.filter((t) => (t.fullPath || t.path) === path || t.affix)
    cachedViews.value = cachedViews.value.filter((key) => {
      return tabs.value.some((t) => getCacheKey(t) === key)
    })
    activeTabPath.value = path
  }

  /**
   * 关闭左侧标签
   */
  const closeLeftTabs = (path: string) => {
    const index = tabs.value.findIndex((t) => (t.fullPath || t.path) === path)
    if (index === -1) return

    const leftTabs = tabs.value.slice(0, index)
    leftTabs.forEach((t) => {
      if (!t.affix) {
        const tabIndex = tabs.value.findIndex((tab) => (tab.fullPath || tab.path) === (t.fullPath || t.path))
        if (tabIndex > -1) {
          tabs.value.splice(tabIndex, 1)
          const cacheKey = getCacheKey(t)
          if (cacheKey) {
            const cacheIndex = cachedViews.value.indexOf(cacheKey)
            if (cacheIndex > -1) {
              cachedViews.value.splice(cacheIndex, 1)
            }
          }
        }
      }
    })
  }

  /**
   * 关闭右侧标签
   */
  const closeRightTabs = (path: string) => {
    const index = tabs.value.findIndex((t) => (t.fullPath || t.path) === path)
    if (index === -1) return

    const rightTabs = tabs.value.slice(index + 1)
    rightTabs.forEach((t) => {
      if (!t.affix) {
        const tabIndex = tabs.value.findIndex((tab) => (tab.fullPath || tab.path) === (t.fullPath || t.path))
        if (tabIndex > -1) {
          tabs.value.splice(tabIndex, 1)
          const cacheKey = getCacheKey(t)
          if (cacheKey) {
            const cacheIndex = cachedViews.value.indexOf(cacheKey)
            if (cacheIndex > -1) {
              cachedViews.value.splice(cacheIndex, 1)
            }
          }
        }
      }
    })
  }

  /**
   * 关闭所有标签
   */
  const closeAllTabs = () => {
    tabs.value = tabs.value.filter((t) => t.affix)
    cachedViews.value = cachedViews.value.filter((key) => {
      return tabs.value.some((t) => getCacheKey(t) === key)
    })
    activeTabPath.value = tabs.value[0]?.fullPath || tabs.value[0]?.path || ''
  }

  /**
   * 设置当前激活标签
   */
  const setActiveTab = (path: string) => {
    activeTabPath.value = path
  }

  /**
   * 刷新标签 - 删除缓存
   */
  const refreshTab = (name: string) => {
    const index = cachedViews.value.indexOf(name)
    if (index > -1) {
      cachedViews.value.splice(index, 1)
    }
  }

  /**
   * 删除缓存视图（refreshTab 别名，兼容 blink-base-web TabsView）
   */
  const delCachedView = (name: string) => {
    refreshTab(name)
  }

  /**
   * 添加缓存视图
   */
  const addCachedView = (name: string) => {
    if (!cachedViews.value.includes(name)) {
      cachedViews.value.push(name)
    }
  }

  /**
   * 更新标签标题
   */
  const updateTabTitle = (path: string, title: string) => {
    const tab = tabs.value.find((t) => (t.fullPath || t.path) === path)
    if (tab) {
      tab.dynamicTitle = title
    }
  }

  return {
    tabs,
    activeTabPath,
    cachedViews,
    getTabs,
    getActiveTabPath,
    getCachedViews,
    initAffixTabs,
    addTab,
    closeTab,
    closeOtherTabs,
    closeLeftTabs,
    closeRightTabs,
    closeAllTabs,
    setActiveTab,
    delCachedView,
    refreshTab,
    addCachedView,
    updateTabTitle,
    getCacheKey,
  }
})
