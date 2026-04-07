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
 * 将 PascalCase 或带空格的名称转换为 camelCase
 * 例如: 'UserList' → 'userList', 'System Config' → 'systemConfig'
 */
const toCamelCase = (str: string): string => {
  // 移除空格并将后续单词首字母大写
  const normalized = str
    .split(/\s+/)
    .map((word, index) => {
      if (index === 0) {
        return word.charAt(0).toLowerCase() + word.slice(1)
      }
      return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    })
    .join('')
  return normalized
}

/**
 * 菜单标题：优先使用 i18n，回退到 menuName
 */
const menuTitle = computed(() => {
  // 如果有 menuEnName，转换为 camelCase 作为 i18n key
  if (props.menu.menuEnName) {
    const key = toCamelCase(props.menu.menuEnName)
    const i18nKey = `menu.${key}`
    const translated = t(i18nKey)
    // 如果翻译存在且不等于 key 本身，使用翻译
    if (translated !== i18nKey) {
      return translated
    }
  }
  // 回退到中文名称
  return props.menu.menuName
})
</script>

<style scoped lang="scss">
</style>