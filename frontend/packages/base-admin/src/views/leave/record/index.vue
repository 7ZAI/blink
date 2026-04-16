<template>
  <div class="leave-record-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="申请人">
          <el-input
            v-model="queryParams.applicantName"
            placeholder="请输入申请人姓名"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="请假类型">
          <el-select v-model="queryParams.leaveType" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in leaveTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetQuery">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <span>请假记录列表</span>
      </template>

      <!-- 表格 -->
      <el-table :data="leaveList" v-loading="loading" stripe>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="部门" width="120">
          <template #default="{ row }">
            {{ row.deptName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="leaveTypeName" label="请假类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.leaveTypeName || row.leaveType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始时间" width="160" />
        <el-table-column prop="endDate" label="结束时间" width="160" />
        <el-table-column prop="days" label="天数" width="80" align="center" />
        <el-table-column prop="reason" label="请假原因" show-overflow-tooltip min-width="150" />
        <el-table-column prop="statusName" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.statusName || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="请假详情" width="650px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentDetail">
        <el-descriptions-item label="申请人">{{ currentDetail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ currentDetail.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请假类型">{{ currentDetail.leaveTypeName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentDetail.status)">{{ currentDetail.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentDetail.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentDetail.endDate }}</el-descriptions-item>
        <el-descriptions-item label="请假天数">{{ currentDetail.days }}天</el-descriptions-item>
        <el-descriptions-item label="当前节点">{{ currentDetail.currentTask || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请假原因" :span="2">{{ currentDetail.reason }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentDetail.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <!-- 审批记录 -->
      <div class="approval-timeline" v-if="currentDetail?.approvalList?.length">
        <h4>审批记录</h4>
        <el-timeline>
          <el-timeline-item
            v-for="item in currentDetail.approvalList"
            :key="item.id"
            :type="item.approvalResult === 'approved' ? 'success' : 'danger'"
          >
            <div class="timeline-content">
              <div class="timeline-header">
                <span class="task-name">{{ item.taskName }}</span>
                <el-tag :type="item.approvalResult === 'approved' ? 'success' : 'danger'" size="small">
                  {{ item.approvalResultName }}
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
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  getAllLeaveList,
  getLeaveDetail,
} from '@/api/leave'
import type { LeaveRequestVO, LeaveType, LeaveStatus, QueryLeaveReq } from '@/api/types/leave'
import { LEAVE_TYPE_OPTIONS, LEAVE_STATUS_OPTIONS, getLeaveStatusType } from '@/api/types/leave'

// 获取状态标签类型
const getStatusType = (status: LeaveStatus) => {
  return getLeaveStatusType(status)
}

// 数据
const loading = ref(false)
const leaveList = ref<LeaveRequestVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentDetail = ref<LeaveRequestVO | null>(null)

// 选项数据
const leaveTypeOptions = LEAVE_TYPE_OPTIONS
const statusOptions = LEAVE_STATUS_OPTIONS

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '' as LeaveStatus | '',
  leaveType: '' as LeaveType | '',
  applicantName: '',
})

// 生命周期
onMounted(() => {
  getList()
})

// 获取列表
const getList = async () => {
  loading.value = true
  try {
    const params: QueryLeaveReq = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      status: queryParams.status || undefined,
      leaveType: queryParams.leaveType || undefined,
      applicantName: queryParams.applicantName || undefined,
    }

    const res = await getAllLeaveList(params)
    leaveList.value = res.rows || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取请假记录失败', error)
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

// 重置
const resetQuery = () => {
  queryParams.status = ''
  queryParams.leaveType = ''
  queryParams.applicantName = ''
  handleQuery()
}

// 查看详情
const handleDetail = async (row: LeaveRequestVO) => {
  try {
    const res = await getLeaveDetail(row.id)
    currentDetail.value = res
    detailVisible.value = true
  } catch (error) {
    console.error('获取详情失败', error)
  }
}
</script>

<style scoped>
.leave-record-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.table-card {
  margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.approval-timeline {
  margin-top: 24px;
}

.approval-timeline h4 {
  margin-bottom: 16px;
  color: #303133;
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
