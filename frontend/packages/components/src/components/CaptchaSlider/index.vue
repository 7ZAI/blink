<!-- src/components/CaptchaSlider/index.vue -->
<template>
  <div class="captcha-slider-wrapper" :class="customClass">
    <!-- 触发器：点击滑块入口 -->
    <div
      v-if="showTrigger && enabled"
      class="captcha-trigger"
      :class="{ verified: verified, disabled: disabled }"
      @click="handleTriggerClick"
    >
      <div class="trigger-track">
        <span class="trigger-text">
          {{ verified ? localeTexts.captchaVerified : triggerText || localeTexts.clickToVerify }}
        </span>
      </div>
      <div class="trigger-btn" :class="{ verified: verified }">
        <svg v-if="!verified" viewBox="0 0 24 24" class="trigger-icon">
          <path
            d="M8.59 16.59L13.17 12L8.59 7.41L10 6L16 12L10 18L8.59 16.59Z"
            fill="currentColor"
          />
        </svg>
        <svg v-else viewBox="0 0 24 24" class="trigger-icon success-icon">
          <path d="M9 16.17L4.83 12L3.41 13.41L9 19L21 7L19.59 5.59L9 16.17Z" fill="currentColor" />
        </svg>
      </div>
    </div>

    <!-- 验证码弹窗子组件 -->
    <CaptchaDialog
      v-model="dialogVisible"
      :captcha-data="captchaData"
      :current-captcha-type="currentCaptchaType"
      :loading="loading"
      :is-refreshing="isRefreshing"
      :image-width="imageWidth"
      :image-height="imageHeight"
      :slider-max-distance="sliderMaxDistance"
      :slider-left="sliderLeft"
      :jigsaw-left="jigsawLeft"
      :clicked-points="clickedPoints"
      :locale="localeTexts"
      :dialog-title="dialogTitle || localeTexts.captchaTitle"
      :dialog-width="computedDialogWidth"
      :slider-hint="sliderHint || localeTexts.dragToVerify"
      :click-word-hint="clickWordHint || localeTexts.clickWordHint"
      :slider-class="sliderClass"
      :dialog-class="dialogClass"
      @refresh="handleRefresh"
      @image-load="handleImageLoad"
      @start-drag="startDrag"
      @on-drag="onDrag"
      @stop-drag="stopDrag"
      @word-click="handleWordClick"
      @open="handleDialogOpen"
      @closed="handleDialogClosed"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type {
  CaptchaSliderProps,
  CaptchaSliderEmits,
  CaptchaData,
  CaptchaLocale,
} from './types'
import { defaultZhCnLocale } from './types'
import { useCaptchaCore } from './composables/useCaptchaCore'
import CaptchaDialog from './components/CaptchaDialog.vue'

/**
 * CaptchaSlider 验证码滑块组件
 *
 * 提供滑块拼图验证码和点选文字验证码的交互组件。
 * 使用 useCaptchaCore composable 管理核心逻辑，
 * 使用 CaptchaDialog 子组件处理弹窗和验证码UI。
 *
 * @author binblink
 * @since 2024-04-10
 */
defineOptions({
  name: 'CaptchaSlider',
})

// ========== Props 定义（保持向后兼容） ==========

const props = withDefaults(defineProps<CaptchaSliderProps>(), {
  captchaType: 'default',
  enabled: true,
  verified: false,
  dialogTitle: '',
  dialogWidth: '400px',
  sliderMaxDistance: 266,
  imageWidth: 310,
  imageHeight: 155,
  autoCloseOnSuccess: true,
  autoSubmitOnSuccess: false,
  customClass: '',
  sliderClass: '',
  dialogClass: '',
  showTrigger: true,
  triggerText: '',
  triggerVerifiedText: '',
  sliderHint: '',
  clickWordHint: '',
  disabled: false,
  locale: () => defaultZhCnLocale,
})

// ========== Emits 定义（保持向后兼容） ==========

const emit = defineEmits<CaptchaSliderEmits>()

// ========== 本地状态 ==========

const dialogVisible = ref(false)

// ========== 计算属性 ==========

/**
 * 国际化文本（合并默认值和用户配置）
 */
const localeTexts = computed<CaptchaLocale>(() => {
  return {
    ...defaultZhCnLocale,
    ...props.locale,
  }
})

/**
 * 弹窗宽度（统一处理数值和字符串格式）
 */
const computedDialogWidth = computed(() => {
  if (typeof props.dialogWidth === 'number') {
    return `${props.dialogWidth}px`
  }
  return props.dialogWidth
})

// ========== 使用 Composable ==========

const {
  captchaData,
  currentCaptchaType,
  loading,
  isRefreshing,
  sliderLeft,
  jigsawLeft,
  clickedPoints,
  fetchCaptcha,
  refreshCaptcha,
  handleImageLoad,
  startDrag,
  onDrag,
  stopDrag,
  handleWordClick,
  cleanup,
} = useCaptchaCore({
  // 使用 getter 函数确保每次都获取最新的 captchaType 值
  getCaptchaType: () => props.captchaType,
  getCaptchaApi: props.getCaptchaApi,
  checkCaptchaApi: props.checkCaptchaApi,
  imageWidth: props.imageWidth,
  imageHeight: props.imageHeight,
  sliderMaxDistance: props.sliderMaxDistance,
  locale: localeTexts.value,
  autoCloseOnSuccess: props.autoCloseOnSuccess,
  onSuccess: (result) => {
    emit('success', result)
    emit('update:verified', true)
    if (props.autoCloseOnSuccess) {
      dialogVisible.value = false
    }
  },
  onFail: (result) => {
    emit('fail', result)
  },
})

// ========== 事件处理 ==========

/**
 * 点击触发器处理
 */
const handleTriggerClick = () => {
  if (props.disabled || props.verified) {
    return
  }
  emit('trigger-click')
  dialogVisible.value = true
}

/**
 * 弹窗打开处理
 */
const handleDialogOpen = async () => {
  emit('open')
  await fetchCaptcha()
}

/**
 * 弹窗关闭完成处理
 */
const handleDialogClosed = () => {
  emit('close')
}

/**
 * 刷新验证码处理（转发事件并调用 composable 方法）
 */
const handleRefresh = async () => {
  emit('refresh')
  await refreshCaptcha()
}

// ========== 监听器 ==========

/**
 * 监听 verified 状态变化
 */
watch(
  () => props.verified,
  (newVal) => {
    if (newVal && props.autoCloseOnSuccess) {
      dialogVisible.value = false
    }
  }
)

// ========== 公开方法（保持向后兼容） ==========

/**
 * 打开弹窗
 */
const open = () => {
  if (!props.disabled && !props.verified) {
    dialogVisible.value = true
  }
}

/**
 * 关闭弹窗
 */
const close = () => {
  dialogVisible.value = false
}

/**
 * 刷新验证码
 */
const refresh = refreshCaptcha

/**
 * 重置验证状态
 */
const reset = () => {
  emit('update:verified', false)
  dialogVisible.value = false
  clickedPoints.value = []
  jigsawLeft.value = 0
  sliderLeft.value = 0
}

/**
 * 获取验证结果数据
 */
const getVerificationData = (): CaptchaData => {
  return captchaData.value
}

defineExpose({
  open,
  close,
  refresh,
  reset,
  getVerificationData,
})
</script>

<style scoped lang="scss">
.captcha-slider-wrapper {
  width: 100%;
}

// ========== 触发器样式 ==========

.captcha-trigger {
  position: relative;
  height: 44px;
  background: var(--bg-color-card, #ffffff);
  border: 2px solid var(--border-color-base, #e2e8f0);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all var(--duration-normal, 0.3s) var(--ease-out-expo, ease);

  &:hover:not(.disabled) {
    border-color: var(--primary-color, #3b82f6);
    box-shadow: var(--glow-primary, 0 0 20px rgba(59, 130, 246, 0.4));

    .trigger-track {
      background: linear-gradient(90deg, rgba(59, 130, 246, 0.05) 0%, transparent 100%);
    }

    .trigger-text {
      color: var(--primary-color, #3b82f6);
    }

    .trigger-btn {
      box-shadow: var(--btn-hover-glow, 0 0 20px rgba(59, 130, 246, 0.4));
    }
  }

  &:active:not(.disabled) {
    transform: scale(0.99);
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.5;
    border-color: var(--border-color-light, #f1f5f9);
  }

  &.verified {
    border-color: var(--success-color, #10b981);
    background: linear-gradient(90deg, rgba(16, 185, 129, 0.08) 0%, transparent 100%);

    &:hover {
      border-color: var(--success-color, #10b981);
      box-shadow: var(--glow-success, 0 0 20px rgba(16, 185, 129, 0.4));
    }

    .trigger-track {
      background: transparent;
    }

    .trigger-text {
      color: var(--success-color, #10b981);
      font-weight: 500;
    }
  }
}

.trigger-track {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--duration-normal, 0.3s) ease;
}

.trigger-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-regular, #475569);
  user-select: none;
  letter-spacing: 0.3px;
  transition: color var(--duration-normal, 0.3s) ease;
}

.trigger-btn {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 36px;
  height: 36px;
  background: var(--gradient-primary, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all var(--duration-normal, 0.3s) var(--ease-out-expo, ease);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);

  &:hover:not(.verified) {
    transform: scale(1.05);
    box-shadow:
      var(--btn-hover-glow, 0 0 20px rgba(59, 130, 246, 0.4)),
      0 4px 12px rgba(102, 126, 234, 0.4);
  }

  &.verified {
    left: calc(100% - 40px);
    background: var(--gradient-success, linear-gradient(135deg, #10b981 0%, #34d399 100%));
    box-shadow: var(--glow-success, 0 0 20px rgba(16, 185, 129, 0.4));

    .success-icon {
      animation: success-check 0.4s ease-out;
    }
  }
}

.trigger-icon {
  width: 18px;
  height: 18px;

  &.success-icon {
    width: 16px;
    height: 16px;
  }
}

// ========== 动画定义 ==========

@keyframes success-check {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

// ========== 深色模式适配 ==========

:root.dark,
html.dark,
html[data-theme='dark'] {
  .captcha-trigger {
    background: var(--bg-color-card, #1e293b);
    border-color: var(--border-color-base, #334155);

    &:hover:not(.disabled) {
      border-color: var(--primary-color, #3b82f6);
      box-shadow: var(--glow-primary, 0 0 20px rgba(59, 130, 246, 0.4));

      .trigger-track {
        background: linear-gradient(90deg, rgba(59, 130, 246, 0.1) 0%, transparent 100%);
      }

      .trigger-text {
        color: var(--primary-color-light, #60a5fa);
      }
    }

    &.verified {
      border-color: var(--success-color, #10b981);
      background: linear-gradient(90deg, rgba(16, 185, 129, 0.15) 0%, transparent 100%);

      .trigger-text {
        color: var(--success-color, #10b981);
      }
    }
  }

  .trigger-text {
    color: var(--text-color-regular, #cbd5e1);
  }
}
</style>