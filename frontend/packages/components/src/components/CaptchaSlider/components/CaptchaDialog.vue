<!-- src/components/CaptchaSlider/components/CaptchaDialog.vue -->
<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    :width="dialogWidth"
    :class="['captcha-dialog', dialogClass]"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :destroy-on-close="true"
    :align-center="true"
    @open="handleOpen"
    @closed="handleClosed"
  >
    <!-- 加载状态 -->
    <div v-if="currentCaptchaType === 'loading'" class="captcha-loading-state">
      <div class="loading-spinner">
        <svg viewBox="0 0 24 24" class="loading-icon">
          <path
            d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"
            fill="currentColor"
          />
        </svg>
      </div>
      <span class="loading-text">{{ locale.loading }}</span>
    </div>

    <!-- 滑块验证 -->
    <BlockPuzzle
      v-else-if="currentCaptchaType === 'blockPuzzle'"
      :captcha-data="captchaData"
      :image-width="imageWidth"
      :image-height="imageHeight"
      :slider-max-distance="sliderMaxDistance"
      :slider-left="sliderLeft"
      :jigsaw-left="jigsawLeft"
      :is-refreshing="isRefreshing"
      :locale="locale"
      :slider-class="sliderClass"
      @refresh="handleRefresh"
      @image-load="handleImageLoad"
      @start-drag="handleStartDrag"
      @on-drag="handleOnDrag"
      @stop-drag="handleStopDrag"
    />

    <!-- 点选验证 -->
    <ClickWord
      v-else-if="currentCaptchaType === 'clickWord'"
      :captcha-data="captchaData"
      :image-width="imageWidth"
      :image-height="imageHeight"
      :clicked-points="clickedPoints"
      :is-refreshing="isRefreshing"
      :locale="locale"
      @refresh="handleRefresh"
      @image-load="handleImageLoad"
      @word-click="handleWordClick"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CaptchaDialogProps, CaptchaDialogEmits } from '../types'
import BlockPuzzle from './BlockPuzzle.vue'
import ClickWord from './ClickWord.vue'

/**
 * CaptchaDialog 弹窗容器子组件
 *
 * 包装 el-dialog 弹窗，根据验证类型切换显示 BlockPuzzle 或 ClickWord 子组件。
 * 转发子组件事件到父组件。
 *
 * @author binblink
 * @since 2024-04-10
 */
defineOptions({
  name: 'CaptchaDialog',
})

const props = withDefaults(defineProps<CaptchaDialogProps>(), {
  modelValue: false,
  captchaData: () => ({}),
  currentCaptchaType: 'loading',
  loading: false,
  isRefreshing: false,
  imageWidth: 310,
  imageHeight: 155,
  sliderMaxDistance: 266,
  sliderLeft: 0,
  jigsawLeft: 0,
  clickedPoints: () => [],
  dialogTitle: '安全验证',
  dialogWidth: '340px',
  sliderHint: '拖动滑块完成验证',
  clickWordHint: '请按顺序点击',
  sliderClass: '',
  dialogClass: '',
})

const emit = defineEmits<CaptchaDialogEmits>()

// ========== 计算属性 ==========

/**
 * 弹窗显示状态（双向绑定）
 */
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

// ========== 事件处理 ==========

/**
 * 处理弹窗打开事件
 */
const handleOpen = () => {
  emit('open')
}

/**
 * 处理弹窗关闭完成事件
 */
const handleClosed = () => {
  emit('closed')
}

/**
 * 处理刷新验证码事件
 */
const handleRefresh = () => {
  emit('refresh')
}

/**
 * 处理图片加载完成事件
 */
const handleImageLoad = () => {
  emit('image-load')
}

/**
 * 处理开始拖动滑块事件
 *
 * @param e 鼠标或触摸事件
 */
const handleStartDrag = (e: MouseEvent | TouchEvent) => {
  emit('start-drag', e)
}

/**
 * 处理拖动过程中事件
 *
 * @param e 鼠标或触摸事件
 */
const handleOnDrag = (e: MouseEvent | TouchEvent) => {
  emit('on-drag', e)
}

/**
 * 处理停止拖动滑块事件
 */
const handleStopDrag = () => {
  emit('stop-drag')
}

/**
 * 处理点击文字事件
 *
 * @param e 鼠标事件
 */
const handleWordClick = (e: MouseEvent) => {
  emit('word-click', e)
}
</script>

<style scoped lang="scss">
// 弹窗样式定制
.captcha-dialog {
  // 弹窗头部样式
  :deep(.el-dialog__header) {
    padding: 16px 20px;
    margin: 0;
    border-bottom: 1px solid var(--border-color-light, #f1f5f9);
    background: var(--bg-color-page, #ffffff);
    border-radius: 8px 8px 0 0;
  }

  // 弹窗标题样式
  :deep(.el-dialog__title) {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color-primary, #1e293b);
    line-height: 1.5;
  }

  // 弹窗头部关闭按钮
  :deep(.el-dialog__headerbtn) {
    top: 16px;
    right: 16px;
    width: 32px;
    height: 32px;
    font-size: 18px;
    background: transparent;
    border: none;
    border-radius: 6px;
    transition: all var(--duration-normal, 0.3s) ease;

    &:hover {
      background: var(--bg-color-page, #f1f5f9);

      .el-dialog__close {
        color: var(--primary-color, #3b82f6);
      }
    }

    .el-dialog__close {
      color: var(--text-color-secondary, #64748b);
      font-weight: 600;
    }
  }

  // 弹窗内容区域样式
  :deep(.el-dialog__body) {
    padding: 20px;
    background: var(--bg-color-page, #ffffff);
    border-radius: 0 0 8px 8px;
  }

  // 弹窗整体样式
  :deep(.el-dialog) {
    border-radius: 8px;
    overflow: hidden;
    box-shadow:
      0 20px 60px rgba(0, 0, 0, 0.15),
      0 8px 24px rgba(0, 0, 0, 0.1);
  }
}

// 加载状态容器
.captcha-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: 40px 20px;
}

// 加载动画容器
.loading-spinner {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 50%;
}

// 加载图标
.loading-icon {
  width: 36px;
  height: 36px;
  color: var(--primary-color, #3b82f6);
  animation: rotate 1s linear infinite;
}

// 加载文字
.loading-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-secondary, #64748b);
  letter-spacing: 0.5px;
}

// ========== 动画定义 ==========

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// ========== 深色模式适配 ==========

:root.dark,
html.dark,
html[data-theme='dark'] {
  .captcha-dialog {
    :deep(.el-dialog__header) {
      background: var(--bg-color-page, #0f172a);
      border-color: var(--border-color-base, #334155);
    }

    :deep(.el-dialog__title) {
      color: var(--text-color-primary, #f1f5f9);
    }

    :deep(.el-dialog__headerbtn) {
      &:hover {
        background: var(--bg-color-page, #1e293b);
      }

      .el-dialog__close {
        color: var(--text-color-secondary, #94a3b8);
      }
    }

    :deep(.el-dialog__body) {
      background: var(--bg-color-page, #0f172a);
    }

    :deep(.el-dialog) {
      background: var(--bg-color-page, #0f172a);
      box-shadow:
        0 20px 60px rgba(0, 0, 0, 0.4),
        0 8px 24px rgba(0, 0, 0, 0.3);
    }
  }

  .loading-spinner {
    background: rgba(59, 130, 246, 0.2);
  }

  .loading-text {
    color: var(--text-color-secondary, #94a3b8);
  }
}
</style>