<script setup lang="ts">
/**
 * 权限按钮组件
 * 根据用户权限自动显示/隐藏按钮
 *
 * 使用方式：
 * AuthButton perm="sysUser:add" type="primary" @click="handleAdd" 新增 /AuthButton
 * AuthButton :perms="['sysUser:add', 'sysUser:edit']" type="primary" 操作 /AuthButton
 */
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

interface Props {
  /** 单个权限标识 */
  perm?: string
  /** 多个权限标识（任一匹配即显示） */
  perms?: string[]
  /** 无权限时禁用而非隐藏 */
  disabledOnNoPerm?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  perm: '',
  perms: () => [],
  disabledOnNoPerm: false,
})

const userStore = useUserStore()

/**
 * 检查权限
 */
const hasPermission = computed(() => {
  // 超级管理员拥有所有权限
  if (userStore.isSuperAdmin) {
    return true
  }

  const permissions = userStore.permissions

  if (!permissions || permissions.length === 0) {
    return false
  }

  // 合并单个和多个权限参数
  const permList = [...(props.perm ? [props.perm] : []), ...props.perms]

  if (permList.length === 0) {
    return true // 没有配置权限要求，默认显示
  }

  return permList.some((perm) => permissions.includes(perm))
})

/**
 * 是否禁用
 */
const isDisabled = computed(() => {
  return props.disabledOnNoPerm && !hasPermission.value
})
</script>

<template>
  <!-- 无权限且不使用禁用模式，不渲染 -->
  <template v-if="hasPermission || disabledOnNoPerm">
    <el-button v-bind="$attrs" :disabled="isDisabled || $attrs.disabled">
      <slot />
    </el-button>
  </template>
</template>
