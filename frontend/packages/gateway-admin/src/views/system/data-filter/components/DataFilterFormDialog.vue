<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="700px"
    :close-on-click-modal="false"
    class="data-filter-dialog"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
      class="data-filter-form"
    >
      <el-form-item :label="t('dataScope.filterName')" prop="dataFilterName">
        <el-input
          v-model.trim="formData.dataFilterName"
          :placeholder="t('common.pleaseInput')"
          :disabled="isDetail"
        />
      </el-form-item>

      <el-form-item :label="t('dataScope.filterEnName')">
        <el-input
          v-model.trim="formData.dataFilterEnName"
          :placeholder="t('common.pleaseInput')"
          :disabled="isDetail"
        />
      </el-form-item>

      <el-form-item :label="t('dataScope.entityClass')" prop="entityClass">
        <el-select
          v-model="formData.entityClass"
          :placeholder="t('common.pleaseSelect')"
          :disabled="isDetail || type === 'edit'"
          filterable
          @change="handleEntityChange"
        >
          <el-option
            v-for="entity in entityList"
            :key="entity.entityClass"
            :label="entity.entityName"
            :value="entity.entityClass"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('dataScope.tableName')">
        <el-input v-model="formData.tableName" disabled />
      </el-form-item>

      <el-form-item :label="t('dataScope.ruleType')" prop="ruleType">
        <el-select
          v-model="formData.ruleType"
          :placeholder="t('common.pleaseSelect')"
          :disabled="isDetail"
          @change="handleRuleTypeChange"
        >
          <el-option
            v-for="option in ruleTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('dataScope.status')">
        <el-switch
          v-model="formData.status"
          :active-value="0"
          :inactive-value="1"
          :active-text="t('common.enabled')"
          :inactive-text="t('common.disabled')"
          :disabled="isDetail"
        />
      </el-form-item>

      <el-form-item :label="t('dataScope.ruleConfig')">
        <el-input
          v-model="formData.ruleConfig"
          type="textarea"
          :rows="6"
          :placeholder="t('dataScope.ruleConfigPlaceholder')"
          :disabled="isDetail"
        />
      </el-form-item>

      <el-form-item :label="t('dataScope.remark')">
        <el-input
          v-model.trim="formData.remark"
          type="textarea"
          :rows="2"
          :placeholder="t('common.pleaseInput')"
          :disabled="isDetail"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">{{ isDetail ? t('common.close') : t('common.cancel') }}</el-button>
      <el-button v-if="!isDetail" type="primary" :loading="isSubmitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * 数据过滤规则表单弹窗组件
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getEntityList,
  addDataFilter,
  updateDataFilter,
  getDataFilterDetail,
  type DataFilterInfo,
  type EntityInfo
} from '@/api/dataScope'

defineOptions({ name: 'DataFilterFormDialog' })

const { t } = useI18n()

interface Props {
  modelValue: boolean
  type: 'add' | 'edit' | 'detail'
  data: DataFilterInfo | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref<FormInstance>()
const isSubmitting = ref(false)
const entityList = ref<EntityInfo[]>([])

const isDetail = computed(() => props.type === 'detail')

const dialogTitle = computed(() => {
  if (props.type === 'add') return t('dataScope.addTitle')
  if (props.type === 'edit') return t('dataScope.editTitle')
  return t('dataScope.detailTitle')
})

const formData = reactive({
  dataFilterId: 0,
  dataFilterName: '',
  dataFilterEnName: '',
  entityClass: '',
  tableName: '',
  ruleType: '',
  ruleConfig: '',
  status: 0,
  remark: ''
})

const ruleTypeOptions = computed(() => [
  { value: 'FIELD_FILTER', label: t('dataScope.fieldFilter') },
  { value: 'CREATOR_FILTER', label: t('dataScope.creatorFilter') },
  { value: 'DATE_RANGE_FILTER', label: t('dataScope.dateRangeFilter') },
  { value: 'CUSTOM_SQL', label: t('dataScope.customSql') },
  { value: 'RELATION_FILTER', label: t('dataScope.relationFilter') }
])

const rules: FormRules = {
  dataFilterName: [
    { required: true, message: t('dataScope.nameRequired'), trigger: 'blur' }
  ],
  entityClass: [
    { required: true, message: t('dataScope.entityRequired'), trigger: 'change' }
  ],
  ruleType: [
    { required: true, message: t('dataScope.ruleTypeRequired'), trigger: 'change' }
  ]
}

const loadEntityList = async () => {
  try {
    entityList.value = await getEntityList()
  } catch {
    entityList.value = []
  }
}

const handleEntityChange = (entityClass: string) => {
  const entity = entityList.value.find(e => e.entityClass === entityClass)
  if (entity) {
    formData.tableName = entity.tableName
  }
}

const handleRuleTypeChange = () => {
  formData.ruleConfig = ''
}

const loadDetail = async () => {
  if (props.type === 'add' || !props.data?.dataFilterId) return

  try {
    const detail = await getDataFilterDetail(props.data.dataFilterId)
    if (detail) {
      Object.assign(formData, {
        dataFilterId: detail.dataFilterId,
        dataFilterName: detail.dataFilterName,
        dataFilterEnName: detail.dataFilterEnName || '',
        entityClass: detail.entityClass,
        tableName: detail.tableName,
        ruleType: detail.ruleType,
        ruleConfig: detail.ruleConfig || '',
        status: detail.status,
        remark: detail.remark || ''
      })
    }
  } catch {
    ElMessage.error(t('dataScope.loadDetailFailed'))
  }
}

watch(visible, (val) => {
  if (val) {
    if (props.type === 'add') {
      resetForm()
    } else {
      loadDetail()
    }
  }
})

const resetForm = () => {
  formData.dataFilterId = 0
  formData.dataFilterName = ''
  formData.dataFilterEnName = ''
  formData.entityClass = ''
  formData.tableName = ''
  formData.ruleType = ''
  formData.ruleConfig = ''
  formData.status = 0
  formData.remark = ''
  formRef.value?.resetFields()
}

const handleClose = () => {
  visible.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    isSubmitting.value = true
    try {
      if (props.type === 'add') {
        await addDataFilter({
          dataFilterName: formData.dataFilterName,
          dataFilterEnName: formData.dataFilterEnName || undefined,
          entityClass: formData.entityClass,
          tableName: formData.tableName,
          ruleType: formData.ruleType,
          ruleConfig: formData.ruleConfig,
          remark: formData.remark || undefined
        })
        ElMessage.success(t('message.success'))
      } else {
        await updateDataFilter({
          dataFilterId: formData.dataFilterId,
          dataFilterName: formData.dataFilterName,
          dataFilterEnName: formData.dataFilterEnName || undefined,
          ruleConfig: formData.ruleConfig,
          status: formData.status,
          remark: formData.remark || undefined
        })
        ElMessage.success(t('message.success'))
      }

      emit('success')
      handleClose()
    } finally {
      isSubmitting.value = false
    }
  })
}

onMounted(() => {
  loadEntityList()
})
</script>

<style scoped lang="scss">
.data-filter-dialog {
  :deep(.el-dialog__body) {
    max-height: 65vh;
    overflow-y: auto;
    padding: 16px 20px;
  }

  :deep(.el-dialog__header) {
    padding: 10px 48px 10px 16px;
    min-height: 20px;
  }

  :deep(.el-dialog__title) {
    font-size: 14px;
    line-height: 20px;
  }

  :deep(.el-dialog__headerbtn) {
    top: 4px;
    right: 12px;
    transform: none;
  }
}
</style>