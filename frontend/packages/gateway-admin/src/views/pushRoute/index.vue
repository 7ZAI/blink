<template>
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
      <div class="header-center">
        <el-select
          v-model="selectedGroup"
          :placeholder="t('pushRoute.selectGroupPlaceholder')"
          clearable
          filterable
          style="width: 280px"
          @change="handleGroupChange"
        >
          <el-option
            v-for="group in routeGroups"
            :key="group.groupKey"
            :label="group.groupName"
            :value="group.groupKey"
          >
            <span class="group-option">
              <span class="group-name">{{ group.groupName }}</span>
              <el-tag size="small" effect="plain" type="info">
                {{ group.groupKey }}
              </el-tag>
            </span>
          </el-option>
        </el-select>
      </div>
      <div class="header-right">
        <el-button
          type="primary"
          :disabled="!selectedGroup"
          @click="handleOpenDiffDialog"
        >
          <el-icon><Promotion /></el-icon>
          {{ t('pushRoute.push') }}
        </el-button>
      </div>
    </div>

    <!-- 主内容区（三列布局） -->
    <div class="main-content" v-loading="pageLoading">
      <!-- 仓库路由列（左列） -->
      <div class="column repository-column">
        <div class="column-header">
          <div class="column-title">
            <BlinkIcon icon="mdi:database" size="18" />
            <span>{{ t('pushRoute.repositoryRoutesCount', { count: repositoryRoutes.length }) }}</span>
          </div>
          <el-input
            v-model="repoSearchKeyword"
            :placeholder="t('common.search')"
            clearable
            size="small"
            style="width: 150px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="column-body">
          <el-scrollbar>
            <div v-if="filteredRepoRoutes.length === 0" class="empty-state">
              <el-empty :description="t('pushRoute.noRoutes')" size="small" />
            </div>
            <div v-else class="route-list">
              <div
                v-for="route in filteredRepoRoutes"
                :key="route.routeId"
                class="route-item"
              >
                <span class="route-id">{{ route.routeId }}</span>
                <span class="route-name">{{ route.routeName || '-' }}</span>
                <el-tag size="small" effect="plain" type="success" class="route-uri">
                  {{ route.uri }}
                </el-tag>
                <el-tag
                  :type="route.status === 1 ? 'success' : 'danger'"
                  size="small"
                  effect="light"
                >
                  {{ route.status === 1 ? t('common.statusEnable') : t('common.statusDisable') }}
                </el-tag>
                <el-tag
                  :type="getPushStatusType(route.pushStatus)"
                  size="small"
                  effect="plain"
                >
                  {{ getPushStatusText(route.pushStatus) }}
                </el-tag>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>

      <!-- 实例路由列（中列） -->
      <div class="column instance-column">
        <div class="column-header">
          <div class="column-title">
            <BlinkIcon icon="mdi:server" size="18" />
            <span>{{ t('pushRoute.instanceRoutesCount', { count: instanceRoutes.length }) }}</span>
          </div>
          <div class="column-source">
            <el-tag v-if="instanceRouteSource" size="small" effect="plain" type="info">
              {{ instanceRouteSource.storageMode }}
            </el-tag>
            <el-tag v-if="instanceRouteSource?.fromActuator" size="small" effect="light">
              {{ t('pushRoute.loadFromActuator') }}
            </el-tag>
          </div>
        </div>
        <div class="column-body">
          <el-scrollbar>
            <div v-if="!selectedGroup" class="empty-state">
              <el-empty :description="t('pushRoute.selectGroup')" size="small" />
            </div>
            <div v-else-if="instanceRouteError" class="empty-state error-state">
              <el-empty :description="t('pushRoute.noInstanceRoutes')" size="small" />
              <div class="error-message">{{ instanceRouteError }}</div>
            </div>
            <div v-else-if="filteredInstanceRoutes.length === 0" class="empty-state">
              <el-empty :description="t('pushRoute.noRoutes')" size="small" />
            </div>
            <div v-else class="route-list instance-route-list">
              <div
                v-for="route in filteredInstanceRoutes"
                :key="route.routeId"
                class="route-item"
              >
                <span class="route-id">{{ route.routeId }}</span>
                <el-tag size="small" effect="plain" type="info" class="route-uri">
                  {{ route.uri }}
                </el-tag>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>

      <!-- 关联实例列（右列） -->
      <div class="column instances-column">
        <div class="column-header">
          <div class="column-title">
            <BlinkIcon icon="mdi:server-network" size="18" />
            <span>{{ t('pushRoute.associatedInstances') }}</span>
          </div>
          <span class="online-count">
            {{ t('pushRoute.onlineInstances', { count: associatedInstances.filter(i => i.status === 0).length }) }}
          </span>
        </div>
        <div class="column-body">
          <el-scrollbar>
            <div v-if="!selectedGroup" class="empty-state">
              <el-empty :description="t('pushRoute.selectGroup')" size="small" />
            </div>
            <div v-else-if="associatedInstances.length === 0" class="empty-state">
              <el-empty :description="t('pushRoute.noOnlineInstance')" size="small" />
            </div>
            <div v-else class="instance-list">
              <div
                v-for="instance in associatedInstances"
                :key="instance.instanceId"
                class="instance-item"
                :class="{ 'is-offline': instance.status !== 0 }"
              >
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
                  {{ instance.status === 0 ? t('common.statusOnline') : t('common.statusOffline') }}
                </el-tag>
                <el-tag v-if="instance.storageMode" size="small" effect="plain" type="warning">
                  {{ instance.storageMode }}
                </el-tag>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>
    </div>

    <!-- 路由差异对比对话框 -->
    <RouteDiffDialog
      v-model="diffDialogVisible"
      :routes-group="selectedGroup"
      @confirm-push="handleConfirmPush"
    />

    <!-- 任务进度弹窗 -->
    <BlinkTaskDialog
      v-model="taskState.visible"
      :status="taskState.status"
      :progress="taskState.progress"
      :title="t('pushRoute.pushToInstances', { count: pushInstanceIds.length })"
      :message="taskState.message"
      :result="taskState.result"
      :error="taskState.error"
      :elapsed-time="taskState.elapsedTime"
      :steps="pushSteps"
      :cancellable="true"
      :backgroundable="true"
      :close-on-complete="false"
      @cancel="handleCancelTask"
      @background="handleBackground"
      @close="handleTaskDialogClose"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 路由推送页面 - 重构版本
 * 分组驱动的推送流程，三列布局展示
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, Search } from '@element-plus/icons-vue'
import {
  BlinkTaskDialog,
  useTaskRunner,
  TaskStatus,
  type ProgressUpdate,
} from '@blink/components'
import {
  routeApi,
  type RouteDefinition,
  type GroupInstanceRoutesRsp,
} from '@/api/route'
import {
  routeGroupApi,
  type RouteGroup,
} from '@/api/routeGroup'
import {
  queryInstanceList,
  type InstanceInfo,
} from '@/api/instance'
import RouteDiffDialog from './components/RouteDiffDialog.vue'

defineOptions({
  name: 'PushRoute',
})

const { t } = useI18n()

// ============================================
// 页面状态
// ============================================

const pageLoading = ref(false)
const selectedGroup = ref<string>('')

// 路由分组列表
const routeGroups = ref<RouteGroup[]>([])

// 仓库路由（左列）
const repositoryRoutes = ref<RouteDefinition[]>([])
const repoSearchKeyword = ref('')

// 实例路由（中列）
const instanceRouteSource = ref<GroupInstanceRoutesRsp | null>(null)
const instanceRoutes = ref<RouteDefinition[]>([])
const instanceRouteError = ref<string>('')

// 关联实例（右列）
const associatedInstances = ref<InstanceInfo[]>([])

// ============================================
// 差异对话框状态
// ============================================

const diffDialogVisible = ref(false)
const pushInstanceIds = ref<string[]>([])

// ============================================
// 任务进度状态
// ============================================

const pushSteps = [
  t('pushRoute.stepPushRoutes'),
  t('pushRoute.stepNotifyChange'),
  t('pushRoute.stepWaitEffect'),
  t('pushRoute.stepVerifyResult'),
]

const { state: taskState, start, cancel } = useTaskRunner({
  onComplete: (result: any) => {
    // 校验结果处理
    handleVerifyResult(result)
  },
  onCancel: () => {
    ElMessage.warning(t('common.cancelled'))
  },
  onError: (error: Error) => {
    ElMessage.error(`${t('message.operationFailed')}: ${error.message}`)
  },
})

// ============================================
// 计算属性
// ============================================

const filteredRepoRoutes = computed(() => {
  if (!repoSearchKeyword.value) return repositoryRoutes.value
  const keyword = repoSearchKeyword.value.toLowerCase()
  return repositoryRoutes.value.filter(route =>
    route.routeId.toLowerCase().includes(keyword) ||
    (route.routeName?.toLowerCase().includes(keyword)) ||
    route.uri.toLowerCase().includes(keyword)
  )
})

const filteredInstanceRoutes = computed(() => {
  return instanceRoutes.value
})

// ============================================
// 加载方法
// ============================================

// 加载路由分组列表
async function loadRouteGroups() {
  try {
    const groups = await routeGroupApi.getEnabledRouteGroups()
    routeGroups.value = groups || []
  } catch (error) {
    console.error('[PushRoute] Failed to load route groups:', error)
    routeGroups.value = []
  }
}

// 处理分组选择变化
async function handleGroupChange(groupKey: string) {
  if (!groupKey) {
    // 清空数据
    repositoryRoutes.value = []
    instanceRoutes.value = []
    instanceRouteSource.value = null
    instanceRouteError.value = ''
    associatedInstances.value = []
    return
  }

  pageLoading.value = true
  try {
    // 并行加载三列数据
    await Promise.all([
      loadRepositoryRoutes(groupKey),
      loadInstanceRoutes(groupKey),
      loadAssociatedInstances(groupKey),
    ])
  } finally {
    pageLoading.value = false
  }
}

// 加载仓库路由（左列）
async function loadRepositoryRoutes(groupKey: string) {
  try {
    const result = await routeApi.getList({
      routesGroup: groupKey,
      status: 1, // 只加载启用状态的路由
      pageNum: 1,
      pageSize: 500,
    })
    repositoryRoutes.value = result?.rows || []
  } catch (error) {
    console.error('[PushRoute] Failed to load repository routes:', error)
    repositoryRoutes.value = []
  }
}

// 加载实例路由（中列）
async function loadInstanceRoutes(groupKey: string) {
  instanceRouteError.value = ''
  instanceRouteSource.value = null
  instanceRoutes.value = []

  try {
    const result = await routeApi.getGroupInstanceRoutes({
      routesGroup: groupKey,
    })
    instanceRouteSource.value = result
    instanceRoutes.value = result?.rows || []

    if (result?.error) {
      instanceRouteError.value = result.error
    }
  } catch (error: any) {
    console.error('[PushRoute] Failed to load instance routes:', error)
    instanceRouteError.value = error?.message || t('pushRoute.loadFailed')
  }
}

// 加载关联实例（右列）
async function loadAssociatedInstances(groupKey: string) {
  try {
    const result = await queryInstanceList({
      groupKey: groupKey,
      pageNum: 1,
      pageSize: 100,
    })
    associatedInstances.value = result?.rows || []
  } catch (error) {
    console.error('[PushRoute] Failed to load associated instances:', error)
    associatedInstances.value = []
  }
}

// ============================================
// 推送状态辅助方法
// ============================================

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

// ============================================
// 推送流程
// ============================================

// 打开差异对比对话框
function handleOpenDiffDialog() {
  if (!selectedGroup.value) {
    ElMessage.warning(t('pushRoute.selectGroup'))
    return
  }
  diffDialogVisible.value = true
}

// 确认推送（从差异对话框）
function handleConfirmPush(data: { routesGroup: string; instanceIds: string[] }) {
  pushInstanceIds.value = data.instanceIds
  diffDialogVisible.value = false

  // 开始执行推送任务
  startPushTask(data.routesGroup, data.instanceIds)
}

// 开始推送任务
async function startPushTask(routesGroup: string, instanceIds: string[]) {
  await start({
    task: async (onProgress: (progress: ProgressUpdate) => void, signal?: AbortSignal) => {
      let pushId: number | null = null

      // 步骤 1: 推送路由
      onProgress({
        step: 0,
        stepMessage: t('pushRoute.stepPushRoutesDesc'),
        message: t('pushRoute.stepPushRoutesDesc'),
      })

      try {
        // 获取分组下第一个实例的 storageMode
        const firstInstance = associatedInstances.value.find(i => instanceIds.includes(i.instanceId))
        const storageMode = firstInstance?.storageMode || 'redis'

        await routeApi.fullPushRoutes({
          storageMode,
          routesGroup: routesGroup,
        })

        // 获取推送记录 ID（用于后续校验）
        try {
          const firstInstanceId = instanceIds[0]
          if (firstInstanceId) {
            const latestPush = await routeApi.getLatestPush({ instanceId: firstInstanceId })
            pushId = latestPush?.pushId
          }
        } catch {
          // 忽略获取推送记录失败
        }
      } catch (error: any) {
        throw new Error(`${t('pushRoute.stepPushRoutes')}: ${error.message}`)
      }

      if (signal?.aborted) return null

      // 步骤 2: 通知变更（已在 fullPushRoutes 中完成）
      onProgress({
        step: 1,
        stepMessage: t('pushRoute.stepNotifyChangeDesc'),
        message: t('pushRoute.stepNotifyChangeDesc'),
      })
      await delay(500) // 等待消息发送

      if (signal?.aborted) return null

      // 步骤 3: 等待生效
      onProgress({
        step: 2,
        stepMessage: t('pushRoute.stepWaitEffectDesc'),
        message: t('pushRoute.stepWaitEffectDesc'),
      })
      await delay(3000) // 等待实例刷新

      if (signal?.aborted) return null

      // 步骤 4: 校验结果
      onProgress({
        step: 3,
        stepMessage: t('pushRoute.stepVerifyResultDesc'),
        message: t('pushRoute.stepVerifyResultDesc'),
      })

      if (!pushId) {
        return { verified: false, reason: t('pushRoute.noPushRecord') }
      }

      try {
        const verifyResult = await routeApi.verifyPushResult({ pushId })
        const isConsistent = verifyResult.verifyResult === 0

        return {
          verified: isConsistent,
          pushId,
          instanceDetails: verifyResult.instanceDetails,
          summary: verifyResult.summary,
        }
      } catch (error: any) {
        return {
          verified: false,
          reason: error.message,
        }
      }
    },
    title: t('pushRoute.pushToInstances', { count: instanceIds.length }),
    message: t('pushRoute.stepPushRoutesDesc'),
    steps: pushSteps,
    cancellable: true,
    backgroundable: true,
  })
}

// 处理校验结果
function handleVerifyResult(result: any) {
  if (result?.verified) {
    ElMessage.success(t('pushRoute.verifySuccess'))
    taskState.value.visible = false
    // 刷新路由列表
    if (selectedGroup.value) {
      loadRepositoryRoutes(selectedGroup.value)
    }
  } else {
    ElMessage.warning(t('pushRoute.verifyFailed'))
    // 校验失败时阻止关闭，显示差异
    // 用户需要手动点击"重新推送"或"强制关闭"
  }
}

// 处理取消任务
function handleCancelTask() {
  cancel()
}

// 处理后台执行
function handleBackground() {
  taskState.value.visible = false
  ElMessage.info(t('common.success'))
}

// 处理任务弹窗关闭（校验未通过时需确认）
async function handleTaskDialogClose() {
  if (taskState.value.result?.verified === false) {
    try {
      await ElMessageBox.confirm(
        t('pushRoute.forceCloseConfirm'),
        t('message.tips'),
        { type: 'warning' }
      )
      taskState.value.visible = false
    } catch {
      // 用户取消关闭
    }
  }
}

// ============================================
// 工具函数
// ============================================

function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// ============================================
// 初始化
// ============================================

onMounted(() => {
  loadRouteGroups()
})
</script>

<style scoped lang="scss">
.push-route-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
  background: var(--bg-color-page);
  gap: 16px;
}

// 页面头部
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-color-card);
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color-base);

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
        color: var(--text-color-primary);
      }

      .subtitle {
        margin: 4px 0 0;
        font-size: 13px;
        color: var(--text-color-secondary);
      }
    }
  }

  .header-center {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .group-option {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    gap: 8px;

    .group-name {
      flex: 1;
    }
  }
}

// 主内容区（三列布局）
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

// 通用列样式
.column {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-color-card);
  border-radius: 12px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color-base);
  overflow: hidden;
  min-height: 0;

  .column-header {
    padding: 12px 16px;
    border-bottom: 1px solid var(--border-color-base);
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: var(--bg-color-page);
    flex-shrink: 0;

    .column-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      color: var(--text-color-primary);
    }

    .column-source {
      display: flex;
      gap: 8px;
    }

    .online-count {
      color: var(--el-color-success);
      font-size: 14px;
    }
  }

  .column-body {
    flex: 1;
    overflow: hidden;
    background: var(--bg-color-card);
  }
}

// 仓库路由列特殊样式
.repository-column {
  .route-list {
    .route-item {
      padding: 10px 16px;
      border-bottom: 1px solid var(--border-color-light);
      display: flex;
      align-items: center;
      gap: 12px;
      transition: all 0.2s ease;

      &:hover {
        background: var(--table-row-hover);
      }

      .route-id {
        font-family: 'SF Mono', 'Monaco', monospace;
        font-size: 13px;
        font-weight: 500;
        color: var(--text-color-primary);
        min-width: 120px;
        max-width: 180px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .route-name {
        font-size: 12px;
        color: var(--text-color-secondary);
        min-width: 80px;
        max-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .route-uri {
        max-width: 150px;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }
}

// 实例路由列特殊样式
.instance-column {
  background: var(--el-fill-color-lighter);

  .instance-route-list {
    .route-item {
      padding: 10px 16px;
      border-bottom: 1px solid var(--border-color-light);
      display: flex;
      align-items: center;
      gap: 12px;
      transition: all 0.2s ease;

      &:hover {
        background: var(--table-row-hover);
      }

      .route-id {
        font-family: 'SF Mono', 'Monaco', monospace;
        font-size: 13px;
        color: var(--text-color-primary);
        min-width: 150px;
      }
    }
  }

  .error-state {
    .error-message {
      color: var(--el-color-danger);
      font-size: 12px;
      padding: 8px 16px;
      background: var(--el-color-danger-light-9);
      border-radius: 4px;
      margin-top: 8px;
    }
  }
}

// 关联实例列特殊样式
.instances-column {
  flex: 0.8;

  .instance-list {
    .instance-item {
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color-light);
      display: flex;
      align-items: center;
      gap: 10px;
      transition: all 0.2s ease;

      &:hover {
        background: var(--table-row-hover);
      }

      &.is-offline {
        opacity: 0.6;
      }

      .instance-indicator {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        flex-shrink: 0;

        &.online {
          background: var(--el-color-success);
          box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
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
          color: var(--text-color-primary);
          margin-bottom: 2px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .instance-uri {
          font-size: 11px;
          color: var(--text-color-secondary);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
}
</style>