<template>
  <el-dialog
    :title="dialogTitle"
    v-model="visible"
    width="600px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="dict-data-form">
      <el-form-item :label="t('dict.dictLabel')" prop="dictLabel">
        <el-input v-model.trim="form.dictLabel" :placeholder="t('common.pleaseInput')" />
      </el-form-item>
      <el-form-item :label="t('dict.dictValue')" prop="dictValue">
        <el-input v-model.trim="form.dictValue" :placeholder="t('common.pleaseInput')" />
      </el-form-item>
      <el-form-item :label="t('dict.locale')" prop="locale">
        <el-select
          v-model="form.locale"
          :placeholder="t('common.pleaseSelect')"
          style="width: 100%"
        >
          <el-option label="简体中文" value="zh_cn" />
          <el-option label="English" value="en_us" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('dict.dictSort')" prop="dictSort">
        <el-input-number
          v-model="form.dictSort"
          :min="0"
          controls-position="right"
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item :label="t('dict.listClass')" prop="listClass">
        <el-select
          v-model="form.listClass"
          :placeholder="t('common.pleaseSelect')"
          style="width: 100%"
        >
          <el-option label="默认" value="" />
          <el-option label="主要" value="primary" />
          <el-option label="成功" value="success" />
          <el-option label="信息" value="info" />
          <el-option label="警告" value="warning" />
          <el-option label="危险" value="danger" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('dict.isDefault')" prop="isDefault">
        <el-radio-group v-model="form.isDefault">
          <el-radio :value="1">{{ t('common.yes') }}</el-radio>
          <el-radio :value="0">{{ t('common.no') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('common.status')" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :value="0">{{ t('dict.statusEnable') }}</el-radio>
          <el-radio :value="1">{{ t('dict.statusDisable') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('common.remark')" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          :placeholder="t('common.pleaseInput')"
        />
      </el-form-item>
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
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addDictData, updateDictData, type DictDataInfo } from '@/api/dict'
import { getCurrentLocale } from '@/locales'
import { useSubmitGuard } from '@blink/components'

/**
 * 组件属性接口
 */
interface Props {
  modelValue: boolean
  type: 'add' | 'edit'
  data: DictDataInfo | null
  dictType: string
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const dialogTitle = computed(() =>
  props.type === 'add' ? t('dict.addDictData') : t('dict.editDictData')
)

const formRef = ref<FormInstance>()
const { isSubmitting, submitGuard } = useSubmitGuard()

const form = reactive({
  dictCode: undefined as number | undefined,
  dictLabel: '',
  dictValue: '',
  dictSort: 0,
  dictType: '',
  cssClass: '',
  listClass: '',
  isDefault: 0,
  status: 0,
  remark: '',
  locale: getCurrentLocale(),
})

const rules: FormRules = {
  dictLabel: [
    { required: true, message: t('common.pleaseInput') + t('dict.dictLabel'), trigger: 'blur' },
  ],
  dictValue: [
    { required: true, message: t('common.pleaseInput') + t('dict.dictValue'), trigger: 'blur' },
  ],
  dictSort: [
    { required: true, message: t('common.pleaseInput') + t('dict.dictSort'), trigger: 'blur' },
  ],
}

/**
 * 提交表单
 */
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    await submitGuard(async () => {
      if (props.type === 'add') {
        await addDictData({
          dictLabel: form.dictLabel,
          dictValue: form.dictValue,
          dictSort: form.dictSort,
          dictType: props.dictType,
          cssClass: form.cssClass || undefined,
          listClass: form.listClass || undefined,
          isDefault: form.isDefault,
          status: form.status,
          remark: form.remark || undefined,
          locale: form.locale,
        })
        ElMessage.success(t('message.success'))
      } else {
        await updateDictData({
          dictCode: form.dictCode!,
          dictLabel: form.dictLabel,
          dictValue: form.dictValue,
          dictSort: form.dictSort,
          dictType: form.dictType,
          cssClass: form.cssClass || undefined,
          listClass: form.listClass || undefined,
          isDefault: form.isDefault,
          status: form.status,
          remark: form.remark || undefined,
          locale: form.locale,
        })
        ElMessage.success(t('message.success'))
      }
      visible.value = false
      emit('success')
    })
  })
}

/**
 * 关闭弹窗时重置表单
 */
const handleClose = () => {
  formRef.value?.resetFields()
  form.dictCode = undefined
  form.dictLabel = ''
  form.dictValue = ''
  form.dictSort = 0
  form.dictType = ''
  form.cssClass = ''
  form.listClass = ''
  form.isDefault = 0
  form.status = 0
  form.remark = ''
  form.locale = getCurrentLocale()
}

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.type === 'edit' && props.data) {
      form.dictCode = props.data.dictCode
      form.dictLabel = props.data.dictLabel
      form.dictValue = props.data.dictValue
      form.dictSort = props.data.dictSort || 0
      form.dictType = props.data.dictType
      form.cssClass = props.data.cssClass || ''
      form.listClass = props.data.listClass || ''
      form.isDefault = props.data.isDefault || 0
      form.status = props.data.status || 0
      form.remark = props.data.remark || ''
      form.locale = props.data.locale || getCurrentLocale()
    }
  }
)
</script>

<style scoped lang="scss">
.dict-data-form {
  padding: 16px 0;
}
</style>
