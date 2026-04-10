<template>
  <div class="workflow-container">
    <el-tabs v-model="activeTab" class="workflow-tabs">
      <el-tab-pane label="流程设计" name="designer">
        <LogicFlowDesigner />
      </el-tab-pane>

      <el-tab-pane label="流程列表" name="list">
        <div class="process-list">
          <div class="toolbar">
            <el-button type="primary" @click="handleDeploy">
              <el-icon><Upload /></el-icon>
              部署流程
            </el-button>
            <el-button @click="handleRefresh">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>

          <el-table :data="processList" stripe>
            <el-table-column prop="name" label="流程名称" />
            <el-table-column prop="key" label="流程KEY" />
            <el-table-column prop="version" label="版本" width="100" />
            <el-table-column prop="deploymentTime" label="部署时间" width="180" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleStart(row)">启动</el-button>
                <el-button link type="primary" @click="handleViewDiagram(row)">
                  查看流程图
                </el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的待办" name="tasks">
        <div class="task-list">
          <el-table :data="taskList" stripe>
            <el-table-column prop="taskName" label="任务名称" />
            <el-table-column prop="processName" label="流程名称" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleCompleteTask(row)">办理</el-button>
                <el-button link type="primary" @click="handleViewHistory(row)">查看历史</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Refresh } from '@element-plus/icons-vue'
import LogicFlowDesigner from '@/components/LogicFlowDesigner/index.vue'

const activeTab = ref('designer')
const processList = ref<any[]>([])
const taskList = ref<any[]>([])

onMounted(() => {
  loadProcessList()
  loadTaskList()
})

const loadProcessList = async () => {}

const loadTaskList = async () => {}

const handleDeploy = () => {
  ElMessage.info('请先在流程设计器中设计流程，然后点击保存')
}

const handleRefresh = () => {
  loadProcessList()
  ElMessage.success('刷新成功')
}

const handleStart = (row: any) => {
  ElMessage.success(`启动流程: ${row.name}`)
}

const handleViewDiagram = (row: any) => {
  ElMessage.info(`查看流程图: ${row.name}`)
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除流程【${row.name}】吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      ElMessage.success('删除成功')
      loadProcessList()
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

const handleCompleteTask = (row: any) => {
  ElMessage.success(`办理任务: ${row.taskName}`)
}

const handleViewHistory = (row: any) => {
  ElMessage.info(`查看历史: ${row.taskName}`)
}
</script>

<style scoped lang="scss">
.workflow-container {
  height: 100%;
  padding: 20px;
  background: white;
  border-radius: 4px;

  .workflow-tabs {
    height: 100%;

    :deep(.el-tabs__content) {
      height: calc(100% - 55px);

      .el-tab-pane {
        height: 100%;
      }
    }
  }

  .process-list,
  .task-list {
    .toolbar {
      margin-bottom: 16px;
    }
  }
}
</style>
