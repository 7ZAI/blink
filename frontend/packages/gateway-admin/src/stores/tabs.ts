import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

export interface TabItem {
  path: string
  name: string
  title: string
  fullPath: string
  query?: Record<string, string>
  affix?: boolean
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
    routes.forEach(route => {
      if (route.meta?.affix && route.name) {
        addTab({
          path: route.path,
          name: route.name as string,
          title: (route.meta?.title as string) || route.name as string,
          fullPath: route.fullPath,
          affix: true
        })
      }
    })
  }

  /**
   * 添加标签
   */
  const addTab = (tab: TabItem) => {
    const exists = tabs.value.some(t => t.path === tab.path)
    if (!exists) {
      tabs.value.push(tab)
    }
    activeTabPath.value = tab.path
    if (tab.name && !cachedViews.value.includes(tab.name)) {
      cachedViews.value.push(tab.name)
    }
  }

  /**
   * 关闭标签
   */
  const closeTab = (path: string): string | null => {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index === -1) return null

    const closedTab = tabs.value[index]
    if (closedTab?.affix) return null

    const cachedName = closedTab?.name
    tabs.value.splice(index, 1)

    if (cachedName) {
      const cacheIndex = cachedViews.value.indexOf(cachedName)
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
        activeTabPath.value = nextTab.path
        return nextTab.path
      }
    }

    return null
  }

  /**
   * 关闭其他标签
   */
  const closeOtherTabs = (path: string) => {
    tabs.value = tabs.value.filter(t => t.path === path || t.affix)
    cachedViews.value = cachedViews.value.filter(name => {
      return tabs.value.some(t => t.name === name)
    })
    activeTabPath.value = path
  }

  /**
   * 关闭左侧标签
   */
  const closeLeftTabs = (path: string) => {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index === -1) return

    const leftTabs = tabs.value.slice(0, index)
    leftTabs.forEach(t => {
      if (!t.affix) {
        const tabIndex = tabs.value.findIndex(tab => tab.path === t.path)
        if (tabIndex > -1) {
          tabs.value.splice(tabIndex, 1)
          if (t.name) {
            const cacheIndex = cachedViews.value.indexOf(t.name)
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
    const index = tabs.value.findIndex(t => t.path === path)
    if (index === -1) return

    const rightTabs = tabs.value.slice(index + 1)
    rightTabs.forEach(t => {
      if (!t.affix) {
        const tabIndex = tabs.value.findIndex(tab => tab.path === t.path)
        if (tabIndex > -1) {
          tabs.value.splice(tabIndex, 1)
          if (t.name) {
            const cacheIndex = cachedViews.value.indexOf(t.name)
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
    tabs.value = tabs.value.filter(t => t.affix)
    cachedViews.value = cachedViews.value.filter(name => {
      return tabs.value.some(t => t.name === name)
    })
    activeTabPath.value = tabs.value[0]?.path || ''
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
    addCachedView
  }
})