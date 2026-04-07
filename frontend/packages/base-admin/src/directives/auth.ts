/**
 * 权限指令 v-auth
 * 根据用户权限控制元素显示/隐藏
 *
 * 使用方式：
 * <el-button v-auth="'sysUser:add'">新增</el-button>
 * <el-button v-auth="['sysUser:add', 'sysUser:edit']">操作</el-button>
 */
import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 检查是否有权限
 * @param perms 权限标识或权限标识数组
 */
function checkPermission(perms: string | string[]): boolean {
  const userStore = useUserStore()

  // 超级管理员拥有所有权限
  if (userStore.isSuperAdmin) {
    return true
  }

  const permissions = userStore.permissions

  // 如果没有权限列表，返回false
  if (!permissions || permissions.length === 0) {
    return false
  }

  // 支持数组形式，任一权限匹配即通过
  if (Array.isArray(perms)) {
    return perms.some(perm => permissions.includes(perm))
  }

  return permissions.includes(perms)
}

/**
 * v-auth 指令
 * 无权限时移除元素
 */
export const auth: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const value = binding.value

    if (!value) {
      console.warn('[v-auth] 缺少权限标识参数')
      return
    }

    if (!checkPermission(value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * v-auth-disabled 指令
 * 无权限时禁用元素而不是移除
 */
export const authDisabled: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const value = binding.value

    if (!value) {
      console.warn('[v-auth-disabled] 缺少权限标识参数')
      return
    }

    if (!checkPermission(value)) {
      el.setAttribute('disabled', 'true')
      el.classList.add('is-disabled')
      el.style.pointerEvents = 'none'
      el.style.opacity = '0.5'
    }
  }
}

/**
 * 权限检查函数
 * 可在组件中直接调用
 */
export function hasAuth(perm: string | string[]): boolean {
  return checkPermission(perm)
}

export default {
  install(app: import('vue').App) {
    app.directive('auth', auth)
    app.directive('auth-disabled', authDisabled)
  }
}