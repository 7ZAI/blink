<template>
  <div class="instance-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon total">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalInstances }}</div>
              <div class="stat-label">{{ t('instance.totalInstances') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon online">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.onlineInstances }}</div>
              <div class="stat-label">{{ t('instance.onlineInstances') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon healthy">
              <el-icon><SuccessFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.healthyInstances }}</div>
              <div class="stat-label">{{ t('instance.healthyInstances') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon cpu">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ statistics.avgCpuUsage }}
                <span class="stat-unit">%</span>
              </div>
              <div class="stat-label">{{ t('instance.avgCpuUsage') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实例列表 -->
    <el-card class="page-card" shadow="never">
      <!-- 搜索区域 -->
      <div class="search-area">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item :label="t('instance.serviceId')">
            <el-input
              v-model="searchForm.serviceId"
              :placeholder="t('common.pleaseInput')"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item :label="t('instance.host')">
            <el-input
              v-model="searchForm.host"
              :placeholder="t('common.pleaseInput')"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item :label="t('common.status')">
            <el-select v-model="searchForm.status" :placeholder="t('common.pleaseSelect')" clearable style="width: 140px">
              <el-option :label="t('common.statusOnline')" :value="0" />
              <el-option :label="t('common.statusOffline')" :value="1" />
              <el-option :label="t('common.statusShutdown')" :value="2" />
              <el-option :label="t('instance.statusDraining')" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('instance.groupKey')">
            <el-select v-model="searchForm.groupKey" :placeholder="t('instance.groupKeyPlaceholder')" clearable style="width: 140px">
              <el-option
                v-for="group in groupOptions"
                :key="group.groupKey"
                :label="group.groupName"
                :value="group.groupKey"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              {{ t('common.search') }}
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              {{ t('common.reset') }}
            </el-button>
          </el-form-item>
          <el-form-item class="right-actions">
            <el-button :loading="refreshing" @click="handleRefresh">
              <el-icon><Refresh /></el-icon>
              {{ t('common.refresh') }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 表格 -->
      <el-table :data="instanceList" v-loading="loading" stripe class="instance-table">
        <el-table-column prop="instanceId" :label="t('common.instanceId')" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="instance-id">{{ row.instanceId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="serviceId" :label="t('instance.serviceId')" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.serviceId }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" :label="t('instance.host')" width="150" show-overflow-tooltip />
        <el-table-column prop="port" :label="t('common.port')" width="90" align="center" />
        <el-table-column :label="t('instance.groupKey')" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.groupKey" type="info" effect="plain" size="small">{{ row.groupKey }}</el-tag>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('instance.storageMode')" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.storageMode" :type="row.storageMode === 'redis' ? 'success' : 'warning'" effect="plain" size="small">
              {{ row.storageMode === 'redis' ? t('instance.storageModeRedis') : t('instance.storageModeNacos') }}
            </el-tag>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light" size="small">
              {{ row.statusDesc || getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="onlineTime" :label="t('instance.onlineTime')" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.onlineTime">{{ formatTime(row.onlineTime) }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" width="290" fixed="right">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button type="primary" link size="small" @click="handleViewDetail(row)">
                <el-icon><View /></el-icon>
                {{ t('common.detail') }}
              </el-button>
              <el-button
                v-if="row.status === INSTANCE_STATUS.ONLINE"
                type="warning"
                link
                size="small"
                @click="handleOffline(row)"
              >
                <el-icon><SwitchButton /></el-icon>
                {{ t('instance.offlineInstance') }}
              </el-button>
              <el-button
                v-else-if="row.status === INSTANCE_STATUS.SHUTDOWN"
                type="success"
                link
                size="small"
                @click="handleOnline(row)"
              >
                <el-icon><CircleCheck /></el-icon>
                {{ t('instance.onlineInstance') }}
              </el-button>
              <el-button
                v-if="row.status === INSTANCE_STATUS.OFFLINE || row.status === INSTANCE_STATUS.SHUTDOWN"
                type="primary"
                link
                size="small"
                @click="handleSwitchGroup(row)"
              >
                <el-icon><Switch /></el-icon>
                {{ t('instance.switchGroup') }}
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
                {{ t('common.delete') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-area">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 下线弹窗 -->
    <el-dialog
      v-model="offlineDialogVisible"
      :title="t('instance.offlineInstance')"
      width="500px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <!-- 最后实例警告（测试环境临时禁用） -->
      <!-- <el-alert
        v-if="isLastInstance"
        :title="t('instance.lastInstanceWarning')"
        type="error"
        :closable="false"
        show-icon
        class="offline-warning-alert"
      /> -->
      <!-- 剩余实例提示 -->
      <el-alert
        v-if="remainingOnlineCount <= 2"
        :title="t('instance.remainingInstancesWarning', { count: remainingOnlineCount })"
        type="warning"
        :closable="false"
        show-icon
        class="offline-warning-alert"
      />

      <el-form :model="offlineForm" label-width="100px" class="offline-form">
        <el-form-item :label="t('common.instanceId')">
          <el-input v-model="offlineForm.instanceId" disabled />
        </el-form-item>
        <!-- 下线模式选择 -->
        <el-form-item :label="t('instance.offlineType')">
          <el-radio-group v-model="offlineForm.mode">
            <el-radio value="graceful">
              {{ t('instance.gracefulOffline') }}
              <el-tooltip :content="t('instance.gracefulOfflineDesc', { seconds: 30 })" placement="top">
                <el-icon size="14" style="margin-left: 4px"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-radio>
            <el-radio value="force">
              {{ t('instance.forceOffline') }}
              <el-tooltip :content="t('instance.forceOfflineWarning')" placement="top">
                <el-icon size="14" style="margin-left: 4px" color="#e6a23c"><WarningFilled /></el-icon>
              </el-tooltip>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('instance.offlineReason')">
          <el-input
            v-model="offlineForm.reason"
            type="textarea"
            :rows="3"
            :placeholder="t('instance.offlineReasonPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offlineDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          :type="offlineForm.mode === 'graceful' ? 'primary' : 'warning'"
          :loading="submitting"
          @click="confirmOffline"
        >
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 切换分组弹窗 -->
    <el-dialog
      v-model="switchGroupDialogVisible"
      :title="t('instance.switchGroup')"
      width="500px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-form :model="switchGroupForm" label-width="100px">
        <el-form-item :label="t('common.instanceId')">
          <el-input v-model="switchGroupForm.instanceId" disabled />
        </el-form-item>
        <el-form-item :label="t('instance.currentGroup')">
          <el-input v-model="switchGroupForm.currentGroup" disabled />
        </el-form-item>
        <el-form-item :label="t('instance.targetGroup')" required>
          <el-select v-model="switchGroupForm.targetGroupKey" :placeholder="t('common.pleaseSelect')" style="width: 100%">
            <el-option
              v-for="group in groupOptions"
              :key="group.groupKey"
              :label="group.groupName"
              :value="group.groupKey"
              :disabled="group.groupKey === switchGroupForm.currentGroup"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="switchGroupDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmSwitchGroup">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Monitor,
  CircleCheck,
  SuccessFilled,
  Cpu,
  Refresh,
  Search,
  RefreshLeft,
  View,
  SwitchButton,
  Delete,
  InfoFilled,
  WarningFilled,
  Switch,
} from '@element-plus/icons-vue'
import {
  queryInstanceList,
  deleteInstance,
  onlineInstance,
  offlineInstance,
  gracefulOfflineInstance,
  refreshInstanceStatus,
  switchInstanceGroup,
  type InstanceInfo,
  INSTANCE_STATUS,
} from '@/api/instance'
import { getEnabledRouteGroups, type RouteGroup } from '@/api/routeGroup'
import { usePermission } from '@/composables/usePermission'
import { useInstanceStatus } from '@/composables/useInstanceStatus'

defineOptions({ name: 'InstanceManagement' })

const { t } = useI18n()
const router = useRouter()
const { hasPermission: checkPermission } = usePermission()

// ==================== SSE 实时状态 ====================

const {
  instances: sseInstances,
  stats: sseStats,
  isConnected: sseConnected,
} = useInstanceStatus({
  onStatusChange: (data) => {
    // SSE 状态变化时更新统计数据
    if (data.stats) {
      statistics.totalInstances = data.stats.total
      statistics.onlineInstances = data.stats.online
      statistics.healthyInstances = data.stats.healthy
      statistics.avgCpuUsage = Math.round(data.stats.avgCpuUsage)
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
const updateInstanceStatusFromSse = (data: { instances: Array<{ instanceId: string; status: number; healthStatus: string; cpuUsage?: number; heapUsagePercent?: number }>; changedInstanceIds?: string[] }) => {
  if (!data.changedInstanceIds?.length) return

  const changedIds = new Set(data.changedInstanceIds)
  instanceList.value = instanceList.value.map((instance) => {
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

// ==================== 数据状态 ====================

const loading = ref(false)
const refreshing = ref(false)
const submitting = ref(false)
const instanceList = ref<InstanceInfo[]>([])
const groupOptions = ref<RouteGroup[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const searchForm = reactive({
  serviceId: '',
  host: '',
  status: undefined as number | undefined,
  groupKey: '',
})

const statistics = reactive({
  totalInstances: 0,
  onlineInstances: 0,
  healthyInstances: 0,
  avgCpuUsage: 0,
})

// ==================== 弹窗状态 ====================

const offlineDialogVisible = ref(false)

const currentInstance = ref<InstanceInfo | null>(null)

// ==================== 下线实例保护 ====================

/**
 * 剩余在线实例数量（从 SSE 获取实时数据）
 */
const remainingOnlineCount = computed(() => {
  return statistics.onlineInstances
})

/**
 * 是否为最后一个在线实例（即将下线的实例是最后一个在线实例）
 */
const isLastInstance = computed(() => {
  // 如果当前实例是在线或排空状态，且在线数量只有1个，则为最后一个
  if (currentInstance.value && (currentInstance.value.status === INSTANCE_STATUS.ONLINE || currentInstance.value.status === INSTANCE_STATUS.DRAINING)) {
    return remainingOnlineCount.value <= 1
  }
  return false
})

const offlineForm = reactive({
  instanceId: '',
  reason: '',
  mode: 'force' as 'force' | 'graceful',
})

// ==================== 切换分组弹窗 ====================

const switchGroupDialogVisible = ref(false)

const switchGroupForm = reactive({
  instanceId: '',
  currentGroup: '',
  targetGroupKey: '',
})

const handleSwitchGroup = (row: InstanceInfo) => {
  switchGroupForm.instanceId = row.instanceId
  switchGroupForm.currentGroup = row.groupKey || 'default'
  switchGroupForm.targetGroupKey = ''
  switchGroupDialogVisible.value = true
}

const confirmSwitchGroup = async () => {
  if (!switchGroupForm.targetGroupKey) {
    ElMessage.warning(t('common.pleaseSelect'))
    return
  }

  submitting.value = true
  try {
    await switchInstanceGroup({
      instanceId: switchGroupForm.instanceId,
      targetGroupKey: switchGroupForm.targetGroupKey,
    })
    ElMessage.success(t('instance.switchGroupSuccess'))
    switchGroupDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('Switch group error:', error)
  } finally {
    submitting.value = false
  }
}

// ==================== 辅助方法 ====================

const getStatusType = (status: number): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  switch (status) {
    case INSTANCE_STATUS.ONLINE:
      return 'success'
    case INSTANCE_STATUS.OFFLINE:
      return 'danger'
    case INSTANCE_STATUS.SHUTDOWN:
      return 'warning'
    case INSTANCE_STATUS.DRAINING:
      return 'primary'
    default:
      return 'info'
  }
}

const getStatusText = (status: number): string => {
  switch (status) {
    case INSTANCE_STATUS.ONLINE:
      return t('common.statusOnline')
    case INSTANCE_STATUS.OFFLINE:
      return t('common.statusOffline')
    case INSTANCE_STATUS.SHUTDOWN:
      return t('common.statusShutdown')
    case INSTANCE_STATUS.DRAINING:
      return t('instance.statusDraining')
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

// ==================== 数据加载 ====================

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm,
    }
    const result = await queryInstanceList(params)
    instanceList.value = result?.rows || []
    pagination.total = result?.total || 0

    // 计算统计数据
    const onlineCount = instanceList.value.filter(i => i.status === INSTANCE_STATUS.ONLINE).length
    statistics.totalInstances = pagination.total
    statistics.onlineInstances = onlineCount
    statistics.healthyInstances = onlineCount // 简化处理，在线即健康
    statistics.avgCpuUsage = 0 // 需要从监控接口获取
  } catch (error) {
    console.error('Load data error:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ==================== 刷新（从Nacos同步状态） ====================

const handleRefresh = async () => {
  refreshing.value = true
  try {
    // 先从 Nacos 同步实例状态到数据库
    await refreshInstanceStatus()
    // 再加载列表
    await loadData()
    ElMessage.success(t('common.success'))
  } catch (error) {
    console.error('Refresh error:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    refreshing.value = false
  }
}

// ==================== 搜索 ====================

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.serviceId = ''
  searchForm.host = ''
  searchForm.status = undefined
  searchForm.groupKey = ''
  pagination.pageNum = 1
  loadData()
}

// ==================== 分页 ====================

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadData()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  loadData()
}

// ==================== 详情 ====================

const handleViewDetail = (row: InstanceInfo) => {
  // 跳转到详情页面，作为新标签页打开
  router.push({
    path: '/instance/detail',
    query: {
      id: row.id,
      instanceId: row.instanceId,
    },
  })
}

// ==================== 删除 ====================

const handleDelete = (row: InstanceInfo) => {
  ElMessageBox.confirm(t('instance.deleteConfirm'), t('message.tips'), { type: 'warning' })
    .then(async () => {
      try {
        await deleteInstance({ id: row.id })
        ElMessage.success(t('common.success'))
        loadData()
      } catch (error) {
        console.error('Delete instance error:', error)
      }
    })
}

// ==================== 上线/下线 ====================

const handleOffline = (row: InstanceInfo) => {
  // 设置当前操作的实例，用于判断是否为最后一个在线实例
  currentInstance.value = row
  offlineForm.instanceId = row.instanceId
  offlineForm.reason = ''
  offlineForm.mode = 'graceful'  // 默认优雅下线
  offlineDialogVisible.value = true
}

const confirmOffline = async () => {
  submitting.value = true
  try {
    if (offlineForm.mode === 'graceful') {
      await gracefulOfflineInstance(offlineForm)
    } else {
      await offlineInstance(offlineForm)
    }
    ElMessage.success(t('common.success'))
    offlineDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('Offline instance error:', error)
  } finally {
    submitting.value = false
  }
}

const handleOnline = (row: InstanceInfo) => {
  ElMessageBox.confirm(t('instance.onlineConfirm'), t('message.tips'), { type: 'info' })
    .then(async () => {
      try {
        await onlineInstance({ instanceId: row.instanceId })
        ElMessage.success(t('common.success'))
        loadData()
      } catch (error) {
        console.error('Online instance error:', error)
      }
    })
}

// ==================== 加载分组列表 ====================

const loadGroupOptions = async () => {
  try {
    const result = await getEnabledRouteGroups()
    groupOptions.value = result || []
  } catch (error) {
    console.error('Load group options error:', error)
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
  loadGroupOptions()
  // SSE 连接由 MainLayout 统一管理，无需在组件中手动连接
})

// onUnmounted 不再需要，SSE 连接由 MainLayout 管理
</script>

<style scoped lang="scss">
.instance-page {
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
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 22px;
      flex-shrink: 0;

      &.total { background: linear-gradient(135deg, #409eff, #66b1ff); }
      &.online { background: linear-gradient(135deg, #67c23a, #85ce61); }
      &.healthy { background: linear-gradient(135deg, #19be6b, #47cb89); }
      &.cpu { background: linear-gradient(135deg, #e6a23c, #f0c78a); }
    }

    .stat-info {
      flex: 1;
      min-width: 0;

      .stat-value {
        font-size: 24px;
        font-weight: 600;
        color: var(--text-color-primary);
        line-height: 1.2;

        .stat-unit {
          font-size: 12px;
          font-weight: normal;
          color: var(--text-color-secondary);
          margin-left: 2px;
        }
      }

      .stat-label {
        font-size: 13px;
        color: var(--text-color-regular);
        margin-top: 4px;
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
      display: flex;
      flex-direction: column;
    }
  }

  .search-area {
    flex-shrink: 0;
    padding-bottom: 8px;

    .search-form {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;

      .right-actions {
        margin-left: auto;
      }
    }
  }

  .instance-table {
    flex: 1;
    min-height: 0;

    .instance-id {
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 13px;
    }
  }

  .pagination-area {
    flex-shrink: 0;
    padding-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .empty-text {
    color: var(--el-text-color-placeholder);
  }

  .operation-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 8px;

    :deep(.el-button) {
      margin: 0;
    }
  }

  .offline-warning-alert {
    margin-bottom: 16px;
  }

  .offline-form {
    margin-top: 0;
  }
}
</style>