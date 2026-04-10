<template>
  <el-dialog
    :title="dialogTitle"
    v-model="visible"
    width="650px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="permission-form">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('system.permission.acName')" prop="acName">
            <el-input v-model.trim="form.acName" :placeholder="t('common.pleaseInput')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('system.permission.acEnName')" prop="acEnName">
            <el-input v-model.trim="form.acEnName" :placeholder="t('common.pleaseInput')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('system.permission.acIdentity')" prop="acIdentity">
            <el-input v-model.trim="form.acIdentity" :placeholder="t('common.pleaseInput')">
              <template #prepend v-if="currentAcType === 2">datascope:</template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12" v-if="props.acType === undefined">
          <el-form-item :label="t('system.permission.acType')" prop="acType">
            <el-select
              v-model="form.acType"
              :placeholder="t('common.pleaseSelect')"
              style="width: 100%"
              @change="handleTypeChange"
            >
              <el-option :label="t('system.permission.typeApi')" :value="1" />
              <el-option :label="t('system.permission.typeData')" :value="2" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 接口权限字段 -->
      <el-form-item v-if="currentAcType === 1" :label="t('system.permission.url')" prop="url">
        <el-input v-model.trim="form.url" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <!-- 关联菜单选择器（仅接口权限显示） -->
      <el-form-item v-if="currentAcType === 1" :label="t('system.permission.relatedMenus')">
        <el-tree-select
          v-model="form.menuIds"
          :data="menuTreeData"
          :props="{
            label: 'menuName',
            value: 'menuId',
            children: 'children',
            disabled: 'disabled',
          }"
          :placeholder="t('common.pleaseSelect')"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          check-strictly
          :render-after-expand="false"
          style="width: 100%"
          :empty-text="t('common.noData')"
        />
      </el-form-item>

      <!-- 数据过滤权限字段 - 表格单选 -->
      <div v-else-if="currentAcType === 2" class="data-filter-section">
        <div class="section-title">{{ t('system.permission.selectDataFilterRule') }}</div>
        <el-form-item prop="dataFilterId" label-width="0">
          <el-table
            ref="tableRef"
            :data="dataFilterList"
            v-loading="dataFilterLoading"
            highlight-current-row
            @current-change="handleCurrentChange"
            style="width: 100%"
            max-height="250px"
            border
            size="small"
          >
            <el-table-column width="50" align="center">
              <template #default="{ row }">
                <el-radio :label="row.dataFilterId" v-model="form.dataFilterId" @click.stop>
                  &nbsp;
                </el-radio>
              </template>
            </el-table-column>
            <el-table-column
              prop="dataFilterName"
              :label="t('dataScope.filterName')"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column
              prop="entityClass"
              :label="t('dataScope.entityClass')"
              min-width="180"
              show-overflow-tooltip
            />
            <el-table-column prop="ruleType" :label="t('dataScope.ruleType')" min-width="100">
              <template #default="{ row }">
                {{ getRuleTypeName(row.ruleType) }}
              </template>
            </el-table-column>
          </el-table>
          <div v-if="dataFilterList.length === 0 && !dataFilterLoading" class="empty-tip">
            {{ t('common.noData') }}
          </div>
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  addPermission,
  updatePermission,
  getMenuTreeForPermission,
  type PermissionInfo,
  type MenuInfo,
} from '@/api/permission'
import { getDataFilterList, type DataFilterInfo } from '@/api/dataScope'
import { useSubmitGuard } from '@/composables/useSubmitGuard'

interface Props {
  modelValue: boolean
  type: 'add' | 'edit'
  data: PermissionInfo | null
  acType?: number
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const dialogTitle = computed(() =>
  props.type === 'add'
    ? t('system.permission.addPermission')
    : t('system.permission.editPermission')
)

const formRef = ref<FormInstance>()
const tableRef = ref()
const { isSubmitting, submitGuard } = useSubmitGuard()
const dataFilterLoading = ref(false)
const dataFilterList = ref<DataFilterInfo[]>([])
const menuTreeData = ref<MenuInfo[]>([])

const form = reactive({
  acId: undefined as number | undefined,
  acName: '',
  acEnName: '',
  acIdentity: '',
  acType: 1,
  url: '',
  dataFilterId: undefined as number | undefined,
  menuIds: [] as number[],
})

// 当前使用的权限类型（优先使用 prop，否则使用 form.acType）
const currentAcType = computed(() => props.acType ?? form.acType)

// 规则类型翻译
const getRuleTypeName = (ruleType: string): string => {
  const ruleTypeMap: Record<string, string> = {
    FIELD_FILTER: t('dataScope.FIELD_FILTER'),
    CREATOR_FILTER: t('dataScope.CREATOR_FILTER'),
    DATE_RANGE_FILTER: t('dataScope.DATE_RANGE_FILTER'),
    CUSTOM_SQL: t('dataScope.CUSTOM_SQL'),
  }
  return ruleTypeMap[ruleType] || ruleType
}

// 表格行选中事件
const handleCurrentChange = (row: DataFilterInfo | null) => {
  if (row) {
    form.dataFilterId = row.dataFilterId
  }
}

const rules = computed<FormRules>(() => ({
  acName: [
    {
      required: true,
      message: t('common.pleaseInput') + t('system.permission.acName'),
      trigger: 'blur',
    },
  ],
  acIdentity: [
    {
      required: true,
      message: t('common.pleaseInput') + t('system.permission.acIdentity'),
      trigger: 'blur',
    },
  ],
  acType:
    props.acType === undefined
      ? [
          {
            required: true,
            message: t('common.pleaseSelect') + t('system.permission.acType'),
            trigger: 'change',
          },
        ]
      : [],
  // 接口权限时，URL必填
  url:
    currentAcType.value === 1
      ? [
          {
            required: true,
            message: t('common.pleaseInput') + t('system.permission.url'),
            trigger: 'blur',
          },
        ]
      : [],
  // 数据过滤权限时，dataFilterId 必填
  dataFilterId:
    currentAcType.value === 2
      ? [
          {
            required: true,
            message: t('common.pleaseSelect') + t('system.permission.dataFilterId'),
            trigger: 'change',
          },
        ]
      : [],
}))

// 获取数据过滤器列表（只查询启用状态 status=0）
const fetchDataFilterList = async () => {
  dataFilterLoading.value = true
  try {
    const res = await getDataFilterList({ pageSize: 1000, status: 0 })
    dataFilterList.value = res.rows || []
  } catch (error) {
    dataFilterList.value = []
  } finally {
    dataFilterLoading.value = false
  }
}

// 获取菜单树（用于接口权限关联）
const fetchMenuTree = async () => {
  try {
    const res = await getMenuTreeForPermission()
    // 过滤并处理菜单树：只保留页面和按钮，目录设为禁用
    const processMenuTree = (menus: MenuInfo[]): MenuInfo[] => {
      return menus.map((menu) => ({
        ...menu,
        disabled: menu.type === 1, // 目录不可选
        children: menu.children ? processMenuTree(menu.children) : undefined,
      }))
    }
    menuTreeData.value = processMenuTree(res || [])
  } catch {
    menuTreeData.value = []
  }
}

// 权限类型切换时清空相关字段
const handleTypeChange = () => {
  form.url = ''
  form.dataFilterId = undefined
  form.acIdentity = ''
}

// 获取实际的权限标识（数据过滤权限自动加前缀）
const getActualAcIdentity = (): string => {
  if (currentAcType.value === 2) {
    // 数据过滤权限自动加前缀
    return 'datascope:' + form.acIdentity
  }
  return form.acIdentity
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    await submitGuard(async () => {
      const acIdentity = getActualAcIdentity()
      const acType = currentAcType.value
      if (props.type === 'add') {
        await addPermission({
          acName: form.acName,
          acEnName: form.acEnName || undefined,
          acIdentity: acIdentity,
          acType: acType,
          url: acType === 1 ? form.url || undefined : undefined,
          dataFilterId: acType === 2 ? form.dataFilterId : undefined,
          menuIds: acType === 1 ? form.menuIds : undefined,
        })
        ElMessage.success(t('message.success'))
      } else {
        await updatePermission({
          acId: form.acId!,
          acName: form.acName,
          acEnName: form.acEnName || undefined,
          acIdentity: acIdentity,
          acType: acType,
          url: acType === 1 ? form.url || undefined : undefined,
          dataFilterId: acType === 2 ? form.dataFilterId : undefined,
          menuIds: acType === 1 ? form.menuIds : undefined,
        })
        ElMessage.success(t('message.success'))
      }
      visible.value = false
      emit('success')
    })
  })
}

const handleClose = () => {
  formRef.value?.resetFields()
  form.acId = undefined
  form.acName = ''
  form.acEnName = ''
  form.acIdentity = ''
  form.acType = props.acType ?? 1
  form.url = ''
  form.dataFilterId = undefined
  form.menuIds = []
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      // 初始化 acType
      form.acType = props.acType ?? 1

      // 获取菜单树（用于接口权限关联）
      fetchMenuTree()

      if (props.type === 'edit' && props.data) {
        form.acId = props.data.acId
        form.acName = props.data.acName
        form.acEnName = props.data.acEnName || ''
        // 编辑数据过滤权限时，去掉前缀显示
        if (props.data.acType === 2 && props.data.acIdentity?.startsWith('datascope:')) {
          form.acIdentity = props.data.acIdentity.substring(10)
        } else {
          form.acIdentity = props.data.acIdentity
        }
        form.acType = props.data.acType
        form.url = props.data.url || ''
        form.dataFilterId = props.data.dataFilterId || undefined
        form.menuIds = props.data.menuIds || []
      }
    }
  }
)

onMounted(() => {
  fetchDataFilterList()
})
</script>

<style scoped lang="scss">
.permission-form {
  padding: 0;
}

// 权限标识 prepend 区域适配
:deep(.el-input-group__prepend) {
  background-color: var(--table-header-bg);
  color: var(--text-color-regular);
  border-color: var(--input-border);
  padding: 0 12px;
  font-size: var(--font-size-base);
  line-height: 32px;
}

// 数据过滤规则区域
.data-filter-section {
  margin-top: 10px;

  .section-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-color-primary);
    margin-bottom: 12px;
    padding-left: 2px;
  }

  :deep(.el-table) {
    border-radius: 4px;
    overflow: hidden;

    .el-table__header-wrapper {
      th.el-table__cell {
        background: var(--bg-color-page) !important;
        color: var(--text-color-primary) !important;
        font-weight: 500;
      }
    }

    // 选中行样式
    .el-table__row {
      &.current-row {
        > td {
          background-color: var(--primary-color-light-9) !important;
        }
      }
    }

    // radio 样式优化
    .el-radio {
      margin-right: 0;

      .el-radio__label {
        display: none;
      }
    }
  }

  .empty-tip {
    text-align: center;
    color: var(--text-color-secondary);
    padding: 20px 0;
    background-color: var(--card-bg);
    border: 1px solid var(--border-color-light);
    border-top: none;
  }
}

// 验证错误提示样式
:deep(.el-form-item__error) {
  padding-top: 4px;
}
</style>
