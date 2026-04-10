<template>
  <div class="workflow-task">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-tabs">
            <el-radio-group v-model="activeStatus" @change="handleStatusChange">
              <el-radio-button value="pending">{{ t('workflow.pendingTasks') }}</el-radio-button>
              <el-radio-button value="completed">
                {{ t('workflow.completedTasks') }}
              </el-radio-button>
              <el-radio-button value="mine">{{ t('workflow.myProcesses') }}</el-radio-button>
            </el-radio-group>
          </div>
          <div class="header-actions">
            <el-input
              v-model.trim="searchName"
              :placeholder="t('workflow.taskName')"
              clearable
              style="width: 200px"
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
          <el-table-column prop="taskName" :label="t('workflow.taskName')" min-width="150" />
          <el-table-column prop="processName" :label="t('workflow.processName')" min-width="120">
            <template #default="{ row }">
              {{ row.processName || row.processDefinitionName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="assignee" :label="t('workflow.assignee')" width="120">
            <template #default="{ row }">
              {{ row.assignee || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('common.createTime')" width="180" />
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
          <el-table-column :label="t('common.operation')" width="200" fixed="right">
            <template #default="{ row }">
              <template v-if="activeStatus === 'pending'">
                <el-button link type="primary" @click="handleComplete(row)">
                  <el-icon><Check /></el-icon>
                  {{ t('workflow.complete') }}
                </el-button>
                <el-button link type="primary" @click="handleDelegate(row)">
                  <el-icon><User /></el-icon>
                  {{ t('workflow.delegate') }}
                </el-button>
              </template>
              <el-button link type="primary" @click="handleViewHistory(row)">
                <el-icon><Clock /></el-icon>
                {{ t('workflow.history') }}
              </el-button>
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

    <!-- 完成任务对话框 -->
    <el-dialog v-model="completeDialogVisible" :title="t('workflow.completeTask')" width="500px">
      <el-form :model="completeForm" label-width="100px">
        <el-form-item :label="t('workflow.taskName')">
          <el-input :value="currentTask?.taskName" disabled />
        </el-form-item>
        <el-form-item :label="t('workflow.comment')">
          <el-input
            v-model="completeForm.comment"
            type="textarea"
            :rows="3"
            :placeholder="t('workflow.commentPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('workflow.approved')">
          <el-radio-group v-model="completeForm.approved">
            <el-radio :value="true">{{ t('workflow.approve') }}</el-radio>
            <el-radio :value="false">{{ t('workflow.reject') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmComplete" :loading="completeLoading">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 委托任务对话框 -->
    <el-dialog v-model="delegateDialogVisible" :title="t('workflow.delegateTask')" width="500px">
      <el-form :model="delegateForm" label-width="100px">
        <el-form-item :label="t('workflow.taskName')">
          <el-input :value="currentTask?.taskName" disabled />
        </el-form-item>
        <el-form-item :label="t('workflow.targetUser')">
          <el-input
            v-model.trim="delegateForm.targetUserId"
            :placeholder="t('workflow.targetUserPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="delegateDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmDelegate" :loading="delegateLoading">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 流程历史对话框 -->
    <el-dialog v-model="historyDialogVisible" :title="t('workflow.processHistory')" width="800px">
      <el-table :data="historyList" stripe v-loading="historyLoading">
        <el-table-column prop="activityName" :label="t('workflow.activityName')" min-width="120" />
        <el-table-column prop="activityType" :label="t('workflow.activityType')" width="120" />
        <el-table-column prop="assignee" :label="t('workflow.assignee')" width="120">
          <template #default="{ row }">
            {{ row.assignee || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" :label="t('workflow.startTime')" width="180" />
        <el-table-column prop="endTime" :label="t('workflow.endTime')" width="180">
          <template #default="{ row }">
            {{ row.endTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.endTime ? 'success' : 'warning'">
              {{ row.endTime ? t('workflow.completed') : t('workflow.pending') }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Check, User, Clock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getPendingTasks,
  getCompletedTasks,
  getMyProcessInstances,
  completeTask,
  delegateTask,
  getProcessHistory,
  type TaskInfo,
  type HistoricTaskInfo,
  type ProcessInstanceInfo,
  type ProcessHistoryInfo,
} from '@/api/workflow'

defineOptions({
  name: 'WorkflowTask',
})

const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const activeStatus = ref('pending')
const searchName = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

type TaskRow = TaskInfo | HistoricTaskInfo | ProcessInstanceInfo
const taskList = ref<TaskRow[]>([])

const completeDialogVisible = ref(false)
const completeLoading = ref(false)
const currentTask = ref<TaskInfo | null>(null)
const completeForm = ref({
  comment: '',
  approved: true,
})

const delegateDialogVisible = ref(false)
const delegateLoading = ref(false)
const delegateForm = ref({
  targetUserId: '',
})

const historyDialogVisible = ref(false)
const historyLoading = ref(false)
const historyList = ref<ProcessHistoryInfo[]>([])

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
      })
      taskList.value = res.rows || []
      total.value = res.total || 0
    } else if (activeStatus.value === 'completed') {
      const res = await getCompletedTasks({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        userId,
        taskName: searchName.value || undefined,
      })
      taskList.value = res.rows || []
      total.value = res.total || 0
    } else {
      const res = await getMyProcessInstances(userId, 'all')
      taskList.value = res || []
      total.value = taskList.value.length
    }
  } catch (error) {
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

const handleComplete = (row: TaskInfo) => {
  currentTask.value = row
  completeForm.value = {
    comment: '',
    approved: true,
  }
  completeDialogVisible.value = true
}

const confirmComplete = async () => {
  if (!currentTask.value) return

  completeLoading.value = true
  try {
    await completeTask({
      taskId: currentTask.value.taskId,
      userId: String(userStore.userInfo?.userId || ''),
      comment: completeForm.value.comment,
      approved: completeForm.value.approved,
    })
    ElMessage.success(t('message.success'))
    completeDialogVisible.value = false
    loadTaskList()
  } catch (error) {
  } finally {
    completeLoading.value = false
  }
}

const handleDelegate = (row: TaskInfo) => {
  currentTask.value = row
  delegateForm.value.targetUserId = ''
  delegateDialogVisible.value = true
}

const confirmDelegate = async () => {
  if (!currentTask.value || !delegateForm.value.targetUserId) {
    ElMessage.warning(t('workflow.targetUserRequired'))
    return
  }

  delegateLoading.value = true
  try {
    await delegateTask({
      taskId: currentTask.value.taskId,
      currentUserId: String(userStore.userInfo?.userId || ''),
      targetUserId: delegateForm.value.targetUserId,
    })
    ElMessage.success(t('message.success'))
    delegateDialogVisible.value = false
    loadTaskList()
  } catch (error) {
  } finally {
    delegateLoading.value = false
  }
}

const handleViewHistory = async (row: TaskRow) => {
  const processInstanceId = 'processInstanceId' in row ? row.processInstanceId : ''

  if (!processInstanceId) {
    ElMessage.warning(t('workflow.noProcessInstance'))
    return
  }

  historyDialogVisible.value = true
  historyLoading.value = true
  try {
    historyList.value = await getProcessHistory(processInstanceId)
  } catch (error) {
    historyList.value = []
  } finally {
    historyLoading.value = false
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
</style>
