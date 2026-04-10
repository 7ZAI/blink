<template>
  <el-dialog
    :title="t('role.assignUser')"
    v-model="visible"
    width="900px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <div class="user-selector-content">
      <!-- 搜索区域 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('user.loginName')">
          <el-input
            v-model.trim="searchForm.loginName"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item :label="t('user.username')">
          <el-input
            v-model.trim="searchForm.username"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 已选用户展示 -->
      <div v-if="selectedUsers.length > 0" class="selected-users">
        <span class="label">{{ t('role.selectedUsers') }}:</span>
        <el-tag
          v-for="user in selectedUsers"
          :key="user.userId"
          closable
          @close="handleRemoveUser(user)"
        >
          {{ user.username || user.loginName }}
        </el-tag>
      </div>

      <!-- 用户列表 -->
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="userList"
        stripe
        border
        max-height="400px"
        row-key="userId"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" :reserve-selection="true" />
        <el-table-column prop="userId" label="ID" width="80" align="center" />
        <el-table-column prop="loginName" :label="t('user.loginName')" min-width="120" />
        <el-table-column prop="username" :label="t('user.username')" min-width="120" />
        <el-table-column prop="phone" :label="t('user.phone')" min-width="120" />
        <el-table-column prop="email" :label="t('user.email')" min-width="150" />
        <el-table-column :label="t('user.sex')" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.sex === 1">{{ t('user.male') }}</span>
            <span v-else-if="row.sex === 2">{{ t('user.female') }}</span>
            <span v-else>{{ t('user.unknown') }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchUserList"
        @current-change="fetchUserList"
      />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="isSubmitting"
          :disabled="selectedUsers.length === 0"
          @click="handleSubmit"
        >
          {{ t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * 分配用户弹窗组件
 * 用于角色管理中将角色分配给多个用户
 */
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getUserList, type UserInfo } from '@/api/user'
import {
  assignRoleToUsers,
  getRoleDetail,
  type RoleInfo,
  type UserInfo as RoleUserInfo,
} from '@/api/role'
import { useSubmitGuard } from '@blink/components'

interface Props {
  modelValue: boolean
  role: RoleInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const { isSubmitting, submitGuard } = useSubmitGuard()
const tableRef = ref()
const userList = ref<UserInfo[]>([])
const selectedUsers = ref<RoleUserInfo[]>([])

// 已分配的用户ID集合（用于回显）
const assignedUserIds = ref<Set<number>>(new Set())

const searchForm = reactive({
  loginName: '',
  username: '',
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

// 获取已分配的用户
const fetchAssignedUsers = async () => {
  if (!props.role?.roleId) return

  try {
    const detail = await getRoleDetail(props.role.roleId)
    const users = detail.users || []
    assignedUserIds.value = new Set(users.map((u) => u.userId))
    // 初始化已选用户列表
    selectedUsers.value = [...users]
  } catch {
    assignedUserIds.value = new Set()
    selectedUsers.value = []
  }
}

const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm,
    })
    userList.value = res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
    userList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 设置表格选中状态
const setTableSelection = () => {
  userList.value.forEach((row) => {
    if (assignedUserIds.value.has(row.userId)) {
      tableRef.value?.toggleRowSelection(row, true)
    }
  })
}

// 初始化数据
const initData = async () => {
  await fetchAssignedUsers()
  await fetchUserList()
  // 等待表格渲染完成后设置选中状态
  await nextTick()
  setTimeout(() => {
    setTableSelection()
  }, 100)
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchUserList()
}

const handleReset = () => {
  searchForm.loginName = ''
  searchForm.username = ''
  pagination.pageNum = 1
  fetchUserList()
}

const handleSelectionChange = (selection: UserInfo[]) => {
  // 合并新选择的用户，保留之前选择的其他页的用户
  const currentPageIds = new Set(userList.value.map((u) => u.userId))
  const otherPageSelected = selectedUsers.value.filter((u) => !currentPageIds.has(u.userId))
  // 转换为 RoleUserInfo 格式（只保留需要的字段）
  const convertedSelection = selection.map((u) => ({
    userId: u.userId,
    loginName: u.loginName,
    username: u.username,
    avatar: u.avatar || '',
    phone: u.phone || '',
    email: u.email || '',
    locked: u.locked || 0,
    createTime: u.createTime || '',
  })) as RoleUserInfo[]
  selectedUsers.value = [...otherPageSelected, ...convertedSelection]
}

const handleRemoveUser = (user: RoleUserInfo) => {
  selectedUsers.value = selectedUsers.value.filter((u) => u.userId !== user.userId)
  // 同步取消表格选中状态
  tableRef.value?.toggleRowSelection(user, false)
}

const handleSubmit = async () => {
  if (!props.role) return
  const role = props.role

  await submitGuard(async () => {
    await assignRoleToUsers({
      roleId: role.roleId,
      userIds: selectedUsers.value.map((u) => u.userId),
    })
    ElMessage.success(t('message.operationSuccess'))
    visible.value = false
    emit('success')
  })
}

const handleClose = () => {
  searchForm.loginName = ''
  searchForm.username = ''
  pagination.pageNum = 1
  selectedUsers.value = []
  assignedUserIds.value = new Set()
  tableRef.value?.clearSelection()
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      initData()
    }
  }
)
</script>

<style scoped lang="scss">
.user-selector-content {
  .search-form {
    margin-bottom: 16px;
  }

  .selected-users {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    padding: 12px;
    background-color: var(--bg-color);
    border-radius: 4px;

    .label {
      font-size: 14px;
      color: var(--text-color-secondary);
    }

    .el-tag {
      margin: 0;
    }
  }

  .el-pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
