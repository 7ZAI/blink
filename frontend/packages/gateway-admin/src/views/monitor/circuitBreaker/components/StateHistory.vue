<template>
  <div class="state-history">
    <div class="history-header">
      <span class="title">{{ t('monitor.stateTransitionHistory') }}</span>
      <span class="subtitle">{{ cbName }}</span>
    </div>

    <div v-if="loading" class="history-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="history.length === 0" class="history-empty">
      <el-empty :description="t('monitor.noHistory')" :image-size="60" />
    </div>

    <div v-else class="history-timeline">
      <el-timeline>
        <el-timeline-item
          v-for="(item, index) in history"
          :key="index"
          :type="getTimelineType(item.toState)"
          :timestamp="formatTime(item.timestamp)"
          placement="top"
        >
          <div class="timeline-content">
            <div class="transition-info">
              <el-tag :type="getStateType(item.fromState)" size="small">
                {{ item.fromState }}
              </el-tag>
              <el-icon><Right /></el-icon>
              <el-tag :type="getStateType(item.toState)" size="small">
                {{ item.toState }}
              </el-tag>
            </div>
            <div class="transition-meta">
              <span>{{ t('monitor.reason') }}: {{ item.reason }}</span>
              <span v-if="item.failureRate !== undefined">
                {{ t('monitor.failureRate') }}: {{ item.failureRate.toFixed(1) }}%
              </span>
              <span v-if="item.numberOfCalls !== undefined">
                {{ t('monitor.numberOfCalls') }}: {{ item.numberOfCalls }}
              </span>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Right } from '@element-plus/icons-vue'
import type { StateTransitionHistory } from '@/api/circuitBreaker'
import { circuitBreakerApi } from '@/api/circuitBreaker'

defineOptions({ name: 'StateHistory' })

const props = defineProps<{
  instanceId: string
  cbName: string
}>()

const { t } = useI18n()

const loading = ref(false)
const history = ref<StateTransitionHistory[]>([])

const fetchHistory = async () => {
  if (!props.instanceId || !props.cbName) return

  loading.value = true
  try {
    history.value = await circuitBreakerApi.getHistory(props.instanceId, props.cbName, 20)
  } catch (error) {
    console.error('[StateHistory] 获取历史失败:', error)
  } finally {
    loading.value = false
  }
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

const getTimelineType = (toState: string): 'success' | 'danger' | 'warning' | 'primary' => {
  switch (toState) {
    case 'CLOSED':
      return 'success'
    case 'OPEN':
      return 'danger'
    case 'HALF_OPEN':
      return 'warning'
    default:
      return 'primary'
  }
}

const formatTime = (timestamp: number): string => {
  return new Date(timestamp).toLocaleString()
}

watch(() => [props.instanceId, props.cbName], fetchHistory, { immediate: true })
onMounted(fetchHistory)
</script>

<style scoped lang="scss">
.state-history {
  .history-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .title {
      font-size: 14px;
      font-weight: 500;
    }

    .subtitle {
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }
  }

  .history-loading,
  .history-empty {
    padding: 16px;
  }

  .history-timeline {
    .timeline-content {
      .transition-info {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;
      }

      .transition-meta {
        display: flex;
        gap: 16px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}
</style>
