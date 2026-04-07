<template>
  <!-- 用户管理页面 - 左右布局 -->
  <div class="user-management table-page-container">
    <!-- 左侧面板 - 组织树 -->
    <div class="left-panel shrink-0 transition-all duration-300" :class="{ 'collapsed w-12': isLeftPanelCollapsed, 'w-[260px]': !isLeftPanelCollapsed }">
      <el-card shadow="never" class="h-full flex flex-col">
        <template #header>
          <div class="card-header flex justify-between items-center px-4 py-3 border-b">
            <span v-if="!isLeftPanelCollapsed" class="font-medium text-text-primary">{{ t('group.title') }}</span>
            <div class="header-actions flex items-center gap-1">
              <el-button v-if="!isLeftPanelCollapsed" type="primary" link @click="handleRefreshGroup" :title="t('group.refresh')">
                <el-icon><Refresh /></el-icon>
              </el-button>
              <el-button type="primary" link @click="toggleLeftPanel">
                <el-icon>
                  <ArrowLeft v-if="!isLeftPanelCollapsed" />
                  <ArrowRight v-else />
                </el-icon>
              </el-button>
            </div>
          </div>
        </template>
        <!-- 组织树 -->
        <div
          v-show="!isLeftPanelCollapsed"
          class="tree-wrapper flex-1 overflow-auto p-3 min-h-[200px]"
        >
          <el-tree
            ref="groupTreeRef"
            :data="groupTreeData"
            :props="defaultProps"
            :highlight-current="true"
            :expand-on-click-node="false"
            v-loading="groupTreeLoading"
            node-key="groupId"
            default-expand-all
            @node-click="handleGroupClick"
          />
        </div>
      </el-card>
    </div>

    <!-- 右侧面板 - 用户列表 -->
    <div class="right-panel flex-1 min-w-0 flex flex-col gap-4">
      <!-- 搜索卡片 -->
      <el-card class="search-card shrink-0" shadow="never">
        <el-form :model="searchForm" inline class="search-form">
          <!-- 操作按钮行 -->
          <div class="search-buttons mb-3">
            <el-button type="info" @click="handleSearch">
              <el-icon><Search /></el-icon>{{ t('common.search') }}
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>{{ t('common.reset') }}
            </el-button>
          </div>
          <!-- 查询条件行 -->
          <div class="search-conditions grid grid-cols-4 gap-x-4 gap-y-3">
            <el-form-item :label="t('user.loginName')" class="mb-0">
              <el-input v-model.trim="searchForm.loginName" :placeholder="t('common.pleaseInput') + t('user.loginName')" clearable />
            </el-form-item>
            <el-form-item :label="t('user.username')" class="mb-0">
              <el-input v-model.trim="searchForm.username" :placeholder="t('common.pleaseInput') + t('user.username')" clearable />
            </el-form-item>
            <el-form-item :label="t('user.sex')" class="mb-0">
              <el-select v-model="searchForm.sex" :placeholder="t('common.pleaseSelect')" clearable class="w-full">
                <el-option :label="t('user.male')" :value="1" />
                <el-option :label="t('user.female')" :value="2" />
                <el-option :label="t('user.unknown')" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('user.createTime')" class="mb-0">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                :range-separator="t('settings.to')"
                :start-placeholder="t('settings.startDate')"
                :end-placeholder="t('settings.endDate')"
                value-format="YYYY-MM-DD"
                @change="handleDateChange"
                class="w-full"
              />
            </el-form-item>
          </div>
        </el-form>
      </el-card>

      <!-- 表格卡片 -->
      <el-card class="table-card flex-1 flex flex-col overflow-hidden" shadow="never">
        <template #header>
          <div class="table-header">
            <div class="header-left flex items-center gap-4">
              <AuthButton :perm="ButtonPerms.User.Add" type="primary" @click="handleAdd">
                <el-icon><Plus /></el-icon>{{ t('common.add') }}
              </AuthButton>
              <AuthButton :perm="ButtonPerms.User.AssignRole" type="primary" :disabled="selectedRows.length === 0" @click="handleAssignRole">
                <el-icon><UserFilled /></el-icon>{{ t('user.assignRole') }}
              </AuthButton>
              <AuthButton :perm="ButtonPerms.User.ResetPwd" type="warning" :disabled="selectedRows.length !== 1" @click="handleResetPasswordBatch">
                <el-icon><Key /></el-icon>{{ t('user.resetPassword') }}
              </AuthButton>
              <AuthButton :perm="ButtonPerms.User.Delete" type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
                <el-icon><Delete /></el-icon>{{ t('common.batchDelete') }}
              </AuthButton>
            </div>
          </div>
        </template>

        <!-- 表格区域 -->
        <div
          class="table-wrapper flex-1 min-h-[200px] data-transition-wrapper"
          :class="transitionClass"
        >
          <el-table
            v-loading="loading"
            :data="userList"
            height="100%"
            @selection-change="handleSelectionChange"
            stripe
            border
          >
            <el-table-column type="selection" width="55" :selectable="checkSelectable" />
            <el-table-column prop="loginName" :label="t('user.loginName')" min-width="120">
              <template #default="{ row }">
                {{ row.loginName || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="username" :label="t('user.username')" min-width="120">
              <template #default="{ row }">
                {{ row.username || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="sex" :label="t('user.sex')" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.sex === 1" type="primary">{{ t('user.male') }}</el-tag>
                <el-tag v-else-if="row.sex === 2" type="danger">{{ t('user.female') }}</el-tag>
                <el-tag v-else type="info">{{ t('user.unknown') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" :label="t('user.phone')" min-width="120">
              <template #default="{ row }">
                {{ row.phone || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="email" :label="t('user.email')" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.email || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="groupName" :label="t('user.group')" min-width="120">
              <template #default="{ row }">
                {{ row.group?.groupName || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="locked" :label="t('user.status')" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.locked === 0" type="success">{{ t('user.normal') }}</el-tag>
                <el-tag v-else-if="row.locked === 1" type="danger">{{ t('user.adminLocked') }}</el-tag>
                <el-tag v-else type="warning">{{ t('user.passwordLocked') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastLoginTime" :label="t('user.lastLoginTime')" min-width="160">
              <template #default="{ row }">
                {{ row.lastLoginTime || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" :label="t('user.createTime')" min-width="160">
              <template #default="{ row }">
                {{ row.createTime || '-' }}
              </template>
            </el-table-column>
            <el-table-column :label="t('common.operation')" width="300" fixed="right">
              <template #default="{ row }">
                <!-- 非超级管理员看不到超级管理员的操作按钮 -->
                <template v-if="userStore.isSuperAdmin || (row.superFlag !== 1 && row.superFlag !== '1')">
                  <div class="operation-buttons">
                    <AuthButton :perm="ButtonPerms.User.Edit" type="primary" link size="small" @click="handleEdit(row)">
                      <el-icon><Edit /></el-icon>{{ t('common.edit') }}
                    </AuthButton>
                    <AuthButton :perm="ButtonPerms.User.Detail" type="info" link size="small" @click="handleDetail(row)">
                      <el-icon><View /></el-icon>{{ t('common.detail') }}
                    </AuthButton>
                    <AuthButton
                      v-if="row.locked === 0"
                      :perm="ButtonPerms.User.Lock"
                      type="warning"
                      link
                      size="small"
                      @click="handleLock(row, 1)"
                    >
                      <el-icon><Lock /></el-icon>{{ t('common.lock') }}
                    </AuthButton>
                    <AuthButton
                      v-else
                      :perm="ButtonPerms.User.Lock"
                      type="success"
                      link
                      size="small"
                      @click="handleLock(row, 0)"
                    >
                      <el-icon><Unlock /></el-icon>{{ t('common.unlock') }}
                    </AuthButton>
                    <AuthButton :perm="ButtonPerms.User.Delete" type="danger" link size="small" @click="handleDelete(row)">
                      <el-icon><Delete /></el-icon>{{ t('common.delete') }}
                    </AuthButton>
                  </div>
                </template>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          >
            <template #total="{ total }">
              {{ t('pagination.total') }} {{ total }} {{ t('pagination.items') }}
            </template>
          </el-pagination>
        </div>
      </el-card>

      <!-- 弹窗组件 -->
      <UserFormDialog
        v-model="dialogVisible"
        :type="dialogType"
        :data="currentRow"
        @success="handleSearch"
      />

      <UserDetailDialog
        v-model="detailVisible"
        :login-name="currentLoginName"
      />

      <AssignRoleDialog
        v-model="assignRoleVisible"
        :users="selectedRows"
        @success="fetchUserList"
      />

      <el-dialog
        v-model="resetPasswordVisible"
        :title="t('user.resetPassword')"
        width="450px"
        :close-on-click-modal="false"
        :lock-scroll="false"
        @closed="handleResetPasswordClose"
      >
        <el-form
          ref="resetPasswordFormRef"
          :model="resetPasswordForm"
          :rules="resetPasswordRules"
          label-width="120px"
        >
          <el-form-item :label="t('user.loginName')">
            <el-input v-model="resetPasswordForm.loginName" disabled />
          </el-form-item>
          <el-form-item :label="t('user.newPassword')" prop="newPassword">
            <el-input
              v-model="resetPasswordForm.newPassword"
              type="password"
              show-password
              :placeholder="t('common.pleaseInput') + t('user.newPassword')"
            />
          </el-form-item>
          <el-form-item :label="t('user.confirmPassword')" prop="confirmPassword">
            <el-input
              v-model="resetPasswordForm.confirmPassword"
              type="password"
              show-password
              :placeholder="t('common.pleaseInput') + t('user.confirmPassword')"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="resetPasswordVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="resetPasswordSubmitting" @click="handleResetPasswordSubmit">
            {{ t('common.confirm') }}
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Delete,
  Edit,
  View,
  Lock,
  Unlock,
  ArrowLeft,
  ArrowRight,
  UserFilled,
  Key,
} from '@element-plus/icons-vue'
import { getUserList, deleteUser, lockUser, resetPassword, type UserInfo, type QueryUserParams } from '@/api/user'
import { getGroupTree, type GroupInfo } from '@/api/group'
import { useUserStore } from '@/stores/user'
import { useTransition } from '@/composables/useDataTransition'
import { useSubmitGuard } from '@/composables/useSubmitGuard'
import { ButtonPerms } from '@/composables/usePermission'
import UserFormDialog from './components/UserFormDialog.vue'
import UserDetailDialog from './components/UserDetailDialog.vue'
import AssignRoleDialog from './components/AssignRoleDialog.vue'
import type { FormInstance, FormRules } from 'element-plus'

defineOptions({
  name: 'SystemUserList',
})

const { t } = useI18n()
const userStore = useUserStore()
const { transitionClass, startTransition, finishTransition } = useTransition()

const isLeftPanelCollapsed = ref(false)

const toggleLeftPanel = () => {
  isLeftPanelCollapsed.value = !isLeftPanelCollapsed.value
}

const searchForm = reactive({
  loginName: '',
  username: '',
  sex: undefined as number | undefined,
  startDate: '',
  endDate: '',
})

const dateRange = ref<[string, string] | null>(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const loading = ref(false)
const { isSubmitting: isDeleting, submitGuard: deleteGuard } = useSubmitGuard()
const userList = ref<UserInfo[]>([])
const selectedRows = ref<UserInfo[]>([])

const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const currentRow = ref<UserInfo | null>(null)
const detailVisible = ref(false)
const currentLoginName = ref('')
const assignRoleVisible = ref(false)

const resetPasswordVisible = ref(false)
const resetPasswordFormRef = ref<FormInstance>()
const resetPasswordSubmitting = ref(false)
const resetPasswordForm = reactive({
  userId: 0,
  loginName: '',
  newPassword: '',
  confirmPassword: ''
})

const validateResetConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value === '') {
    callback(new Error(t('validation.required', { field: t('user.confirmPassword') })))
  } else if (value !== resetPasswordForm.newPassword) {
    callback(new Error(t('validation.passwordNotMatch')))
  } else {
    callback()
  }
}

const resetPasswordRules = reactive<FormRules>({
  newPassword: [
    { required: true, message: t('common.pleaseInput') + t('user.newPassword'), trigger: 'blur' },
    { min: 6, max: 20, message: t('validation.passwordLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateResetConfirmPassword, trigger: 'blur' }
  ]
})

const groupTreeData = ref<GroupInfo[]>([])
const groupTreeLoading = ref(false)
const currentGroupId = ref<number | null>(null)

const defaultProps = {
  children: 'children',
  label: 'groupName',
  value: 'groupId',
}

const handleDateChange = (val: [string, string] | null) => {
  if (val) {
    searchForm.startDate = val[0]
    searchForm.endDate = val[1]
  } else {
    searchForm.startDate = ''
    searchForm.endDate = ''
  }
}

const fetchGroupTree = async () => {
  groupTreeLoading.value = true
  try {
    groupTreeData.value = await getGroupTree()
    // 如果组织树为空，使用当前用户的部门作为根节点
    if (groupTreeData.value.length === 0 && userStore.userInfo?.group) {
      const userGroup = userStore.userInfo.group
      groupTreeData.value = [{
        groupId: userGroup.groupId,
        groupName: userGroup.groupName,
        groupParentId: 0,
        groupNo: '',
        groupEnName: '',
        groupLevel: 1,
        isLeaf: 1,
        groupLeader: '',
        groupAddress: '',
        phone: '',
        createTime: '',
        createBy: '',
        updateTime: '',
        updateBy: '',
        children: []
      }]
    }
  } catch (error) {
    console.error('[UserManagement] Failed to fetch group tree:', error)
    // 如果查询失败，尝试使用当前用户的部门作为根节点
    if (userStore.userInfo?.group) {
      const userGroup = userStore.userInfo.group
      groupTreeData.value = [{
        groupId: userGroup.groupId,
        groupName: userGroup.groupName,
        groupParentId: 0,
        groupNo: '',
        groupEnName: '',
        groupLevel: 1,
        isLeaf: 1,
        groupLeader: '',
        groupAddress: '',
        phone: '',
        createTime: '',
        createBy: '',
        updateTime: '',
        updateBy: '',
        children: []
      }]
    } else {
      groupTreeData.value = []
    }
  } finally {
    groupTreeLoading.value = false
  }
}

const handleGroupClick = (data: GroupInfo) => {
  currentGroupId.value = data.groupId
  pagination.pageNum = 1
  fetchUserList()
}

const handleRefreshGroup = () => {
  fetchGroupTree()
}

const fetchUserList = async () => {
  startTransition()
  loading.value = true
  try {
    const params: QueryUserParams = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm,
      groupId: currentGroupId.value || undefined,
    }
    const res = await getUserList(params)
    userList.value = res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error('[UserManagement] Failed to fetch user list:', error)
  } finally {
    loading.value = false
    finishTransition()
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchUserList()
}

const handleReset = () => {
  searchForm.loginName = ''
  searchForm.username = ''
  searchForm.sex = undefined
  searchForm.startDate = ''
  searchForm.endDate = ''
  dateRange.value = null
  currentGroupId.value = null
  handleSearch()
}

const handleSelectionChange = (rows: UserInfo[]) => {
  selectedRows.value = rows
}

const handleAdd = () => {
  dialogType.value = 'add'
  currentRow.value = null
  dialogVisible.value = true
}

const handleEdit = (row: UserInfo) => {
  dialogType.value = 'edit'
  currentRow.value = row
  dialogVisible.value = true
}

const handleDetail = (row: UserInfo) => {
  currentLoginName.value = row.loginName
  detailVisible.value = true
}

const handleDelete = async (row: UserInfo) => {
  try {
    await ElMessageBox.confirm(t('user.deleteConfirm'), t('message.tips'), {
      type: 'warning',
    })
    await deleteGuard(async () => {
      await deleteUser({ userId: row.userId, batchDelete: false })
      ElMessage.success(t('message.deleteSuccess'))
      fetchUserList()
    })
  } catch (error) {
    // 用户取消删除，不做处理
    if (error !== 'cancel') {
      console.error('[UserManagement] Failed to delete user:', error)
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning(t('user.selectUser'))
    return
  }
  try {
    await ElMessageBox.confirm(
      t('user.batchDeleteConfirm', { count: selectedRows.value.length }),
      t('message.tips'),
      { type: 'warning' }
    )
    const userIds = selectedRows.value.map((row) => row.userId)
    await deleteGuard(async () => {
      await deleteUser({ userIdList: userIds, batchDelete: true })
      ElMessage.success(t('message.deleteSuccess'))
      fetchUserList()
    })
  } catch (error) {
    // 用户取消删除
    if (error !== 'cancel') {
      console.error('[UserManagement] Failed to batch delete users:', error)
    }
  }
}

const handleLock = async (row: UserInfo, locked: number) => {
  const action = locked === 1 ? t('common.lock') : t('common.unlock')
  try {
    await ElMessageBox.confirm(
      t('user.lockConfirm', { action, name: row.loginName }),
      t('message.tips'),
      { type: 'warning' }
    )
    await lockUser({ userId: row.userId, locked })
    ElMessage.success(t('message.success'))
    fetchUserList()
  } catch (error) {
    // 用户取消操作
    if (error !== 'cancel') {
      console.error('[UserManagement] Failed to lock/unlock user:', error)
    }
  }
}

const handleAssignRole = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning(t('user.selectUser'))
    return
  }
  assignRoleVisible.value = true
}

const handleResetPasswordBatch = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning(t('user.selectUserResetPassword'))
    return
  }
  const row = selectedRows.value[0]
  if (!row) return

  // 超级管理员不允许重置密码
  if (row.superFlag === 1) {
    ElMessage.warning(t('user.cannotResetSuperAdminPassword'))
    return
  }

  resetPasswordForm.userId = row.userId
  resetPasswordForm.loginName = row.loginName
  resetPasswordForm.newPassword = ''
  resetPasswordForm.confirmPassword = ''
  resetPasswordVisible.value = true
}

const handleResetPasswordClose = () => {
  resetPasswordForm.userId = 0
  resetPasswordForm.loginName = ''
  resetPasswordForm.newPassword = ''
  resetPasswordForm.confirmPassword = ''
  resetPasswordFormRef.value?.resetFields()
}

const handleResetPasswordSubmit = async () => {
  if (!resetPasswordFormRef.value) return

  await resetPasswordFormRef.value.validate(async (valid) => {
    if (!valid) return

    resetPasswordSubmitting.value = true
    try {
      await resetPassword({
        userId: resetPasswordForm.userId,
        newPassword: resetPasswordForm.newPassword
      })
      ElMessage.success(t('message.passwordResetSuccess'))
      resetPasswordVisible.value = false
    } catch {
      // 错误已在请求拦截器中处理
    } finally {
      resetPasswordSubmitting.value = false
    }
  })
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  fetchUserList()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  fetchUserList()
}

// 判断用户是否可选（超级管理员不可选）
const checkSelectable = (row: UserInfo) => {
  return row.superFlag !== 1
}

onMounted(() => {
  fetchGroupTree()
  fetchUserList()
})
</script>

<style scoped lang="scss">
/* 用户管理页面 - 左右布局，继承全局 table-page-container 样式 */
.user-management {
  flex-direction: row;
  gap: 16px;

  /* 左侧面板 - 组织树 */
  .left-panel {
    width: 260px;
    flex-shrink: 0;
    transition: width 0.3s ease;
    display: flex;
    flex-direction: column;

    &.collapsed {
      width: 48px;
    }

    :deep(.el-card) {
      height: 100%;
      display: flex;
      flex-direction: column;
      background: var(--glass-bg);
      backdrop-filter: blur(var(--glass-blur));
      border: 1px solid var(--glass-border);

      .el-card__header {
        padding: 0;
      }

      .el-card__body {
        flex: 1;
        overflow: auto;
        padding: 12px;
      }
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color-light);
      background: linear-gradient(90deg, rgba(59, 130, 246, 0.05) 0%, transparent 100%);

      .header-actions {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }

    .tree-wrapper {
      flex: 1;
      min-height: 200px;
      overflow: auto;
      padding: 12px;
    }
  }

  /* 右侧面板 - 用户列表 */
  .right-panel {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  /* 搜索表单 */
  .search-form {
    .search-buttons {
      display: flex;
      gap: 8px;
    }

    .search-conditions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
    }
  }
}
</style>
