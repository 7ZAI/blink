<template>
  <div v-if="status" class="status-banner" :class="statusClass">
    <div class="status-indicator" :class="statusClass"></div>
    <span class="status-text">{{ t(statusText) }}</span>
    <span class="status-detail">{{ statusDetail }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'StatusBanner' })

const { t } = useI18n()

/**
 * 统计信息接口
 */
interface StatisticsInfo {
  totalInstances: number
  healthyInstances: number
}

/**
 * Props - 可选的外部统计数据传入
 */
const props = defineProps<{
  /** 外部传入的统计数据（可选） */
  statistics?: StatisticsInfo
}>()

/**
 * 从父组件注入的统计数据（可选）
 * 父组件可通过 provide('statistics', computed(() => dashboardStore.statistics)) 注入
 */
const injectedStatistics = inject<ComputedRef<StatisticsInfo> | null>('statistics', null)

/**
 * 获取统计数据
 * 优先级：props > inject > 默认空值
 */
const statistics = computed<StatisticsInfo>(() => {
  if (props.statistics) {
    return props.statistics
  }
  if (injectedStatistics?.value) {
    return injectedStatistics.value
  }
  return { totalInstances: 0, healthyInstances: 0 }
})

const status = computed(() => {
  const { totalInstances, healthyInstances } = statistics.value
  if (totalInstances === 0) return null

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
  const { totalInstances, healthyInstances } = statistics.value
  if (totalInstances === 0) return ''
  return `${healthyInstances}/${totalInstances} ${t('common.instancesHealthy')}`
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