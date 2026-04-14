<template>
  <div class="instance-detail-page">
    <!-- 顶部信息栏 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <el-button link @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
            {{ t('common.back') }}
          </el-button>
          <el-divider direction="vertical" />
          <span class="instance-id">{{ instanceId }}</span>
          <el-tag :type="getStatusType(instanceInfo?.status)" effect="light" size="small" class="status-tag">
            {{ instanceInfo?.statusDesc || getStatusText(instanceInfo?.status) }}
          </el-tag>
        </div>
        <div class="header-right">
          <el-button :loading="refreshing" @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            {{ t('common.refresh') }}
          </el-button>
          <el-button
            v-if="instanceInfo?.status === INSTANCE_STATUS.ONLINE"
            type="warning"
            @click="handleOffline"
          >
            <el-icon><SwitchButton /></el-icon>
            {{ t('instance.offlineInstance') }}
          </el-button>
          <el-button
            v-else-if="instanceInfo?.status === INSTANCE_STATUS.SHUTDOWN"
            type="success"
            @click="handleOnline"
          >
            <el-icon><CircleCheck /></el-icon>
            {{ t('instance.onlineInstance') }}
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- Tab 内容 -->
    <el-card class="content-card" shadow="never">
      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- 基本信息 -->
        <el-tab-pane :label="t('instance.basicInfo')" name="basic">
          <el-descriptions :column="2" border v-if="instanceInfo" class="basic-info">
            <el-descriptions-item :label="t('instance.instanceId')">
              {{ instanceInfo.instanceId }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('instance.serviceId')">
              {{ instanceInfo.serviceId }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('instance.host')">
              {{ instanceInfo.host }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('instance.port')">
              {{ instanceInfo.port }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('route.uri')" :span="2">
              {{ instanceInfo.uri }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('common.status')">
              <el-tag :type="getStatusType(instanceInfo.status)" effect="light">
                {{ instanceInfo.statusDesc || getStatusText(instanceInfo.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="t('instance.onlineTime')">
              {{ instanceInfo.onlineTime ? formatTime(instanceInfo.onlineTime) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('instance.metadata')" :span="2">
              <el-input v-if="instanceInfo.metadata" :model-value="instanceInfo.metadata" type="textarea" :rows="3" readonly />
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else :description="t('common.noData')" />
        </el-tab-pane>

        <!-- 健康状态 -->
        <el-tab-pane :label="t('instance.healthStatus')" name="health">
          <div v-if="instanceDetail?.healthDetail" class="health-section">
            <div class="health-overall">
              <span class="health-label">{{ t('instance.healthStatus') }}:</span>
              <el-tag :type="instanceDetail.healthDetail.status === 'UP' ? 'success' : 'danger'">
                {{ instanceDetail.healthDetail.status }}
              </el-tag>
            </div>
            <el-table
              v-if="instanceDetail.healthDetail.components?.length"
              :data="instanceDetail.healthDetail.components"
              stripe
              size="small"
            >
              <el-table-column prop="name" :label="t('instance.componentName')" width="150" />
              <el-table-column prop="status" :label="t('common.status')" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'UP' ? 'success' : 'danger'" size="small">
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('common.detail')">
                <template #default="{ row }">
                  <span v-if="row.details">{{ JSON.stringify(row.details) }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else :description="t('common.noData')" />
        </el-tab-pane>

        <!-- JVM 监控 -->
        <el-tab-pane :label="t('instance.jvmMetrics')" name="jvm">
          <div v-if="instanceDetail?.jvmMetrics" class="jvm-section">
            <!-- 堆内存 -->
            <div class="metric-group">
              <h4>{{ t('instance.heapMemory') }}</h4>
              <el-row :gutter="20">
                <el-col :span="8">
                  <div class="metric-item">
                    <div class="metric-label">{{ t('instance.used') }}</div>
                    <div class="metric-value">{{ formatBytes(instanceDetail.jvmMetrics.heapUsed) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric-item">
                    <div class="metric-label">{{ t('instance.max') }}</div>
                    <div class="metric-value">{{ formatBytes(instanceDetail.jvmMetrics.heapMax) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric-item">
                    <div class="metric-label">{{ t('instance.usage') }}</div>
                    <div class="metric-value">
                      {{ instanceDetail.jvmMetrics.heapUsagePercent?.toFixed(2) || 0 }}%
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>
            <!-- GC 统计 -->
            <div class="metric-group">
              <h4>{{ t('instance.gcStatistics') }}</h4>
              <el-row :gutter="20">
                <el-col :span="12">
                  <div class="metric-item">
                    <div class="metric-label">{{ t('instance.youngGc') }}</div>
                    <div class="metric-value">
                      {{ instanceDetail.jvmMetrics.youngGcCount || 0 }} 次 /
                      {{ instanceDetail.jvmMetrics.youngGcTime || 0 }} ms
                    </div>
                  </div>
                </el-col>
                <el-col :span="12">
                  <div class="metric-item">
                    <div class="metric-label">{{ t('instance.oldGc') }}</div>
                    <div class="metric-value">
                      {{ instanceDetail.jvmMetrics.oldGcCount || 0 }} 次 /
                      {{ instanceDetail.jvmMetrics.oldGcTime || 0 }} ms
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>
            <!-- 线程信息 -->
            <div class="metric-group">
              <h4>{{ t('instance.threadInfo') }}</h4>
              <el-row :gutter="20">
                <el-col :span="8">
                  <div class="metric-item">
                    <div class="metric-label">{{ t('instance.liveThreads') }}</div>
                    <div class="metric-value">{{ instanceDetail.jvmMetrics.liveThreads || 0 }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric-item">
                    <div class="metric-label">{{ t('instance.peakThreads') }}</div>
                    <div class="metric-value">{{ instanceDetail.jvmMetrics.peakThreads || 0 }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric-item">
                    <div class="metric-label">{{ t('instance.daemonThreads') }}</div>
                    <div class="metric-value">{{ instanceDetail.jvmMetrics.daemonThreads || 0 }}</div>
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>
          <el-empty v-else :description="t('common.noData')" />
        </el-tab-pane>

        <!-- HTTP 统计 -->
        <el-tab-pane :label="t('instance.httpStatistics')" name="http">
          <div v-if="instanceDetail?.httpMetrics" class="http-section">
            <el-row :gutter="20">
              <el-col :span="6">
                <div class="metric-item">
                  <div class="metric-label">{{ t('instance.totalRequests') }}</div>
                  <div class="metric-value">{{ formatNumber(instanceDetail.httpMetrics.totalRequests || 0) }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="metric-item">
                  <div class="metric-label">{{ t('instance.successRequests') }}</div>
                  <div class="metric-value success">{{ formatNumber(instanceDetail.httpMetrics.successRequests || 0) }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="metric-item">
                  <div class="metric-label">{{ t('instance.failedRequests') }}</div>
                  <div class="metric-value danger">{{ formatNumber(instanceDetail.httpMetrics.failedRequests || 0) }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="metric-item">
                  <div class="metric-label">{{ t('instance.successRate') }}</div>
                  <div class="metric-value">{{ instanceDetail.httpMetrics.successRate?.toFixed(2) || 0 }}%</div>
                </div>
              </el-col>
            </el-row>
            <div class="metric-item center">
              <div class="metric-label">{{ t('instance.avgResponseTime') }}</div>
              <div class="metric-value">{{ instanceDetail.httpMetrics.avgResponseTime || 0 }} ms</div>
            </div>
          </div>
          <el-empty v-else :description="t('common.noData')" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 下线弹窗 -->
    <el-dialog
      v-model="offlineDialogVisible"
      :title="t('instance.offlineInstance')"
      width="450px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-form :model="offlineForm" label-width="100px">
        <el-form-item :label="t('instance.instanceId')">
          <el-input v-model="offlineForm.instanceId" disabled />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="offlineForm.reason" type="textarea" :rows="3" :placeholder="t('common.pleaseInput')" />
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
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Refresh, SwitchButton, CircleCheck } from '@element-plus/icons-vue'
import {
  getInstanceDetailWithMetrics,
  onlineInstance,
  offlineInstance,
  type InstanceInfo,
  type InstanceDetail,
  INSTANCE_STATUS,
} from '@/api/instance'
import { useInstanceStatus } from '@/composables/useInstanceStatus'

defineOptions({ name: 'InstanceDetail' })

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

// 从路由获取实例 ID
const instanceId = computed(() => route.params.instanceId as string || route.query.instanceId as string)
const instanceRowId = computed(() => route.query.id ? Number(route.query.id) : undefined)

// 状态
const loading = ref(false)
const refreshing = ref(false)
const submitting = ref(false)
const activeTab = ref('basic')

const instanceInfo = ref<InstanceInfo | null>(null)
const instanceDetail = ref<InstanceDetail | null>(null)

const offlineDialogVisible = ref(false)
const offlineForm = reactive({
  instanceId: '',
  reason: '',
})

// SSE 实时状态（复用 MainLayout 的 SSE 连接）
// 用于更新实例基本状态信息（status, healthStatus, cpuUsage, heapUsagePercent）
useInstanceStatus({
  onStatusChange: (data) => {
    // 更新实例状态
    if (data.instances && instanceId.value) {
      const sseInstance = data.instances.find(i => i.instanceId === instanceId.value)
      if (sseInstance && instanceInfo.value) {
        instanceInfo.value = {
          ...instanceInfo.value,
          status: sseInstance.status,
          statusDesc: getStatusText(sseInstance.status),
        }
        // 同时更新 JVM 指标中的部分实时数据（如果有）
        if (instanceDetail.value?.jvmMetrics) {
          instanceDetail.value.jvmMetrics.cpuUsage = sseInstance.cpuUsage
          instanceDetail.value.jvmMetrics.heapUsagePercent = sseInstance.heapUsagePercent
        }
      }
    }
  },
})

// 辅助方法
const getStatusType = (status?: number): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  switch (status) {
    case INSTANCE_STATUS.ONLINE:
      return 'success'
    case INSTANCE_STATUS.OFFLINE:
      return 'danger'
    case INSTANCE_STATUS.SHUTDOWN:
      return 'warning'
    default:
      return 'info'
  }
}

const getStatusText = (status?: number): string => {
  switch (status) {
    case INSTANCE_STATUS.ONLINE:
      return t('instance.statusOnline')
    case INSTANCE_STATUS.OFFLINE:
      return t('instance.statusOffline')
    case INSTANCE_STATUS.SHUTDOWN:
      return t('instance.statusShutdown')
    default:
      return t('common.unknown')
  }
}

const formatTime = (time: string): string => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const formatNumber = (num: number): string => {
  if (!num) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const formatBytes = (bytes: number | undefined): string => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let unitIndex = 0
  let value = bytes
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex++
  }
  return `${value.toFixed(2)} ${units[unitIndex]}`
}

// 加载数据
const loadData = async () => {
  if (!instanceRowId.value) {
    ElMessage.error(t('common.paramError'))
    return
  }

  loading.value = true
  try {
    const detail = await getInstanceDetailWithMetrics({ id: instanceRowId.value })
    instanceDetail.value = detail
    instanceInfo.value = detail?.instanceInfo || null
  } catch (error) {
    console.error('Load instance detail error:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 刷新
const handleRefresh = async () => {
  refreshing.value = true
  try {
    await loadData()
    ElMessage.success(t('common.success'))
  } catch (error) {
    console.error('Refresh error:', error)
  } finally {
    refreshing.value = false
  }
}

// 返回
const handleBack = () => {
  router.push('/instance')
}

// 下线
const handleOffline = () => {
  if (!instanceInfo.value) return
  offlineForm.instanceId = instanceInfo.value.instanceId
  offlineForm.reason = ''
  offlineDialogVisible.value = true
}

const confirmOffline = async () => {
  submitting.value = true
  try {
    await offlineInstance(offlineForm)
    ElMessage.success(t('common.success'))
    offlineDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('Offline instance error:', error)
  } finally {
    submitting.value = false
  }
}

// 上线
const handleOnline = () => {
  if (!instanceInfo.value) return
  ElMessageBox.confirm(t('instance.onlineConfirm'), t('message.tips'), { type: 'info' })
    .then(async () => {
      try {
        await onlineInstance({ instanceId: instanceInfo.value!.instanceId })
        ElMessage.success(t('common.success'))
        loadData()
      } catch (error) {
        console.error('Online instance error:', error)
      }
    })
}

// 移除定时轮询，改用 SSE 实时推送更新基本状态
// 详细指标（JVM、HTTP、健康详情）需要手动刷新获取
onMounted(() => {
  loadData()
})

onUnmounted(() => {
  // SSE 连接由 MainLayout 管理，无需断开
})

// 监听路由参数变化
watch(() => route.query.id, (newId) => {
  if (newId && Number(newId) !== instanceRowId.value) {
    loadData()
  }
})
</script>

<style scoped lang="scss">
.instance-detail-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .header-card {
    flex-shrink: 0;

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        display: flex;
        align-items: center;
        gap: 8px;

        .instance-id {
          font-family: 'Monaco', 'Menlo', monospace;
          font-size: 15px;
          font-weight: 500;
          color: var(--text-color-primary);
        }

        .status-tag {
          margin-left: 8px;
        }
      }

      .header-right {
        display: flex;
        gap: 8px;
      }
    }
  }

  .content-card {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      overflow: auto;
      display: flex;
      flex-direction: column;
    }
  }

  .detail-tabs {
    flex: 1;
    min-height: 0;

    :deep(.el-tabs__content) {
      flex: 1;
      overflow: auto;
    }
  }

  .basic-info {
    :deep(.el-descriptions__label) {
      width: 100px;
    }
  }

  .health-section {
    .health-overall {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;

      .health-label {
        font-weight: 500;
      }
    }
  }

  .jvm-section,
  .http-section {
    .metric-group {
      margin-bottom: 24px;

      h4 {
        margin-bottom: 12px;
        font-size: 14px;
        font-weight: 500;
        color: var(--text-color-primary);
      }
    }

    .metric-item {
      text-align: center;
      padding: 16px;
      background: var(--bg-color);
      border-radius: 8px;

      .metric-label {
        font-size: 12px;
        color: var(--text-color-secondary);
        margin-bottom: 8px;
      }

      .metric-value {
        font-size: 20px;
        font-weight: 600;
        color: var(--text-color-primary);

        &.success { color: var(--success-color); }
        &.danger { color: var(--danger-color); }
      }

      &.center {
        max-width: 200px;
        margin: 16px auto 0;
      }
    }
  }
}
</style>
