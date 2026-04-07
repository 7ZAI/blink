<template>
  <div class="group-management table-page-container">
    <el-card class="simple-table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-right">
            <AuthButton :perm="ButtonPerms.Group.Add" type="success" @click="handleAdd(null)">
              <el-icon><Plus /></el-icon>{{ t('common.add') }}
            </AuthButton>
            <el-button type="info" @click="fetchGroupTree">
              <el-icon><Refresh /></el-icon>{{ t('group.refresh') }}
            </el-button>
          </div>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="groupTreeData"
          height="100%"
          row-key="groupId"
          :tree-props="{ children: 'children' }"
          stripe
          border
          default-expand-all
        >
          <el-table-column prop="groupName" :label="t('group.groupName')" min-width="180" />
          <el-table-column prop="groupEnName" :label="t('group.groupEnName')" min-width="120" />
          <el-table-column prop="groupNo" :label="t('group.groupNo')" min-width="120" />
          <el-table-column prop="groupLeader" :label="t('group.groupLeader')" min-width="100" />
          <el-table-column prop="phone" :label="t('group.phone')" min-width="120" />
          <el-table-column prop="groupAddress" :label="t('group.groupAddress')" min-width="150" show-overflow-tooltip />
          <el-table-column prop="createTime" :label="t('group.createTime')" min-width="160" />
          <el-table-column :label="t('common.operation')" width="220" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton :perm="ButtonPerms.Group.Edit" type="primary" link size="small" @click="handleEdit(row)">
                  <el-icon><Edit /></el-icon>{{ t('common.edit') }}
                </AuthButton>
                <AuthButton :perm="ButtonPerms.Group.Add" type="success" link size="small" @click="handleAdd(row)">
                  <el-icon><Plus /></el-icon>{{ t('common.add') }}
                </AuthButton>
                <AuthButton :perm="ButtonPerms.Group.Delete" type="danger" link size="small" @click="handleDelete(row)">
                  <el-icon><Delete /></el-icon>{{ t('common.delete') }}
                </AuthButton>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <GroupFormDialog
      v-model="formDialogVisible"
      :type="formType"
      :data="currentGroup"
      :parent-group="parentGroup"
      @success="fetchGroupTree"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { getGroupTree, deleteGroup, type GroupInfo } from '@/api/group'
import { ButtonPerms } from '@/composables/usePermission'
import AuthButton from '@/components/AuthButton.vue'

defineOptions({
  name: 'SystemGroup',
})
import GroupFormDialog from './components/GroupFormDialog.vue'

const { t } = useI18n()

const loading = ref(false)
const groupTreeData = ref<GroupInfo[]>([])
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')
const currentGroup = ref<GroupInfo | null>(null)
const parentGroup = ref<GroupInfo | null>(null)

const fetchGroupTree = async () => {
  loading.value = true
  try {
    groupTreeData.value = await getGroupTree()
  } catch (error) {
    groupTreeData.value = []
    const message = error instanceof Error ? error.message : t('common.networkError')
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const handleAdd = (row: GroupInfo | null) => {
  formType.value = 'add'
  currentGroup.value = null
  parentGroup.value = row
  formDialogVisible.value = true
}

const handleEdit = (row: GroupInfo) => {
  formType.value = 'edit'
  currentGroup.value = row
  parentGroup.value = null
  formDialogVisible.value = true
}

const handleDelete = async (row: GroupInfo) => {
  try {
    await ElMessageBox.confirm(t('group.deleteConfirm'), t('message.tips'), {
      type: 'warning',
    })
    await deleteGroup({
      deleteId: row.groupId,
      batchDelete: false
    })
    ElMessage.success(t('message.deleteSuccess'))
    fetchGroupTree()
  } catch {
    // 取消删除
  }
}

onMounted(() => {
  fetchGroupTree()
})
</script>

<style scoped lang="scss">
/* 组织管理页面样式 - 继承全局 table-page-container 和 simple-table-card 样式 */
.group-management {
  /* 可添加页面特定的样式覆盖 */
}
</style>
