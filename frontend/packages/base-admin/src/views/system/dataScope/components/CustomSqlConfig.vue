<template>
  <div class="custom-sql-config">
    <el-alert
      :title="t('dataScope.securityWarning')"
      type="warning"
      :closable="false"
      show-icon
      class="security-warning"
    />

    <el-form-item :label="t('dataScope.commonTemplates')">
      <el-button-group>
        <el-button size="small" :disabled="disabled" @click="insertTemplate('last7days')">
          {{ t('dataScope.last7Days') }}
        </el-button>
        <el-button size="small" :disabled="disabled" @click="insertTemplate('thismonth')">
          {{ t('dataScope.thisMonth') }}
        </el-button>
        <el-button size="small" :disabled="disabled" @click="insertTemplate('statusenabled')">
          {{ t('dataScope.statusEnabled') }}
        </el-button>
      </el-button-group>
    </el-form-item>

    <el-form-item :label="t('dataScope.sqlFragment')">
      <el-input
        v-model="sqlFragment"
        type="textarea"
        :rows="6"
        :placeholder="t('common.pleaseInput')"
        :disabled="disabled"
        @input="updateConfig"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
/**
 * 自定义SQL配置组件
 * 用于配置数据范围权限中的自定义SQL过滤条件
 *
 * @author binblink
 * @since 2026-03-19
 */
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'CustomSqlConfig' })

const { t } = useI18n()

interface Props {
  modelValue: string
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const sqlFragment = ref('')

/**
 * 初始化配置，从modelValue解析JSON
 */
const initConfig = () => {
  if (!props.modelValue) {
    sqlFragment.value = ''
    return
  }

  try {
    const parsed = JSON.parse(props.modelValue)
    sqlFragment.value = parsed.sqlFragment || ''
  } catch (e) {
    sqlFragment.value = ''
  }
}

watch(() => props.modelValue, initConfig, { immediate: true })

/**
 * 插入SQL模板
 * 将预定义的SQL条件片段追加到当前SQL片段中
 *
 * @param type 模板类型：last7days-最近7天，thismonth-本月，statusenabled-状态启用
 */
const insertTemplate = (type: string) => {
  const templates: Record<string, string> = {
    last7days: 'create_time > DATE_SUB(NOW(), INTERVAL 7 DAY)',
    thismonth: "DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')",
    statusenabled: 'status = 0',
  }

  const template = templates[type]
  if (template) {
    sqlFragment.value = sqlFragment.value ? `${sqlFragment.value} AND ${template}` : template
    updateConfig()
  }
}

/**
 * 更新配置并触发emit
 */
const updateConfig = () => {
  emit(
    'update:modelValue',
    JSON.stringify({
      sqlFragment: sqlFragment.value,
    })
  )
}
</script>

<style scoped lang="scss">
.custom-sql-config {
  .security-warning {
    margin-bottom: 16px;
  }

  :deep(.el-alert) {
    --el-alert-bg-color: var(--card-bg);
    --el-alert-border-color: var(--border-color-light);
  }

  :deep(.el-textarea__inner) {
    background-color: var(--input-bg);
    color: var(--text-color-primary);

    &::placeholder {
      color: var(--text-color-placeholder);
    }
  }
}
</style>
