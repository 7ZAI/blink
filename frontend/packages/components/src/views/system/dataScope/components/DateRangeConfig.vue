<template>
  <div class="date-range-config">
    <!-- 无时间字段提示 -->
    <el-alert
      v-if="timeFields.length === 0"
      :title="t('dataScope.noTimeField')"
      type="warning"
      :closable="false"
      show-icon
      class="no-time-field-warning"
    />

    <!-- 有时间字段时显示配置 -->
    <template v-else>
      <el-form-item :label="t('dataScope.matchField')">
        <el-select v-model="config.field" :placeholder="t('common.pleaseSelect')" :disabled="disabled" @change="updateConfig">
          <el-option
            v-for="field in timeFields"
            :key="field.columnName"
            :label="field.columnName"
            :value="field.columnName"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('dataScope.rangeType')">
        <el-radio-group v-model="config.rangeType" :disabled="disabled" @change="updateConfig">
          <el-radio value="RELATIVE">{{ t('dataScope.relativeTime') }}</el-radio>
          <el-radio value="ABSOLUTE">{{ t('dataScope.absoluteTime') }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 相对时间 -->
      <template v-if="config.rangeType === 'RELATIVE'">
        <el-form-item :label="t('dataScope.timeValue')">
          <div class="time-input-group">
            <el-input-number v-model="config.relativeValue" :disabled="disabled" @change="updateConfig" />
            <el-select v-model="config.relativeUnit" :placeholder="t('common.pleaseSelect')" :disabled="disabled" @change="updateConfig">
              <el-option :label="t('dataScope.day')" value="DAY" />
              <el-option :label="t('dataScope.week')" value="WEEK" />
              <el-option :label="t('dataScope.month')" value="MONTH" />
              <el-option :label="t('dataScope.year')" value="YEAR" />
            </el-select>
          </div>
        </el-form-item>
      </template>

      <!-- 绝对时间 -->
      <template v-if="config.rangeType === 'ABSOLUTE'">
        <el-form-item :label="t('dataScope.startTime')">
          <el-date-picker
            v-model="config.startTime"
            type="datetime"
            :placeholder="t('common.pleaseSelect')"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled="disabled"
            @change="updateConfig"
          />
        </el-form-item>
        <el-form-item :label="t('dataScope.endTime')">
          <el-date-picker
            v-model="config.endTime"
            type="datetime"
            :placeholder="t('common.pleaseSelect')"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled="disabled"
            @change="updateConfig"
          />
        </el-form-item>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EntityFieldVO } from '@/api/dataScope'

defineOptions({ name: 'DateRangeConfig' })

const { t } = useI18n()

interface Props {
  modelValue: string
  fields: EntityFieldVO[]
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:valid': [value: boolean]
}>()

/**
 * 时间类型字段列表
 * 支持：LocalDateTime, Date, Timestamp, LocalDate, LocalTime, Instant
 */
const timeFields = computed(() => {
  const timeTypes = ['LocalDateTime', 'Date', 'Timestamp', 'LocalDate', 'LocalTime', 'Instant', 'java.util.Date', 'java.sql.Timestamp', 'java.time.LocalDateTime', 'java.time.LocalDate', 'java.time.LocalTime', 'java.time.Instant']

  return props.fields.filter(field => timeTypes.includes(field.fieldType))
})

// 当时间字段列表变化时，通知父组件
watch(timeFields, (fields) => {
  emit('update:valid', fields.length > 0)
}, { immediate: true })

interface DateRangeConfig {
  field: string
  rangeType: string
  relativeValue: number | null
  relativeUnit: string
  startTime: string | null
  endTime: string | null
}

const config = reactive<DateRangeConfig>({
  field: '',
  rangeType: 'RELATIVE',
  relativeValue: -7,
  relativeUnit: 'DAY',
  startTime: null,
  endTime: null
})

// 初始化配置
const initConfig = () => {
  if (!props.modelValue) {
    config.field = ''
    config.rangeType = 'RELATIVE'
    config.relativeValue = -7
    config.relativeUnit = 'DAY'
    config.startTime = null
    config.endTime = null
    return
  }

  try {
    const parsed: DateRangeConfig = JSON.parse(props.modelValue)
    Object.assign(config, parsed)
  } catch (e) {
  }
}

watch(() => props.modelValue, initConfig, { immediate: true })

const updateConfig = () => {
  const result: DateRangeConfig = {
    field: config.field,
    rangeType: config.rangeType,
    relativeValue: config.rangeType === 'RELATIVE' ? config.relativeValue : null,
    relativeUnit: config.rangeType === 'RELATIVE' ? config.relativeUnit : 'DAY',
    startTime: config.rangeType === 'ABSOLUTE' ? config.startTime : null,
    endTime: config.rangeType === 'ABSOLUTE' ? config.endTime : null
  }
  emit('update:modelValue', JSON.stringify(result))
}
</script>

<style scoped lang="scss">
.time-input-group {
  display: flex;
  gap: 12px;

  .el-input-number {
    width: 120px;
  }

  .el-select {
    width: 100px;
  }
}

.date-range-config {
  .no-time-field-warning {
    margin-bottom: 16px;
  }

  :deep(.el-alert) {
    --el-alert-bg-color: var(--card-bg);
    --el-alert-border-color: var(--border-color-light);
  }

  :deep(.el-select .el-select__placeholder) {
    color: var(--text-color-placeholder);
  }

  :deep(.el-select .el-select__selected-item) {
    color: var(--text-color-primary);
  }

  :deep(.el-form-item__label) {
    color: var(--text-color-regular);
  }
}
</style>