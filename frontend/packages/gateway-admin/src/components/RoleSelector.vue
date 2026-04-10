<template>
  <el-dialog
    v-model="visible"
    :title="t('role.selectRole')"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-input
      v-model.trim="searchText"
      :placeholder="t('common.pleaseInput') + t('role.roleName')"
      clearable
      style="margin-bottom: 16px"
      @input="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="filteredRoles"
      @selection-change="handleSelectionChange"
      max-height="400px"
      border
    >
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column prop="roleName" :label="t('role.roleName')" min-width="120" />
      <el-table-column prop="roleCode" :label="t('role.roleCode')" min-width="120" />
      <el-table-column prop="status" :label="t('common.status')" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <div class="dialog-footer">
        <span class="selected-count">
          {{ t('role.selectedCount', { count: selectedRoles.length }) }}
        </span>
        <div>
          <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" @click="handleConfirm">
            {{ t('common.confirm') }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import { getAllRoles, type RoleInfo } from '@/api/role'

interface Props {
  modelValue: boolean
  selectedIds?: number[]
}

const props = withDefaults(defineProps<Props>(), {
  selectedIds: () => [],
})

const emit = defineEmits(['update:modelValue', 'select'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const tableRef = ref()
const loading = ref(false)
const allRoles = ref<RoleInfo[]>([])
const searchText = ref('')
const selectedRoles = ref<RoleInfo[]>([])

const filteredRoles = computed(() => {
  if (!searchText.value) {
    return allRoles.value
  }
  return allRoles.value.filter(
    (role) => role.roleName.includes(searchText.value) || role.roleCode.includes(searchText.value)
  )
})

const handleSearch = () => {
  // 搜索时保持已选中的状态
}

const handleSelectionChange = (selection: RoleInfo[]) => {
  selectedRoles.value = selection
}

const fetchRoles = async () => {
  loading.value = true
  try {
    allRoles.value = await getAllRoles()
  } finally {
    loading.value = false
  }
}

const handleConfirm = () => {
  emit('select', selectedRoles.value)
  visible.value = false
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchRoles()
      searchText.value = ''
      selectedRoles.value = []
      if (props.selectedIds.length > 0) {
        setTimeout(() => {
          props.selectedIds.forEach((id) => {
            const role = allRoles.value.find((r) => r.roleId === id)
            if (role) {
              tableRef.value?.toggleRowSelection(role, true)
            }
          })
        }, 100)
      }
    }
  }
)
</script>

<style scoped lang="scss">
.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .selected-count {
    color: var(--text-color-secondary);
    font-size: 13px;
  }
}
</style>
