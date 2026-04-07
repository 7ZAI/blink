<template>
  <el-dropdown
    v-if="userInfo"
    class="user-dropdown"
    :trigger="trigger"
    :placement="placement"
    @command="handleCommand"
  >
    <!-- 触发器 Slot -->
    <slot name="trigger" :user-info="userInfo" :collapsed="collapsed">
      <!-- 默认触发器 -->
      <div class="user-dropdown-trigger" :class="{ 'collapsed': collapsed }">
        <!-- 头像 -->
        <slot name="avatar" :user-info="userInfo">
          <el-avatar
            :src="resolveAvatar"
            :size="collapsed ? 32 : avatarSize"
            class="user-avatar"
          >
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
        </slot>

        <!-- 用户名（非折叠模式显示） -->
        <template v-if="!collapsed">
          <span class="user-name">{{ displayName }}</span>
          <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
        </template>
      </div>
    </slot>

    <!-- 下拉菜单 -->
    <template #dropdown>
      <el-dropdown-menu>
        <slot name="menu" :user-info="userInfo" :handle-command="handleCommand">
          <!-- 默认菜单项（通过 menuItems 配置） -->
          <template v-for="item in computedMenuItems" :key="item.command">
            <el-dropdown-item
              v-if="item.visible !== false"
              :command="item.command"
              :divided="item.divided"
              :disabled="item.disabled"
            >
              <component :is="item.icon" v-if="item.icon" />
              <span>{{ resolveLabel(item) }}</span>
            </el-dropdown-item>
          </template>
        </slot>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
/**
 * UserDropdown 用户下拉菜单组件
 *
 * 特点：
 * - 完全可定制的触发器和菜单
 * - 支持配置化菜单项
 * - 解耦 i18n，支持外部传入文本或自定义渲染
 * - 支持折叠模式（只显示头像）
 * - 支持自定义头像渲染
 */

import { computed, type Component } from 'vue'
import { UserFilled, ArrowDown } from '@element-plus/icons-vue'

// ============================================
// 类型定义
// ============================================

/**
 * 用户信息接口
 */
export interface UserInfo {
  /** 用户名 */
  username?: string
  /** 登录名 */
  loginName?: string
  /** 头像 URL */
  avatar?: string
  /** 头像样式（用于生成头像） */
  avatarStyle?: string
  /** 用户 ID */
  userId?: number | string
  /** 其他扩展字段 */
  [key: string]: any
}

/**
 * 菜单项接口
 */
export interface MenuItem {
  /** 命令标识 */
  command: string
  /** 显示文本或 i18n key */
  label: string
  /** 图标组件 */
  icon?: Component
  /** 是否显示分割线 */
  divided?: boolean
  /** 是否禁用 */
  disabled?: boolean
  /** 是否可见 */
  visible?: boolean
}

/**
 * Props 接口
 */
export interface Props {
  /** 用户信息 */
  userInfo?: UserInfo | null
  /** 是否显示主题设置菜单项 */
  showThemeSettings?: boolean
  /** 是否折叠模式（只显示头像） */
  collapsed?: boolean
  /** 头像大小 */
  avatarSize?: number
  /** 下拉触发方式 */
  trigger?: 'hover' | 'click' | 'contextmenu'
  /** 下拉菜单位置 */
  placement?: 'top' | 'top-start' | 'top-end' | 'bottom' | 'bottom-start' | 'bottom-end'
  /** 菜单项配置（传入后覆盖默认菜单） */
  menuItems?: MenuItem[]
  /** 文本映射（用于解耦 i18n） */
  labels?: Record<string, string>
  /** 头像解析函数 */
  avatarResolver?: (user: UserInfo) => string
  /** 名称解析函数 */
  nameResolver?: (user: UserInfo) => string
}

// ============================================
// Props 定义
// ============================================

const props = withDefaults(defineProps<Props>(), {
  userInfo: null,
  showThemeSettings: false,
  collapsed: false,
  avatarSize: 36,
  trigger: 'click',
  placement: 'bottom-end',
  menuItems: undefined,
  labels: () => ({
    profile: '个人中心',
    themeSettings: '主题设置',
    logout: '退出登录',
  }),
  avatarResolver: undefined,
  nameResolver: undefined,
})

// ============================================
// Emits 定义
// ============================================

const emit = defineEmits<{
  (e: 'command', command: string): void
  (e: 'avatar-click', user: UserInfo): void
}>()

// ============================================
// 计算属性
// ============================================

/**
 * 显示名称
 */
const displayName = computed(() => {
  if (props.nameResolver) {
    return props.nameResolver(props.userInfo!)
  }
  return props.userInfo?.username || props.userInfo?.loginName || 'User'
})

/**
 * 解析头像 URL
 */
const resolveAvatar = computed(() => {
  if (props.avatarResolver && props.userInfo) {
    return props.avatarResolver(props.userInfo)
  }
  return props.userInfo?.avatar
})

/**
 * 默认菜单项
 */
const defaultMenuItems = computed<MenuItem[]>(() => [
  {
    command: 'profile',
    label: props.labels.profile || '个人中心',
    icon: undefined, // User 图标需要外部传入或使用 slot
  },
  ...(props.showThemeSettings
    ? [
        {
          command: 'themeSettings',
          label: props.labels.themeSettings || '主题设置',
          icon: undefined,
        },
      ]
    : []),
  {
    command: 'logout',
    label: props.labels.logout || '退出登录',
    divided: true,
    icon: undefined,
  },
])

/**
 * 计算后的菜单项
 */
const computedMenuItems = computed(() => {
  return props.menuItems || defaultMenuItems.value
})

// ============================================
// 方法
// ============================================

/**
 * 解析标签文本
 */
const resolveLabel = (item: MenuItem): string => {
  // 优先使用 labels 映射
  const mappedLabel = props.labels?.[item.label]
  if (mappedLabel) {
    return mappedLabel
  }
  return item.label
}

/**
 * 处理命令
 */
const handleCommand = (command: string) => {
  emit('command', command)
}

// ============================================
// 暴露
// ============================================

defineExpose({
  handleCommand,
  displayName,
  resolveAvatar,
})
</script>

<style scoped lang="scss">
/* === 触发器 === */
.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-color-regular, #606266);

  &:hover {
    background: rgba(59, 130, 246, 0.1);
    color: var(--primary-color, #3b82f6);
  }

  &.collapsed {
    padding: 6px;
    gap: 0;
  }
}

/* === 头像 === */
.user-avatar {
  border: 2px solid transparent;
  transition: all 0.2s ease;
  cursor: pointer;

  &:hover {
    border-color: var(--primary-color, #3b82f6);
    box-shadow: 0 0 10px rgba(59, 130, 246, 0.3);
  }
}

/* === 用户名 === */
.user-name {
  font-size: 14px;
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-color-primary, #303133);
}

/* === 下拉箭头 === */
.dropdown-arrow {
  font-size: 12px;
  color: var(--text-color-secondary, #909399);
  transition: transform 0.2s ease;
}

.user-dropdown-trigger:hover .dropdown-arrow {
  transform: rotate(180deg);
}

/* === 下拉菜单项 === */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;

  .el-icon {
    font-size: 16px;
  }

  &:hover {
    color: var(--primary-color, #3b82f6);
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.1) 0%, transparent 100%);
  }
}
</style>