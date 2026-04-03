/**
 * 权限相关的组合式函数
 */
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 权限检查组合式函数
 *
 * @example
 * const { hasPermission, hasAnyPermission, hasAllPermissions } = usePermission()
 *
 * // 检查单个权限
 * if (hasPermission('sysUser:add')) { ... }
 *
 * // 检查任一权限
 * if (hasAnyPermission(['sysUser:add', 'sysUser:edit'])) { ... }
 *
 * // 检查所有权限
 * if (hasAllPermissions(['sysUser:add', 'sysUser:edit'])) { ... }
 */
export function usePermission() {
  const userStore = useUserStore()

  /**
   * 检查是否有指定权限
   * @param perm 权限标识
   */
  const hasPermission = (perm: string): boolean => {
    // 超级管理员拥有所有权限
    if (userStore.isSuperAdmin) {
      return true
    }
    return userStore.permissions.includes(perm)
  }

  /**
   * 检查是否有任一权限
   * @param perms 权限标识数组
   */
  const hasAnyPermission = (perms: string[]): boolean => {
    // 超级管理员拥有所有权限
    if (userStore.isSuperAdmin) {
      return true
    }
    return perms.some(perm => userStore.permissions.includes(perm))
  }

  /**
   * 检查是否有所有权限
   * @param perms 权限标识数组
   */
  const hasAllPermissions = (perms: string[]): boolean => {
    // 超级管理员拥有所有权限
    if (userStore.isSuperAdmin) {
      return true
    }
    return perms.every(perm => userStore.permissions.includes(perm))
  }

  /**
   * 当前用户的所有权限
   */
  const permissions = computed(() => userStore.permissions)

  /**
   * 是否为超级管理员
   */
  const isSuperAdmin = computed(() => userStore.isSuperAdmin)

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    permissions,
    isSuperAdmin
  }
}

/**
 * 按钮权限常量
 * 按页面模块组织，与数据库 sys_permission.ac_identity 对应
 */
export const ButtonPerms = {
  // 用户管理
  User: {
    Add: 'sysUser:add',
    Edit: 'sysUser:update',
    Delete: 'sysUser:delete',
    Detail: 'sysUser:detail',
    Lock: 'sysUser:lock',
    AssignRole: 'sysUser:assignRole',
    ResetPwd: 'sysUser:resetPwd'
  },
  // 角色管理
  Role: {
    Add: 'sysRole:add',
    Edit: 'sysRole:update',
    Delete: 'sysRole:delete',
    Detail: 'sysRole:detail',
    AssignPerm: 'sysRole:assignPerm',
    AssignMenu: 'sysRole:assignMenu'
  },
  // 菜单管理
  Menu: {
    Add: 'sysMenu:add',
    Edit: 'sysMenu:update',
    Delete: 'sysMenu:delete'
  },
  // 组织管理
  Group: {
    Add: 'sysGroup:add',
    Edit: 'sysGroup:update',
    Delete: 'sysGroup:delete'
  },
  // 权限管理
  Permission: {
    Add: 'sysPerm:add',
    Edit: 'sysPerm:update',
    Delete: 'sysPerm:delete'
  },
  // 数据过滤规则
  DataFilter: {
    Add: 'dataFilter:add',
    Edit: 'dataFilter:update',
    Delete: 'dataFilter:delete',
    Detail: 'dataFilter:detail'
  },
  // 字典类型
  DictType: {
    Add: 'dictType:add',
    Edit: 'dictType:update',
    Delete: 'dictType:delete'
  },
  // 字典数据
  DictData: {
    Add: 'dictData:add',
    Edit: 'dictData:update',
    Delete: 'dictData:delete'
  },
  // 在线用户
  OnlineUser: {
    List: 'onlineUser:list',
    Kickout: 'onlineUser:kickout'
  },
  // 系统配置
  Config: {
    Update: 'sysConfig:update'
  }
} as const