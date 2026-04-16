<template>
  <!-- 使用重构后的 MainLayout 组件 -->
  <MainLayout
    :logo="systemConfigStore.systemLogo"
    :title="systemConfigStore.systemTitle"
    :menu-list="menuList"
    :user-info="userStore.userInfo"
    :current-theme="themeStore.theme"
    :current-language="currentLocale"
    :tabs="convertedTabs"
    :active-path="tabsStore.activeTab"
    :show-theme-settings="true"
    :enable-keep-alive="true"
    :cached-views="tabsStore.cachedViews"
    :avatar-resolver="avatarResolver"
    :min-tab-width="60"
    :labels="{
      fullscreen: { enter: t('header.fullscreen'), exit: t('header.exitFullscreen') },
      theme: { dark: t('header.darkMode'), light: t('header.lightMode') },
      user: {
        profile: t('header.profile'),
        themeSettings: t('header.themeSettings'),
        logout: t('header.logout'),
      },
    }"
    @theme-toggle="handleThemeToggle"
    @theme-change="handleThemeChange"
    @language-change="handleLanguageChange"
    @user-command="handleUserCommand"
    @tab-click="handleTabClick"
    @close-tab="handleCloseTab"
    @close-other-tabs="handleCloseOtherTabs"
    @close-right-tabs="handleCloseRightTabs"
    @close-left-tabs="handleCloseLeftTabs"
    @close-all-tabs="handleCloseAllTabs"
    @refresh-tab="handleRefreshTab"
    @max-tabs-reached="handleMaxTabsReached"
  >
    <!-- 自定义用户下拉菜单 -->
    <template #dropdown-menu>
      <el-dropdown-item command="profile">
        <el-icon><User /></el-icon>
        {{ t('header.profile') }}
      </el-dropdown-item>
      <el-dropdown-item command="themeSettings">
        <el-icon><Brush /></el-icon>
        {{ t('header.themeSettings') }}
      </el-dropdown-item>
      <el-dropdown-item divided command="toggleTheme">
        <el-icon v-if="themeStore.theme === 'dark'"><Sunny /></el-icon>
        <el-icon v-else><Moon /></el-icon>
        {{ themeStore.theme === 'dark' ? t('header.lightMode') : t('header.darkMode') }}
      </el-dropdown-item>
      <el-dropdown-item command="toggleLocale">
        <BlinkIcon icon="mdi:translate" size="14" />
        {{ currentLocale === 'zh_cn' ? t('header.enUS') : t('header.zhCN') }}
      </el-dropdown-item>
      <el-dropdown-item divided command="logout">
        <el-icon><SwitchButton /></el-icon>
        {{ t('header.logout') }}
      </el-dropdown-item>
    </template>
  </MainLayout>

  <!-- 主题设置抽屉 -->
  <el-drawer
    v-model="themeDrawerVisible"
    :title="t('header.themeSettings')"
    direction="rtl"
    size="450px"
    :append-to-body="true"
  >
    <ThemeEditor />
  </el-drawer>
</template>

<script setup lang="ts">
/**
 * 应用主布局页面
 *
 * 使用 MainLayout 组件，并连接业务 Store
 * 展示如何将通用组件与业务逻辑解耦
 */

import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, SwitchButton, Moon, Sunny, Brush } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { useThemeStore } from '@/stores/theme'
import { useSystemConfigStore } from '@/stores/systemConfig'
import { setLocale, getCurrentLocale } from '@/locales'
import { MainLayout } from '@blink/components'
import type { TabItem } from '@/stores/tabs'
import ThemeEditor from '@/views/settings/components/ThemeEditor.vue'
import { getLocalAvatarUrl } from '@/utils/avatar'

// ============================================
// Store 和路由
// ============================================

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const userStore = useUserStore()
const tabsStore = useTabsStore()
const themeStore = useThemeStore()
const systemConfigStore = useSystemConfigStore()

// ============================================
// 计算属性
// ============================================

const menuList = computed(() => userStore.menuTree || [])
const currentLocale = ref(getCurrentLocale())

// 转换 TabItem 格式
const convertedTabs = computed(() => {
  return tabsStore.tabs.map((tab) => ({
    path: tab.path,
    name: tab.name,
    title: tab.dynamicTitle || t(tab.title),
    fullPath: tab.fullPath,
    query: tab.query,
    params: tab.params,
    affix: tab.affix,
    closable: tab.closable,
    icon: tab.icon,
    status: tab.status,
    badge: tab.badge,
    tooltip: tab.tooltip,
    isNew: tab.isNew,
  }))
})

// 头像解析函数 - 将 avatar 名称转换为实际 URL
const avatarResolver = (user: { avatar?: string }) => {
  return getLocalAvatarUrl(user.avatar)
}

// ============================================
// 状态
// ============================================

const themeDrawerVisible = ref(false)

// ============================================
// 事件处理
// ============================================

/**
 * 主题切换
 */
const handleThemeToggle = () => {
  themeStore.toggleTheme()
}

/**
 * 主题变化
 */
const handleThemeChange = (theme: 'light' | 'dark') => {
  themeStore.setTheme(theme)
}

/**
 * 语言切换
 */
const handleLanguageChange = (lang: string) => {
  setLocale(lang)
  currentLocale.value = lang
}

/**
 * 用户命令处理
 */
const handleUserCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'themeSettings':
      themeDrawerVisible.value = true
      break
    case 'toggleTheme':
      themeStore.toggleTheme()
      break
    case 'toggleLocale':
      const newLocale = currentLocale.value === 'zh_cn' ? 'en_us' : 'zh_cn'
      handleLanguageChange(newLocale)
      break
    case 'logout':
      try {
        await ElMessageBox.confirm(t('header.logoutConfirm'), t('message.tips'), {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'warning',
        })
        await userStore.logout()
        tabsStore.closeAllTabs()
        ElMessage.success(t('header.logoutSuccess'))
        router.push('/login')
      } catch {
        // 用户取消
      }
      break
  }
}

// ============================================
// 标签页事件处理
// ============================================

const handleTabClick = (tab: TabItem) => {
  const targetPath = tab.fullPath || tab.path
  if (targetPath !== route.fullPath) {
    router.push(targetPath)
  }
}

const handleCloseTab = (tab: TabItem) => {
  const tabPath = tab.fullPath || tab.path
  const nextPath = tabsStore.closeTab(tabPath)
  if (nextPath) {
    router.push(nextPath)
  }
}

const handleCloseOtherTabs = (tab: TabItem) => {
  const tabPath = tab.fullPath || tab.path
  tabsStore.closeOtherTabs(tabPath)
}

const handleCloseRightTabs = (tab: TabItem) => {
  const tabPath = tab.fullPath || tab.path
  tabsStore.closeRightTabs(tabPath)
}

const handleCloseLeftTabs = (tab: TabItem) => {
  const tabPath = tab.fullPath || tab.path
  tabsStore.closeLeftTabs(tabPath)
}

const handleCloseAllTabs = () => {
  tabsStore.closeAllTabs()
  router.push('/dashboard')
}

const handleRefreshTab = (tab: TabItem) => {
  tabsStore.delCachedView(tab.name)
  if (tab.fullPath === route.fullPath) {
    router.replace({
      path: `/redirect${tab.path}`,
      query: tab.query,
    })
  }
}

/**
 * 达到最大标签数量时的处理
 */
const handleMaxTabsReached = (currentCount: number, maxTabs: number) => {
  ElMessage.warning(`标签数量已达上限 ${maxTabs}，请先关闭部分标签后再打开新页面`)
}

// ============================================
// 初始化
// ============================================

// 监听路由变化，自动添加标签
watch(
  () => route.fullPath,
  (fullPath) => {
    if (fullPath) {
      const { name, path, meta, query, params } = route
      if (name) {
        tabsStore.addTab({
          name: String(name),
          path,
          title: (meta?.title as string) || 'no-name',
          fullPath,
          query: query as Record<string, any>,
          params: params as Record<string, any>,
          affix: meta?.affix as boolean,
        })
      }
    }
  },
  { immediate: true }
)

onMounted(() => {
  // onMounted 中的逻辑已通过 watch immediate: true 处理
})
</script>

<style lang="scss">
/* 下拉菜单项样式 */
.el-dropdown-menu__item {
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

/* 主题设置抽屉样式 */
.el-drawer {
  border-radius: 12px 0 0 12px;

  .el-drawer__header {
    margin-bottom: 0;
    padding: 16px 20px;
    padding-right: 50px;
    border-bottom: 1px solid var(--border-color-light, #e5e7eb);
  }

  .el-drawer__close-btn {
    position: absolute;
    top: 16px;
    right: 16px;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--bg-color-page, #f5f7fa);
    border: 1px solid var(--border-color-light, #e5e7eb);
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    z-index: 10;

    &:hover {
      background: var(--primary-color, #3b82f6);
      border-color: var(--primary-color, #3b82f6);
      color: #fff;
      transform: scale(1.1);
    }

    .el-icon {
      font-size: 14px;
    }
  }
}
</style>