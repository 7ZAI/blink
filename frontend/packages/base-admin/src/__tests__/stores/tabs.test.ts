import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTabsStore } from '@/stores/tabs'
import type { TabItem } from '@/stores/tabs'

describe('Tabs Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('initial state', () => {
    it('should have empty tabs by default', () => {
      const store = useTabsStore()
      expect(store.tabs).toEqual([])
      expect(store.activeTab).toBe('')
      expect(store.cachedViews).toEqual([])
    })
  })

  describe('addTab', () => {
    it('should add a new tab', () => {
      const store = useTabsStore()
      const tab: TabItem = {
        path: '/dashboard',
        name: 'Dashboard',
        title: '仪表盘',
        fullPath: '/dashboard',
      }

      store.addTab(tab)

      expect(store.tabs).toHaveLength(1)
      expect(store.tabs[0]).toEqual(tab)
      expect(store.activeTab).toBe('/dashboard')
    })

    it('should not add duplicate tab', () => {
      const store = useTabsStore()
      const tab: TabItem = {
        path: '/dashboard',
        name: 'Dashboard',
        title: '仪表盘',
        fullPath: '/dashboard',
      }

      store.addTab(tab)
      store.addTab(tab)

      expect(store.tabs).toHaveLength(1)
    })

    it('should update activeTab when adding duplicate', () => {
      const store = useTabsStore()
      const tab: TabItem = {
        path: '/dashboard',
        name: 'Dashboard',
        title: '仪表盘',
        fullPath: '/dashboard',
      }

      store.addTab(tab)
      // 添加其他标签页
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      // 再次添加第一个
      store.addTab(tab)

      expect(store.activeTab).toBe('/dashboard')
    })

    it('should add name to cachedViews', () => {
      const store = useTabsStore()
      const tab: TabItem = {
        path: '/dashboard',
        name: 'Dashboard',
        title: '仪表盘',
        fullPath: '/dashboard',
      }

      store.addTab(tab)

      expect(store.cachedViews).toContain('Dashboard')
    })

    it('should not add duplicate name to cachedViews', () => {
      const store = useTabsStore()
      const tab: TabItem = {
        path: '/dashboard',
        name: 'Dashboard',
        title: '仪表盘',
        fullPath: '/dashboard',
      }

      store.addTab(tab)
      store.addTab(tab)

      expect(store.cachedViews.filter(v => v === 'Dashboard')).toHaveLength(1)
    })
  })

  describe('closeTab', () => {
    it('should return null when tab not found', () => {
      const store = useTabsStore()
      const result = store.closeTab('/non-existent')
      expect(result).toBeNull()
    })

    it('should remove tab and return next tab path', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard' })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.setActiveTab('/user')

      const result = store.closeTab('/user')

      expect(store.tabs).toHaveLength(1)
      expect(result).toBe('/dashboard')
      expect(store.activeTab).toBe('/dashboard')
    })

    it('should return dashboard when no tabs left', () => {
      const store = useTabsStore()
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })

      const result = store.closeTab('/user')

      expect(store.tabs).toHaveLength(0)
      expect(result).toBe('/dashboard')
    })

    it('should remove from cachedViews', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard' })

      store.closeTab('/dashboard')

      expect(store.cachedViews).not.toContain('Dashboard')
    })

    it('should not change activeTab when closing inactive tab', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard' })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.setActiveTab('/dashboard')

      const result = store.closeTab('/user')

      expect(result).toBeNull()
      expect(store.activeTab).toBe('/dashboard')
    })
  })

  describe('closeOtherTabs', () => {
    it('should keep only specified tab and affix tabs', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard', affix: true })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.addTab({ path: '/role', name: 'Role', title: '角色', fullPath: '/role' })

      store.closeOtherTabs('/user')

      expect(store.tabs).toHaveLength(2)
      expect(store.tabs.find(t => t.path === '/dashboard')).toBeDefined()
      expect(store.tabs.find(t => t.path === '/user')).toBeDefined()
    })

    it('should update activeTab', () => {
      const store = useTabsStore()
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.addTab({ path: '/role', name: 'Role', title: '角色', fullPath: '/role' })
      store.setActiveTab('/role')

      store.closeOtherTabs('/user')

      expect(store.activeTab).toBe('/user')
    })
  })

  describe('closeRightTabs', () => {
    it('should close tabs to the right', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard' })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.addTab({ path: '/role', name: 'Role', title: '角色', fullPath: '/role' })
      store.addTab({ path: '/menu', name: 'Menu', title: '菜单', fullPath: '/menu' })

      store.closeRightTabs('/user')

      expect(store.tabs).toHaveLength(2)
      expect(store.tabs.find(t => t.path === '/role')).toBeUndefined()
      expect(store.tabs.find(t => t.path === '/menu')).toBeUndefined()
    })

    it('should keep affix tabs even on right', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard' })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.addTab({ path: '/role', name: 'Role', title: '角色', fullPath: '/role', affix: true })

      store.closeRightTabs('/user')

      expect(store.tabs).toHaveLength(3)
    })

    it('should update activeTab if current tab is closed', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard' })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.addTab({ path: '/role', name: 'Role', title: '角色', fullPath: '/role' })
      store.setActiveTab('/role')

      store.closeRightTabs('/user')

      expect(store.activeTab).toBe('/user')
    })
  })

  describe('closeLeftTabs', () => {
    it('should close tabs to the left', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard' })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.addTab({ path: '/role', name: 'Role', title: '角色', fullPath: '/role' })

      store.closeLeftTabs('/role')

      // closeLeftTabs('/role') 会保留索引 >= 2 的标签页
      // 所以只保留 /role（index=2），/dashboard 和 /user 都会被关闭
      expect(store.tabs).toHaveLength(1)
      expect(store.tabs.find(t => t.path === '/dashboard')).toBeUndefined()
      expect(store.tabs.find(t => t.path === '/user')).toBeUndefined()
      expect(store.tabs.find(t => t.path === '/role')).toBeDefined()
    })

    it('should keep affix tabs even on left', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard', affix: true })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.addTab({ path: '/role', name: 'Role', title: '角色', fullPath: '/role' })

      store.closeLeftTabs('/role')

      // /dashboard 是 affix，会被保留
      // /user 会被关闭
      // /role 会被保留
      expect(store.tabs).toHaveLength(2)
      expect(store.tabs.find(t => t.path === '/dashboard')).toBeDefined()
      expect(store.tabs.find(t => t.path === '/user')).toBeUndefined()
      expect(store.tabs.find(t => t.path === '/role')).toBeDefined()
    })
  })

  describe('closeAllTabs', () => {
    it('should close all tabs except affix tabs', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard', affix: true })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })
      store.addTab({ path: '/role', name: 'Role', title: '角色', fullPath: '/role' })

      store.closeAllTabs()

      expect(store.tabs).toHaveLength(1)
      expect(store.tabs[0].path).toBe('/dashboard')
    })

    it('should update activeTab to first remaining tab', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard', affix: true })
      store.addTab({ path: '/user', name: 'User', title: '用户', fullPath: '/user' })

      store.closeAllTabs()

      expect(store.activeTab).toBe('/dashboard')
    })
  })

  describe('setActiveTab', () => {
    it('should update activeTab', () => {
      const store = useTabsStore()
      store.setActiveTab('/dashboard')

      expect(store.activeTab).toBe('/dashboard')
    })
  })

  describe('cachedViews management', () => {
    it('delCachedView should remove from cachedViews', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard' })

      store.delCachedView('Dashboard')

      expect(store.cachedViews).not.toContain('Dashboard')
    })

    it('addCachedView should add to cachedViews', () => {
      const store = useTabsStore()
      store.addCachedView('Dashboard')

      expect(store.cachedViews).toContain('Dashboard')
    })

    it('addCachedView should not add duplicate', () => {
      const store = useTabsStore()
      store.addCachedView('Dashboard')
      store.addCachedView('Dashboard')

      expect(store.cachedViews.filter(v => v === 'Dashboard')).toHaveLength(1)
    })
  })

  describe('getters', () => {
    it('getTabs should return tabs', () => {
      const store = useTabsStore()
      store.addTab({ path: '/dashboard', name: 'Dashboard', title: '仪表盘', fullPath: '/dashboard' })

      expect(store.getTabs).toEqual(store.tabs)
    })

    it('getActiveTab should return activeTab', () => {
      const store = useTabsStore()
      store.setActiveTab('/dashboard')

      expect(store.getActiveTab).toBe('/dashboard')
    })

    it('getCachedViews should return cachedViews', () => {
      const store = useTabsStore()
      store.addCachedView('Dashboard')

      expect(store.getCachedViews).toEqual(['Dashboard'])
    })
  })
})
