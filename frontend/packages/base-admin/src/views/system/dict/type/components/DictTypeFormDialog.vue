<template>
  <el-dialog
    :title="dialogTitle"
    v-model="visible"
    width="600px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="dict-type-form">
      <el-form-item :label="t('dict.dictName')" prop="dictName">
        <el-input v-model.trim="form.dictName" :placeholder="t('common.pleaseInput')" />
      </el-form-item>
      <el-form-item :label="t('dict.dictType')" prop="dictType">
        <el-input
          v-model.trim="form.dictType"
          :placeholder="t('common.pleaseInput')"
          :disabled="props.type === 'edit'"
        />
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
import { addDictType, updateDictType, type DictTypeInfo } from '@/api/dict'
import { useSubmitGuard } from '@blink/components'
import { getCurrentLocale } from '@/locales'

/**
 * 组件属性接口
 */
interface Props {
  modelValue: boolean
  type: 'add' | 'edit'
  data: DictTypeInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const dialogTitle = computed(() =>
  props.type === 'add' ? t('dict.addDictType') : t('dict.editDictType')
)

const formRef = ref<FormInstance>()
const { isSubmitting, submitGuard } = useSubmitGuard()

const form = reactive({
  dictId: undefined as number | undefined,
  dictName: '',
  dictType: '',
  status: 0,
  remark: '',
  locale: getCurrentLocale(),
})

const rules: FormRules = {
  dictName: [
    { required: true, message: t('common.pleaseInput') + t('dict.dictName'), trigger: 'blur' },
  ],
  dictType: [
    { required: true, message: t('common.pleaseInput') + t('dict.dictType'), trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]*$/, message: t('dict.dictTypeFormat'), trigger: 'blur' },
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
        await addDictType({
          dictName: form.dictName,
          dictType: form.dictType,
          status: form.status,
          remark: form.remark || undefined,
          locale: form.locale,
        })
        ElMessage.success(t('message.success'))
      } else {
        await updateDictType({
          dictId: form.dictId!,
          dictName: form.dictName,
          dictType: form.dictType,
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
  form.dictId = undefined
  form.dictName = ''
  form.dictType = ''
  form.status = 0
  form.remark = ''
  form.locale = getCurrentLocale()
}

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.type === 'edit' && props.data) {
      form.dictId = props.data.dictId
      form.dictName = props.data.dictName
      form.dictType = props.data.dictType
      form.status = props.data.status || 0
      form.remark = props.data.remark || ''
      form.locale = props.data.locale || getCurrentLocale()
    }
  }
)
</script>

<style scoped lang="scss">
.dict-type-form {
  padding: 16px 0;
}
</style>
