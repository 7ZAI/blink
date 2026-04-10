import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface TabItem {
  path: string
  name: string
  title: string
  fullPath: string
  query?: Record<string, any>
  params?: Record<string, any>
  affix?: boolean
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<TabItem[]>([])
  const activeTab = ref('')
  const cachedViews = ref<string[]>([])

  const getTabs = computed(() => tabs.value)
  const getActiveTab = computed(() => activeTab.value)
  const getCachedViews = computed(() => cachedViews.value)

  const addTab = (tab: TabItem) => {
    const exists = tabs.value.some((t) => t.path === tab.path)
    if (!exists) {
      tabs.value.push(tab)
    }
    activeTab.value = tab.path
    if (tab.name && !cachedViews.value.includes(tab.name)) {
      cachedViews.value.push(tab.name)
    }
  }

  const closeTab = (path: string): string | null => {
    const index = tabs.value.findIndex((t) => t.path === path)
    if (index === -1) return null

    const isActive = activeTab.value === path
    const closedTab = tabs.value[index]
    const cachedName = closedTab?.name

    tabs.value.splice(index, 1)

    if (cachedName) {
      const cacheIndex = cachedViews.value.indexOf(cachedName)
      if (cacheIndex > -1) {
        cachedViews.value.splice(cacheIndex, 1)
      }
    }

    if (tabs.value.length === 0) {
      activeTab.value = ''
      return '/dashboard'
    }

    if (isActive) {
      const nextTab = tabs.value[Math.min(index, tabs.value.length - 1)]
      if (nextTab) {
        activeTab.value = nextTab.path
        return nextTab.path
      }
    }

    return null
  }

  const closeOtherTabs = (path: string) => {
    tabs.value = tabs.value.filter((t) => t.path === path || t.affix)
    cachedViews.value = cachedViews.value.filter((name) => {
      return tabs.value.some((t) => t.name === name)
    })
    activeTab.value = path
  }

  const closeRightTabs = (path: string) => {
    const index = tabs.value.findIndex((t) => t.path === path)
    if (index === -1) return

    tabs.value = tabs.value.filter((t, i) => i <= index || t.affix)
    cachedViews.value = cachedViews.value.filter((name) => {
      return tabs.value.some((t) => t.name === name)
    })

    // 如果当前激活的标签页被关闭，跳转到指定标签页
    if (!tabs.value.some((t) => t.path === activeTab.value)) {
      activeTab.value = path
    }
  }

  const closeLeftTabs = (path: string) => {
    const index = tabs.value.findIndex((t) => t.path === path)
    if (index === -1) return

    tabs.value = tabs.value.filter((t, i) => i >= index || t.affix)
    cachedViews.value = cachedViews.value.filter((name) => {
      return tabs.value.some((t) => t.name === name)
    })

    // 如果当前激活的标签页被关闭，跳转到指定标签页
    if (!tabs.value.some((t) => t.path === activeTab.value)) {
      activeTab.value = path
    }
  }

  const closeAllTabs = () => {
    tabs.value = tabs.value.filter((t) => t.affix)
    cachedViews.value = cachedViews.value.filter((name) => {
      return tabs.value.some((t) => t.name === name)
    })
    activeTab.value = tabs.value[0]?.path || ''
  }

  const setActiveTab = (path: string) => {
    activeTab.value = path
  }

  const delCachedView = (name: string) => {
    const index = cachedViews.value.indexOf(name)
    if (index > -1) {
      cachedViews.value.splice(index, 1)
    }
  }

  const addCachedView = (name: string) => {
    if (!cachedViews.value.includes(name)) {
      cachedViews.value.push(name)
    }
  }

  return {
    tabs,
    activeTab,
    cachedViews,
    getTabs,
    getActiveTab,
    getCachedViews,
    addTab,
    closeTab,
    closeOtherTabs,
    closeRightTabs,
    closeLeftTabs,
    closeAllTabs,
    setActiveTab,
    delCachedView,
    addCachedView,
  }
})
