<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="900px"
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
      <el-divider content-position="left">{{ t('dataScope.basicInfo') }}</el-divider>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('dataScope.filterName')" prop="dataFilterName">
            <el-input
              v-model.trim="formData.dataFilterName"
              :placeholder="t('common.pleaseInput')"
              :disabled="isDetail"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('dataScope.filterEnName')">
            <el-input
              v-model.trim="formData.dataFilterEnName"
              :placeholder="t('common.pleaseInput')"
              :disabled="isDetail"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
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
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('dataScope.tableName')">
            <el-input v-model="formData.tableName" disabled :placeholder="t('common.pleaseInput')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
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
                :disabled="option.disabled"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
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
        </el-col>
      </el-row>

      <el-form-item :label="t('dataScope.remark')">
        <el-input
          v-model.trim="formData.remark"
          type="textarea"
          :rows="2"
          :placeholder="t('common.pleaseInput')"
          :disabled="isDetail"
        />
      </el-form-item>

      <el-divider content-position="left">{{ t('dataScope.ruleConfig') }}</el-divider>

      <div v-if="formData.ruleType" class="rule-config-area">
        <FieldFilterConfig
          v-if="formData.ruleType === 'FIELD_FILTER'"
          v-model="formData.ruleConfig"
          :fields="entityFields"
          :disabled="isDetail"
        />
        <CreatorFilterConfig
          v-else-if="formData.ruleType === 'CREATOR_FILTER'"
          v-model="formData.ruleConfig"
          :fields="entityFields"
          :disabled="isDetail"
          @update:valid="ruleConfigValid = $event"
        />
        <DateRangeConfig
          v-else-if="formData.ruleType === 'DATE_RANGE_FILTER'"
          v-model="formData.ruleConfig"
          :fields="entityFields"
          :disabled="isDetail"
          @update:valid="ruleConfigValid = $event"
        />
        <CustomSqlConfig
          v-else-if="formData.ruleType === 'CUSTOM_SQL'"
          v-model="formData.ruleConfig"
          :disabled="isDetail"
        />
        <RelationFilterConfig
          v-else-if="formData.ruleType === 'RELATION_FILTER'"
          v-model="formData.ruleConfig"
          :entity-info="selectedEntity"
          :disabled="isDetail"
        />
      </div>
      <el-empty v-else :description="t('dataScope.ruleTypeRequired')" />
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
 * 数据权限规则表单弹窗组件
 * 用于新增、编辑和查看详情数据权限规则，支持多种规则类型配置
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getEntityList,
  getEntityFields,
  addDataFilter,
  updateDataFilter,
  getDataFilterDetail,
  type DataFilterInfo,
  type EntityInfo,
  type EntityFieldVO
} from '@/api/dataScope'
import FieldFilterConfig from './FieldFilterConfig.vue'
import CreatorFilterConfig from './CreatorFilterConfig.vue'
import DateRangeConfig from './DateRangeConfig.vue'
import CustomSqlConfig from './CustomSqlConfig.vue'
import RelationFilterConfig from './RelationFilterConfig.vue'
import { useSubmitGuard } from '@/composables/useSubmitGuard'

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
const { isSubmitting, submitGuard } = useSubmitGuard()
const entityList = ref<EntityInfo[]>([])
const entityFields = ref<EntityFieldVO[]>([])
const ruleConfigValid = ref(true) // 规则配置是否有效

/**
 * 是否为详情模式
 */
const isDetail = computed(() => props.type === 'detail')

/**
 * 弹窗标题
 */
const dialogTitle = computed(() => {
  if (props.type === 'add') return t('dataScope.addTitle')
  if (props.type === 'edit') return t('dataScope.editTitle')
  return t('dataScope.detailTitle') || t('dataScope.editTitle')
})

/**
 * 当前选中的实体信息（包含tableName供关联过滤配置使用）
 */
const selectedEntity = computed(() => {
  const entity = entityList.value.find(e => e.entityClass === formData.entityClass)
  if (entity) {
    return {
      ...entity,
      tableName: formData.tableName || entity.tableName
    }
  }
  return undefined
})

/**
 * 表单数据结构
 */
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

/**
 * 规则类型选项列表
 */
const ruleTypeOptions = computed(() => {
  const types = [
    { value: 'FIELD_FILTER', label: t('dataScope.fieldFilter') },
    { value: 'CREATOR_FILTER', label: t('dataScope.creatorFilter') },
    { value: 'DATE_RANGE_FILTER', label: t('dataScope.dateRangeFilter') },
    { value: 'CUSTOM_SQL', label: t('dataScope.customSql') },
    { value: 'RELATION_FILTER', label: t('dataScope.relationFilter'), disabled: false }
  ]

  // 如果当前实体没有关联关系，禁用关联过滤选项
  if (formData.entityClass) {
    const entity = entityList.value.find(e => e.entityClass === formData.entityClass)
    if (entity && (!entity.relations || entity.relations.length === 0)) {
      const relationType = types.find(t => t.value === 'RELATION_FILTER')
      if (relationType) {
        relationType.disabled = true
        relationType.label = t('dataScope.relationFilter') + ' (' + t('dataScope.noRelationSupport') + ')'
      }
    }
  }

  return types
})

/**
 * 表单验证规则
 */
const rules: FormRules = {
  dataFilterName: [
    { required: true, message: t('dataScope.nameRequired'), trigger: 'blur' },
    { max: 64, message: t('dataScope.nameMaxLength', { max: 64 }), trigger: 'blur' }
  ],
  entityClass: [
    { required: true, message: t('dataScope.entityRequired'), trigger: 'change' }
  ],
  ruleType: [
    { required: true, message: t('dataScope.ruleTypeRequired'), trigger: 'change' }
  ]
}

/**
 * 加载实体列表
 * 从后端获取已注册的实体类列表供用户选择
 */
const loadEntityList = async () => {
  try {
    entityList.value = await getEntityList()
  } catch {
    // 静默失败
  }
}

/**
 * 处理实体类变更事件
 * 更新表名并加载对应实体的字段列表
 *
 * @param entityClass 选中的实体类全路径
 */
const handleEntityChange = async (entityClass: string) => {
  const entity = entityList.value.find(e => e.entityClass === entityClass)
  if (entity) {
    formData.tableName = entity.tableName
  }

  // 加载字段列表
  try {
    const res = await getEntityFields(entityClass)
    entityFields.value = res.fields || []
  } catch {
    entityFields.value = []
  }
}

/**
 * 处理规则类型变更事件
 * 清空现有规则配置以便重新配置
 */
const handleRuleTypeChange = () => {
  formData.ruleConfig = ''
  ruleConfigValid.value = true // 重置为有效状态
}

/**
 * 加载规则详情
 * 编辑模式和详情模式下根据ID加载完整规则信息
 */
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

      // 加载字段列表
      const fieldsRes = await getEntityFields(detail.entityClass)
      entityFields.value = fieldsRes.fields || []
    }
  } catch {
    ElMessage.error(t('dataScope.loadDetailFailed'))
  }
}

/**
 * 监听弹窗打开状态
 * 打开时根据模式初始化表单
 */
watch(visible, (val) => {
  if (val) {
    if (props.type === 'add') {
      resetForm()
    } else {
      loadDetail()
    }
  }
})

/**
 * 重置表单数据
 * 清空所有字段并重置验证状态
 */
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
  entityFields.value = []
  formRef.value?.resetFields()
}

/**
 * 关闭弹窗
 */
const handleClose = () => {
  visible.value = false
}

/**
 * 提交表单
 * 验证通过后根据模式调用新增或更新接口
 */
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    // 检查规则配置是否有效
    if (!ruleConfigValid.value) {
      const warningMsg = formData.ruleType === 'DATE_RANGE_FILTER'
        ? t('dataScope.noTimeField')
        : t('dataScope.noUserIdField')
      ElMessage.warning(warningMsg)
      return
    }

    // 检查规则配置是否为空
    if (!formData.ruleConfig) {
      ElMessage.warning(t('dataScope.ruleConfigRequired'))
      return
    }

    await submitGuard(async () => {
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
    })
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

.data-filter-form {
  .entity-class-path {
    margin-left: 8px;
    color: var(--text-color-secondary);
    font-size: 12px;
  }

  .rule-config-area {
    padding: 16px;
    background: var(--bg-color-page);
    border-radius: 4px;
    border: 1px solid var(--border-color-light);

    :deep(.el-form-item__content) {
      width: 100%;
    }

    :deep(.el-divider__text) {
      color: var(--text-color-primary);
      background-color: var(--card-bg);
      padding: 0 12px;
    }

    :deep(.el-divider::before) {
      border-color: var(--border-color-light);
    }

    :deep(.el-form-item__label) {
      color: var(--text-color-regular);
    }
  }
}
</style>