<template>
  <div class="workflow-task">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-tabs">
            <el-radio-group v-model="activeStatus" @change="handleStatusChange">
              <el-radio-button value="pending">{{ t('workflow.pendingTasks') }}</el-radio-button>
              <el-radio-button value="completed">{{ t('workflow.completedTasks') }}</el-radio-button>
              <el-radio-button value="mine">{{ t('workflow.myProcesses') }}</el-radio-button>
            </el-radio-group>
          </div>
          <div class="header-actions">
            <el-select
              v-model="filterProcessType"
              :placeholder="t('workflow.processType')"
              clearable
              style="width: 150px"
              @change="handleSearch"
            >
              <el-option label="请假审批" value="leaveRequest" />
            </el-select>
            <el-input
              v-model.trim="searchName"
              :placeholder="t('workflow.taskName')"
              clearable
              style="width: 180px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              {{ t('common.search') }}
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              {{ t('common.reset') }}
            </el-button>
          </div>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table :data="taskList" stripe v-loading="loading">
          <el-table-column prop="taskName" :label="t('workflow.taskName')" min-width="120" />
          <el-table-column :label="t('workflow.processName')" min-width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="getProcessTypeTag(row)">
                {{ getProcessTypeName(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="activeStatus === 'pending'" label="申请人" width="100">
            <template #default="{ row }">
              {{ row.processVariables?.applicant || row.processVariables?.applicantName || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="activeStatus === 'pending' && filterProcessType === 'leaveRequest'"
            label="请假信息"
            min-width="200"
          >
            <template #default="{ row }">
              <span v-if="row.processVariables">
                {{ getLeaveTypeLabel(row.processVariables.leaveType) }} |
                {{ row.processVariables.days }}天 |
                {{ row.processVariables.startDate?.substring(0, 10) }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="assignee" :label="t('workflow.assignee')" width="100">
            <template #default="{ row }">
              {{ row.assignee || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('common.createTime')" width="160" />
          <el-table-column
            prop="status"
            :label="t('common.status')"
            width="100"
            v-if="activeStatus !== 'pending'"
          >
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="220" fixed="right">
            <template #default="{ row }">
              <template v-if="activeStatus === 'pending'">
                <el-button link type="primary" @click="handleDetail(row)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
                <el-button link type="success" @click="handleApprove(row, true)">
                  <el-icon><Check /></el-icon>
                  {{ t('workflow.approve') }}
                </el-button>
                <el-button link type="danger" @click="handleApprove(row, false)">
                  <el-icon><Close /></el-icon>
                  {{ t('workflow.reject') }}
                </el-button>
              </template>
              <template v-else>
                <el-button link type="primary" @click="handleDetail(row)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadTaskList"
          @current-change="loadTaskList"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="任务详情" width="700px" destroy-on-close>
      <template v-if="currentTaskDetail">
        <!-- 请假审批详情 -->
        <template v-if="currentTaskDetail.processDefinitionKey === 'leaveRequest'">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="申请人">
              {{ currentTaskDetail.processVariables?.applicantName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="请假类型">
              {{ getLeaveTypeLabel(currentTaskDetail.processVariables?.leaveType) }}
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">
              {{ currentTaskDetail.processVariables?.startDate }}
            </el-descriptions-item>
            <el-descriptions-item label="结束时间">
              {{ currentTaskDetail.processVariables?.endDate }}
            </el-descriptions-item>
            <el-descriptions-item label="请假天数">
              {{ currentTaskDetail.processVariables?.days }}天
            </el-descriptions-item>
            <el-descriptions-item label="当前节点">
              {{ currentTaskDetail.taskName }}
            </el-descriptions-item>
            <el-descriptions-item label="请假原因" :span="2">
              {{ currentTaskDetail.processVariables?.reason || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
        <!-- 其他流程详情 -->
        <template v-else>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="任务名称">{{ currentTaskDetail.taskName }}</el-descriptions-item>
            <el-descriptions-item label="流程名称">{{ getProcessTypeName(currentTaskDetail) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentTaskDetail.createTime }}</el-descriptions-item>
            <el-descriptions-item label="受理人">{{ currentTaskDetail.assignee || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>

        <!-- 审批历史 -->
        <div class="approval-history" v-if="approvalHistory.length">
          <h4>审批历史</h4>
          <el-timeline>
            <el-timeline-item
              v-for="item in approvalHistory"
              :key="item.id"
              :type="item.approvalResult === 'approved' ? 'success' : 'danger'"
            >
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="task-name">{{ item.taskName }}</span>
                  <el-tag
                    :type="item.approvalResult === 'approved' ? 'success' : 'danger'"
                    size="small"
                  >
                    {{ item.approvalResult === 'approved' ? '通过' : '拒绝' }}
                  </el-tag>
                </div>
                <div class="timeline-info">
                  <span>审批人：{{ item.approverName }}</span>
                  <span>审批时间：{{ item.approvalTime }}</span>
                </div>
                <div v-if="item.approvalComment" class="timeline-comment">
                  审批意见：{{ item.approvalComment }}
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <template v-if="activeStatus === 'pending'">
          <el-button type="danger" @click="handleApproveFromDetail(false)">
            {{ t('workflow.reject') }}
          </el-button>
          <el-button type="success" @click="handleApproveFromDetail(true)">
            {{ t('workflow.approve') }}
          </el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog v-model="approvalDialogVisible" :title="approvalTitle" width="450px" destroy-on-close>
      <el-form :model="approvalForm" label-width="80px">
        <el-form-item label="审批意见">
          <el-input
            v-model="approvalForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入审批意见（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          :type="approvalForm.approved ? 'success' : 'danger'"
          @click="confirmApproval"
          :loading="approvalLoading"
        >
          确认{{ approvalForm.approved ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Refresh, View, Check, Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getPendingTasks,
  getCompletedTasks,
  getMyProcessInstances,
  completeTask,
  type TaskInfo,
  type HistoricTaskInfo,
  type ProcessInstanceInfo,
} from '@/api/workflow'
import { getLeaveDetail, approvalLeave } from '@/api/leave'
import type { LeaveRequestVO } from '@/api/types/leave'

defineOptions({
  name: 'WorkflowTask',
})

const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const activeStatus = ref('pending')
const searchName = ref('')
const filterProcessType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

type TaskRow = TaskInfo | HistoricTaskInfo | ProcessInstanceInfo
const taskList = ref<TaskRow[]>([])

// 详情对话框
const detailDialogVisible = ref(false)
const currentTaskDetail = ref<TaskInfo | null>(null)
const currentLeaveDetail = ref<LeaveRequestVO | null>(null)
const approvalHistory = ref<any[]>([])

// 审批对话框
const approvalDialogVisible = ref(false)
const approvalLoading = ref(false)
const approvalForm = ref({
  taskId: '',
  processInstanceId: '',
  approved: true,
  comment: '',
  leaveRequestId: 0,
})

const approvalTitle = computed(() => {
  return approvalForm.value.approved ? '审批通过' : '审批拒绝'
})

// 请假类型映射
const leaveTypeMap: Record<string, string> = {
  annual: '年假',
  sick: '病假',
  personal: '事假',
  compensatory: '调休',
  marriage: '婚假',
  maternity: '产假',
}

const getLeaveTypeLabel = (type?: string) => {
  return leaveTypeMap[type || ''] || type || '-'
}

const getProcessTypeName = (row: TaskRow) => {
  const key = 'processDefinitionKey' in row ? row.processDefinitionKey : ''
  const name = 'processDefinitionName' in row ? row.processDefinitionName : ''
  if (key === 'leaveRequest') return '请假审批'
  return name || '未知流程'
}

const getProcessTypeTag = (row: TaskRow) => {
  const key = 'processDefinitionKey' in row ? row.processDefinitionKey : ''
  if (key === 'leaveRequest') return 'success'
  return 'info'
}

onMounted(() => {
  loadTaskList()
})

const loadTaskList = async () => {
  loading.value = true
  try {
    const userId = String(userStore.userInfo?.userId || '')

    if (activeStatus.value === 'pending') {
      const res = await getPendingTasks({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        userId,
        taskName: searchName.value || undefined,
        processDefinitionKey: filterProcessType.value || undefined,
      })
      taskList.value = res.rows || []
      total.value = res.total || 0
    } else if (activeStatus.value === 'completed') {
      const res = await getCompletedTasks({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        userId,
        taskName: searchName.value || undefined,
        processDefinitionKey: filterProcessType.value || undefined,
      })
      taskList.value = res.rows || []
      total.value = res.total || 0
    } else {
      const res = await getMyProcessInstances(userId, 'all')
      taskList.value = res || []
      total.value = taskList.value.length
    }
  } catch (error) {
    console.error('[WorkflowTask] 加载任务列表失败', error)
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  currentPage.value = 1
  loadTaskList()
}

const handleSearch = () => {
  currentPage.value = 1
  loadTaskList()
}

const handleReset = () => {
  searchName.value = ''
  filterProcessType.value = ''
  currentPage.value = 1
  loadTaskList()
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    running: 'warning',
    completed: 'success',
    terminated: 'danger',
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    running: t('workflow.running'),
    completed: t('workflow.completed'),
    terminated: t('workflow.terminated'),
  }
  return map[status] || status
}

// 查看详情
const handleDetail = async (row: TaskInfo) => {
  currentTaskDetail.value = row as TaskInfo
  currentLeaveDetail.value = null
  approvalHistory.value = []

  // 如果是请假流程，加载请假详情
  if (row.processDefinitionKey === 'leaveRequest' && row.processVariables?.businessKey) {
    try {
      const leaveDetail = await getLeaveDetail(Number(row.processVariables.businessKey))
      currentLeaveDetail.value = leaveDetail
      approvalHistory.value = leaveDetail.approvalList || []
    } catch (error) {
      console.error('[WorkflowTask] 加载请假详情失败', error)
    }
  }

  detailDialogVisible.value = true
}

// 打开审批对话框
const handleApprove = (row: TaskInfo, approved: boolean) => {
  approvalForm.value = {
    taskId: row.taskId,
    processInstanceId: row.processInstanceId,
    approved,
    comment: '',
    leaveRequestId: row.processVariables?.businessKey
      ? Number(row.processVariables.businessKey)
      : 0,
  }
  approvalDialogVisible.value = true
}

// 从详情对话框审批
const handleApproveFromDetail = (approved: boolean) => {
  if (!currentTaskDetail.value) return
  detailDialogVisible.value = false
  handleApprove(currentTaskDetail.value, approved)
}

// 确认审批
const confirmApproval = async () => {
  approvalLoading.value = true
  try {
    const userId = String(userStore.userInfo?.userId || '')

    // 如果是请假流程，调用请假审批接口
    if (currentTaskDetail.value?.processDefinitionKey === 'leaveRequest') {
      await approvalLeave({
        leaveRequestId: approvalForm.value.leaveRequestId,
        approvalResult: approvalForm.value.approved ? 'approved' : 'rejected',
        approvalComment: approvalForm.value.comment,
      })
    } else {
      // 其他流程使用通用审批
      await completeTask({
        taskId: approvalForm.value.taskId,
        userId,
        comment: approvalForm.value.comment,
        approved: approvalForm.value.approved,
      })
    }

    ElMessage.success(approvalForm.value.approved ? '审批通过成功' : '审批拒绝成功')
    approvalDialogVisible.value = false
    loadTaskList()
  } catch (error) {
    console.error('[WorkflowTask] 审批失败', error)
    ElMessage.error('审批失败')
  } finally {
    approvalLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.workflow-task {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;

  .header-tabs {
    display: flex;
    align-items: center;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.approval-history {
  margin-top: 24px;

  h4 {
    margin-bottom: 16px;
    color: #303133;
  }
}

.timeline-content {
  padding: 4px 0;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.task-name {
  font-weight: 500;
  color: #303133;
}

.timeline-info {
  display: flex;
  gap: 24px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 4px;
}

.timeline-comment {
  color: #606266;
  font-size: 13px;
}
</style>
