<template>
  <div class="workflow-process">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('workflow.processList') }}</span>
          <div class="header-actions">
            <el-input
              v-model.trim="searchName"
              :placeholder="t('workflow.processName')"
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
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :show-file-list="false"
              accept=".bpmn20.xml,.bpmn"
              :on-change="handleXmlFileChange"
            >
              <el-button type="success">
                <el-icon><Upload /></el-icon>
                {{ t('workflow.importXml') }}
              </el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table :data="processList" stripe v-loading="loading">
          <el-table-column
            prop="processDefinitionName"
            :label="t('workflow.processName')"
            min-width="150"
          />
          <el-table-column
            prop="processDefinitionKey"
            :label="t('workflow.processKey')"
            min-width="150"
          />
          <el-table-column prop="version" :label="t('workflow.version')" width="100">
            <template #default="{ row }">
              <el-tag type="primary">v{{ row.version }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deploymentTime" :label="t('common.createTime')" width="180" />
          <el-table-column prop="suspended" :label="t('common.status')" width="100">
            <template #default="{ row }">
              <el-tag :type="!row.suspended ? 'success' : 'warning'">
                {{ !row.suspended ? t('workflow.active') : t('workflow.suspended') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleStart(row)">
                <el-icon><VideoPlay /></el-icon>
                {{ t('workflow.start') }}
              </el-button>
              <el-button link type="primary" @click="handleViewDiagram(row)">
                <el-icon><View /></el-icon>
                {{ t('workflow.viewDiagram') }}
              </el-button>
              <el-button
                link
                :type="row.suspended ? 'success' : 'warning'"
                @click="handleToggleSuspend(row)"
              >
                <el-icon><component :is="row.suspended ? VideoPlay : VideoPause" /></el-icon>
                {{ row.suspended ? t('workflow.activate') : t('workflow.suspend') }}
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
                {{ t('common.delete') }}
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
          @size-change="loadProcessList"
          @current-change="loadProcessList"
        />
      </div>
    </el-card>

    <!-- 启动流程对话框 -->
    <el-dialog v-model="startDialogVisible" :title="t('workflow.startProcess')" width="500px">
      <el-form :model="startForm" label-width="100px">
        <el-form-item :label="t('workflow.processName')">
          <el-input :value="currentProcess?.processDefinitionName" disabled />
        </el-form-item>
        <el-form-item :label="t('workflow.businessKey')">
          <el-input
            v-model.trim="startForm.businessKey"
            :placeholder="t('workflow.businessKeyPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmStart" :loading="startLoading">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看流程图对话框 -->
    <el-dialog v-model="diagramDialogVisible" :title="t('workflow.processDiagram')" width="800px">
      <div class="diagram-container">
        <pre v-if="diagramXml" class="xml-content">{{ diagramXml }}</pre>
        <el-empty v-else :description="t('common.noData')" />
      </div>
    </el-dialog>

    <!-- 导入XML流程对话框 -->
    <el-dialog v-model="importDialogVisible" :title="t('workflow.importXml')" width="500px">
      <el-form :model="importForm" label-width="100px">
        <el-form-item :label="t('workflow.processName')" required>
          <el-input
            v-model.trim="importForm.processName"
            :placeholder="t('workflow.processNamePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('workflow.description')">
          <el-input
            v-model.trim="importForm.description"
            type="textarea"
            :rows="3"
            :placeholder="t('workflow.descriptionPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmImport" :loading="importLoading">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { Search, Refresh, VideoPlay, View, VideoPause, Delete, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getProcessDefinitionList,
  suspendProcessDefinition,
  activateProcessDefinition,
  deleteProcessDefinition,
  startProcess,
  getProcessDiagramXml,
  importProcessFromXml,
  type ProcessDefinitionInfo,
} from '@/api/workflow'

defineOptions({
  name: 'WorkflowProcess',
})

const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const searchName = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const processList = ref<ProcessDefinitionInfo[]>([])

const startDialogVisible = ref(false)
const startLoading = ref(false)
const currentProcess = ref<ProcessDefinitionInfo | null>(null)
const startForm = ref({
  businessKey: '',
})

const diagramDialogVisible = ref(false)
const diagramXml = ref('')

const importDialogVisible = ref(false)
const importLoading = ref(false)
const importForm = ref({
  processName: '',
  bpmnXmlContent: '',
  description: '',
})

onMounted(() => {
  loadProcessList()
})

const loadProcessList = async () => {
  loading.value = true
  try {
    const res = await getProcessDefinitionList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      name: searchName.value || undefined,
      latestVersion: true,
    })
    processList.value = res.rows || []
    total.value = res.total || 0
  } catch (error) {
    console.error('[WorkflowProcess] 加载流程定义列表失败', error)
    ElMessage.error(t('message.loadFailed'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadProcessList()
}

const handleReset = () => {
  searchName.value = ''
  currentPage.value = 1
  loadProcessList()
}

const handleStart = (row: ProcessDefinitionInfo) => {
  currentProcess.value = row
  startForm.value.businessKey = ''
  startDialogVisible.value = true
}

const confirmStart = async () => {
  if (!currentProcess.value) return

  startLoading.value = true
  try {
    await startProcess({
      processDefinitionKey: currentProcess.value.processDefinitionKey,
      businessKey: startForm.value.businessKey || undefined,
      variables: {
        startUserId: userStore.userInfo?.userId,
        startUserName: userStore.userInfo?.username,
      },
    })
    ElMessage.success(t('workflow.startSuccess'))
    startDialogVisible.value = false
  } catch (error) {
    console.error('[WorkflowProcess] 启动流程失败', error)
    ElMessage.error(t('message.operationFailed'))
  } finally {
    startLoading.value = false
  }
}

const handleViewDiagram = async (row: ProcessDefinitionInfo) => {
  try {
    diagramXml.value = await getProcessDiagramXml(row.processDefinitionId)
    diagramDialogVisible.value = true
  } catch (error) {
    console.error('[WorkflowProcess] 获取流程图XML失败', error)
    ElMessage.error(t('message.operationFailed'))
  }
}

const handleXmlFileChange = async (file: UploadFile) => {
  if (!file.raw) return

  const reader = new FileReader()
  reader.onload = async (e) => {
    const xmlContent = e.target?.result as string
    importForm.value.bpmnXmlContent = xmlContent
    importForm.value.processName = ''
    importForm.value.description = ''
    importDialogVisible.value = true
  }
  reader.readAsText(file.raw)
}

const confirmImport = async () => {
  if (!importForm.value.processName) {
    ElMessage.warning(t('workflow.processNameRequired'))
    return
  }

  importLoading.value = true
  try {
    await importProcessFromXml(importForm.value)
    ElMessage.success(t('workflow.importSuccess'))
    importDialogVisible.value = false
    loadProcessList()
  } catch (error) {
    console.error('[WorkflowProcess] 导入流程失败', error)
    ElMessage.error(t('workflow.importFailed'))
  } finally {
    importLoading.value = false
  }
}

const handleToggleSuspend = async (row: ProcessDefinitionInfo) => {
  const action = row.suspended ? t('workflow.activate') : t('workflow.suspend')
  try {
    await ElMessageBox.confirm(
      t('workflow.toggleSuspendConfirm', { action, name: row.processDefinitionName }),
      t('message.tips'),
      { type: 'warning' }
    )

    if (row.suspended) {
      await activateProcessDefinition(row.processDefinitionId)
    } else {
      await suspendProcessDefinition(row.processDefinitionId)
    }

    ElMessage.success(t('message.success'))
    loadProcessList()
  } catch {
    // 取消操作
  }
}

const handleDelete = async (row: ProcessDefinitionInfo) => {
  try {
    await ElMessageBox.confirm(
      t('workflow.deleteProcessConfirm', { name: row.processDefinitionName }),
      t('message.tips'),
      { type: 'warning' }
    )
    await deleteProcessDefinition(row.deploymentId, true)
    ElMessage.success(t('message.deleteSuccess'))
    loadProcessList()
  } catch {
    // 取消删除
  }
}
</script>

<style scoped lang="scss">
.workflow-process {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

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

.diagram-container {
  max-height: 500px;
  overflow: auto;

  .xml-content {
    background: var(--bg-color-page);
    padding: 16px;
    border-radius: 8px;
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 12px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
