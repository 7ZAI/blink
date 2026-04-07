<!-- src/components/BlinkDialog/index.vue -->
<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="computedWidth"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    :show-close="showClose"
    :lock-scroll="lockScroll"
    :before-close="handleBeforeClose"
    :class="['blink-dialog', customClass]"
    :destroy-on-close="destroyOnClose"
    @open="emit('open')"
    @opened="emit('opened')"
    @closed="handleClosed"
  >
    <!-- 内容区域 -->
    <div v-loading="loading" class="blink-dialog__body">
      <slot />
    </div>

    <!-- 底部区域 -->
    <template v-if="showFooter" #footer>
      <slot name="footer">
        <div class="blink-dialog__footer">
          <el-button v-if="showCancel" @click="handleCancel">
            {{ cancelText }}
          </el-button>
          <el-button
            v-if="showConfirm"
            :type="confirmType"
            :loading="confirmLoading"
            @click="handleConfirm"
          >
            {{ confirmText }}
          </el-button>
        </div>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BlinkDialogProps, BlinkDialogEmits } from './types'

defineOptions({
  name: 'BlinkDialog',
})

const props = withDefaults(defineProps<BlinkDialogProps>(), {
  title: '',
  width: '500px',
  closeOnClickModal: false,
  closeOnPressEscape: true,
  showClose: true,
  lockScroll: false,
  loading: false,
  confirmLoading: false,
  showFooter: true,
  showCancel: true,
  showConfirm: true,
  cancelText: '取消',
  confirmText: '确定',
  confirmType: 'primary',
  customClass: '',
  destroyOnClose: false,
})

const emit = defineEmits<BlinkDialogEmits>()

// 双向绑定
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// 宽度处理
const computedWidth = computed(() => {
  if (typeof props.width === 'number') {
    return `${props.width}px`
  }
  return props.width
})

// 关闭前处理
const handleBeforeClose = (done: () => void) => {
  if (props.beforeClose) {
    props.beforeClose(done)
  } else {
    done()
  }
}

// 取消按钮
const handleCancel = () => {
  emit('cancel')
  visible.value = false
}

// 确认按钮
const handleConfirm = () => {
  emit('confirm')
}

// 关闭完成
const handleClosed = () => {
  emit('close')
  emit('closed')
}

// Slots 定义
defineSlots<{
  default?: () => any
  footer?: () => any
}>()
</script>

<style scoped lang="scss">
.blink-dialog {
  :deep(.el-dialog) {
    border-radius: 12px;
    overflow: hidden;
  }

  :deep(.el-dialog__header) {
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-color-light);
    margin-right: 0;

    .el-dialog__title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color-primary);
    }
  }

  &__body {
    padding: 20px;
    color: var(--text-color-regular);
    min-height: 50px;
  }

  :deep(.el-dialog__footer) {
    padding: 12px 20px;
    border-top: 1px solid var(--border-color-light);
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

// 深色模式适配
.dark .blink-dialog {
  :deep(.el-dialog__header) {
    border-bottom-color: var(--border-color-light);
  }

  :deep(.el-dialog__footer) {
    border-top-color: var(--border-color-light);
  }
}
</style>