<!-- src/components/BlinkTable/index.vue -->
<template>
  <div class="blink-table">
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="data"
      :row-key="rowKey"
      :height="height"
      :max-height="maxHeight"
      :stripe="stripe"
      :border="border"
      :default-sort="defaultSort"
      :empty-text="emptyText"
      @selection-change="handleSelectionChange"
      @select="handleSelect"
      @select-all="handleSelectAll"
      @sort-change="handleSortChange"
      @row-click="handleRowClick"
      @row-dblclick="handleRowDblclick"
    >
      <!-- 插槽模式：使用默认插槽 -->
      <template v-if="$slots.default">
        <slot />
      </template>

      <!-- 配置模式：根据 columns 配置渲染 -->
      <template v-else-if="columns?.length">
        <!-- 选择列 -->
        <el-table-column
          v-if="selectable && selectType === 'checkbox'"
          type="selection"
          width="55"
          align="center"
          :selectable="checkSelectable"
        />

        <!-- 数据列 -->
        <template v-for="col in columns" :key="col.prop">
          <el-table-column
            :prop="col.prop"
            :label="col.label"
            :width="col.width"
            :min-width="col.minWidth"
            :align="col.align"
            :fixed="col.fixed"
            :sortable="col.sortable"
            :show-overflow-tooltip="col.showOverflowTooltip ?? showOverflowTooltip"
          >
            <template #default="{ row, $index }">
              <!-- 索引列 -->
              <template v-if="col.type === 'index'">
                {{ $index + 1 }}
              </template>

              <!-- 标签列 -->
              <template v-else-if="col.type === 'tag'">
                <el-tag :type="getTagType(col, row)">
                  {{ getTagLabel(col, row) }}
                </el-tag>
              </template>

              <!-- 图片列 -->
              <template v-else-if="col.type === 'image'">
                <el-image
                  :src="row[col.prop]"
                  :style="{
                    width: `${col.imageWidth || 40}px`,
                    height: `${col.imageHeight || 40}px`
                  }"
                  fit="cover"
                />
              </template>

              <!-- 日期时间列 -->
              <template v-else-if="col.type === 'datetime'">
                {{ row[col.prop] || '-' }}
              </template>

              <!-- 自定义格式化 -->
              <template v-else-if="col.formatter">
                {{ col.formatter(row, col, row[col.prop]) }}
              </template>

              <!-- 默认显示 -->
              <template v-else>
                {{ row[col.prop] ?? '-' }}
              </template>
            </template>
          </el-table-column>
        </template>
      </template>

      <!-- 操作列（配置模式） -->
      <el-table-column
        v-if="operations?.length && !$slots.default"
        :label="$t('common.operation')"
        :width="operationWidth || 200"
        :fixed="operationFixed || 'right'"
      >
        <template #default="{ row }">
          <div class="operation-buttons">
            <template v-for="(op, index) in operations" :key="index">
              <el-button
                v-if="op.visible ? op.visible(row) : true"
                :type="op.type || 'primary'"
                :link="op.link ?? true"
                size="small"
                :disabled="op.disabled ? op.disabled(row) : false"
                @click="op.onClick(row)"
              >
                {{ op.label }}
              </el-button>
            </template>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { TableColumn, BlinkTableProps, BlinkTableEmits, BlinkTableExpose } from './types'

defineOptions({
  name: 'BlinkTable',
})

const props = withDefaults(defineProps<BlinkTableProps>(), {
  rowKey: 'id',
  selectable: false,
  selectType: 'checkbox',
  stripe: true,
  border: true,
  showOverflowTooltip: true,
  loading: false,
  emptyText: '暂无数据',
})

const emit = defineEmits<BlinkTableEmits>()

const tableRef = ref()

// 选择变化
const handleSelectionChange = (selection: any[]) => {
  emit('selection-change', selection)
  const keys = selection.map(item => {
    if (typeof props.rowKey === 'function') {
      return props.rowKey(item)
    }
    return item[props.rowKey as string]
  })
  emit('update:selectedKeys', keys)
}

const handleSelect = (selection: any[], row: any) => {
  emit('select', selection, row)
}

const handleSelectAll = (selection: any[]) => {
  emit('select-all', selection)
}

const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
  emit('sort-change', { prop, order: order || '' })
}

const handleRowClick = (row: any, column: any, event: Event) => {
  emit('row-click', row, column, event)
}

const handleRowDblclick = (row: any, column: any, event: Event) => {
  emit('row-dblclick', row, column, event)
}

// 获取标签类型
const getTagType = (col: TableColumn, row: any): string => {
  if (typeof col.tagType === 'function') {
    return col.tagType(row)
  }
  if (col.tagOptions) {
    const option = col.tagOptions.find(opt => opt.value === row[col.prop])
    return option?.type || 'info'
  }
  return col.tagType || 'info'
}

// 获取标签文本
const getTagLabel = (col: TableColumn, row: any): string => {
  if (col.tagOptions) {
    const option = col.tagOptions.find(opt => opt.value === row[col.prop])
    return option?.label ?? row[col.prop]
  }
  return row[col.prop]
}

// Expose 方法
const clearSelection = () => tableRef.value?.clearSelection()
const toggleRowSelection = (row: any) => tableRef.value?.toggleRowSelection(row)
const toggleAllSelection = () => tableRef.value?.toggleAllSelection()
const setCurrentRow = (row: any) => tableRef.value?.setCurrentRow(row)
const clearSort = () => tableRef.value?.clearSort()
const clearFilter = () => tableRef.value?.clearFilter()
const doLayout = () => tableRef.value?.doLayout()

defineExpose<BlinkTableExpose>({
  clearSelection,
  toggleRowSelection,
  toggleAllSelection,
  setCurrentRow,
  clearSort,
  clearFilter,
  doLayout,
})
</script>

<style scoped lang="scss">
.blink-table {
  :deep(.el-table) {
    background: var(--card-bg);
    border-radius: 8px;
    overflow: hidden;

    // 表头样式
    .el-table__header-wrapper {
      th {
        background: var(--bg-color-page);
        color: var(--text-color-secondary);
        font-weight: 500;
        font-size: 13px;
      }
    }

    // 表格行样式
    .el-table__row {
      td {
        padding: 12px 0;
      }

      &:hover > td {
        background: var(--table-row-hover) !important;
      }
    }

    // 斑马纹
    &--striped .el-table__row--striped td {
      background: var(--bg-color-page);
    }
  }

  .operation-buttons {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;

    .el-button + .el-button {
      margin-left: 0;
    }
  }
}
</style>