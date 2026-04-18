<template>
  <div class="instance-group-page">
    <!-- 分组列表 -->
    <el-card class="page-card" shadow="never">
      <!-- 搜索区域 -->
      <div class="search-area">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item :label="t('instanceGroup.groupKey')">
            <el-input
              v-model="searchForm.groupKey"
              :placeholder="t('instanceGroup.groupKeyPlaceholder')"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item :label="t('instanceGroup.groupName')">
            <el-input
              v-model="searchForm.groupName"
              :placeholder="t('instanceGroup.groupNamePlaceholder')"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item :label="t('common.status')">
            <el-select v-model="searchForm.status" :placeholder="t('common.pleaseSelect')" clearable style="width: 140px">
              <el-option :label="t('common.statusEnable')" :value="0" />
              <el-option :label="t('common.statusDisable')" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              {{ t('common.search') }}
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              {{ t('common.reset') }}
            </el-button>
          </el-form-item>
          <el-form-item class="right-actions">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              {{ t('common.add') }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 表格 -->
      <el-table :data="groupList" v-loading="loading" stripe class="group-table">
        <el-table-column prop="groupKey" :label="t('instanceGroup.groupKey')" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="group-key">{{ row.groupKey }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="groupName" :label="t('instanceGroup.groupName')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="t('common.status')" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="row.groupKey === DEFAULT_GROUP_KEY"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.remark">{{ row.remark }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('common.createTime')" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.createTime">{{ formatTime(row.createTime) }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
                {{ t('common.edit') }}
              </el-button>
              <el-button
                v-if="row.groupKey !== DEFAULT_GROUP_KEY"
                type="danger"
                link
                size="small"
                @click="handleDelete(row)"
              >
                <el-icon><Delete /></el-icon>
                {{ t('common.delete') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-area">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('instanceGroup.editGroup') : t('instanceGroup.addGroup')"
      width="500px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        class="group-form"
      >
        <el-form-item :label="t('instanceGroup.groupKey')" prop="groupKey">
          <el-input
            v-model="formData.groupKey"
            :placeholder="t('instanceGroup.groupKeyPlaceholder')"
            :disabled="isEdit"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item :label="t('instanceGroup.groupName')" prop="groupName">
          <el-input
            v-model="formData.groupName"
            :placeholder="t('instanceGroup.groupNamePlaceholder')"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item :label="t('common.remark')" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            :placeholder="t('instanceGroup.remarkPlaceholder')"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Search,
  RefreshLeft,
  Plus,
  Edit,
  Delete,
} from '@element-plus/icons-vue'
import {
  queryInstanceGroupList,
  addInstanceGroup,
  updateInstanceGroup,
  deleteInstanceGroup,
  type InstanceGroup,
} from '@/api/instanceGroup'

defineOptions({ name: 'InstanceGroupManagement' })

const { t } = useI18n()

// 默认分组标识（不可删除、不可禁用）
const DEFAULT_GROUP_KEY = 'default'

// ==================== 数据状态 ====================

const loading = ref(false)
const submitting = ref(false)
const groupList = ref<InstanceGroup[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const searchForm = reactive({
  groupKey: '',
  groupName: '',
  status: undefined as number | undefined,
})

// ==================== 弹窗状态 ====================

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  groupId: 0,
  groupKey: '',
  groupName: '',
  remark: '',
})

// ==================== 表单校验规则 ====================

const formRules = computed<FormRules>(() => ({
  groupKey: [
    { required: true, message: t('instanceGroup.groupKeyRequired'), trigger: 'blur' },
    { min: 1, max: 50, message: t('validation.length', { min: 1, max: 50 }), trigger: 'blur' },
  ],
  groupName: [
    { required: true, message: t('instanceGroup.groupNameRequired'), trigger: 'blur' },
    { min: 1, max: 100, message: t('validation.length', { min: 1, max: 100 }), trigger: 'blur' },
  ],
}))

// ==================== 辅助方法 ====================

const formatTime = (time: string): string => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

// ==================== 数据加载 ====================

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm,
    }
    const result = await queryInstanceGroupList(params)
    groupList.value = result?.rows || []
    pagination.total = result?.total || 0
  } catch (error) {
    console.error('Load data error:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ==================== 搜索 ====================

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.groupKey = ''
  searchForm.groupName = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  loadData()
}

// ==================== 分页 ====================

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadData()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  loadData()
}

// ==================== 新增 ====================

const handleAdd = () => {
  isEdit.value = false
  formData.groupId = 0
  formData.groupKey = ''
  formData.groupName = ''
  formData.remark = ''
  dialogVisible.value = true
}

// ==================== 编辑 ====================

const handleEdit = (row: InstanceGroup) => {
  isEdit.value = true
  formData.groupId = row.groupId
  formData.groupKey = row.groupKey
  formData.groupName = row.groupName
  formData.remark = row.remark || ''
  dialogVisible.value = true
}

// ==================== 删除 ====================

const handleDelete = (row: InstanceGroup) => {
  // 默认分组不可删除
  if (row.groupKey === DEFAULT_GROUP_KEY) {
    ElMessage.warning(t('instanceGroup.hasInstances'))
    return
  }

  ElMessageBox.confirm(t('instanceGroup.deleteConfirm'), t('message.tips'), { type: 'warning' })
    .then(async () => {
      try {
        await deleteInstanceGroup({ groupId: row.groupId })
        ElMessage.success(t('common.success'))
        loadData()
      } catch (error) {
        console.error('Delete group error:', error)
      }
    })
}

// ==================== 状态切换 ====================

const handleStatusChange = async (row: InstanceGroup) => {
  // 默认分组不可禁用
  if (row.groupKey === DEFAULT_GROUP_KEY && row.status === 0) {
    row.status = 1 // 恢复状态
    ElMessage.warning(t('instanceGroup.defaultGroupCannotDisable'))
    return
  }

  try {
    await updateInstanceGroup({
      groupId: row.groupId,
      groupKey: row.groupKey,
      groupName: row.groupName,
      status: row.status,
    })
    ElMessage.success(t('common.success'))
  } catch (error) {
    console.error('Update status error:', error)
    // 恢复原状态
    row.status = row.status === 0 ? 1 : 0
  }
}

// ==================== 弹窗关闭 ====================

const handleDialogClosed = () => {
  formRef.value?.resetFields()
}

// ==================== 提交表单 ====================

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      // 编辑
      await updateInstanceGroup({
        groupId: formData.groupId,
        groupName: formData.groupName,
        remark: formData.remark,
      })
    } else {
      // 新增
      await addInstanceGroup({
        groupKey: formData.groupKey,
        groupName: formData.groupName,
        remark: formData.remark,
      })
    }
    ElMessage.success(t('common.success'))
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('Submit error:', error)
  } finally {
    submitting.value = false
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.instance-group-page {
  height: 100%;
  display: flex;
  flex-direction: column;

  .page-card {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      overflow: auto;
      display: flex;
      flex-direction: column;
    }
  }

  .search-area {
    flex-shrink: 0;
    padding-bottom: 8px;

    .search-form {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;

      .right-actions {
        margin-left: auto;
      }
    }
  }

  .group-table {
    flex: 1;
    min-height: 0;

    .group-key {
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 13px;
    }
  }

  .pagination-area {
    flex-shrink: 0;
    padding-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .empty-text {
    color: var(--el-text-color-placeholder);
  }

  .operation-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 8px;

    :deep(.el-button) {
      margin: 0;
    }
  }

  .group-form {
    margin-top: 0;
  }
}
</style>
