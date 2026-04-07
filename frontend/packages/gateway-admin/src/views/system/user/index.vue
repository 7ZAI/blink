<template>
  <!-- 用户管理页面 -->
  <div class="user-management table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card shrink-0" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('system.user.loginName')">
          <el-input v-model.trim="searchForm.loginName" :placeholder="t('common.pleaseInput') + t('system.user.loginName')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('system.user.username')">
          <el-input v-model.trim="searchForm.username" :placeholder="t('common.pleaseInput') + t('system.user.username')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('system.user.sex')">
          <el-select v-model="searchForm.sex" :placeholder="t('common.pleaseSelect')" clearable style="width: 100px">
            <el-option :label="t('system.user.male')" :value="1" />
            <el-option :label="t('system.user.female')" :value="2" />
            <el-option :label="t('system.user.unknown')" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('system.user.createTime')">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            :range-separator="t('settings.to')"
            :start-placeholder="t('settings.startDate')"
            :end-placeholder="t('settings.endDate')"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
            style="width: 200px"
          />
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
    <el-card class="table-card flex-1 flex flex-col overflow-hidden" shadow="never">
      <template #header>
        <div class="table-header">
          <AuthButton :perm="ButtonPerms.User.Add" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>{{ t('common.add') }}
          </AuthButton>
          <AuthButton :perm="ButtonPerms.User.AssignRole" type="primary" :disabled="selectedRows.length === 0" @click="handleAssignRole">
            <el-icon><UserFilled /></el-icon>{{ t('system.user.assignRole') }}
          </AuthButton>
          <AuthButton :perm="ButtonPerms.User.ResetPwd" type="warning" :disabled="selectedRows.length !== 1" @click="handleResetPasswordBatch">
            <el-icon><Key /></el-icon>{{ t('system.user.resetPassword') }}
          </AuthButton>
          <AuthButton :perm="ButtonPerms.User.Delete" type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>{{ t('common.batchDelete') }}
          </AuthButton>
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
        >
          <el-table-column type="selection" width="55" :selectable="checkSelectable" />
          <el-table-column prop="loginName" :label="t('system.user.loginName')" min-width="120">
            <template #default="{ row }">
              {{ row.loginName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="username" :label="t('system.user.username')" min-width="120">
            <template #default="{ row }">
              {{ row.username || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="sex" :label="t('system.user.sex')" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.sex === 1" type="primary">{{ t('system.user.male') }}</el-tag>
              <el-tag v-else-if="row.sex === 2" type="danger">{{ t('system.user.female') }}</el-tag>
              <el-tag v-else type="info">{{ t('system.user.unknown') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="phone" :label="t('system.user.phone')" min-width="120">
            <template #default="{ row }">
              {{ row.phone || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="email" :label="t('system.user.email')" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.email || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="locked" :label="t('system.user.status')" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.locked === 0" type="success">{{ t('system.user.normal') }}</el-tag>
              <el-tag v-else-if="row.locked === 1" type="danger">{{ t('system.user.adminLocked') }}</el-tag>
              <el-tag v-else type="warning">{{ t('system.user.passwordLocked') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginTime" :label="t('system.user.lastLoginTime')" min-width="160">
            <template #default="{ row }">
              {{ row.lastLoginTime || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('system.user.createTime')" min-width="160">
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
      :title="t('system.user.resetPassword')"
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
        <el-form-item :label="t('system.user.loginName')">
          <el-input v-model="resetPasswordForm.loginName" disabled />
        </el-form-item>
        <el-form-item :label="t('system.user.newPassword')" prop="newPassword">
          <el-input
            v-model="resetPasswordForm.newPassword"
            type="password"
            show-password
            :placeholder="t('common.pleaseInput') + t('system.user.newPassword')"
          />
        </el-form-item>
        <el-form-item :label="t('system.user.confirmPassword')" prop="confirmPassword">
          <el-input
            v-model="resetPasswordForm.confirmPassword"
            type="password"
            show-password
            :placeholder="t('common.pleaseInput') + t('system.user.confirmPassword')"
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
  UserFilled,
  Key,
} from '@element-plus/icons-vue'
import { getUserList, deleteUser, lockUser, resetPassword, type UserInfo, type QueryUserParams } from '@/api/user'
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
    callback(new Error(t('validation.required', { field: t('system.user.confirmPassword') })))
  } else if (value !== resetPasswordForm.newPassword) {
    callback(new Error(t('validation.passwordNotMatch')))
  } else {
    callback()
  }
}

const resetPasswordRules = reactive<FormRules>({
  newPassword: [
    { required: true, message: t('common.pleaseInput') + t('system.user.newPassword'), trigger: 'blur' },
    { min: 6, max: 20, message: t('validation.passwordLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateResetConfirmPassword, trigger: 'blur' }
  ]
})

const handleDateChange = (val: [string, string] | null) => {
  if (val) {
    searchForm.startDate = val[0]
    searchForm.endDate = val[1]
  } else {
    searchForm.startDate = ''
    searchForm.endDate = ''
  }
}

const fetchUserList = async () => {
  startTransition()
  loading.value = true
  try {
    const params: QueryUserParams = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm,
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
    await ElMessageBox.confirm(t('system.user.deleteConfirm'), t('message.tips'), {
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
    ElMessage.warning(t('system.user.selectUser'))
    return
  }
  try {
    await ElMessageBox.confirm(
      t('system.user.batchDeleteConfirm', { count: selectedRows.value.length }),
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
      t('system.user.lockConfirm', { action, name: row.loginName }),
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
    ElMessage.warning(t('system.user.selectUser'))
    return
  }
  assignRoleVisible.value = true
}

const handleResetPasswordBatch = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning(t('system.user.selectUserResetPassword'))
    return
  }
  const row = selectedRows.value[0]
  if (!row) return

  // 超级管理员不允许重置密码
  if (row.superFlag === 1) {
    ElMessage.warning(t('system.user.cannotResetSuperAdminPassword'))
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
  fetchUserList()
})
</script>

<style scoped lang="scss">
/* 用户管理页面 - 继承全局 table-page-container 样式 */
</style>