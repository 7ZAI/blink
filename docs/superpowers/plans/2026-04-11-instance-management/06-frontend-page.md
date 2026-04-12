# Task 6: 前端实例管理页面

**依赖:** Task 5 (前端 API 模块)

**目标:** 创建实例管理页面，包含统计卡片、列表、详情弹窗、新增/编辑弹窗

---

## 文件清单

- 新增: `frontend/packages/gateway-admin/src/views/instance/index.vue`
- 修改: `frontend/packages/gateway-admin/src/router/index.ts`

---

### Task 6.1: 添加路由配置

- [ ] **Step 1: 在 router/index.ts 中添加实例管理路由**

文件: `frontend/packages/gateway-admin/src/router/index.ts`

在 children 数组中添加路由配置（在 monitor 路由之后）：

```typescript
      {
        path: 'instance',
        name: 'Instance',
        component: () => import('@/views/instance/index.vue'),
        meta: { title: 'instance.title' },
      },
```

---

### Task 6.2: 创建实例管理页面

- [ ] **Step 2: 创建 instance/index.vue 页面组件**

文件: `frontend/packages/gateway-admin/src/views/instance/index.vue`

```vue
<template>
  <div class="instance-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon total">
              <el-icon><Server /></el-icon>
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
      <template #header>
        <div class="card-header">
          <span class="card-title">{{ t('instance.title') }}</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              {{ t('instance.addInstance') }}
            </el-button>
            <el-button @click="loadData">
              <el-icon><Refresh /></el-icon>
              {{ t('common.refresh') }}
            </el-button>
          </div>
        </div>
      </template>

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
            <el-select v-model="searchForm.status" :placeholder="t('common.pleaseSelect')" clearable>
              <el-option :label="t('instance.statusOnline')" :value="0" />
              <el-option :label="t('instance.statusOffline')" :value="1" />
              <el-option :label="t('instance.statusShutdown')" :value="2" />
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
        </el-form>
      </div>

      <!-- 表格 -->
      <el-table :data="instanceList" v-loading="loading" stripe class="instance-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="instanceId" :label="t('instance.instanceId')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="instance-id">{{ row.instanceId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="serviceId" :label="t('instance.serviceId')" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.serviceId }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" :label="t('instance.host')" width="140" />
        <el-table-column prop="port" :label="t('instance.port')" width="80" align="center" />
        <el-table-column :label="t('common.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">
              {{ row.statusDesc || getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="onlineTime" :label="t('instance.onlineTime')" width="170">
          <template #default="{ row }">
            <span v-if="row.onlineTime">{{ formatTime(row.onlineTime) }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" width="220" fixed="right">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button type="primary" link size="small" @click="handleViewDetail(row)">
                <el-icon><View /></el-icon>
                {{ t('common.detail') }}
              </el-button>
              <template v-if="row.status === 0">
                <el-button type="warning" link size="small" @click="handleOffline(row)">
                  <el-icon><SwitchButton /></el-icon>
                  {{ t('instance.offlineInstance') }}
                </el-button>
              </template>
              <template v-else-if="row.status === 2">
                <el-button type="success" link size="small" @click="handleOnline(row)">
                  <el-icon><CircleCheck /></el-icon>
                  {{ t('instance.onlineInstance') }}
                </el-button>
              </template>
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
                {{ t('common.edit') }}
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

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="t('instance.instanceDetail') + ' - ' + (currentInstance?.instanceId || '')"
      width="800px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="detail-dialog"
    >
      <el-tabs v-model="detailActiveTab" class="detail-tabs">
        <!-- 基本信息 -->
        <el-tab-pane :label="t('instance.basicInfo')" name="basic">
          <el-descriptions :column="2" border v-if="currentInstance">
            <el-descriptions-item :label="t('instance.instanceId')">
              {{ currentInstance.instanceId }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('instance.serviceId')">
              {{ currentInstance.serviceId }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('instance.host')">
              {{ currentInstance.host }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('instance.port')">
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
            <el-descriptions-item :label="t('instance.onlineTime')">
              {{ currentInstance.onlineTime ? formatTime(currentInstance.onlineTime) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('instance.metadata')" :span="2">
              <el-input v-if="currentInstance.metadata" :model-value="currentInstance.metadata" type="textarea" :rows="3" readonly />
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>
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
      <template #footer>
        <el-button @click="detailDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="formType === 'add' ? t('instance.addInstance') : t('instance.editInstance')"
      width="500px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-form ref="formRef" :model="instanceForm" :rules="formRules" label-width="100px">
        <el-form-item :label="t('instance.serviceId')" prop="serviceId">
          <el-input v-model="instanceForm.serviceId" :placeholder="t('common.pleaseInput')" />
        </el-form-item>
        <el-form-item :label="t('instance.host')" prop="host">
          <el-input v-model="instanceForm.host" :placeholder="t('instance.hostPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('instance.port')" prop="port">
          <el-input-number v-model="instanceForm.port" :min="1" :max="65535" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('instance.metadata')">
          <el-input
            v-model="instanceForm.metadata"
            type="textarea"
            :rows="3"
            :placeholder="t('instance.metadataPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitForm">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Server,
  CircleCheck,
  SuccessFilled,
  Cpu,
  Plus,
  Refresh,
  Search,
  RefreshLeft,
  View,
  SwitchButton,
  Edit,
  Delete,
} from '@element-plus/icons-vue'
import {
  queryInstanceList,
  saveInstance,
  deleteInstance,
  getInstanceDetailWithMetrics,
  onlineInstance,
  offlineInstance,
  type InstanceInfo,
  type InstanceDetail,
  INSTANCE_STATUS,
} from '@/api/instance'

defineOptions({ name: 'InstanceManagement' })

const { t } = useI18n()

// ==================== 数据状态 ====================

const loading = ref(false)
const submitting = ref(false)
const instanceList = ref<InstanceInfo[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const searchForm = reactive({
  serviceId: '',
  host: '',
  status: undefined as number | undefined,
})

const statistics = reactive({
  totalInstances: 0,
  onlineInstances: 0,
  healthyInstances: 0,
  avgCpuUsage: 0,
})

// ==================== 弹窗状态 ====================

const detailDialogVisible = ref(false)
const formDialogVisible = ref(false)
const offlineDialogVisible = ref(false)
const detailActiveTab = ref('basic')

const currentInstance = ref<InstanceInfo | null>(null)
const instanceDetail = ref<InstanceDetail | null>(null)

const formRef = ref<FormInstance>()
const formType = ref<'add' | 'edit'>('add')
const instanceForm = reactive({
  id: undefined as number | undefined,
  serviceId: '',
  host: '',
  port: 8080,
  metadata: '',
})

const offlineForm = reactive({
  instanceId: '',
  reason: '',
})

const formRules = computed<FormRules>(() => ({
  serviceId: [{ required: true, message: t('common.pleaseInput') + t('instance.serviceId'), trigger: 'blur' }],
  host: [{ required: true, message: t('common.pleaseInput') + t('instance.host'), trigger: 'blur' }],
  port: [{ required: true, message: t('common.pleaseInput') + t('instance.port'), trigger: 'change' }],
}))

let refreshTimer: number | null = null

// ==================== 辅助方法 ====================

const getStatusType = (status: number): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
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

const getStatusText = (status: number): string => {
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

// ==================== 搜索 ====================

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.serviceId = ''
  searchForm.host = ''
  searchForm.status = undefined
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

const handleViewDetail = async (row: InstanceInfo) => {
  currentInstance.value = row
  detailActiveTab.value = 'basic'
  detailDialogVisible.value = true

  try {
    const detail = await getInstanceDetailWithMetrics({ id: row.id })
    instanceDetail.value = detail
  } catch (error) {
    console.error('Get instance detail error:', error)
    instanceDetail.value = null
  }
}

// ==================== 新增/编辑 ====================

const handleAdd = () => {
  formType.value = 'add'
  instanceForm.id = undefined
  instanceForm.serviceId = 'gateway-app'
  instanceForm.host = ''
  instanceForm.port = 8080
  instanceForm.metadata = ''
  formDialogVisible.value = true
}

const handleEdit = (row: InstanceInfo) => {
  formType.value = 'edit'
  instanceForm.id = row.id
  instanceForm.serviceId = row.serviceId
  instanceForm.host = row.host
  instanceForm.port = row.port
  instanceForm.metadata = row.metadata || ''
  formDialogVisible.value = true
}

const handleSubmitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    await saveInstance(instanceForm)
    ElMessage.success(t('common.success'))
    formDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('Save instance error:', error)
  } finally {
    submitting.value = false
  }
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
  offlineForm.instanceId = row.instanceId
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

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
  refreshTimer = window.setInterval(loadData, 60000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
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

    .header-actions {
      display: flex;
      gap: 8px;
    }
  }

  .search-area {
    flex-shrink: 0;
    padding-bottom: 16px;

    .search-form {
      display: flex;
      flex-wrap: wrap;
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
    color: var(--text-color-placeholder);
  }

  .operation-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.detail-dialog {
  .detail-tabs {
    :deep(.el-tabs__content) {
      padding: 16px 0;
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
      margin-bottom: 20px;

      h4 {
        margin-bottom: 12px;
        font-size: 14px;
        font-weight: 500;
        color: var(--text-color-primary);
      }
    }

    .metric-item {
      text-align: center;
      padding: 12px;
      background: var(--bg-color);
      border-radius: 8px;

      .metric-label {
        font-size: 12px;
        color: var(--text-color-secondary);
        margin-bottom: 8px;
      }

      .metric-value {
        font-size: 18px;
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

  :deep(.el-descriptions__label) {
    width: 100px;
  }
}
</style>
```

---

### Task 6.3: 提交更改

- [ ] **Step 3: Git 提交**

```bash
git add frontend/packages/gateway-admin/src/views/instance/index.vue
git add frontend/packages/gateway-admin/src/router/index.ts
git commit -m "feat(instance): 新增实例管理页面

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验收检查

| 检查项 | 状态 |
|--------|------|
| 路由配置已添加 | [ ] |
| 页面组件已创建 | [ ] |
| 统计卡片显示正确 | [ ] |
| 搜索和筛选功能正常 | [ ] |
| 分页功能正常 | [ ] |
| 详情弹窗显示完整指标 | [ ] |
| 新增/编辑弹窗正常 | [ ] |
| 上线/下线功能正常 | [ ] |
| Git 提交成功 | [ ] |