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

    <template v-else-if="formatter" #default="{ row, column }">
      {{ formatter(row, column, prop ? row[prop] : undefined) }}
    </template>

    <template v-if="$slots.header" #header="scope">
      <slot name="header" v-bind="scope" />
    </template>
  </el-table-column>
</template>

<script setup lang="ts">
import type { BlinkTableColumnProps } from './types'

defineOptions({
  name: 'BlinkTableColumn',
})

withDefaults(defineProps<BlinkTableColumnProps>(), {
  showOverflowTooltip: true,
})
</script>
