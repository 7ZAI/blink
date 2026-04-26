<template>
  <div class="instance-page">
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
        <el-table-column :label="t('instance.storageMode')" width="100" align="center">
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
        <el-table-column :label="t('instance.healthStatus')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getHealthStatusType(row.healthStatus)" effect="light" size="small">
              {{ row.healthStatus || 'UNKNOWN' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('instance.registryStatus')" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.inRegistry" type="success" effect="plain" size="small">
              {{ t('instance.inRegistry') }}
            </el-tag>
            <el-tag v-else type="info" effect="plain" size="small">
              {{ t('instance.notInRegistry') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('instance.statusConflict')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-text v-if="row.statusConflict" type="warning" size="small">
              <el-icon><WarningFilled /></el-icon>
              {{ row.statusConflict }}
            </el-text>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('instance.offlineType')" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.offlineType" :type="getOfflineTypeColor(row.offlineType)" effect="plain" size="small">
              {{ getOfflineTypeText(row.offlineType) }}
            </el-tag>
            <span v-else class="empty-text">-</span>
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

    <!-- 上线任务进度弹窗 -->
    <BlinkTaskDialog
      v-model="onlineTaskState.visible"
      :status="onlineTaskState.status"
      :progress="onlineTaskState.progress"
      :title="onlineTaskState.title"
      :message="onlineTaskState.message"
      :result="onlineTaskState.result"
      :error="onlineTaskState.error"
      :elapsed-time="onlineTaskState.elapsedTime"
      :cancellable="false"
      :backgroundable="false"
    />

    <!-- 下线进度弹窗 -->
    <BlinkTaskDialog
      v-model="offlineTaskState.visible"
      :status="offlineTaskState.status"
      :progress="offlineTaskState.progress"
      :title="offlineTaskState.title"
      :message="offlineTaskState.message"
      :result="offlineTaskState.result"
      :error="offlineTaskState.error"
      :elapsed-time="offlineTaskState.elapsedTime"
      :cancellable="false"
      :backgroundable="false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Search,
  RefreshLeft,
  View,
  SwitchButton,
  Delete,
  InfoFilled,
  WarningFilled,
  Switch,
  CircleCheck,
} from '@element-plus/icons-vue'
import {
  queryInstanceList,
  deleteInstance,
  onlineInstance,
  offlineInstance,
  gracefulOfflineInstance,
  refreshInstanceStatus,
  switchInstanceGroup,
  getInstanceDetailWithMetrics,
  type InstanceInfo,
  INSTANCE_STATUS,
} from '@/api/instance'
import { getEnabledRouteGroups, type RouteGroup } from '@/api/routeGroup'
import { usePermission } from '@/composables/usePermission'
import {
  BlinkTaskDialog,
  useTaskRunner,
  type ProgressUpdate,
} from '@blink/components'

defineOptions({ name: 'InstanceManagement' })

const { t } = useI18n()
const router = useRouter()
const { hasPermission: checkPermission } = usePermission()

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

// ==================== 弹窗状态 ====================

const offlineDialogVisible = ref(false)

const currentInstance = ref<InstanceInfo | null>(null)

const offlineForm = reactive({
  instanceId: '',
  reason: '',
  mode: 'force' as 'force' | 'graceful',
})

// ==================== 上线任务进度弹窗 ====================

const { state: onlineTaskState, start: startOnlineTask } = useTaskRunner({
  onComplete: () => {
    loadData()
  },
  onError: (error: Error) => {
    ElMessage.error(t('instance.onlineFailed') + ': ' + error.message)
  },
})

// ==================== 下线任务进度弹窗 ====================

const { state: offlineTaskState, start: startOfflineTask } = useTaskRunner({
  onComplete: () => {
    loadData()
  },
  onError: (error: Error) => {
    ElMessage.error(t('instance.offlineFailed') + ': ' + error.message)
  },
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
  } catch (error: any) {
    console.error('Switch group error:', error)
    ElMessage.error(error?.message || t('common.failed'))
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

/**
 * 健康状态颜色
 */
const getHealthStatusType = (healthStatus: string): 'success' | 'danger' | 'warning' | 'info' | 'primary' => {
  switch (healthStatus) {
    case 'UP':
      return 'success'
    case 'DOWN':
      return 'danger'
    case 'OFFLINE':
      return 'warning'
    case 'DRAINING':
      return 'primary'
    default:
      return 'info'
  }
}

/**
 * 下线类型颜色
 */
const getOfflineTypeColor = (offlineType: string): 'primary' | 'warning' | 'danger' | 'info' => {
  switch (offlineType) {
    case 'MANUAL':
      return 'primary'
    case 'FAULT':
      return 'danger'
    case 'DRAINING':
      return 'warning'
    default:
      return 'info'
  }
}

/**
 * 下线类型文本
 */
const getOfflineTypeText = (offlineType: string): string => {
  switch (offlineType) {
    case 'MANUAL':
      return t('instance.offlineTypeManual')
    case 'FAULT':
      return t('instance.offlineTypeFault')
    case 'DRAINING':
      return t('instance.offlineTypeDraining')
    default:
      return offlineType
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
  // 关闭确认弹窗
  offlineDialogVisible.value = false

  const instanceId = offlineForm.instanceId
  const mode = offlineForm.mode
  const reason = offlineForm.reason

  // 使用任务弹窗展示下线进度
  if (mode === 'graceful') {
    // 优雅下线：多步骤流程
    await startOfflineTask({
      task: async (onProgress: (update: ProgressUpdate) => void, signal?: AbortSignal) => {
        // 步骤 0: 发送下线请求
        onProgress({ step: 0, stepMessage: t('instance.offlineStepSending') })
        await gracefulOfflineInstance({ instanceId, reason })

        // 步骤 1: 等待流量排空
        onProgress({ step: 1, stepMessage: t('instance.offlineStepDraining') })

        // 轮询检查实例状态，等待从 DRAINING 变为 SHUTDOWN
        const maxPollCount = 60 // 最多轮询 60 次（约 30 秒）
        const pollInterval = 500 // 每 500ms 轮询一次

        for (let i = 0; i < maxPollCount; i++) {
          if (signal?.aborted) {
            return null
          }

          // 获取实例详情以检查状态
          const instance = instanceList.value.find(item => item.instanceId === instanceId)
          if (instance) {
            const detail = await getInstanceDetailWithMetrics({ id: instance.id })
            const status = detail?.instanceInfo?.status

            // 状态变为 SHUTDOWN(2)，说明下线完成
            if (status === INSTANCE_STATUS.SHUTDOWN) {
              onProgress({ step: 2, stepMessage: t('instance.offlineStepComplete') })
              return { status: 'shutdown', instanceId }
            }

            // 更新等待进度
            onProgress({
              step: 1,
              stepMessage: t('instance.offlineStepDraining') + ` (${i + 1}/${maxPollCount})`,
            })
          }

          // 等待轮询间隔
          await new Promise(resolve => setTimeout(resolve, pollInterval))
        }

        // 超时后仍返回成功，让用户自行确认
        onProgress({ step: 2, stepMessage: t('instance.offlineStepTimeout') })
        return { status: 'timeout', instanceId }
      },
      title: t('instance.gracefulOfflineTitle'),
      message: t('instance.offlineStarting'),
      steps: [
        t('instance.offlineStepSending'),
        t('instance.offlineStepDraining'),
        t('instance.offlineStepComplete'),
      ],
      backgroundable: false,
      onCompleteBehavior: 'show-result',
    })
  } else {
    // 强制下线：轮询等待状态变化
    await startOfflineTask({
      task: async (onProgress: (update: ProgressUpdate) => void, signal?: AbortSignal) => {
        // 步骤 0: 发送下线请求
        onProgress({ step: 0, stepMessage: t('instance.offlineStepSending') })
        await offlineInstance({ instanceId, reason })

        // 步骤 1: 等待状态变为 SHUTDOWN
        onProgress({ step: 1, stepMessage: t('instance.offlineStepWaiting') })

        // 轮询检查实例状态，等待变为 SHUTDOWN
        const maxPollCount = 60 // 最多轮询 60 次（约 30 秒）
        const pollInterval = 500 // 每 500ms 轮询一次

        for (let i = 0; i < maxPollCount; i++) {
          if (signal?.aborted) {
            return null
          }

          // 获取实例详情以检查状态
          const instance = instanceList.value.find(item => item.instanceId === instanceId)
          if (instance) {
            const detail = await getInstanceDetailWithMetrics({ id: instance.id })
            const status = detail?.instanceInfo?.status

            // 状态变为 SHUTDOWN(2)，说明下线完成
            if (status === INSTANCE_STATUS.SHUTDOWN) {
              onProgress({ step: 2, stepMessage: t('instance.offlineStepComplete') })
              return { status: 'shutdown', instanceId, mode: 'force' }
            }

            // 更新等待进度
            onProgress({
              step: 1,
              stepMessage: t('instance.offlineStepWaiting') + ` (${i + 1}/${maxPollCount})`,
            })
          }

          // 等待轮询间隔
          await new Promise(resolve => setTimeout(resolve, pollInterval))
        }

        // 超时后仍返回成功，让用户自行确认
        onProgress({ step: 2, stepMessage: t('instance.offlineStepTimeout') })
        return { status: 'timeout', instanceId, mode: 'force' }
      },
      title: t('instance.forceOfflineTitle'),
      message: t('instance.offlineStarting'),
      steps: [
        t('instance.offlineStepSending'),
        t('instance.offlineStepWaiting'),
        t('instance.offlineStepComplete'),
      ],
      backgroundable: false,
      onCompleteBehavior: 'show-result',
    })
  }
}

const handleOnline = async (row: InstanceInfo) => {
  const instanceId = row.instanceId
  const instanceDbId = row.id

  // 使用任务弹窗展示上线进度
  await startOnlineTask({
    task: async (onProgress: (update: ProgressUpdate) => void, signal?: AbortSignal) => {
      // 步骤 0: 发送上线请求
      onProgress({ step: 0, stepMessage: t('instance.onlineStepSending') })
      await onlineInstance({ instanceId })

      // 步骤 1: 等待健康状态变为 UP
      onProgress({ step: 1, stepMessage: t('instance.onlineStepWaiting') })

      // 轮询检查实例健康状态，等待变为 UP
      const maxPollCount = 60 // 最多轮询 60 次（约 30 秒）
      const pollInterval = 500 // 每 500ms 轮询一次

      for (let i = 0; i < maxPollCount; i++) {
        if (signal?.aborted) {
          return null
        }

        // 获取实例详情以检查健康状态
        const detail = await getInstanceDetailWithMetrics({ id: instanceDbId })
        const healthStatus = detail?.healthDetail?.status

        // 健康状态变为 UP，说明上线完成
        if (healthStatus === 'UP') {
          onProgress({ step: 2, stepMessage: t('instance.onlineStepComplete') })
          return { status: 'online', instanceId, healthStatus }
        }

        // 更新等待进度
        onProgress({
          step: 1,
          stepMessage: t('instance.onlineStepWaiting') + ` (${i + 1}/${maxPollCount})`,
        })

        // 等待轮询间隔
        await new Promise(resolve => setTimeout(resolve, pollInterval))
      }

      // 超时后仍返回成功，让用户自行确认
      onProgress({ step: 2, stepMessage: t('instance.onlineStepTimeout') })
      return { status: 'timeout', instanceId }
    },
    title: t('instance.onlineTitle'),
    message: t('instance.onlineStarting'),
    steps: [
      t('instance.onlineStepSending'),
      t('instance.onlineStepWaiting'),
      t('instance.onlineStepComplete'),
    ],
    backgroundable: false,
    onCompleteBehavior: 'show-result',
  })
}

// ==================== 加载分组列表 ====================

const loadGroupOptions = async () => {
  try {
    const result = await getEnabledRouteGroups()
    const groups = result || []

    // 确保 default 分组始终存在
    const hasDefault = groups.some(g => g.groupKey === 'default')
    if (!hasDefault) {
      groups.unshift({
        groupKey: 'default',
        groupName: '默认分组',
        status: 0,
        createTime: '',
        updateTime: '',
        remark: '',
      })
    }

    groupOptions.value = groups
  } catch (error) {
    console.error('Load group options error:', error)
  }
}

// ==================== 初始化加载（先刷新再查询） ====================

const initLoad = async () => {
  loading.value = true
  try {
    // 先从 Nacos 同步实例状态到数据库
    await refreshInstanceStatus()
    // 再加载列表
    await loadData()
  } catch (error) {
    console.error('Init load error:', error)
    // 刷新失败时仍然尝试加载列表
    await loadData()
  } finally {
    loading.value = false
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  initLoad()
  loadGroupOptions()
})
</script>

<style scoped lang="scss">
.instance-page {
  height: 100%;
  display: flex;
  flex-direction: column;

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

  .offline-form {
    margin-top: 0;
  }
}
</style>