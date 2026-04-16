<template>
  <div class="alert-history-page">
    <!-- Header -->
    <div class="page-header">
      <h3>{{ t('alert.historyManagement') }}</h3>
      <el-button @click="loadHistory">
        <el-icon><Refresh /></el-icon>
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <!-- Filters -->
    <div class="filters">
      <el-row :gutter="16">
        <el-col :span="4">
          <el-select v-model="filters.status" :placeholder="t('alert.statusFiring')" clearable>
            <el-option v-for="opt in STATUS_OPTIONS" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filters.severity" :placeholder="t('alert.severity')" clearable>
            <el-option v-for="opt in SEVERITY_OPTIONS" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            :start-placeholder="t('common.startTime')"
            :end-placeholder="t('common.endTime')"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadHistory">{{ t('common.search') }}</el-button>
          <el-button @click="resetFilters">{{ t('common.reset') }}</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- Table -->
    <el-table :data="historyList" v-loading="loading" stripe>
      <el-table-column prop="ruleName" :label="t('alert.ruleName')" min-width="150" />
      <el-table-column prop="instanceId" :label="t('alert.instanceId')" min-width="150" />
      <el-table-column prop="alertTitle" :label="t('alert.alertTitle')" min-width="200">
        <template #default="{ row }">
          <el-text type="primary">{{ row.alertTitle }}</el-text>
        </template>
      </el-table-column>
      <el-table-column prop="severity" :label="t('alert.severity')" width="100">
        <template #default="{ row }">
          <el-tag :type="getSeverityType(row.severity)" size="small">
            {{ getSeverityLabel(row.severity) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="firedTime" :label="t('alert.firedTime')" width="180" />
      <el-table-column prop="resolvedTime" :label="t('alert.resolvedTime')" width="180">
        <template #default="{ row }">
          {{ row.resolvedTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('common.operation')" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">
            {{ t('common.detail') }}
          </el-button>
          <el-button
            link
            type="success"
            @click="handleAcknowledge(row)"
            v-if="row.status === 'FIRING'"
          >
            {{ t('alert.acknowledge') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <el-pagination
      v-model:current-page="pagination.pageNum"
      v-model:page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="pagination.total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="loadHistory"
      @current-change="loadHistory"
      class="pagination"
    />

    <!-- Detail Dialog -->
    <el-dialog
      v-model="detailVisible"
      :title="t('alert.alertContent')"
      width="600px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('alert.ruleName')">
          {{ currentHistory?.ruleName }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('alert.instanceId')">
          {{ currentHistory?.instanceId }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('alert.severity')">
          <el-tag :type="getSeverityType(currentHistory?.severity)" size="small">
            {{ getSeverityLabel(currentHistory?.severity) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">
          <el-tag :type="getStatusType(currentHistory?.status)" size="small">
            {{ getStatusLabel(currentHistory?.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('alert.firedTime')">
          {{ currentHistory?.firedTime }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('alert.resolvedTime')">
          {{ currentHistory?.resolvedTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('alert.acknowledgedTime')" v-if="currentHistory?.acknowledgedTime">
          {{ currentHistory?.acknowledgedTime }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('alert.acknowledgedBy')" v-if="currentHistory?.acknowledgedBy">
          {{ currentHistory?.acknowledgedBy }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="alert-content">
        <h4>{{ t('alert.alertContent') }}</h4>
        <pre>{{ currentHistory?.alertContent }}</pre>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
        <el-button
          type="success"
          @click="handleAcknowledge(currentHistory)"
          v-if="currentHistory?.status === 'FIRING'"
        >
          {{ t('alert.acknowledge') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  alertApi,
  type AlertHistory,
  STATUS_OPTIONS,
  SEVERITY_OPTIONS,
} from '@/api/alert'

defineOptions({ name: 'AlertHistoryManagement' })

const { t } = useI18n()

// State
const historyList = ref<AlertHistory[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const currentHistory = ref<AlertHistory | null>(null)

// Filters
const filters = reactive({
  status: '',
  severity: '',
  timeRange: [] as string[],
})

// Pagination
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0,
})

// Load history
async function loadHistory() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    }

    if (filters.status) {
      params.status = filters.status
    }
    if (filters.severity) {
      params.severity = filters.severity
    }
    if (filters.timeRange && filters.timeRange.length === 2) {
      params.startTime = filters.timeRange[0]
      params.endTime = filters.timeRange[1]
    }

    const res = await alertApi.getHistory(params)
    historyList.value = res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error('[AlertHistory] Load failed:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

// Reset filters
function resetFilters() {
  filters.status = ''
  filters.severity = ''
  filters.timeRange = []
  pagination.pageNum = 1
  loadHistory()
}

// View detail
function viewDetail(history: AlertHistory) {
  currentHistory.value = history
  detailVisible.value = true
}

// Acknowledge
async function handleAcknowledge(history: AlertHistory | null) {
  if (!history) return

  try {
    await ElMessageBox.confirm(
      t('alert.acknowledgeConfirm'),
      t('common.confirm'),
      { type: 'info', lockScroll: false }
    )

    await alertApi.acknowledge(history.id)
    ElMessage.success(t('common.success'))
    detailVisible.value = false
    loadHistory()
  } catch {
    // User cancelled
  }
}

// Get labels
function getSeverityLabel(severity: string | undefined): string {
  if (!severity) return ''
  return SEVERITY_OPTIONS.find(o => o.value === severity)?.label || severity
}

function getStatusLabel(status: string | undefined): string {
  if (!status) return ''
  return STATUS_OPTIONS.find(o => o.value === status)?.label || status
}

function getSeverityType(severity: string | undefined): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  switch (severity) {
    case 'ERROR':
      return 'danger'
    case 'WARNING':
      return 'warning'
    case 'INFO':
      return 'info'
    default:
      return 'primary'
  }
}

function getStatusType(status: string | undefined): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'FIRING':
      return 'danger'
    case 'RESOLVED':
      return 'success'
    case 'ACKNOWLEDGED':
      return 'info'
    default:
      return 'primary'
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped lang="scss">
.alert-history-page {
  padding: 24px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h3 {
      font-size: 20px;
      font-weight: 600;
      margin: 0;
    }
  }

  .filters {
    margin-bottom: 16px;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }

  .alert-content {
    margin-top: 16px;

    h4 {
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 8px;
    }

    pre {
      background: var(--el-fill-color-light);
      padding: 12px;
      border-radius: 4px;
      font-size: 13px;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }
}
</style>