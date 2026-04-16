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

    <!-- 核心监控指标三联排 -->
    <el-row :gutter="20" class="core-metrics-row">
      <!-- 实时 QPS 仪表盘 -->
      <el-col :xs="24" :sm="24" :md="8">
        <el-card class="metric-card qps-gauge-card">
          <template #header>
            <div class="metric-header">
              <div class="header-left">
                <el-icon><Odometer /></el-icon>
                <span>{{ t('dashboard.currentQps') }}</span>
              </div>
              <el-tag :type="qpsStatusType" size="small">{{ qpsStatusLabel }}</el-tag>
            </div>
          </template>
          <div class="metric-body">
            <v-chart :option="qpsGaugeOption" autoresize style="height: 130px" />
            <div class="mini-trend">
              <span class="trend-label">{{ t('dashboard.recent5MinTrend') }}</span>
              <v-chart :option="qpsTrendOption" autoresize style="height: 25px" />
            </div>
          </div>
          <div class="metric-footer">
            <div class="footer-item">
              <span class="footer-label">{{ t('dashboard.peakQps') }}</span>
              <span class="footer-value">{{ trafficHistory.reduce((max, p) => Math.max(max, p.peakQps || 0), 0) }}</span>
            </div>
            <div class="footer-item">
              <span class="footer-label">{{ t('dashboard.qpsThreshold') }}</span>
              <span class="footer-value">{{ QPS_THRESHOLD }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 响应时间分布直方图 -->
      <el-col :xs="24" :sm="24" :md="8">
        <el-card class="metric-card response-time-bar-card">
          <template #header>
            <div class="metric-header">
              <div class="header-left">
                <el-icon><Timer /></el-icon>
                <span>{{ t('dashboard.responseTimeDistribution') }}</span>
              </div>
              <el-tag :type="slaStatusType" size="small">{{ slaStatusLabel }}</el-tag>
            </div>
          </template>
          <div class="metric-body">
            <v-chart :option="responseTimeBarOption" autoresize style="height: 130px" />
            <div class="sla-progress">
              <div class="progress-header">
                <span>P99: {{ statistics.p99ResponseTime || 0 }}{{ t('dashboard.slaUnit') }}</span>
                <span>SLA: {{ SLA_TARGET_MS }}{{ t('dashboard.slaUnit') }}</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: p99SlaPercentage + '%', backgroundColor: slaStatusType === 'success' ? '#10b981' : '#ef4444' }"></div>
              </div>
              <div class="progress-label">{{ p99SlaPercentage }}% {{ t('dashboard.slaTarget') }}</div>
            </div>
          </div>
          <div class="metric-footer">
            <div class="footer-item highlight">
              <span class="footer-label">Max</span>
              <span class="footer-value">{{ statistics.maxResponseTime || 0 }}ms</span>
            </div>
            <div class="footer-item">
              <span class="footer-label">P50</span>
              <span class="footer-value">{{ statistics.p50ResponseTime || 0 }}ms</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 错误分类饼图 -->
      <el-col :xs="24" :sm="24" :md="8">
        <el-card class="metric-card error-pie-card">
          <template #header>
            <div class="metric-header">
              <div class="header-left">
                <el-icon><Warning /></el-icon>
                <span>{{ t('dashboard.errorClassification') }}</span>
              </div>
              <el-tag :type="errorStatusType" size="small">{{ statistics.errorRate || 0 }}%</el-tag>
            </div>
          </template>
          <div class="metric-body">
            <v-chart :option="errorPieOption" autoresize style="height: 130px" />
            <div class="error-stats">
              <div class="error-stat-item client">
                <span class="stat-dot"></span>
                <span class="stat-label">4xx {{ t('dashboard.clientError') }}</span>
                <span class="stat-value">{{ formatNumber(statistics.error4xxCount || 0) }}</span>
              </div>
              <div class="error-stat-item server">
                <span class="stat-dot"></span>
                <span class="stat-label">5xx {{ t('dashboard.serverError') }}</span>
                <span class="stat-value">{{ formatNumber(statistics.error5xxCount || 0) }}</span>
              </div>
            </div>
          </div>
          <div class="metric-footer">
            <div class="footer-item">
              <span class="footer-label">{{ t('dashboard.errorThreshold') }}</span>
              <span class="footer-value">{{ ERROR_RATE_THRESHOLD }}%</span>
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
        <el-table-column prop="instanceId" :label="t('common.instanceId')" />
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
import { LineChart, PieChart, GaugeChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, MarkLineComponent } from 'echarts/components'
import { useDashboardStore } from '@/stores/dashboard'
import { useNotificationStore } from '@/stores/notification'
import { refreshRoutes } from '@/api/route'
import { syncConfig } from '@/api/config'

// 注册 ECharts 组件
use([CanvasRenderer, LineChart, PieChart, GaugeChart, BarChart, GridComponent, TooltipComponent, LegendComponent, MarkLineComponent])

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
      // 构建 tooltip 内容
      let content = `${data.name}<br/>请求: ${data.value}`
      content += `<br/>成功: ${point.successCount || 0}<br/>失败: ${point.failedCount || 0}`
      // 响应时间分布
      if (point.p95ResponseTime || point.avgResponseTime) {
        content += `<br/>P95: ${point.p95ResponseTime || point.avgResponseTime || 0}ms`
      }
      if (point.p99ResponseTime) {
        content += `<br/>P99: ${point.p99ResponseTime}ms`
      }
      // 错误率
      if (point.errorRate) {
        content += `<br/>错误率: ${point.errorRate.toFixed(2)}%`
      }
      // QPS
      content += `<br/>峰值QPS: ${point.peakQps || 0}`
      if (point.currentQps) {
        content += `<br/>实时QPS: ${point.currentQps}`
      }
      return content
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

// ==================== 核心监控指标图表配置 ====================

/**
 * QPS 告警阈值（默认 5000 req/s）
 */
const QPS_THRESHOLD = 5000

/**
 * SLA 响应时间目标（默认 200ms）
 */
const SLA_TARGET_MS = 200

/**
 * 错误率阈值（默认 5%）
 */
const ERROR_RATE_THRESHOLD = 5

/**
 * 计算 QPS 状态类型（用于 el-tag 显示）
 */
const qpsStatusType = computed(() => {
  const qps = statistics.value.currentQps || 0
  const percentage = (qps / QPS_THRESHOLD) * 100
  if (percentage > 100) return 'danger'
  if (percentage > 80) return 'warning'
  return 'success'
})

/**
 * 计算 QPS 状态标签
 */
const qpsStatusLabel = computed(() => {
  const qps = statistics.value.currentQps || 0
  const percentage = (qps / QPS_THRESHOLD) * 100
  if (percentage > 100) return t('dashboard.qpsCritical')
  if (percentage > 80) return t('dashboard.qpsWarning')
  return t('dashboard.qpsNormal')
})

/**
 * 计算 SLA 达标状态类型
 */
const slaStatusType = computed(() => {
  const p99 = statistics.value.p99ResponseTime || 0
  return p99 <= SLA_TARGET_MS ? 'success' : 'danger'
})

/**
 * 计算 SLA 状态标签
 */
const slaStatusLabel = computed(() => {
  const p99 = statistics.value.p99ResponseTime || 0
  return p99 <= SLA_TARGET_MS ? t('dashboard.slaCompliant') : t('dashboard.slaViolation')
})

/**
 * 计算错误率状态类型
 */
const errorStatusType = computed(() => {
  const rate = statistics.value.errorRate || 0
  if (rate > ERROR_RATE_THRESHOLD) return 'danger'
  if (rate > 2) return 'warning'
  return 'success'
})

/**
 * 计算 P99 占 SLA 比例
 */
const p99SlaPercentage = computed(() => {
  const p99 = statistics.value.p99ResponseTime || 0
  return Math.min(Math.round((p99 / SLA_TARGET_MS) * 100), 100)
})

/**
 * QPS 仪表盘配置 - 半圆仪表盘
 */
const qpsGaugeOption = computed(() => {
  const currentQps = statistics.value.currentQps || 0
  const percentage = Math.min((currentQps / QPS_THRESHOLD) * 100, 100)

  // 根据百分比决定颜色
  let color = '#10b981' // 绿色 - 正常
  if (percentage > 80) color = '#f59e0b' // 黄色 - 接近阈值
  if (percentage > 100) color = '#ef4444' // 红色 - 超限

  return {
    series: [
      {
        type: 'gauge',
        startAngle: 180,
        endAngle: 0,
        min: 0,
        max: QPS_THRESHOLD,
        splitNumber: 5,
        radius: '100%',
        center: ['50%', '70%'],
        axisLine: {
          lineStyle: {
            width: 15,
            color: [
              [0.8, '#10b981'],
              [1, '#f59e0b'],
            ],
          },
        },
        pointer: {
          length: '60%',
          width: 6,
          itemStyle: {
            color: color,
          },
        },
        axisTick: {
          show: false,
        },
        splitLine: {
          length: 15,
          lineStyle: {
            width: 2,
            color: 'var(--text-color-secondary)',
          },
        },
        axisLabel: {
          distance: 20,
          color: 'var(--text-color-secondary)',
          fontSize: 10,
          formatter: (value: number) => {
            if (value === 0) return '0'
            if (value === QPS_THRESHOLD) return value.toString()
            return ''
          },
        },
        detail: {
          valueAnimation: true,
          formatter: `{value} ${t('dashboard.reqPerSecond')}`,
          color: 'var(--text-color-primary)',
          fontSize: 14,
          offsetCenter: [0, '20%'],
        },
        data: [
          {
            value: currentQps,
            itemStyle: {
              color: color,
            },
          },
        ],
      },
    ],
  }
})

/**
 * QPS 迷你趋势线配置（最近 5 分钟）
 */
const qpsTrendOption = computed(() => {
  // 从 trafficHistory 取最近 5 个数据点的 count 值作为 QPS 趋势参考
  const recentPoints = trafficHistory.value.slice(-5)
  const maxCount = Math.max(...recentPoints.map((p) => p.count || 0), 1)

  return {
    grid: {
      left: 0,
      right: 0,
      top: 5,
      bottom: 5,
    },
    xAxis: {
      type: 'category',
      show: false,
      data: recentPoints.map((p) => p.time),
    },
    yAxis: {
      type: 'value',
      show: false,
      min: 0,
      max: maxCount,
    },
    series: [
      {
        type: 'line',
        data: recentPoints.map((p) => p.count || 0),
        smooth: true,
        symbol: 'none',
        lineStyle: {
          width: 2,
          color: '#3b82f6',
        },
        areaStyle: {
          opacity: 0.3,
          color: '#3b82f6',
        },
      },
    ],
  }
})

/**
 * 响应时间分布直方图配置
 */
const responseTimeBarOption = computed(() => {
  const p50 = statistics.value.p50ResponseTime || 0
  const p95 = statistics.value.p95ResponseTime || 0
  const p99 = statistics.value.p99ResponseTime || 0
  const maxVal = statistics.value.maxResponseTime || 0

  // 估算 P75 和 P90（如果没有实际数据，用线性插值）
  const p75 = Math.round(p50 + (p95 - p50) * 0.5)
  const p90 = Math.round(p50 + (p99 - p50) * 0.8)

  // Y 轴最大值
  const yAxisMax = Math.max(SLA_TARGET_MS * 1.5, p99 * 1.2, maxVal * 1.1)

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'var(--border-color-base)',
      borderWidth: 1,
      formatter: (params: any) => {
        if (!params || params.length === 0) return ''
        const data = params[0]
        return `${data.name}: ${data.value}ms`
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: ['P50', 'P75', 'P90', 'P95', 'P99'],
      axisLine: {
        lineStyle: {
          color: 'var(--neutral-300)',
        },
      },
      axisLabel: {
        color: 'var(--text-color-secondary)',
        fontSize: 11,
      },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: yAxisMax,
      axisLine: {
        show: false,
      },
      axisLabel: {
        color: 'var(--text-color-secondary)',
        fontSize: 10,
        formatter: '{value}ms',
      },
      splitLine: {
        lineStyle: {
          color: 'var(--neutral-200)',
        },
      },
    },
    series: [
      {
        type: 'bar',
        data: [
          { value: p50, itemStyle: { color: '#3b82f6' } },
          { value: p75, itemStyle: { color: '#3b82f6' } },
          { value: p90, itemStyle: { color: '#3b82f6' } },
          { value: p95, itemStyle: { color: '#60a5fa' } },
          { value: p99, itemStyle: { color: '#f59e0b', borderRadius: [4, 4, 0, 0] } },
        ],
        barWidth: 20,
        markLine: {
          silent: true,
          symbol: 'none',
          data: [
            {
              yAxis: SLA_TARGET_MS,
              label: {
                formatter: `SLA ${SLA_TARGET_MS}ms`,
                position: 'end',
                color: '#ef4444',
                fontSize: 10,
              },
              lineStyle: {
                color: '#ef4444',
                type: 'dashed',
                width: 2,
              },
            },
          ],
        },
      },
    ],
  }
})

/**
 * 错误分类饼图配置
 */
const errorPieOption = computed(() => {
  const error4xx = statistics.value.error4xxCount || 0
  const error5xx = statistics.value.error5xxCount || 0
  const totalErrors = error4xx + error5xx

  // 如果没有错误数据，显示空状态
  if (totalErrors === 0) {
    return {
      series: [
        {
          type: 'pie',
          radius: ['40%', '65%'],
          center: ['50%', '50%'],
          data: [
            { value: 1, name: '无错误', itemStyle: { color: '#10b981' } },
          ],
          label: {
            show: true,
            position: 'center',
            formatter: '无错误',
            color: 'var(--text-color-secondary)',
            fontSize: 14,
          },
          emphasis: {
            disabled: true,
          },
        },
      ],
    }
  }

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'var(--border-color-base)',
      borderWidth: 1,
      formatter: (params: any) => {
        const percent = Math.round((params.value / totalErrors) * 100)
        return `${params.name}: ${formatNumber(params.value)} (${percent}%)`
      },
    },
    legend: {
      show: false,
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: 'var(--card-bg)',
          borderWidth: 2,
        },
        label: {
          show: true,
          position: 'center',
          formatter: () => {
            return `{total|${formatNumber(totalErrors)}}\n{label|${t('dashboard.errorClassification')}}`
          },
          rich: {
            total: {
              fontSize: 18,
              fontWeight: '600',
              color: 'var(--text-color-primary)',
            },
            label: {
              fontSize: 11,
              color: 'var(--text-color-secondary)',
              padding: [4, 0, 0, 0],
            },
          },
        },
        emphasis: {
          label: {
            show: true,
          },
        },
        labelLine: {
          show: false,
        },
        data: [
          {
            value: error4xx,
            name: '4xx',
            itemStyle: { color: '#f59e0b' },
          },
          {
            value: error5xx,
            name: '5xx',
            itemStyle: { color: '#ef4444' },
          },
        ],
      },
    ],
  }
})

/**
 * 错误趋势迷你图配置（最近 5 分钟）
 */
const errorTrendOption = computed(() => {
  // 从 trafficHistory 取最近 5 个数据点的 failedCount 值
  const recentPoints = trafficHistory.value.slice(-5)
  const maxFailed = Math.max(...recentPoints.map((p) => p.failedCount || 0), 1)

  return {
    grid: {
      left: 0,
      right: 0,
      top: 5,
      bottom: 5,
    },
    xAxis: {
      type: 'category',
      show: false,
      data: recentPoints.map((p) => p.time),
    },
    yAxis: {
      type: 'value',
      show: false,
      min: 0,
      max: maxFailed,
    },
    series: [
      {
        type: 'line',
        data: recentPoints.map((p) => p.failedCount || 0),
        smooth: true,
        symbol: 'none',
        lineStyle: {
          width: 2,
          color: '#ef4444',
        },
        areaStyle: {
          opacity: 0.3,
          color: '#ef4444',
        },
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

  // ==================== 核心监控指标卡片样式 ====================
  .core-metrics-row {
    margin-bottom: 24px;
  }

  .metric-card {
    margin-bottom: 20px;
    border: 1px solid var(--border-color-base);
    height: 320px;
    display: flex;
    flex-direction: column;
    transition:
      box-shadow 0.2s ease,
      transform 0.2s ease;

    &:hover {
      box-shadow: var(--shadow-medium);
      transform: translateY(-2px);
    }

    :deep(.el-card__header) {
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color-base);
      flex-shrink: 0;
    }

    :deep(.el-card__body) {
      padding: 12px 16px;
      flex: 1;
      display: flex;
      flex-direction: column;
      min-height: 0;
    }

    .metric-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 500;
        color: var(--text-color-primary);
        font-size: 14px;
      }
    }

    .metric-body {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-height: 0;
    }

    .mini-trend {
      flex-shrink: 0;
      .trend-label {
        font-size: 11px;
        color: var(--text-color-secondary);
        margin-bottom: 2px;
        display: block;
      }
    }

    .metric-footer {
      flex-shrink: 0;
      display: flex;
      justify-content: space-between;
      padding-top: 8px;
      border-top: 1px solid var(--border-color-light);
      margin-top: 8px;

      .footer-item {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .footer-label {
          font-size: 11px;
          color: var(--text-color-secondary);
        }

        .footer-value {
          font-size: 13px;
          font-weight: 500;
          color: var(--text-color-primary);
        }

        &.highlight .footer-value {
          color: #f59e0b;
        }
      }
    }

    // QPS 仪表盘卡片特有样式
    &.qps-gauge-card {
      .metric-body {
        padding: 0;
      }
    }

    // 响应时间直方图卡片特有样式
    &.response-time-bar-card {
      .sla-progress {
        flex-shrink: 0;
        margin-top: 8px;

        .progress-header {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: var(--text-color-secondary);
        }

        .progress-bar {
          height: 6px;
          background: var(--neutral-200);
          border-radius: 3px;
          margin-top: 4px;
          overflow: hidden;

          .progress-fill {
            height: 100%;
            border-radius: 3px;
            transition: width 0.3s ease;
          }
        }

        .progress-label {
          font-size: 11px;
          color: var(--text-color-placeholder);
          margin-top: 2px;
          text-align: right;
        }
      }
    }

    // 错误饼图卡片特有样式
    &.error-pie-card {
      .error-stats {
        flex-shrink: 0;
        display: flex;
        gap: 16px;
        margin-top: 8px;

        .error-stat-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;

          .stat-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
          }

          .stat-label {
            color: var(--text-color-secondary);
          }

          .stat-value {
            font-weight: 500;
            color: var(--text-color-primary);
          }

          &.client .stat-dot {
            background: #f59e0b;
          }

          &.server .stat-dot {
            background: #ef4444;
          }
        }
      }
    }
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
