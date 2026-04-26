<template>
  <el-dialog
    v-model="visible"
    :title="t('route.pushGroupRoutes')"
    width="900px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    class="push-route-dialog"
    @open="handleOpen"
    @closed="handleClosed"
  >
    <!-- 差异对比区域 -->
    <div class="diff-section" v-loading="diffLoading">
      <!-- 差异统计 -->
      <div class="diff-stats-bar" v-if="diffResult">
        <el-tag type="success" effect="plain" size="small">
          {{ t('route.addedRoutes') }}: {{ diffResult.diffStats.addedCount }}
        </el-tag>
        <el-tag type="warning" effect="plain" size="small">
          {{ t('route.modifiedRoutes') }}: {{ diffResult.diffStats.modifiedCount }}
        </el-tag>
        <el-tag type="danger" effect="plain" size="small">
          {{ t('route.deletedRoutes') }}: {{ diffResult.diffStats.deletedCount }}
        </el-tag>
        <el-tag type="info" effect="plain" size="small">
          {{ t('route.unchangedRoutes') }}: {{ diffResult.diffStats.unchangedCount }}
        </el-tag>
      </div>

      <!-- 差异详情表格 -->
      <el-table
        v-if="diffResult && diffResult.diffDetails.length > 0"
        :data="diffResult.diffDetails"
        max-height="400"
        stripe
        size="small"
        :row-class-name="getRowClassName"
      >
        <el-table-column prop="routeId" :label="t('route.routeId')" min-width="180">
          <template #default="{ row }">
            <span class="route-id-cell">
              <span v-if="row.diffType === 'added'" class="diff-marker added">+</span>
              <span v-else-if="row.diffType === 'modified'" class="diff-marker modified">*</span>
              <span v-else-if="row.diffType === 'deleted'" class="diff-marker deleted">-</span>
              <span class="route-id">{{ row.routeId }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="t('route.diffType')" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="getDiffTypeTag(row.diffType)"
              size="small"
              effect="light"
            >
              {{ getDiffTypeText(row.diffType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('route.repositoryConfig')" min-width="150">
          <template #default="{ row }">
            <div v-if="row.repositoryRoute" class="config-preview">
              <el-tag type="success" size="small" effect="plain">{{ row.repositoryRoute.uri }}</el-tag>
            </div>
            <span v-else class="empty-cell">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('route.instanceConfig')" min-width="150">
          <template #default="{ row }">
            <div v-if="row.instanceRoute" class="config-preview">
              <el-tag type="info" size="small" effect="plain">{{ row.instanceRoute.uri }}</el-tag>
            </div>
            <span v-else class="empty-cell">-</span>
          </template>
        </el-table-column>
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
          </template>
        </el-table-column>
      </el-table>

      <!-- 无差异提示 -->
      <div v-if="diffResult && diffResult.diffDetails.length === 0" class="no-diff-tip">
        <el-empty :description="t('route.noDiff')" size="small" />
      </div>
    </div>

    <!-- 实例列表 -->
    <div class="instance-section" v-loading="instancesLoading">
      <div class="section-header">
        <span class="section-title">{{ t('pushRoute.gatewayInstances') }}</span>
        <span class="instance-count">
          {{ t('pushRoute.onlineInstances', { count: onlineInstances.length }) }}
        </span>
      </div>
      <div class="instance-list">
        <div v-for="instance in onlineInstances" :key="instance.instanceId" class="instance-item">
          <span class="instance-id">{{ instance.instanceId }}</span>
          <el-tag type="success" size="small" effect="dark">{{ t('common.online') }}</el-tag>
        </div>
      </div>
      <div v-if="onlineInstances.length === 0" class="no-instance-tip">
        {{ t('route.noOnlineInstances') }}
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="pushing"
          :disabled="!canPush"
          @click="handlePush"
        >
          <el-icon><Promotion /></el-icon>
          {{ t('route.pushGroupRoutes') }}
        </el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 备注输入弹窗 -->
  <el-dialog
    v-model="remarkDialogVisible"
    :title="t('pushRoute.pushConfirm')"
    width="400px"
    :close-on-click-modal="false"
    append-to-body
  >
    <el-form label-width="80px">
      <el-form-item :label="t('pushRoute.remark')">
        <el-input
          v-model="pushRemark"
          type="textarea"
          :rows="3"
          :placeholder="t('pushRoute.remarkPlaceholder')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="remarkDialogVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="pushing" @click="confirmPush">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import { getRouteDiff, fullPushRoutes } from '@/api/route'
import type { RouteDiffRsp, RouteDiffItem } from '@/api/route'
import { queryInstanceList } from '@/api/instance'
import type { InstanceInfo } from '@/api/instance'

const { t } = useI18n()

// Props
interface Props {
  modelValue: boolean
  routesGroup: string
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'push-success'])

// Dialog visibility
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// State
const diffLoading = ref(false)
const diffResult = ref<RouteDiffRsp | null>(null)
const instancesLoading = ref(false)
const onlineInstances = ref<InstanceInfo[]>([])
const pushing = ref(false)
const remarkDialogVisible = ref(false)
const pushRemark = ref('')

// Can push check
const canPush = computed(() => {
  if (!diffResult.value) return false
  if (onlineInstances.value.length === 0) return false
  // 至少有差异才需要推送
  const stats = diffResult.value.diffStats
  return stats.addedCount > 0 || stats.modifiedCount > 0 || stats.deletedCount > 0
})

// Handle dialog open
const handleOpen = async () => {
  if (!props.routesGroup) {
    ElMessage.warning(t('route.selectGroupFirst'))
    visible.value = false
    return
  }

  loadDiff()
  loadInstances()
}

// Load diff
const loadDiff = async () => {
  diffLoading.value = true
  try {
    const result = await getRouteDiff({ routesGroup: props.routesGroup })
    diffResult.value = result
  } catch (error) {
    console.error('[PushRouteDialog] Failed to load diff:', error)
    ElMessage.error(t('message.operationFailed'))
    diffResult.value = null
  } finally {
    diffLoading.value = false
  }
}

// Load instances
const loadInstances = async () => {
  instancesLoading.value = true
  try {
    const res = await queryInstanceList({
      groupKey: props.routesGroup,
      status: 0,
      pageNum: 1,
      pageSize: 100
    })
    onlineInstances.value = res.rows || []
  } catch (error) {
    console.error('[PushRouteDialog] Failed to load instances:', error)
    onlineInstances.value = []
  } finally {
    instancesLoading.value = false
  }
}

// Handle dialog closed
const handleClosed = () => {
  diffResult.value = null
  onlineInstances.value = []
  pushRemark.value = ''
}

// Get row class name for diff table
const getRowClassName = ({ row }: { row: RouteDiffItem }) => {
  const typeMap: Record<string, string> = {
    added: 'diff-row-added',
    modified: 'diff-row-modified',
    deleted: 'diff-row-deleted',
    unchanged: 'diff-row-unchanged'
  }
  return typeMap[row.diffType] || ''
}

// Get diff type tag
const getDiffTypeTag = (type: string): 'success' | 'warning' | 'info' | 'danger' => {
  const tagMap: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    added: 'success',
    modified: 'warning',
    deleted: 'danger',
    unchanged: 'info'
  }
  return tagMap[type] || 'info'
}

// Get diff type text
const getDiffTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    added: t('route.addedRoutes'),
    modified: t('route.modifiedRoutes'),
    deleted: t('route.deletedRoutes'),
    unchanged: t('route.unchangedRoutes')
  }
  return textMap[type] || type
}

// Handle push button click
const handlePush = () => {
  remarkDialogVisible.value = true
}

// Confirm push
const confirmPush = async () => {
  if (!diffResult.value) return

  pushing.value = true
  remarkDialogVisible.value = false

  try {
    await fullPushRoutes({
      storageMode: 'redis',
      routesGroup: props.routesGroup
    })

    ElMessage.success(t('pushRoute.pushSuccess'))
    visible.value = false
    emit('push-success')
  } catch (error) {
    console.error('[PushRouteDialog] Failed to push routes:', error)
    ElMessage.error(t('message.operationFailed'))
  } finally {
    pushing.value = false
  }
}
</script>

<style scoped lang="scss">
.push-route-dialog {
  .diff-section {
    margin-bottom: 20px;

    .diff-stats-bar {
      display: flex;
      gap: 12px;
      margin-bottom: 12px;
      padding: 8px 12px;
      background: var(--el-fill-color-light);
      border-radius: 4px;
    }
  }

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

  // Diff row colors
  .diff-row-added {
    background-color: rgba(var(--el-color-success-rgb), 0.1) !important;
  }
  .diff-row-modified {
    background-color: rgba(var(--el-color-warning-rgb), 0.1) !important;
  }
  .diff-row-deleted {
    background-color: rgba(var(--el-color-danger-rgb), 0.1) !important;
  }

  .no-diff-tip {
    padding: 20px;
    text-align: center;
  }

  .instance-section {
    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      .section-title {
        font-weight: bold;
        color: var(--el-text-color-primary);
      }

      .instance-count {
        color: var(--el-color-success);
        font-size: 14px;
      }
    }

    .instance-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .instance-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 4px 8px;
        background: var(--el-fill-color-light);
        border-radius: 4px;

        .instance-id {
          font-family: monospace;
          font-size: 13px;
        }
      }
    }

    .no-instance-tip {
      color: var(--el-text-color-placeholder);
      padding: 12px;
      text-align: center;
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>