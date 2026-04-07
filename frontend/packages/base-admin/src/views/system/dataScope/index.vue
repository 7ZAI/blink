<template>
  <div class="data-scope-management table-page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('dataScope.filterName')">
          <el-input v-model.trim="searchForm.dataFilterName" :placeholder="t('common.pleaseInput')" clearable />
        </el-form-item>
        <el-form-item :label="t('dataScope.entityClass')">
          <el-select v-model="searchForm.entityClass" :placeholder="t('common.pleaseSelect')" clearable filterable style="width: 160px">
            <el-option
              v-for="entity in entityList"
              :key="entity.entityClass"
              :label="entity.entityName"
              :value="entity.entityClass"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('dataScope.ruleType')">
          <el-select v-model="searchForm.ruleType" :placeholder="t('common.pleaseSelect')" clearable style="width: 140px">
            <el-option
              v-for="option in ruleTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="searchForm.status" :placeholder="t('common.pleaseSelect')" clearable style="width: 100px">
            <el-option :label="t('common.enabled')" :value="0" />
            <el-option :label="t('common.disabled')" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="info" @click="handleSearch">
            <el-icon><Search /></el-icon>{{ t('common.search') }}
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>{{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-left">
            <AuthButton :perm="ButtonPerms.DataFilter.Add" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>{{ t('common.add') }}
            </AuthButton>
          </div>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table v-loading="loading" :data="dataFilterList" height="100%" stripe border>
          <el-table-column prop="dataFilterId" label="ID" width="80" align="center" />
          <el-table-column prop="dataFilterName" :label="t('dataScope.filterName')" min-width="120" />
          <el-table-column prop="entityClass" :label="t('dataScope.entityClass')" min-width="150">
            <template #default="{ row }">
              <el-tooltip :content="row.entityClass" placement="top">
                <span class="entity-name">{{ getEntityName(row.entityClass) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="tableName" :label="t('dataScope.tableName')" min-width="120" />
          <el-table-column prop="ruleType" :label="t('dataScope.ruleType')" width="140" align="center">
            <template #default="{ row }">
              <el-tag>{{ t(`dataScope.${row.ruleType}`) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('common.status')" width="100" align="center">
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
          <el-table-column prop="createBy" :label="t('common.createBy')" width="120" align="center" />
          <el-table-column prop="createTime" :label="t('common.createTime')" width="180" align="center" />
          <el-table-column :label="t('common.operation')" width="220" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton :perm="ButtonPerms.DataFilter.Detail" type="primary" link size="small" @click="handleDetail(row)">
                  <el-icon><View /></el-icon>{{ t('common.detail') }}
                </AuthButton>
                <AuthButton :perm="ButtonPerms.DataFilter.Edit" type="primary" link size="small" @click="handleEdit(row)">
                  <el-icon><Edit /></el-icon>{{ t('common.edit') }}
                </AuthButton>
                <AuthButton :perm="ButtonPerms.DataFilter.Delete" type="danger" link size="small" @click="handleDelete(row)">
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
          @size-change="fetchDataFilterList"
          @current-change="fetchDataFilterList"
        />
      </div>
    </el-card>

    <DataFilterFormDialog
      v-model="dialogVisible"
      :type="dialogType"
      :data="currentRow"
      @success="fetchDataFilterList"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 数据权限管理主页面
 * 提供数据过滤规则的增删改查功能
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
  type EntityInfo
} from '@/api/dataScope'
import { ButtonPerms } from '@/composables/usePermission'
import { useTransition } from '@/composables/useDataTransition'
import DataFilterFormDialog from './components/DataFilterFormDialog.vue'

defineOptions({
  name: 'SystemDataScope',
})

const { t } = useI18n()
const { transitionClass, startTransition, finishTransition } = useTransition()

const loading = ref(false)
const dataFilterList = ref<DataFilterInfo[]>([])
const entityList = ref<EntityInfo[]>([])

const searchForm = reactive({
  dataFilterName: '',
  entityClass: '',
  ruleType: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit' | 'detail'>('add')
const currentRow = ref<DataFilterInfo | null>(null)

/**
 * 规则类型选项列表
 */
const ruleTypeOptions = computed(() => [
  { value: 'FIELD_FILTER', label: t('dataScope.fieldFilter') },
  { value: 'CREATOR_FILTER', label: t('dataScope.creatorFilter') },
  { value: 'DATE_RANGE_FILTER', label: t('dataScope.dateRangeFilter') },
  { value: 'CUSTOM_SQL', label: t('dataScope.customSql') },
  { value: 'RELATION_FILTER', label: t('dataScope.relationFilter') }
])

/**
 * 根据实体类全路径获取中文名称
 *
 * @param entityClass 实体类全路径
 * @return 实体中文名称，未找到时返回类简名
 */
const getEntityName = (entityClass: string) => {
  const entity = entityList.value.find(e => e.entityClass === entityClass)
  if (entity?.entityName) {
    return entity.entityName
  }
  // 降级：返回类简名
  const parts = entityClass.split('.')
  return parts[parts.length - 1]
}

/**
 * 加载实体列表
 * 获取系统中已注册的实体类供搜索和选择
 */
const loadEntityList = async () => {
  try {
    entityList.value = await getEntityList()
  } catch {
    // 静默失败
  }
}

/**
 * 加载数据过滤规则列表
 * 根据搜索条件和分页参数获取数据
 */
const fetchDataFilterList = async () => {
  startTransition()
  loading.value = true
  try {
    const res = await getDataFilterList({
      ...pagination,
      ...searchForm
    })
    dataFilterList.value = res.rows || []
    pagination.total = res.total || 0
  } catch {
    dataFilterList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
    finishTransition()
  }
}

/**
 * 执行搜索
 * 重置页码后刷新列表
 */
const handleSearch = () => {
  pagination.pageNum = 1
  fetchDataFilterList()
}

/**
 * 重置搜索条件
 * 清空所有搜索条件并刷新列表
 */
const handleReset = () => {
  searchForm.dataFilterName = ''
  searchForm.entityClass = ''
  searchForm.ruleType = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  fetchDataFilterList()
}

/**
 * 新增数据过滤规则
 * 打开新增表单弹窗
 */
const handleAdd = () => {
  dialogType.value = 'add'
  currentRow.value = null
  dialogVisible.value = true
}

/**
 * 编辑数据过滤规则
 * 打开编辑表单弹窗
 *
 * @param row 要编辑的规则数据
 */
const handleEdit = (row: DataFilterInfo) => {
  dialogType.value = 'edit'
  currentRow.value = row
  dialogVisible.value = true
}

/**
 * 查看数据过滤规则详情
 * 打开详情弹窗（只读模式）
 *
 * @param row 要查看的规则数据
 */
const handleDetail = (row: DataFilterInfo) => {
  dialogType.value = 'detail'
  currentRow.value = row
  dialogVisible.value = true
}

/**
 * 删除数据过滤规则
 * 确认后调用删除接口
 *
 * @param row 要删除的规则数据
 */
const handleDelete = async (row: DataFilterInfo) => {
  try {
    await ElMessageBox.confirm(t('dataScope.deleteConfirm'), t('message.tips'), {
      type: 'warning',
    })
    await deleteDataFilter(row.dataFilterId)
    ElMessage.success(t('message.deleteSuccess'))
    fetchDataFilterList()
  } catch {
    // 取消删除
  }
}

/**
 * 状态切换处理
 * 更新规则的启用/禁用状态
 *
 * @param row 规则数据
 */
const handleStatusChange = async (row: DataFilterInfo) => {
  const newStatus = row.status === 0 ? 1 : 0
  try {
    await updateDataFilter({
      dataFilterId: row.dataFilterId,
      dataFilterName: row.dataFilterName,
      ruleConfig: row.ruleConfig,
      status: newStatus
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
  fetchDataFilterList()
})
</script>

<style scoped lang="scss">
/* 数据权限管理页面样式 - 继承全局 table-page-container 样式 */
.data-scope-management {
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
}
</style>