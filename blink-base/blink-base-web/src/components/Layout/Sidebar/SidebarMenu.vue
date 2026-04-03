<template>
  <template v-if="!menu.children || menu.children.length === 0">
    <el-menu-item :index="menu.url" v-if="menu.status === 0">
      <BlinkIcon v-if="menu.icon" :icon="menu.icon" size="18" />
      <template #title>{{ menuTitle }}</template>
    </el-menu-item>
  </template>
  <template v-else>
    <el-sub-menu :index="String(menu.menuId)" v-if="menu.status === 0">
      <template #title>
        <BlinkIcon v-if="menu.icon" :icon="menu.icon" size="18" />
        <span>{{ menuTitle }}</span>
      </template>
      <template v-for="child in menu.children" :key="child.menuId">
        <SidebarMenu :menu="child" />
      </template>
    </el-sub-menu>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import BlinkIcon from '../../BlinkIcon/index.vue'

/**
 * 菜单项接口定义
 */
export interface MenuItem {
  menuId: number
  menuName: string
  menuEnName?: string
  icon?: string
  url?: string
  status?: number
  children?: MenuItem[]
}

interface Props {
  menu: MenuItem
}

const props = defineProps<Props>()

const { t } = useI18n()

/**
 * 菜单标题：优先使用 i18n，回退到 menuName
 */
const menuTitle = computed(() => {
  const key = props.menu.menuEnName?.toLowerCase() || ''
  return t(`menu.${key}`, props.menu.menuName)
})
</script>

<style scoped lang="scss">
</style>