<template>
  <el-card shadow="never" class="circuit-breaker-list-card">
    <el-table
      :data="overview?.circuitBreakers || []"
      v-loading="loading"
      stripe
      @row-click="handleRowClick"
    >
      <el-table-column prop="name" :label="t('monitor.circuitBreakerName')" min-width="180">
        <template #default="{ row }">
          <span class="cb-name">{{ row.name }}</span>
          <el-tag
            v-if="row.baseConfig"
            size="small"
            type="info"
            effect="plain"
            class="base-config-tag"
          >
            {{ row.baseConfig }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="failureRateThreshold"
        :label="t('monitor.failureRateThreshold')"
        width="100"
      >
        <template #default="{ row }">
          {{ row.failureRateThreshold }}%
        </template>
      </el-table-column>
      <el-table-column
        prop="slidingWindowSize"
        :label="t('monitor.slidingWindowSize')"
        width="100"
      />
      <el-table-column
        prop="minimumNumberOfCalls"
        :label="t('monitor.minimumNumberOfCalls')"
        width="100"
      />
      <el-table-column
        prop="waitDurationInOpenState"
        :label="t('monitor.waitDurationInOpenState')"
        width="120"
      >
        <template #default="{ row }">
          {{ row.waitDurationInOpenState }}s
        </template>
      </el-table-column>
      <el-table-column :label="t('monitor.stateDistribution')" min-width="200">
        <template #default="{ row }">
          <div class="state-distribution">
            <span class="state-item success">
              CLOSED: {{ row.closedCount || 0 }}
            </span>
            <span v-if="row.openCount > 0" class="state-item danger">
              OPEN: {{ row.openCount }}
            </span>
            <span v-if="row.halfOpenCount > 0" class="state-item warning">
              HALF_OPEN: {{ row.halfOpenCount }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.operation')" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="viewDetail(row)">
            {{ t('common.detail') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { CircuitBreakerOverviewNew, CircuitBreakerSummary } from '@/api/circuitBreaker'

defineOptions({ name: 'CircuitBreakerList' })

const props = defineProps<{
  overview: CircuitBreakerOverviewNew | null
  loading: boolean
}>()

const emit = defineEmits<{
  viewDetail: [cb: CircuitBreakerSummary]
}>()

const { t } = useI18n()

const viewDetail = (cb: CircuitBreakerSummary) => {
  emit('viewDetail', cb)
}

const handleRowClick = (row: CircuitBreakerSummary) => {
  viewDetail(row)
}
</script>

<style scoped lang="scss">
.circuit-breaker-list-card {
  .cb-name {
    font-weight: 500;
  }

  .base-config-tag {
    margin-left: 8px;
  }

  .state-distribution {
    display: flex;
    gap: 16px;

    .state-item {
      font-size: 13px;

      &.success {
        color: var(--el-color-success);
      }
      &.danger {
        color: var(--el-color-danger);
      }
      &.warning {
        color: var(--el-color-warning);
      }
    }
  }
}
</style>
