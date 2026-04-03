<template>
  <el-dialog
    :title="t('menu.selectPermission')"
    v-model="visible"
    width="700px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    append-to-body
  >
    <!-- 查询条件 -->
    <div class="search-bar">
      <el-input
        v-model.trim="searchForm.acName"
        :placeholder="t('permission.acName')"
        clearable
        style="width: 180px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model.trim="searchForm.acIdentity"
        :placeholder="t('permission.acIdentity')"
        clearable
        style="width: 180px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        {{ t('common.search') }}
      </el-button>
      <el-button @click="handleReset">
        {{ t('common.reset') }}
      </el-button>
    </div>

    <!-- 权限表格 -->
    <el-table
      ref="tableRef"
      :data="permissionList"
      v-loading="loading"
      highlight-current-row
      @current-change="handleCurrentChange"
      style="width: 100%"
      height="350px"
      border
      size="small"
    >
      <el-table-column width="50" align="center">
        <template #default="{ row }">
          <el-radio
            :label="row.acId"
            v-model="selectedPermId"
            @click.stop
          >&nbsp;</el-radio>
        </template>
      </el-table-column>
      <el-table-column prop="acIdentity" :label="t('permission.acIdentity')" min-width="150" show-overflow-tooltip />
      <el-table-column prop="acName" :label="t('permission.acName')" min-width="120" show-overflow-tooltip />
      <el-table-column prop="url" :label="t('permission.url')" min-width="150" show-overflow-tooltip />
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchPermissionList"
        @current-change="fetchPermissionList"
      />
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :disabled="!selectedPermId" @click="handleConfirm">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getApiPermissions, type PermissionInfo } from '@/api/menu'

interface Props {
  modelValue: boolean
  selectedId?: number
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'confirm'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const tableRef = ref()
const loading = ref(false)
const permissionList = ref<PermissionInfo[]>([])
const selectedPermId = ref<number | undefined>()
const selectedPerm = ref<PermissionInfo | null>(null)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  acName: '',
  acIdentity: '',
})

// 获取权限列表
const fetchPermissionList = async () => {
  loading.value = true
  try {
    const res = await getApiPermissions({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      acName: searchForm.acName || undefined,
      acIdentity: searchForm.acIdentity || undefined,
    })
    permissionList.value = res?.rows || []
    total.value = res?.total || 0
  } catch {
    permissionList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 表格行选中
const handleCurrentChange = (row: PermissionInfo | null) => {
  if (row) {
    selectedPermId.value = row.acId
    selectedPerm.value = row
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  fetchPermissionList()
}

// 重置
const handleReset = () => {
  searchForm.acName = ''
  searchForm.acIdentity = ''
  pageNum.value = 1
  fetchPermissionList()
}

// 确认选择
const handleConfirm = () => {
  emit('confirm', selectedPerm.value)
  visible.value = false
}

// 弹窗打开时初始化
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      selectedPermId.value = props.selectedId
      selectedPerm.value = null
      searchForm.acName = ''
      searchForm.acIdentity = ''
      pageNum.value = 1
      fetchPermissionList()
    }
  }
)
</script>

<style scoped lang="scss">
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

:deep(.el-table) {
  .el-radio {
    margin-right: 0;

    .el-radio__label {
      display: none;
    }
  }
}
</style>