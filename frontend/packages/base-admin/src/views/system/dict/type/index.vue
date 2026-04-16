<template>
  <div class="dict-type-management table-page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('dict.dictName')">
          <el-input
            v-model.trim="searchForm.dictName"
            :placeholder="t('common.pleaseInput')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('dict.dictType')">
          <el-input
            v-model.trim="searchForm.dictType"
            :placeholder="t('common.pleaseInput')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select
            v-model="searchForm.status"
            :placeholder="t('common.pleaseSelect')"
            clearable
            style="width: 120px"
          >
            <el-option :label="t('common.statusEnable')" :value="0" />
            <el-option :label="t('common.statusDisable')" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="info" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-left">
            <AuthButton :perm="ButtonPerms.DictType.Add" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              {{ t('common.add') }}
            </AuthButton>
          </div>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table v-loading="loading" :data="dictTypeList" height="100%" stripe border>
          <el-table-column prop="dictId" label="ID" width="80" align="center" />
          <el-table-column prop="dictName" :label="t('dict.dictName')" min-width="140" />
          <el-table-column prop="dictType" :label="t('dict.dictType')" min-width="160">
            <template #default="{ row }">
              <el-tag type="info">{{ row.dictType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('common.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="success">{{ t('common.statusEnable') }}</el-tag>
              <el-tag v-else type="danger">{{ t('common.statusDisable') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="locale" :label="t('dict.locale')" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.locale === 'zh_cn'" type="primary">简体中文</el-tag>
              <el-tag v-else-if="row.locale === 'en_us'" type="success">English</el-tag>
              <span v-else>{{ row.locale }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('common.createTime')" min-width="160" />
          <el-table-column
            prop="remark"
            :label="t('common.remark')"
            min-width="140"
            show-overflow-tooltip
          />
          <el-table-column :label="t('common.operation')" min-width="180" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton
                  :perm="ButtonPerms.DictType.Edit"
                  type="primary"
                  link
                  size="small"
                  @click="handleEdit(row)"
                >
                  <el-icon><Edit /></el-icon>
                  {{ t('common.edit') }}
                </AuthButton>
                <AuthButton
                  :perm="ButtonPerms.DictType.Delete"
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
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchDictTypeList"
          @current-change="fetchDictTypeList"
        />
      </div>
    </el-card>

    <DictTypeFormDialog
      v-model="formDialogVisible"
      :type="formType"
      :data="currentDictType"
      @success="fetchDictTypeList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getDictTypeList, deleteDictType, type DictTypeInfo } from '@/api/dict'
import { ButtonPerms } from '@/composables/usePermission'

defineOptions({
  name: 'SystemDictType',
})
import DictTypeFormDialog from './components/DictTypeFormDialog.vue'

const { t } = useI18n()

const loading = ref(false)
const dictTypeList = ref<DictTypeInfo[]>([])
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')
const currentDictType = ref<DictTypeInfo | null>(null)

const searchForm = reactive({
  dictName: '',
  dictType: '',
  status: undefined as number | undefined,
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

/**
 * 获取字典类型列表
 */
const fetchDictTypeList = async () => {
  loading.value = true
  try {
    const res = await getDictTypeList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm,
    })
    dictTypeList.value = res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
    dictTypeList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
const handleSearch = () => {
  pagination.pageNum = 1
  fetchDictTypeList()
}

/**
 * 重置
 */
const handleReset = () => {
  searchForm.dictName = ''
  searchForm.dictType = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  fetchDictTypeList()
}

/**
 * 新增
 */
const handleAdd = () => {
  formType.value = 'add'
  currentDictType.value = null
  formDialogVisible.value = true
}

/**
 * 编辑
 * @param row 行数据
 */
const handleEdit = (row: DictTypeInfo) => {
  formType.value = 'edit'
  currentDictType.value = row
  formDialogVisible.value = true
}

/**
 * 删除
 * @param row 行数据
 */
const handleDelete = async (row: DictTypeInfo) => {
  try {
    await ElMessageBox.confirm(t('dict.deleteTypeConfirm'), t('message.tips'), {
      type: 'warning',
    })
    await deleteDictType({
      deleteId: row.dictId,
      batchDelete: false,
    })
    ElMessage.success(t('message.deleteSuccess'))
    fetchDictTypeList()
  } catch {
    // 取消删除
  }
}

onMounted(() => {
  fetchDictTypeList()
})
</script>

<style scoped lang="scss">
/* 字典类型管理页面样式 - 继承全局 table-page-container 样式 */
.dict-type-management {
  /* 可添加页面特定的样式覆盖 */
}
</style>
