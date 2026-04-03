<template>
  <el-aside
    :width="sidebarWidth + 'px'"
    class="sidebar transition-sidebar relative overflow-hidden select-none"
    :class="{ 'sidebar-collapsed': isCollapsed }"
  >
    <!-- Logo 区域 - 展开状态 -->
    <div class="logo h-[50px] flex items-center px-4 border-b" v-show="!isCollapsed">
      <div class="logo-icon shrink-0" v-html="logo"></div>
      <span class="logo-text ml-3">{{ title }}</span>
    </div>
    <!-- Logo 区域 - 折叠状态 -->
    <div class="logo-mini h-[50px] flex items-center justify-center border-b" v-show="isCollapsed">
      <div class="logo-icon-mini" v-html="logo"></div>
    </div>

    <!-- 侧边栏菜单 -->
    <el-menu
      :default-active="activeMenu"
      class="sidebar-menu"
      :collapse="isCollapsed"
      :collapse-transition="false"
      router
    >
      <template v-for="menu in menuList" :key="menu.menuId">
        <SidebarMenu :menu="menu" />
      </template>
    </el-menu>

    <!-- 拖拽调整宽度的手柄 -->
    <div
      v-if="resizable"
      class="resize-handle"
      :class="{ 'resizing': isResizing }"
      @mousedown="startResize"
    >
      <div class="resize-indicator"></div>
    </div>
  </el-aside>

  <!-- 折叠按钮 -->
  <div
    class="collapse-btn"
    :class="{ 'collapsed': isCollapsed }"
    @click="toggleSidebar"
  >
    <el-icon><ArrowLeft v-if="!isCollapsed" /><ArrowRight v-else /></el-icon>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import SidebarMenu, { type MenuItem } from './SidebarMenu.vue'

interface Props {
  /** Logo SVG 字符串 */
  logo?: string
  /** 系统标题 */
  title?: string
  /** 菜单列表 */
  menuList?: MenuItem[]
  /** 是否可拖拽调整宽度 */
  resizable?: boolean
  /** 默认宽度 */
  defaultWidth?: number
  /** 最小宽度 */
  minWidth?: number
  /** 最大宽度 */
  maxWidth?: number
}

const props = withDefaults(defineProps<Props>(), {
  logo: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>',
  title: 'Blink Admin',
  menuList: () => [],
  resizable: true,
  defaultWidth: 220,
  minWidth: 60,
  maxWidth: 400,
})

const route = useRoute()
const activeMenu = computed(() => route.path)

const MIN_WIDTH = props.minWidth
const MAX_WIDTH = props.maxWidth
const DEFAULT_WIDTH = props.defaultWidth
const COLLAPSE_THRESHOLD = 100

const sidebarWidth = ref(DEFAULT_WIDTH)
const isCollapsed = ref(false)
const isResizing = ref(false)

const startResize = (e: MouseEvent) => {
  if (!props.resizable) return

  isResizing.value = true
  const startX = e.clientX
  const startWidth = sidebarWidth.value

  const handleMouseMove = (e: MouseEvent) => {
    const delta = e.clientX - startX
    let newWidth = startWidth + delta

    if (newWidth < MIN_WIDTH) {
      newWidth = MIN_WIDTH
    } else if (newWidth > MAX_WIDTH) {
      newWidth = MAX_WIDTH
    }

    sidebarWidth.value = newWidth

    if (newWidth <= COLLAPSE_THRESHOLD) {
      isCollapsed.value = true
    } else {
      isCollapsed.value = false
    }
  }

  const handleMouseUp = () => {
    isResizing.value = false
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)

    localStorage.setItem('sidebarWidth', String(sidebarWidth.value))
    localStorage.setItem('sidebarCollapsed', String(isCollapsed.value))
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  if (isCollapsed.value) {
    sidebarWidth.value = MIN_WIDTH
  } else {
    sidebarWidth.value = DEFAULT_WIDTH
  }

  localStorage.setItem('sidebarWidth', String(sidebarWidth.value))
  localStorage.setItem('sidebarCollapsed', String(isCollapsed.value))
}

onMounted(() => {
  const savedWidth = localStorage.getItem('sidebarWidth')
  const savedCollapsed = localStorage.getItem('sidebarCollapsed')

  if (savedCollapsed !== null) {
    isCollapsed.value = savedCollapsed === 'true'
  }

  if (savedWidth !== null) {
    const width = parseInt(savedWidth)
    if (width >= MIN_WIDTH && width <= MAX_WIDTH) {
      sidebarWidth.value = width
    }
  }
})

defineExpose({
  toggleSidebar,
  isCollapsed,
  sidebarWidth,
})
</script>

<style scoped lang="scss">
/* 侧边栏样式 */
.sidebar {
  position: relative;
  overflow: hidden;
  user-select: none;
  background: linear-gradient(180deg, var(--sidebar-bg) 0%, rgba(15, 23, 42, 0.98) 100%);
  transition: width var(--duration-normal) var(--ease-out-expo), background var(--duration-normal) var(--ease-out-expo);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);

  /* Logo 区域 */
  .logo {
    @apply h-[50px] flex items-center px-4;
    border-bottom: 1px solid var(--sidebar-border);
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.08) 0%, transparent 100%);
    @apply overflow-hidden;
  }

  .logo-icon {
    @apply w-8 h-8 flex items-center justify-center shrink-0;

    :deep(svg) {
      width: 28px !important;
      height: 28px !important;
      fill: var(--primary-color);
      filter: drop-shadow(0 0 8px var(--primary-color));
    }
  }

  /* Logo 文字 */
  .logo-text {
    @apply text-base font-semibold tracking-wider whitespace-nowrap;
    color: var(--sidebar-text-active);
    text-shadow: 0 0 20px rgba(59, 130, 246, 0.5);
  }

  /* 折叠状态的 Logo */
  .logo-mini {
    @apply h-[50px] flex items-center justify-center;
    border-bottom: 1px solid var(--sidebar-border);
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.08) 0%, transparent 100%);
  }

  .logo-icon-mini {
    @apply w-8 h-8 flex items-center justify-center;

    :deep(svg) {
      width: 28px !important;
      height: 28px !important;
      fill: var(--primary-color);
      filter: drop-shadow(0 0 8px var(--primary-color));
    }
  }

  /* 拖拽调整宽度的手柄 */
  .resize-handle {
    @apply absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize z-[100];

    .resize-indicator {
      @apply absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-0.5 h-10 rounded-sm opacity-0 transition-opacity;
      background: var(--gradient-cyber);
      box-shadow: var(--glow-primary);
    }

    &:hover {
      .resize-indicator {
        @apply opacity-100;
      }
    }

    &.resizing {
      background: linear-gradient(180deg, var(--primary-color) 0%, var(--neon-purple) 100%);
      box-shadow: var(--glow-primary);

      .resize-indicator {
        @apply opacity-100;
      }
    }
  }

  &.sidebar-collapsed {
    .resize-handle {
      @apply hidden;
    }
  }
}

/* 折叠按钮 */
.collapse-btn {
  @apply fixed top-1/2 -translate-y-1/2 w-5 h-[60px] rounded-r-lg flex items-center justify-center cursor-pointer z-[99] opacity-0 transition-all;
  border: 1px solid var(--sidebar-border);
  border-left: none;
  background: linear-gradient(180deg, var(--sidebar-bg) 0%, rgba(15, 23, 42, 0.95) 100%);
  left: v-bind('sidebarWidth + "px"');

  &:hover {
    @apply opacity-100;
    background: linear-gradient(180deg, var(--sidebar-bg-hover) 0%, var(--sidebar-bg) 100%);
    box-shadow: var(--glow-primary);
  }

  .el-icon {
    @apply text-xs;
    color: var(--sidebar-text);
    transition: all var(--duration-normal) var(--ease-out-expo);
  }

  &:hover .el-icon {
    color: var(--primary-color);
    filter: drop-shadow(0 0 6px var(--primary-color));
  }

  &.collapsed {
    @apply left-[60px];
  }
}

.sidebar:hover + .collapse-btn {
  @apply opacity-100;
}

/* 侧边栏菜单样式 */
.sidebar-menu {
  @apply border-r-0 h-[calc(100%-50px)] overflow-y-auto overflow-x-hidden;
  background: transparent !important;

  &:not(.el-menu--collapse) {
    @apply w-full;
  }

  :deep(.el-menu-item) {
    @apply h-11 leading-[44px] mx-2 my-1 rounded-lg transition-all;
    color: var(--sidebar-text);
    position: relative;

    &:hover {
      @apply translate-x-1;
      background: var(--sidebar-bg-hover) !important;
      color: var(--sidebar-text-active);
      box-shadow: inset 0 0 20px rgba(59, 130, 246, 0.1);
    }

    &.is-active {
      @apply font-medium;
      background: var(--sidebar-active-bg) !important;
      color: var(--sidebar-text-active) !important;
      box-shadow: inset 0 0 30px rgba(59, 130, 246, 0.15);

      &::before {
        content: '';
        @apply absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-5 rounded-r-sm;
        background: var(--gradient-cyber);
        box-shadow: 0 0 10px var(--primary-color);
      }
    }
  }

  :deep(.el-sub-menu__title) {
    @apply h-11 leading-[44px] mx-2 my-1 rounded-lg transition-all;
    color: var(--sidebar-text);

    &:hover {
      @apply translate-x-1;
      background: var(--sidebar-bg-hover) !important;
      color: var(--sidebar-text-active);
    }
  }

  :deep(.el-sub-menu) {
    .el-menu {
      background: transparent !important;
    }

    .el-menu-item {
      background: transparent !important;
      padding-left: 48px !important;

      &:hover {
        background: var(--sidebar-bg-hover) !important;
      }
    }

    // 三级菜单缩进
    .el-sub-menu {
      .el-menu-item {
        padding-left: 68px !important;
      }

      // 四级菜单缩进
      .el-sub-menu {
        .el-menu-item {
          padding-left: 88px !important;
        }
      }
    }

    &.is-active {
      > .el-sub-menu__title {
        @apply font-medium;
        color: var(--sidebar-text-active);
      }
    }
  }
}
</style>