<template>
  <el-dialog
    :title="t('dataScope.selectRole')"
    v-model="visible"
    width="700px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
    class="role-select-dialog"
  >
    <div class="role-selector-content">
      <!-- 搜索区域 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('role.roleName')">
          <el-input
            v-model.trim="searchForm.roleName"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item :label="t('role.roleCode')">
          <el-input
            v-model.trim="searchForm.roleCode"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>{{ t('common.search') }}
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>{{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 已选角色展示 -->
      <div v-if="selectedRoles.length > 0" class="selected-roles">
        <span class="label">{{ t('dataScope.selectRole') }}:</span>
        <el-tag
          v-for="role in selectedRoles"
          :key="role.roleId"
          closable
          @close="handleRemoveRole(role)"
        >
          {{ role.roleName }}
        </el-tag>
      </div>

      <!-- 角色列表 -->
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="roleList"
        stripe
        border
        max-height="350px"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="roleId" label="ID" width="80" align="center" />
        <el-table-column prop="roleName" :label="t('role.roleName')" min-width="120" />
        <el-table-column prop="roleEnName" :label="t('role.roleEnName')" min-width="120" />
        <el-table-column prop="roleCode" :label="t('role.roleCode')" min-width="120" />
        <el-table-column :label="t('common.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? t('common.enabled') : t('common.disabled') }}
            </el-tag>
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
        @size-change="fetchRoleList"
        @current-change="fetchRoleList"
      />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :disabled="selectedRoles.length === 0" @click="handleSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * 角色选择弹窗组件
 * 用于数据权限配置中选择指定角色
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getRoleList, type RoleInfo } from '@/api/role'

defineOptions({ name: 'RoleSelectDialog' })

interface Props {
  modelValue: boolean
  selectedIds: number[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'confirm': [roles: RoleInfo[]]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const tableRef = ref()
const roleList = ref<RoleInfo[]>([])
const selectedRoles = ref<RoleInfo[]>([])

const searchForm = reactive({
  roleName: '',
  roleCode: '',
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

/**
 * 获取角色列表
 */
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
    // 恢复当前页的选中状态
    restoreSelection()
  } catch {
    roleList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/**
 * 恢复表格选中状态
 */
const restoreSelection = () => {
  const selectedIds = new Set(selectedRoles.value.map(r => r.roleId))
  roleList.value.forEach(role => {
    if (selectedIds.has(role.roleId)) {
      tableRef.value?.toggleRowSelection(role, true)
    }
  })
}

/**
 * 搜索
 */
const handleSearch = () => {
  pagination.pageNum = 1
  fetchRoleList()
}

/**
 * 重置搜索
 */
const handleReset = () => {
  searchForm.roleName = ''
  searchForm.roleCode = ''
  pagination.pageNum = 1
  fetchRoleList()
}

/**
 * 处理表格选择变化
 */
const handleSelectionChange = (selection: RoleInfo[]) => {
  // 合并新选择的角色，保留之前选择的其他页的角色
  const currentPageIds = new Set(roleList.value.map(r => r.roleId))
  const otherPageSelected = selectedRoles.value.filter(r => !currentPageIds.has(r.roleId))
  selectedRoles.value = [...otherPageSelected, ...selection]
}

/**
 * 移除已选角色
 */
const handleRemoveRole = (role: RoleInfo) => {
  selectedRoles.value = selectedRoles.value.filter(r => r.roleId !== role.roleId)
  // 同步取消表格选中状态
  tableRef.value?.toggleRowSelection(role, false)
}

/**
 * 确认选择
 */
const handleSubmit = () => {
  emit('confirm', selectedRoles.value)
  visible.value = false
}

/**
 * 关闭弹窗时重置
 */
const handleClose = () => {
  searchForm.roleName = ''
  searchForm.roleCode = ''
  pagination.pageNum = 1
  selectedRoles.value = []
  tableRef.value?.clearSelection()
}

/**
 * 监听弹窗打开
 */
watch(() => props.modelValue, (val) => {
  if (val) {
    fetchRoleList()
  }
})
</script>

<style scoped lang="scss">
.role-select-dialog {
  .role-selector-content {
    .search-form {
      margin-bottom: 16px;
    }

    .selected-roles {
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