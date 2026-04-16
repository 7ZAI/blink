<template>
  <div class="circuit-breaker-page">
    <!-- Header -->
    <div class="page-header">
      <h3>{{ t('monitor.circuitBreaker') }}</h3>
      <div class="header-actions">
        <el-button @click="refresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <!-- Main Content - Left Right Split -->
    <div class="page-content">
      <!-- Left Panel - Instance List -->
      <div class="left-panel">
        <InstancePanel
          :instances="instances"
          :selected-instance-id="selectedInstanceId"
          :overview="overview"
          @select="selectInstance"
        />
      </div>

      <!-- Right Panel - Details -->
      <div class="right-panel">
        <!-- Summary Cards -->
        <SummaryCards :overview="overview" />

        <!-- Circuit Breaker List -->
        <CircuitBreakerList
          :overview="overview"
          :loading="loading"
          @view-detail="viewDetail"
        />
      </div>
    </div>

    <!-- Detail Dialog -->
    <el-dialog
      v-model="detailVisible"
      :title="t('monitor.circuitBreakerDetail')"
      width="800px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <div v-if="selectedCb" class="detail-content">
        <!-- Config Section -->
        <div class="detail-section">
          <h4>{{ t('monitor.configParams') }}</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="t('monitor.circuitBreakerName')">
              {{ selectedCb.name }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.baseConfig')">
              {{ selectedCb.baseConfig || '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.failureRateThreshold')">
              {{ selectedCb.failureRateThreshold }}%
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.slidingWindowSize')">
              {{ selectedCb.slidingWindowSize }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.minimumNumberOfCalls')">
              {{ selectedCb.minimumNumberOfCalls }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('monitor.waitDurationInOpenState')">
              {{ selectedCb.waitDurationInOpenState }}s
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Instance Status Section -->
        <div v-if="detail?.instances?.length" class="detail-section">
          <h4>{{ t('monitor.instanceStatus') }}</h4>
          <el-table :data="detail.instances" stripe size="small">
            <el-table-column prop="instanceId" :label="t('common.instanceId')" min-width="200" />
            <el-table-column prop="state" :label="t('monitor.state')" width="120">
              <template #default="{ row }">
                <el-tag :type="getStateType(row.state)" size="small">
                  {{ row.state }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failureRate" :label="t('monitor.failureRate')" width="100">
              <template #default="{ row }">
                {{ row.failureRate?.toFixed(2) || 0 }}%
              </template>
            </el-table-column>
            <el-table-column prop="numberOfCalls" :label="t('monitor.numberOfCalls')" width="100" />
            <el-table-column prop="numberOfFailedCalls" :label="t('monitor.numberOfFailedCalls')" width="100" />
          </el-table>
        </div>

        <!-- State History Section -->
        <div v-if="selectedInstanceId" class="detail-section">
          <StateHistory
            :instance-id="selectedInstanceId"
            :cb-name="selectedCb.name"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'
import { useCircuitBreaker } from '@/composables/useCircuitBreaker'
import type { CircuitBreakerSummary } from '@/api/circuitBreaker'
import InstancePanel from './components/InstancePanel.vue'
import SummaryCards from './components/SummaryCards.vue'
import CircuitBreakerList from './components/CircuitBreakerList.vue'
import StateHistory from './components/StateHistory.vue'

defineOptions({ name: 'CircuitBreakerMonitor' })

const { t } = useI18n()

const {
  overview,
  instances,
  selectedInstanceId,
  detail,
  loading,
  refresh,
  selectInstance,
  fetchDetail,
} = useCircuitBreaker()

const detailVisible = ref(false)
const selectedCb = ref<CircuitBreakerSummary | null>(null)

const viewDetail = async (cb: CircuitBreakerSummary) => {
  selectedCb.value = cb
  detailVisible.value = true
  await fetchDetail(cb.name)
}

const getStateType = (state: string): 'success' | 'danger' | 'warning' | 'info' => {
  switch (state) {
    case 'CLOSED':
      return 'success'
    case 'OPEN':
      return 'danger'
    case 'HALF_OPEN':
      return 'warning'
    default:
      return 'info'
  }
}

onMounted(() => {
  // 初始化数据由 useCircuitBreaker composable 自动处理
})
</script>

<style scoped lang="scss">
.circuit-breaker-page {
  height: 100%;
  display: flex;
  flex-direction: column;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 0 4px;

    h3 {
      font-size: 20px;
      font-weight: 600;
      margin: 0;
    }
  }

  .page-content {
    flex: 1;
    display: flex;
    gap: 16px;
    overflow: hidden;

    .left-panel {
      width: 280px;
      flex-shrink: 0;
    }

    .right-panel {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
    }
  }

  .detail-content {
    .detail-section {
      margin-bottom: 24px;

      h4 {
        font-size: 14px;
        font-weight: 500;
        margin-bottom: 12px;
      }
    }
  }
}
</style>
