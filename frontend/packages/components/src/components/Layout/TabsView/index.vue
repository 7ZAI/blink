<template>
  <div
    class="tabs-view"
    role="tablist"
    aria-label="页面标签栏"
    @keydown="handleKeydown"
  >
    <!-- 左侧滚动按钮 -->
    <div
      v-show="showScrollButtons"
      class="scroll-btn scroll-left"
      :class="{ disabled: !canScrollLeft }"
      role="button"
      aria-label="向左滚动"
      :aria-disabled="!canScrollLeft"
      tabindex="0"
      @click="scrollLeft"
      @keydown.enter="scrollLeft"
      @keydown.space.prevent="scrollLeft"
    >
      <el-icon><ArrowLeft /></el-icon>
    </div>

    <!-- 标签容器 -->
    <div ref="tabsContainerRef" class="tabs-wrapper" :style="tabWidthStyle">
      <div
        v-for="tab in tabs"
        :key="tab.fullPath || tab.path"
        ref="tabRefs"
        class="tags-item"
        :class="{
          active: isActive(tab),
          'is-loading': tab.status === 'loading',
          'is-error': tab.status === 'error',
          'is-modified': tab.status === 'modified',
          'is-new': tab.isNew,
        }"
        role="tab"
        :aria-selected="isActive(tab)"
        :aria-controls="`tabpanel-${tab.name}`"
        :aria-disabled="tab.affix ? 'false' : undefined"
        :tabindex="isActive(tab) ? 0 : -1"
        :title="tab.tooltip || tab.title"
        @click="handleTabClick(tab)"
        @click.middle="handleCloseTab(tab)"
        @contextmenu.prevent="handleContextMenu($event, tab)"
      >
        <!-- 图标 -->
        <el-icon v-if="tab.icon" class="tags-icon" :size="14">
          <component :is="tab.icon" />
        </el-icon>

        <!-- 状态指示器 -->
        <span v-if="tab.status === 'loading'" class="status-indicator loading">
          <span class="spinner"></span>
        </span>
        <span v-else-if="tab.status === 'error'" class="status-indicator error"></span>
        <span v-else-if="tab.status === 'modified'" class="status-indicator modified"></span>

        <!-- 标题 -->
        <span class="tags-title">{{ tab.title }}</span>

        <!-- 徽标 -->
        <span v-if="tab.badge" class="tags-badge">{{ tab.badge }}</span>

        <!-- 关闭按钮 -->
        <el-icon
          v-if="showCloseButton(tab)"
          class="tags-close"
          :aria-label="`关闭 ${tab.title}`"
          role="button"
          tabindex="0"
          @click.stop="handleCloseTab(tab)"
          @keydown.stop.enter="handleCloseTab(tab)"
          @keydown.stop.space="handleCloseTab(tab)"
        >
          <Close />
        </el-icon>
      </div>
    </div>

    <!-- 右侧滚动按钮 -->
    <div
      v-show="showScrollButtons"
      class="scroll-btn scroll-right"
      :class="{ disabled: !canScrollRight }"
      role="button"
      aria-label="向右滚动"
      :aria-disabled="!canScrollRight"
      tabindex="0"
      @click="scrollRight"
      @keydown.enter="scrollRight"
      @keydown.space.prevent="scrollRight"
    >
      <el-icon><ArrowRight /></el-icon>
    </div>

    <!-- 右键菜单 -->
    <teleport to="body">
      <div
        v-show="contextMenuVisible"
        ref="contextMenuRef"
        class="tabs-context-menu"
        :style="contextMenuStyle"
        role="menu"
        aria-label="标签操作菜单"
        @click.stop
        @keydown.esc="closeContextMenu"
      >
        <div
          v-for="item in visibleContextMenuItems"
          :key="item.command"
          class="context-menu-item"
          :class="{ disabled: item.disabled }"
          role="menuitem"
          :aria-disabled="item.disabled"
          tabindex="0"
          @click="handleContextCommand(item.command)"
          @keydown.enter="handleContextCommand(item.command)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </teleport>

    <!-- 标签过多警告 -->
    <teleport to="body">
      <el-tooltip
        v-if="showOverflowWarning"
        :content="`标签数量已达 ${tabs.length}，建议关闭部分标签以保持良好体验`"
        placement="top"
        effect="warning"
      >
        <div class="overflow-warning">
          <el-icon><Warning /></el-icon>
        </div>
      </el-tooltip>
    </teleport>
  </div>
</template>

<script lang="ts">
/**
 * 标签页项接口
 */
export interface TabItem {
  path: string
  name: string
  title: string
  fullPath: string
  query?: Record<string, any>
  params?: Record<string, any>
  affix?: boolean
  closable?: boolean
  icon?: string
  status?: 'normal' | 'loading' | 'error' | 'modified'
  badge?: string | number
  tooltip?: string
  isNew?: boolean
}

/**
 * 右键菜单项接口
 */
export interface ContextMenuItem {
  command: string
  label: string
  icon: string
  disabled?: boolean
  visible?: boolean
}

/**
 * TabsView 组件 Props 接口
 */
export interface Props {
  tabs: TabItem[]
  activePath?: string
  showContextMenu?: boolean
  contextMenuItems?: ContextMenuItem[]
  /** 最大标签数量，默认 20，超过后触发 max-tabs-reached 事件 */
  maxTabs?: number
  overflowWarningThreshold?: number
  /** 标签最小宽度（像素），默认 80 */
  minTabWidth?: number
  /** 标签最大宽度（像素），默认 160 */
  maxTabWidth?: number
}

/**
 * 默认右键菜单项
 */
const DEFAULT_CONTEXT_MENU_ITEMS: ContextMenuItem[] = [
  { command: 'refresh', label: '刷新', icon: 'Refresh' },
  { command: 'close', label: '关闭', icon: 'Close' },
  { command: 'closeOthers', label: '关闭其他', icon: 'CircleClose' },
  { command: 'closeRight', label: '关闭右侧', icon: 'DArrowRight' },
  { command: 'closeLeft', label: '关闭左侧', icon: 'DArrowLeft' },
  { command: 'closeAll', label: '关闭所有', icon: 'FolderDelete' },
]
</script>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  Close,
  Refresh,
  CircleClose,
  DArrowRight,
  DArrowLeft,
  FolderDelete,
  Warning,
  ArrowLeft,
  ArrowRight,
} from '@element-plus/icons-vue'

const props = withDefaults(defineProps<Props>(), {
  tabs: () => [],
  activePath: '',
  showContextMenu: true,
  contextMenuItems: () => [...DEFAULT_CONTEXT_MENU_ITEMS],
  maxTabs: 20,
  overflowWarningThreshold: 15,
  minTabWidth: 80,
  maxTabWidth: 160,
})

const emit = defineEmits([
  'tab-click',
  'close-tab',
  'close-other-tabs',
  'close-right-tabs',
  'close-left-tabs',
  'close-all-tabs',
  'refresh-tab',
  'max-tabs-reached',
])

const route = useRoute()

// refs
const tabRefs = ref<HTMLElement[]>([])
const tabsContainerRef = ref<HTMLElement>()
const contextMenuRef = ref<HTMLElement>()
const contextMenuVisible = ref(false)
const contextMenuStyle = ref({ left: '0px', top: '0px' })
const selectedTab = ref<TabItem | null>(null)
const focusedIndex = ref(0)

// 滚动相关状态
const showScrollButtons = ref(false)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)

// computed
const currentPath = computed(() => props.activePath || route.fullPath)

const isActive = (tab: TabItem) => (tab.fullPath || tab.path) === currentPath.value

const showCloseButton = (tab: TabItem) => {
  if (tab.affix) return false
  if (tab.closable === false) return false
  return true
}

const showOverflowWarning = computed(() => {
  return props.tabs.length >= props.overflowWarningThreshold
})

const tabWidthStyle = computed(() => {
  const style: Record<string, string> = {
    '--tab-min-width': `${props.minTabWidth}px`,
    '--tab-max-width': `${props.maxTabWidth}px`,
  }
  // 当 min 和 max 相等时，设置固定宽度
  if (props.minTabWidth === props.maxTabWidth) {
    style['--tab-fixed-width'] = `${props.minTabWidth}px`
  }
  return style
})

const visibleContextMenuItems = computed(() => {
  const items = props.contextMenuItems
  const tab = selectedTab.value

  return items.map((item) => {
    let disabled = false

    // 根据当前选中标签状态判断菜单项是否可用
    if (!tab) {
      disabled = true
    } else if (item.command === 'close' && tab.affix) {
      disabled = true
    } else if (item.command === 'close' && tab.closable === false) {
      disabled = true
    } else if (item.command === 'closeOthers' && props.tabs.filter((t) => !t.affix && t !== tab).length === 0) {
      disabled = true
    } else if (item.command === 'closeRight' && props.tabs.indexOf(tab) === props.tabs.length - 1) {
      disabled = true
    } else if (item.command === 'closeLeft' && props.tabs.indexOf(tab) === 0) {
      disabled = true
    } else if (item.command === 'closeAll' && props.tabs.every((t) => t.affix)) {
      disabled = true
    }

    return { ...item, disabled }
  })
})

// methods
const handleTabClick = (tab: TabItem) => {
  emit('tab-click', tab)
}

const handleCloseTab = (tab: TabItem) => {
  if (tab.affix || tab.closable === false) return
  emit('close-tab', tab)
}

const handleContextMenu = (event: MouseEvent, tab: TabItem) => {
  if (!props.showContextMenu) return

  event.preventDefault()
  event.stopPropagation()

  selectedTab.value = tab

  // 边界检测：确保菜单在可视区域内
  const menuWidth = 150
  const menuHeight = 200

  let left = event.clientX
  let top = event.clientY

  // 检测右边界
  if (left + menuWidth > window.innerWidth) {
    left = window.innerWidth - menuWidth - 10
  }

  // 检测底部边界
  if (top + menuHeight > window.innerHeight) {
    top = window.innerHeight - menuHeight - 10
  }

  // 确保最小值
  left = Math.max(10, left)
  top = Math.max(10, top)

  contextMenuStyle.value = {
    left: `${left}px`,
    top: `${top}px`,
  }

  contextMenuVisible.value = true
}

const closeContextMenu = () => {
  contextMenuVisible.value = false
  selectedTab.value = null
}

const handleContextCommand = (command: string) => {
  if (!selectedTab.value) return

  const tab = selectedTab.value

  switch (command) {
    case 'refresh':
      emit('refresh-tab', tab)
      break
    case 'close':
      handleCloseTab(tab)
      break
    case 'closeOthers':
      emit('close-other-tabs', tab)
      break
    case 'closeRight':
      emit('close-right-tabs', tab)
      break
    case 'closeLeft':
      emit('close-left-tabs', tab)
      break
    case 'closeAll':
      emit('close-all-tabs')
      break
  }

  closeContextMenu()
}

// 键盘导航
const handleKeydown = (event: KeyboardEvent) => {
  const { key } = event
  const tabs = props.tabs
  const currentIndex = tabs.findIndex((t) => isActive(t))

  switch (key) {
    case 'ArrowLeft':
      event.preventDefault()
      if (currentIndex > 0) {
        focusTab(currentIndex - 1)
        emit('tab-click', tabs[currentIndex - 1])
      }
      break

    case 'ArrowRight':
      event.preventDefault()
      if (currentIndex < tabs.length - 1) {
        focusTab(currentIndex + 1)
        emit('tab-click', tabs[currentIndex + 1])
      }
      break

    case 'Home':
      event.preventDefault()
      if (tabs.length > 0) {
        focusTab(0)
        emit('tab-click', tabs[0])
      }
      break

    case 'End':
      event.preventDefault()
      if (tabs.length > 0) {
        focusTab(tabs.length - 1)
        emit('tab-click', tabs[tabs.length - 1])
      }
      break

    case 'Delete':
    case 'Backspace':
      event.preventDefault()
      const currentTab = tabs[currentIndex]
      if (currentTab && showCloseButton(currentTab)) {
        handleCloseTab(currentTab)
      }
      break

    case 'Escape':
      if (contextMenuVisible.value) {
        closeContextMenu()
      }
      break
  }
}

const focusTab = (index: number) => {
  focusedIndex.value = index
  nextTick(() => {
    const tabEl = tabRefs.value[index]
    if (tabEl) {
      tabEl.focus()
    }
  })
}

/**
 * 检查是否达到最大标签数量
 * @returns true 表示可以继续添加，false 表示已达到上限
 */
const checkMaxTabs = (): boolean => {
  if (props.tabs.length >= props.maxTabs) {
    emit('max-tabs-reached', props.tabs.length, props.maxTabs)
    return false
  }
  return true
}

/**
 * 暴露给父组件调用，用于在添加标签前检查
 */
defineExpose({
  checkMaxTabs,
})

/**
 * 更新滚动按钮状态
 */
const updateScrollState = () => {
  const container = tabsContainerRef.value
  if (!container) {
    showScrollButtons.value = false
    return
  }

  const { scrollWidth, clientWidth } = container
  const hasOverflow = scrollWidth > clientWidth

  showScrollButtons.value = hasOverflow

  // 基于当前激活标签位置判断是否可以切换
  const currentIndex = props.tabs.findIndex((t) => isActive(t))
  canScrollLeft.value = currentIndex > 0
  canScrollRight.value = currentIndex < props.tabs.length - 1
}

/**
 * 切换到左边一个标签（同时滚动）
 */
const scrollLeft = () => {
  const currentIndex = props.tabs.findIndex((t) => isActive(t))
  if (currentIndex <= 0) return

  // 切换到前一个标签
  const prevTab = props.tabs[currentIndex - 1]
  emit('tab-click', prevTab)
}

/**
 * 切换到右边一个标签（同时滚动）
 */
const scrollRight = () => {
  const currentIndex = props.tabs.findIndex((t) => isActive(t))
  if (currentIndex >= props.tabs.length - 1) return

  // 切换到后一个标签
  const nextTab = props.tabs[currentIndex + 1]
  emit('tab-click', nextTab)
}

/**
 * 滚动到激活的标签
 */
const scrollToActiveTab = () => {
  const container = tabsContainerRef.value
  if (!container) return

  const activeIndex = props.tabs.findIndex((t) => isActive(t))
  if (activeIndex === -1) return

  const tabEl = tabRefs.value[activeIndex]
  if (!tabEl) return

  const containerRect = container.getBoundingClientRect()
  const tabRect = tabEl.getBoundingClientRect()

  // 计算滚动位置
  const scrollLeft = container.scrollLeft
  const containerWidth = container.clientWidth
  const tabLeft = tabEl.offsetLeft
  const tabWidth = tabEl.offsetWidth

  // 如果标签在可视区域左侧
  if (tabLeft < scrollLeft) {
    container.scrollTo({ left: tabLeft - 10, behavior: 'smooth' })
  }
  // 如果标签在可视区域右侧
  else if (tabLeft + tabWidth > scrollLeft + containerWidth) {
    container.scrollTo({ left: tabLeft + tabWidth - containerWidth + 10, behavior: 'smooth' })
  }
}

// 生命周期
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  document.addEventListener('click', closeContextMenu)
  window.addEventListener('resize', closeContextMenu)
  window.addEventListener('resize', updateScrollState)

  // 初始化滚动状态
  nextTick(() => {
    updateScrollState()
    scrollToActiveTab()
  })

  // 监听容器滚动事件
  if (tabsContainerRef.value) {
    tabsContainerRef.value.addEventListener('scroll', updateScrollState)
  }

  // 使用 ResizeObserver 监听容器大小变化
  if (tabsContainerRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      updateScrollState()
    })
    resizeObserver.observe(tabsContainerRef.value)
  }
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
  window.removeEventListener('resize', closeContextMenu)
  window.removeEventListener('resize', updateScrollState)

  if (tabsContainerRef.value) {
    tabsContainerRef.value.removeEventListener('scroll', updateScrollState)
  }

  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})

// watch: 自动聚焦当前激活标签
watch(
  () => props.activePath,
  () => {
    const index = props.tabs.findIndex((t) => isActive(t))
    if (index !== -1 && index !== focusedIndex.value) {
      focusedIndex.value = index
    }
    // 滚动到激活标签
    nextTick(() => {
      scrollToActiveTab()
    })
  },
  { immediate: true }
)

// watch: 监听标签数量变化，更新滚动状态
watch(
  () => props.tabs.length,
  () => {
    nextTick(() => {
      updateScrollState()
    })
  }
)

// watch: 监听标签数组变化（深度监听，处理标签属性变化）
watch(
  () => props.tabs,
  () => {
    nextTick(() => {
      updateScrollState()
    })
  },
  { deep: true }
)
</script>

<style scoped lang="scss">
.tabs-view {
  display: flex;
  align-items: center;
  height: 36px;
  background: var(--tabs-bg, var(--header-bg));
  border-bottom: 1px solid var(--tabs-border, var(--border-color-light));
  padding: 0 8px;
  transition: background var(--duration-normal) var(--ease-out-expo);
  overflow: hidden;
  position: relative;
}

// 滚动按钮
.scroll-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-color-secondary);
  transition: all var(--duration-normal) var(--ease-out-expo);
  z-index: 1;

  &:hover:not(.disabled) {
    background: var(--table-row-hover);
    color: var(--text-color-primary);
  }

  &:focus-visible {
    outline: 2px solid var(--primary-color);
    outline-offset: 2px;
  }

  &.disabled {
    color: var(--text-color-disabled);
    cursor: not-allowed;
  }
}

.tabs-wrapper {
  display: flex;
  flex: 1;
  min-width: 0;
  gap: 4px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none; // Firefox
  -ms-overflow-style: none; // IE/Edge

  &::-webkit-scrollbar {
    display: none; // Chrome/Safari
  }
}

.tags-item {
  display: inline-flex;
  align-items: center;
  height: 28px;
  min-width: var(--tab-min-width, 80px);
  max-width: var(--tab-max-width, 160px);
  flex: 0 0 auto;
  // 当 min-width 和 max-width 相等时，固定标签宽度
  width: var(--tab-fixed-width, auto);
  padding: 0 12px;
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
  outline: none;

  &:focus-visible {
    outline: 2px solid var(--primary-color);
    outline-offset: 2px;
  }

  &:hover {
    background: var(--table-row-hover);
    color: var(--text-color-primary);
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  &.active {
    background: var(--primary-color);
    color: #ffffff;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);

    .tags-close {
      color: rgba(255, 255, 255, 0.85);

      &:hover {
        background: rgba(255, 255, 255, 0.2);
        color: #ffffff;
      }
    }
  }

  // 新标签闪烁动画
  &.is-new {
    animation: tabNewFlash 0.6s ease-out;
  }

  // 状态样式
  &.is-loading {
    .status-indicator.loading {
      display: flex;
    }
  }

  &.is-error {
    .status-indicator.error {
      display: block;
    }
  }

  &.is-modified {
    .status-indicator.modified {
      display: block;
    }
  }
}

@keyframes tabNewFlash {
  0% {
    background: var(--primary-color-light-3);
    transform: scale(1.05);
  }
  100% {
    background: var(--bg-color);
    transform: scale(1);
  }
}

.tags-icon {
  margin-right: 6px;
  color: inherit;
}

.tags-title {
  margin-right: 4px;
  line-height: 1;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tags-badge {
  margin-right: 4px;
  padding: 1px 4px;
  font-size: 10px;
  line-height: 1;
  background: var(--primary-color);
  color: #fff;
  border-radius: 4px;
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

// 状态指示器
.status-indicator {
  display: none;
  margin-right: 4px;

  &.loading {
    .spinner {
      width: 12px;
      height: 12px;
      border: 2px solid var(--text-color-secondary);
      border-top-color: transparent;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
  }

  &.error {
    width: 6px;
    height: 6px;
    background: var(--danger-color);
    border-radius: 50%;
  }

  &.modified {
    width: 6px;
    height: 6px;
    background: var(--warning-color);
    border-radius: 50%;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

// 溢出警告
.overflow-warning {
  position: fixed;
  bottom: 20px;
  right: 20px;
  padding: 8px 12px;
  background: var(--warning-color);
  color: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  animation: warningPulse 2s ease-in-out infinite;
}

@keyframes warningPulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}
</style>

<style lang="scss">
.tabs-context-menu {
  position: fixed;
  background: var(--card-bg);
  border-radius: 12px;
  box-shadow:
    var(--card-shadow),
    0 10px 40px rgba(0, 0, 0, 0.2);
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
    outline: none;

    &:hover,
    &:focus-visible {
      color: var(--primary-color);
      background: linear-gradient(90deg, rgba(59, 130, 246, 0.1) 0%, transparent 100%);
    }

    &.disabled {
      color: var(--text-color-disabled);
      cursor: not-allowed;

      &:hover {
        background: transparent;
      }
    }
  }
}
</style>