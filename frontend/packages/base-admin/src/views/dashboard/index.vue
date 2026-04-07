<template>
  <div class="dashboard">
    <!-- 首次登录重置密码弹窗 -->
    <ResetPasswordDialog v-if="userStore.needResetPassword" @success="handleResetPasswordSuccess" />

    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card">
          <div class="welcome-content">
            <div class="welcome-text">
              <h2>{{ t('dashboard.welcome') }}，{{ userStore.userInfo?.username }}</h2>
              <p>{{ t('dashboard.todayIs') }} {{ today }}，{{ t('dashboard.wishWork') }}</p>
            </div>
            <el-icon :size="64" class="welcome-icon"><Platform /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="6">
        <el-card class="stat-card" v-loading="loading">
          <div class="stat-icon blue">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData.totalUsers }}</div>
            <div class="stat-label">{{ t('dashboard.totalUsers') }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" v-loading="loading">
          <div class="stat-icon green">
            <el-icon :size="32"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData.onlineUsers }}</div>
            <div class="stat-label">{{ t('dashboard.onlineUsers') }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" v-loading="loading">
          <div class="stat-icon orange">
            <el-icon :size="32"><Collection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData.totalRoles }}</div>
            <div class="stat-label">{{ t('dashboard.totalRoles') }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" v-loading="loading">
          <div class="stat-icon purple">
            <el-icon :size="32"><MenuIcon /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData.totalMenus }}</div>
            <div class="stat-label">{{ t('dashboard.totalMenus') }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>{{ t('dashboard.systemInfo') }}</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="t('dashboard.systemName')">Blink Base</el-descriptions-item>
            <el-descriptions-item :label="t('dashboard.systemVersion')">v1.0.0</el-descriptions-item>
            <el-descriptions-item :label="t('dashboard.currentUser')">{{ userStore.userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item :label="t('dashboard.userRoles')">{{ userStore.roles?.join(', ') || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="t('dashboard.userGroup')">{{ userStore.userInfo?.groupName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card v-if="visibleQuickLinks.length > 0">
          <template #header>
            <span>{{ t('dashboard.quickEntry') }}</span>
          </template>
          <div class="quick-links">
            <el-button
              v-for="link in visibleQuickLinks"
              :key="link.path"
              :type="link.type as any"
              plain
              @click="$router.push(link.path)"
            >
              <el-icon>
                <User v-if="link.icon === 'User'" />
                <Collection v-else-if="link.icon === 'Collection'" />
                <MenuIcon v-else-if="link.icon === 'Menu'" />
                <OfficeBuilding v-else />
              </el-icon>{{ t(link.labelKey) }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Platform, User, UserFilled, Collection, Menu as MenuIcon, OfficeBuilding } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getDashboardData, type DashboardData } from '@/api/dashboard'
import ResetPasswordDialog from '@/views/login/components/ResetPasswordDialog.vue'
import type { Menu } from '@/types'

defineOptions({
  name: 'Dashboard',
})

const { t, locale } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const dashboardData = ref<DashboardData>({
  totalUsers: 0,
  onlineUsers: 0,
  totalRoles: 0,
  totalMenus: 0,
})

/**
 * 检查用户是否有指定菜单的访问权限
 * @param path 菜单路径
 */
const hasMenuAccess = (path: string): boolean => {
  if (userStore.isSuperAdmin) {
    return true
  }

  const checkMenus = (menus: Menu[], targetPath: string): boolean => {
    return menus.some(menu => {
      if (menu.url === targetPath) {
        return true
      }
      if (menu.children && menu.children.length > 0) {
        return checkMenus(menu.children, targetPath)
      }
      return false
    })
  }

  return checkMenus(userStore.menus, path)
}

// 快捷入口配置
const quickLinks = [
  { path: '/system/user', icon: 'User', type: 'primary', labelKey: 'menu.user' },
  { path: '/system/role', icon: 'Collection', type: 'success', labelKey: 'menu.role' },
  { path: '/system/menu', icon: 'Menu', type: 'warning', labelKey: 'menu.menu' },
  { path: '/system/group', icon: 'OfficeBuilding', type: 'info', labelKey: 'menu.group' },
]

// 过滤出用户有权限访问的快捷入口
const visibleQuickLinks = computed(() => {
  return quickLinks.filter(link => hasMenuAccess(link.path))
})

const today = computed(() => {
  const date = new Date()
  const localeStr = locale.value === 'zh-cn' ? 'zh-CN' : 'en-US'
  return date.toLocaleDateString(localeStr, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  })
})

const fetchDashboardData = async () => {
  loading.value = true
  try {
    const res = await getDashboardData()
    dashboardData.value = res
  } finally {
    loading.value = false
  }
}

// 重置密码成功后清除标记
const handleResetPasswordSuccess = () => {
  userStore.clearNeedResetPassword()
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 0;
}

.mt-20 {
  margin-top: 20px;
}

.welcome-card {
  background: var(--gradient-cyber);
  border: none;
  position: relative;
  overflow: hidden;

  /* 添加动态光效背景 */
  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
    animation: rotate 10s linear infinite;
  }

  :deep(.el-card__body) {
    padding: 30px;
    position: relative;
    z-index: 1;
  }
}

.welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;

  h2 {
    margin: 0 0 8px;
    font-size: 24px;
  }

  p {
    margin: 0;
    opacity: 0.9;
  }
}

.welcome-icon {
  color: rgba(255, 255, 255, 0.9);
}

.stat-card {
  transition: all var(--duration-normal) var(--ease-out-expo);

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--card-hover-glow);
  }

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    padding: 20px;
  }
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: #fff;
  position: relative;
  overflow: hidden;

  /* 确保图标在深色模式下清晰可见 */
  :deep(.el-icon),
  :deep(svg) {
    color: #fff !important;
    fill: #fff !important;
  }

  /* 添加发光效果 */
  &::before {
    content: '';
    position: absolute;
    inset: -2px;
    border-radius: inherit;
    background: inherit;
    filter: blur(10px);
    opacity: 0.5;
    z-index: -1;
  }

  &.blue {
    background: var(--gradient-primary);
    box-shadow: var(--glow-primary);
  }

  &.green {
    background: var(--gradient-success);
    box-shadow: var(--glow-success);
  }

  &.orange {
    background: var(--gradient-warning);
    box-shadow: var(--glow-warning);
  }

  &.purple {
    background: var(--gradient-info);
    box-shadow: var(--glow-primary);
  }
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-color-primary);
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-color-secondary);
}

.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  .el-button {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
</style>