<template>
  <!-- 推送历史管理页面 -->
  <div class="push-history-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <div class="title-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12,6 12,12 16,14" />
          </svg>
        </div>
        <h3>{{ t('pushHistory.title') }}</h3>
      </div>
      <div class="header-actions">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item :label="t('pushHistory.storageMode')">
            <el-select
              v-model="searchForm.storageMode"
              :placeholder="t('common.pleaseSelect')"
              clearable
              class="form-select"
            >
              <el-option value="redis" :label="t('pushHistory.redisStorage')" />
              <el-option value="nacos" :label="t('pushHistory.nacosStorage')" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('pushHistory.routesGroup')">
            <el-input
              v-model="searchForm.routesGroup"
              :placeholder="t('pushHistory.routesGroupPlaceholder')"
              clearable
              class="form-input"
            />
          </el-form-item>
          <el-form-item :label="t('pushHistory.pushResult')">
            <el-select
              v-model="searchForm.pushResult"
              :placeholder="t('common.pleaseSelect')"
              clearable
              class="form-select"
            >
              <el-option :value="0" :label="t('pushHistory.pushResultSuccess')" />
              <el-option :value="1" :label="t('pushHistory.pushResultPartial')" />
              <el-option :value="2" :label="t('pushHistory.pushResultFailed')" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadPushHistory">
              {{ t('common.search') }}
            </el-button>
            <el-button :icon="Refresh" @click="resetSearch">
              {{ t('common.reset') }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-wrapper">
      <el-table
        :data="pushHistoryList"
        v-loading="historyLoading"
        stripe
        border
        size="small"
        class="history-table"
      >
        <el-table-column prop="pushId" :label="t('pushHistory.pushId')" width="80" align="center">
          <template #default="{ row }">
            <span class="push-id">#{{ row.pushId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="storageMode" :label="t('pushHistory.storageModeColumn')" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.storageMode === 'redis' ? 'success' : 'warning'"
              size="small"
              effect="light"
            >
              {{ row.storageMode === 'redis' ? t('pushHistory.redisStorage') : t('pushHistory.nacosStorage') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="routesGroup" :label="t('pushHistory.routesGroupColumn')" min-width="100">
          <template #default="{ row }">
            <span>{{ row.routesGroup || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="routeIds" :label="t('pushHistory.routeIdsColumn')" min-width="150">
          <template #default="{ row }">
            <el-popover placement="top" trigger="hover" :width="300">
              <template #reference>
                <div class="route-ids-preview">
                  <span class="preview-text">{{ formatRouteIds(row.routeIds) }}</span>
                  <el-icon class="expand-icon"><ArrowRight /></el-icon>
                </div>
              </template>
              <div class="route-ids-detail">
                <div class="detail-header">{{ t('pushHistory.routeIdsPreview') }}</div>
                <div class="detail-list">
                  {{ parseRouteIds(row.routeIds).join(', ') }}
                </div>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column prop="pushMode" :label="t('pushHistory.pushModeColumn')" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.pushMode === 'broadcast' ? 'info' : 'primary'"
              size="small"
              effect="light"
            >
              {{ row.pushMode === 'broadcast' ? t('pushHistory.broadcastMode') : t('pushHistory.specifiedMode') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="instanceCount" :label="t('pushHistory.instanceCountColumn')" width="80" align="center">
          <template #default="{ row }">
            <span class="count-cell">{{ row.instanceCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="successCount" :label="t('pushHistory.successCountColumn')" width="80" align="center">
          <template #default="{ row }">
            <span class="success-count">{{ row.successCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pushResult" :label="t('pushHistory.pushResultColumn')" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="getPushResultType(row.pushResult)"
              size="small"
              effect="dark"
            >
              {{ getPushResultText(row.pushResult) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 失败实例详情列 -->
        <el-table-column :label="t('pushHistory.failedInstancesColumn')" min-width="120">
          <template #default="{ row }">
            <template v-if="row.failedInstanceIds">
              <el-popover placement="top" trigger="hover" :width="450">
                <template #reference>
                  <el-tag type="danger" effect="light" size="small" class="failed-tag">
                    {{ parseFailedIds(row.failedInstanceIds).length }} {{ t('pushHistory.failedInstances') }}
                  </el-tag>
                </template>
                <div class="failed-detail-popover">
                  <div class="failed-header">{{ t('pushHistory.failedInstancesDetail') }}</div>
                  <div class="failed-list">
                    <div
                      v-for="(id, idx) in parseFailedIds(row.failedInstanceIds)"
                      :key="idx"
                      class="failed-item"
                    >
                      <span class="instance-id-text">{{ id }}</span>
                      <span class="error-msg-text">
                        {{ row.instanceErrors?.[id] || t('pushHistory.unknownError') }}
                      </span>
                    </div>
                  </div>
                </div>
              </el-popover>
            </template>
            <span v-else class="no-failed">-</span>
          </template>
        </el-table-column>
        <!-- 确认状态列 -->
        <el-table-column :label="t('pushHistory.confirmStatusColumn')" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="getConfirmStatusType(row.confirmStatus)"
              size="small"
              effect="plain"
            >
              {{ getConfirmStatusText(row.confirmStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" :label="t('pushHistory.operatorColumn')" width="100">
          <template #default="{ row }">
            <span class="operator-cell">{{ row.operatorName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pushTime" :label="t('pushHistory.pushTimeColumn')" width="160">
          <template #default="{ row }">
            <span class="time-cell">{{ row.pushTime }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('pushHistory.actionColumn')" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-if="row.confirmStatus === 0"
                type="success"
                link
                size="small"
                @click="handleConfirm(row)"
              >
                <el-icon><Check /></el-icon>
                {{ t('pushHistory.confirmPush') }}
              </el-button>
              <el-button
                type="primary"
                link
                size="small"
                @click="openSnapshotDialog(row)"
              >
                <el-icon><View /></el-icon>
                {{ t('pushHistory.viewSnapshot') }}
              </el-button>
              <el-button
                type="warning"
                link
                size="small"
                @click="handleRollback(row)"
              >
                <el-icon><RefreshLeft /></el-icon>
                {{ t('pushHistory.rollbackPush') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="historyPage.pageNum"
        v-model:page-size="historyPage.pageSize"
        :total="historyPage.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pagination-wrapper"
        @change="loadPushHistory"
      />
    </div>

    <!-- 路由快照弹窗 -->
    <el-dialog
      v-model="snapshotDialogVisible"
      :title="t('pushHistory.routeSnapshot')"
      width="800px"
      class="snapshot-dialog"
    >
      <div v-if="snapshotRoutes.length > 0" class="snapshot-info">
        {{ t('pushHistory.snapshotRoutesCount', { count: snapshotRoutes.length }) }}
      </div>
      <el-table :data="snapshotRoutes" border stripe size="small" class="snapshot-table">
        <el-table-column prop="routeId" :label="t('pushHistory.routeIdColumn')" min-width="180">
          <template #default="{ row }">
            <span class="route-id-cell">{{ row.routeId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="routeName" :label="t('pushHistory.routeNameColumn')" min-width="150">
          <template #default="{ row }">
            <span>{{ row.routeName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="uri" :label="t('pushHistory.uriColumn')" min-width="200">
          <template #default="{ row }">
            <el-tag type="success" effect="plain" size="small">{{ row.uri }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" :label="t('pushHistory.orderColumn')" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">{{ row.orderNum || 0 }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="snapshotRoutes.length === 0"
        :description="t('pushHistory.noSnapshotData')"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 推送历史管理页面
 * 展示所有路由推送历史记录，支持查看快照和回滚操作
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, ArrowRight, View, RefreshLeft, Check } from '@element-plus/icons-vue'
import {
  routeApi,
  type RouteDefinition,
  type GaRoutePushLogDO,
  type RollbackPushReq,
} from '@/api/route'

defineOptions({
  name: 'PushHistory',
})

const { t } = useI18n()

// 搜索表单
const searchForm = reactive({
  storageMode: '',
  routesGroup: '',
  pushResult: undefined as number | undefined,
})

// 推送历史状态
const pushHistoryList = ref<GaRoutePushLogDO[]>([])
const historyLoading = ref(false)
const historyPage = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

// 快照弹窗状态
const snapshotDialogVisible = ref(false)
const snapshotRoutes = ref<RouteDefinition[]>([])

// 加载推送历史
async function loadPushHistory() {
  historyLoading.value = true
  try {
    const result = await routeApi.getPushHistory({
      storageMode: searchForm.storageMode || undefined,
      routesGroup: searchForm.routesGroup || undefined,
      pushResult: searchForm.pushResult,
      pageNum: historyPage.value.pageNum,
      pageSize: historyPage.value.pageSize,
    })
    pushHistoryList.value = Array.isArray(result?.rows) ? result.rows : []
    historyPage.value.total = result?.total || 0
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    pushHistoryList.value = []
  } finally {
    historyLoading.value = false
  }
}

// 重置搜索
function resetSearch() {
  searchForm.storageMode = ''
  searchForm.routesGroup = ''
  searchForm.pushResult = undefined
  historyPage.value.pageNum = 1
  loadPushHistory()
}

// 处理回滚
async function handleRollback(row: GaRoutePushLogDO) {
  try {
    await ElMessageBox.confirm(
      t('pushHistory.rollbackConfirm'),
      t('common.confirm'),
      { type: 'warning' }
    )
  } catch {
    return
  }

  const rollbackReq: RollbackPushReq = {
    pushId: row.pushId,
    pushMode: row.pushMode,
    targetInstanceIds: row.targetInstanceIds ? JSON.parse(row.targetInstanceIds) : [],
  }

  try {
    await routeApi.rollbackPush(rollbackReq)
    ElMessage.success(t('pushHistory.rollbackSuccess'))
    loadPushHistory()
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  }
}

// 处理确认推送
async function handleConfirm(row: GaRoutePushLogDO) {
  try {
    await ElMessageBox.confirm(
      t('pushHistory.confirmPushConfirm'),
      t('common.confirm'),
      { type: 'info' }
    )
  } catch {
    return
  }

  try {
    await routeApi.confirmPush({ pushId: row.pushId })
    ElMessage.success(t('pushHistory.confirmSuccess'))
    loadPushHistory()
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  }
}

// 打开快照弹窗
function openSnapshotDialog(row: GaRoutePushLogDO) {
  try {
    snapshotRoutes.value = Array.isArray(row.routeSnapshot) ? row.routeSnapshot : []
    snapshotDialogVisible.value = true
  } catch {
    ElMessage.warning(t('pushHistory.noRoutes'))
  }
}

// 解析路由ID JSON
function parseRouteIds(routeIdsJson: string): string[] {
  try {
    return JSON.parse(routeIdsJson)
  } catch {
    return []
  }
}

// 格式化路由ID显示
function formatRouteIds(routeIdsJson: string): string {
  try {
    const ids = JSON.parse(routeIdsJson)
    if (ids.length <= 3) {
      return ids.join(', ')
    }
    return `${ids.slice(0, 3).join(', ')}... ${t('pushHistory.moreRoutes', { count: ids.length - 3 })}`
  } catch {
    return routeIdsJson
  }
}

// 获取推送结果类型
function getPushResultType(result: number): 'success' | 'warning' | 'danger' {
  if (result === 0) return 'success'
  if (result === 1) return 'warning'
  return 'danger'
}

// 获取推送结果文本
function getPushResultText(result: number): string {
  if (result === 0) return t('pushHistory.pushResultSuccess')
  if (result === 1) return t('pushHistory.pushResultPartial')
  return t('pushHistory.pushResultFailed')
}

// 解析失败实例ID列表
function parseFailedIds(failedInstanceIdsJson: string): string[] {
  if (!failedInstanceIdsJson) return []
  try {
    return JSON.parse(failedInstanceIdsJson)
  } catch {
    return []
  }
}

// 获取确认状态类型
function getConfirmStatusType(confirmStatus: number | undefined): 'warning' | 'success' | 'info' {
  if (confirmStatus === undefined || confirmStatus === 0) return 'warning'
  if (confirmStatus === 1) return 'success'
  return 'info'
}

// 获取确认状态文本
function getConfirmStatusText(confirmStatus: number | undefined): string {
  if (confirmStatus === undefined || confirmStatus === 0) return t('pushHistory.confirmPending')
  if (confirmStatus === 1) return t('pushHistory.confirmConfirmed')
  return t('pushHistory.confirmTimeout')
}

// 初始化
onMounted(() => {
  loadPushHistory()
})
</script>

<style scoped lang="scss">
// 推送历史页面
.push-history-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
  background: var(--el-bg-color-page);
  gap: 16px;
}

// 页面头部
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--el-border-color-light);

  .header-title {
    display: flex;
    align-items: center;
    gap: 10px;

    .title-icon {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, var(--el-color-warning), var(--el-color-warning-light-3));
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;

      svg {
        width: 22px;
        height: 22px;
        color: white;
      }
    }

    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }

  .header-actions {
    .search-form {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .form-select {
        width: 140px;
      }

      .form-input {
        width: 160px;
      }
    }
  }
}

// 表格容器
.table-wrapper {
  flex: 1;
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

// 历史表格样式
.history-table {
  flex: 1;
  overflow: auto;

  :deep(.el-table__header) {
    th {
      background: var(--el-fill-color-light) !important;
      font-weight: 600;
    }
  }

  .push-id {
    font-family: 'SF Mono', 'Monaco', monospace;
    font-weight: 600;
    color: var(--el-color-primary);
  }

  .route-id-cell {
    font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
    font-size: 13px;
    color: var(--el-text-color-primary);
  }

  .count-cell {
    font-family: 'SF Mono', 'Monaco', monospace;
    font-weight: 500;
  }

  .success-count {
    font-family: 'SF Mono', 'Monaco', monospace;
    font-weight: 600;
    color: var(--el-color-success);
  }

  .operator-cell {
    font-size: 13px;
  }

  .time-cell {
    font-size: 12px;
    font-family: 'SF Mono', 'Monaco', monospace;
    color: var(--el-text-color-secondary);
  }

  .failed-tag {
    cursor: pointer;
  }

  .no-failed {
    color: var(--el-text-color-secondary);
  }
}

// 失败详情弹出框样式
.failed-detail-popover {
  .failed-header {
    font-weight: 600;
    margin-bottom: 12px;
    color: var(--el-color-danger);
  }

  .failed-list {
    max-height: 200px;
    overflow-y: auto;
  }

  .failed-item {
    padding: 8px 12px;
    background: var(--el-fill-color-light);
    border-radius: 6px;
    margin-bottom: 8px;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .instance-id-text {
      font-family: 'SF Mono', 'Monaco', monospace;
      font-size: 12px;
      font-weight: 500;
      color: var(--el-text-color-primary);
    }

    .error-msg-text {
      font-size: 12px;
      color: var(--el-color-danger);
      word-break: break-all;
    }
  }
}

// 路由ID预览
.route-ids-preview {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--el-color-primary);
  font-size: 13px;

  .preview-text {
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .expand-icon {
    font-size: 12px;
    transition: transform 0.2s;
  }

  &:hover .expand-icon {
    transform: translateX(2px);
  }
}

.route-ids-detail {
  .detail-header {
    font-weight: 600;
    margin-bottom: 8px;
    color: var(--el-text-color-primary);
  }

  .detail-list {
    font-size: 12px;
    font-family: 'SF Mono', 'Monaco', monospace;
    color: var(--el-text-color-secondary);
    word-break: break-all;
    line-height: 1.6;
  }
}

// 操作按钮
.action-buttons {
  display: flex;
  gap: 8px;
}

// 分页样式
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

// 快照弹窗样式
.snapshot-dialog {
  .snapshot-info {
    padding: 12px 16px;
    background: var(--el-fill-color-light);
    border-radius: 8px;
    margin-bottom: 16px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .snapshot-table {
    .route-id-cell {
      font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
      font-size: 13px;
      color: var(--el-text-color-primary);
    }
  }
}
</style>
