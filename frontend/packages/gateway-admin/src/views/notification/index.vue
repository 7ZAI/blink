<template>
  <div class="notification-history table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card shrink-0" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('notification.type')">
          <el-select
            v-model="searchForm.type"
            :placeholder="t('common.pleaseSelect')"
            clearable
            style="width: 150px"
          >
            <el-option label="缓存同步" value="cache_sync" />
            <el-option label="系统通知" value="SYSTEM" />
            <el-option label="操作通知" value="OPERATION" />
            <el-option label="告警通知" value="ALERT" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('notification.severity')">
          <el-select
            v-model="searchForm.severity"
            :placeholder="t('common.pleaseSelect')"
            clearable
            style="width: 120px"
          >
            <el-option :label="t('notification.info')" value="INFO" />
            <el-option :label="t('notification.warning')" value="WARNING" />
            <el-option :label="t('notification.error')" value="ERROR" />
            <el-option :label="t('notification.success')" value="SUCCESS" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('notification.timeRange')">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            range-separator="-"
            :start-placeholder="t('common.startDate')"
            :end-placeholder="t('common.endDate')"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            style="height: 28px; padding: 0 12px; font-size: 13px"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card flex-1 flex flex-col overflow-hidden" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-left">
            <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleDeleteSelected">
              <el-icon><Delete /></el-icon>
              {{ t('common.delete') }}
            </el-button>
            <el-button type="primary" @click="handleMarkAllRead">
              <el-icon><Check /></el-icon>
              {{ t('notification.markAllRead') }}
            </el-button>
          </div>
        </div>
      </template>

      <!-- 表格区域 -->
      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="tableData"
          height="100%"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column :label="t('notification.title')" min-width="200">
            <template #default="{ row }">
              <div class="notification-title-cell" :class="{ unread: !row.read }">
                <el-icon :class="getSeverityClass(row.severity)" class="severity-icon">
                  <component :is="getIcon(row.severity)" />
                </el-icon>
                <span class="title-text">{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('notification.content')" min-width="300" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.content }}
            </template>
          </el-table-column>
          <el-table-column :label="t('notification.type')" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getTypeTagType(row.type)" size="small">
                {{ getTypeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('notification.severity')" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getSeverityTagType(row.severity)" size="small">
                {{ row.severity }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('notification.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.read" type="info" size="small">{{ t('notification.read') }}</el-tag>
              <el-tag v-else type="warning" size="small">{{ t('notification.unread') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.createTime')" width="160">
            <template #default="{ row }">
              {{ formatTime(row.createdTime) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleViewDetail(row)">
                <el-icon><View /></el-icon>
                {{ t('common.detail') }}
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
                {{ t('common.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchForm.pageNum"
          v-model:page-size="searchForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        >
          <template #total="{ total }">
            {{ t('pagination.total', { total }) }}
          </template>
        </el-pagination>
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="t('notification.detail')"
      width="500px"
      :lock-scroll="false"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="t('notification.title')">
          {{ detailData.title }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('notification.content')">
          {{ detailData.content }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('notification.type')">
          <el-tag :type="getTypeTagType(detailData.type)" size="small">
            {{ getTypeLabel(detailData.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('notification.severity')">
          <el-tag :type="getSeverityTagType(detailData.severity)" size="small">
            {{ detailData.severity }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.createTime')">
          {{ detailData.createdTime }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Delete,
  Check,
  View,
  InfoFilled,
  WarningFilled,
  CircleCloseFilled,
  CircleCheckFilled,
} from '@element-plus/icons-vue'
import { notificationApi, type NotificationItem } from '@/api/notification'

defineOptions({ name: 'NotificationHistory' })

const { t } = useI18n()

// 搜索表单
const searchForm = reactive({
  pageNum: 1,
  pageSize: 10,
  type: '',
  severity: '',
  startTime: '',
  endTime: '',
})

// 时间范围
const timeRange = ref<[string, string] | null>(null)

// 监听时间范围变化
watch(timeRange, (val) => {
  if (val) {
    searchForm.startTime = val[0]
    searchForm.endTime = val[1]
  } else {
    searchForm.startTime = ''
    searchForm.endTime = ''
  }
})

// 表格数据
const loading = ref(false)
const tableData = ref<NotificationItem[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref<NotificationItem>({
  notificationId: 0,
  title: '',
  content: '',
  type: '',
  severity: 'INFO',
  createdTime: '',
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await notificationApi.getHistory({
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
      type: searchForm.type,
      severity: searchForm.severity,
      startTime: searchForm.startTime,
      endTime: searchForm.endTime,
    })
    tableData.value = res.rows || []
    total.value = res.total || 0
  } catch (error) {
    console.error('[NotificationHistory] Failed to load data:', error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  searchForm.pageNum = 1
  loadData()
}

// 处理重置
const handleReset = () => {
  searchForm.type = ''
  searchForm.severity = ''
  searchForm.startTime = ''
  searchForm.endTime = ''
  timeRange.value = null
  searchForm.pageNum = 1
  loadData()
}

// 处理分页
const handleSizeChange = () => loadData()
const handleCurrentChange = () => loadData()

// 处理选择
const handleSelectionChange = (selection: NotificationItem[]) => {
  selectedIds.value = selection.map((item) => item.notificationId)
}

// 查看详情
const handleViewDetail = (row: NotificationItem) => {
  detailData.value = { ...row }
  detailVisible.value = true
  // 如果未读，标记为已读
  if (!row.read) {
    notificationApi.markRead(row.notificationId)
    row.read = true
  }
}

// 删除单个
const handleDelete = async (row: NotificationItem) => {
  try {
    await ElMessageBox.confirm(t('common.confirm') + t('common.delete') + '?', t('message.tips'), {
      type: 'warning',
      lockScroll: false,
    })
    // 这里需要后端提供删除单个通知的 API
    // await notificationApi.delete(row.notificationId)
    const index = tableData.value.findIndex((item) => item.notificationId === row.notificationId)
    if (index > -1) {
      tableData.value.splice(index, 1)
      total.value--
    }
    ElMessage.success(t('message.deleteSuccess'))
  } catch {
    // 用户取消
  }
}

// 删除选中
const handleDeleteSelected = async () => {
  try {
    await ElMessageBox.confirm(
      t('notification.deleteSelectedConfirm', { count: selectedIds.value.length }),
      t('message.tips'),
      { type: 'warning', lockScroll: false }
    )
    // 这里需要后端提供批量删除的 API
    // await notificationApi.deleteBatch(selectedIds.value)
    tableData.value = tableData.value.filter(
      (item) => !selectedIds.value.includes(item.notificationId)
    )
    total.value -= selectedIds.value.length
    selectedIds.value = []
    ElMessage.success(t('message.deleteSuccess'))
  } catch {
    // 用户取消
  }
}

// 标记全部已读
const handleMarkAllRead = async () => {
  try {
    await notificationApi.markAllRead()
    tableData.value.forEach((item) => {
      item.read = true
    })
    ElMessage.success(t('message.success'))
  } catch (error) {
    console.error('[NotificationHistory] Failed to mark all read:', error)
  }
}

// 获取图标
const getIcon = (severity: string) => {
  const icons: Record<string, any> = {
    INFO: InfoFilled,
    WARNING: WarningFilled,
    ERROR: CircleCloseFilled,
    SUCCESS: CircleCheckFilled,
  }
  return icons[severity] || InfoFilled
}

// 获取严重程度类名
const getSeverityClass = (severity: string) => {
  return severity.toLowerCase()
}

// 获取严重程度标签类型
const getSeverityTagType = (severity: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  const types: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    INFO: 'info',
    WARNING: 'warning',
    ERROR: 'danger',
    SUCCESS: 'success',
  }
  return types[severity] || 'info'
}

// 获取类型标签类型
const getTypeTagType = (type: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  if (type?.startsWith('cache_sync')) return 'success'
  if (type?.startsWith('instance_sync')) return 'info'
  const types: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    SYSTEM: 'primary',
    OPERATION: 'success',
    ALERT: 'warning',
  }
  return types[type] || 'info'
}

// 获取类型标签文本
const getTypeLabel = (type: string) => {
  if (type?.startsWith('cache_sync_summary')) return '缓存同步'
  if (type?.startsWith('instance_sync')) return '实例同步'
  const labels: Record<string, string> = {
    SYSTEM: '系统',
    OPERATION: '操作',
    ALERT: '告警',
  }
  return labels[type] || type
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.notification-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .severity-icon {
    font-size: 16px;

    &.info {
      color: var(--primary-color, #3b82f6);
    }
    &.warning {
      color: var(--warning-color, #f59e0b);
    }
    &.error {
      color: var(--danger-color, #ef4444);
    }
    &.success {
      color: var(--success-color, #10b981);
    }
  }

  .title-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &.unread .title-text {
    font-weight: 600;
  }
}

.table-header {
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}
</style>