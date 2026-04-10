<template>
  <div class="field-filter-config">
    <el-form-item :label="t('dataScope.matchMode')">
      <el-radio-group v-model="matchMode" :disabled="disabled" @change="handleMatchModeChange">
        <el-radio value="include">{{ t('dataScope.includeMode') }}</el-radio>
        <el-radio value="exclude">{{ t('dataScope.excludeMode') }}</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item :label="t('dataScope.ruleConfig')">
      <el-transfer
        v-model="selectedFields"
        :data="transferData"
        :titles="[t('dataScope.availableFields'), t('dataScope.selectedFields')]"
        :props="{
          key: 'columnName',
          label: 'columnName',
        }"
        filterable
        :filter-placeholder="t('common.pleaseInput')"
        :disabled="disabled"
        @change="handleFieldChange"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EntityFieldVO } from '@/api/dataScope'

defineOptions({ name: 'FieldFilterConfig' })

const { t } = useI18n()

interface Props {
  modelValue: string
  fields: EntityFieldVO[]
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// 解析配置
interface FieldFilterConfig {
  excludeFields: string[] | null
  includeFields: string[] | null
}

const matchMode = ref<'include' | 'exclude'>('include')
const selectedFields = ref<string[]>([])

// Transfer 数据源
const transferData = computed(() => props.fields)

// 初始化配置
const initConfig = () => {
  if (!props.modelValue) {
    matchMode.value = 'include'
    selectedFields.value = []
    return
  }

  try {
    const config: FieldFilterConfig = JSON.parse(props.modelValue)
    if (config.includeFields && config.includeFields.length > 0) {
      matchMode.value = 'include'
      selectedFields.value = config.includeFields
    } else if (config.excludeFields && config.excludeFields.length > 0) {
      matchMode.value = 'exclude'
      selectedFields.value = config.excludeFields
    }
  } catch {
    matchMode.value = 'include'
    selectedFields.value = []
  }
}

// 监听 modelValue 变化
watch(() => props.modelValue, initConfig, { immediate: true })

// 匹配模式变更
const handleMatchModeChange = () => {
  updateConfig()
}

// 字段变更
const handleFieldChange = () => {
  updateConfig()
}

// 更新配置
const updateConfig = () => {
  const config: FieldFilterConfig = {
    excludeFields: matchMode.value === 'exclude' ? selectedFields.value : null,
    includeFields: matchMode.value === 'include' ? selectedFields.value : null,
  }
  emit('update:modelValue', JSON.stringify(config))
}
</script>

<style scoped lang="scss">
.field-filter-config {
  :deep(.el-transfer) {
    display: flex;
    justify-content: flex-start;
    align-items: stretch;
    width: 100%;
  }

  :deep(.el-transfer-panel) {
    flex: 1;
    min-width: 240px;
    max-width: 320px;
    background-color: var(--card-bg);
    border-color: var(--border-color-light);
  }

  :deep(.el-transfer-panel__header) {
    background-color: var(--table-header-bg);
    color: var(--text-color-primary);
    border-bottom-color: var(--border-color-light);
  }

  :deep(.el-transfer-panel__body) {
    height: 350px;
  }

  :deep(.el-transfer-panel__list) {
    height: 310px;
    background-color: var(--card-bg);
  }

  :deep(.el-transfer-panel__item) {
    color: var(--text-color-regular);
    padding: 0 12px;
    line-height: 30px;
    height: 30px;

    &:hover {
      background-color: var(--table-row-hover);
    }
  }

  :deep(.el-transfer-panel__filter) {
    padding: 8px 12px;

    .el-input__wrapper {
      background-color: var(--input-bg);
    }
  }

  :deep(.el-transfer__buttons) {
    padding: 0 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
  }

  :deep(.el-transfer__button) {
    margin: 0;
    padding: 8px 14px;
    min-width: 44px;
  }
}
</style>
