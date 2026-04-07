<template>
  <el-breadcrumb :separator="separator" class="breadcrumb">
    <template v-if="resolvedItems.length === 0 && showHome">
      <el-breadcrumb-item class="no-redirect">
        <el-icon><HomeFilled /></el-icon>
        <span class="home-text">{{ resolvedHomeLabel }}</span>
      </el-breadcrumb-item>
    </template>
    <template v-else>
      <el-breadcrumb-item v-if="showHome" :to="homePath ? { path: homePath } : undefined">
        <el-icon><HomeFilled /></el-icon>
        <span class="home-text">{{ resolvedHomeLabel }}</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item
        v-for="(item, index) in resolvedItems"
        :key="item.path"
        :to="item.redirect && index !== resolvedItems.length - 1 ? { path: item.redirect } : undefined"
        :class="{ 'no-redirect': index === resolvedItems.length - 1 }"
      >
        {{ resolveTitle(item) }}
      </el-breadcrumb-item>
    </template>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { HomeFilled } from '@element-plus/icons-vue'
import type { RouteLocationMatched } from 'vue-router'

export interface BreadcrumbItem {
  path: string
  title?: string
  redirect?: string
  meta?: Record<string, unknown>
}

const LEGACY_TITLE_MAP: Record<string, string> = {
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

interface Props {
  items?: BreadcrumbItem[]
  separator?: string
  showHome?: boolean
  homePath?: string
  homeLabel?: string
  homeTitleKey?: string
  routeIntegration?: boolean
  excludeRouteNames?: string[]
  titleMap?: Record<string, string>
  titleResolver?: (item: BreadcrumbItem) => string
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  separator: '/',
  showHome: true,
  homePath: '/dashboard',
  homeLabel: '',
  homeTitleKey: 'menu.dashboard',
  routeIntegration: true,
  excludeRouteNames: () => ['Layout'],
  titleMap: () => ({ ...LEGACY_TITLE_MAP }),
  titleResolver: undefined,
})

const { t } = useI18n()
const route = useRoute()

const resolvedHomeLabel = computed(() => props.homeLabel || t(props.homeTitleKey))

const routeItems = computed<BreadcrumbItem[]>(() => {
  if (!props.routeIntegration) {
    return []
  }

  return route.matched
    .filter(item => item.meta && item.meta.title && !props.excludeRouteNames.includes(String(item.name)))
    .map((item: RouteLocationMatched) => ({
      path: item.path,
      redirect: typeof item.redirect === 'string' ? item.redirect : undefined,
      title: item.meta?.title as string | undefined,
      meta: item.meta as Record<string, unknown>,
    }))
})

const resolvedItems = computed(() => {
  if (props.items.length > 0) {
    return props.items
  }
  return routeItems.value.filter(item => item.path !== props.homePath)
})

const resolveTitle = (item: BreadcrumbItem): string => {
  if (props.titleResolver) {
    return props.titleResolver(item)
  }

  const title = item.title || String(item.meta?.title || '')
  if (!title) {
    return ''
  }

  const translated = t(title)
  if (translated !== title) {
    return translated
  }

  const mappedKey = props.titleMap[title]
  return mappedKey ? t(mappedKey) : title
}
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
