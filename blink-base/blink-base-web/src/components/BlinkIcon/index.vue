<template>
  <component
    :is="isElementIcon ? 'el-icon' : 'span'"
    class="blink-icon"
    :class="customClass"
    :style="iconStyle"
  >
    <component v-if="isElementIcon" :is="iconName" />
    <Icon v-else :icon="icon" />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Icon } from '@iconify/vue'

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

const isElementIcon = computed(() => {
  if (!props.icon) return false
  return !props.icon.includes(':')
})

const iconName = computed(() => {
  return props.icon
})

const customClass = computed(() => {
  return props.class
})

const iconStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.size) {
    const sizeValue = typeof props.size === 'number' ? `${props.size}px` : props.size
    style.fontSize = sizeValue
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
