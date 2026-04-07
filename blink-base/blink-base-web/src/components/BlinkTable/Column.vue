<!-- src/components/BlinkTable/Column.vue -->
<template>
  <el-table-column
    :prop="prop"
    :label="label"
    :width="width"
    :min-width="minWidth"
    :align="align"
    :fixed="fixed"
    :sortable="sortable"
    :type="type"
    :show-overflow-tooltip="showOverflowTooltip"
  >
    <template v-if="$slots.default" #default="scope">
      <slot v-bind="scope" />
    </template>

    <template v-if="$slots.header" #header="scope">
      <slot name="header" v-bind="scope" />
    </template>
  </el-table-column>
</template>

<script setup lang="ts">
defineOptions({
  name: 'BlinkTableColumn',
})

interface Props {
  prop?: string
  label?: string
  width?: string | number
  minWidth?: string | number
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right' | boolean
  sortable?: boolean | 'custom'
  type?: 'index' | 'selection' | 'expand'
  showOverflowTooltip?: boolean
}

withDefaults(defineProps<Props>(), {
  showOverflowTooltip: true,
})
</script>