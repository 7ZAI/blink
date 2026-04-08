<template>
  <div class="role-management table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('system.role.roleName')">
          <el-input v-model.trim="searchForm.roleName" :placeholder="t('common.pleaseInput')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('system.role.roleCode')">
          <el-input v-model.trim="searchForm.roleCode" :placeholder="t('common.pleaseInput')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="searchForm.status" :placeholder="t('common.pleaseSelect')" clearable style="width: 100px">
            <el-option :label="t('system.role.statusEnable')" :value="0" />
            <el-option :label="t('system.role.statusDisable')" :value="1" />
          </el-select>
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
      <template #header>
        <div class="table-header">
          <AuthButton :has-permission="() => checkPermission(ButtonPerms.Role.Add)" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>{{ t('common.add') }}
          </AuthButton>
          <AuthButton v-if="selectedRoles.length > 0" :has-permission="() => checkPermission(ButtonPerms.Role.Delete)" type="danger" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>{{ t('common.batchDelete') }}({{ selectedRoles.length }})
          </AuthButton>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table
          ref="tableRef"
          v-loading="loading"
          :data="roleList"
          height="100%"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" :selectable="(row: RoleInfo) => row.roleId !== 1" />
          <el-table-column prop="roleId" label="ID" width="80" align="center" />
          <el-table-column prop="roleName" :label="t('system.role.roleName')" min-width="120" />
          <el-table-column prop="roleEnName" :label="t('system.role.roleEnName')" min-width="120" />
          <el-table-column prop="roleCode" :label="t('system.role.roleCode')" min-width="120" />
          <el-table-column prop="roleType" :label="t('system.role.roleType')" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.roleType === 1" type="primary">{{ t('system.role.typeSystem') }}</el-tag>
              <el-tag v-else type="info">{{ t('system.role.typeCustom') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('common.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="success">{{ t('system.role.statusEnable') }}</el-tag>
              <el-tag v-else type="danger">{{ t('system.role.statusDisable') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('system.role.createTime')" min-width="160" />
          <el-table-column :label="t('common.operation')" min-width="400" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton v-if="row.roleId !== 1" :has-permission="() => checkPermission(ButtonPerms.Role.Edit)" type="primary" link size="small" @click="handleEdit(row)">
                  <el-icon><Edit /></el-icon>{{ t('common.edit') }}
                </AuthButton>
                <AuthButton v-if="row.roleId !== 1" :has-permission="() => checkPermission(ButtonPerms.Role.AssignPerm)" type="warning" link size="small" @click="handleAssignPermission(row)">
                  <el-icon><Key /></el-icon>{{ t('system.role.assignDataPermission') }}
                </AuthButton>
                <AuthButton v-if="row.roleId !== 1" :has-permission="() => checkPermission(ButtonPerms.Role.AssignMenu)" type="success" link size="small" @click="handleAssignMenu(row)">
                  <el-icon><Menu /></el-icon>{{ t('system.role.assignMenu') }}
                </AuthButton>
                <el-button v-if="row.roleId !== 1" type="primary" link size="small" @click="handleAssignUser(row)">
                  <el-icon><User /></el-icon>{{ t('system.role.assignUser') }}
                </el-button>
                <AuthButton :has-permission="() => checkPermission(ButtonPerms.Role.Detail)" type="info" link size="small" @click="handleDetail(row)">
                  <el-icon><View /></el-icon>{{ t('common.detail') }}
                </AuthButton>
                <AuthButton v-if="row.roleId !== 1" :has-permission="() => checkPermission(ButtonPerms.Role.Delete)" type="danger" link size="small" @click="handleDelete(row)">
                  <el-icon><Delete /></el-icon>{{ t('common.delete') }}
                </AuthButton>
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
          @size-change="fetchRoleList"
          @current-change="fetchRoleList"
        />
      </div>
    </el-card>

    <RoleFormDialog
      v-model="formDialogVisible"
      :type="formType"
      :data="currentRole"
      @success="fetchRoleList"
    />

    <AssignPermissionDialog
      v-model="permissionDialogVisible"
      :role="currentRole"
      @success="fetchRoleList"
    />

    <AssignMenuDialog
      v-model="menuDialogVisible"
      :role-id="currentRole?.roleId ?? null"
      @success="fetchRoleList"
    />

    <AssignUserDialog
      v-model="userDialogVisible"
      :role="currentRole"
      @success="fetchRoleList"
    />

    <RoleDetailDialog
      v-model="detailDialogVisible"
      :role="currentRole"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Key, Menu, View, User } from '@element-plus/icons-vue'
import { getRoleList, deleteRole, type RoleInfo } from '@/api/role'
import { ButtonPerms, usePermission } from '@/composables/usePermission'

defineOptions({
  name: 'SystemRole',
})
import RoleFormDialog from './components/RoleFormDialog.vue'

const { hasPermission: checkPermission } = usePermission()
import AssignPermissionDialog from './components/AssignPermissionDialog.vue'
import AssignMenuDialog from './components/AssignMenuDialog.vue'
import AssignUserDialog from './components/AssignUserDialog.vue'
import RoleDetailDialog from './components/RoleDetailDialog.vue'

const { t } = useI18n()

const loading = ref(false)
const roleList = ref<RoleInfo[]>([])
const tableRef = ref()
const selectedRoles = ref<RoleInfo[]>([])
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')
const currentRole = ref<RoleInfo | null>(null)
const permissionDialogVisible = ref(false)
const menuDialogVisible = ref(false)
const userDialogVisible = ref(false)
const detailDialogVisible = ref(false)

const searchForm = reactive({
  roleName: '',
  roleCode: '',
  status: undefined as number | undefined,
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const fetchRoleList = async () => {
  loading.value = true
  try {
    const res = await getRoleList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm,
    })
    roleList.value = res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
    roleList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchRoleList()
}

const handleReset = () => {
  searchForm.roleName = ''
  searchForm.roleCode = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  fetchRoleList()
}

const handleAdd = () => {
  formType.value = 'add'
  currentRole.value = null
  formDialogVisible.value = true
}

const handleEdit = (row: RoleInfo) => {
  formType.value = 'edit'
  currentRole.value = row
  formDialogVisible.value = true
}

const handleDelete = async (row: RoleInfo) => {
  try {
    await ElMessageBox.confirm(t('system.role.deleteConfirm'), t('message.tips'), {
      type: 'warning',
    })
    await deleteRole({
      deleteId: row.roleId,
      batchDelete: false
    })
    ElMessage.success(t('message.deleteSuccess'))
    fetchRoleList()
  } catch {
    // 取消删除
  }
}

const handleSelectionChange = (selection: RoleInfo[]) => {
  selectedRoles.value = selection
}

const handleBatchDelete = async () => {
  if (selectedRoles.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      t('system.role.batchDeleteConfirm', { count: selectedRoles.value.length }),
      t('message.tips'),
      { type: 'warning' }
    )
    // 批量删除：发送 idList 进行一次性删除
    const deleteIds = selectedRoles.value.map(row => row.roleId)
    await deleteRole({ idList: deleteIds, batchDelete: true })
    ElMessage.success(t('message.deleteSuccess'))
    selectedRoles.value = []
    tableRef.value?.clearSelection()
    fetchRoleList()
  } catch {
    // 取消删除
  }
}

const handleAssignPermission = (row: RoleInfo) => {
  currentRole.value = row
  permissionDialogVisible.value = true
}

const handleAssignMenu = (row: RoleInfo) => {
  currentRole.value = row
  menuDialogVisible.value = true
}

const handleAssignUser = (row: RoleInfo) => {
  currentRole.value = row
  userDialogVisible.value = true
}

const handleDetail = (row: RoleInfo) => {
  currentRole.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  fetchRoleList()
})
</script>

<style scoped lang="scss">
/* 角色管理页面 - 继承全局 table-page-container 样式 */
</style>