<script setup lang="ts">
/**
 * 权限按钮组件
 * 根据用户权限自动显示/隐藏按钮
 *
 * 使用方式：
 * AuthButton perm="sysUser:add" type="primary" @click="handleAdd" 新增 /AuthButton
 * AuthButton :perms="['sysUser:add', 'sysUser:edit']" type="primary" 操作 /AuthButton
 * AuthButton :has-permission="() => userStore.permissions.includes('xxx')" type="primary" 操作 /AuthButton
 */
import { computed } from 'vue'

interface Props {
  /** 单个权限标识 */
  perm?: string
  /** 多个权限标识（任一匹配即显示） */
  perms?: string[]
  /** 无权限时禁用而非隐藏 */
  disabledOnNoPerm?: boolean
  /** 自定义权限检查函数，返回 true 表示有权限 */
  hasPermission?: () => boolean
}

const props = withDefaults(defineProps<Props>(), {
  perm: '',
  perms: () => [],
  disabledOnNoPerm: false,
  hasPermission: undefined
})

/**
 * 检查权限
 * 注意：如果没有提供 hasPermission 函数，且没有 perm/perms 参数，默认显示按钮
 */
const checkHasPermission = computed(() => {
  // 如果提供了自定义权限检查函数，直接使用
  if (props.hasPermission) {
    return props.hasPermission()
  }

  // 没有配置权限要求，默认显示
  const permList = [...(props.perm ? [props.perm] : []), ...props.perms]
  if (permList.length === 0) {
    return true
  }

  // 没有权限检查函数时，默认显示（应由调用方确保提供 hasPermission 或确保全局 userStore 已注册）
  return true
})

/**
 * 是否禁用
 */
const isDisabled = computed(() => {
  return props.disabledOnNoPerm && !checkHasPermission.value
})
</script>

<template>
  <!-- 无权限且不使用禁用模式，不渲染 -->
  <template v-if="checkHasPermission || disabledOnNoPerm">
    <el-button v-bind="$attrs" :disabled="isDisabled || $attrs.disabled">
      <slot />
    </el-button>
  </template>
</template>