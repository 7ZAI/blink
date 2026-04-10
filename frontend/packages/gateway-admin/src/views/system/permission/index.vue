<template>
  <div class="permission-management table-page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('system.permission.acName')">
          <el-input
            v-model.trim="searchForm.acName"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item :label="t('system.permission.acIdentity')">
          <el-input
            v-model.trim="searchForm.acIdentity"
            :placeholder="t('common.pleaseInput')"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item :label="t('system.permission.createTime')">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            :range-separator="t('settings.to')"
            :start-placeholder="t('settings.startDate')"
            :end-placeholder="t('settings.endDate')"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
            style="width: 200px"
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
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <AuthButton
            :has-permission="() => checkPermission(ButtonPerms.Permission.Add)"
            type="primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon>
            {{ t('common.add') }}
          </AuthButton>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table v-loading="loading" :data="permissionList" height="100%" stripe>
          <el-table-column prop="acName" :label="t('system.permission.acName')" min-width="120" />
          <el-table-column
            prop="acEnName"
            :label="t('system.permission.acEnName')"
            min-width="120"
          />
          <el-table-column
            prop="acIdentity"
            :label="t('system.permission.acIdentity')"
            min-width="150"
            show-overflow-tooltip
          />
          <el-table-column
            v-if="fixedAcType === 1"
            prop="url"
            :label="t('system.permission.url')"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column
            v-if="fixedAcType === 2"
            prop="dataFilterName"
            :label="t('system.permission.dataFilterId')"
            min-width="140"
            show-overflow-tooltip
          />
          <el-table-column
            prop="createBy"
            :label="t('system.permission.createBy')"
            width="120"
            align="center"
          />
          <el-table-column
            prop="createTime"
            :label="t('system.permission.createTime')"
            width="180"
            align="center"
          />
          <el-table-column :label="t('common.operation')" width="160" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton
                  :has-permission="() => checkPermission(ButtonPerms.Permission.Edit)"
                  type="primary"
                  link
                  size="small"
                  @click="handleEdit(row)"
                >
                  <el-icon><Edit /></el-icon>
                  {{ t('common.edit') }}
                </AuthButton>
                <AuthButton
                  :has-permission="() => checkPermission(ButtonPerms.Permission.Delete)"
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
          @size-change="fetchPermissionList"
          @current-change="fetchPermissionList"
        />
      </div>
    </el-card>

    <PermissionFormDialog
      v-model="formDialogVisible"
      :type="formType"
      :data="currentPermission"
      :ac-type="fixedAcType"
      @success="fetchPermissionList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getPermissionList, deletePermission, type PermissionInfo } from '@/api/permission'
import { ButtonPerms, usePermission } from '@/composables/usePermission'

defineOptions({
  name: 'SystemPermission',
})
import PermissionFormDialog from './components/PermissionFormDialog.vue'

const { hasPermission: checkPermission } = usePermission()

const route = useRoute()
const { t } = useI18n()

// 从路由 meta 获取固定的权限类型
const fixedAcType = computed(() => route.meta?.acType as number | undefined)

const loading = ref(false)
const permissionList = ref<PermissionInfo[]>([])
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')
const currentPermission = ref<PermissionInfo | null>(null)
const dateRange = ref<[string, string] | null>(null)

const searchForm = reactive({
  acName: '',
  acIdentity: '',
  createTimeStart: '',
  createTimeEnd: '',
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const handleDateChange = (val: [string, string] | null) => {
  if (val) {
    searchForm.createTimeStart = val[0]
    searchForm.createTimeEnd = val[1]
  } else {
    searchForm.createTimeStart = ''
    searchForm.createTimeEnd = ''
  }
}

const fetchPermissionList = async () => {
  loading.value = true
  try {
    const res = await getPermissionList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      acType: fixedAcType.value,
      ...searchForm,
    })
    permissionList.value = res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
    permissionList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchPermissionList()
}

const handleReset = () => {
  searchForm.acName = ''
  searchForm.acIdentity = ''
  searchForm.createTimeStart = ''
  searchForm.createTimeEnd = ''
  dateRange.value = null
  pagination.pageNum = 1
  fetchPermissionList()
}

const handleAdd = () => {
  formType.value = 'add'
  currentPermission.value = null
  formDialogVisible.value = true
}

const handleEdit = (row: PermissionInfo) => {
  formType.value = 'edit'
  currentPermission.value = row
  formDialogVisible.value = true
}

const handleDelete = async (row: PermissionInfo) => {
  try {
    await ElMessageBox.confirm(t('system.permission.deleteConfirm'), t('message.tips'), {
      type: 'warning',
    })
    await deletePermission({ deleteId: row.acId, batchDelete: false })
    ElMessage.success(t('message.deleteSuccess'))
    fetchPermissionList()
  } catch {
    // 取消删除
  }
}

onMounted(() => {
  fetchPermissionList()
})
</script>

<style scoped lang="scss">
/* 权限管理页面 - 继承全局 table-page-container 样式 */
</style>
