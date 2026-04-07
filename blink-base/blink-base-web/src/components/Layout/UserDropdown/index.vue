<template>
  <el-dropdown v-if="userInfo" class="user-dropdown" @command="handleCommand">
    <div class="header-item user-item">
      <slot name="trigger" :user-info="userInfo">
        <el-avatar :src="userInfo.avatar" :size="32">
          <el-icon><UserFilled /></el-icon>
        </el-avatar>
        <span class="user-name">{{ userInfo.username || userInfo.loginName }}</span>
        <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
      </slot>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <slot name="menu">
          <el-dropdown-item command="profile">
            <el-icon><User /></el-icon>{{ t('header.profile') }}
          </el-dropdown-item>
          <el-dropdown-item command="themeSettings" v-if="showThemeSettings">
            <el-icon><Brush /></el-icon>{{ t('header.themeSettings') }}
          </el-dropdown-item>
          <el-dropdown-item divided command="logout">
            <el-icon><SwitchButton /></el-icon>{{ t('header.logout') }}
          </el-dropdown-item>
        </slot>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  UserFilled,
  ArrowDown,
  User,
  SwitchButton,
  Brush,
} from '@element-plus/icons-vue'

export interface UserInfo {
  username?: string
  loginName?: string
  avatar?: string
  avatarStyle?: string
}

interface Props {
  userInfo?: UserInfo | null
  showThemeSettings?: boolean
}

withDefaults(defineProps<Props>(), {
  userInfo: null,
  showThemeSettings: false,
})

const emit = defineEmits<{
  (e: 'command', command: string): void
}>()

const { t } = useI18n()

const handleCommand = (command: string) => {
  emit('command', command)
}
</script>

<style scoped lang="scss">
.header-item {
  @apply inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg cursor-pointer transition-all text-[13px] whitespace-nowrap shrink-0;
  color: var(--text-color-regular);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: var(--gradient-primary);
    opacity: 0;
    transition: opacity var(--duration-normal) var(--ease-out-expo);
  }

  &:hover {
    @apply text-primary;

    &::before {
      opacity: 0.1;
    }

    box-shadow: inset 0 0 20px rgba(59, 130, 246, 0.1);
  }

  .el-icon {
    @apply text-lg shrink-0;
  }
}

.user-item {
  @apply flex items-center gap-2.5 px-3.5;

  .el-avatar {
    @apply cursor-pointer border-2 border-transparent transition-all w-9 h-9;

    &:hover {
      @apply border-primary;
      box-shadow: var(--glow-primary);
    }
  }

  .user-name {
    @apply text-[15px] font-medium max-w-[120px] overflow-hidden text-ellipsis whitespace-nowrap;
    color: var(--text-color-primary);
  }

  .dropdown-arrow {
    @apply text-[13px] transition-transform;
    color: var(--text-color-secondary);
  }

  &:hover .dropdown-arrow {
    @apply rotate-180;
  }
}

:deep(.el-dropdown-menu__item) {
  @apply flex items-center gap-2 px-4 py-2.5 text-sm;

  .el-icon {
    @apply text-base;
  }

  &:hover {
    @apply text-primary;
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.1) 0%, transparent 100%);
  }
}
</style>
