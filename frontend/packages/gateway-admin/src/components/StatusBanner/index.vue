<template>
  <div v-if="status" class="status-banner" :class="statusClass">
    <div class="status-indicator" :class="statusClass"></div>
    <span class="status-text">{{ t(statusText) }}</span>
    <span class="status-detail">{{ statusDetail }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getStatistics, type StatisticsInfo } from '@/api/monitor'

defineOptions({ name: 'StatusBanner' })

const { t } = useI18n()

const statistics = ref<StatisticsInfo | null>(null)
let refreshInterval: number | null = null

const status = computed(() => {
  if (!statistics.value) return null

  const { totalInstances, healthyInstances } = statistics.value
  if (totalInstances === 0) return 'unknown'

  const healthyRate = healthyInstances / totalInstances
  if (healthyRate >= 0.9) return 'operational'
  if (healthyRate >= 0.5) return 'degraded'
  return 'down'
})

const statusClass = computed(() => {
  switch (status.value) {
    case 'operational':
      return 'status-ok'
    case 'degraded':
      return 'status-warning'
    case 'down':
      return 'status-error'
    default:
      return 'status-unknown'
  }
})

const statusText = computed(() => {
  switch (status.value) {
    case 'operational':
      return 'common.allSystemsOperational'
    case 'degraded':
      return 'common.degraded'
    case 'down':
      return 'common.systemDown'
    default:
      return 'common.unknown'
  }
})

const statusDetail = computed(() => {
  if (!statistics.value) return ''
  const { totalInstances, healthyInstances } = statistics.value
  return `${healthyInstances}/${totalInstances} ${t('common.instancesHealthy')}`
})

const fetchStatistics = async () => {
  try {
    statistics.value = await getStatistics({})
  } catch (error) {
    console.error('Failed to fetch statistics:', error)
  }
}

onMounted(() => {
  fetchStatistics()
  // Refresh every 10 seconds
  refreshInterval = window.setInterval(fetchStatistics, 10000)
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<style scoped lang="scss">
.status-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  margin-bottom: 16px;

  &.status-ok {
    background-color: rgba(16, 185, 129, 0.1);
    color: var(--success-color);
  }

  &.status-warning {
    background-color: rgba(245, 158, 11, 0.1);
    color: var(--warning-color);
  }

  &.status-error {
    background-color: rgba(239, 68, 68, 0.1);
    color: var(--danger-color);
  }

  &.status-unknown {
    background-color: var(--bg-color-page);
    color: var(--text-color-secondary);
  }
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.status-ok {
    background-color: var(--success-color);
  }
  &.status-warning {
    background-color: var(--warning-color);
  }
  &.status-error {
    background-color: var(--danger-color);
  }
  &.status-unknown {
    background-color: var(--text-color-placeholder);
  }
}

.status-text {
  font-weight: 500;
}

.status-detail {
  color: var(--text-color-secondary);
  margin-left: auto;
}
</style>
