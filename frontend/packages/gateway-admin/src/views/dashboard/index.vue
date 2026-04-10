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
            <span>{{ t('dashboard.trafficTrend') }}</span>
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

    <!-- Instance Health Grid -->
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('dashboard.instanceHealth') }}</span>
          <el-button type="primary" link @click="router.push('/monitor')">
            {{ t('common.viewAll') }}
          </el-button>
        </div>
      </template>
      <div class="instance-grid">
        <div
          v-for="instance in instances"
          :key="instance.instanceId"
          class="instance-card"
          :class="{ unhealthy: !instance.healthy }"
          @click="router.push('/monitor')"
        >
          <div class="instance-status" :class="instance.healthy ? 'healthy' : 'unhealthy'"></div>
          <div class="instance-info">
            <div class="instance-id">{{ instance.instanceId }}</div>
            <div class="instance-address">{{ instance.host }}:{{ instance.port }}</div>
          </div>
          <div class="instance-weight">
            <el-tag size="small" :type="instance.healthy ? 'success' : 'danger'">
              {{ instance.healthy ? t('monitor.up') : t('monitor.down') }}
            </el-tag>
          </div>
        </div>
        <div v-if="instances.length === 0" class="no-instances">
          {{ t('common.noData') }}
        </div>
      </div>
    </el-card>

    <!-- Instance List -->
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('dashboard.instanceList') }}</span>
          <el-button type="primary" @click="loadData">
            <el-icon><Refresh /></el-icon>
            {{ t('common.refresh') }}
          </el-button>
        </div>
      </template>
      <el-table :data="instances" v-loading="loading" stripe>
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
import {
  getGatewayInstances,
  getStatistics,
  type InstanceInfo,
  type StatisticsInfo,
} from '@/api/monitor'
import { refreshRoutes } from '@/api/route'
import { syncConfig } from '@/api/config'

// 注册 ECharts 组件
use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)

// 快捷操作加载状态
const actionsLoading = reactive({
  refreshRoutes: false,
  syncConfig: false,
  clearCache: false,
})

const statistics = reactive<StatisticsInfo>({
  totalInstances: 0,
  healthyInstances: 0,
  totalRequests: 0,
  successRequests: 0,
  failedRequests: 0,
  avgResponseTime: 0,
})

const instances = ref<InstanceInfo[]>([])

// 流量历史数据（保留最近10条）
const trafficHistory = ref<{ time: string; count: number }[]>([])

let refreshInterval: number | null = null

/**
 * 计算成功率
 */
const successRate = computed(() => {
  if (!statistics.totalRequests || statistics.totalRequests === 0) {
    return '0%'
  }
  return Math.round((statistics.successRequests / statistics.totalRequests) * 100) + '%'
})

/**
 * 计算健康实例比例
 */
const healthyRatio = computed(() => {
  if (!statistics.totalInstances || statistics.totalInstances === 0) {
    return '0/0'
  }
  return `${statistics.healthyInstances}/${statistics.totalInstances}`
})

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
  const unhealthyCount = statistics.totalInstances - statistics.healthyInstances
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
            const total = statistics.totalInstances || 0
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
            value: statistics.healthyInstances,
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
 * 格式化数字（添加千位分隔符）
 */
const formatNumber = (num: number): string => {
  if (!num) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 加载数据
 */
const loadData = async () => {
  loading.value = true
  try {
    const [statsRes, instancesRes] = await Promise.all([getStatistics({}), getGatewayInstances({})])

    // 统计数据
    if (statsRes) {
      statistics.totalInstances = statsRes.totalInstances || 0
      statistics.healthyInstances = statsRes.healthyInstances || 0
      statistics.totalRequests = statsRes.totalRequests || 0
      statistics.successRequests = statsRes.successRequests || 0
      statistics.avgResponseTime = statsRes.avgResponseTime || 0

      // 记录流量历史
      trafficHistory.value.push({
        time: new Date().toLocaleTimeString(),
        count: statsRes.totalRequests || 0,
      })
      // 保留最近10条记录
      if (trafficHistory.value.length > 10) {
        trafficHistory.value.shift()
      }
    }

    // 实例列表
    instances.value = instancesRes?.instances || []
  } catch (error) {
    console.error('Load data error:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 刷新路由
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
  loadData()
  // 设置自动刷新，每5秒刷新一次
  refreshInterval = window.setInterval(loadData, 5000)
})

onUnmounted(() => {
  // 组件卸载时清除定时器
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
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

  .instance-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
  }

  .instance-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background-color: var(--bg-color-page);
    border-radius: var(--radius-md);
    border: 1px solid var(--border-color-light);
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-medium);
      border-color: var(--primary-color-light-9);
    }

    &.unhealthy {
      background-color: rgba(239, 68, 68, 0.04);
      border-color: rgba(239, 68, 68, 0.15);
    }
  }

  .instance-status {
    width: 10px;
    height: 10px;
    border-radius: 50%;

    &.healthy {
      background-color: var(--success-color);
    }

    &.unhealthy {
      background-color: var(--danger-color);
    }
  }

  .instance-info {
    flex: 1;
    min-width: 0;
  }

  .instance-id {
    font-weight: 500;
    color: var(--text-color-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .instance-address {
    font-size: 13px;
    color: var(--text-color-secondary);
    margin-top: 2px;
  }

  .no-instances {
    grid-column: 1 / -1;
    text-align: center;
    padding: 40px 0;
    color: var(--text-color-secondary);
  }
}
</style>
