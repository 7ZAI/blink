<template>
  <el-row :gutter="16" class="summary-cards">
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.totalCircuitBreakers') }}</div>
          <div class="summary-value">{{ overview?.totalCircuitBreakers || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card closed">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.closedCount') }}</div>
          <div class="summary-value success">{{ overview?.closedCount || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card open">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.openCount') }}</div>
          <div class="summary-value danger">{{ overview?.openCount || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card half-open">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.halfOpenCount') }}</div>
          <div class="summary-value warning">{{ overview?.halfOpenCount || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.totalInstances') }}</div>
          <div class="summary-value">{{ overview?.totalInstances || 0 }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="4">
      <el-card shadow="hover" class="summary-card health">
        <div class="summary-item">
          <div class="summary-label">{{ t('monitor.healthScore') }}</div>
          <div class="summary-value" :class="healthLevel">
            {{ (overview?.healthScore || 100).toFixed(1) }}%
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CircuitBreakerOverviewNew } from '@/api/circuitBreaker'

defineOptions({ name: 'SummaryCards' })

const props = defineProps<{
  overview: CircuitBreakerOverviewNew | null
}>()

const { t } = useI18n()

const healthLevel = computed(() => {
  const score = props.overview?.healthScore ?? 100
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
})
</script>

<style scoped lang="scss">
.summary-cards {
  margin-bottom: 16px;

  .summary-card {
    .summary-item {
      text-align: center;

      .summary-label {
        font-size: 12px;
        color: var(--el-text-color-secondary);
        margin-bottom: 8px;
      }

      .summary-value {
        font-size: 24px;
        font-weight: 600;

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
}
</style>
