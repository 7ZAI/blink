<template>
  <!-- 实例路由管理页面 - 工业技术风格设计 -->
  <div class="instance-route-page">
    <!-- 左侧面板：实例列表 -->
    <div class="left-panel">
      <div class="panel-header">
        <div class="header-title">
          <div class="title-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="2" width="20" height="20" rx="2" />
              <path d="M8 8h8M8 12h8M8 16h4" />
            </svg>
          </div>
          <h3>{{ t('instanceRoute.instanceList') }}</h3>
        </div>
        <el-button
          type="primary"
          size="small"
          :icon="Refresh"
          class="refresh-btn"
          @click="loadInstances"
          :loading="instanceLoading"
        >
          {{ t('common.refresh') }}
        </el-button>
      </div>

      <!-- 实例统计 -->
      <div class="instance-stats">
        <div class="stat-item">
          <span class="stat-label">{{ t('instanceRoute.instancesTotal', { count: instances.length }) }}</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value online">{{ onlineInstances.length }}</span>
          <span class="stat-label">{{ t('instanceRoute.online') }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-value offline">{{ instances.length - onlineInstances.length }}</span>
          <span class="stat-label">{{ t('instanceRoute.offline') }}</span>
        </div>
      </div>

      <div class="panel-content">
        <el-scrollbar>
          <div
            v-for="instance in instances"
            :key="instance.instanceId"
            class="instance-item"
            :class="{
              active: selectedInstance?.instanceId === instance.instanceId,
              offline: instance.status !== 0
            }"
            @click="selectInstance(instance)"
          >
            <div class="instance-indicator" :class="instance.status === 0 ? 'online' : 'offline'"></div>
            <div class="instance-info">
              <div class="instance-id">{{ instance.instanceId }}</div>
              <div class="instance-meta">
                <span class="instance-address">{{ instance.uri }}</span>
              </div>
            </div>
            <div class="instance-status-badge">
              <el-tag
                :type="instance.status === 0 ? 'success' : 'danger'"
                size="small"
                effect="dark"
              >
                {{ instance.status === 0 ? t('instanceRoute.online') : t('instanceRoute.offline') }}
              </el-tag>
            </div>
          </div>
          <el-empty
            v-if="instances.length === 0 && !instanceLoading"
            :description="t('instanceRoute.noOnlineInstances')"
            class="empty-state"
          />
        </el-scrollbar>
      </div>
    </div>

    <!-- 右侧面板：路由与推送历史 -->
    <div class="right-panel">
      <!-- 当前路由区域 -->
      <div class="routes-section">
        <div class="section-header">
          <div class="header-title">
            <div class="title-icon routes-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5z" />
                <path d="M2 17l10 5 10-5" />
                <path d="M2 12l10 5 10-5" />
              </svg>
            </div>
            <h3>{{ t('instanceRoute.currentRoutes') }}</h3>
            <span v-if="selectedInstance" class="selected-instance-badge">
              {{ selectedInstance.instanceId }}
            </span>
          </div>
          <div class="header-actions">
            <el-select
              v-model="storageMode"
              :placeholder="t('instanceRoute.selectStorageMode')"
              size="small"
              class="storage-select"
              @change="loadInstanceRoutes"
            >
              <el-option value="redis">
                <div class="storage-option">
                  <span class="option-label">{{ t('instanceRoute.redisStorageLabel') }}</span>
                </div>
              </el-option>
              <el-option value="nacos">
                <div class="storage-option">
                  <span class="option-label">{{ t('instanceRoute.nacosConfigLabel') }}</span>
                </div>
              </el-option>
            </el-select>
            <el-input
              v-if="storageMode === 'redis'"
              v-model="routesGroup"
              :placeholder="t('instanceRoute.inputRoutesGroup')"
              size="small"
              class="group-input"
              @change="loadInstanceRoutes"
            />
            <el-button
              type="primary"
              :icon="Promotion"
              class="push-btn"
              @click="openPushDialog"
            >
              {{ t('instanceRoute.pushRoutes') }}
            </el-button>
          </div>
        </div>

        <div class="section-content">
          <el-table
            :data="instanceRoutes"
            v-loading="routesLoading"
            stripe
            border
            size="small"
            class="routes-table"
          >
            <el-table-column prop="routeId" :label="t('instanceRoute.routeIdColumn')" min-width="180">
              <template #default="{ row }">
                <span class="route-id-cell">{{ row.routeId }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="routeName" :label="t('instanceRoute.routeNameColumn')" min-width="150">
              <template #default="{ row }">
                <span>{{ row.routeName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="uri" :label="t('instanceRoute.uriColumn')" min-width="200">
              <template #default="{ row }">
                <el-tag type="success" effect="plain" size="small" class="uri-tag">
                  {{ row.uri }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="orderNum" :label="t('instanceRoute.orderColumn')" width="80" align="center">
              <template #default="{ row }">
                <el-tag type="info" effect="plain" size="small">{{ row.orderNum || 0 }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="t('instanceRoute.statusColumn')" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="dark">
                  {{ row.status === 0 ? t('instanceRoute.statusEnable') : t('instanceRoute.statusDisable') }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-if="instanceRoutes.length === 0 && !routesLoading"
            :description="t('instanceRoute.noRoutes')"
            class="empty-state"
          />
        </div>
      </div>

      <!-- 推送历史区域 -->
      <div class="history-section">
        <div class="section-header">
          <div class="header-title">
            <div class="title-icon history-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12,6 12,12 16,14" />
              </svg>
            </div>
            <h3>{{ t('instanceRoute.pushHistory') }}</h3>
          </div>
          <el-button
            type="default"
            size="small"
            :icon="Refresh"
            class="refresh-btn"
            @click="loadPushHistory"
          >
            {{ t('common.refresh') }}
          </el-button>
        </div>

        <div class="section-content">
          <el-table
            :data="pushHistoryList"
            v-loading="historyLoading"
            stripe
            border
            size="small"
            class="history-table"
          >
            <el-table-column prop="pushId" :label="t('instanceRoute.pushId')" width="80" align="center">
              <template #default="{ row }">
                <span class="push-id">#{{ row.pushId }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="storageMode" :label="t('instanceRoute.storageModeColumn')" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="row.storageMode === 'redis' ? 'success' : 'warning'"
                  size="small"
                  effect="light"
                >
                  {{ row.storageMode === 'redis' ? t('instanceRoute.redisStorageLabel') : t('instanceRoute.nacosConfigLabel') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="routesGroup" :label="t('instanceRoute.routesGroupColumn')" min-width="100">
              <template #default="{ row }">
                <span>{{ row.routesGroup || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="routeIds" :label="t('instanceRoute.routeIdsColumn')" min-width="150">
              <template #default="{ row }">
                <el-popover placement="top" trigger="hover" :width="300">
                  <template #reference>
                    <div class="route-ids-preview">
                      <span class="preview-text">{{ formatRouteIds(row.routeIds) }}</span>
                      <el-icon class="expand-icon"><ArrowRight /></el-icon>
                    </div>
                  </template>
                  <div class="route-ids-detail">
                    <div class="detail-header">{{ t('instanceRoute.routeIdsPreview') }}</div>
                    <div class="detail-list">
                      {{ parseRouteIds(row.routeIds).join(', ') }}
                    </div>
                  </div>
                </el-popover>
              </template>
            </el-table-column>
            <el-table-column prop="pushMode" :label="t('instanceRoute.pushModeColumn')" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="row.pushMode === 'broadcast' ? 'info' : 'primary'"
                  size="small"
                  effect="light"
                >
                  {{ row.pushMode === 'broadcast' ? t('instanceRoute.broadcastMode') : t('instanceRoute.specifiedMode') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="instanceCount" :label="t('instanceRoute.instanceCountColumn')" width="80" align="center">
              <template #default="{ row }">
                <span class="count-cell">{{ row.instanceCount || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="successCount" :label="t('instanceRoute.successCountColumn')" width="80" align="center">
              <template #default="{ row }">
                <span class="success-count">{{ row.successCount || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="pushResult" :label="t('instanceRoute.pushResultColumn')" width="100" align="center">
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
            <el-table-column prop="operatorName" :label="t('instanceRoute.operatorColumn')" width="100">
              <template #default="{ row }">
                <span class="operator-cell">{{ row.operatorName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="pushTime" :label="t('instanceRoute.pushTimeColumn')" width="160">
              <template #default="{ row }">
                <span class="time-cell">{{ row.pushTime }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('instanceRoute.actionColumn')" width="120" fixed="right">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="openSnapshotDialog(row)"
                  >
                    <el-icon><View /></el-icon>
                    {{ t('instanceRoute.viewSnapshot') }}
                  </el-button>
                  <el-button
                    type="warning"
                    link
                    size="small"
                    @click="handleRollback(row)"
                  >
                    <el-icon><RefreshLeft /></el-icon>
                    {{ t('instanceRoute.rollbackPush') }}
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
      </div>
    </div>

    <!-- 推送路由弹窗 -->
    <el-dialog
      v-model="pushDialogVisible"
      :title="t('instanceRoute.pushRoutes')"
      width="680px"
      class="push-dialog"
      :close-on-click-modal="false"
    >
      <div class="dialog-tip">{{ t('instanceRoute.pushRoutesTip') }}</div>
      <el-form label-width="120px" class="push-form">
        <el-form-item :label="t('instanceRoute.storageMode')">
          <el-select v-model="pushForm.storageMode" style="width: 100%" class="form-select">
            <el-option value="redis">
              <div class="storage-option">
                <span class="option-label">{{ t('instanceRoute.redisStorageLabel') }}</span>
              </div>
            </el-option>
            <el-option value="nacos">
              <div class="storage-option">
                <span class="option-label">{{ t('instanceRoute.nacosConfigLabel') }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="pushForm.storageMode === 'redis'" :label="t('instanceRoute.routesGroupLabel')">
          <el-input
            v-model="pushForm.routesGroup"
            :placeholder="t('instanceRoute.defaultRoutesGroup')"
          />
        </el-form-item>
        <el-form-item v-if="pushForm.storageMode === 'nacos'" :label="t('instanceRoute.dataIdLabel')">
          <el-input
            v-model="pushForm.nacosDataId"
            :placeholder="t('instanceRoute.defaultDataId')"
          />
        </el-form-item>
        <el-form-item v-if="pushForm.storageMode === 'nacos'" :label="t('instanceRoute.groupLabel')">
          <el-input
            v-model="pushForm.nacosGroup"
            :placeholder="t('instanceRoute.defaultGroup')"
          />
        </el-form-item>
        <el-form-item :label="t('instanceRoute.pushModeLabel')">
          <el-radio-group v-model="pushForm.pushMode" class="push-mode-radio">
            <el-radio value="broadcast">
              <div class="radio-content">
                <span class="radio-label">{{ t('instanceRoute.broadcastMode') }}</span>
                <span class="radio-desc">{{ t('instanceRoute.broadcastToAll') }}</span>
              </div>
            </el-radio>
            <el-radio value="specified">
              <div class="radio-content">
                <span class="radio-label">{{ t('instanceRoute.specifiedMode') }}</span>
                <span class="radio-desc">{{ t('instanceRoute.specifiedInstances') }}</span>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="pushForm.pushMode === 'specified'" :label="t('instanceRoute.targetInstancesLabel')">
          <el-select v-model="pushForm.targetInstanceIds" multiple style="width: 100%" class="form-select">
            <el-option
              v-for="inst in onlineInstances"
              :key="inst.instanceId"
              :label="inst.instanceId"
              :value="inst.instanceId"
            >
              <div class="instance-option">
                <span class="option-id">{{ inst.instanceId }}</span>
                <span class="option-uri">{{ inst.uri }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('instanceRoute.routesToPush')">
          <el-table
            ref="routeSelectTableRef"
            :data="warehouseRoutes"
            @selection-change="handleRouteSelection"
            max-height="300"
            border
            stripe
            size="small"
            class="route-select-table"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="routeId" :label="t('instanceRoute.routeIdColumn')" min-width="180">
              <template #default="{ row }">
                <span class="route-id-cell">{{ row.routeId }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="routeName" :label="t('instanceRoute.routeNameColumn')" min-width="150" />
            <el-table-column prop="uri" :label="t('instanceRoute.uriColumn')" min-width="200">
              <template #default="{ row }">
                <el-tag type="success" effect="plain" size="small">{{ row.uri }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-form-item :label="t('instanceRoute.remark')">
          <el-input v-model="pushForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pushDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handlePushRoutes" :loading="pushLoading">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 路由快照弹窗 -->
    <el-dialog
      v-model="snapshotDialogVisible"
      :title="t('instanceRoute.routeSnapshot')"
      width="800px"
      class="snapshot-dialog"
    >
      <div v-if="snapshotRoutes.length > 0" class="snapshot-info">
        {{ t('instanceRoute.snapshotRoutesCount', { count: snapshotRoutes.length }) }}
      </div>
      <el-table :data="snapshotRoutes" border stripe size="small" class="snapshot-table">
        <el-table-column prop="routeId" :label="t('instanceRoute.routeIdColumn')" min-width="180">
          <template #default="{ row }">
            <span class="route-id-cell">{{ row.routeId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="routeName" :label="t('instanceRoute.routeNameColumn')" min-width="150">
          <template #default="{ row }">
            <span>{{ row.routeName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="uri" :label="t('instanceRoute.uriColumn')" min-width="200">
          <template #default="{ row }">
            <el-tag type="success" effect="plain" size="small">{{ row.uri }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" :label="t('instanceRoute.orderColumn')" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">{{ row.orderNum || 0 }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="snapshotRoutes.length === 0"
        :description="t('instanceRoute.noSnapshotData')"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 实例路由管理页面
 * 管理网关实例的路由配置，包括查看、推送、历史和回滚功能
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, Refresh, ArrowRight, View, RefreshLeft } from '@element-plus/icons-vue'
import {
  routeApi,
  type RouteDefinition,
  type GatewayInstanceVO,
  type GaRoutePushLogDO,
  type PushRoutesReq,
  type RollbackPushReq,
} from '@/api/route'

const { t } = useI18n()

// 实例状态
const instances = ref<GatewayInstanceVO[]>([])
const selectedInstance = ref<GatewayInstanceVO | null>(null)
const instanceLoading = ref(false)

// 路由状态
const storageMode = ref('redis')
const routesGroup = ref('default_routes')
const instanceRoutes = ref<RouteDefinition[]>([])
const routesLoading = ref(false)

// 推送历史状态
const pushHistoryList = ref<GaRoutePushLogDO[]>([])
const historyLoading = ref(false)
const historyPage = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

// 推送弹窗状态
const pushDialogVisible = ref(false)
const pushLoading = ref(false)
const pushForm = ref<PushRoutesReq>({
  routeIds: [],
  storageMode: 'redis',
  routesGroup: 'default_routes',
  pushMode: 'broadcast',
  targetInstanceIds: [],
  remark: '',
})
const warehouseRoutes = ref<RouteDefinition[]>([])
const selectedRoutesToPush = ref<RouteDefinition[]>([])

// 快照弹窗状态
const snapshotDialogVisible = ref(false)
const snapshotRoutes = ref<RouteDefinition[]>([])

// 在线实例计算属性
const onlineInstances = computed(() =>
  instances.value.filter(inst => inst.status === 0)
)

// 加载实例列表
async function loadInstances() {
  instanceLoading.value = true
  try {
    const result = await routeApi.getOnlineGatewayInstances()
    instances.value = result || []
    // 自动选择第一个在线实例
    const firstInstance = result && result[0]
    if (firstInstance && firstInstance.status === 0 && !selectedInstance.value) {
      selectInstance(firstInstance)
    }
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    instanceLoading.value = false
  }
}

// 选择实例
function selectInstance(instance: GatewayInstanceVO) {
  selectedInstance.value = instance
  loadInstanceRoutes()
}

// 加载实例路由
async function loadInstanceRoutes() {
  routesLoading.value = true
  try {
    const routes = await routeApi.getInstanceRoutes({
      storageMode: storageMode.value,
      routesGroup: routesGroup.value,
      nacosDataId: pushForm.value.nacosDataId,
      nacosGroup: pushForm.value.nacosGroup,
    })
    instanceRoutes.value = routes || []
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    routesLoading.value = false
  }
}

// 加载推送历史
async function loadPushHistory() {
  historyLoading.value = true
  try {
    const result = await routeApi.getPushHistory({
      storageMode: storageMode.value,
      routesGroup: routesGroup.value,
      pageNum: historyPage.value.pageNum,
      pageSize: historyPage.value.pageSize,
    })
    pushHistoryList.value = result?.rows || []
    historyPage.value.total = result?.total || 0
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    historyLoading.value = false
  }
}

// 打开推送弹窗
async function openPushDialog() {
  // 加载仓库路由供选择
  try {
    const result = await routeApi.getList({
      storageMode: storageMode.value,
      routesGroup: routesGroup.value,
      pageNum: 1,
      pageSize: 100,
    })
    warehouseRoutes.value = result?.rows || []
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    return
  }

  // 重置表单
  pushForm.value = {
    routeIds: [],
    storageMode: storageMode.value,
    routesGroup: routesGroup.value,
    pushMode: 'broadcast',
    targetInstanceIds: [],
    remark: '',
  }
  selectedRoutesToPush.value = []

  pushDialogVisible.value = true
}

// 处理路由选择
function handleRouteSelection(selection: RouteDefinition[]) {
  selectedRoutesToPush.value = selection
  pushForm.value.routeIds = selection.map(r => r.routeId)
}

// 处理推送路由
async function handlePushRoutes() {
  if (pushForm.value.routeIds.length === 0) {
    ElMessage.warning(t('instanceRoute.selectAtLeastOneRoute'))
    return
  }

  pushLoading.value = true
  try {
    await routeApi.pushRoutes(pushForm.value)
    ElMessage.success(t('instanceRoute.pushSuccess'))
    pushDialogVisible.value = false
    loadInstanceRoutes()
    loadPushHistory()
  } catch (error) {
    ElMessage.error(t('instanceRoute.pushFailed'))
  } finally {
    pushLoading.value = false
  }
}

// 处理回滚
async function handleRollback(row: GaRoutePushLogDO) {
  try {
    await ElMessageBox.confirm(
      t('instanceRoute.rollbackConfirm'),
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
    ElMessage.success(t('instanceRoute.rollbackSuccess'))
    loadInstanceRoutes()
    loadPushHistory()
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  }
}

// 打开快照弹窗
function openSnapshotDialog(row: GaRoutePushLogDO) {
  try {
    snapshotRoutes.value = row.routeSnapshot || []
    snapshotDialogVisible.value = true
  } catch {
    ElMessage.warning(t('instanceRoute.noRoutes'))
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
    return `${ids.slice(0, 3).join(', ')}... ${t('instanceRoute.moreRoutes', { count: ids.length - 3 })}`
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
  if (result === 0) return t('instanceRoute.pushResultSuccess')
  if (result === 1) return t('instanceRoute.pushResultPartial')
  return t('instanceRoute.pushResultFailed')
}

// 初始化
onMounted(() => {
  loadInstances()
  loadPushHistory()
})
</script>

<style scoped lang="scss">
// 实例路由页面 - 工业技术风格设计
.instance-route-page {
  display: flex;
  gap: 20px;
  height: 100%;
  padding: 20px;
  background: var(--el-bg-color-page);
}

// 左侧面板
.left-panel {
  width: 300px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);

  .panel-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--el-border-color-light);
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-title {
      display: flex;
      align-items: center;
      gap: 10px;

      .title-icon {
        width: 32px;
        height: 32px;
        background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;

        svg {
          width: 18px;
          height: 18px;
          color: white;
        }
      }

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }
    }

    .refresh-btn {
      border-radius: 8px;
    }
  }

  .instance-stats {
    padding: 12px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    background: var(--el-fill-color-light);
    border-bottom: 1px solid var(--el-border-color-lighter);

    .stat-item {
      display: flex;
      align-items: center;
      gap: 6px;

      .stat-label {
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }

      .stat-value {
        font-size: 18px;
        font-weight: 600;
        font-family: 'SF Mono', 'Monaco', monospace;

        &.online { color: var(--el-color-success); }
        &.offline { color: var(--el-color-danger); }
      }
    }

    .stat-divider {
      width: 1px;
      height: 20px;
      background: var(--el-border-color);
    }
  }

  .panel-content {
    flex: 1;
    overflow: hidden;
  }

  .instance-item {
    padding: 14px 20px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    gap: 12px;
    position: relative;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &.active {
      background: var(--el-color-primary-light-9);
      border-left: 3px solid var(--el-color-primary);
      padding-left: 17px;
    }

    &.offline {
      opacity: 0.7;
    }

    .instance-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      transition: all 0.3s ease;

      &.online {
        background: var(--el-color-success);
        box-shadow: 0 0 8px var(--el-color-success-light-3);
      }

      &.offline {
        background: var(--el-color-danger);
      }
    }

    .instance-info {
      flex: 1;
      min-width: 0;

      .instance-id {
        font-size: 14px;
        font-weight: 500;
        color: var(--el-text-color-primary);
        margin-bottom: 4px;
        font-family: 'SF Mono', 'Monaco', monospace;
      }

      .instance-meta {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .instance-status-badge {
      :deep(.el-tag) {
        border-radius: 6px;
      }
    }
  }

  .empty-state {
    padding: 40px 20px;
  }
}

// 右侧面板
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow: hidden;
}

.routes-section,
.history-section {
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);

  .section-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--el-border-color-light);
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-title {
      display: flex;
      align-items: center;
      gap: 10px;

      .title-icon {
        width: 32px;
        height: 32px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;

        svg {
          width: 18px;
          height: 18px;
          color: white;
        }

        &.routes-icon {
          background: linear-gradient(135deg, var(--el-color-success), var(--el-color-success-light-3));
        }

        &.history-icon {
          background: linear-gradient(135deg, var(--el-color-warning), var(--el-color-warning-light-3));
        }
      }

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      .selected-instance-badge {
        padding: 4px 10px;
        background: var(--el-color-primary-light-9);
        border-radius: 6px;
        font-size: 12px;
        font-family: 'SF Mono', 'Monaco', monospace;
        color: var(--el-color-primary);
      }
    }

    .header-actions {
      display: flex;
      gap: 10px;
      align-items: center;

      .storage-select {
        width: 140px;
      }

      .group-input {
        width: 140px;
      }

      .push-btn {
        border-radius: 8px;
      }
    }

    .refresh-btn {
      border-radius: 8px;
    }
  }

  .section-content {
    flex: 1;
    padding: 16px 20px;
    overflow: auto;
  }
}

.routes-section {
  flex: 1;
}

.history-section {
  height: 340px;
}

// 路由表格样式
.routes-table,
.history-table,
.snapshot-table,
.route-select-table {
  :deep(.el-table__header) {
    th {
      background: var(--el-fill-color-light) !important;
      font-weight: 600;
    }
  }

  .route-id-cell {
    font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
    font-size: 13px;
    color: var(--el-text-color-primary);
  }

  .uri-tag {
    font-family: 'SF Mono', 'Monaco', monospace;
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

// 推送历史表格特殊样式
.history-table {
  .push-id {
    font-family: 'SF Mono', 'Monaco', monospace;
    font-weight: 600;
    color: var(--el-color-primary);
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
}

.action-buttons {
  display: flex;
  gap: 8px;
}

// 分页样式
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

// 存储方式选项
.storage-option {
  .option-label {
    font-size: 14px;
  }
}

// 实例选项
.instance-option {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .option-id {
    font-family: 'SF Mono', 'Monaco', monospace;
    font-weight: 500;
  }

  .option-uri {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>

<style lang="scss">
// 推送弹窗样式（非 scoped）
.push-dialog,
.snapshot-dialog {
  .dialog-tip {
    padding: 12px 16px;
    background: var(--el-color-primary-light-9);
    border-radius: 8px;
    margin-bottom: 16px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    border-left: 3px solid var(--el-color-primary);
  }

  .push-form {
    .form-select {
      .el-select-dropdown__item {
        padding: 8px 12px;
      }
    }

    .push-mode-radio {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .el-radio {
        height: auto;
        padding: 12px 16px;
        background: var(--el-fill-color-light);
        border-radius: 8px;
        margin-right: 0;

        &.is-checked {
          background: var(--el-color-primary-light-9);
          border: 1px solid var(--el-color-primary);
        }
      }

      .radio-content {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .radio-label {
          font-size: 14px;
          font-weight: 500;
          color: var(--el-text-color-primary);
        }

        .radio-desc {
          font-size: 12px;
          color: var(--el-text-color-secondary);
        }
      }
    }
  }

  .snapshot-info {
    padding: 12px 16px;
    background: var(--el-fill-color-light);
    border-radius: 8px;
    margin-bottom: 16px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}
</style>