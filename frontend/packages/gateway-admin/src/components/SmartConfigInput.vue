<template>
  <div class="smart-config-input">
    <!-- IP列表类型：使用IP标签选择器 -->
    <IpTagSelector
      v-if="isIpListType"
      :model-value="String(modelValue)"
      :size="size"
      @update:model-value="handleValueChange"
    />

    <!-- 选择型字符串：使用下拉选择器 -->
    <el-select
      v-else-if="isSelectType"
      v-model="selectValue"
      :size="size"
      :placeholder="t('common.pleaseSelect')"
      class="config-select"
      @change="handleValueChange"
    >
      <el-option
        v-for="option in currentOptions"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>

    <!-- 布尔类型：使用开关 -->
    <el-switch
      v-else-if="configType === 2"
      v-model="boolValue"
      :size="size"
      @change="handleValueChange"
    />

    <!-- 数字类型：使用数字输入框 -->
    <el-input-number
      v-else-if="configType === 1"
      v-model="numberValue"
      :min="minValue"
      :max="maxValue"
      controls-position="right"
      :size="size"
      class="config-number"
      @change="handleValueChange"
    />

    <!-- JSON/数组类型：使用编辑按钮 -->
    <el-button
      v-else-if="configType === 3 || configType === 4"
      type="primary"
      :size="size"
      text
      @click="openJsonEditor"
    >
      <el-icon><Edit /></el-icon>
      {{ t('config.editJson') }}
    </el-button>

    <!-- 默认：普通输入框 -->
    <el-input
      v-else
      v-model.trim="stringValue"
      :placeholder="t('common.pleaseInput')"
      :size="size"
      class="config-input"
      @input="handleValueChange"
    />

    <!-- JSON编辑器弹窗 -->
    <el-dialog
      v-model="jsonEditorVisible"
      :title="t('config.editJson')"
      width="650px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="json-dialog"
    >
      <div class="json-editor-wrapper">
        <el-input
          v-model="jsonEditorContent"
          type="textarea"
          :rows="18"
          :placeholder="t('config.jsonPlaceholder')"
          class="json-textarea"
        />
      </div>
      <template #footer>
        <el-button @click="jsonEditorVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveJsonConfig">
          <el-icon><Check /></el-icon>
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Edit, Check } from '@element-plus/icons-vue'
import IpTagSelector from './IpTagSelector.vue'

defineOptions({
  name: 'SmartConfigInput'
})

const props = defineProps<{
  configKey: string
  configType: number // 0-字符串 1-数字 2-布尔 3-JSON 4-数组
  modelValue: any
  size?: 'small' | 'default' | 'large'
  minValue?: number
  maxValue?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: any): void
  (e: 'change'): void
}>()

const { t } = useI18n()

// 固定选项配置（label 使用 i18n key）
const CONFIG_OPTIONS_KEYS: Record<string, { labelKey: string; value: string }[]> = {
  'gateway:route:dynamic:mode': [
    { labelKey: 'config.nacosMode', value: 'nacos' },
    { labelKey: 'config.redisMode', value: 'redis' }
  ]
}

// 选择类型判断：configKey 包含 :mode 或 :list 结尾
const isSelectType = computed(() => {
  return props.configKey.endsWith(':mode') || CONFIG_OPTIONS_KEYS[props.configKey]
})

// IP列表类型判断：configKey 以 :ips 结尾
const isIpListType = computed(() => {
  return props.configKey.endsWith(':ips')
})

// 当前选项列表（翻译后的）
const currentOptions = computed(() => {
  const options = CONFIG_OPTIONS_KEYS[props.configKey] || []
  return options.map(opt => ({
    label: t(opt.labelKey),
    value: opt.value
  }))
})

// 各类型的内部值
const boolValue = ref(false)
const numberValue = ref(0)
const stringValue = ref('')
const selectValue = ref('')
const jsonEditorVisible = ref(false)
const jsonEditorContent = ref('')

// 初始化值
watch(() => props.modelValue, (newVal) => {
  if (props.configType === 2) {
    boolValue.value = newVal === true || newVal === 'true'
  } else if (props.configType === 1) {
    numberValue.value = parseInt(String(newVal)) || 0
  } else if (isSelectType.value) {
    selectValue.value = String(newVal || '')
  } else if (isIpListType.value) {
    // IP列表由 IpTagSelector 自己处理
  } else {
    stringValue.value = String(newVal || '')
  }
}, { immediate: true })

// 处理值变化
const handleValueChange = (value?: any) => {
  let emitValue: any

  if (props.configType === 2) {
    emitValue = boolValue.value
  } else if (props.configType === 1) {
    emitValue = numberValue.value
  } else if (isSelectType.value) {
    emitValue = selectValue.value
  } else if (isIpListType.value) {
    emitValue = value // 来自 IpTagSelector 的 JSON字符串
  } else {
    emitValue = stringValue.value
  }

  emit('update:modelValue', emitValue)
  emit('change')
}

// 打开JSON编辑器
const openJsonEditor = () => {
  try {
    const parsed = JSON.parse(String(props.modelValue))
    jsonEditorContent.value = JSON.stringify(parsed, null, 2)
  } catch {
    jsonEditorContent.value = String(props.modelValue || '')
  }
  jsonEditorVisible.value = true
}

// 保存JSON配置
const saveJsonConfig = () => {
  try {
    JSON.parse(jsonEditorContent.value)
  } catch {
    ElMessage.error(t('config.invalidJson'))
    return
  }

  emit('update:modelValue', jsonEditorContent.value)
  emit('change')
  jsonEditorVisible.value = false
}
</script>

<style scoped lang="scss">
.smart-config-input {
  display: flex;
  align-items: center;

  .config-select {
    width: 180px;
  }

  .config-number {
    width: 120px;
  }

  .config-input {
    width: 180px;
  }
}

.json-dialog {
  .json-editor-wrapper {
    background: var(--bg-color-page);
    border-radius: 8px;
    padding: 4px;
  }

  .json-textarea {
    :deep(.el-textarea__inner) {
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 13px;
      line-height: 1.6;
      background: transparent;
      border: none;
      box-shadow: none;
      border-radius: 8px;
    }
  }
}
</style>