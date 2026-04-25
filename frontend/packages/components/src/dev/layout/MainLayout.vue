<template>
  <div class="dev-layout">
    <!-- 侧边栏 -->
    <aside class="dev-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo-container">
          <div class="logo-icon">
            <svg viewBox="0 0 32 32" fill="currentColor">
              <path d="M16 2L4 8v16l12 6 12-6V8L16 2zm0 4l8 4-8 4-8-4 8-4zm-8 8l8 4v8l-8-4v-8zm16 0v8l-8 4v-8l8-4z"/>
            </svg>
          </div>
          <transition name="logo-text">
            <span v-if="!sidebarCollapsed" class="logo-text">Blink Components</span>
          </transition>
        </div>
      </div>

      <el-scrollbar class="sidebar-scrollbar">
        <el-menu
          :default-active="currentRoute"
          :collapse="sidebarCollapsed"
          :router="true"
          class="sidebar-menu"
        >
          <template v-for="item in parsedMenuItems" :key="item.path">
            <!-- 有子菜单 -->
            <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
              <template #title>
                <el-icon class="menu-icon">
                  <component :is="item.iconComponent" v-if="item.iconComponent" />
                </el-icon>
                <span>{{ item.meta?.title }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.path"
                :index="`${item.path}/${child.path}`"
              >
                <template #title>
                  <span class="submenu-title">{{ child.meta?.title }}</span>
                </template>
              </el-menu-item>
            </el-sub-menu>
            <!-- 无子菜单 -->
            <el-menu-item v-else :index="item.path">
              <el-icon class="menu-icon">
                <component :is="item.iconComponent" v-if="item.iconComponent" />
              </el-icon>
              <template #title>
                <span class="menu-title">{{ item.meta?.title }}</span>
              </template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>

      <div class="sidebar-footer">
        <el-tooltip :content="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'" placement="right">
          <el-button
            class="collapse-btn"
            :icon="sidebarCollapsed ? Expand : Fold"
            circle
            size="small"
            @click="sidebarCollapsed = !sidebarCollapsed"
          />
        </el-tooltip>
        <transition name="version">
          <span v-if="!sidebarCollapsed" class="version-text">v1.0.0</span>
        </transition>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="dev-main">
      <header class="dev-header">
        <div class="header-left">
          <el-breadcrumb separator="/" class="custom-breadcrumb">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              <span class="breadcrumb-text">{{ item.title }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-button type="primary" class="header-btn" @click="runTests">
            <el-icon><VideoPlay /></el-icon>
            运行测试
          </el-button>
          <el-button class="header-btn" @click="openCoverage">
            <el-icon><Document /></el-icon>
            测试报告
          </el-button>
        </div>
      </header>
      <div class="dev-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  HomeFilled,
  Grid,
  Tools,
  Briefcase,
  Monitor,
  VideoPlay,
  Document,
  Expand,
  Fold,
  Menu,
  UserFilled,
  FolderOpened,
  Sunny,
  Rank,
  FullScreen,
  Setting,
  Lock,
  PictureFilled,
  ChatDotSquare,
  Tickets,
  Operation,
  Star,
  Check,
} from '@element-plus/icons-vue'
import routes from '../routes'

const route = useRoute()

const sidebarCollapsed = ref(false)
const currentRoute = computed(() => route.path)

// 图标映射
const iconMap: Record<string, any> = {
  HomeFilled,
  Grid,
  Tools,
  Briefcase,
  Monitor,
  Menu,
  UserFilled,
  FolderOpened,
  Sunny,
  Rank,
  FullScreen,
  Setting,
  Lock,
  PictureFilled,
  ChatDotSquare,
  Tickets,
  Operation,
  Document,
  Star,
  Check,
}

// 获取图标组件
const getIconComponent = (iconName: string | undefined) => {
  if (!iconName) return null
  return iconMap[iconName] || null
}

// 菜单项
const menuItems = computed(() => {
  const homeRoute = routes.find((r) => r.path === '/')
  return homeRoute?.children || []
})

// 带图标解析的菜单项
const parsedMenuItems = computed(() => {
  return menuItems.value.map(item => ({
    ...item,
    iconComponent: getIconComponent(item.meta?.icon as string),
    children: item.children?.map(child => ({
      ...child,
    }))
  }))
})

// 面包屑
const breadcrumbs = computed(() => {
  const crumbs: { path: string; title: string }[] = []
  const pathParts = route.path.split('/').filter(Boolean)

  let currentPath = ''
  for (const part of pathParts) {
    currentPath += '/' + part
    const matched = route.matched.find((r) => r.path === currentPath)
    if (matched?.meta?.title) {
      crumbs.push({
        path: currentPath,
        title: matched.meta.title as string,
      })
    }
  }
  return crumbs
})

// 运行测试
const runTests = () => {
  window.open('/?runTests=true', '_blank')
}

// 打开覆盖率报告
const openCoverage = () => {
  window.open('/coverage/index.html', '_blank')
}
</script>

<style scoped lang="scss">
.dev-layout {
  display: flex;
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.dev-sidebar {
  width: 260px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 100;

  &.collapsed {
    width: 64px;

    .sidebar-scrollbar {
      :deep(.el-menu) {
        padding: 8px 0;
      }
    }
  }
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(0, 0, 0, 0.2);
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.logo-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #00d4ff;
  flex-shrink: 0;

  svg {
    width: 100%;
    height: 100%;
  }
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.sidebar-scrollbar {
  flex: 1;
  overflow: hidden;

  :deep(.el-scrollbar__wrap) {
    overflow-x: hidden;
  }
}

.sidebar-menu {
  border-right: none;
  background: transparent;
  padding: 8px 0;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 48px;
    line-height: 48px;
    margin: 4px 8px;
    border-radius: 8px;
    color: rgba(255, 255, 255, 0.7);
    background: transparent;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(0, 212, 255, 0.15);
      color: #00d4ff;
    }
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(90deg, rgba(0, 212, 255, 0.3) 0%, rgba(168, 85, 247, 0.3) 100%);
    color: #fff;
    box-shadow: 0 2px 8px rgba(0, 212, 255, 0.3);

    .menu-icon {
      color: #00d4ff;
    }
  }

  :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
    color: #00d4ff;

    .menu-icon {
      color: #00d4ff;
    }
  }

  :deep(.el-sub-menu__children) {
    background: rgba(0, 0, 0, 0.2);
    margin: 0 8px 8px 8px;
    border-radius: 8px;

    .el-menu-item {
      height: 40px;
      line-height: 40px;
      margin: 2px 4px;
      padding-left: 48px !important;

      &.is-active {
        background: rgba(0, 212, 255, 0.2);
        color: #00d4ff;
      }
    }
  }

  .menu-icon {
    font-size: 18px;
    margin-right: 8px;
    transition: color 0.2s ease;
  }
}

.sidebar-footer {
  height: 56px;
  padding: 0 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(0, 0, 0, 0.2);
}

.collapse-btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: rgba(255, 255, 255, 0.7);

  &:hover {
    background: rgba(0, 212, 255, 0.2);
    color: #00d4ff;
  }
}

.version-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  font-family: monospace;
}

.dev-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f0f2f5;
}

.dev-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.custom-breadcrumb {
  :deep(.el-breadcrumb__item) {
    .el-breadcrumb__inner {
      font-weight: 500;
    }

    &:last-child .el-breadcrumb__inner {
      color: var(--el-color-primary);
    }
  }
}

.breadcrumb-text {
  font-size: 14px;
}

.header-right {
  display: flex;
  gap: 12px;
}

.header-btn {
  font-weight: 500;

  &.el-button--primary {
    background: linear-gradient(135deg, #00d4ff 0%, #a855f7 100%);
    border: none;

    &:hover {
      background: linear-gradient(135deg, #00b8e6 0%, #9333ea 100%);
    }
  }
}

.dev-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #f0f2f5;
}

// 过渡动画
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.logo-text-enter-active,
.logo-text-leave-active {
  transition: all 0.3s ease;
}

.logo-text-enter-from,
.logo-text-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

.version-enter-active,
.version-leave-active {
  transition: all 0.3s ease;
}

.version-enter-from,
.version-leave-to {
  opacity: 0;
}
</style>