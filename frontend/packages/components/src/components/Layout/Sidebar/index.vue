<template>
  <el-aside
    :width="sidebarWidth + 'px'"
    class="sidebar-container"
    :class="{ 'sidebar-collapsed': isCollapsed }"
    :style="containerStyle"
  >
    <!-- Logo 区域 Slot -->
    <slot
      name="logo"
      :collapsed="isCollapsed"
      :width="sidebarWidth"
      :toggle="toggleSidebar"
    >
      <!-- 默认 Logo - 展开状态 -->
      <div class="sidebar-logo" v-show="!isCollapsed">
        <div class="logo-icon" v-html="logo"></div>
        <span class="logo-text">{{ title }}</span>
      </div>
      <!-- 默认 Logo - 折叠状态 -->
      <div class="sidebar-logo-mini" v-show="isCollapsed">
        <div class="logo-icon-mini" v-html="logo"></div>
      </div>
    </slot>

    <!-- 顶部额外内容 Slot -->
    <slot name="top" :collapsed="isCollapsed" :width="sidebarWidth" />

    <!-- 菜单 Slot -->
    <slot
      name="menu"
      :active-menu="activeMenu"
      :collapsed="isCollapsed"
      :width="sidebarWidth"
      :menu-list="menuList"
      :toggle="toggleSidebar"
    >
      <!-- 默认菜单 -->
      <el-menu
        v-if="menuList.length > 0"
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        :router="routerMode"
      >
        <template v-for="menu in menuList" :key="menu.menuId">
          <SidebarMenu :menu="menu" />
        </template>
      </el-menu>
    </slot>

    <!-- 底部额外内容 Slot -->
    <slot name="bottom" :collapsed="isCollapsed" :width="sidebarWidth" />

    <!-- 拖拽调整宽度手柄 -->
    <div
      v-if="resizable && !isCollapsed"
      class="sidebar-resize-handle"
      :class="{ 'resizing': isResizing }"
      @mousedown="startResize"
    >
      <div class="resize-indicator"></div>
    </div>
  </el-aside>

  <!-- 折叠按钮 Slot -->
  <slot
    name="collapse-trigger"
    :collapsed="isCollapsed"
    :width="sidebarWidth"
    :toggle="toggleSidebar"
  >
    <!-- 默认折叠按钮 -->
    <div
      class="sidebar-collapse-btn"
      :class="{ 'collapsed': isCollapsed }"
      :style="collapseBtnStyle"
      @click="toggleSidebar"
    >
      <el-icon>
        <ArrowLeft v-if="!isCollapsed" />
        <ArrowRight v-else />
      </el-icon>
    </div>
  </slot>
</template>

<script setup lang="ts">
/**
 * Sidebar 侧边栏组件
 */

import { ref, computed, watch, useSlots } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import SidebarMenu from './SidebarMenu.vue'
import { useSidebarState } from '../../../composables/useSidebarState'

// 从 SidebarMenu 导入类型
export type MenuItem = {
  menuId: number
  menuName: string
  menuEnName?: string
  icon?: string
  url?: string
  status?: number
  children?: MenuItem[]
}

export interface Props {
  logo?: string
  title?: string
  menuList?: MenuItem[]
  resizable?: boolean
  defaultWidth?: number
  minWidth?: number
  maxWidth?: number
  collapseThreshold?: number
  storageKey?: string
  collapsed?: boolean
  width?: number
  routerMode?: boolean
  activeMenuPath?: string
  customStyle?: Record<string, string>
  customClass?: string
}

export interface Emits {
  (e: 'collapse-change', collapsed: boolean): void
  (e: 'width-change', width: number): void
  (e: 'resize-start', event: MouseEvent): void
  (e: 'resize-end', width: number): void
}

const props = withDefaults(defineProps<Props>(), {
  logo: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>',
  title: 'Blink Admin',
  menuList: () => [],
  resizable: true,
  defaultWidth: 220,
  minWidth: 60,
  maxWidth: 400,
  collapseThreshold: 100,
  storageKey: 'blink-layout-sidebar',
  routerMode: true,
  customStyle: () => ({}),
  customClass: '',
})

const emit = defineEmits<Emits>()
const slots = useSlots()

const route = useRoute()

const sidebarState = useSidebarState({
  defaultWidth: props.defaultWidth,
  minWidth: props.minWidth,
  maxWidth: props.maxWidth,
  collapseThreshold: props.collapseThreshold,
  storageKey: props.storageKey,
  resizable: props.resizable,
  onCollapseChange: (collapsed: boolean) => emit('collapse-change', collapsed),
  onWidthChange: (width: number) => emit('width-change', width),
})

const { sidebarWidth, isCollapsed, isResizing, toggleSidebar, startResize } = sidebarState

watch(() => props.collapsed, (val) => {
  if (val !== undefined && val !== isCollapsed.value) {
    sidebarState.setCollapsed(val)
  }
})

watch(() => props.width, (val) => {
  if (val !== undefined && val !== sidebarWidth.value) {
    sidebarState.setWidth(val)
  }
})

const activeMenu = computed(() => props.activeMenuPath || route.path)

const containerStyle = computed(() => ({
  ...props.customStyle,
}))

const collapseBtnStyle = computed(() => ({
  left: `${sidebarWidth.value}px`,
}))

defineExpose({
  toggleSidebar,
  setCollapsed: sidebarState.setCollapsed,
  setWidth: sidebarState.setWidth,
  resetSidebar: sidebarState.resetSidebar,
  sidebarWidth,
  isCollapsed,
  sidebarState,
})
</script>

<style scoped lang="scss">
.sidebar-container {
  position: relative;
  overflow: hidden;
  user-select: none;
  transition: width var(--duration-normal, 0.3s) var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1));
  background: var(--sidebar-bg);
  height: 100vh;
}

.sidebar-logo {
  height: 50px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid var(--sidebar-border, rgba(255, 255, 255, 0.1));
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.08) 0%, transparent 100%);
  overflow: hidden;
  white-space: nowrap;
}

.logo-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  :deep(svg) {
    width: 28px !important;
    height: 28px !important;
    fill: var(--primary-color, #3b82f6);
    filter: drop-shadow(0 0 8px var(--primary-color, #3b82f6));
  }
}

.logo-text {
  margin-left: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--sidebar-text-active, #ffffff);
}

.sidebar-logo-mini {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--sidebar-border, rgba(255, 255, 255, 0.1));
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.08) 0%, transparent 100%);
}

.logo-icon-mini {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;

  :deep(svg) {
    width: 28px !important;
    height: 28px !important;
    fill: var(--primary-color, #3b82f6);
  }
}

.sidebar-resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  z-index: 100;

  .resize-indicator {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 2px;
    height: 40px;
    border-radius: 2px;
    opacity: 0;
    transition: opacity 0.2s;
    background: linear-gradient(180deg, var(--primary-color, #3b82f6) 0%, #667eea 100%);
  }

  &:hover {
    .resize-indicator {
      opacity: 1;
    }
  }

  &.resizing {
    background: linear-gradient(180deg, var(--primary-color, #3b82f6) 0%, #667eea 100%);
    .resize-indicator {
      opacity: 1;
    }
  }
}

.sidebar-collapse-btn {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 60px;
  border-radius: 0 8px 8px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 99;
  opacity: 0;
  transition: all 0.3s ease;
  border: 1px solid var(--sidebar-border, rgba(255, 255, 255, 0.1));
  border-left: none;

  &:hover {
    opacity: 1;
    box-shadow: 0 0 10px var(--primary-color, #3b82f6);
  }

  .el-icon {
    font-size: 12px;
    color: var(--sidebar-text, rgba(255, 255, 255, 0.7));
  }

  &:hover .el-icon {
    color: var(--primary-color, #3b82f6);
  }

  &.collapsed {
    left: 60px;
  }
}

.sidebar-container:hover + .sidebar-collapse-btn {
  opacity: 1;
}

.sidebar-menu {
  border-right: none;
  height: calc(100% - 50px);
  overflow-y: auto;
  overflow-x: hidden;
  background: transparent !important;

  &:not(.el-menu--collapse) {
    width: 100%;
  }
}

.sidebar-collapsed {
  .sidebar-resize-handle {
    display: none;
  }
}
</style>