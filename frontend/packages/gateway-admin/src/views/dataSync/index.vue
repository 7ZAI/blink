<template>
  <div class="data-sync-page table-page-container">
    <!-- 同步操作区 -->
    <el-card class="sync-card shrink-0" shadow="never">
      <template #header>
        <span class="card-title">{{ t('dataSync.syncOperation') }}</span>
      </template>
      <div class="sync-buttons">
        <el-button type="primary" :loading="syncing" @click="handleSync('channel')">
          <el-icon><Refresh /></el-icon>
          {{ t('dataSync.channelSync') }}
        </el-button>
        <el-button type="primary" :loading="syncing" @click="handleSync('route')">
          <el-icon><Refresh /></el-icon>
          {{ t('dataSync.routeSync') }}
        </el-button>
        <el-button type="primary" :loading="syncing" @click="handleSync('config')">
          <el-icon><Refresh /></el-icon>
          {{ t('dataSync.configSync') }}
        </el-button>
      </div>
    </el-card>

    <!-- 一致性检查区 -->
    <el-card class="check-card flex-1 flex flex-col overflow-hidden" shadow="never">
      <template #header>
        <div class="check-header">
          <span class="card-title">{{ t('dataSync.consistencyCheck') }}</span>
          <div class="check-actions">
            <el-select v-model="checkType" style="width: 120px">
              <el-option :label="t('dataSync.channel')" value="channel" />
              <el-option :label="t('dataSync.route')" value="route" />
              <el-option :label="t('dataSync.config')" value="config" />
            </el-select>
            <el-button type="primary" :loading="checking" @click="handleCheck">
              <el-icon><Search /></el-icon>
              {{ t('dataSync.startCheck') }}
            </el-button>
          </div>
        </div>
      </template>

      <!-- 检查结果表格 -->
      <div class="table-wrapper">
        <el-table
          v-loading="checking"
          :data="checkResult?.dbItems || []"
          height="100%"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column :label="t('dataSync.name')" prop="key" min-width="160" />
          <el-table-column :label="t('dataSync.database')" width="100" align="center">
            <template #default>
              <el-tag type="success">✅</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-for="instance in checkResult?.instances"
            :key="instance.instanceId"
            :label="instance.instanceId"
            min-width="120"
            align="center"
          >
            <template #default="{ row }">
              <template v-if="getInstanceItemStatus(instance, row.key) === 'MATCH'">
                <el-tag type="success">{{ t('dataSync.match') }}</el-tag>
              </template>
              <template v-else-if="getInstanceItemStatus(instance, row.key) === 'MISMATCH'">
                <el-tag type="warning">{{ t('dataSync.mismatch') }}</el-tag>
              </template>
              <template v-else-if="getInstanceItemStatus(instance, row.key) === 'MISSING'">
                <el-tag type="danger">{{ t('dataSync.missing') }}</el-tag>
              </template>
              <template v-else>
                <el-tag type="info">-</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleSyncSingle(row.key)">
                {{ t('dataSync.sync') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 批量操作 -->
      <div class="batch-actions" v-if="selectedKeys.length > 0">
        <el-button type="primary" @click="handleSyncSelected">
          {{ t('dataSync.syncSelected') }} ({{ selectedKeys.length }})
        </el-button>
      </div>
    </el-card>

    <!-- 同步日志区 -->
    <el-card class="log-card shrink-0" shadow="never">
      <template #header>
        <div class="log-header">
          <span class="card-title">{{ t('dataSync.syncLog') }}</span>
          <el-button link type="primary" @click="loadSyncLogs">
            <el-icon><Refresh /></el-icon>
            {{ t('common.refresh') }}
          </el-button>
        </div>
      </template>
      <el-table v-loading="loadingLogs" :data="syncLogs" max-height="280" stripe>
        <el-table-column :label="t('dataSync.syncTime')" prop="createTime" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('dataSync.syncType')" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ t(`dataSync.${row.syncType}`) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('dataSync.operator')" prop="operator" min-width="100" show-overflow-tooltip />
        <el-table-column :label="t('dataSync.status')" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.status === 0 ? t('common.success') : t('common.failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('dataSync.instanceCount')" min-width="120" align="center">
          <template #default="{ row }">
            <span :class="row.successCount === row.instanceCount ? 'success-count' : 'partial-count'">
              {{ row.successCount }} / {{ row.instanceCount }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  checkConsistency,
  syncData,
  getSyncLogs,
  type CacheCheckRsp,
  type SyncLogItem,
  type InstanceCacheStatus,
} from '@/api/dataSync'

defineOptions({
  name: 'DataSync',
})

const { t } = useI18n()

// 状态
const checkType = ref<'channel' | 'route' | 'config'>('channel')
const checking = ref(false)
const syncing = ref(false)
const loadingLogs = ref(false)
const checkResult = ref<CacheCheckRsp | null>(null)
const syncLogs = ref<SyncLogItem[]>([])
const selectedKeys = ref<string[]>([])

/**
 * 执行一致性检查
 */
const handleCheck = async () => {
  checking.value = true
  try {
    const res = await checkConsistency({ type: checkType.value })
    checkResult.value = res
    ElMessage.success(t('dataSync.checkSuccess'))
  } catch (error) {
    console.error('[DataSync] Check failed:', error)
  } finally {
    checking.value = false
  }
}

/**
 * 全量同步
 */
const handleSync = async (type: 'channel' | 'route' | 'config') => {
  try {
    await ElMessageBox.confirm(t('dataSync.confirmSyncAll'), t('message.tips'), { type: 'warning' })
    syncing.value = true
    await syncData({ type, syncAll: true })
    ElMessage.success(t('dataSync.syncSuccess'))
    loadSyncLogs()
  } catch {
    // 用户取消
  } finally {
    syncing.value = false
  }
}

/**
 * 单项同步
 */
const handleSyncSingle = async (key: string) => {
  try {
    syncing.value = true
    await syncData({ type: checkType.value, keys: [key], syncAll: false })
    ElMessage.success(t('dataSync.syncSuccess'))
    handleCheck()
    loadSyncLogs()
  } catch (error) {
    console.error('[DataSync] Sync failed:', error)
  } finally {
    syncing.value = false
  }
}

/**
 * 批量同步选中项
 */
const handleSyncSelected = async () => {
  try {
    await ElMessageBox.confirm(
      t('dataSync.confirmSync', { count: selectedKeys.value.length }),
      t('message.tips'),
      { type: 'warning' }
    )
    syncing.value = true
    await syncData({ type: checkType.value, keys: selectedKeys.value, syncAll: false })
    ElMessage.success(t('dataSync.syncSuccess'))
    handleCheck()
    loadSyncLogs()
  } catch {
    // 用户取消
  } finally {
    syncing.value = false
  }
}

/**
 * 表格选择变更
 */
const handleSelectionChange = (selection: { key: string }[]) => {
  selectedKeys.value = selection.map((item) => item.key)
}

/**
 * 获取实例中某项的状态
 */
const getInstanceItemStatus = (instance: InstanceCacheStatus, key: string): string => {
  const item = instance.items.find((i) => i.key === key)
  return item?.status || 'MISSING'
}

/**
 * 加载同步日志
 */
const loadSyncLogs = async () => {
  loadingLogs.value = true
  try {
    const res = await getSyncLogs(1, 10)
    syncLogs.value = res.rows || []
  } catch (error) {
    console.error('[DataSync] Load logs failed:', error)
  } finally {
    loadingLogs.value = false
  }
}

onMounted(() => {
  loadSyncLogs()
})
</script>

<style scoped lang="scss">
.data-sync-page {
  gap: 16px;
}

.sync-card {
  .sync-buttons {
    display: flex;
    gap: 12px;
  }
}

.check-card {
  .check-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .check-actions {
      display: flex;
      gap: 12px;
    }
  }
}

.batch-actions {
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
}

.log-card {
  max-height: 380px;

  .log-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.success-count {
  color: var(--el-color-success);
  font-weight: 500;
}

.partial-count {
  color: var(--el-color-warning);
  font-weight: 500;
}

.card-title {
  font-weight: 500;
  font-size: 15px;
}
</style>
