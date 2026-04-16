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
          <el-form-item label="任务分组" class="mb-0">
            <el-input
              v-model.trim="searchForm.jobGroup"
              placeholder="请输入任务分组"
              clearable
            />
          </el-form-item>
          <el-form-item label="任务状态" class="mb-0">
            <el-select v-model="searchForm.jobStatus" placeholder="请选择状态" clearable class="w-full">
              <el-option label="正常" :value="1" />
              <el-option label="暂停" :value="0" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
    </el-card>

    <!-- 数据表格卡片 -->
    <el-card class="table-card flex-1 min-h-0" shadow="never">
      <template #header>
        <div class="card-header flex justify-between items-center">
          <span class="font-medium">定时任务列表</span>
          <div class="header-buttons flex gap-2">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增
            </el-button>
            <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
              <el-icon><Delete /></el-icon>
              批量删除
            </el-button>
          </div>
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
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="jobName" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="jobGroup" label="任务分组" width="120" />
        <el-table-column prop="cronExpression" label="Cron表达式" width="140" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.jobStatus === 1 ? 'success' : 'danger'">
              {{ row.jobStatus === 1 ? '正常' : '暂停' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="jobDescription" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.jobStatus === 1 ? 'warning' : 'success'"
              link
              @click="handleToggleStatus(row)"
            >
              {{ row.jobStatus === 1 ? '暂停' : '恢复' }}
            </el-button>
            <el-button type="primary" link @click="handleTrigger(row)">执行</el-button>
            <el-button type="primary" link @click="handleViewLog(row)">日志</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑对话框 -->
    <JobForm ref="jobFormRef" @success="handleSearch" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { getJobList, deleteJob, pauseJob, resumeJob, triggerJob } from '@/api/job'
import JobForm from './components/JobForm.vue'
import type { SysJobVO, QuerySysJobReq } from '@/api/job'

const router = useRouter()
const { t } = useI18n()

// 搜索表单
const searchForm = reactive<QuerySysJobReq>({
  pageNum: 1,
  pageSize: 10,
  jobName: '',
  jobGroup: '',
  jobStatus: undefined
})

// 状态
const loading = ref(false)
const total = ref(0)
const tableData = ref<SysJobVO[]>([])
const selectedIds = ref<number[]>([])

// refs
const tableRef = ref()
const jobFormRef = ref()

// 获取列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getJobList(searchForm)
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
  searchForm.jobGroup = ''
  searchForm.jobStatus = undefined
  handleSearch()
}

// 多选
const handleSelectionChange = (selection: SysJobVO[]) => {
  selectedIds.value = selection.map(item => item.jobId)
}

// 新增
const handleAdd = () => {
  jobFormRef.value?.open()
}

// 编辑
const handleEdit = (row: SysJobVO) => {
  jobFormRef.value?.open(row)
}

// 切换状态
const handleToggleStatus = async (row: SysJobVO) => {
  const text = row.jobStatus === 1 ? '暂停' : '恢复'
  try {
    await ElMessageBox.confirm(`确认要${text}任务"${row.jobName}"吗?`, '警告', {
      type: 'warning'
    })
    if (row.jobStatus === 1) {
      await pauseJob({ jobId: row.jobId })
    } else {
      await resumeJob({ jobId: row.jobId })
    }
    ElMessage.success(`${text}成功`)
    fetchData()
  } catch {
    // 用户取消
  }
}

// 立即执行
const handleTrigger = async (row: SysJobVO) => {
  try {
    await ElMessageBox.confirm(`确认要立即执行任务"${row.jobName}"吗?`, '提示')
    await triggerJob({ jobId: row.jobId })
    ElMessage.success('执行成功')
  } catch {
    // 用户取消
  }
}

// 查看日志
const handleViewLog = (row: SysJobVO) => {
  router.push({ path: '/system/job/log', query: { jobId: row.jobId, jobName: row.jobName } })
}

// 删除
const handleDelete = async (row: SysJobVO) => {
  try {
    await ElMessageBox.confirm(`确认要删除任务"${row.jobName}"吗?`, '警告', { type: 'warning' })
    await deleteJob({ jobIds: [row.jobId] })
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 用户取消
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm('确认要删除选中的任务吗?', '警告', { type: 'warning' })
    await deleteJob({ jobIds: selectedIds.value })
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  fetchData()
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
</style>
