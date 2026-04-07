import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  login as loginApi,
  logout as logoutApi,
  getUserInfo,
  type LoginRsp,
  type MenuVO,
  type UserInfoVO
} from '@/api/auth'
import { useTabsStore } from './tabs'

/**
 * 构建菜单树形结构
 */
function buildMenuTree(menus: MenuVO[]): MenuVO[] {
  const menuMap = new Map<number, MenuVO>()
  const rootMenus: MenuVO[] = []

  menus.forEach(menu => {
    menuMap.set(menu.menuId, { ...menu, children: [] })
  })

  menus.forEach(menu => {
    const currentMenu = menuMap.get(menu.menuId)!
    if (!menu.parentId || menu.parentId === 0) {
      rootMenus.push(currentMenu)
    } else {
      const parentMenu = menuMap.get(menu.parentId)
      if (parentMenu) {
        if (!parentMenu.children) {
          parentMenu.children = []
        }
        parentMenu.children.push(currentMenu)
      }
    }
  })

  const sortMenus = (menus: MenuVO[]): MenuVO[] => {
    return menus
      .sort((a, b) => (a.orderNumber || 0) - (b.orderNumber || 0))
      .map(menu => {
        if (menu.children && menu.children.length > 0) {
          menu.children = sortMenus(menu.children)
        }
        return menu
      })
  }

  return sortMenus(rootMenus)
}

/**
 * 用户状态管理Store
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || sessionStorage.getItem('token') || '')
  const userInfo = ref<UserInfoVO | null>(null)
  const menus = ref<MenuVO[]>([])
  const functionMenu = ref<MenuVO[]>([])
  const permissions = ref<string[]>([])
  const roles = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const menuTree = computed(() => buildMenuTree(menus.value))
  const isSuperAdmin = computed(() => {
    return roles.value.includes('superAdmin') ||
           userInfo.value?.superFlag === 1 ||
           String(userInfo.value?.superFlag) === '1'
  })

  /**
   * 用户登录
   */
  const login = async (loginName: string, password: string, rememberMe: boolean): Promise<LoginRsp> => {
    const rsp = await loginApi({ loginName, password, rememberMe })

    // 先存储 token，确保后续请求能携带
    token.value = rsp.token
    if (rememberMe) {
      localStorage.setItem('token', rsp.token)
    } else {
      sessionStorage.setItem('token', rsp.token)
    }

    // 设置用户信息和菜单
    userInfo.value = rsp.userInfo
    menus.value = rsp.menus || []
    functionMenu.value = rsp.functionMenu || []
    permissions.value = rsp.permissions || []
    roles.value = rsp.roles || []

    return rsp
  }

  /**
   * 用户登出
   */
  const logout = async () => {
    try {
      await logoutApi()
    } catch (error) {
      console.error('Logout error:', error)
    }
    token.value = ''
    userInfo.value = null
    menus.value = []
    functionMenu.value = []
    permissions.value = []
    roles.value = []
    localStorage.removeItem('token')
    sessionStorage.removeItem('token')

    // 清除便签缓存
    const tabsStore = useTabsStore()
    tabsStore.closeAllTabs()
  }

  /**
   * 刷新用户信息（页面刷新后恢复数据）
   */
  const fetchUserInfo = async () => {
    const rsp = await getUserInfo()
    userInfo.value = rsp.userInfo
    menus.value = rsp.menus || []
    functionMenu.value = rsp.functionMenu || []
    permissions.value = rsp.permissions || []
    roles.value = rsp.roles || []
    return rsp
  }

  return {
    token,
    userInfo,
    menus,
    menuTree,
    functionMenu,
    permissions,
    roles,
    isLoggedIn,
    isSuperAdmin,
    login,
    logout,
    fetchUserInfo
  }
})