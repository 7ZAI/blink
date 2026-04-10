<template>
  <div class="dev-layout">
    <!-- 侧边栏 -->
    <aside class="dev-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h1 v-if="!sidebarCollapsed">Blink Components</h1>
        <span v-else>B</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="sidebarCollapsed"
        :router="true"
        class="sidebar-menu"
      >
        <template v-for="item in menuItems" :key="item.path">
          <!-- 有子菜单 -->
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.meta?.icon" /></el-icon>
              <span>{{ item.meta?.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="`${item.path}/${child.path}`"
            >
              {{ child.meta?.title }}
            </el-menu-item>
          </el-sub-menu>
          <!-- 无子菜单 -->
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.meta?.icon" /></el-icon>
            <span>{{ item.meta?.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
      <div class="sidebar-footer">
        <el-button
          :icon="sidebarCollapsed ? 'Expand' : 'Fold'"
          text
          @click="sidebarCollapsed = !sidebarCollapsed"
        />
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="dev-main">
      <header class="dev-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-button type="primary" @click="runTests">
            <el-icon><VideoPlay /></el-icon>
            运行测试
          </el-button>
          <el-button @click="openCoverage">
            <el-icon><Document /></el-icon>
            测试报告
          </el-button>
        </div>
      </header>
      <div class="dev-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled,
  Grid,
  Tools,
  Briefcase,
  Monitor,
  VideoPlay,
  Document,
} from '@element-plus/icons-vue'
import routes from '../routes'

const route = useRoute()
const router = useRouter()

const sidebarCollapsed = ref(false)
const currentRoute = computed(() => route.path)

// 菜单项
const menuItems = computed(() => {
  const homeRoute = routes.find((r) => r.path === '/')
  return homeRoute?.children || []
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
  background: #f5f7fa;
}

.dev-sidebar {
  width: 240px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;

  &.collapsed {
    width: 64px;
  }
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #e4e7ed;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #e4e7ed;
  text-align: center;
}

.dev-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dev-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-right {
  display: flex;
  gap: 12px;
}

.dev-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

// 过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>