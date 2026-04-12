<template>
  <!-- 实例路由管理页面 -->
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

    <!-- 右侧面板：三个独立panel -->
    <div class="right-panel">
      <!-- Panel 1: 当前路由 -->
      <div class="panel-card routes-panel">
        <div class="panel-header">
          <div class="header-left">
            <div class="panel-icon routes-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5z" />
                <path d="M2 17l10 5 10-5" />
                <path d="M2 12l10 5 10-5" />
              </svg>
            </div>
            <h4>{{ t('instanceRoute.currentRoutes') }}</h4>
            <span v-if="selectedInstance" class="instance-badge">{{ selectedInstance.instanceId }}</span>
          </div>
          <div class="header-right">
            <el-button
              type="primary"
              size="small"
              :icon="Refresh"
              :loading="routesLoading"
              :disabled="!selectedInstance"
              @click="loadInstanceRoutes"
            >
              {{ t('common.refresh') }}
            </el-button>
          </div>
        </div>
        <div class="panel-body">
          <div v-if="!selectedInstance" class="empty-placeholder">
            <el-empty :description="t('instanceRoute.selectInstance')" :image-size="60" />
          </div>
          <div v-else-if="instanceRoutes.length === 0 && !routesLoading" class="empty-placeholder">
            <el-empty :description="t('instanceRoute.noRoutes')" :image-size="60" />
          </div>
          <template v-else>
            <div class="format-switcher">
              <el-radio-group v-model="routeFormat" size="small">
                <el-radio-button value="json">JSON</el-radio-button>
                <el-radio-button value="yaml">YAML</el-radio-button>
              </el-radio-group>
              <el-button size="small" text @click="copyRoutes">
                <el-icon><CopyDocument /></el-icon>
                {{ t('common.copy') }}
              </el-button>
            </div>
            <div class="code-preview">
              <pre>{{ formattedRoutes }}</pre>
            </div>
          </template>
        </div>
      </div>

      <!-- Panel 2: 最新推送历史 -->
      <div class="panel-card history-panel">
        <div class="panel-header">
          <div class="header-left">
            <div class="panel-icon history-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
            </div>
            <h4>{{ t('instanceRoute.latestPushHistory') }}</h4>
          </div>
          <el-button
            size="small"
            text
            :icon="Refresh"
            :loading="historyLoading"
            @click="loadPushHistory"
          >
            {{ t('common.refresh') }}
          </el-button>
        </div>
        <div class="panel-body">
          <div v-if="pushHistory.length === 0 && !historyLoading" class="empty-placeholder">
            <el-empty :description="t('instanceRoute.noPushHistory')" :image-size="60" />
          </div>
          <div v-else class="history-list">
            <div
              v-for="item in pushHistory"
              :key="item.historyId"
              class="history-item"
            >
              <div class="history-left">
                <el-tag :type="item.pushStatus === 1 ? 'success' : 'danger'" size="small" effect="plain">
                  {{ item.pushStatus === 1 ? t('instanceRoute.pushSuccess') : t('instanceRoute.pushFailed') }}
                </el-tag>
                <span class="route-count">{{ t('instanceRoute.routeCount', { count: item.routeCount || 0 }) }}</span>
              </div>
              <div class="history-right">
                <span class="operator">{{ item.operatorName || '-' }}</span>
                <span class="time">{{ item.pushTime || '-' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Panel 3: 比对 -->
      <div class="panel-card compare-panel">
        <div class="panel-header">
          <div class="header-left">
            <div class="panel-icon compare-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>
            </div>
            <h4>{{ t('instanceRoute.compare') }}</h4>
          </div>
          <div class="header-right">
            <el-button
              type="primary"
              size="small"
              :loading="comparing"
              :disabled="!selectedInstance"
              @click="executeCompare"
            >
              {{ t('instanceRoute.executeCompare') }}
            </el-button>
            <el-button
              type="success"
              size="small"
              :icon="CircleCheck"
              :disabled="!selectedInstance || instanceRoutes.length === 0"
              @click="handleSyncValidate"
            >
              {{ t('instanceRoute.sync') }}
            </el-button>
          </div>
        </div>
        <div class="panel-body">
          <div v-if="!compareResult && !comparing" class="empty-placeholder">
            <el-empty :description="t('instanceRoute.clickToCompare')" :image-size="60" />
          </div>
          <div v-else-if="compareResult" class="compare-result">
            <!-- 统计概览 -->
            <div class="compare-summary">
              <div class="summary-item matched">
                <span class="summary-value">{{ compareResult.matchedCount || 0 }}</span>
                <span class="summary-label">{{ t('instanceRoute.matched') }}</span>
              </div>
              <div class="summary-item added">
                <span class="summary-value">{{ compareResult.addedCount || 0 }}</span>
                <span class="summary-label">{{ t('instanceRoute.added') }}</span>
              </div>
              <div class="summary-item modified">
                <span class="summary-value">{{ compareResult.modifiedCount || 0 }}</span>
                <span class="summary-label">{{ t('instanceRoute.modified') }}</span>
              </div>
              <div class="summary-item deleted">
                <span class="summary-value">{{ compareResult.deletedCount || 0 }}</span>
                <span class="summary-label">{{ t('instanceRoute.deleted') }}</span>
              </div>
            </div>
            <!-- 差异列表 -->
            <div v-if="compareResult.differences && compareResult.differences.length > 0" class="diff-list">
              <div
                v-for="diff in compareResult.differences"
                :key="diff.routeId"
                class="diff-item"
                :class="diff.diffType"
              >
                <div class="diff-header">
                  <span class="route-id">{{ diff.routeId }}</span>
                  <el-tag :type="getDiffTagType(diff.diffType) as any" size="small">
                    {{ getDiffTypeLabel(diff.diffType) }}
                  </el-tag>
                </div>
                <div v-if="diff.detail" class="diff-detail">{{ diff.detail }}</div>
              </div>
            </div>
            <div v-else class="all-matched">
              <el-icon><CircleCheckFilled /></el-icon>
              <span>{{ t('instanceRoute.allRoutesConsistent') }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 同步校验弹窗 -->
    <el-dialog
      v-model="validateDialogVisible"
      :title="t('instanceRoute.syncValidate')"
      width="700px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="validate-dialog"
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
    >
      <div v-loading="validating">
        <div v-if="validateResult" class="validate-result">
          <div v-if="validateResult.consistent" class="validate-success">
            <div class="result-header">
              <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
              <span>{{ t('instanceRoute.validateSuccess') }}</span>
            </div>
            <div class="result-info">
              {{ t('instanceRoute.allRoutesConsistent') }}
            </div>
          </div>
          <div v-else class="validate-error">
            <div class="result-header">
              <el-icon class="error-icon"><WarningFilled /></el-icon>
              <span>{{ t('instanceRoute.validateFailed') }}</span>
            </div>
            <div class="diff-list">
              <div v-for="diff in validateResult.differences" :key="diff.routeId" class="diff-item">
                <div class="diff-header">
                  <span class="route-id">{{ diff.routeId }}</span>
                  <el-tag size="small" type="warning">{{ diff.diffType }}</el-tag>
                </div>
                <div class="diff-detail">
                  <div class="diff-row">
                    <span class="label">{{ t('instanceRoute.expected') }}:</span>
                    <span class="value">{{ diff.expected }}</span>
                  </div>
                  <div class="diff-row">
                    <span class="label">{{ t('instanceRoute.actual') }}:</span>
                    <span class="value">{{ diff.actual }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="validate-empty">
          <el-empty :description="t('instanceRoute.clickToValidate')" />
        </div>
      </div>
      <template #footer>
        <el-button @click="validateDialogVisible = false">{{ t('common.close') }}</el-button>
        <el-button type="primary" :loading="validating" @click="executeValidate">
          {{ t('instanceRoute.executeValidate') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 实例路由管理页面
 * 管理网关实例的路由配置，查看路由、同步校验功能
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh, CircleCheck, CopyDocument, CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import yaml from 'js-yaml'
import {
  routeApi,
  type RouteDefinition,
  type GatewayInstanceVO,
} from '@/api/route'

defineOptions({
  name: 'InstanceRoute',
})

const { t } = useI18n()

// 实例状态
const instances = ref<GatewayInstanceVO[]>([])
const selectedInstance = ref<GatewayInstanceVO | null>(null)
const instanceLoading = ref(false)

// 路由状态
const instanceRoutes = ref<RouteDefinition[]>([])
const routesLoading = ref(false)
const routeFormat = ref<'json' | 'yaml'>('json')

// 推送历史状态
const pushHistory = ref<any[]>([])
const historyLoading = ref(false)

// 校验状态
const validateDialogVisible = ref(false)
const validating = ref(false)
const validateResult = ref<{
  consistent: boolean
  differences?: Array<{
    routeId: string
    diffType: string
    expected: string
    actual: string
  }>
} | null>(null)

// 比对状态
const comparing = ref(false)
const compareResult = ref<{
  matchedCount: number
  addedCount: number
  modifiedCount: number
  deletedCount: number
  differences?: Array<{
    routeId: string
    diffType: 'added' | 'modified' | 'deleted'
    detail?: string
  }>
} | null>(null)

// 在线实例计算属性
const onlineInstances = computed(() =>
  instances.value.filter(inst => inst.status === 0)
)

// 格式化路由展示
const formattedRoutes = computed(() => {
  if (instanceRoutes.value.length === 0) return ''

  // 转换为 Spring Cloud Gateway 格式
  const gatewayRoutes = instanceRoutes.value.map(route => ({
    id: route.routeId,
    uri: route.uri,
    predicates: route.predicates || [],
    filters: route.filters || [],
    order: route.orderNum || 0,
    metadata: route.metadata || undefined,
  }))

  if (routeFormat.value === 'yaml') {
    try {
      return yaml.dump(gatewayRoutes, { indent: 2, lineWidth: -1 })
    } catch {
      return JSON.stringify(gatewayRoutes, null, 2)
    }
  }
  return JSON.stringify(gatewayRoutes, null, 2)
})

// 加载实例列表
async function loadInstances() {
  instanceLoading.value = true
  try {
    const result = await routeApi.getOnlineGatewayInstances()
    instances.value = Array.isArray(result) ? result : []
    // 自动选择第一个在线实例
    const firstOnline = instances.value.find(inst => inst.status === 0)
    if (firstOnline && !selectedInstance.value) {
      selectInstance(firstOnline)
    }
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    instances.value = []
  } finally {
    instanceLoading.value = false
  }
}

// 选择实例
function selectInstance(instance: GatewayInstanceVO) {
  selectedInstance.value = instance
  instanceRoutes.value = []
  pushHistory.value = []
  compareResult.value = null
  // 自动加载路由和历史
  loadInstanceRoutes()
  loadPushHistory()
}

// 加载实例路由
async function loadInstanceRoutes() {
  if (!selectedInstance.value) {
    instanceRoutes.value = []
    return
  }
  routesLoading.value = true
  try {
    const result = await routeApi.getInstanceRoutes({
      instanceId: selectedInstance.value.instanceId,
    })
    // getInstanceRoutes 返回 RouteDefinition[] 或 { rows: RouteDefinition[] }
    if (Array.isArray(result)) {
      instanceRoutes.value = result
    } else if (result && Array.isArray((result as any).rows)) {
      instanceRoutes.value = (result as any).rows
    } else {
      instanceRoutes.value = []
    }
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    instanceRoutes.value = []
  } finally {
    routesLoading.value = false
  }
}

// 加载推送历史
async function loadPushHistory() {
  if (!selectedInstance.value) {
    pushHistory.value = []
    return
  }
  historyLoading.value = true
  try {
    const result = await routeApi.getInstancePushHistory({
      instanceId: selectedInstance.value.instanceId,
      pageSize: 10,
    })
    // getInstancePushHistory 返回 PageResult 或数组
    if (result && Array.isArray((result as any).rows)) {
      pushHistory.value = (result as any).rows
    } else if (Array.isArray(result)) {
      pushHistory.value = result
    } else {
      pushHistory.value = []
    }
  } catch (error) {
    console.error('[InstanceRoute] Failed to load push history:', error)
    pushHistory.value = []
  } finally {
    historyLoading.value = false
  }
}

// 复制路由
async function copyRoutes() {
  if (!formattedRoutes.value) return
  try {
    await navigator.clipboard.writeText(formattedRoutes.value)
    ElMessage.success(t('common.copy') + ' ' + t('common.success'))
  } catch {
    ElMessage.error(t('common.copy') + ' ' + t('common.failed'))
  }
}

// 打开同步校验弹窗
function handleSyncValidate() {
  validateResult.value = null
  validateDialogVisible.value = true
}

// 执行校验
async function executeValidate() {
  if (!selectedInstance.value) return

  validating.value = true
  try {
    const result = await routeApi.validateInstanceRoutes({
      instanceId: selectedInstance.value.instanceId,
    })
    validateResult.value = result
    if (result.consistent) {
      ElMessage.success(t('instanceRoute.validateSuccess'))
    } else {
      ElMessage.warning(t('instanceRoute.validateFailed'))
    }
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    validateResult.value = {
      consistent: false,
      differences: [{
        routeId: 'unknown',
        diffType: t('instanceRoute.error'),
        expected: '-',
        actual: t('message.fetchFailed'),
      }]
    }
  } finally {
    validating.value = false
  }
}

// 执行比对
async function executeCompare() {
  if (!selectedInstance.value) return

  comparing.value = true
  try {
    const result = await routeApi.compareInstanceRoutes({
      instanceId: selectedInstance.value.instanceId,
    })
    compareResult.value = result
    if (result.matchedCount > 0 && result.addedCount === 0 && result.modifiedCount === 0 && result.deletedCount === 0) {
      ElMessage.success(t('instanceRoute.compareSuccess'))
    }
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
    compareResult.value = {
      matchedCount: 0,
      addedCount: 0,
      modifiedCount: 0,
      deletedCount: 0,
      differences: [{
        routeId: 'error',
        diffType: 'modified',
        detail: t('message.fetchFailed'),
      }]
    }
  } finally {
    comparing.value = false
  }
}

// 获取差异类型标签样式
function getDiffTagType(diffType: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const typeMap: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    added: 'success',
    modified: 'warning',
    deleted: 'danger',
  }
  return typeMap[diffType] || 'info'
}

// 获取差异类型标签文本
function getDiffTypeLabel(diffType: string): string {
  const labelMap: Record<string, string> = {
    added: t('instanceRoute.added'),
    modified: t('instanceRoute.modified'),
    deleted: t('instanceRoute.deleted'),
  }
  return labelMap[diffType] || diffType
}

// 弹窗防抖动
const lockBodyScroll = () => {
  document.body.classList.add('dialog-open')
}
const unlockBodyScroll = () => {
  document.body.classList.remove('dialog-open')
}

// 初始化
onMounted(() => {
  loadInstances()
})
</script>

<style scoped lang="scss">
// 实例路由页面
.instance-route-page {
  display: flex;
  gap: 16px;
  height: 100%;
  padding: 16px;
  background: var(--el-bg-color-page);
}

// 左侧面板
.left-panel {
  width: 280px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);

  .panel-header {
    padding: 14px 16px;
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
        font-size: 15px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }
    }
  }

  .instance-stats {
    padding: 10px 16px;
    display: flex;
    align-items: center;
    gap: 12px;
    background: var(--el-fill-color-lighter);
    border-bottom: 1px solid var(--el-border-color-lighter);

    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;

      .stat-label {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }

      .stat-value {
        font-size: 16px;
        font-weight: 600;
        font-family: 'SF Mono', 'Monaco', monospace;

        &.online { color: var(--el-color-success); }
        &.offline { color: var(--el-color-danger); }
      }
    }

    .stat-divider {
      width: 1px;
      height: 16px;
      background: var(--el-border-color);
    }
  }

  .panel-content {
    flex: 1;
    overflow: hidden;
  }

  .instance-item {
    padding: 12px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    gap: 10px;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &.active {
      background: var(--el-color-primary-light-9);
      border-left: 3px solid var(--el-color-primary);
      padding-left: 13px;
    }

    &.offline {
      opacity: 0.6;
    }

    .instance-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;

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
        font-size: 13px;
        font-weight: 500;
        color: var(--el-text-color-primary);
        margin-bottom: 2px;
        font-family: 'SF Mono', 'Monaco', monospace;
      }

      .instance-meta {
        font-size: 11px;
        color: var(--el-text-color-secondary);
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
  display: grid;
  grid-template-rows: 1fr auto auto;
  gap: 12px;
  overflow: hidden;
}

// 通用面板卡片样式
.panel-card {
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;

  .panel-header {
    padding: 12px 16px;
    border-bottom: 1px solid var(--el-border-color-light);
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .panel-icon {
        width: 28px;
        height: 28px;
        border-radius: 6px;
        display: flex;
        align-items: center;
        justify-content: center;

        svg {
          width: 16px;
          height: 16px;
          color: white;
        }

        &.routes-icon {
          background: linear-gradient(135deg, var(--el-color-success), var(--el-color-success-light-3));
        }

        &.history-icon {
          background: linear-gradient(135deg, var(--el-color-warning), var(--el-color-warning-light-3));
        }

        &.compare-icon {
          background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
        }
      }

      h4 {
        margin: 0;
        font-size: 14px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      .instance-badge {
        padding: 2px 6px;
        background: var(--el-color-primary-light-9);
        border-radius: 4px;
        font-size: 11px;
        font-family: 'SF Mono', 'Monaco', monospace;
        color: var(--el-color-primary);
      }
    }

    .header-right {
      display: flex;
      gap: 8px;
    }
  }

  .panel-body {
    flex: 1;
    padding: 12px 16px;
    overflow: auto;
    min-height: 0;
  }
}

// Panel 1: 当前路由（占主要空间）
.routes-panel {
  // 默认 1fr
}

// Panel 2: 推送历史
.history-panel {
  max-height: 180px;
}

// Panel 3: 比对
.compare-panel {
  max-height: 220px;
}

// 空状态占位
.empty-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

// 格式切换器
.format-switcher {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

// 代码预览
.code-preview {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 12px;
  overflow: auto;
  height: calc(100% - 42px);

  pre {
    margin: 0;
    font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
    font-size: 12px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

// 推送历史列表
.history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .history-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 10px;
    background: var(--el-fill-color-lighter);
    border-radius: 6px;

    .history-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .route-count {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .history-right {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 12px;
      color: var(--el-text-color-secondary);

      .operator {
        font-weight: 500;
      }
    }
  }
}

// 比对结果
.compare-result {
  .compare-summary {
    display: flex;
    gap: 16px;
    margin-bottom: 12px;
    padding: 12px;
    background: var(--el-fill-color-lighter);
    border-radius: 8px;

    .summary-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      flex: 1;

      .summary-value {
        font-size: 20px;
        font-weight: 600;
        font-family: 'SF Mono', 'Monaco', monospace;
      }

      .summary-label {
        font-size: 11px;
        color: var(--el-text-color-secondary);
        margin-top: 2px;
      }

      &.matched .summary-value { color: var(--el-color-success); }
      &.added .summary-value { color: var(--el-color-success); }
      &.modified .summary-value { color: var(--el-color-warning); }
      &.deleted .summary-value { color: var(--el-color-danger); }
    }
  }

  .diff-list {
    max-height: 120px;
    overflow-y: auto;

    .diff-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 10px;
      background: var(--el-fill-color-lighter);
      border-radius: 6px;
      margin-bottom: 6px;
      border-left: 3px solid transparent;

      &.added { border-left-color: var(--el-color-success); }
      &.modified { border-left-color: var(--el-color-warning); }
      &.deleted { border-left-color: var(--el-color-danger); }

      .diff-header {
        display: flex;
        align-items: center;
        gap: 8px;
        flex: 1;

        .route-id {
          font-size: 12px;
          font-family: 'SF Mono', 'Monaco', monospace;
          font-weight: 500;
        }
      }

      .diff-detail {
        font-size: 11px;
        color: var(--el-text-color-secondary);
        margin-top: 4px;
        padding-left: 8px;
        border-left: 2px solid var(--el-border-color-lighter);
      }
    }
  }

  .all-matched {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px;
    background: var(--el-color-success-light-9);
    border-radius: 8px;
    color: var(--el-color-success);
    font-size: 13px;

    .el-icon {
      font-size: 18px;
    }
  }
}
</style>

<style lang="scss">
// 校验弹窗样式
.validate-dialog {
  .validate-result {
    .result-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      font-weight: 500;

      .success-icon {
        font-size: 20px;
        color: var(--el-color-success);
      }

      .error-icon {
        font-size: 20px;
        color: var(--el-color-danger);
      }
    }

    .result-info {
      padding: 12px 16px;
      background: var(--el-color-success-light-9);
      border-radius: 8px;
      border-left: 3px solid var(--el-color-success);
      color: var(--el-text-color-secondary);
    }

    .diff-list {
      .diff-item {
        padding: 12px;
        background: var(--el-color-warning-light-9);
        border-radius: 8px;
        margin-bottom: 12px;
        border-left: 3px solid var(--el-color-warning);

        .diff-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          .route-id {
            font-family: 'SF Mono', 'Monaco', monospace;
            font-weight: 500;
          }
        }

        .diff-detail {
          .diff-row {
            display: flex;
            gap: 8px;
            font-size: 12px;
            margin-bottom: 4px;

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
  }

  .validate-empty {
    padding: 20px;
  }
}
</style>
