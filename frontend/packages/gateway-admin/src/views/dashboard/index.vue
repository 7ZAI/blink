<template>
  <div class="dashboard-page">
    <h2 class="page-title">{{ t('dashboard.gatewayOverview') }}</h2>

    <!-- Statistics Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409eff">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(statistics.totalRequests) }}</div>
              <div class="stat-label">{{ t('dashboard.totalRequests') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67c23a">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ successRate }}</div>
              <div class="stat-label">{{ t('dashboard.successRate') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #e6a23c">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ statistics.avgResponseTime || 0 }}
                <span class="stat-unit">ms</span>
              </div>
              <div class="stat-label">{{ t('dashboard.avgResponseTime') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #f56c6c">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ healthyRatio }}</div>
              <div class="stat-label">{{ t('dashboard.healthyInstances') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Section -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :md="16">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>{{ t('dashboard.trafficTrend') }}</span>
              <div class="chart-controls">
                <el-date-picker
                  v-model="timeRangeValue"
                  type="datetimerange"
                  :shortcuts="timeShortcuts"
                  :placeholder="t('dashboard.selectTimeRange')"
                  value-format="x"
                  :clearable="true"
                  @change="handleTimeRangeChange"
                />
                <el-radio-group v-model="granularityValue" size="small" @change="handleGranularityChange">
                  <el-radio-button value="MINUTE">{{ t('dashboard.minute') }}</el-radio-button>
                  <el-radio-button value="HOUR">{{ t('dashboard.hour') }}</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <v-chart :option="lineChartOption" autoresize style="height: 300px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="chart-card">
          <template #header>
            <span>{{ t('dashboard.statusDistribution') }}</span>
          </template>
          <v-chart :option="pieChartOption" autoresize style="height: 300px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Actions -->
    <el-card class="page-card quick-actions-card">
      <template #header>
        <span>{{ t('dashboard.quickActions') }}</span>
      </template>
      <div class="quick-actions">
        <el-button
          type="primary"
          :loading="actionsLoading.refreshRoutes"
          @click="handleRefreshRoutes"
        >
          <el-icon><Refresh /></el-icon>
          {{ t('dashboard.refreshRoutes') }}
        </el-button>
        <el-button type="success" :loading="actionsLoading.syncConfig" @click="handleSyncConfig">
          <el-icon><Connection /></el-icon>
          {{ t('dashboard.syncConfig') }}
        </el-button>
        <el-button :loading="actionsLoading.clearCache" @click="handleClearCache">
          <el-icon><Delete /></el-icon>
          {{ t('dashboard.clearCache') }}
        </el-button>
      </div>
    </el-card>

    <!-- Recent Activity Log -->
    <el-card class="page-card activity-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('dashboard.recentActivity') }}</span>
        </div>
      </template>
      <div class="activity-list">
        <div class="activity-empty">
          <el-icon :size="48"><Document /></el-icon>
          <p>{{ t('dashboard.noActivity') }}</p>
          <span class="activity-note">{{ t('dashboard.activityNote') }}</span>
        </div>
      </div>
    </el-card>

    <!-- Instance List -->
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('dashboard.instanceList') }}</span>
          <el-button type="primary" @click="handleManualRefresh">
            <el-icon><Refresh /></el-icon>
            {{ t('common.refresh') }}
          </el-button>
        </div>
      </template>
      <el-table :data="instances" v-loading="instancesLoading" stripe>
        <el-table-column prop="instanceId" :label="t('monitor.instanceId')" />
        <el-table-column prop="host" :label="t('monitor.ip')" />
        <el-table-column prop="port" :label="t('monitor.port')" />
        <el-table-column :label="t('monitor.healthStatus')">
          <template #default="{ row }">
            <el-tag :type="row.healthy ? 'success' : 'danger'">
              {{ row.healthy ? t('monitor.up') : t('monitor.down') }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { useDashboardStore } from '@/stores/dashboard'
import { useNotificationStore } from '@/stores/notification'
import { refreshRoutes } from '@/api/route'
import { syncConfig } from '@/api/config'

// 注册 ECharts 组件
use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const { t } = useI18n()
const router = useRouter()

// 使用 dashboard store
const dashboardStore = useDashboardStore()
const notificationStore = useNotificationStore()

// 从 store 获取数据（computed 响应式）
const statistics = computed(() => dashboardStore.statistics)
const instances = computed(() => dashboardStore.instances)
const instancesLoading = computed(() => dashboardStore.instancesLoading)
const trafficHistory = computed(() => dashboardStore.trafficHistory)
const trafficHistoryLoading = computed(() => dashboardStore.trafficHistoryLoading)
const trafficGranularity = computed(() => dashboardStore.trafficGranularity)

// ==================== 流量趋势控制状态 ====================

/**
 * 时间范围选择器绑定值
 */
const timeRangeValue = ref<[number, number] | null>(null)

/**
 * 粒度选择器绑定值
 */
const granularityValue = ref<'MINUTE' | 'HOUR'>('MINUTE')

/**
 * 时间范围快捷选项
 */
const timeShortcuts = computed(() => [
  {
    text: t('dashboard.last1Hour'),
    value: () => {
      const end = Date.now()
      const start = end - 3600000
      return [start, end]
    },
  },
  {
    text: t('dashboard.last6Hours'),
    value: () => {
      const end = Date.now()
      const start = end - 21600000
      return [start, end]
    },
  },
  {
    text: t('dashboard.last24Hours'),
    value: () => {
      const end = Date.now()
      const start = end - 86400000
      return [start, end]
    },
  },
])

/**
 * 处理时间范围变化
 */
const handleTimeRangeChange = (val: [number, number] | null) => {
  if (val) {
    dashboardStore.setTimeRange({ startTime: val[0], endTime: val[1] })
  } else {
    // 清空时间范围，恢复实时模式
    dashboardStore.setTimeRange({})
    dashboardStore.loadTrafficHistory({ granularity: granularityValue.value })
  }
}

/**
 * 处理粒度变化
 */
const handleGranularityChange = (val: 'MINUTE' | 'HOUR') => {
  dashboardStore.setGranularity(val)
}

// ==================== 快捷操作状态 ====================

// 快捷操作加载状态
const actionsLoading = reactive({
  refreshRoutes: false,
  syncConfig: false,
  clearCache: false,
})

/**
 * 计算成功率（使用 store 提供的计算）
 */
const successRate = computed(() => dashboardStore.successRateDisplay)

/**
 * 计算健康实例比例（使用 store 提供的计算）
 */
const healthyRatio = computed(() => dashboardStore.healthyRatioDisplay)

/**
 * 折线图配置 - 简约风格
 */
const lineChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'line',
    },
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: 'var(--border-color-base)',
    borderWidth: 1,
    formatter: (params: any) => {
      if (!params || params.length === 0) return ''
      const data = params[0]
      const point = trafficHistory.value[data.dataIndex]
      if (!point) return `${data.name}<br/>请求: ${data.value}`
      return `${data.name}<br/>请求: ${data.value}<br/>成功: ${point.successCount}<br/>失败: ${point.failedCount}<br/>峰值QPS: ${point.peakQps}`
    },
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trafficHistory.value.map((d) => d.time),
    axisLine: {
      lineStyle: {
        color: 'var(--neutral-300)',
      },
    },
    axisLabel: {
      color: 'var(--text-color-secondary)',
    },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLine: {
      show: false,
    },
    axisLabel: {
      color: 'var(--text-color-secondary)',
    },
    splitLine: {
      lineStyle: {
        color: 'var(--neutral-200)',
      },
    },
  },
  series: [
    {
      data: trafficHistory.value.map((d) => d.count),
      type: 'line',
      smooth: true,
      areaStyle: {
        opacity: 0.15,
        color: '#3b82f6',
      },
      lineStyle: {
        color: '#3b82f6',
        width: 2,
      },
      itemStyle: {
        color: '#3b82f6',
      },
      symbol: 'circle',
      symbolSize: 6,
    },
  ],
}))

/**
 * 饼图配置 - 简约风格
 */
const pieChartOption = computed(() => {
  const unhealthyCount = statistics.value.totalInstances - statistics.value.healthyInstances
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'var(--border-color-base)',
      borderWidth: 1,
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemGap: 20,
      textStyle: {
        color: 'var(--text-color-secondary)',
      },
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: 'var(--card-bg)',
          borderWidth: 2,
        },
        label: {
          show: true,
          position: 'center',
          formatter: () => {
            const total = statistics.value.totalInstances || 0
            return `{total|${total}}\n{label|${t('dashboard.instanceCount')}}`
          },
          rich: {
            total: {
              fontSize: 22,
              fontWeight: '600',
              color: 'var(--text-color-primary)',
            },
            label: {
              fontSize: 12,
              color: 'var(--text-color-secondary)',
              padding: [5, 0, 0, 0],
            },
          },
        },
        emphasis: {
          label: {
            show: true,
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.15)',
          },
        },
        labelLine: {
          show: false,
        },
        data: [
          {
            value: statistics.value.healthyInstances,
            name: t('monitor.healthy'),
            itemStyle: { color: '#10b981' },
          },
          {
            value: unhealthyCount,
            name: t('monitor.unhealthy'),
            itemStyle: { color: '#ef4444' },
          },
        ],
      },
    ],
  }
})

/**
 * 格式化数字（使用 store 提供的方法）
 */
const formatNumber = (num: number): string => {
  return dashboardStore.formatNumber(num)
}

/**
 * 手动刷新实例列表
 * 实例数据通过 API 查询获取，点击刷新按钮主动请求
 */
const handleManualRefresh = async () => {
  await dashboardStore.fetchInstances()
  ElMessage.success(t('common.refreshSuccess'))
}

/**
 * 快捷操作：刷新路由
 */
const handleRefreshRoutes = async () => {
  actionsLoading.refreshRoutes = true
  try {
    await refreshRoutes()
    ElMessage.success(t('common.success'))
  } catch (error) {
    ElMessage.error(t('common.failed'))
  } finally {
    actionsLoading.refreshRoutes = false
  }
}

/**
 * 同步配置
 */
const handleSyncConfig = async () => {
  actionsLoading.syncConfig = true
  try {
    await syncConfig()
    ElMessage.success(t('common.success'))
  } catch (error) {
    ElMessage.error(t('common.failed'))
  } finally {
    actionsLoading.syncConfig = false
  }
}

/**
 * 清除缓存
 */
const handleClearCache = async () => {
  actionsLoading.clearCache = true
  try {
    // No API yet, just show success
    await new Promise((resolve) => setTimeout(resolve, 500))
    ElMessage.success(t('common.success'))
  } catch (error) {
    ElMessage.error(t('common.failed'))
  } finally {
    actionsLoading.clearCache = false
  }
}

onMounted(() => {
  // 组件挂载时获取实例列表（通过 API 查询）
  dashboardStore.fetchInstances()
  // 初始化加载流量历史数据（默认最近 1 小时分钟级数据）
  dashboardStore.loadTrafficHistory({ granularity: 'MINUTE' })
  // 统计信息由 SSE 推送，无需手动初始化
})

onUnmounted(() => {
  // SSE 连接由 notification store 统一管理，dashboard 组件不需要断开
})
</script>

<style scoped lang="scss">
.dashboard-page {
  padding: 24px;

  .page-title {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 24px;
    color: var(--text-color-primary);
  }

  .stats-row {
    margin-bottom: 24px;
  }

  .stat-card {
    margin-bottom: 20px;
    border: 1px solid var(--border-color-base);
    transition:
      box-shadow 0.2s ease,
      transform 0.2s ease;

    &:hover {
      box-shadow: var(--shadow-medium);
      transform: translateY(-2px);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 22px;
    }

    .stat-info {
      .stat-value {
        font-size: 22px;
        font-weight: 600;
        color: var(--text-color-primary);

        .stat-unit {
          font-size: 13px;
          font-weight: normal;
          color: var(--text-color-secondary);
          margin-left: 2px;
        }
      }
      .stat-label {
        font-size: 13px;
        color: var(--text-color-secondary);
        margin-top: 4px;
      }
    }
  }

  .charts-row {
    margin-bottom: 24px;
  }

  .chart-card {
    margin-bottom: 20px;
    border: 1px solid var(--border-color-base);

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid var(--border-color-base);
      font-weight: 500;
    }

    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;

      .chart-controls {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }
    }
  }

  .page-card {
    margin-bottom: 20px;
  }

  .quick-actions-card {
    .quick-actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }
  }

  .activity-card {
    .activity-list {
      min-height: 200px;
    }

    .activity-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px 20px;
      color: var(--text-color-secondary);

      .el-icon {
        color: var(--neutral-400);
        margin-bottom: 16px;
      }

      p {
        margin: 0 0 8px;
        font-size: 14px;
      }
    }

    .activity-note {
      font-size: 12px;
      color: var(--text-color-placeholder);
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
