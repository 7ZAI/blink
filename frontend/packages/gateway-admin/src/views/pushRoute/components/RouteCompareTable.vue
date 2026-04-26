<template>
  <el-table
    :data="routes"
    :row-class-name="getRowClassName"
    stripe
    size="small"
    :max-height="maxHeight"
    @row-click="handleRowClick"
  >
    <el-table-column prop="routeId" :label="t('route.routeId')" min-width="180">
      <template #default="{ row }">
        <div class="route-id-cell">
          <span class="diff-marker" :class="row.diffType">
            {{ getDiffMarker(row.diffType) }}
          </span>
          <span class="route-id">{{ row.routeId }}</span>
        </div>
      </template>
    </el-table-column>

    <el-table-column :label="t('route.diffType')" width="100" align="center">
      <template #default="{ row }">
        <el-tag :type="getDiffTypeTag(row.diffType)" size="small" effect="light">
          {{ getDiffTypeText(row.diffType) }}
        </el-tag>
      </template>
    </el-table-column>

    <el-table-column :label="t('route.repositoryConfig')" min-width="150">
      <template #default="{ row }">
        <div v-if="row.repositoryRoute" class="config-preview">
          <el-tag type="success" size="small" effect="plain">
            {{ row.repositoryRoute.uri }}
          </el-tag>
        </div>
        <span v-else class="empty-cell">-</span>
      </template>
    </el-table-column>

    <el-table-column :label="t('route.instanceConfig')" min-width="150">
      <template #default="{ row }">
        <div v-if="row.instanceRoute" class="config-preview">
          <el-tag type="info" size="small" effect="plain">
            {{ row.instanceRoute.uri }}
          </el-tag>
        </div>
        <span v-else class="empty-cell">-</span>
      </template>
    </el-table-column>

    <!-- 可展开字段差异 -->
    <el-table-column type="expand" width="50">
      <template #default="{ row }">
        <div v-if="row.diffType === 'modified' && row.fieldDiffs" class="field-diffs">
          <div class="field-diffs-header">{{ t('route.fieldDiff') }}</div>
          <el-table :data="row.fieldDiffs" size="small" border>
            <el-table-column prop="fieldName" :label="t('route.fieldName')" width="120" />
            <el-table-column prop="oldValue" :label="t('route.oldValue')">
              <template #default="{ row: fieldRow }">
                <el-text type="danger" size="small">{{ fieldRow.oldValue || '-' }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="newValue" :label="t('route.newValue')">
              <template #default="{ row: fieldRow }">
                <el-text type="success" size="small">{{ fieldRow.newValue || '-' }}</el-text>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <!-- 完整配置详情 -->
        <div v-else class="route-config-detail">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="URI">
              <el-tag size="small">{{ row.repositoryRoute?.uri || row.instanceRoute?.uri }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Predicates">
              <span class="mono-text">{{ formatPredicates(row) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="Filters">
              <span class="mono-text">{{ formatFilters(row) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { RouteDiffItem, RouteDefinition } from '@/api/route'

interface Props {
  routes: RouteDiffItem[]
  maxHeight?: number
}

const props = withDefaults(defineProps<Props>(), {
  maxHeight: 400,
})

const emit = defineEmits<{
  rowClick: [route: RouteDiffItem]
}>()

const { t } = useI18n()

// 获取行样式类名
function getRowClassName({ row }: { row: RouteDiffItem }): string {
  const typeMap: Record<string, string> = {
    added: 'diff-row-added',
    modified: 'diff-row-modified',
    deleted: 'diff-row-deleted',
    unchanged: 'diff-row-unchanged',
  }
  return typeMap[row.diffType] || ''
}

// 获取差异标记符号
function getDiffMarker(type: string): string {
  const markerMap: Record<string, string> = {
    added: '+',
    modified: '*',
    deleted: '-',
    unchanged: ' ',
  }
  return markerMap[type] || ' '
}

// 获取差异类型标签样式
function getDiffTypeTag(type: string): 'success' | 'warning' | 'info' | 'danger' {
  const tagMap: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    added: 'success',
    modified: 'warning',
    deleted: 'danger',
    unchanged: 'info',
  }
  return tagMap[type] || 'info'
}

// 获取差异类型文本
function getDiffTypeText(type: string): string {
  const textMap: Record<string, string> = {
    added: t('route.addedRoutes'),
    modified: t('route.modifiedRoutes'),
    deleted: t('route.deletedRoutes'),
    unchanged: t('route.unchangedRoutes'),
  }
  return textMap[type] || type
}

// 格式化断言
function formatPredicates(row: RouteDiffItem): string {
  const route = row.repositoryRoute || row.instanceRoute
  if (!route?.predicates?.length) return '-'
  return route.predicates.map(p => `${p.name}: ${JSON.stringify(p.args)}`).join('; ')
}

// 格式化过滤器
function formatFilters(row: RouteDiffItem): string {
  const route = row.repositoryRoute || row.instanceRoute
  if (!route?.filters?.length) return '-'
  return route.filters.map(f => `${f.name}: ${JSON.stringify(f.args)}`).join('; ')
}

// 处理行点击
function handleRowClick(row: RouteDiffItem) {
  emit('rowClick', row)
}
</script>

<style scoped lang="scss">
.route-id-cell {
  display: flex;
  align-items: center;
  gap: 4px;

  .diff-marker {
    font-weight: bold;
    font-size: 14px;

    &.added {
      color: var(--el-color-success);
    }
    &.modified {
      color: var(--el-color-warning);
    }
    &.deleted {
      color: var(--el-color-danger);
    }
    &.unchanged {
      color: var(--el-text-color-placeholder);
    }
  }

  .route-id {
    font-family: monospace;
  }
}

.config-preview {
  display: flex;
  gap: 4px;
}

.empty-cell {
  color: var(--el-text-color-placeholder);
}

.field-diffs {
  padding: 12px;

  .field-diffs-header {
    font-weight: bold;
    margin-bottom: 8px;
    color: var(--el-text-color-primary);
  }
}

.route-config-detail {
  padding: 12px;

  .mono-text {
    font-family: monospace;
    font-size: 12px;
  }
}

// 差异行背景色
.diff-row-added {
  background-color: rgba(var(--el-color-success-rgb), 0.1) !important;
}
.diff-row-modified {
  background-color: rgba(var(--el-color-warning-rgb), 0.1) !important;
}
.diff-row-deleted {
  background-color: rgba(var(--el-color-danger-rgb), 0.1) !important;
}
</style>