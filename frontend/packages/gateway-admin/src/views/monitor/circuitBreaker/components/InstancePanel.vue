<template>
  <div class="instance-panel">
    <div class="panel-header">
      <span class="title">{{ t('monitor.instanceList') }}</span>
    </div>

    <div class="instance-list">
      <!-- 聚合视图选项 -->
      <div
        class="instance-item aggregate"
        :class="{ active: !selectedInstanceId }"
        @click="selectInstance(null)"
      >
        <el-icon><DataLine /></el-icon>
        <span class="name">{{ t('monitor.aggregateView') }}</span>
        <el-tag v-if="overview" size="small" type="info">
          {{ overview.totalInstances }} {{ t('monitor.instances') }}
        </el-tag>
      </div>

      <el-divider />

      <!-- 实例列表 -->
      <div
        v-for="instance in instances"
        :key="instance.instanceId"
        class="instance-item"
        :class="{ active: selectedInstanceId === instance.instanceId }"
        @click="selectInstance(instance.instanceId)"
      >
        <div class="instance-info">
          <div class="instance-header">
            <span class="name">{{ instance.instanceId }}</span>
            <el-tag
              :type="instance.status === 'ONLINE' ? 'success' : 'danger'"
              size="small"
              effect="light"
            >
              {{ instance.status }}
            </el-tag>
          </div>
          <div class="instance-meta">
            <span>{{ instance.host }}:{{ instance.port }}</span>
          </div>
        </div>

        <div class="circuit-breaker-summary">
          <span
            v-if="instance.summary.open > 0"
            class="state-badge danger"
          >
            OPEN: {{ instance.summary.open }}
          </span>
          <span
            v-if="instance.summary.halfOpen > 0"
            class="state-badge warning"
          >
            HALF_OPEN: {{ instance.summary.halfOpen }}
          </span>
          <span class="state-badge success">
            CLOSED: {{ instance.summary.closed }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { DataLine } from '@element-plus/icons-vue'
import type { InstanceSummary, CircuitBreakerOverviewNew } from '@/api/circuitBreaker'

defineOptions({ name: 'InstancePanel' })

const props = defineProps<{
  instances: InstanceSummary[]
  selectedInstanceId: string | null
  overview: CircuitBreakerOverviewNew | null
}>()

const emit = defineEmits<{
  select: [instanceId: string | null]
}>()

const { t } = useI18n()

const selectInstance = (instanceId: string | null) => {
  emit('select', instanceId)
}
</script>

<style scoped lang="scss">
.instance-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);

  .panel-header {
    padding: 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);

    .title {
      font-size: 14px;
      font-weight: 500;
    }
  }

  .instance-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;

    .el-divider {
      margin: 8px 0;
    }
  }

  .instance-item {
    padding: 12px;
    margin-bottom: 8px;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;
    border: 1px solid transparent;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &.active {
      background: var(--el-color-primary-light-9);
      border-color: var(--el-color-primary-light-5);
    }

    &.aggregate {
      display: flex;
      align-items: center;
      gap: 8px;

      .name {
        flex: 1;
        font-weight: 500;
      }
    }

    .instance-info {
      .instance-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 4px;

        .name {
          font-size: 13px;
          font-weight: 500;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .instance-meta {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .circuit-breaker-summary {
      display: flex;
      gap: 8px;
      margin-top: 8px;
      flex-wrap: wrap;

      .state-badge {
        font-size: 11px;
        padding: 2px 6px;
        border-radius: 4px;

        &.success {
          background: var(--el-color-success-light-9);
          color: var(--el-color-success);
        }

        &.danger {
          background: var(--el-color-danger-light-9);
          color: var(--el-color-danger);
        }

        &.warning {
          background: var(--el-color-warning-light-9);
          color: var(--el-color-warning);
        }
      }
    }
  }
}
</style>
