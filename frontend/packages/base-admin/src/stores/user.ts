import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, Menu, LoginRsp, CaptchaVO } from '@/types'
import { login as loginApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '@/api/auth'

function buildMenuTree(menus: Menu[]): Menu[] {
  const menuMap = new Map<number, Menu>()
  const rootMenus: Menu[] = []

  menus.forEach((menu) => {
    menuMap.set(menu.menuId, { ...menu, children: [] })
  })

  menus.forEach((menu) => {
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

  const sortMenus = (menus: Menu[]): Menu[] => {
    return menus
      .sort((a, b) => (a.orderNumber || 0) - (b.orderNumber || 0))
      .map((menu) => {
        if (menu.children && menu.children.length > 0) {
          menu.children = sortMenus(menu.children)
        }
        return menu
      })
  }

  return sortMenus(rootMenus)
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const menus = ref<Menu[]>([])
  const functionMenu = ref<Menu[]>([])
  const permissions = ref<string[]>([])
  const roles = ref<string[]>([])
  const captchaVerification = ref<string>('')
  const needResetPassword = ref(false)

  const menuTree = computed(() => buildMenuTree(menus.value))

  const isLoggedIn = computed(() => !!token.value)
  const hasPermission = (permission: string) => permissions.value.includes(permission)
  const hasRole = (role: string) => roles.value.includes(role)

  // 判断当前用户是否为超级管理员
  const isSuperAdmin = computed(() => {
    return (
      roles.value.includes('superAdmin') ||
      userInfo.value?.superFlag === 1 ||
      String(userInfo.value?.superFlag) === '1'
    )
  })

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (data: LoginRsp) => {
    userInfo.value = data.userInfo
    menus.value = data.menus
    functionMenu.value = data.functionMenu
    permissions.value = data.permissions
    roles.value = data.roles
  }

  // 设置验证码校验结果
  const setCaptchaVerification = (verification: string) => {
    captchaVerification.value = verification
  }

  // 清除重置密码标记
  const clearNeedResetPassword = () => {
    needResetPassword.value = false
  }

  const login = async (loginName: string, password: string) => {
    // 构建登录请求参数，包含验证码信息
    const loginReq = {
      loginName,
      password,
    } as { loginName: string; password: string; captchaVO?: CaptchaVO }

    // 如果有验证码校验结果，添加到请求中
    if (captchaVerification.value) {
      loginReq.captchaVO = {
        captchaVerification: captchaVerification.value,
      }
    }

    const res = await loginApi(loginReq)

    // request.ts 已经返回了 res.body，所以这里直接使用 res
    if (res) {
      setToken(res.token)
      setUserInfo(res)
      // 登录成功后清空验证码
      captchaVerification.value = ''
      // 存储是否需要重置密码标记
      needResetPassword.value = res.needResetPassword || false
    }

    return res
  }

  const logout = async () => {
    try {
      if (token.value && userInfo.value?.userId) {
        await logoutApi({
          token: token.value,
          userId: String(userInfo.value.userId),
        })
      }
    } finally {
      clearUserInfo()
    }
  }

  const clearUserInfo = () => {
    token.value = ''
    userInfo.value = null
    menus.value = []
    functionMenu.value = []
    permissions.value = []
    roles.value = []
    captchaVerification.value = ''
    localStorage.removeItem('token')
  }

  const restoreUserInfo = async () => {
    if (!token.value) {
      return false
    }

    try {
      const res = await getUserInfoApi()
      if (res) {
        setUserInfo(res)
        return true
      }
    } catch (error) {
      clearUserInfo()
    }

    return false
  }

  const fetchUserInfo = async () => {
    if (!token.value) {
      return false
    }

    try {
      const res = await getUserInfoApi()
      if (res) {
        setUserInfo(res)
        return true
      }
    } catch (error) {
      console.error('Failed to fetch user info:', error)
    }

    return false
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
    hasPermission,
    hasRole,
    isSuperAdmin,
    login,
    logout,
    clearUserInfo,
    setUserInfo,
    setCaptchaVerification,
    captchaVerification,
    restoreUserInfo,
    fetchUserInfo,
    needResetPassword,
    clearNeedResetPassword,
  }
})
