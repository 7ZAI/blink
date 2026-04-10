<template>
  <el-dialog
    :title="t('system.role.assignDataPermission')"
    v-model="visible"
    width="800px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <div class="permission-content">
      <!-- 搜索栏 -->
      <div class="search-header">
        <el-input
          v-model.trim="searchKeyword"
          :placeholder="t('common.pleaseInput')"
          clearable
          style="width: 240px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="selected-info">
          <el-tag type="success" size="small">
            {{ t('system.role.selectedCount') }}: {{ currentSelectedCount }}
          </el-tag>
        </div>
      </div>

      <!-- 权限类型页签 -->
      <el-tabs v-model="activeTab" class="permission-tabs">
        <!-- 接口权限 -->
        <el-tab-pane :label="t('system.role.apiPermission')" name="api">
          <el-table
            ref="apiTableRef"
            v-loading="loading"
            :data="filteredApiPermissions"
            stripe
            border
            max-height="350px"
            row-key="acId"
            @selection-change="handleApiSelectionChange"
          >
            <el-table-column type="selection" width="50" align="center" :reserve-selection="true" />
            <el-table-column
              prop="acName"
              :label="t('system.permission.acName')"
              min-width="120"
              show-overflow-tooltip
            />
            <el-table-column
              prop="acIdentity"
              :label="t('system.permission.acIdentity')"
              min-width="180"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.acIdentity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="url" label="URL" min-width="180" show-overflow-tooltip />
          </el-table>
          <el-empty
            v-if="!filteredApiPermissions.length && !loading"
            :description="t('common.noData')"
          />
        </el-tab-pane>

        <!-- 数据权限 -->
        <el-tab-pane :label="t('system.role.dataPermission')" name="data">
          <el-table
            ref="dataTableRef"
            v-loading="loading"
            :data="filteredDataPermissions"
            stripe
            border
            max-height="350px"
            row-key="acId"
            @selection-change="handleDataSelectionChange"
          >
            <el-table-column type="selection" width="50" align="center" :reserve-selection="true" />
            <el-table-column
              prop="acName"
              :label="t('system.permission.acName')"
              min-width="120"
              show-overflow-tooltip
            />
            <el-table-column
              prop="acIdentity"
              :label="t('system.permission.acIdentity')"
              min-width="180"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.acIdentity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="dataFilterName"
              :label="t('system.permission.dataFilterId')"
              min-width="140"
              show-overflow-tooltip
            />
          </el-table>
          <el-empty
            v-if="!filteredDataPermissions.length && !loading"
            :description="t('common.noData')"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getPermissionList, type PermissionInfo } from '@/api/permission'
import { assignPermissions, getRoleDetail, type RoleInfo } from '@/api/role'
import { useSubmitGuard } from '@/composables/useSubmitGuard'

interface Props {
  modelValue: boolean
  role: RoleInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const apiTableRef = ref()
const dataTableRef = ref()
const loading = ref(false)
const { isSubmitting, submitGuard } = useSubmitGuard()
const searchKeyword = ref('')
const activeTab = ref<'api' | 'data'>('api')

// 所有权限数据
const allPermissions = ref<PermissionInfo[]>([])

// 已分配的权限ID集合（用于回显）
const assignedPermissionIds = ref<Set<number>>(new Set())

// 接口权限（ac_type=1）
const apiPermissions = computed(() => allPermissions.value.filter((p) => p.acType === 1))

// 数据权限（ac_type=2）
const dataPermissions = computed(() => allPermissions.value.filter((p) => p.acType === 2))

// 筛选后的接口权限
const filteredApiPermissions = computed(() => {
  if (!searchKeyword.value) return apiPermissions.value
  const keyword = searchKeyword.value.toLowerCase()
  return apiPermissions.value.filter(
    (p) =>
      p.acName?.toLowerCase().includes(keyword) ||
      p.acIdentity?.toLowerCase().includes(keyword) ||
      p.url?.toLowerCase().includes(keyword)
  )
})

// 筛选后的数据权限
const filteredDataPermissions = computed(() => {
  if (!searchKeyword.value) return dataPermissions.value
  const keyword = searchKeyword.value.toLowerCase()
  return dataPermissions.value.filter(
    (p) =>
      p.acName?.toLowerCase().includes(keyword) ||
      p.acIdentity?.toLowerCase().includes(keyword) ||
      p.dataFilterName?.toLowerCase().includes(keyword)
  )
})

// 已选中的接口权限
const selectedApiPermissions = ref<PermissionInfo[]>([])

// 已选中的数据权限
const selectedDataPermissions = ref<PermissionInfo[]>([])

// 当前选中数量
const currentSelectedCount = computed(() => {
  return selectedApiPermissions.value.length + selectedDataPermissions.value.length
})

// 获取所有权限列表
const fetchAllPermissions = async () => {
  loading.value = true
  try {
    const res = await getPermissionList({ pageNum: 1, pageSize: 1000 })
    allPermissions.value = res.rows || []
  } finally {
    loading.value = false
  }
}

// 获取角色已分配的权限
const fetchRolePermissions = async () => {
  if (!props.role?.roleId) return

  try {
    const detail = await getRoleDetail(props.role.roleId)
    const assignedIds = (detail.permissions || []).map((p) => p.acId)
    assignedPermissionIds.value = new Set(assignedIds)
  } catch {
    assignedPermissionIds.value = new Set()
  }
}

// 设置表格选中状态
const setTableSelection = () => {
  // 设置接口权限选中状态
  apiPermissions.value.forEach((row) => {
    if (assignedPermissionIds.value.has(row.acId)) {
      apiTableRef.value?.toggleRowSelection(row, true)
    }
  })
  // 设置数据权限选中状态
  dataPermissions.value.forEach((row) => {
    if (assignedPermissionIds.value.has(row.acId)) {
      dataTableRef.value?.toggleRowSelection(row, true)
    }
  })
}

// 初始化数据
const initData = async () => {
  await fetchAllPermissions()
  await fetchRolePermissions()
  // 等待表格渲染完成后设置选中状态
  await nextTick()
  setTimeout(() => {
    setTableSelection()
  }, 100)
}

// 接口权限选择变化
const handleApiSelectionChange = (selection: PermissionInfo[]) => {
  selectedApiPermissions.value = selection
}

// 数据权限选择变化
const handleDataSelectionChange = (selection: PermissionInfo[]) => {
  selectedDataPermissions.value = selection
}

// 提交授权
const handleSubmit = async () => {
  const role = props.role
  if (!role?.roleId) return

  await submitGuard(async () => {
    // 合并接口权限和数据权限的ID
    const selectedIds = [
      ...selectedApiPermissions.value.map((p) => p.acId),
      ...selectedDataPermissions.value.map((p) => p.acId),
    ]

    await assignPermissions({
      roleId: role.roleId,
      permissionIds: selectedIds,
    })
    ElMessage.success(t('message.success'))
    visible.value = false
    emit('success')
  })
}

// 关闭弹窗时重置
const handleClose = () => {
  searchKeyword.value = ''
  selectedApiPermissions.value = []
  selectedDataPermissions.value = []
  assignedPermissionIds.value = new Set()
  activeTab.value = 'api'
  apiTableRef.value?.clearSelection()
  dataTableRef.value?.clearSelection()
}

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

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
.permission-content {
  .search-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .selected-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .permission-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 16px;
    }
  }

  :deep(.el-table) {
    border-radius: 4px;

    .el-table__header-wrapper {
      th {
        background-color: var(--el-fill-color-light) !important;
        font-weight: 600;
      }
    }
  }
}
</style>
