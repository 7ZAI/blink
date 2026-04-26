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
        v-if="diffResult && filteredDiffDetails.length > 0"
        :routes="filteredDiffDetails"
        :max-height="350"
      />
      <el-empty v-else-if="diffResult" :description="t('route.noDiff')" size="small" />
    </div>

    <!-- 目标实例列表 -->
    <div class="target-instances-section" v-loading="instancesLoading">
      <div class="section-header">
        <span class="section-title">{{ t('pushRoute.targetInstancesTitle') }}</span>
        <span class="instance-count">
          {{ t('pushRoute.onlineInstances', { count: onlineInstances.length }) }}
        </span>
      </div>
      <el-checkbox-group v-model="selectedInstanceIds" class="instance-checkbox-group">
        <el-checkbox
          v-for="inst in onlineInstances"
          :key="inst.instanceId"
          :label="inst.instanceId"
          class="instance-checkbox"
        >
          <span class="instance-id">{{ inst.instanceId }}</span>
          <el-tag type="success" size="small" effect="dark">{{ t('common.statusOnline') }}</el-tag>
        </el-checkbox>
      </el-checkbox-group>
      <div v-if="onlineInstances.length === 0" class="no-instance-tip">
        {{ t('common.noOnlineInstances') }}
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :disabled="!canPush"
          @click="handleConfirmPush"
        >
          {{ t('pushRoute.confirmPush') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import RouteCompareTable from './RouteCompareTable.vue'
import { getRouteDiff } from '@/api/route'
import type { RouteDiffRsp, RouteDiffItem } from '@/api/route'
import { queryInstanceList } from '@/api/instance'
import type { InstanceInfo } from '@/api/instance'

const { t } = useI18n()

// Props
interface Props {
  modelValue: boolean
  routesGroup: string
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'confirm-push': [data: { routesGroup: string; instanceIds: string[] }]
}>()

// Dialog visibility
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// State
const diffLoading = ref(false)
const diffResult = ref<RouteDiffRsp | null>(null)
const instancesLoading = ref(false)
const onlineInstances = ref<InstanceInfo[]>([])
const selectedInstanceIds = ref<string[]>([])

// 过滤差异详情，只显示有变化的项（新增、修改、删除）
const filteredDiffDetails = computed<RouteDiffItem[]>(() => {
  if (!diffResult.value?.diffDetails) return []
  return diffResult.value.diffDetails.filter(
    (item) => item.diffType !== 'unchanged'
  )
})

// Can push check
const canPush = computed(() => {
  if (!diffResult.value) return false
  if (selectedInstanceIds.value.length === 0) return false
  // 至少有差异才需要推送
  const stats = diffResult.value.diffStats
  return stats.addedCount > 0 || stats.modifiedCount > 0 || stats.deletedCount > 0
})

// Handle dialog open
const handleOpen = async () => {
  if (!props.routesGroup) {
    ElMessage.warning(t('route.selectGroupFirst'))
    visible.value = false
    return
  }

  loadDiff()
  loadInstances()
}

// Load diff
const loadDiff = async () => {
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

// Load instances
const loadInstances = async () => {
  instancesLoading.value = true
  try {
    const res = await queryInstanceList({
      groupKey: props.routesGroup,
      status: 0, // 只查询在线实例
      pageNum: 1,
      pageSize: 100
    })
    onlineInstances.value = res.rows || []
    // 默认全选所有在线实例
    selectedInstanceIds.value = onlineInstances.value.map((inst) => inst.instanceId)
  } catch (error) {
    console.error('[RouteDiffDialog] Failed to load instances:', error)
    onlineInstances.value = []
    selectedInstanceIds.value = []
  } finally {
    instancesLoading.value = false
  }
}

// Handle dialog closed
const handleClosed = () => {
  diffResult.value = null
  onlineInstances.value = []
  selectedInstanceIds.value = []
}

// Handle confirm push
const handleConfirmPush = () => {
  if (!canPush.value) return

  emit('confirm-push', {
    routesGroup: props.routesGroup,
    instanceIds: selectedInstanceIds.value
  })

  visible.value = false
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
    margin-bottom: 20px;
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

    .instance-checkbox-group {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .instance-checkbox {
      display: flex;
      align-items: center;
      padding: 4px 8px;
      background: var(--el-fill-color-light);
      border-radius: 4px;
      margin-right: 0;

      .instance-id {
        font-family: monospace;
        font-size: 13px;
        margin-right: 8px;
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