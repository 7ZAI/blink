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
      :enable-keep-alive="true"
      :cached-views="tabsStore.getCachedViews"
      @theme-change="handleThemeChange"
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
          :avatar-resolver="avatarResolver"
          @command="handleUserCommand"
        >
          <template #menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              {{ t('header.profile') }}
            </el-dropdown-item>
            <el-dropdown-item command="themeSettings">
              <el-icon><Setting /></el-icon>
              {{ t('header.themeSettings') }}
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              {{ t('header.logout') }}
            </el-dropdown-item>
          </template>
        </UserDropdown>
      </template>

      <template #header-right-before>
        <NotificationCenter />
      </template>

      <template #tabs>
        <TabsView
          :tabs="convertedTabs"
          :active-path="tabsStore.getActiveTabPath || undefined"
          :show-context-menu="true"
          :max-tabs="20"
          :overflow-warning-threshold="15"
          :min-tab-width="80"
          :max-tab-width="160"
          @tab-click="handleTabClick"
          @close-tab="handleCloseTab"
          @close-other-tabs="handleCloseOtherTabs"
          @close-right-tabs="handleCloseRightTabs"
          @close-left-tabs="handleCloseLeftTabs"
          @close-all-tabs="handleCloseAllTabs"
          @refresh-tab="handleRefreshTab"
          @max-tabs-reached="handleMaxTabsReached"
        />
      </template>
    </MainLayout>

    <!-- 主题设置抽屉 -->
    <el-drawer
      v-model="themeSettingsVisible"
      :title="t('header.themeSettings')"
      direction="rtl"
      size="400px"
      :append-to-body="true"
      :lock-scroll="false"
    >
      <ThemeSettings
        :model-value="themeConfig"
        :custom-presets="themeStore.customPresets"
        @update:model-value="handleThemeConfigUpdate"
        @preset-change="handlePresetChange"
        @color-change="handleColorChange"
        @font-change="handleFontChange"
        @animation-change="handleAnimationChange"
        @preset-save="handlePresetSave"
        @preset-delete="handlePresetDelete"
      />
    </el-drawer>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElConfigProvider, ElMessage, ElMessageBox } from 'element-plus'
import { User, SwitchButton, Setting } from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { Breadcrumb, MainLayout, TabsView, UserDropdown, ThemeSettings } from '@blink/components'
import type { MenuItem, ThemeColors } from '@blink/components'
import type { TabItem } from '@/stores/tabs'
import NotificationCenter from '@/components/NotificationCenter/index.vue'
import { useAppStore } from '@/stores/app'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { useNotificationStore } from '@/stores/notification'
import { useSystemConfigStore } from '@/stores/systemConfig'
import type { MenuVO } from '@/api/auth'
import { getLocalAvatarUrl } from '@/utils/avatar'

defineOptions({ name: 'MainLayout' })

const { t, locale } = useI18n()
const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const themeStore = useThemeStore()
const userStore = useUserStore()
const tabsStore = useTabsStore()
const notificationStore = useNotificationStore()
const systemConfigStore = useSystemConfigStore()

// 监听路由变化，自动添加标签并更新激活状态
watch(
  () => route.fullPath,
  (fullPath) => {
    if (fullPath) {
      // 自动添加当前路由为标签
      const { name, path, meta, query } = route
      if (name) {
        tabsStore.addTab({
          name: String(name),
          path,
          title: (meta?.title as string) || 'no-name',
          fullPath,
          query: query as Record<string, string>,
          affix: meta?.affix as boolean,
        })
      }
      // 更新激活标签
      if (tabsStore.getActiveTabPath !== fullPath) {
        tabsStore.setActiveTab(fullPath)
      }
    }
  },
  { immediate: true }
)

// Element Plus 语言配置
const elementLocale = computed(() => {
  return appStore.language === 'zh-cn' ? zhCn : en
})

// 主题设置抽屉状态
const themeSettingsVisible = ref(false)

// 主题配置
const themeConfig = computed(() => ({
  presetId: themeStore.currentPresetId || undefined,
  colors: { ...themeStore.colors },
  font: { ...themeStore.font },
  animationsEnabled: themeStore.animationsEnabled,
  system: {},
}))

// SSE 连接初始化 - MainLayout 作为 SSE 连接的唯一管理者
  // 遵循消息总线模式：顶层统一管理连接，其他页面只监听数据
  onMounted(() => {
    // Initialize SSE connection after user is logged in
    notificationStore.connectSse()
  })

  onUnmounted(() => {
    // 用户退出时断开 SSE 连接
    notificationStore.disconnectSse()
  })

// Logo SVG - 从系统配置动态获取
const logoSvg = computed(() => systemConfigStore.systemLogo)

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

// 头像解析函数
const avatarResolver = (user: { avatar?: string }) => {
  return getLocalAvatarUrl(user.avatar)
}

// 转换 TabItem 格式（支持新属性）
const convertedTabs = computed(() => {
  return tabsStore.getTabs.map((tab) => ({
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

// 语言切换
const handleLanguageChange = (lang: string) => {
  const normalizedLang = lang.replace('_', '-')
  appStore.setLanguage(normalizedLang)
  locale.value = normalizedLang
}

// 主题切换
const handleThemeChange = (theme: 'light' | 'dark') => {
  themeStore.setTheme(theme)
}

// 用户菜单命令处理
const handleUserCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'themeSettings':
      themeSettingsVisible.value = true
      break
    case 'logout':
      try {
        await ElMessageBox.confirm(t('header.logoutConfirm'), t('header.logoutDialogTitle'), {
          confirmButtonText: t('header.confirm'),
          cancelButtonText: t('header.cancel'),
          type: 'warning',
        })
        await userStore.logout()
        ElMessage.success(t('header.logoutSuccess'))
        router.push('/login')
      } catch {
        // 用户取消
      }
      break
  }
}

// 主题配置更新
const handleThemeConfigUpdate = (config: any) => {
  // 配置更新时同步到 store
}

// 预设主题切换
const handlePresetChange = (presetId: string) => {
  themeStore.applyPreset(presetId)
}

// 颜色变更
const handleColorChange = (colors: ThemeColors) => {
  themeStore.setColors(colors)
}

// 字体变更
const handleFontChange = (font: {
  family: string
  baseSize: number
  largeSize: number
  smallSize: number
}) => {
  themeStore.setFont(font)
}

// 动画开关变更
const handleAnimationChange = (enabled: boolean) => {
  themeStore.setAnimationsEnabled(enabled)
}

// 保存预设
const handlePresetSave = (preset: any) => {
  themeStore.saveAsPreset(preset.name)
  ElMessage.success(t('settings.presetSaved'))
}

// 删除预设
const handlePresetDelete = (presetId: string) => {
  themeStore.deletePreset(presetId)
  ElMessage.success(t('settings.presetDeleted'))
}

// 标签页操作
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
  // 刷新当前标签：清除缓存并重新加载
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
  ElMessage.warning(t('tabs.maxTabsReached', { max: maxTabs }))
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
</style>
