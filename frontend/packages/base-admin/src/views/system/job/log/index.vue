<template>
  <div class="table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card shrink-0" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <!-- 操作按钮行 -->
        <div class="search-buttons mb-3">
          <el-button type="info" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
          <el-button @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>
        <!-- 查询条件行 -->
        <div class="search-conditions grid grid-cols-4 gap-x-4 gap-y-3">
          <el-form-item label="任务名称" class="mb-0">
            <el-input
              v-model.trim="searchForm.jobName"
              placeholder="请输入任务名称"
              clearable
            />
          </el-form-item>
          <el-form-item label="执行状态" class="mb-0">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable class="w-full">
              <el-option label="执行中" :value="0" />
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="执行时间" class="mb-0">
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="-"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="w-full"
            />
          </el-form-item>
        </div>
      </el-form>
    </el-card>

    <!-- 数据表格卡片 -->
    <el-card class="table-card flex-1 min-h-0" shadow="never">
      <template #header>
        <div class="card-header flex justify-between items-center">
          <span class="font-medium">执行日志</span>
        </div>
      </template>

      <!-- 表格 -->
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        border
        stripe
        height="100%"
      >
        <el-table-column prop="jobName" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="jobGroup" label="任务分组" width="120" />
        <el-table-column prop="triggerTime" label="触发时间" width="180" />
        <el-table-column prop="finishTime" label="完成时间" width="180" />
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ row.duration ? row.duration + 'ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="executeCount" label="重试次数" width="80" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="searchForm.pageNum"
          v-model:page-size="searchForm.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog title="日志详情" v-model="detailVisible" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务名称">{{ detail.jobName }}</el-descriptions-item>
        <el-descriptions-item label="任务分组">{{ detail.jobGroup }}</el-descriptions-item>
        <el-descriptions-item label="触发时间">{{ detail.triggerTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detail.finishTime }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.duration }}ms</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ detail.executeCount }}</el-descriptions-item>
      </el-descriptions>
      <div class="mt-4" v-if="detail.resultMessage">
        <h4 class="text-sm font-medium mb-2">执行结果</h4>
        <pre class="code-block">{{ detail.resultMessage }}</pre>
      </div>
      <div class="mt-4" v-if="detail.errorMessage">
        <h4 class="text-sm font-medium mb-2 text-red-500">错误信息</h4>
        <pre class="code-block error">{{ detail.errorMessage }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Refresh, ArrowLeft } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { getLogList } from '@/api/job'
import type { SysJobLogVO, QuerySysJobLogReq } from '@/api/job'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

// 搜索表单
const searchForm = reactive<QuerySysJobLogReq>({
  pageNum: 1,
  pageSize: 10,
  jobId: undefined,
  jobName: '',
  status: undefined,
  triggerTimeStart: '',
  triggerTimeEnd: ''
})

// 状态
const loading = ref(false)
const total = ref(0)
const tableData = ref<SysJobLogVO[]>([])
const dateRange = ref<string[]>([])
const detailVisible = ref(false)
const detail = ref<SysJobLogVO>({} as SysJobLogVO)

// 状态类型
const statusType = (status: number) => {
  const types: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger' }
  return types[status] || 'info'
}

// 状态文本
const statusText = (status: number) => {
  const texts: Record<number, string> = { 0: '执行中', 1: '成功', 2: '失败' }
  return texts[status] || '未知'
}

// 获取列表
const fetchData = async () => {
  loading.value = true
  try {
    // 处理日期范围
    if (dateRange.value && dateRange.value.length === 2) {
      searchForm.triggerTimeStart = dateRange.value[0]
      searchForm.triggerTimeEnd = dateRange.value[1]
    } else {
      searchForm.triggerTimeStart = ''
      searchForm.triggerTimeEnd = ''
    }

    const res = await getLogList(searchForm)
    tableData.value = res.data?.body?.rows || []
    total.value = res.data?.body?.total || 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  searchForm.pageNum = 1
  fetchData()
}

// 重置
const handleReset = () => {
  searchForm.jobName = ''
  searchForm.status = undefined
  dateRange.value = []
  handleSearch()
}

// 返回
const handleBack = () => {
  router.push('/system/job')
}

// 查看详情
const handleDetail = (row: SysJobLogVO) => {
  detail.value = row
  detailVisible.value = true
}

// 监听路由参数
watch(
  () => route.query.jobId,
  (val) => {
    if (val) {
      searchForm.jobId = Number(val)
    }
    fetchData()
  },
  { immediate: true }
)

onMounted(() => {
  if (!route.query.jobId) {
    fetchData()
  }
})
</script>

<style scoped>
.table-page-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 16px;
}

.search-card {
  flex-shrink: 0;
}

.table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.table-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.table-card :deep(.el-table) {
  flex: 1;
}

.code-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}

.code-block.error {
  background: #fef0f0;
  color: #f56c6c;
}
</style>
