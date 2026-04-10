<template>
  <div class="data-filter-page table-page-container">
    <!-- Search Form -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item :label="t('dataScope.filterName')">
          <el-input
            v-model="searchForm.dataFilterName"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item :label="t('dataScope.entityClass')">
          <el-select
            v-model="searchForm.entityClass"
            :placeholder="t('common.pleaseSelect')"
            clearable
            filterable
            style="width: 160px"
          >
            <el-option
              v-for="entity in entityList"
              :key="entity.entityClass"
              :label="entity.entityName"
              :value="entity.entityClass"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('dataScope.ruleType')">
          <el-select
            v-model="searchForm.ruleType"
            :placeholder="t('common.pleaseSelect')"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="option in ruleTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select
            v-model="searchForm.status"
            :placeholder="t('common.pleaseSelect')"
            clearable
            style="width: 90px"
          >
            <el-option :label="t('common.enabled')" :value="0" />
            <el-option :label="t('common.disabled')" :value="1" />
          </el-select>
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
    </el-card>

    <!-- Table -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <AuthButton
            :has-permission="() => checkPermission(ButtonPerms.DataFilter.Add)"
            type="primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon>
            {{ t('common.add') }}
          </AuthButton>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table :data="tableData" v-loading="loading" height="100%" stripe>
          <el-table-column prop="dataFilterId" label="ID" width="80" align="center" />
          <el-table-column
            prop="dataFilterName"
            :label="t('dataScope.filterName')"
            min-width="120"
          />
          <el-table-column prop="entityClass" :label="t('dataScope.entityClass')" min-width="150">
            <template #default="{ row }">
              <el-tooltip :content="row.entityClass" placement="top">
                <span class="entity-name">{{ getEntityName(row.entityClass) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="tableName" :label="t('dataScope.tableName')" min-width="120" />
          <el-table-column
            prop="ruleType"
            :label="t('dataScope.ruleType')"
            width="140"
            align="center"
          >
            <template #default="{ row }">
              <el-tag>{{ t(`dataScope.${row.ruleType}`) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.status')" width="100" align="center">
            <template #default="{ row }">
              <div class="status-switch-wrapper" @click.stop>
                <el-switch
                  :model-value="row.status"
                  :active-value="0"
                  :inactive-value="1"
                  :before-change="() => handleStatusChange(row)"
                />
              </div>
            </template>
          </el-table-column>
          <el-table-column
            prop="createBy"
            :label="t('common.createBy')"
            width="120"
            align="center"
          />
          <el-table-column
            prop="createTime"
            :label="t('common.createTime')"
            width="180"
            align="center"
          />
          <el-table-column :label="t('common.operation')" width="220" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton
                  :has-permission="() => checkPermission(ButtonPerms.DataFilter.Detail)"
                  type="primary"
                  link
                  size="small"
                  @click="handleDetail(row)"
                >
                  <el-icon><View /></el-icon>
                  {{ t('common.detail') }}
                </AuthButton>
                <AuthButton
                  :has-permission="() => checkPermission(ButtonPerms.DataFilter.Edit)"
                  type="primary"
                  link
                  size="small"
                  @click="handleEdit(row)"
                >
                  <el-icon><Edit /></el-icon>
                  {{ t('common.edit') }}
                </AuthButton>
                <AuthButton
                  :has-permission="() => checkPermission(ButtonPerms.DataFilter.Delete)"
                  type="danger"
                  link
                  size="small"
                  @click="handleDelete(row)"
                >
                  <el-icon><Delete /></el-icon>
                  {{ t('common.delete') }}
                </AuthButton>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchForm.pageNum"
          v-model:page-size="searchForm.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- Form Dialog -->
    <DataFilterFormDialog
      v-model="dialogVisible"
      :type="dialogType"
      :data="currentRow"
      @success="loadData"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 数据过滤规则管理页面
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, View } from '@element-plus/icons-vue'
import {
  getDataFilterList,
  deleteDataFilter,
  updateDataFilter,
  getEntityList,
  type DataFilterInfo,
  type EntityInfo,
} from '@/api/dataScope'
import { ButtonPerms, usePermission } from '@/composables/usePermission'
import DataFilterFormDialog from './components/DataFilterFormDialog.vue'

defineOptions({ name: 'SystemDataFilter' })

const { t } = useI18n()
const { hasPermission: checkPermission } = usePermission()

const loading = ref(false)
const tableData = ref<DataFilterInfo[]>([])
const total = ref(0)
const entityList = ref<EntityInfo[]>([])

const searchForm = reactive({
  pageNum: 1,
  pageSize: 10,
  dataFilterName: '',
  entityClass: '',
  ruleType: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit' | 'detail'>('add')
const currentRow = ref<DataFilterInfo | null>(null)

const ruleTypeOptions = computed(() => [
  { value: 'FIELD_FILTER', label: t('dataScope.fieldFilter') },
  { value: 'CREATOR_FILTER', label: t('dataScope.creatorFilter') },
  { value: 'DATE_RANGE_FILTER', label: t('dataScope.dateRangeFilter') },
  { value: 'CUSTOM_SQL', label: t('dataScope.customSql') },
  { value: 'RELATION_FILTER', label: t('dataScope.relationFilter') },
])

const getEntityName = (entityClass: string) => {
  const entity = entityList.value.find((e) => e.entityClass === entityClass)
  if (entity?.entityName) {
    return entity.entityName
  }
  const parts = entityClass.split('.')
  return parts[parts.length - 1]
}

const loadEntityList = async () => {
  try {
    entityList.value = await getEntityList()
  } catch {
    entityList.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDataFilterList(searchForm)
    tableData.value = res.rows || []
    total.value = res.total || 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  searchForm.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.dataFilterName = ''
  searchForm.entityClass = ''
  searchForm.ruleType = ''
  searchForm.status = undefined
  searchForm.pageNum = 1
  loadData()
}

const handleAdd = () => {
  dialogType.value = 'add'
  currentRow.value = null
  dialogVisible.value = true
}

const handleEdit = (row: DataFilterInfo) => {
  dialogType.value = 'edit'
  currentRow.value = row
  dialogVisible.value = true
}

const handleDetail = (row: DataFilterInfo) => {
  dialogType.value = 'detail'
  currentRow.value = row
  dialogVisible.value = true
}

const handleDelete = async (row: DataFilterInfo) => {
  try {
    await ElMessageBox.confirm(t('dataScope.deleteConfirm'), t('message.tips'), {
      type: 'warning',
    })
    await deleteDataFilter(row.dataFilterId)
    ElMessage.success(t('message.deleteSuccess'))
    loadData()
  } catch {
    // 取消删除
  }
}

const handleStatusChange = async (row: DataFilterInfo) => {
  const newStatus = row.status === 0 ? 1 : 0
  try {
    await updateDataFilter({
      dataFilterId: row.dataFilterId,
      dataFilterName: row.dataFilterName,
      ruleConfig: row.ruleConfig,
      status: newStatus,
    })
    ElMessage.success(t('message.success'))
    row.status = newStatus
    return true
  } catch {
    return false
  }
}

onMounted(() => {
  loadEntityList()
  loadData()
})
</script>

<style scoped lang="scss">
/* 数据过滤规则页面 - 继承全局 table-page-container 样式 */
.data-filter-page {
  .entity-name {
    cursor: pointer;
  }

  .status-switch-wrapper {
    display: inline-block;
    line-height: 1;

    :deep(.el-switch) {
      vertical-align: middle;
    }
  }

  .operation-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: nowrap;
  }
}
</style>
