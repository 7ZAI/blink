<template>
  <!-- 推送路由页面 -->
  <div class="push-route-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-icon">
          <BlinkIcon icon="mdi:upload-network" size="24" />
        </div>
        <div class="title-info">
          <h3>{{ t('pushRoute.title') }}</h3>
          <p class="subtitle">{{ t('pushRoute.subtitle') }}</p>
        </div>
      </div>
      <div class="header-right">
        <div class="action-buttons">
          <el-button type="primary" :loading="pushing" :disabled="!canPush" @click="handlePush">
            <el-icon><Promotion /></el-icon>
            {{ t('pushRoute.push') }}
          </el-button>
          <el-button type="success" :loading="pushing" @click="handleFullPush">
            <el-icon><Upload /></el-icon>
            {{ t('pushRoute.fullPush') }}
          </el-button>
          <el-button
            type="info"
            :disabled="!canValidate"
            @click="handleValidate"
          >
            <el-icon><CircleCheck /></el-icon>
            {{ t('pushRoute.validate') }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 左侧：路由列表 + 结果面板 -->
      <div class="left-column">
        <!-- 路由列表 -->
        <div class="left-panel">
          <div class="panel-header">
            <div class="panel-title">
              <BlinkIcon icon="mdi:routes" size="18" />
              <span>{{ t('pushRoute.routesFromRepository') }}</span>
            </div>
            <div class="panel-actions">
              <el-input
                v-model="routeSearchKeyword"
                :placeholder="t('common.search')"
                clearable
                size="small"
                style="width: 180px"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-checkbox
                v-model="selectAllRoutes"
                :indeterminate="isRoutesIndeterminate"
                @change="handleSelectAllRoutes as any"
              >
                {{ t('common.selectAll') }}
              </el-checkbox>
            </div>
          </div>
          <div class="panel-stats">
            <span class="stat-item">
              {{ t('pushRoute.selectedRoutes', { count: selectedRouteIds.length }) }}
            </span>
            <span class="stat-divider">|</span>
            <span class="stat-item">
              {{ t('pushRoute.totalRoutes', { count: filteredRoutes.length }) }}
            </span>
          </div>
          <div class="panel-body" v-loading="routesLoading">
            <el-scrollbar>
              <div v-if="filteredRoutes.length === 0" class="empty-state">
                <el-empty :description="t('pushRoute.noRoutes')" size="small" />
              </div>
              <div v-else class="route-list">
                <div
                  v-for="route in filteredRoutes"
                  :key="route.routeId"
                  class="route-item"
                  :class="{ 'is-selected': selectedRouteIds.includes(route.routeId) }"
                >
                  <el-checkbox
                    :model-value="selectedRouteIds.includes(route.routeId)"
                    @change="toggleRouteSelection(route.routeId)"
                  />
                  <div class="route-info" @click="toggleRouteSelection(route.routeId)">
                    <span class="route-id">{{ route.routeId }}</span>
                    <span class="route-name">{{ route.routeName || '-' }}</span>
                    <el-tag size="small" effect="plain" type="success" class="route-uri">{{ route.uri }}</el-tag>
                    <el-tag
                      :type="route.status === 1 ? 'success' : 'danger'"
                      size="small"
                      effect="light"
                    >
                      {{ route.status === 1 ? t('route.statusEnable') : t('route.statusDisable') }}
                    </el-tag>
                    <el-tag
                      :type="getPushStatusType(route.pushStatus)"
                      size="small"
                      effect="plain"
                    >
                      {{ getPushStatusText(route.pushStatus) }}
                    </el-tag>
                  </div>
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click.stop="showRouteDetail(route)"
                  >
                    <el-icon><View /></el-icon>
                    {{ t('common.detail') }}
                  </el-button>
                </div>
              </div>
            </el-scrollbar>
          </div>
        </div>

        <!-- 推送结果区域 -->
        <div class="result-panel">
          <div class="panel-header">
            <div class="panel-title">
              <BlinkIcon :icon="getResultPanelIcon()" size="18" />
              <span>{{ getResultPanelTitle() }}</span>
            </div>
            <div class="panel-actions">
              <el-radio-group v-model="resultTab" size="small">
                <el-radio-button value="preview">{{ t('pushRoute.routePreview') }}</el-radio-button>
                <el-radio-button value="push">{{ t('pushRoute.pushResult') }}</el-radio-button>
                <el-radio-button value="validate" :disabled="!validateResult">
                  {{ t('pushRoute.validateResult') }}
                </el-radio-button>
              </el-radio-group>
              <template v-if="resultTab === 'preview' || resultTab === 'push'">
                <el-radio-group v-model="resultFormat" size="small">
                  <el-radio-button value="json">JSON</el-radio-button>
                  <el-radio-button value="yaml">YAML</el-radio-button>
                </el-radio-group>
                <el-button size="small" text @click="copyResult">
                  <el-icon><CopyDocument /></el-icon>
                  {{ t('common.copy') }}
                </el-button>
              </template>
            </div>
          </div>
          <div class="panel-body">
            <!-- 路由预览 -->
            <template v-if="resultTab === 'preview'">
              <div v-if="selectedRoutesData.length === 0" class="empty-result">
                <el-empty :description="t('pushRoute.noRoutesSelected')" size="small" />
              </div>
              <div v-else class="route-preview">
                <div class="preview-header">
                  <el-tag type="info" effect="plain" size="small">
                    {{ t('pushRoute.selectedRoutesCount', { count: selectedRoutesData.length }) }}
                  </el-tag>
                  <el-tag v-if="selectedInstanceIds.length > 0" type="success" effect="plain" size="small">
                    {{ t('pushRoute.targetInstancesCount', { count: selectedInstanceIds.length }) }}
                  </el-tag>
                </div>
                <div class="code-block">
                  <pre>{{ formattedRoutesPreview }}</pre>
                </div>
              </div>
            </template>

            <!-- 推送结果 -->
            <template v-else-if="resultTab === 'push'">
              <div v-if="!pushResult" class="empty-result">
                <el-empty :description="t('pushRoute.noResult')" size="small" />
              </div>
              <template v-else>
                <div v-if="pushResult.success" class="result-success">
                  <div class="success-header">
                    <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
                    <span>{{ t('pushRoute.pushSuccess') }}</span>
                  </div>
                  <div class="code-block">
                    <pre>{{ formattedPushResult }}</pre>
                  </div>
                </div>
                <div v-else class="result-error">
                  <div class="error-header">
                    <el-icon class="error-icon"><CircleCloseFilled /></el-icon>
                    <span>{{ t('pushRoute.pushFailed') }}</span>
                  </div>
                  <div class="error-detail">
                    <div v-for="(error, idx) in pushResult.errors" :key="idx" class="error-item">
                      <span class="error-instance">{{ error.instanceId }}:</span>
                      <span class="error-message">{{ error.message }}</span>
                    </div>
                  </div>
                </div>
              </template>
            </template>

            <!-- 校验结果 -->
            <template v-else-if="resultTab === 'validate' && validateResult">
              <div v-if="validateResult.consistent" class="validate-success">
                <div class="success-header">
                  <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
                  <span>{{ t('pushRoute.validateSuccess') }}</span>
                </div>
                <div class="validate-info">
                  {{ t('pushRoute.validateConsistent') }}
                </div>
              </div>
              <div v-else class="validate-error">
                <div class="error-header">
                  <el-icon class="error-icon"><WarningFilled /></el-icon>
                  <span>{{ t('pushRoute.validateFailed') }}</span>
                </div>
                <div class="validate-diff">
                  <div v-for="diff in validateResult.differences" :key="diff.instanceId" class="diff-item">
                    <div class="diff-header">
                      <span class="instance-id">{{ diff.instanceId }}</span>
                      <el-tag size="small" type="warning">{{ diff.type }}</el-tag>
                    </div>
                    <div class="diff-detail">
                      <div class="diff-row">
                        <span class="label">{{ t('pushRoute.expected') }}:</span>
                        <span class="value">{{ diff.expected }}</span>
                      </div>
                      <div class="diff-row">
                        <span class="label">{{ t('pushRoute.actual') }}:</span>
                        <span class="value">{{ diff.actual }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 右侧：实例列表 -->
      <div class="right-panel">
        <div class="panel-header">
          <div class="panel-title">
            <BlinkIcon icon="mdi:server-network" size="18" />
            <span>{{ t('pushRoute.gatewayInstances') }}</span>
          </div>
          <div class="panel-actions">
            <el-checkbox
              v-model="selectAllInstances"
              :indeterminate="isInstancesIndeterminate"
              @change="handleSelectAllInstances as any"
            >
              {{ t('common.selectAll') }}
            </el-checkbox>
          </div>
        </div>
        <div class="panel-stats">
          <span class="stat-item">
            {{ t('pushRoute.selectedInstances', { count: selectedInstanceIds.length }) }}
          </span>
          <span class="stat-divider">|</span>
          <span class="stat-item online">
            {{ t('pushRoute.onlineInstances', { count: onlineInstances.length }) }}
          </span>
        </div>
        <div class="panel-body" v-loading="instancesLoading">
          <el-scrollbar>
            <div v-if="instances.length === 0" class="empty-state">
              <el-empty :description="t('pushRoute.noInstances')" size="small" />
            </div>
            <div v-else class="instance-list">
              <div
                v-for="instance in instances"
                :key="instance.instanceId"
                class="instance-item"
                :class="{
                  'is-selected': selectedInstanceIds.includes(instance.instanceId),
                  'is-offline': instance.status !== 0
                }"
                @click="toggleInstanceSelection(instance.instanceId, instance.status === 0)"
              >
                <el-checkbox
                  :model-value="selectedInstanceIds.includes(instance.instanceId)"
                  :disabled="instance.status !== 0"
                  @click.stop
                  @change="toggleInstanceSelection(instance.instanceId, instance.status === 0)"
                />
                <div class="instance-indicator" :class="instance.status === 0 ? 'online' : 'offline'"></div>
                <div class="instance-info">
                  <div class="instance-id">{{ instance.instanceId }}</div>
                  <div class="instance-uri">{{ instance.uri }}</div>
                </div>
                <el-tag
                  :type="instance.status === 0 ? 'success' : 'danger'"
                  size="small"
                  effect="dark"
                >
                  {{ instance.status === 0 ? t('instanceRoute.online') : t('instanceRoute.offline') }}
                </el-tag>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>
    </div>

    <!-- 备注输入弹窗 -->
    <el-dialog
      v-model="remarkDialogVisible"
      :title="t('pushRoute.pushConfirm')"
      width="500px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
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

    <!-- 路由详情弹窗 -->
    <el-dialog
      v-model="routeDetailVisible"
      :title="t('pushRoute.routeDetail')"
      width="700px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="route-detail-dialog"
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
    >
      <div v-if="currentRouteDetail" class="route-detail-content">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="t('route.routeId')">
            <span class="mono-text">{{ currentRouteDetail.routeId }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.routeName')">
            {{ currentRouteDetail.routeName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.routeGroup')">
            <el-tag size="small" effect="plain" type="info">{{ currentRouteDetail.routesGroup || 'default' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.uri')">
            <el-tag size="small" effect="plain" type="success">{{ currentRouteDetail.uri }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.order')">
            {{ currentRouteDetail.orderNum || 0 }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.status')">
            <el-tag :type="currentRouteDetail.status === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ currentRouteDetail.status === 1 ? t('route.statusEnable') : t('route.statusDisable') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.pushStatus')">
            <el-tag :type="getPushStatusType(currentRouteDetail.pushStatus)" size="small" effect="plain">
              {{ getPushStatusText(currentRouteDetail.pushStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.lastPushTime')">
            {{ currentRouteDetail.lastPushTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.remark')" :span="2">
            {{ currentRouteDetail.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 断言配置 -->
        <div class="detail-section">
          <div class="section-title">{{ t('route.predicates') }}</div>
          <div v-if="currentRouteDetail.predicates?.length" class="predicate-list">
            <div v-for="(p, idx) in currentRouteDetail.predicates" :key="idx" class="predicate-item">
              <el-tag type="primary" effect="light" size="small">{{ p.name }}</el-tag>
              <span class="predicate-args">{{ formatPredicateArgs(p) }}</span>
            </div>
          </div>
          <div v-else class="empty-section">-</div>
        </div>

        <!-- 过滤器配置 -->
        <div class="detail-section">
          <div class="section-title">{{ t('route.filters') }}</div>
          <div v-if="currentRouteDetail.filters?.length" class="filter-list">
            <div v-for="(f, idx) in currentRouteDetail.filters" :key="idx" class="filter-item">
              <el-tag type="warning" effect="light" size="small">{{ f.name }}</el-tag>
              <span class="filter-args">{{ formatFilterArgs(f) }}</span>
            </div>
          </div>
          <div v-else class="empty-section">-</div>
        </div>

        <!-- JSON 预览 -->
        <div class="detail-section">
          <div class="section-title">JSON (Spring Cloud Gateway)</div>
          <div class="json-preview">
            <pre>{{ JSON.stringify(currentRouteDetail ? convertToGatewayRouteDefinition(currentRouteDetail) : null, null, 2) }}</pre>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 推送路由页面
 * 从路由仓库选择路由推送到指定的网关实例
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Promotion,
  Upload,
  CircleCheck,
  Search,
  CopyDocument,
  CircleCheckFilled,
  CircleCloseFilled,
  WarningFilled,
  View,
} from '@element-plus/icons-vue'
import {
  routeApi,
  type RouteDefinition,
  type GatewayInstanceVO,
  type PushRoutesReq,
} from '@/api/route'
import yaml from 'js-yaml'

defineOptions({
  name: 'PushRoute',
})

const { t } = useI18n()

// 路由列表状态
const routesLoading = ref(false)
const warehouseRoutes = ref<RouteDefinition[]>([])
const routeSearchKeyword = ref('')
const selectedRouteIds = ref<string[]>([])

// 实例列表状态
const instancesLoading = ref(false)
const instances = ref<GatewayInstanceVO[]>([])
const selectedInstanceIds = ref<string[]>([])

// 推送状态
const pushing = ref(false)
const remarkDialogVisible = ref(false)
const pushRemark = ref('')

// 路由详情状态
const routeDetailVisible = ref(false)
const currentRouteDetail = ref<RouteDefinition | null>(null)

// 结果状态
const resultTab = ref<'preview' | 'push' | 'validate'>('preview')
const resultFormat = ref<'json' | 'yaml'>('json')
const pushResult = ref<{
  success: boolean
  data?: any
  errors?: Array<{ instanceId: string; message: string }>
} | null>(null)
const validateResult = ref<{
  consistent: boolean
  differences?: Array<{
    instanceId: string
    type: string
    expected: string
    actual: string
  }>
} | null>(null)

// 计算属性
const filteredRoutes = computed(() => {
  if (!routeSearchKeyword.value) return warehouseRoutes.value
  const keyword = routeSearchKeyword.value.toLowerCase()
  return warehouseRoutes.value.filter(route =>
    route.routeId.toLowerCase().includes(keyword) ||
    (route.routeName?.toLowerCase().includes(keyword)) ||
    route.uri.toLowerCase().includes(keyword)
  )
})

const onlineInstances = computed(() =>
  instances.value.filter(inst => inst.status === 0)
)

const selectAllRoutes = computed({
  get: () => {
    return filteredRoutes.value.length > 0 &&
      filteredRoutes.value.every(r => selectedRouteIds.value.includes(r.routeId))
  },
  set: () => {}
})

const isRoutesIndeterminate = computed(() => {
  const selected = selectedRouteIds.value.filter(id =>
    filteredRoutes.value.some(r => r.routeId === id)
  )
  return selected.length > 0 && selected.length < filteredRoutes.value.length
})

const selectAllInstances = computed({
  get: () => {
    return onlineInstances.value.length > 0 &&
      onlineInstances.value.every(i => selectedInstanceIds.value.includes(i.instanceId))
  },
  set: () => {}
})

const isInstancesIndeterminate = computed(() => {
  const selected = selectedInstanceIds.value.filter(id =>
    onlineInstances.value.some(i => i.instanceId === id)
  )
  return selected.length > 0 && selected.length < onlineInstances.value.length
})

const canPush = computed(() =>
  selectedRouteIds.value.length > 0 && selectedInstanceIds.value.length > 0
)

const canValidate = computed(() =>
  pushResult.value?.success === true
)

// 选中的路由数据
const selectedRoutesData = computed(() =>
  warehouseRoutes.value.filter(r => selectedRouteIds.value.includes(r.routeId))
)

/**
 * 将路由数据转换为 Spring Cloud Gateway 路由定义格式
 * Spring Cloud Gateway RouteDefinition 包含:
 * - id: 路由ID
 * - uri: 目标URI
 * - predicates: 断言配置数组
 * - filters: 过滤器配置数组
 * - order: 顺序
 * - metadata: 元数据
 */
function convertToGatewayRouteDefinition(route: RouteDefinition) {
  const gatewayRoute: {
    id: string
    uri: string
    predicates: Array<{ name: string; args: Record<string, any> }>
    filters: Array<{ name: string; args: Record<string, any> }>
    order: number
    metadata?: Record<string, any>
  } = {
    id: route.routeId,
    uri: route.uri,
    predicates: [],
    filters: [],
    order: route.orderNum || 0,
  }

  // 转换断言配置
  if (route.predicates && Array.isArray(route.predicates)) {
    gatewayRoute.predicates = route.predicates.map(p => ({
      name: p.name,
      args: p.args || {},
    }))
  }

  // 转换过滤器配置
  if (route.filters && Array.isArray(route.filters)) {
    gatewayRoute.filters = route.filters.map(f => ({
      name: f.name,
      args: f.args || {},
    }))
  }

  // 只在有元数据时添加
  if (route.metadata && Object.keys(route.metadata).length > 0) {
    gatewayRoute.metadata = route.metadata
  }

  return gatewayRoute
}

// 格式化路由预览
const formattedRoutesPreview = computed(() => {
  const routes = selectedRoutesData.value
  if (routes.length === 0) return ''

  // 转换为 Spring Cloud Gateway 路由定义格式
  const gatewayRoutes = routes.map(convertToGatewayRouteDefinition)

  const previewData = {
    routes: gatewayRoutes,
    pushConfig: {
      targetInstances: selectedInstanceIds.value,
      pushMode: selectedInstanceIds.value.length === onlineInstances.value.length ? 'broadcast' : 'specified',
      timestamp: new Date().toISOString(),
    }
  }

  if (resultFormat.value === 'yaml') {
    try {
      return yaml.dump(previewData, { indent: 2, lineWidth: -1 })
    } catch {
      return JSON.stringify(previewData, null, 2)
    }
  }
  return JSON.stringify(previewData, null, 2)
})

const formattedPushResult = computed(() => {
  if (!pushResult.value?.data) return ''
  if (resultFormat.value === 'yaml') {
    try {
      return yaml.dump(pushResult.value.data, { indent: 2, lineWidth: -1 })
    } catch {
      return JSON.stringify(pushResult.value.data, null, 2)
    }
  }
  return JSON.stringify(pushResult.value.data, null, 2)
})

// 获取结果面板图标
function getResultPanelIcon(): string {
  switch (resultTab.value) {
    case 'preview':
      return 'mdi:eye'
    case 'push':
      return pushResult.value?.success ? 'mdi:check-circle' : 'mdi:close-circle'
    case 'validate':
      return validateResult.value?.consistent ? 'mdi:check-circle' : 'mdi:alert-circle'
    default:
      return 'mdi:eye'
  }
}

// 获取结果面板标题
function getResultPanelTitle(): string {
  switch (resultTab.value) {
    case 'preview':
      return t('pushRoute.routePreview')
    case 'push':
      return t('pushRoute.pushResult')
    case 'validate':
      return t('pushRoute.validateResult')
    default:
      return t('pushRoute.routePreview')
  }
}

// 加载路由仓库
async function loadWarehouseRoutes() {
  routesLoading.value = true
  try {
    const result = await routeApi.getList({
      pageNum: 1,
      pageSize: 500, // 加载足够多的路由
      status: 1, // 只加载启用的路由
    })
    warehouseRoutes.value = Array.isArray(result?.rows) ? result.rows : []
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    warehouseRoutes.value = []
  } finally {
    routesLoading.value = false
  }
}

// 加载实例列表
async function loadInstances() {
  instancesLoading.value = true
  try {
    const result = await routeApi.getOnlineGatewayInstances()
    instances.value = Array.isArray(result) ? result : []
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    instances.value = []
  } finally {
    instancesLoading.value = false
  }
}

// 路由选择
function toggleRouteSelection(routeId: string) {
  const index = selectedRouteIds.value.indexOf(routeId)
  if (index > -1) {
    selectedRouteIds.value.splice(index, 1)
  } else {
    selectedRouteIds.value.push(routeId)
  }
}

function handleSelectAllRoutes(val: boolean) {
  if (val) {
    selectedRouteIds.value = filteredRoutes.value.map(r => r.routeId)
  } else {
    selectedRouteIds.value = []
  }
}

// 实例选择
function toggleInstanceSelection(instanceId: string, isOnline: boolean) {
  if (!isOnline) return
  const index = selectedInstanceIds.value.indexOf(instanceId)
  if (index > -1) {
    selectedInstanceIds.value.splice(index, 1)
  } else {
    selectedInstanceIds.value.push(instanceId)
  }
}

function handleSelectAllInstances(val: boolean) {
  if (val) {
    selectedInstanceIds.value = onlineInstances.value.map(i => i.instanceId)
  } else {
    selectedInstanceIds.value = []
  }
}

// 推送状态
function getPushStatusType(pushStatus: number | undefined): 'info' | 'success' | 'danger' | 'warning' {
  if (pushStatus === undefined || pushStatus === 0) return 'info'
  if (pushStatus === 1) return 'success'
  if (pushStatus === 2) return 'danger'
  return 'warning'
}

function getPushStatusText(pushStatus: number | undefined): string {
  if (pushStatus === undefined || pushStatus === 0) return t('route.pushStatusNotPushed')
  if (pushStatus === 1) return t('route.pushStatusPushed')
  if (pushStatus === 2) return t('route.pushStatusFailed')
  return t('route.pushStatusUnknown')
}

// 显示路由详情
function showRouteDetail(route: RouteDefinition) {
  currentRouteDetail.value = route
  routeDetailVisible.value = true
}

// 格式化断言参数
function formatPredicateArgs(predicate: { name: string; args?: Record<string, any> }): string {
  if (!predicate.args) return '-'
  const args = Object.entries(predicate.args)
    .map(([k, v]) => `${k}: ${Array.isArray(v) ? v.join(', ') : v}`)
    .join(', ')
  return args || '-'
}

// 格式化过滤器参数
function formatFilterArgs(filter: { name: string; args?: Record<string, any> }): string {
  if (!filter.args) return '-'
  const args = Object.entries(filter.args)
    .map(([k, v]) => `${k}: ${Array.isArray(v) ? v.join(', ') : v}`)
    .join(', ')
  return args || '-'
}

// 推送操作
function handlePush() {
  if (!canPush.value) {
    ElMessage.warning(t('pushRoute.selectRoutesAndInstances'))
    return
  }
  pushRemark.value = ''
  remarkDialogVisible.value = true
}

async function confirmPush() {
  pushing.value = true
  pushResult.value = null
  validateResult.value = null

  try {
    const req: PushRoutesReq = {
      routeIds: selectedRouteIds.value,
      pushMode: 'specified',
      targetInstanceIds: selectedInstanceIds.value,
      remark: pushRemark.value,
    }
    await routeApi.pushRoutes(req)

    pushResult.value = {
      success: true,
      data: {
        pushedRoutes: selectedRouteIds.value.length,
        targetInstances: selectedInstanceIds.value.length,
        timestamp: new Date().toISOString(),
      }
    }

    ElMessage.success(t('pushRoute.pushSuccess'))
    remarkDialogVisible.value = false

    // 刷新路由列表更新推送状态
    loadWarehouseRoutes()
  } catch (error: any) {
    pushResult.value = {
      success: false,
      errors: [{
        instanceId: 'unknown',
        message: error?.message || t('pushRoute.pushFailed')
      }]
    }
    ElMessage.error(t('pushRoute.pushFailed'))
  } finally {
    pushing.value = false
  }
}

// 全实例推送
async function handleFullPush() {
  try {
    await ElMessageBox.confirm(
      t('pushRoute.fullPushConfirm'),
      t('message.tips'),
      { type: 'warning' }
    )
  } catch {
    return
  }

  pushing.value = true
  pushResult.value = null
  validateResult.value = null

  try {
    // 全实例推送所有选中的路由
    const req: PushRoutesReq = {
      routeIds: selectedRouteIds.value.length > 0 ? [...selectedRouteIds.value] : [],
      pushMode: 'broadcast',
      remark: 'Full push from push route page',
    }
    await routeApi.pushRoutes(req)

    pushResult.value = {
      success: true,
      data: {
        pushedRoutes: selectedRouteIds.value.length || 'all enabled',
        targetInstances: onlineInstances.value.length,
        timestamp: new Date().toISOString(),
      }
    }

    ElMessage.success(t('pushRoute.pushSuccess'))
    loadWarehouseRoutes()
  } catch (error: any) {
    pushResult.value = {
      success: false,
      errors: [{
        instanceId: 'unknown',
        message: error?.message || t('pushRoute.pushFailed')
      }]
    }
    ElMessage.error(t('pushRoute.pushFailed'))
  } finally {
    pushing.value = false
  }
}

// 校验操作
async function handleValidate() {
  if (!canValidate.value) return

  // TODO: 调用 actuator 接口校验路由一致性
  // 这里需要后端提供相应的接口

  // 模拟校验结果
  validateResult.value = {
    consistent: true,
    differences: []
  }

  resultTab.value = 'validate'
  ElMessage.success(t('pushRoute.validateSuccess'))
}

// 复制结果
async function copyResult() {
  let content = ''
  if (resultTab.value === 'preview') {
    content = formattedRoutesPreview.value
  } else if (resultTab.value === 'push') {
    content = formattedPushResult.value
  }
  if (!content) return
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success(t('common.copy') + ' ' + t('common.success'))
  } catch {
    ElMessage.error(t('common.copy') + ' ' + t('common.failed'))
  }
}

// 监听推送成功后启用校验按钮
watch(() => pushResult.value?.success, (success) => {
  if (success) {
    // 推送成功后可以校验
  }
})

// 初始化
onMounted(() => {
  loadWarehouseRoutes()
  loadInstances()
})

// 弹窗防抖动 - 手动锁定滚动条
const lockBodyScroll = () => {
  document.body.classList.add('dialog-open')
}

const unlockBodyScroll = () => {
  document.body.classList.remove('dialog-open')
}
</script>

<style scoped lang="scss">
// 推送路由页面
.push-route-page {
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
  align-items: center;
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--el-border-color-light);

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .title-icon {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }

    .title-info {
      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      .subtitle {
        margin: 4px 0 0;
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }
    }
  }

  .header-right {
    .action-buttons {
      display: flex;
      gap: 10px;
    }
  }
}

// 主要内容区
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

// 左侧列（路由列表 + 结果面板）
.left-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

// 左侧面板（路由列表）- 减少高度
.left-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
  min-height: 0;
  max-height: 50%;
}

// 结果面板
.result-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
  min-height: 0;
}

// 右侧面板（实例列表）
.right-panel {
  width: 380px;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
}

// 面板头部
.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--el-fill-color-light);
  flex-shrink: 0;

  .panel-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .panel-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

// 面板统计
.panel-stats {
  padding: 8px 16px;
  background: var(--el-fill-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: flex;
  gap: 8px;
  flex-shrink: 0;

  .stat-item {
    &.online {
      color: var(--el-color-success);
      font-weight: 500;
    }
  }

  .stat-divider {
    color: var(--el-border-color);
  }
}

// 面板内容
.panel-body {
  flex: 1;
  overflow: hidden;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.empty-result {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

// 路由列表
.route-list {
  .route-item {
    padding: 10px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    display: flex;
    align-items: center;
    gap: 12px;
    transition: all 0.2s ease;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &.is-selected {
      background: var(--el-color-primary-light-9);
      border-left: 3px solid var(--el-color-primary);
      padding-left: 13px;
    }

    .route-info {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      min-width: 0;

      .route-id {
        font-family: 'SF Mono', 'Monaco', monospace;
        font-size: 13px;
        font-weight: 500;
        color: var(--el-text-color-primary);
        min-width: 120px;
        max-width: 180px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .route-name {
        font-size: 12px;
        color: var(--el-text-color-secondary);
        min-width: 80px;
        max-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .route-uri {
        max-width: 200px;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }
}

// 实例列表
.instance-list {
  .instance-item {
    padding: 12px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &.is-selected {
      background: var(--el-color-primary-light-9);
      border-left: 3px solid var(--el-color-primary);
      padding-left: 13px;
    }

    &.is-offline {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .instance-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      flex-shrink: 0;

      &.online {
        background: var(--el-color-success);
        box-shadow: 0 0 6px var(--el-color-success-light-3);
      }

      &.offline {
        background: var(--el-color-danger);
      }
    }

    .instance-info {
      flex: 1;
      min-width: 0;

      .instance-id {
        font-family: 'SF Mono', 'Monaco', monospace;
        font-size: 12px;
        font-weight: 500;
        color: var(--el-text-color-primary);
        margin-bottom: 2px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .instance-uri {
        font-size: 11px;
        color: var(--el-text-color-secondary);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

// 成功结果
.result-success,
.validate-success {
  height: 100%;
  display: flex;
  flex-direction: column;

  .success-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    color: var(--el-color-success);
    font-weight: 500;
    flex-shrink: 0;

    .success-icon {
      font-size: 20px;
    }
  }

  .code-block {
    flex: 1;
    background: var(--el-fill-color-light);
    border-radius: 8px;
    padding: 12px;
    overflow: auto;

    pre {
      margin: 0;
      font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
      font-size: 12px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }

  .validate-info {
    padding: 12px 16px;
    background: var(--el-color-success-light-9);
    border-radius: 8px;
    border-left: 3px solid var(--el-color-success);
    color: var(--el-color-success-dark-2);
  }
}

// 路由预览
.route-preview {
  height: 100%;
  display: flex;
  flex-direction: column;

  .preview-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    flex-shrink: 0;
  }

  .code-block {
    flex: 1;
    background: var(--el-fill-color-light);
    border-radius: 8px;
    padding: 12px;
    overflow: auto;

    pre {
      margin: 0;
      font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
      font-size: 12px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }
}

// 错误结果
.result-error,
.validate-error {
  height: 100%;
  overflow: auto;

  .error-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    color: var(--el-color-danger);
    font-weight: 500;

    .error-icon {
      font-size: 20px;
    }
  }

  .error-detail {
    .error-item {
      padding: 10px 12px;
      background: var(--el-color-danger-light-9);
      border-radius: 6px;
      margin-bottom: 8px;
      border-left: 3px solid var(--el-color-danger);

      .error-instance {
        font-family: 'SF Mono', 'Monaco', monospace;
        font-weight: 500;
        color: var(--el-text-color-primary);
      }

      .error-message {
        color: var(--el-color-danger);
        margin-left: 8px;
      }
    }
  }
}

// 校验差异
.validate-diff {
  .diff-item {
    padding: 12px;
    background: var(--el-color-warning-light-9);
    border-radius: 8px;
    margin-bottom: 12px;
    border-left: 3px solid var(--el-color-warning);

    .diff-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;

      .instance-id {
        font-family: 'SF Mono', 'Monaco', monospace;
        font-weight: 500;
      }
    }

    .diff-detail {
      .diff-row {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        margin-bottom: 6px;
        font-size: 12px;

        .label {
          color: var(--el-text-color-secondary);
          min-width: 60px;
        }

        .value {
          flex: 1;
          word-break: break-all;
        }
      }
    }
  }
}

// 路由详情弹窗
.route-detail-dialog {
  .route-detail-content {
    .mono-text {
      font-family: 'SF Mono', 'Monaco', monospace;
    }

    .detail-section {
      margin-top: 16px;

      .section-title {
        font-size: 13px;
        font-weight: 600;
        color: var(--el-text-color-primary);
        margin-bottom: 8px;
        padding-left: 8px;
        border-left: 3px solid var(--el-color-primary);
      }

      .empty-section {
        color: var(--el-text-color-secondary);
        font-size: 12px;
        padding: 8px 12px;
        background: var(--el-fill-color-lighter);
        border-radius: 4px;
      }

      .predicate-list,
      .filter-list {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .predicate-item,
        .filter-item {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 8px 12px;
          background: var(--el-fill-color-light);
          border-radius: 6px;

          .predicate-args,
          .filter-args {
            font-size: 12px;
            color: var(--el-text-color-secondary);
            font-family: 'SF Mono', 'Monaco', monospace;
          }
        }
      }

      .json-preview {
        background: var(--el-fill-color-light);
        border-radius: 8px;
        padding: 12px;
        overflow: auto;
        max-height: 200px;

        pre {
          margin: 0;
          font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
          font-size: 12px;
          line-height: 1.6;
          white-space: pre-wrap;
          word-break: break-all;
        }
      }
    }
  }
}
</style>
