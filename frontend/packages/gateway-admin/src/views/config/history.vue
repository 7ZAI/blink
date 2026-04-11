<template>
  <!-- 配置推送历史页面 -->
  <div class="config-history-page table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card shrink-0" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('config.pushResult')">
          <el-select
            v-model="searchForm.pushResult"
            :placeholder="t('common.pleaseSelect')"
            clearable
            style="width: 120px"
          >
            <el-option value="0" :label="t('config.syncSuccess')" />
            <el-option value="1" :label="t('instanceRoute.partialSuccess')" />
            <el-option value="2" :label="t('config.syncFailed')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('config.pushMode')">
          <el-select
            v-model="searchForm.pushMode"
            :placeholder="t('common.pleaseSelect')"
            clearable
            style="width: 120px"
          >
            <el-option value="broadcast" :label="t('instanceRoute.broadcastMode')" />
            <el-option value="specified" :label="t('instanceRoute.specifiedMode')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('config.operator')">
          <el-input
            v-model.trim="searchForm.operatorName"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item :label="t('config.pushTime')">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="daterange"
            :range-separator="t('common.to')"
            :start-placeholder="t('common.startTime')"
            :end-placeholder="t('common.endTime')"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card flex-1 flex flex-col overflow-hidden" shadow="never">
      <template #header>
        <div class="table-header">
          <span class="header-title">{{ t('config.historyTitle') }}</span>
          <el-button type="primary" :icon="Refresh" @click="loadData">
            {{ t('common.refresh') }}
          </el-button>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table v-loading="loading" :data="tableData" height="100%" stripe border size="small">
          <el-table-column prop="pushId" :label="t('config.pushId')" width="80" align="center">
            <template #default="{ row }">
              <span class="push-id">#{{ row.pushId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="pushMode" :label="t('config.pushMode')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.pushMode === 'broadcast' ? 'info' : 'primary'" size="small" effect="light">
                {{ row.pushMode === 'broadcast' ? t('instanceRoute.broadcastMode') : t('instanceRoute.specifiedMode') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetInstanceIds" :label="t('config.targetInstances')" min-width="150">
            <template #default="{ row }">
              <el-popover placement="top" trigger="hover" :width="300">
                <template #reference>
                  <div class="instance-ids-preview">
                    <span class="preview-text">{{ formatInstanceIds(row.targetInstanceIds) }}</span>
                    <el-icon class="expand-icon"><ArrowRight /></el-icon>
                  </div>
                </template>
                <div class="instance-ids-detail">
                  <div class="detail-header">{{ t('config.targetInstances') }}</div>
                  <div class="detail-list">{{ parseInstanceIds(row.targetInstanceIds).join(', ') }}</div>
                </div>
              </el-popover>
            </template>
          </el-table-column>
          <el-table-column prop="instanceCount" :label="t('config.instanceCount')" width="80" align="center">
            <template #default="{ row }">
              <span class="count-cell">{{ row.instanceCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="successCount" :label="t('config.successCount')" width="80" align="center">
            <template #default="{ row }">
              <span class="success-count">{{ row.successCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="pushResult" :label="t('config.pushResult')" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getPushResultType(row.pushResult)" size="small" effect="dark">
                {{ getPushResultText(row.pushResult) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="operatorName" :label="t('config.operator')" width="100">
            <template #default="{ row }">
              <span class="operator-cell">{{ row.operatorName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="pushTime" :label="t('config.pushTime')" width="160">
            <template #default="{ row }">
              <span class="time-cell">{{ row.pushTime }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="t('config.remark')" min-width="120">
            <template #default="{ row }">
              <span>{{ row.remark || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openSnapshotDialog(row)">
                <el-icon><View /></el-icon>
                {{ t('instanceRoute.viewSnapshot') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 配置快照弹窗 -->
    <el-dialog
      v-model="snapshotDialogVisible"
      :title="t('config.configSnapshot')"
      width="800px"
      class="snapshot-dialog"
    >
      <div v-if="snapshotData" class="snapshot-content">
        <div class="snapshot-info">
          <span>{{ t('config.snapshotContains') }} {{ snapshotKeys.length }} {{ t('config.configItems') }}</span>
        </div>
        <el-table :data="snapshotTableData" border stripe size="small" max-height="400">
          <el-table-column prop="configKey" :label="t('config.configKey')" min-width="180">
            <template #default="{ row }">
              <span class="key-cell">{{ row.configKey }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="configValue" :label="t('config.configValue')" min-width="200">
            <template #default="{ row }">
              <el-tag effect="plain" size="small">{{ row.configValue }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else :description="t('config.noSnapshotData')" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Refresh, ArrowRight, View } from '@element-plus/icons-vue'
import request from '@/utils/request'

const { t } = useI18n()

// 搜索表单
const searchForm = reactive({
  pushResult: '',
  pushMode: '',
  operatorName: '',
  timeRange: null as [Date, Date] | null,
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

// 表格数据
const loading = ref(false)
const tableData = ref<any[]>([])

// 快照弹窗
const snapshotDialogVisible = ref(false)
const snapshotData = ref<Record<string, any> | null>(null)

const snapshotKeys = computed(() => {
  if (!snapshotData.value) return []
  return Object.keys(snapshotData.value)
})

const snapshotTableData = computed(() => {
  if (!snapshotData.value) return []
  return Object.entries(snapshotData.value).map(([key, value]) => ({
    configKey: key,
    configValue: String(value),
  }))
})

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const params = {
      pushResult: searchForm.pushResult || undefined,
      pushMode: searchForm.pushMode || undefined,
      operatorName: searchForm.operatorName || undefined,
      startTime: searchForm.timeRange?.[0]?.toISOString(),
      endTime: searchForm.timeRange?.[1]?.toISOString(),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    }
    const result = await request.post('/gatewayConfig/getPushHistory', { body: params })
    tableData.value = result?.rows || []
    pagination.total = result?.total || 0
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

function handleReset() {
  searchForm.pushResult = ''
  searchForm.pushMode = ''
  searchForm.operatorName = ''
  searchForm.timeRange = null
  pagination.pageNum = 1
  loadData()
}

function parseInstanceIds(json: string): string[] {
  try {
    return JSON.parse(json)
  } catch {
    return []
  }
}

function formatInstanceIds(json: string): string {
  try {
    const ids = JSON.parse(json)
    if (ids.length <= 2) return ids.join(', ')
    return `${ids.slice(0, 2).join(', ')}... (${ids.length})`
  } catch {
    return json || '-'
  }
}

function getPushResultType(result: number): 'success' | 'warning' | 'danger' {
  if (result === 0) return 'success'
  if (result === 1) return 'warning'
  return 'danger'
}

function getPushResultText(result: number): string {
  if (result === 0) return t('config.syncSuccess')
  if (result === 1) return t('instanceRoute.partialSuccess')
  return t('config.syncFailed')
}

function openSnapshotDialog(row: any) {
  try {
    snapshotData.value = row.configSnapshot ? JSON.parse(row.configSnapshot) : null
    snapshotDialogVisible.value = true
  } catch {
    snapshotData.value = null
    snapshotDialogVisible.value = true
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.config-history-page {
  .push-id {
    font-family: 'SF Mono', 'Monaco', monospace;
    font-weight: 600;
    color: var(--el-color-primary);
  }

  .instance-ids-preview {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    color: var(--el-color-primary);
    font-size: 13px;

    .preview-text { overflow: hidden; text-overflow: ellipsis; }
    .expand-icon { font-size: 12px; transition: transform 0.2s; }
    &:hover .expand-icon { transform: translateX(2px); }
  }

  .instance-ids-detail {
    .detail-header { font-weight: 600; margin-bottom: 8px; }
    .detail-list { font-size: 12px; font-family: 'SF Mono', 'Monaco', monospace; word-break: break-all; line-height: 1.6; }
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

  .operator-cell { font-size: 13px; }

  .time-cell {
    font-size: 12px;
    font-family: 'SF Mono', 'Monaco', monospace;
    color: var(--el-text-color-secondary);
  }
}

.snapshot-dialog {
  .snapshot-content {
    .snapshot-info {
      padding: 12px 16px;
      background: var(--el-fill-color-light);
      border-radius: 8px;
      margin-bottom: 16px;
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }

    .key-cell {
      font-family: 'SF Mono', 'Monaco', monospace;
      font-size: 13px;
    }
  }
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}
</style>