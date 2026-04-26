# 路由推送页面重构 - 前端实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构路由推送页面，实现分组驱动的推送流程，支持差异对比和四步骤任务进度弹窗。

**Architecture:** 采用三列布局（仓库路由 | 实例路由 | 关联实例），点击推送显示 git-style 差异对比对话框，确认后通过 BlinkTaskDialog 四步骤流程完成推送和校验。

**Tech Stack:** Vue 3 Composition API, Element Plus, BlinkTaskDialog (useTaskRunner), TypeScript

---

## 文件结构

### 新建文件

| 文件 | 职责 |
|------|------|
| `frontend/packages/gateway-admin/src/views/pushRoute/components/RouteDiffDialog.vue` | 路由差异对比对话框组件 |
| `frontend/packages/gateway-admin/src/views/pushRoute/components/RouteCompareTable.vue` | 路由对比表格组件（可复用） |

### 修改文件

| 文件 | 责任 |
|------|------|
| `frontend/packages/gateway-admin/src/views/pushRoute/index.vue` | 重构为三列布局，分组驱动流程 |
| `frontend/packages/gateway-admin/src/api/route.ts` | 新增 getGroupInstanceRoutes API 和类型定义 |
| `frontend/packages/gateway-admin/src/locales/zh-cn.ts` | 新增国际化文本 |
| `frontend/packages/gateway-admin/src/locales/en-us.ts` | 新增国际化文本（英文） |

---

## Task 1: 新增 API 类型定义和接口

**Files:**
- Modify: `frontend/packages/gateway-admin/src/api/route.ts`

- [ ] **Step 1: 在 route.ts 中新增 GetGroupInstanceRoutesReq 类型定义**

在 `// ========== 路由差异对比接口 ==========` 部分前添加：

```typescript
// ========== 分组实例路由接口 ==========

/**
 * 获取分组实例路由请求
 */
export interface GetGroupInstanceRoutesReq {
  routesGroup: string // 路由分组（必填）
}

/**
 * 分组实例路由响应
 */
export interface GroupInstanceRoutesRsp {
  instanceId: string // 来源实例 ID
  storageMode: string // 存储模式（redis/nacos）
  timestamp: string // 获取时间
  rows: RouteDefinition[] // 路由列表
  total: number // 路由总数
  fromActuator: boolean // 是否来自 Actuator
  error?: string // 错误信息（可选）
}
```

- [ ] **Step 2: 在 route.ts 中新增 getGroupInstanceRoutes API 函数**

在 `GroupInstanceRoutesRsp` 类型定义后添加：

```typescript
/**
 * 获取分组下实例的实际路由
 * 从分组下第一个在线实例获取路由配置
 */
export const getGroupInstanceRoutes = (params: GetGroupInstanceRoutesReq): Promise<GroupInstanceRoutesRsp> => {
  return request.post('/route/getGroupInstanceRoutes', { body: params })
}
```

- [ ] **Step 3: 在 routeApi 对象中添加新接口**

在 `routeApi` 对象中添加 `getGroupInstanceRoutes`：

```typescript
// Route API object (for component using routeApi.xxx pattern)
export const routeApi = {
  getList: getRouteList,
  // ... 其他现有方法 ...
  getGroupInstanceRoutes, // 新增
  // 从实例同步路由
  syncRoutesFromInstance,
}
```

- [ ] **Step 4: 运行 TypeScript 类型检查**

Run: `cd frontend && pnpm tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 5: Commit**

```bash
git add frontend/packages/gateway-admin/src/api/route.ts
git commit -m "feat(route): 新增 getGroupInstanceRoutes API 类型定义"
```

---

## Task 2: 新增国际化文本

**Files:**
- Modify: `frontend/packages/gateway-admin/src/locales/zh-cn.ts`
- Modify: `frontend/packages/gateway-admin/src/locales/en-us.ts`

- [ ] **Step 1: 在 zh-cn.ts 的 pushRoute 部分新增文本**

找到 `pushRoute:` 部分，添加以下内容：

```typescript
pushRoute: {
  title: '路由推送',
  subtitle: '选择路由分组，推送仓库路由到网关实例',
  push: '推送',
  // ... 保留现有字段 ...
  
  // 新增字段
  selectGroup: '选择路由分组',
  selectGroupPlaceholder: '请选择路由分组',
  repositoryRoutes: '仓库路由',
  repositoryRoutesCount: '仓库路由 ({count} 条)',
  instanceRoutes: '实例路由',
  instanceRoutesCount: '实例路由 ({count} 条)',
  associatedInstances: '关联实例',
  noOnlineInstance: '当前分组无在线实例',
  noInstanceRoutes: '无法获取实例路由',
  loadFromActuator: '从 Actuator 获取',
  loadFailed: '获取失败',
  diffDialogTitle: '路由差异对比',
  diffStatsTitle: '差异统计',
  diffDetailsTitle: '差异详情',
  targetInstancesTitle: '目标实例',
  confirmPush: '确认推送',
  pushToInstances: '推送 {count} 个实例',
  // 四步骤文案
  stepPushRoutes: '推送路由',
  stepNotifyChange: '通知变更',
  stepWaitEffect: '等待生效',
  stepVerifyResult: '校验结果',
  stepPushRoutesDesc: '正在推送路由到存储...',
  stepNotifyChangeDesc: '正在通知实例刷新路由配置...',
  stepWaitEffectDesc: '等待实例刷新路由配置...',
  stepVerifyResultDesc: '正在校验路由一致性...',
  // 校验结果
  verifySuccess: '校验通过，路由配置一致',
  verifyFailed: '校验失败，存在路由差异',
  verifyDiffTitle: '校验差异详情',
  retryPush: '重新推送',
  forceClose: '强制关闭',
  forceCloseConfirm: '校验未通过，确定要关闭弹窗吗？',
}
```

- [ ] **Step 2: 在 en-us.ts 的 pushRoute 部分新增英文文本**

找到 `pushRoute:` 部分，添加对应的英文内容：

```typescript
pushRoute: {
  title: 'Route Push',
  subtitle: 'Select route group to push repository routes to gateway instances',
  push: 'Push',
  // ... 保留现有字段 ...
  
  // New fields
  selectGroup: 'Select Route Group',
  selectGroupPlaceholder: 'Please select a route group',
  repositoryRoutes: 'Repository Routes',
  repositoryRoutesCount: 'Repository Routes ({count})',
  instanceRoutes: 'Instance Routes',
  instanceRoutesCount: 'Instance Routes ({count})',
  associatedInstances: 'Associated Instances',
  noOnlineInstance: 'No online instances in current group',
  noInstanceRoutes: 'Cannot fetch instance routes',
  loadFromActuator: 'From Actuator',
  loadFailed: 'Load Failed',
  diffDialogTitle: 'Route Diff Comparison',
  diffStatsTitle: 'Diff Statistics',
  diffDetailsTitle: 'Diff Details',
  targetInstancesTitle: 'Target Instances',
  confirmPush: 'Confirm Push',
  pushToInstances: 'Push to {count} instances',
  // Four steps
  stepPushRoutes: 'Push Routes',
  stepNotifyChange: 'Notify Change',
  stepWaitEffect: 'Wait Effect',
  stepVerifyResult: 'Verify Result',
  stepPushRoutesDesc: 'Pushing routes to storage...',
  stepNotifyChangeDesc: 'Notifying instances to refresh route config...',
  stepWaitEffectDesc: 'Waiting for instances to refresh...',
  stepVerifyResultDesc: 'Verifying route consistency...',
  // Verify result
  verifySuccess: 'Verification passed, routes are consistent',
  verifyFailed: 'Verification failed, route differences exist',
  verifyDiffTitle: 'Verification Diff Details',
  retryPush: 'Retry Push',
  forceClose: 'Force Close',
  forceCloseConfirm: 'Verification not passed, are you sure to close the dialog?',
}
```

- [ ] **Step 3: 运行构建检查**

Run: `cd frontend && pnpm build:gateway-admin`
Expected: 构建成功，无错误

- [ ] **Step 4: Commit**

```bash
git add frontend/packages/gateway-admin/src/locales/zh-cn.ts frontend/packages/gateway-admin/src/locales/en-us.ts
git commit -m "feat(i18n): 新增路由推送页面重构国际化文本"
```

---

## Task 3: 创建 RouteCompareTable 组件

**Files:**
- Create: `frontend/packages/gateway-admin/src/views/pushRoute/components/RouteCompareTable.vue`

- [ ] **Step 1: 创建组件文件骨架**

```vue
<template>
  <el-table
    :data="routes"
    :row-class-name="getRowClassName"
    stripe
    size="small"
    :max-height="maxHeight"
    @row-click="handleRowClick"
  >
    <el-table-column prop="routeId" :label="t('route.routeId')" min-width="180">
      <template #default="{ row }">
        <div class="route-id-cell">
          <span class="diff-marker" :class="row.diffType">
            {{ getDiffMarker(row.diffType) }}
          </span>
          <span class="route-id">{{ row.routeId }}</span>
        </div>
      </template>
    </el-table-column>
    
    <el-table-column :label="t('route.diffType')" width="100" align="center">
      <template #default="{ row }">
        <el-tag :type="getDiffTypeTag(row.diffType)" size="small" effect="light">
          {{ getDiffTypeText(row.diffType) }}
        </el-tag>
      </template>
    </el-table-column>
    
    <el-table-column :label="t('route.repositoryConfig')" min-width="150">
      <template #default="{ row }">
        <div v-if="row.repositoryRoute" class="config-preview">
          <el-tag type="success" size="small" effect="plain">
            {{ row.repositoryRoute.uri }}
          </el-tag>
        </div>
        <span v-else class="empty-cell">-</span>
      </template>
    </el-table-column>
    
    <el-table-column :label="t('route.instanceConfig')" min-width="150">
      <template #default="{ row }">
        <div v-if="row.instanceRoute" class="config-preview">
          <el-tag type="info" size="small" effect="plain">
            {{ row.instanceRoute.uri }}
          </el-tag>
        </div>
        <span v-else class="empty-cell">-</span>
      </template>
    </el-table-column>
    
    <!-- 可展开字段差异 -->
    <el-table-column type="expand" width="50">
      <template #default="{ row }">
        <div v-if="row.diffType === 'modified' && row.fieldDiffs" class="field-diffs">
          <div class="field-diffs-header">{{ t('route.fieldDiff') }}</div>
          <el-table :data="row.fieldDiffs" size="small" border>
            <el-table-column prop="fieldName" :label="t('route.fieldName')" width="120" />
            <el-table-column prop="oldValue" :label="t('route.oldValue')">
              <template #default="{ row: fieldRow }">
                <el-text type="danger" size="small">{{ fieldRow.oldValue || '-' }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="newValue" :label="t('route.newValue')">
              <template #default="{ row: fieldRow }">
                <el-text type="success" size="small">{{ fieldRow.newValue || '-' }}</el-text>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <!-- 完整配置详情 -->
        <div v-else class="route-config-detail">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="URI">
              <el-tag size="small">{{ row.repositoryRoute?.uri || row.instanceRoute?.uri }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Predicates">
              <span class="mono-text">{{ formatPredicates(row) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="Filters">
              <span class="mono-text">{{ formatFilters(row) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { RouteDiffItem, FieldDiff, RouteDefinition } from '@/api/route'

interface Props {
  routes: RouteDiffItem[]
  maxHeight?: number
}

const props = withDefaults(defineProps<Props>(), {
  maxHeight: 400,
})

const emit = defineEmits<{
  rowClick: [route: RouteDiffItem]
}>()

const { t } = useI18n()

// 获取行样式类名
function getRowClassName({ row }: { row: RouteDiffItem }): string {
  const typeMap: Record<string, string> = {
    added: 'diff-row-added',
    modified: 'diff-row-modified',
    deleted: 'diff-row-deleted',
    unchanged: 'diff-row-unchanged',
  }
  return typeMap[row.diffType] || ''
}

// 获取差异标记符号
function getDiffMarker(type: string): string {
  const markerMap: Record<string, string> = {
    added: '+',
    modified: '*',
    deleted: '-',
    unchanged: ' ',
  }
  return markerMap[type] || ' '
}

// 获取差异类型标签样式
function getDiffTypeTag(type: string): 'success' | 'warning' | 'info' | 'danger' {
  const tagMap: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    added: 'success',
    modified: 'warning',
    deleted: 'danger',
    unchanged: 'info',
  }
  return tagMap[type] || 'info'
}

// 获取差异类型文本
function getDiffTypeText(type: string): string {
  const textMap: Record<string, string> = {
    added: t('route.addedRoutes'),
    modified: t('route.modifiedRoutes'),
    deleted: t('route.deletedRoutes'),
    unchanged: t('route.unchangedRoutes'),
  }
  return textMap[type] || type
}

// 格式化断言
function formatPredicates(row: RouteDiffItem): string {
  const route = row.repositoryRoute || row.instanceRoute
  if (!route?.predicates?.length) return '-'
  return route.predicates.map(p => `${p.name}: ${JSON.stringify(p.args)}`).join('; ')
}

// 格式化过滤器
function formatFilters(row: RouteDiffItem): string {
  const route = row.repositoryRoute || row.instanceRoute
  if (!route?.filters?.length) return '-'
  return route.filters.map(f => `${f.name}: ${JSON.stringify(f.args)}`).join('; ')
}

// 处理行点击
function handleRowClick(row: RouteDiffItem) {
  emit('rowClick', row)
}
</script>

<style scoped lang="scss">
.route-id-cell {
  display: flex;
  align-items: center;
  gap: 4px;

  .diff-marker {
    font-weight: bold;
    font-size: 14px;

    &.added {
      color: var(--el-color-success);
    }
    &.modified {
      color: var(--el-color-warning);
    }
    &.deleted {
      color: var(--el-color-danger);
    }
    &.unchanged {
      color: var(--el-text-color-placeholder);
    }
  }

  .route-id {
    font-family: monospace;
  }
}

.config-preview {
  display: flex;
  gap: 4px;
}

.empty-cell {
  color: var(--el-text-color-placeholder);
}

.field-diffs {
  padding: 12px;

  .field-diffs-header {
    font-weight: bold;
    margin-bottom: 8px;
    color: var(--el-text-color-primary);
  }
}

.route-config-detail {
  padding: 12px;

  .mono-text {
    font-family: monospace;
    font-size: 12px;
  }
}

// 差异行背景色
.diff-row-added {
  background-color: rgba(var(--el-color-success-rgb), 0.1) !important;
}
.diff-row-modified {
  background-color: rgba(var(--el-color-warning-rgb), 0.1) !important;
}
.diff-row-deleted {
  background-color: rgba(var(--el-color-danger-rgb), 0.1) !important;
}
</style>
```

- [ ] **Step 2: 运行类型检查**

Run: `cd frontend && pnpm tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: Commit**

```bash
git add frontend/packages/gateway-admin/src/views/pushRoute/components/RouteCompareTable.vue
git commit -m "feat(pushRoute): 创建 RouteCompareTable 路由对比表格组件"
```

---

## Task 4: 创建 RouteDiffDialog 组件

**Files:**
- Create: `frontend/packages/gateway-admin/src/views/pushRoute/components/RouteDiffDialog.vue`

- [ ] **Step 1: 创建差异对比对话框组件**

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="t('pushRoute.diffDialogTitle')"
    width="900px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    class="route-diff-dialog"
    @open="handleOpen"
    @closed="handleClosed"
  >
    <!-- 差异统计条 -->
    <div class="diff-stats-bar" v-if="diffResult">
      <el-tag type="success" effect="plain" size="small">
        {{ t('route.addedRoutes') }}: {{ diffResult.diffStats.addedCount }}
      </el-tag>
      <el-tag type="warning" effect="plain" size="small">
        {{ t('route.modifiedRoutes') }}: {{ diffResult.diffStats.modifiedCount }}
      </el-tag>
      <el-tag type="danger" effect="plain" size="small">
        {{ t('route.deletedRoutes') }}: {{ diffResult.diffStats.deletedCount }}
      </el-tag>
      <el-tag type="info" effect="plain" size="small">
        {{ t('route.unchangedRoutes') }}: {{ diffResult.diffStats.unchangedCount }}
      </el-tag>
    </div>

    <!-- 差异详情表格 -->
    <div class="diff-content" v-loading="diffLoading">
      <RouteCompareTable
        v-if="diffResult && diffResult.diffDetails.length > 0"
        :routes="filteredDiffDetails"
        :max-height="350"
        @row-click="handleRowClick"
      />
      
      <!-- 无差异提示 -->
      <div v-if="diffResult && diffResult.diffDetails.length === 0" class="no-diff-tip">
        <el-empty :description="t('route.noDiff')" size="small" />
      </div>
    </div>

    <!-- 目标实例列表 -->
    <div class="target-instances-section" v-loading="instancesLoading">
      <div class="section-header">
        <span class="section-title">{{ t('pushRoute.targetInstancesTitle') }}</span>
        <span class="instance-count">
          {{ t('pushRoute.onlineInstances', { count: onlineInstances.length }) }}
        </span>
      </div>
      <div class="instance-checkbox-list">
        <el-checkbox-group v-model="selectedInstanceIds">
          <el-checkbox
            v-for="instance in onlineInstances"
            :key="instance.instanceId"
            :label="instance.instanceId"
            :disabled="false"
          >
            <div class="instance-checkbox-item">
              <span class="instance-id">{{ instance.instanceId }}</span>
              <el-tag type="success" size="small" effect="dark">
                {{ t('common.online') }}
              </el-tag>
            </div>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <div v-if="onlineInstances.length === 0" class="no-instance-tip">
        {{ t('pushRoute.noOnlineInstance') }}
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="pushing"
          :disabled="!canPush"
          @click="handleConfirmPush"
        >
          <el-icon><Promotion /></el-icon>
          {{ t('pushRoute.confirmPush') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import RouteCompareTable from './RouteCompareTable.vue'
import {
  getRouteDiff,
  fullPushRoutes,
  type RouteDiffRsp,
  type RouteDiffItem,
} from '@/api/route'
import { queryInstanceList, type InstanceInfo } from '@/api/instance'

interface Props {
  modelValue: boolean
  routesGroup: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'confirm-push': [data: { routesGroup: string; instanceIds: string[] }]
}>()

const { t } = useI18n()

// Dialog 可见性
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// 状态
const diffLoading = ref(false)
const diffResult = ref<RouteDiffRsp | null>(null)
const instancesLoading = ref(false)
const onlineInstances = ref<InstanceInfo[]>([])
const selectedInstanceIds = ref<string[]>([])
const pushing = ref(false)

// 可推送检查
const canPush = computed(() => {
  if (!diffResult.value) return false
  if (selectedInstanceIds.value.length === 0) return false
  // 至少有差异才需要推送
  const stats = diffResult.value.diffStats
  return stats.addedCount > 0 || stats.modifiedCount > 0 || stats.deletedCount > 0
})

// 过滤差异详情（只显示有差异的）
const filteredDiffDetails = computed(() => {
  if (!diffResult.value) return []
  return diffResult.value.diffDetails.filter(
    item => item.diffType !== 'unchanged'
  )
})

// 监听 routesGroup 变化
watch(() => props.routesGroup, (newVal) => {
  if (newVal && visible.value) {
    loadDiff()
    loadInstances()
  }
})

// Dialog 打开时
function handleOpen() {
  if (!props.routesGroup) {
    ElMessage.warning(t('pushRoute.selectGroup'))
    visible.value = false
    return
  }
  selectedInstanceIds.value = []
  loadDiff()
  loadInstances()
}

// Dialog 关闭时
function handleClosed() {
  diffResult.value = null
  onlineInstances.value = []
  selectedInstanceIds.value = []
}

// 加载差异
async function loadDiff() {
  diffLoading.value = true
  try {
    const result = await getRouteDiff({ routesGroup: props.routesGroup })
    diffResult.value = result
  } catch (error) {
    console.error('[RouteDiffDialog] Failed to load diff:', error)
    ElMessage.error(t('message.operationFailed'))
    diffResult.value = null
  } finally {
    diffLoading.value = false
  }
}

// 加载实例
async function loadInstances() {
  instancesLoading.value = true
  try {
    const res = await queryInstanceList({
      groupKey: props.routesGroup,
      status: 0, // 只查询在线实例
      pageNum: 1,
      pageSize: 100,
    })
    onlineInstances.value = res.rows || []
    // 默认全选在线实例
    selectedInstanceIds.value = onlineInstances.value.map(i => i.instanceId)
  } catch (error) {
    console.error('[RouteDiffDialog] Failed to load instances:', error)
    onlineInstances.value = []
  } finally {
    instancesLoading.value = false
  }
}

// 处理行点击
function handleRowClick(route: RouteDiffItem) {
  console.log('[RouteDiffDialog] Row clicked:', route.routeId)
}

// 确认推送
async function handleConfirmPush() {
  if (!canPush.value) {
    ElMessage.warning(t('pushRoute.selectRoutesAndInstances'))
    return
  }

  pushing.value = true
  try {
    emit('confirm-push', {
      routesGroup: props.routesGroup,
      instanceIds: selectedInstanceIds.value,
    })
    // 注意：不在这里关闭弹窗，由父组件通过 BlinkTaskDialog 处理
  } finally {
    pushing.value = false
  }
}
</script>

<style scoped lang="scss">
.route-diff-dialog {
  .diff-stats-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
    padding: 8px 12px;
    background: var(--el-fill-color-light);
    border-radius: 4px;
  }

  .diff-content {
    margin-bottom: 16px;
  }

  .no-diff-tip {
    padding: 20px;
    text-align: center;
  }

  .target-instances-section {
    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      .section-title {
        font-weight: bold;
        color: var(--el-text-color-primary);
      }

      .instance-count {
        color: var(--el-color-success);
        font-size: 14px;
      }
    }

    .instance-checkbox-list {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;

      .instance-checkbox-item {
        display: flex;
        align-items: center;
        gap: 8px;

        .instance-id {
          font-family: monospace;
          font-size: 13px;
        }
      }
    }

    .no-instance-tip {
      color: var(--el-text-color-placeholder);
      padding: 12px;
      text-align: center;
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>
```

- [ ] **Step 2: 运行类型检查**

Run: `cd frontend && pnpm tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: Commit**

```bash
git add frontend/packages/gateway-admin/src/views/pushRoute/components/RouteDiffDialog.vue
git commit -m "feat(pushRoute): 创建 RouteDiffDialog 路由差异对比对话框组件"
```

---

## Task 5: 重构 pushRoute/index.vue 页面（三列布局）

**Files:**
- Modify: `frontend/packages/gateway-admin/src/views/pushRoute/index.vue`

这是一个大型重构任务，分为多个子步骤。

- [ ] **Step 1: 重构页面模板 - 页面头部**

替换现有 `<template>` 内容为新的三列布局结构。首先替换页面头部：

```vue
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
                  {{ instance.status === 0 ? t('common.online') : t('common.offline') }}
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
```

- [ ] **Step 2: 重构脚本部分 - 状态和计算属性**

替换 `<script setup>` 部分：

```typescript
<script setup lang="ts">
/**
 * 路由推送页面 - 重构版本
 * 分组驱动的推送流程，三列布局展示
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, Search } from '@element-plus/icons-vue'
import {
  BlinkTaskDialog,
  useTaskRunner,
  TaskStatus,
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
const instSearchKeyword = ref('')

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
  onComplete: (result) => {
    // 校验结果处理
    handleVerifyResult(result)
  },
  onCancel: () => {
    ElMessage.warning(t('common.cancelled'))
  },
  onError: (error) => {
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
  if (!instSearchKeyword.value) return instanceRoutes.value
  const keyword = instSearchKeyword.value.toLowerCase()
  return instanceRoutes.value.filter(route =>
    route.routeId.toLowerCase().includes(keyword) ||
    route.uri.toLowerCase().includes(keyword)
  )
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
    task: async (onProgress, signal) => {
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
          routesGroup,
        })

        // 获取推送记录 ID（用于后续校验）
        try {
          const latestPush = await routeApi.getLatestPush({ instanceId: instanceIds[0] })
          pushId = latestPush?.pushId
        } catch {
          // 忽略获取推送记录失败
        }
      } catch (error: any) {
        throw new Error(`${t('pushRoute.stepPushRoutes')}失败: ${error.message}`)
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
        return { verified: false, reason: '无法获取推送记录ID' }
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
  ElMessage.info(t('common.backgroundRunning'))
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
```

- [ ] **Step 3: 重构样式部分**

替换 `<style scoped lang="scss">` 部分：

```scss
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
```

- [ ] **Step 4: 运行完整构建验证**

Run: `cd frontend && pnpm build:gateway-admin`
Expected: 构建成功

- [ ] **Step 5: 启动开发服务器测试**

Run: `cd frontend && pnpm dev:gateway-admin`
Expected: 开发服务器启动成功，访问 http://localhost:3001/pushRoute 页面能正常显示

- [ ] **Step 6: Commit**

```bash
git add frontend/packages/gateway-admin/src/views/pushRoute/index.vue
git commit -m "feat(pushRoute): 重构路由推送页面为三列布局分组驱动流程"
```

---

## Task 6: 验证整体功能

**Files:**
- None (测试验证)

- [ ] **Step 1: 手动测试 - 分组选择流程**

操作步骤：
1. 打开路由推送页面
2. 选择一个路由分组
3. 验证三列数据是否正确加载：
   - 左列：仓库路由（数据库启用状态路由）
   - 中列：实例路由（从实例获取）
   - 右列：关联实例（分组下实例列表）

- [ ] **Step 2: 手动测试 - 差异对比流程**

操作步骤：
1. 点击"推送"按钮
2. 验证差异对话框是否正确显示：
   - 差异统计条
   - 差异详情表格
   - 目标实例列表（checkbox）
3. 验证差异类型标记是否正确（+ 绿色、* 黄色、- 红色）

- [ ] **Step 3: 手动测试 - 任务进度流程**

操作步骤：
1. 点击"确认推送"
2. 验证任务进度弹窗是否正确显示：
   - 四步骤指示器
   - 进度条和消息
3. 等待任务完成，验证校验结果

- [ ] **Step 4: 最终 Commit**

```bash
git add -A
git commit -m "feat(pushRoute): 完成路由推送页面重构 - 三列布局、差异对比、四步骤任务进度"
```

---

## 自我审查清单

完成后检查以下内容：

| 项目 | 检查结果 |
|------|----------|
| API 类型定义完整性 | ✓/✗ |
| 国际化文本完整性（中英文） | ✓/✗ |
| RouteCompareTable 组件正确导出 | ✓/✗ |
| RouteDiffDialog 组件正确接收 props | ✓/✗ |
| pushRoute/index.vue 三列布局正确渲染 | ✓/✗ |
| useTaskRunner 四步骤流程正确配置 | ✓/✗ |
| 校验失败阻止关闭弹窗逻辑正确 | ✓/✗ |
| 无 TypeScript 类型错误 | ✓/✗ |
| 构建成功 | ✓/✗ |