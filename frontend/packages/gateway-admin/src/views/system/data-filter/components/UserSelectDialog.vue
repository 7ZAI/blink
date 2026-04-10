<template>
  <el-dialog
    :title="t('dataScope.selectUser')"
    v-model="visible"
    width="800px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
    class="user-select-dialog"
  >
    <div class="user-selector-content">
      <!-- 搜索区域 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('system.user.loginName')">
          <el-input
            v-model.trim="searchForm.loginName"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item :label="t('system.user.username')">
          <el-input
            v-model.trim="searchForm.username"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            style="height: 28px; padding: 0 12px; font-size: 13px"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 已选用户展示 -->
      <div v-if="selectedUsers.length > 0" class="selected-users">
        <span class="label">{{ t('system.role.selectedUsers') }}:</span>
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
        max-height="350px"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="userId" label="ID" width="80" align="center" />
        <el-table-column prop="loginName" :label="t('system.user.loginName')" min-width="120" />
        <el-table-column prop="username" :label="t('system.user.username')" min-width="120" />
        <el-table-column prop="phone" :label="t('system.user.phone')" min-width="120" />
        <el-table-column prop="email" :label="t('system.user.email')" min-width="150" />
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
        <el-button type="primary" :disabled="selectedUsers.length === 0" @click="handleSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * 用户选择弹窗组件
 * 用于数据权限配置中选择指定用户
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getUserList, type UserInfo } from '@/api/user'

defineOptions({ name: 'UserSelectDialog' })

interface Props {
  modelValue: boolean
  selectedUsers: UserInfo[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [users: UserInfo[]]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const tableRef = ref()
const userList = ref<UserInfo[]>([])
const selectedUsers = ref<UserInfo[]>([])

const searchForm = reactive({
  loginName: '',
  username: '',
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

/**
 * 获取用户列表
 */
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
    // 恢复当前页的选中状态
    restoreSelection()
  } catch {
    userList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/**
 * 恢复表格选中状态
 */
const restoreSelection = () => {
  const selectedLoginNames = new Set(selectedUsers.value.map((u) => u.loginName))
  userList.value.forEach((user) => {
    if (selectedLoginNames.has(user.loginName)) {
      tableRef.value?.toggleRowSelection(user, true)
    }
  })
}

/**
 * 搜索
 */
const handleSearch = () => {
  pagination.pageNum = 1
  fetchUserList()
}

/**
 * 重置搜索
 */
const handleReset = () => {
  searchForm.loginName = ''
  searchForm.username = ''
  pagination.pageNum = 1
  fetchUserList()
}

/**
 * 处理表格选择变化
 */
const handleSelectionChange = (selection: UserInfo[]) => {
  // 合并新选择的用户，保留之前选择的其他页的用户
  const currentPageLoginNames = new Set(userList.value.map((u) => u.loginName))
  const otherPageSelected = selectedUsers.value.filter(
    (u) => !currentPageLoginNames.has(u.loginName)
  )
  selectedUsers.value = [...otherPageSelected, ...selection]
}

/**
 * 移除已选用户
 */
const handleRemoveUser = (user: UserInfo) => {
  selectedUsers.value = selectedUsers.value.filter((u) => u.loginName !== user.loginName)
  // 同步取消表格选中状态
  tableRef.value?.toggleRowSelection(user, false)
}

/**
 * 确认选择
 */
const handleSubmit = () => {
  emit('confirm', selectedUsers.value)
  visible.value = false
}

/**
 * 关闭弹窗时重置
 */
const handleClose = () => {
  searchForm.loginName = ''
  searchForm.username = ''
  pagination.pageNum = 1
  // 不清空 selectedUsers，保持状态
  tableRef.value?.clearSelection()
}

/**
 * 监听弹窗打开
 */
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      // 从 props 初始化选中用户
      selectedUsers.value = [...props.selectedUsers]
      fetchUserList()
    }
  }
)
</script>

<style scoped lang="scss">
.user-select-dialog {
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
      background-color: var(--bg-color-page);
      border-radius: 4px;
      border: 1px solid var(--border-color-light);

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
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
