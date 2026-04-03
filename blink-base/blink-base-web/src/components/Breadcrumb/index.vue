<template>
  <el-breadcrumb separator="/" class="breadcrumb">
    <template v-if="isHomePage">
      <el-breadcrumb-item class="no-redirect">
        <el-icon><HomeFilled /></el-icon>
        <span class="home-text">{{ t('menu.dashboard') }}</span>
      </el-breadcrumb-item>
    </template>
    <template v-else>
      <el-breadcrumb-item :to="{ path: '/dashboard' }">
        <el-icon><HomeFilled /></el-icon>
        <span class="home-text">{{ t('menu.dashboard') }}</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item
        v-for="(item, index) in breadcrumbs"
        :key="item.path"
        :to="item.redirect ? { path: item.redirect } : undefined"
        :class="{ 'no-redirect': index === breadcrumbs.length - 1 }"
      >
        {{ getMenuTitle(item.meta?.title as string | undefined) }}
      </el-breadcrumb-item>
    </template>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { HomeFilled } from '@element-plus/icons-vue'
import type { RouteLocationMatched } from 'vue-router'

const { t } = useI18n()
const route = useRoute()

const isHomePage = ref(false)
const breadcrumbs = ref<RouteLocationMatched[]>([])

const getMenuTitle = (title: string | undefined): string => {
  if (!title) return ''

  // 首先尝试直接翻译（假设 title 已经是翻译键）
  const translated = t(title)
  if (translated !== title) {
    return translated
  }

  // 兼容旧的中文名称映射
  const titleMap: Record<string, string> = {
    '首页': 'menu.dashboard',
    '系统管理': 'menu.system',
    '用户管理': 'menu.user',
    '角色管理': 'menu.role',
    '菜单管理': 'menu.menu',
    '部门管理': 'menu.dept',
    '个人中心': 'menu.profile',
    '系统设置': 'menu.settings',
    '用户列表': 'menu.userList',
    '在线用户': 'menu.onlineUser',
    '组织管理': 'menu.group',
    '权限管理': 'menu.permission',
    '流程管理': 'menu.workflow',
    '流程设计': 'menu.workflowDesigner',
    '流程列表': 'menu.workflowProcess',
    '我的待办': 'menu.workflowTask',
    '系统配置': 'menu.config',
    '字典管理': 'menu.dict',
    '字典类型': 'menu.dictType',
    '字典数据': 'menu.dictData',
  }

  const key = titleMap[title]
  if (key) {
    return t(key)
  }
  return title
}

const getBreadcrumbs = () => {
  isHomePage.value = route.path === '/dashboard' || route.path === '/'

  if (isHomePage.value) {
    breadcrumbs.value = []
    return
  }

  const matched = route.matched.filter(
    (item) => item.meta && item.meta.title && item.name !== 'Layout'
  )

  breadcrumbs.value = matched
}

watch(
  () => route.path,
  () => {
    getBreadcrumbs()
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.breadcrumb {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  white-space: nowrap;
  overflow: hidden;

  :deep(.el-breadcrumb__item) {
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;

    .el-breadcrumb__inner {
      display: inline-flex;
      align-items: center;
      font-size: 13px;
      color: var(--text-color-secondary);
      transition: color 0.3s;
      white-space: nowrap;
      font-weight: 400;

      &:hover {
        color: var(--primary-color);
      }

      .el-icon {
        margin-right: 4px;
        font-size: 14px;
        flex-shrink: 0;
      }

      .home-text {
        margin-left: 4px;
      }
    }

    &.no-redirect {
      .el-breadcrumb__inner {
        color: var(--text-color-placeholder);
        cursor: text;

        &:hover {
          color: var(--text-color-placeholder);
        }
      }
    }

    &:last-child {
      .el-breadcrumb__inner {
        font-weight: 400;
        color: var(--text-color-secondary);
      }
    }
  }

  :deep(.el-breadcrumb__separator) {
    color: var(--text-color-placeholder);
    margin: 0 4px;
    flex-shrink: 0;
    font-size: 12px;
  }
}
</style>
