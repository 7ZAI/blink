<template>
  <div class="monitor-page">
    <!-- Statistics -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon online">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalInstances }}</div>
              <div class="stat-label">{{ t('monitor.instanceList') }}</div>
              <div class="stat-sub">
                <span class="healthy-count">
                  {{ t('monitor.healthy') }}: {{ statistics.healthyInstances }}
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon requests">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(statistics.totalRequests) }}</div>
              <div class="stat-label">{{ t('monitor.statistics') }}</div>
              <div class="stat-sub">{{ t('dashboard.todayRequests') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon health">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ healthyRate }}
                <span class="stat-unit">%</span>
              </div>
              <div class="stat-label">{{ t('monitor.healthStatus') }}</div>
              <div class="stat-sub">{{ t('dashboard.healthyRate') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Instance List -->
    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">{{ t('monitor.instanceList') }}</span>
          <div class="header-actions">
            <el-button @click="loadData">
              <el-icon><Refresh /></el-icon>
              {{ t('common.refresh') }}
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="instances" v-loading="loading" stripe class="instance-table">
        <el-table-column
          prop="instanceId"
          :label="t('monitor.instanceId')"
          min-width="180"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="instance-id">{{ row.instanceId }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="serviceId"
          :label="t('route.routeName')"
          width="150"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.serviceId || 'gateway-reactive' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" :label="t('monitor.ip')" width="140" />
        <el-table-column prop="port" :label="t('monitor.port')" width="80" align="center" />
        <el-table-column :label="t('common.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">
              {{ row.statusDesc || getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('monitor.healthStatus')" width="100" align="center">
          <template #default="{ row }">
            <div class="health-status">
              <span
                class="status-dot"
                :class="isHealthy(row.status) ? 'healthy' : 'unhealthy'"
              ></span>
              <span>
                {{ isHealthy(row.status) ? t('monitor.healthy') : t('monitor.unhealthy') }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="onlineTime" :label="t('system.user.lastLoginTime')" width="170">
          <template #default="{ row }">
            <span v-if="row.onlineTime">{{ formatTime(row.onlineTime) }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" width="180" fixed="right">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button type="primary" link size="small" @click="handleViewDetail(row)">
                <el-icon><View /></el-icon>
                {{ t('common.detail') }}
              </el-button>
              <template v-if="row.status === 0">
                <el-button type="warning" link size="small" @click="handleOffline(row)">
                  <el-icon><SwitchButton /></el-icon>
                  {{ t('common.lock') }}
                </el-button>
              </template>
              <template v-else-if="row.status === 2">
                <el-button type="success" link size="small" @click="handleOnline(row)">
                  <el-icon><CircleCheck /></el-icon>
                  {{ t('common.unlock') }}
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Detail Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="t('monitor.instanceId') + ': ' + (currentInstance?.instanceId || '')"
      width="600px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="detail-dialog"
    >
      <el-descriptions :column="2" border v-if="currentInstance">
        <el-descriptions-item :label="t('monitor.instanceId')">
          {{ currentInstance.instanceId }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('route.routeName')">
          {{ currentInstance.serviceId }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('monitor.ip')">
          {{ currentInstance.host }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('monitor.port')">
          {{ currentInstance.port }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('route.uri')" :span="2">
          {{ currentInstance.uri }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">
          <el-tag :type="getStatusType(currentInstance.status)" effect="light">
            {{ currentInstance.statusDesc || getStatusText(currentInstance.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('monitor.healthStatus')">
          <div class="health-status">
            <span
              class="status-dot"
              :class="isHealthy(currentInstance.status) ? 'healthy' : 'unhealthy'"
            ></span>
            <span>
              {{
                isHealthy(currentInstance.status) ? t('monitor.healthy') : t('monitor.unhealthy')
              }}
            </span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.lastLoginTime')">
          {{ currentInstance.onlineTime ? formatTime(currentInstance.onlineTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.status') + t('common.remark')">
          {{ currentInstance.offlineTime ? formatTime(currentInstance.offlineTime) : '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>

    <!-- Offline Dialog -->
    <el-dialog
      v-model="offlineDialogVisible"
      :title="t('monitor.instanceId') + ' ' + t('common.lock')"
      width="450px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-form :model="offlineForm" label-width="100px">
        <el-form-item :label="t('monitor.instanceId')">
          <el-input v-model="offlineForm.instanceId" disabled />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input
            v-model="offlineForm.reason"
            type="textarea"
            :rows="3"
            :placeholder="t('common.pleaseInput')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offlineDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="warning" :loading="submitting" @click="confirmOffline">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Monitor,
  TrendCharts,
  CircleCheck,
  View,
  SwitchButton,
} from '@element-plus/icons-vue'
import {
  getInstanceList,
  getStatistics,
  offlineInstance,
  onlineInstance,
  type InstanceInfo,
  type StatisticsInfo,
} from '@/api/monitor'
import { useInstanceStatus } from '@/composables/useInstanceStatus'

defineOptions({ name: 'MonitorManagement' })

const { t } = useI18n()
const loading = ref(false)
const submitting = ref(false)
const instances = ref<InstanceInfo[]>([])
const statistics = ref<StatisticsInfo>({
  totalInstances: 0,
  healthyInstances: 0,
  totalRequests: 0,
  successRequests: 0,
  failedRequests: 0,
  avgResponseTime: 0,
})

// ==================== SSE 实时状态 ====================

const {
  isConnected: sseConnected,
} = useInstanceStatus({
  onStatusChange: (data) => {
    // SSE 状态变化时更新统计数据
    if (data.stats) {
      statistics.value.totalInstances = data.stats.total
      statistics.value.healthyInstances = data.stats.healthy
    }

    // 更新列表中已存在实例的状态
    if (data.hasChange && data.changedInstanceIds?.length) {
      updateInstanceStatusFromSse(data)
    }
  },
})

/**
 * 从 SSE 数据更新实例列表中的状态
 */
const updateInstanceStatusFromSse = (data: { instances: Array<{ instanceId: string; status: number; healthStatus: string }>; changedInstanceIds?: string[] }) => {
  if (!data.changedInstanceIds?.length) return

  const changedIds = new Set(data.changedInstanceIds)
  instances.value = instances.value.map((instance) => {
    if (changedIds.has(instance.instanceId)) {
      const sseInstance = data.instances.find((i) => i.instanceId === instance.instanceId)
      if (sseInstance) {
        return {
          ...instance,
          status: sseInstance.status,
          statusDesc: getStatusText(sseInstance.status),
        }
      }
    }
    return instance
  })
}

const detailDialogVisible = ref(false)
const offlineDialogVisible = ref(false)
const currentInstance = ref<InstanceInfo | null>(null)

const offlineForm = reactive({
  instanceId: '',
  reason: '',
})

/**
 * 计算健康率
 */
const healthyRate = computed(() => {
  if (statistics.value.totalInstances === 0) return 0
  return Math.round((statistics.value.healthyInstances / statistics.value.totalInstances) * 100)
})

/**
 * 格式化数字（添加千位分隔符）
 */
const formatNumber = (num: number): string => {
  if (!num) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 格式化时间
 */
const formatTime = (time: string): string => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

/**
 * 获取状态类型
 */
const getStatusType = (status: number): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  switch (status) {
    case 0:
      return 'success'
    case 1:
      return 'danger'
    case 2:
      return 'warning'
    default:
      return 'info'
  }
}

/**
 * 获取状态文本
 */
const getStatusText = (status: number): string => {
  switch (status) {
    case 0:
      return t('monitor.up')
    case 1:
      return t('monitor.down')
    case 2:
      return t('system.user.statusLocked')
    default:
      return t('common.unknown')
  }
}

/**
 * 是否健康
 */
const isHealthy = (status: number): boolean => {
  return status === 0
}

/**
 * 加载数据
 */
const loadData = async () => {
  loading.value = true
  try {
    const [statsRes, instancesRes] = await Promise.all([getStatistics({}), getInstanceList()])
    if (statsRes) {
      statistics.value = statsRes
    }
    instances.value = instancesRes?.instances || []
  } catch (error) {
    console.error('Load data error:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

/**
 * 查看详情
 */
const handleViewDetail = (row: InstanceInfo) => {
  currentInstance.value = row
  detailDialogVisible.value = true
}

/**
 * 下线实例
 */
const handleOffline = (row: InstanceInfo) => {
  offlineForm.instanceId = row.instanceId
  offlineForm.reason = ''
  offlineDialogVisible.value = true
}

/**
 * 确认下线
 */
const confirmOffline = async () => {
  submitting.value = true
  try {
    await offlineInstance(offlineForm)
    ElMessage.success(t('common.success'))
    offlineDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('Offline error:', error)
  } finally {
    submitting.value = false
  }
}

/**
 * 上线实例
 */
const handleOnline = (row: InstanceInfo) => {
  ElMessageBox.confirm(t('system.user.unlockConfirm'), t('common.confirm'), {
    type: 'warning',
  }).then(async () => {
    try {
      await onlineInstance({ instanceId: row.instanceId })
      ElMessage.success(t('common.success'))
      loadData()
    } catch (error) {
      console.error('Online error:', error)
    }
  })
}

onMounted(() => {
  loadData()
  // SSE 连接由 MainLayout 统一管理，无需在组件中手动连接
})
</script>

<style scoped lang="scss">
.monitor-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .stats-row {
    flex-shrink: 0;
  }

  .stat-card {
    margin-bottom: 0;

    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 26px;
      flex-shrink: 0;

      &.online {
        background: linear-gradient(135deg, #409eff, #66b1ff);
      }

      &.requests {
        background: linear-gradient(135deg, #67c23a, #85ce61);
      }

      &.health {
        background: linear-gradient(135deg, #e6a23c, #f0c78a);
      }
    }

    .stat-info {
      flex: 1;
      min-width: 0;

      .stat-value {
        font-size: 28px;
        font-weight: 600;
        color: var(--text-color-primary);
        line-height: 1.2;

        .stat-unit {
          font-size: 14px;
          font-weight: normal;
          color: var(--text-color-secondary);
          margin-left: 2px;
        }
      }

      .stat-label {
        font-size: 14px;
        color: var(--text-color-regular);
        margin-top: 4px;
      }

      .stat-sub {
        font-size: 12px;
        color: var(--text-color-secondary);
        margin-top: 4px;

        .healthy-count {
          color: var(--success-color);
        }
      }
    }
  }

  .page-card {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      overflow: auto;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;

    .card-title {
      font-size: 16px;
      font-weight: 500;
    }
  }

  .instance-table {
    .instance-id {
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 13px;
    }
  }

  .health-status {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.healthy {
        background-color: var(--success-color);
        box-shadow: 0 0 6px var(--success-color);
      }

      &.unhealthy {
        background-color: var(--danger-color);
      }
    }
  }

  .empty-text {
    color: var(--text-color-placeholder);
  }

  .operation-buttons {
    display: flex;
    gap: 8px;
  }
}

.detail-dialog {
  :deep(.el-descriptions__label) {
    width: 100px;
  }
}
</style>
