<template>
  <div class="ip-tag-selector">
    <el-select
      v-model="ipList"
      multiple
      filterable
      allow-create
      default-first-option
      :placeholder="t('config.ipPlaceholder')"
      :size="size"
      class="ip-select"
      @change="handleChange"
    >
      <el-option
        v-for="ip in ipList"
        :key="ip"
        :label="ip"
        :value="ip"
      />
    </el-select>
    <div class="ip-hint">{{ t('config.ipFormatHint') }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

defineOptions({
  name: 'IpTagSelector'
})

const props = defineProps<{
  modelValue: string // JSON数组字符串
  size?: 'small' | 'default' | 'large'
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const { t } = useI18n()

const ipList = ref<string[]>([])

// 解析初始值
const parseValue = (value: string) => {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) {
      return parsed.filter(item => typeof item === 'string')
    }
    return []
  } catch {
    return []
  }
}

// 初始化
watch(() => props.modelValue, (newVal) => {
  ipList.value = parseValue(newVal)
}, { immediate: true })

// IP格式验证
const isValidIpFormat = (ip: string): boolean => {
  // 单个IP地址
  const ipRegex = /^(\d{1,3}\.){3}\d{1,3}$/
  // CIDR网段
  const cidrRegex = /^(\d{1,3}\.){3}\d{1,3}\/\d{1,2}$/

  if (ipRegex.test(ip)) {
    // 验证每个部分在0-255范围内
    const parts = ip.split('.')
    return parts.every(part => parseInt(part) >= 0 && parseInt(part) <= 255)
  }

  if (cidrRegex.test(ip)) {
    const [ipPart, mask] = ip.split('/')
    if (!ipPart || !mask) return false
    const parts = ipPart.split('.')
    return parts.every(part => parseInt(part) >= 0 && parseInt(part) <= 255) &&
           parseInt(mask) >= 0 && parseInt(mask) <= 32
  }

  return false
}

const handleChange = (value: string[]) => {
  // 验证新添加的IP
  const invalidIps = value.filter(ip => !isValidIpFormat(ip))
  if (invalidIps.length > 0) {
    ElMessage.warning(`${t('config.invalidIpFormat')}: ${invalidIps.join(', ')}`)
    // 移除无效IP
    ipList.value = value.filter(ip => isValidIpFormat(ip))
  } else {
    ipList.value = value
  }

  emit('update:modelValue', JSON.stringify(ipList.value))
}
</script>

<style scoped lang="scss">
.ip-tag-selector {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .ip-select {
    width: 100%;
    min-width: 280px;
  }

  .ip-hint {
    font-size: 12px;
    color: var(--text-color-secondary);
    line-height: 1.4;
  }
}

:deep(.el-select-tags) {
  .el-tag {
    max-width: 150px;
  }
}
</style>