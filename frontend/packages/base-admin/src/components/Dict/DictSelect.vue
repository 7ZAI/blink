<template>
  <el-select
    v-model="modelValue"
    :placeholder="placeholder || t('common.pleaseSelect')"
    :clearable="clearable"
    :disabled="disabled"
    :loading="loading"
    v-bind="$attrs"
  >
    <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
  </el-select>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDict, type DictOption } from '@/composables/useDict'

/**
 * 字典选择器组件
 * 根据字典类型自动加载字典数据并渲染下拉选择框
 */

interface Props {
  /** v-model绑定值 */
  modelValue?: string | number | null
  /** 字典类型编码 */
  dictType: string
  /** 占位符文本 */
  placeholder?: string
  /** 是否可清空 */
  clearable?: boolean
  /** 是否禁用 */
  disabled?: boolean
  /** 值类型是否为数字 */
  valueType?: 'string' | 'number'
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  clearable: true,
  disabled: false,
  valueType: 'string',
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number | null]
}>()

const { t } = useI18n()
const { options: dictOptions, loading } = useDict(props.dictType)

// 转换选项值类型
const options = computed<DictOption[]>(() => {
  return dictOptions.value.map((item) => ({
    ...item,
    value: props.valueType === 'number' ? Number(item.value) : item.value,
  }))
})

// 双向绑定值
const modelValue = computed({
  get: () => {
    if (props.modelValue === undefined || props.modelValue === null) {
      return undefined
    }
    return props.valueType === 'number' ? Number(props.modelValue) : String(props.modelValue)
  },
  set: (val) => {
    emit('update:modelValue', val ?? null)
  },
})
</script>
