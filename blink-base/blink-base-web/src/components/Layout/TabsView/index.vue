<template>
  <div class="tabs-view">
    <div v-show="showScrollButtons" class="tabs-left">
      <el-button class="tabs-btn" @click="scrollLeft">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
    </div>

    <div ref="scrollContainer" class="tabs-scroll-container">
      <div ref="scrollWrapper" class="tabs-scroll-wrapper" :style="{ transform: `translateX(${left}px)` }">
        <div
          v-for="tab in tabs"
          :key="tab.path"
          class="tags-item"
          :class="{ active: isActive(tab) }"
          @click="handleTabClick(tab)"
          @click.middle="closeTab(tab)"
          @contextmenu.prevent="openContextMenu($event, tab)"
        >
          <span class="tags-title">{{ getTabTitle(tab) }}</span>
          <el-icon
            v-if="!tab.affix"
            class="tags-close"
            @click.stop="closeTab(tab)"
          >
            <Close />
          </el-icon>
        </div>
      </div>
    </div>

    <div class="tabs-right">
      <el-button v-show="showScrollButtons" class="tabs-btn" @click="scrollRight">
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <teleport to="body">
      <div
        v-show="showContextMenu && contextMenuVisible"
        class="tabs-context-menu"
        :style="contextMenuStyle"
        @click.stop
      >
        <div class="context-menu-item" @click="handleContextCommand('refresh')">
          <el-icon><Refresh /></el-icon>
          <span>{{ t('tabs.refresh') }}</span>
        </div>
        <div class="context-menu-item" @click="handleContextCommand('close')">
          <el-icon><Close /></el-icon>
          <span>{{ t('tabs.close') }}</span>
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click="handleContextCommand('closeOthers')">
          <el-icon><CircleClose /></el-icon>
          <span>{{ t('tabs.closeOthers') }}</span>
        </div>
        <div class="context-menu-item" @click="handleContextCommand('closeRight')">
          <el-icon><DArrowRight /></el-icon>
          <span>{{ t('tabs.closeRight') }}</span>
        </div>
        <div class="context-menu-item" @click="handleContextCommand('closeLeft')">
          <el-icon><DArrowLeft /></el-icon>
          <span>{{ t('tabs.closeLeft') }}</span>
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click="handleContextCommand('closeAll')">
          <el-icon><FolderDelete /></el-icon>
          <span>{{ t('tabs.closeAll') }}</span>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  ArrowRight,
  Refresh,
  Close,
  CircleClose,
  DArrowRight,
  DArrowLeft,
  FolderDelete,
} from '@element-plus/icons-vue'

export interface TabItem {
  path: string
  name: string
  title: string
  fullPath: string
  query?: Record<string, any>
  params?: Record<string, any>
  affix?: boolean
}

const LEGACY_TITLE_MAP: Record<string, string> = {
  '首页': 'menu.dashboard',
  '系统管理': 'menu.system',
  '用户管理': 'menu.user',
  '角色管理': 'menu.role',
  '菜单管理': 'menu.menu',
}

interface Props {
  tabs: TabItem[]
  cachedViews?: string[]
  activePath?: string
  routeIntegration?: boolean
  autoAddCurrentRoute?: boolean
  closeFallbackPath?: string
  refreshRedirectPrefix?: string
  useRefreshRedirect?: boolean
  showContextMenu?: boolean
  titleMap?: Record<string, string>
  titleResolver?: (tab: TabItem) => string
}

const props = withDefaults(defineProps<Props>(), {
  tabs: () => [],
  cachedViews: () => [],
  activePath: '',
  routeIntegration: true,
  autoAddCurrentRoute: true,
  closeFallbackPath: '/dashboard',
  refreshRedirectPrefix: '/redirect',
  useRefreshRedirect: true,
  showContextMenu: true,
  titleMap: () => ({ ...LEGACY_TITLE_MAP }),
  titleResolver: undefined,
})

const emit = defineEmits<{
  (e: 'add-tab', tab: TabItem): void
  (e: 'close-tab', path: string): string | null
  (e: 'close-other-tabs', path: string): void
  (e: 'close-right-tabs', path: string): void
  (e: 'close-left-tabs', path: string): void
  (e: 'close-all-tabs'): void
  (e: 'refresh-tab', name: string): void
  (e: 'del-cached-view', name: string): void
  (e: 'add-cached-view', name: string): void
  (e: 'tab-click', tab: TabItem): void
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const scrollContainer = ref<HTMLElement>()
const scrollWrapper = ref<HTMLElement>()
const left = ref(0)
const showScrollButtons = ref(false)
const contextMenuVisible = ref(false)
const contextMenuStyle = ref({ left: '0px', top: '0px' })
const selectedTab = ref<TabItem | null>(null)

const currentPath = computed(() => props.activePath || route.path)

const isActive = (tab: TabItem) => tab.path === currentPath.value

const checkScrollButtons = () => {
  nextTick(() => {
    const container = scrollContainer.value
    const wrapper = scrollWrapper.value
    showScrollButtons.value = !!container && !!wrapper && wrapper.scrollWidth > container.clientWidth
  })
}

const getTabTitle = (tab: TabItem): string => {
  if (props.titleResolver) {
    return props.titleResolver(tab)
  }

  const mappedKey = props.titleMap[tab.title]
  if (mappedKey) {
    return t(mappedKey)
  }

  const translated = t(tab.title)
  return translated !== tab.title ? translated : tab.title
}

const addCurrentRouteTab = () => {
  if (!props.routeIntegration || !props.autoAddCurrentRoute) {
    return
  }

  const { name, path, meta, fullPath, query, params } = route
  if (!name) {
    return
  }

  emit('add-tab', {
    name: String(name),
    path,
    title: (meta?.title as string) || 'no-name',
    fullPath,
    query: query as Record<string, any>,
    params: params as Record<string, any>,
    affix: meta?.affix as boolean,
  })
}

const moveToCurrentTab = () => {
  nextTick(() => {
    const tags = scrollWrapper.value?.querySelectorAll('.tags-item')
    if (!tags) {
      return
    }

    for (const tag of tags) {
      if ((tag as HTMLElement).classList.contains('active')) {
        moveToTarget(tag as HTMLElement)
        break
      }
    }
  })
}

const moveToTarget = (target: HTMLElement) => {
  const container = scrollContainer.value
  if (!container) {
    return
  }

  const containerWidth = container.offsetWidth
  const targetWidth = target.offsetWidth
  const targetLeft = target.offsetLeft

  if (targetLeft < -left.value) {
    left.value = -targetLeft + 10
  } else if (targetLeft + targetWidth > -left.value + containerWidth) {
    left.value = -(targetLeft + targetWidth - containerWidth + 10)
  }
}

const navigateToTab = (tab: TabItem) => {
  if (!props.routeIntegration) {
    return
  }

  router.push({
    path: tab.path,
    query: tab.query || {},
  })
}

const handleTabClick = (tab: TabItem) => {
  emit('tab-click', tab)
  if (tab.path !== currentPath.value) {
    navigateToTab(tab)
  }
}

const closeTab = (tab: TabItem) => {
  if (tab.affix) {
    return
  }

  emit('close-tab', tab.path)

  if (!props.routeIntegration || tab.path !== currentPath.value) {
    return
  }

  const currentIndex = props.tabs.findIndex(item => item.path === tab.path)
  const nextTab = props.tabs[currentIndex + 1] || props.tabs[currentIndex - 1]

  if (nextTab) {
    navigateToTab(nextTab)
  } else if (props.closeFallbackPath) {
    router.push(props.closeFallbackPath)
  }
}

const handleRefresh = (tab: TabItem) => {
  if (!tab.name) {
    return
  }

  emit('refresh-tab', tab.name)
  emit('del-cached-view', tab.name)

  nextTick(() => {
    emit('add-cached-view', tab.name!)

    if (!props.routeIntegration) {
      return
    }

    if (!props.useRefreshRedirect) {
      if (tab.path === currentPath.value) {
        router.replace({
          path: tab.path,
          query: { ...tab.query, _t: String(Date.now()) },
        })
      }
      return
    }

    if (tab.path === currentPath.value) {
      router.replace({
        path: `${props.refreshRedirectPrefix}${tab.path}`,
        query: tab.query,
      }).then(() => {
        router.replace({
          path: tab.path,
          query: { ...tab.query, _t: String(Date.now()) },
        })
      })
    } else {
      router.push({
        path: tab.path,
        query: { ...tab.query, _t: String(Date.now()) },
      })
    }
  })
}

const scrollLeft = () => {
  left.value = Math.min(0, left.value + 200)
}

const scrollRight = () => {
  const container = scrollContainer.value
  const wrapper = scrollWrapper.value
  if (!container || !wrapper) {
    return
  }

  const containerWidth = container.offsetWidth
  const wrapperWidth = wrapper.offsetWidth

  if (wrapperWidth - containerWidth + left.value > 0) {
    left.value = Math.max(left.value - 200, containerWidth - wrapperWidth)
  }
}

const openContextMenu = (event: MouseEvent, tab: TabItem) => {
  if (!props.showContextMenu) {
    return
  }

  event.preventDefault()
  event.stopPropagation()
  selectedTab.value = tab
  contextMenuStyle.value = {
    left: `${event.clientX}px`,
    top: `${event.clientY}px`,
  }
  contextMenuVisible.value = true
}

const closeContextMenu = () => {
  contextMenuVisible.value = false
  selectedTab.value = null
}

const handleContextCommand = (command: string) => {
  if (!selectedTab.value) {
    return
  }

  switch (command) {
    case 'refresh':
      handleRefresh(selectedTab.value)
      break
    case 'close':
      closeTab(selectedTab.value)
      break
    case 'closeOthers':
      emit('close-other-tabs', selectedTab.value.path)
      break
    case 'closeRight':
      emit('close-right-tabs', selectedTab.value.path)
      break
    case 'closeLeft':
      emit('close-left-tabs', selectedTab.value.path)
      break
    case 'closeAll':
      emit('close-all-tabs')
      break
  }

  closeContextMenu()
}

watch(
  () => route.path,
  () => {
    addCurrentRouteTab()
    moveToCurrentTab()
    checkScrollButtons()
  },
  { immediate: true }
)

watch(
  () => props.tabs.length,
  () => {
    checkScrollButtons()
  }
)

onMounted(() => {
  addCurrentRouteTab()
  checkScrollButtons()
  document.addEventListener('click', closeContextMenu)
  window.addEventListener('resize', checkScrollButtons)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
  window.removeEventListener('resize', checkScrollButtons)
})
</script>

<style scoped lang="scss">
.tabs-view {
  display: flex;
  align-items: center;
  height: 36px;
  background: var(--tabs-bg, var(--header-bg));
  backdrop-filter: blur(var(--glass-blur));
  border-bottom: 1px solid var(--tabs-border, var(--border-color-light));
  padding: 0 8px;
  transition: all var(--duration-normal) var(--ease-out-expo);
}

.tabs-left,
.tabs-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 2px;
}

.tabs-btn {
  padding: 6px 10px;
  border: none;
  background: transparent;
  color: var(--text-color-secondary);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out-expo);
  border-radius: 6px;

  &:hover {
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, transparent 100%);
    color: var(--primary-color);
    box-shadow: inset 0 0 10px rgba(59, 130, 246, 0.1);
  }

  .el-icon {
    font-size: 13px;
  }
}

.tabs-scroll-container {
  flex: 1;
  overflow: hidden;
  position: relative;
  margin: 0 4px;
}

.tabs-scroll-wrapper {
  display: flex;
  align-items: center;
  white-space: nowrap;
  transition: transform 0.3s;
}

.tags-item {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  margin-right: 4px;
  background: var(--bg-color);
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out-expo);
  font-size: 12px;
  color: var(--text-color-regular);
  user-select: none;
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
    background: var(--table-row-hover);
    color: var(--text-color-primary);
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  &.active {
    background: var(--gradient-cyber);
    color: #ffffff;
    box-shadow: var(--glow-primary), 0 2px 12px rgba(102, 126, 234, 0.4);
    border: 1px solid rgba(255, 255, 255, 0.2);

    .tags-close {
      color: rgba(255, 255, 255, 0.9);

      &:hover {
        background: rgba(255, 255, 255, 0.25);
        color: #ffffff;
      }
    }
  }
}

.tags-title {
  margin-right: 6px;
  line-height: 1;
}

.tags-close {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  color: var(--text-color-secondary);
  margin-left: 2px;

  &:hover {
    background: var(--table-row-hover);
    color: var(--text-color-primary);
  }

  .el-icon {
    font-size: 10px;
  }
}
</style>

<style lang="scss">
.tabs-context-menu {
  position: fixed;
  background: var(--card-bg);
  backdrop-filter: blur(var(--glass-blur));
  border-radius: 12px;
  box-shadow: var(--card-shadow), 0 10px 40px rgba(0, 0, 0, 0.2);
  padding: 8px 0;
  min-width: 150px;
  z-index: 9999;
  animation: contextMenuIn 0.2s var(--ease-out-back);
  border: 1px solid var(--border-color-light);

  @keyframes contextMenuIn {
    from {
      opacity: 0;
      transform: scale(0.9) translateY(-10px);
    }
    to {
      opacity: 1;
      transform: scale(1) translateY(0);
    }
  }

  .context-menu-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 16px;
    font-size: 13px;
    color: var(--text-color-regular);
    cursor: pointer;

    &:hover {
      color: var(--primary-color);
      background: linear-gradient(90deg, rgba(59, 130, 246, 0.1) 0%, transparent 100%);
    }
  }

  .context-menu-divider {
    height: 1px;
    margin: 6px 0;
    background: var(--border-color-light);
  }
}
</style>
