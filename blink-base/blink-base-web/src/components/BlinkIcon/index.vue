<template>
  <component
    :is="wrapperTag"
    class="blink-icon"
    :class="customClass"
    :style="iconStyle"
  >
    <component v-if="resolvedElementIcon" :is="resolvedElementIcon" />
    <Icon v-else-if="icon" :icon="icon" />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Icon } from '@iconify/vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

interface Props {
  icon?: string
  size?: number | string
  color?: string
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  icon: '',
  size: undefined,
  color: undefined,
  class: '',
})

const resolvedElementIcon = computed(() => {
  if (!props.icon || props.icon.includes(':')) {
    return null
  }
  return ElementPlusIconsVue[props.icon as keyof typeof ElementPlusIconsVue] || null
})

const wrapperTag = computed(() => (resolvedElementIcon.value ? 'el-icon' : 'span'))

const customClass = computed(() => props.class)

const iconStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.size) {
    style.fontSize = typeof props.size === 'number' ? `${props.size}px` : props.size
  }
  if (props.color) {
    style.color = props.color
  }
  return style
})
</script>

<style scoped lang="scss">
.blink-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
}
</style>
