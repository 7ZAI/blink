import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key]
    }),
    clear: vi.fn(() => {
      store = {}
    }),
  }
})()

Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// Mock auth API
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getUserInfo: vi.fn(),
}))

describe('User Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorageMock.clear()
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have empty user info by default', () => {
      const store = useUserStore()
      expect(store.userInfo).toBeNull()
      expect(store.token).toBe('')
      expect(store.isLoggedIn).toBe(false)
    })
  })

  describe('isLoggedIn', () => {
    it('should return false when token is empty', () => {
      const store = useUserStore()
      expect(store.isLoggedIn).toBe(false)
    })
  })

  describe('hasPermission', () => {
    it('should return false when permissions are empty', () => {
      const store = useUserStore()
      expect(store.hasPermission('user:read')).toBe(false)
    })
  })

  describe('hasRole', () => {
    it('should return false when roles are empty', () => {
      const store = useUserStore()
      expect(store.hasRole('admin')).toBe(false)
    })
  })

  describe('setUserInfo', () => {
    it('should set user info and roles correctly', () => {
      const store = useUserStore()
      const mockLoginRsp = {
        token: 'test-token',
        userInfo: {
          userId: 1,
          loginName: 'admin',
          username: 'Administrator',
          avatar: '',
          avatarStyle: '',
          sex: 1,
          phone: '13800138000',
          email: 'admin@example.com',
          groupName: 'Admin',
          lastLoginTime: '2024-01-01 00:00:00',
          locked: 0,
          superFlag: 1,
          pswRetry: 0,
          createBy: '',
          updateBy: '',
          createTime: '',
          updateTime: '',
          lockTime: '',
          remark: '',
        },
        roles: ['admin', 'superAdmin'],
        menus: [],
        functionMenu: [],
        permissions: ['user:read', 'user:write'],
      }

      store.setUserInfo(mockLoginRsp)

      expect(store.userInfo).toEqual(mockLoginRsp.userInfo)
      expect(store.roles).toEqual(['admin', 'superAdmin'])
      expect(store.permissions).toEqual(['user:read', 'user:write'])
    })
  })

  describe('clearUserInfo', () => {
    it('should clear all user data', () => {
      const store = useUserStore()

      // First set some data
      store.setUserInfo({
        token: 'test-token',
        userInfo: {
          userId: 1,
          loginName: 'admin',
          username: 'Admin',
          avatar: '',
          avatarStyle: '',
          sex: 1,
          phone: '',
          email: '',
          groupName: '',
          lastLoginTime: '',
          locked: 0,
          superFlag: 1,
          pswRetry: 0,
          createBy: '',
          updateBy: '',
          createTime: '',
          updateTime: '',
          lockTime: '',
          remark: '',
        },
        roles: ['admin'],
        menus: [],
        functionMenu: [],
        permissions: ['user:read'],
      })

      // Then clear
      store.clearUserInfo()

      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
      expect(store.roles).toEqual([])
      expect(store.permissions).toEqual([])
      expect(store.isLoggedIn).toBe(false)
    })
  })

  describe('isSuperAdmin', () => {
    it('should return true when superFlag is 1', () => {
      const store = useUserStore()
      store.setUserInfo({
        token: 'test-token',
        userInfo: {
          userId: 1,
          loginName: 'admin',
          username: 'Admin',
          avatar: '',
          avatarStyle: '',
          sex: 1,
          phone: '',
          email: '',
          groupName: '',
          lastLoginTime: '',
          locked: 0,
          superFlag: 1,
          pswRetry: 0,
          createBy: '',
          updateBy: '',
          createTime: '',
          updateTime: '',
          lockTime: '',
          remark: '',
        },
        roles: [],
        menus: [],
        functionMenu: [],
        permissions: [],
      })

      expect(store.isSuperAdmin).toBe(true)
    })

    it('should return true when roles contains superAdmin', () => {
      const store = useUserStore()
      store.setUserInfo({
        token: 'test-token',
        userInfo: {
          userId: 2,
          loginName: 'user',
          username: 'User',
          avatar: '',
          avatarStyle: '',
          sex: 1,
          phone: '',
          email: '',
          groupName: '',
          lastLoginTime: '',
          locked: 0,
          superFlag: 0,
          pswRetry: 0,
          createBy: '',
          updateBy: '',
          createTime: '',
          updateTime: '',
          lockTime: '',
          remark: '',
        },
        roles: ['superAdmin'],
        menus: [],
        functionMenu: [],
        permissions: [],
      })

      expect(store.isSuperAdmin).toBe(true)
    })

    it('should return false when not super admin', () => {
      const store = useUserStore()
      store.setUserInfo({
        token: 'test-token',
        userInfo: {
          userId: 2,
          loginName: 'user',
          username: 'User',
          avatar: '',
          avatarStyle: '',
          sex: 1,
          phone: '',
          email: '',
          groupName: '',
          lastLoginTime: '',
          locked: 0,
          superFlag: 0,
          pswRetry: 0,
          createBy: '',
          updateBy: '',
          createTime: '',
          updateTime: '',
          lockTime: '',
          remark: '',
        },
        roles: ['user'],
        menus: [],
        functionMenu: [],
        permissions: [],
      })

      expect(store.isSuperAdmin).toBe(false)
    })
  })

  describe('hasPermission and hasRole after setUserInfo', () => {
    it('should correctly check permissions and roles', () => {
      const store = useUserStore()
      store.setUserInfo({
        token: 'test-token',
        userInfo: {
          userId: 1,
          loginName: 'admin',
          username: 'Admin',
          avatar: '',
          avatarStyle: '',
          sex: 1,
          phone: '',
          email: '',
          groupName: '',
          lastLoginTime: '',
          locked: 0,
          superFlag: 0,
          pswRetry: 0,
          createBy: '',
          updateBy: '',
          createTime: '',
          updateTime: '',
          lockTime: '',
          remark: '',
        },
        roles: ['admin'],
        menus: [],
        functionMenu: [],
        permissions: ['user:read', 'user:write'],
      })

      expect(store.hasPermission('user:read')).toBe(true)
      expect(store.hasPermission('admin:delete')).toBe(false)
      expect(store.hasRole('admin')).toBe(true)
      expect(store.hasRole('superAdmin')).toBe(false)
    })
  })
})
