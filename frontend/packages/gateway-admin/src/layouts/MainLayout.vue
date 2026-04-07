<template>
  <el-config-provider :locale="elementLocale">
    <MainLayout
      :logo="logoSvg"
      :title="'Gateway Admin'"
      :menu-list="menuList"
      :user-info="userInfoComputed"
      :current-theme="themeStore.theme"
      :current-language="appStore.language.replace('-', '_')"
      :show-breadcrumb="false"
      :show-theme-toggle="true"
      :show-language-switch="true"
      @theme-toggle="themeStore.toggleTheme"
      @language-change="handleLanguageChange"
    >
      <template #header-left>
        <Breadcrumb
          :home-path="'/dashboard'"
          :home-title-key="'menu.dashboard'"
          :exclude-route-names="['Layout', 'MainLayout']"
        />
      </template>

      <template #header-user-menu>
        <UserDropdown
          :user-info="userInfoComputed"
          :show-theme-settings="true"
          @command="handleUserCommand"
        >
          <template #menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>{{ t('header.profile') }}
            </el-dropdown-item>
            <el-dropdown-item command="themeSettings">
              <el-icon><Setting /></el-icon>{{ t('header.themeSettings') }}
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>{{ t('header.logout') }}
            </el-dropdown-item>
          </template>
        </UserDropdown>
      </template>

      <template #header-right-before>
        <NotificationCenter />
      </template>

      <template #tabs>
        <TabsView
          :tabs="tabsStore.getTabs.map(convertTabItem)"
          :cached-views="tabsStore.getCachedViews"
          :active-path="tabsStore.getActiveTabPath || undefined"
          :close-fallback-path="'/dashboard'"
          :refresh-redirect-prefix="'/redirect'"
          :show-context-menu="true"
          @add-tab="handleAddTab"
          @close-tab="handleCloseTab"
          @close-other-tabs="handleCloseOtherTabs"
          @close-right-tabs="handleCloseRightTabs"
          @close-left-tabs="handleCloseLeftTabs"
          @close-all-tabs="handleCloseAllTabs"
          @del-cached-view="handleDelCachedView"
          @add-cached-view="handleAddCachedView"
        />
      </template>
    </MainLayout>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElConfigProvider, ElMessage, ElMessageBox } from 'element-plus'
import { User, SwitchButton, Setting } from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { Breadcrumb, MainLayout, TabsView, UserDropdown } from '@blink/components'
import NotificationCenter from '@/components/NotificationCenter/index.vue'
import type { MenuItem, TabItem } from '@blink/components'
import { useAppStore } from '@/stores/app'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { useNotificationStore } from '@/stores/notification'
import type { MenuVO } from '@/api/auth'

defineOptions({ name: 'MainLayout' })

const { t, locale } = useI18n()
const router = useRouter()
const appStore = useAppStore()
const themeStore = useThemeStore()
const userStore = useUserStore()
const tabsStore = useTabsStore()
const notificationStore = useNotificationStore()

// Element Plus 语言配置
const elementLocale = computed(() => {
  return appStore.language === 'zh-cn' ? zhCn : en
})

// SSE 连接初始化
onMounted(() => {
  // Initialize SSE connection after user is logged in
  notificationStore.connectSse()
})

onUnmounted(() => {
  notificationStore.disconnectSse()
})

// Logo SVG
const logoSvg = `<svg viewBox="0 0 24 24" fill="currentColor">
  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
</svg>`

// 转换 MenuVO 到 MenuItem
const convertMenuVO = (menu: MenuVO): MenuItem => {
  const item: MenuItem = {
    menuId: menu.menuId,
    menuName: menu.menuName,
    menuEnName: menu.menuEnName,
    icon: menu.icon,
    url: menu.url,
    status: menu.status,
    children: menu.children?.map(convertMenuVO) || [],
  }
  return item
}

// 转换菜单列表（使用 menuTree 已构建好的树形结构）
const menuList = computed(() => {
  return userStore.menuTree.map(convertMenuVO)
})

// 用户信息计算属性
const userInfoComputed = computed(() => {
  if (!userStore.userInfo) return null
  return {
    username: userStore.userInfo.username,
    loginName: userStore.userInfo.loginName,
    avatar: userStore.userInfo.avatar,
    avatarStyle: userStore.userInfo.avatarStyle,
  }
})

// 转换 TabItem 格式
const convertTabItem = (tab: any): TabItem => {
  return {
    path: tab.path,
    name: tab.name,
    title: tab.title,
    fullPath: tab.fullPath,
    query: tab.query,
    params: tab.params,
    affix: tab.affix,
  }
}

// 语言切换
const handleLanguageChange = (lang: string) => {
  const normalizedLang = lang.replace('_', '-')
  appStore.setLanguage(normalizedLang)
  locale.value = normalizedLang
}

// 用户菜单命令处理
const handleUserCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/settings')
      break
    case 'themeSettings':
      router.push('/settings')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm(
          t('header.logoutConfirm'),
          t('header.logoutDialogTitle'),
          {
            confirmButtonText: t('header.confirm'),
            cancelButtonText: t('header.cancel'),
            type: 'warning'
          }
        )
        await userStore.logout()
        ElMessage.success(t('header.logoutSuccess'))
        router.push('/login')
      } catch {
        // 用户取消
      }
      break
  }
}

// 标签页操作
const handleAddTab = (tab: TabItem) => {
  tabsStore.addTab({
    name: tab.name,
    path: tab.path,
    title: tab.title,
    fullPath: tab.fullPath,
    query: tab.query as Record<string, string>,
    affix: tab.affix,
  })
}

const handleCloseTab = (path: string) => {
  tabsStore.closeTab(path)
}

const handleCloseOtherTabs = (path: string) => {
  tabsStore.closeOtherTabs(path)
}

const handleCloseRightTabs = (path: string) => {
  tabsStore.closeRightTabs(path)
}

const handleCloseLeftTabs = (path: string) => {
  tabsStore.closeLeftTabs(path)
}

const handleCloseAllTabs = () => {
  tabsStore.closeAllTabs()
}

const handleDelCachedView = (name: string) => {
  tabsStore.delCachedView(name)
}

const handleAddCachedView = (name: string) => {
  tabsStore.addCachedView(name)
}
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
}

/* 覆盖侧边栏 Logo 样式 */
.sidebar .logo-icon {
  margin-right: 12px !important;
}

.sidebar .logo-text {
  margin-left: 0 !important;
}

/* 消息通知按钮样式 - 与 Header 其他按钮一致 */
:deep(.notification-trigger) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 1.5;
  padding: 0 12px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: var(--el-text-color-regular);
  position: relative;
  overflow: hidden;
}

:deep(.notification-trigger:hover) {
  color: var(--el-color-primary);
  background-color: rgba(59, 130, 246, 0.1);
}

:deep(.notification-trigger .el-icon) {
  font-size: 18px;
}
</style>
