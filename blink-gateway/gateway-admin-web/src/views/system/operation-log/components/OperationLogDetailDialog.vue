<template>
  <el-dialog
    v-model="visible"
    :title="t('system.operationLog.detailTitle')"
    width="800px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    destroy-on-close
  >
    <el-descriptions :column="2" border v-loading="loading">
      <el-descriptions-item :label="t('system.operationLog.logId')" :span="1">{{ logDetail?.logId }}</el-descriptions-item>
      <el-descriptions-item :label="t('system.operationLog.operationTime')" :span="1">{{ logDetail?.operationTime }}</el-descriptions-item>

      <el-descriptions-item :label="t('system.operationLog.operator')" :span="1">{{ logDetail?.loginName }}</el-descriptions-item>
      <el-descriptions-item :label="t('system.operationLog.userId')" :span="1">{{ logDetail?.userId }}</el-descriptions-item>

      <el-descriptions-item :label="t('system.operationLog.logType')" :span="1">
        <el-tag :type="getLogTypeTagType(logDetail?.logType)">
          {{ logDetail?.logTypeDesc }}
        </el-tag>
      </el-descriptions-item>

      <el-descriptions-item :label="t('system.operationLog.description')" :span="1">{{ logDetail?.description || '-' }}</el-descriptions-item>

      <el-descriptions-item :label="t('system.operationLog.requestUrl')" :span="2">
        <code class="url-code">{{ logDetail?.requestMethod }} {{ logDetail?.requestUrl }}</code>
      </el-descriptions-item>

      <el-descriptions-item :label="t('system.operationLog.executeStatus')" :span="1">
        <el-tag :type="logDetail?.executeStatus === 0 ? 'success' : 'danger'">
          {{ logDetail?.executeStatusDesc }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item :label="t('system.operationLog.executeTimeMs')" :span="1">
        <span :class="getExecuteTimeClass(logDetail?.executeTimeMs)">{{ logDetail?.executeTimeMs }}ms</span>
      </el-descriptions-item>

      <el-descriptions-item :label="t('system.operationLog.ipAddress')" :span="1">{{ logDetail?.ipAddress }}</el-descriptions-item>
      <el-descriptions-item :label="t('system.operationLog.userAgent')" :span="1">
        <el-tooltip :content="logDetail?.userAgent" placement="top">
          <span class="truncate-text">{{ logDetail?.userAgent }}</span>
        </el-tooltip>
      </el-descriptions-item>

      <!-- 请求参数 -->
      <el-descriptions-item :label="t('system.operationLog.requestParams')" :span="2">
        <div class="json-content" v-if="logDetail?.requestParams">
          <pre>{{ formatJson(logDetail.requestParams) }}</pre>
        </div>
        <span v-else>-</span>
      </el-descriptions-item>

      <!-- 响应数据 -->
      <el-descriptions-item :label="t('system.operationLog.responseData')" :span="2" v-if="logDetail?.executeStatus === 0">
        <div class="json-content" v-if="logDetail?.responseData">
          <pre>{{ formatJson(logDetail.responseData) }}</pre>
        </div>
        <span v-else>-</span>
      </el-descriptions-item>

      <!-- 错误信息 -->
      <el-descriptions-item :label="t('system.operationLog.errorMsg')" :span="2" v-if="logDetail?.executeStatus === 1">
        <div class="error-content">
          {{ logDetail?.errorMsg }}
        </div>
      </el-descriptions-item>
    </el-descriptions>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.close') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getOperationLogDetail, type OperationLogDetail, LogType } from '@/api/operation-log'

const props = defineProps<{
  modelValue: boolean
  logId: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { t } = useI18n()

const visible = ref(props.modelValue)
const loading = ref(false)
const logDetail = ref<OperationLogDetail | null>(null)

// 监听visible变化
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.logId) {
    fetchLogDetail()
  }
})

// 监听visible变化同步到父组件
watch(() => visible.value, (val) => {
  emit('update:modelValue', val)
})

// 监听logId变化
watch(() => props.logId, (newLogId) => {
  if (newLogId && visible.value) {
    fetchLogDetail()
  }
})

/**
 * 获取日志详情
 */
const fetchLogDetail = async () => {
  if (!props.logId) return

  loading.value = true
  try {
    logDetail.value = await getOperationLogDetail(props.logId)
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    loading.value = false
  }
}

/**
 * 获取日志类型标签样式
 */
const getLogTypeTagType = (type: string | undefined): string => {
  if (!type) return 'info'
  const typeMap: Record<string, string> = {
    [LogType.LOGIN]: 'success',
    [LogType.SYSTEM]: 'warning',
    [LogType.OPERATION]: 'primary',
  }
  return typeMap[type] || 'info'
}

/**
 * 获取执行时长样式类
 */
const getExecuteTimeClass = (ms: number | undefined): string => {
  if (!ms) return ''
  if (ms < 100) return 'text-green-500'
  if (ms < 500) return 'text-yellow-500'
  return 'text-red-500'
}

/**
 * 格式化JSON字符串
 */
const formatJson = (jsonStr: string): string => {
  try {
    const obj = JSON.parse(jsonStr)
    return JSON.stringify(obj, null, 2)
  } catch {
    return jsonStr
  }
}
</script>

<style scoped lang="scss">
.url-code {
  @apply px-2 py-1 rounded text-xs;
  background-color: var(--bg-color-page);
  font-family: monospace;
}

.truncate-text {
  @apply inline-block max-w-[200px] truncate;
}

.json-content {
  @apply max-h-[300px] overflow-auto;

  pre {
    @apply p-3 rounded text-xs;
    background-color: var(--bg-color-page);
    font-family: 'Consolas', 'Monaco', monospace;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.error-content {
  @apply p-3 rounded text-sm text-red-500;
  background-color: var(--el-color-danger-light-9);
}
</style>