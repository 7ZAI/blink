<template>
  <el-tag :type="tagType" v-bind="$attrs">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useDict } from '@/composables/useDict'

/**
 * 字典标签组件
 * 根据字典类型和值自动显示对应的标签
 */

interface Props {
  /** 字典值 */
  value?: string | number | null
  /** 字典类型编码 */
  dictType: string
}

const props = defineProps<Props>()

const { getLabel, getListClass } = useDict(props.dictType)

// 获取显示标签
const label = computed(() => {
  if (props.value === undefined || props.value === null) {
    return '-'
  }
  return getLabel(props.value)
})

// 将listClass映射为el-tag的type
const tagType = computed(() => {
  if (props.value === undefined || props.value === null) {
    return 'info'
  }

  const listClass = getListClass(props.value)

  // 映射关系：primary -> primary, success -> success, warning -> warning, danger -> danger, info -> info
  const typeMap: Record<string, string> = {
    primary: 'primary',
    success: 'success',
    warning: 'warning',
    danger: 'danger',
    info: 'info'
  }

  return typeMap[listClass] || 'info'
})
</script>