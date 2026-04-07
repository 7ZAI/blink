<template>
  <!-- 操作日志管理页面 -->
  <div class="operation-log-management table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('system.operationLog.operator')">
          <el-input v-model.trim="searchForm.loginName" :placeholder="t('common.pleaseInput')" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item :label="t('system.operationLog.logType')">
          <el-select v-model="searchForm.logType" :placeholder="t('common.pleaseSelect')" clearable style="width: 100px">
            <el-option
              v-for="item in logTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('system.operationLog.executeStatus')">
          <el-select v-model="searchForm.executeStatus" :placeholder="t('common.pleaseSelect')" clearable style="width: 90px">
            <el-option
              v-for="item in executeStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('system.operationLog.operationTime')">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            :range-separator="t('common.to')"
            :start-placeholder="t('common.startTime')"
            :end-placeholder="t('common.endTime')"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="handleDateChange"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item :label="t('common.keyword')">
          <el-input v-model.trim="searchForm.keyword" :placeholder="t('system.operationLog.keywordPlaceholder')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleSearch">
            <el-icon><Search /></el-icon>{{ t('common.search') }}
          </el-button>
          <el-button style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleReset">
            <el-icon><Refresh /></el-icon>{{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card" shadow="never">
      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="logList"
          height="100%"
          stripe
          @sort-change="handleSortChange"
        >
          <el-table-column prop="operationTime" :label="t('system.operationLog.operationTime')" min-width="155" sortable="custom" />
          <el-table-column prop="loginName" :label="t('system.operationLog.operator')" min-width="80" />
          <el-table-column prop="logTypeDesc" :label="t('system.operationLog.logType')" min-width="85">
            <template #default="{ row }">
              <el-tag :type="getLogTypeTagType(row.logType)">{{ row.logTypeDesc }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" :label="t('system.operationLog.description')" min-width="140" show-overflow-tooltip />
          <el-table-column prop="requestUrl" :label="t('system.operationLog.requestUrl')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="executeStatusDesc" :label="t('system.operationLog.executeStatus')" min-width="80">
            <template #default="{ row }">
              <el-tag :type="row.executeStatus === 0 ? 'success' : 'danger'">{{ row.executeStatusDesc }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="executeTimeMs" :label="t('system.operationLog.executeTimeMs')" min-width="85" sortable="custom">
            <template #default="{ row }">
              <span :class="getExecuteTimeClass(row.executeTimeMs)">{{ row.executeTimeMs }}ms</span>
            </template>
          </el-table-column>
          <el-table-column prop="ipAddress" :label="t('system.operationLog.ipAddress')" min-width="130" />
          <el-table-column :label="t('common.operation')" width="90" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <el-button type="primary" link size="small" @click="handleDetail(row)">
                  <el-icon><View /></el-icon>{{ t('common.detail') }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <OperationLogDetailDialog
      v-model="detailVisible"
      :log-id="currentLogId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import {
  getOperationLogList,
  type OperationLogInfo,
  type QueryOperationLogParams,
  logTypeOptions,
  executeStatusOptions,
  LogType
} from '@/api/operation-log'
import OperationLogDetailDialog from './components/OperationLogDetailDialog.vue'

defineOptions({
  name: 'SystemOperationLog',
})

const { t } = useI18n()

const searchForm = reactive({
  loginName: '',
  logType: '',
  executeStatus: undefined as number | undefined,
  startTime: '',
  endTime: '',
  keyword: '',
  orderBy: '',
})

const dateRange = ref<[string, string] | null>(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

// 排序状态
const sortState = ref<{ prop: string; order: string | null }>({
  prop: 'operationTime',
  order: 'descending'
})

const loading = ref(false)
const logList = ref<OperationLogInfo[]>([])

const detailVisible = ref(false)
const currentLogId = ref<number>(0)

/**
 * 获取日志类型标签样式
 * @param type 日志类型
 * @returns 标签类型
 */
const getLogTypeTagType = (type: string): string => {
  const typeMap: Record<string, string> = {
    [LogType.LOGIN]: 'success',
    [LogType.SYSTEM]: 'warning',
    [LogType.OPERATION]: 'primary',
  }
  return typeMap[type] || 'info'
}

/**
 * 获取执行时长样式类
 * @param ms 执行时长（毫秒）
 * @returns CSS类名
 */
const getExecuteTimeClass = (ms: number): string => {
  if (ms < 100) return 'text-green-500'
  if (ms < 500) return 'text-yellow-500'
  return 'text-red-500'
}

const handleDateChange = (val: [string, string] | null) => {
  if (val) {
    searchForm.startTime = val[0]
    searchForm.endTime = val[1]
  } else {
    searchForm.startTime = ''
    searchForm.endTime = ''
  }
}

const fetchLogList = async () => {
  loading.value = true
  try {
    // 构建排序参数
    let orderBy = ''
    if (sortState.value.prop && sortState.value.order) {
      const order = sortState.value.order === 'ascending' ? 'asc' : 'desc'
      orderBy = `${sortState.value.prop} ${order}`
    }

    const params: QueryOperationLogParams = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm,
      orderBy,
    }
    const res = await getOperationLogList(params)
    logList.value = res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchLogList()
}

const handleReset = () => {
  searchForm.loginName = ''
  searchForm.logType = ''
  searchForm.executeStatus = undefined
  searchForm.startTime = ''
  searchForm.endTime = ''
  searchForm.keyword = ''
  searchForm.orderBy = ''
  dateRange.value = null
  // 重置排序状态为默认（操作时间降序）
  sortState.value = { prop: 'operationTime', order: 'descending' }
  handleSearch()
}

const handleDetail = (row: OperationLogInfo) => {
  currentLogId.value = row.logId
  detailVisible.value = true
}

/**
 * 表格排序变化处理
 * @param sort 排序信息
 */
const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
  sortState.value = { prop, order }
  pagination.pageNum = 1
  fetchLogList()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  fetchLogList()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  fetchLogList()
}

onMounted(() => {
  fetchLogList()
})
</script>

<style scoped lang="scss">
/* 操作日志管理页面 - 继承全局 table-page-container 样式 */
</style>