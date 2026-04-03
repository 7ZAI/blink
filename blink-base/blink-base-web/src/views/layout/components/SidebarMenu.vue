<template>
  <template v-if="!menu.children || menu.children.length === 0">
    <el-menu-item :index="menu.url" v-if="menu.status === 0">
      <BlinkIcon v-if="menu.icon" :icon="menu.icon" size="18" />
      <template #title>{{ getMenuTitle(menu.menuName) }}</template>
    </el-menu-item>
  </template>
  <template v-else>
    <el-sub-menu :index="String(menu.menuId)" v-if="menu.status === 0">
      <template #title>
        <BlinkIcon v-if="menu.icon" :icon="menu.icon" size="18" />
        <span>{{ getMenuTitle(menu.menuName) }}</span>
      </template>
      <template v-for="child in menu.children" :key="child.menuId">
        <SidebarMenu :menu="child" />
      </template>
    </el-sub-menu>
  </template>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { Menu } from '@/types'

interface Props {
  menu: Menu
}

defineProps<Props>()

const { t } = useI18n()

const getMenuTitle = (title: string): string => {
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
    '操作日志': 'menu.operationLog',
  }

  const key = titleMap[title]
  if (key) {
    return t(key)
  }
  return title
}
</script>

<style scoped lang="scss">
</style>
