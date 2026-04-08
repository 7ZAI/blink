<template>
  <div class="dict-data-container">
    <!-- 左侧字典类型列表 -->
    <div class="dict-sidebar">
      <div class="sidebar-header">
        <BlinkIcon icon="mdi:book-open-variant" size="20" class="header-icon" />
        <span class="sidebar-title">{{ t('dict.typeTitle') }}</span>
      </div>
      <div class="sidebar-search">
        <el-input
          v-model.trim="searchKeyword"
          :placeholder="t('common.search')"
          clearable
          size="small"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="sidebar-menu">
        <div
          v-for="item in filteredDictTypes"
          :key="item.dictId"
          class="menu-item"
          :class="{ 'is-active': activeDictType === item.dictType }"
          @click="handleDictTypeChange(item)"
        >
          <div class="menu-icon-wrapper">
            <BlinkIcon icon="mdi:tag" size="18" />
          </div>
          <span class="menu-label">{{ item.dictName }}</span>
          <el-tag size="small" type="info" class="menu-type">{{ item.dictType }}</el-tag>
        </div>
        <el-empty v-if="filteredDictTypes.length === 0" :description="t('common.noData')" :image-size="80" />
      </div>
    </div>

    <!-- 右侧字典数据列表 -->
    <div class="dict-content">
      <div class="content-header">
        <div class="header-left">
          <BlinkIcon icon="mdi:format-list-bulleted" size="22" class="content-icon" />
          <span class="content-title">{{ currentDictTypeName || t('dict.selectDictType') }}</span>
        </div>
        <div class="header-right">
          <span v-if="activeDictType" class="data-count">{{ pagination.total }} {{ t('dict.dataItems') }}</span>
        </div>
      </div>

      <div v-if="!activeDictType" class="empty-state">
        <el-empty :description="t('dict.selectDictTypeHint')" />
      </div>

      <div v-else class="content-body">
        <el-card class="search-card" shadow="never">
          <el-form :model="searchForm" inline>
            <el-form-item :label="t('dict.dictLabel')">
              <el-input v-model.trim="searchForm.dictLabel" :placeholder="t('common.pleaseInput')" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item :label="t('common.status')">
              <el-select v-model="searchForm.status" :placeholder="t('common.pleaseSelect')" clearable style="width: 100px">
                <el-option :label="t('dict.statusEnable')" :value="0" />
                <el-option :label="t('dict.statusDisable')" :value="1" />
              </el-select>
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

        <el-card class="table-card" shadow="never">
          <template #header>
            <div class="table-header">
              <div class="header-left">
                <AuthButton :has-permission="() => checkPermission(ButtonPerms.DictData.Add)" type="primary" @click="handleAdd">
                  <el-icon><Plus /></el-icon>{{ t('common.add') }}
                </AuthButton>
              </div>
            </div>
          </template>

          <div class="table-wrapper data-transition-wrapper" :class="transitionClass">
            <el-table v-loading="loading" :data="dictDataList" height="100%" stripe>
              <el-table-column prop="dictCode" label="ID" width="80" align="center" />
              <el-table-column prop="dictLabel" :label="t('dict.dictLabel')" min-width="120">
                <template #default="{ row }">
                  <el-tag v-if="row.listClass" :type="row.listClass as any">{{ row.dictLabel }}</el-tag>
                  <span v-else>{{ row.dictLabel }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="dictValue" :label="t('dict.dictValue')" min-width="120" />
              <el-table-column prop="dictSort" :label="t('dict.dictSort')" width="100" align="center" />
              <el-table-column prop="locale" :label="t('dict.locale')" width="110" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.locale === 'zh_cn'" type="primary" size="small">{{ t('settings.chinese') }}</el-tag>
                  <el-tag v-else-if="row.locale === 'en_us'" type="success" size="small">{{ t('settings.english') }}</el-tag>
                  <span v-else>{{ row.locale }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="isDefault" :label="t('dict.isDefault')" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.isDefault === 1" type="success">{{ t('common.yes') }}</el-tag>
                  <el-tag v-else type="info">{{ t('common.no') }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" :label="t('common.status')" width="80" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.status === 0" type="success">{{ t('dict.statusEnable') }}</el-tag>
                  <el-tag v-else type="danger">{{ t('dict.statusDisable') }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="t('common.createTime')" min-width="160" />
              <el-table-column :label="t('common.operation')" width="180" fixed="right">
                <template #default="{ row }">
                  <div class="operation-buttons">
                    <AuthButton :has-permission="() => checkPermission(ButtonPerms.DictData.Edit)" type="primary" link size="small" @click="handleEdit(row)">
                      <el-icon><Edit /></el-icon>{{ t('common.edit') }}
                    </AuthButton>
                    <AuthButton :has-permission="() => checkPermission(ButtonPerms.DictData.Delete)" type="danger" link size="small" @click="handleDelete(row)">
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
              @size-change="fetchDictDataList"
              @current-change="fetchDictDataList"
            />
          </div>
        </el-card>
      </div>
    </div>

    <DictDataFormDialog
      v-model="formDialogVisible"
      :type="formType"
      :data="currentDictData"
      :dict-type="activeDictType"
      @success="fetchDictDataList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getDictTypeList, getDictDataList, deleteDictData, type DictTypeInfo, type DictDataInfo } from '@/api/dict'
import { useTransition } from '@/composables/useDataTransition'
import { ButtonPerms, usePermission } from '@/composables/usePermission'

defineOptions({
  name: 'SystemDictData',
})
import DictDataFormDialog from './components/DictDataFormDialog.vue'

const { hasPermission: checkPermission } = usePermission()

const { t } = useI18n()
const { transitionClass, startTransition, finishTransition } = useTransition()

const loading = ref(false)
const dictTypeList = ref<DictTypeInfo[]>([])
const dictDataList = ref<DictDataInfo[]>([])
const searchKeyword = ref('')
const activeDictType = ref('')
const currentDictTypeName = ref('')
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')
const currentDictData = ref<DictDataInfo | null>(null)

const searchForm = reactive({
  dictLabel: '',
  status: undefined as number | undefined,
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

/**
 * 过滤后的字典类型列表
 */
const filteredDictTypes = computed(() => {
  if (!searchKeyword.value) {
    return dictTypeList.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return dictTypeList.value.filter(
    (item) =>
      item.dictName.toLowerCase().includes(keyword) ||
      item.dictType.toLowerCase().includes(keyword)
  )
})

/**
 * 获取字典类型列表
 */
const fetchDictTypeList = async () => {
  try {
    const res = await getDictTypeList({ pageNum: 1, pageSize: 1000, status: 0 })
    dictTypeList.value = res.rows || []
  } catch (error) {
    console.error('[DictData] Failed to fetch dict type list:', error)
    dictTypeList.value = []
  }
}

/**
 * 获取字典数据列表
 */
const fetchDictDataList = async () => {
  if (!activeDictType.value) return

  startTransition()
  loading.value = true
  try {
    const res = await getDictDataList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      dictType: activeDictType.value,
      ...searchForm,
    })
    dictDataList.value = res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
    dictDataList.value = []
    pagination.total = 0
    console.error('[DictData] Failed to fetch dict data list:', error)
  } finally {
    loading.value = false
    finishTransition()
  }
}

/**
 * 切换字典类型
 * @param item 字典类型项
 */
const handleDictTypeChange = (item: DictTypeInfo) => {
  activeDictType.value = item.dictType
  currentDictTypeName.value = item.dictName
  pagination.pageNum = 1
  searchForm.dictLabel = ''
  searchForm.status = undefined
  fetchDictDataList()
}

/**
 * 搜索
 */
const handleSearch = () => {
  pagination.pageNum = 1
  fetchDictDataList()
}

/**
 * 重置
 */
const handleReset = () => {
  searchForm.dictLabel = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  fetchDictDataList()
}

/**
 * 新增
 */
const handleAdd = () => {
  formType.value = 'add'
  currentDictData.value = null
  formDialogVisible.value = true
}

/**
 * 编辑
 * @param row 行数据
 */
const handleEdit = (row: DictDataInfo) => {
  formType.value = 'edit'
  currentDictData.value = row
  formDialogVisible.value = true
}

/**
 * 删除
 * @param row 行数据
 */
const handleDelete = async (row: DictDataInfo) => {
  try {
    await ElMessageBox.confirm(t('dict.deleteDataConfirm'), t('message.tips'), {
      type: 'warning',
    })
    await deleteDictData({
      deleteId: row.dictCode,
      batchDelete: false,
    })
    ElMessage.success(t('message.deleteSuccess'))
    fetchDictDataList()
  } catch (error) {
    // 用户取消删除
    if (error !== 'cancel') {
      console.error('[DictData] Failed to delete dict data:', error)
    }
  }
}

onMounted(() => {
  fetchDictTypeList()
})
</script>

<style scoped lang="scss">
.dict-data-container {
  display: flex;
  height: 100%;
  background: var(--bg-color-page);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.dict-sidebar {
  width: 280px;
  background: var(--card-bg);
  border-right: 1px solid var(--border-color-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  .sidebar-header {
    padding: 16px;
    border-bottom: 1px solid var(--border-color-light);
    display: flex;
    align-items: center;
    gap: 10px;

    .header-icon {
      color: var(--primary-color);
    }

    .sidebar-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-color-primary);
    }
  }

  .sidebar-search {
    padding: 12px;
    border-bottom: 1px solid var(--border-color-light);
  }

  .sidebar-menu {
    flex: 1;
    overflow-y: auto;
    padding: 8px;

    .menu-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      margin: 2px 0;
      cursor: pointer;
      transition: all 0.2s ease;
      color: var(--text-color-regular);
      font-size: 13px;
      border-radius: 6px;
      position: relative;

      &:hover {
        background: var(--bg-color);
        color: var(--primary-color);
      }

      &.is-active {
        background: var(--primary-color-light-9);
        color: var(--primary-color);
        font-weight: 500;
      }

      .menu-icon-wrapper {
        width: 28px;
        height: 28px;
        border-radius: 6px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--bg-color);
        transition: all 0.2s ease;
      }

      .menu-label {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .menu-type {
        font-size: 11px;
      }
    }
  }
}

.dict-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-color);
  overflow: hidden;

  .content-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-color-light);
    background: var(--card-bg);
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .content-icon {
        color: var(--primary-color);
      }

      .content-title {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-color-primary);
      }
    }

    .header-right {
      .data-count {
        font-size: 12px;
        color: var(--text-color-secondary);
        background: var(--bg-color);
        padding: 4px 10px;
        border-radius: 10px;
      }
    }
  }

  .empty-state {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .content-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 16px;
    overflow-y: auto;

    .search-card {
      flex-shrink: 0;
    }

    .table-card {
      flex: 1;
      display: flex;
      flex-direction: column;
      background: var(--card-bg);
      border: 1px solid var(--border-color-base);
      box-shadow: var(--shadow-card);

      :deep(.el-card__header) {
        padding: 0;
        background: var(--bg-color-page);
      }

      :deep(.el-card__body) {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        padding: 0;
      }

      .table-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px;
        border-bottom: 1px solid var(--border-color-light);
      }

      .table-wrapper {
        flex: 1;
        min-height: 200px;
        overflow: auto;
        padding: 0 16px;

        .el-table {
          height: 100% !important;
        }
      }

      .pagination-wrapper {
        flex-shrink: 0;
        display: flex;
        justify-content: flex-end;
        padding: 12px 16px;
        border-top: 1px solid var(--border-color-light);
      }
    }
  }
}
</style>