<!-- src/components/CaptchaSlider/components/BlockPuzzle.vue -->
<template>
  <div class="block-puzzle-captcha">
    <!-- 图片容器区域 -->
    <div
      class="captcha-image-wrapper"
      :style="imageWrapperStyle"
      :class="{ 'is-refreshing': isRefreshing }"
    >
      <!-- 刷新遮罩层 -->
      <transition name="refresh-fade">
        <div v-if="isRefreshing" class="refresh-overlay">
          <div class="refresh-spinner">
            <svg viewBox="0 0 24 24" class="refresh-icon">
              <path
                d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"
                fill="currentColor"
              />
            </svg>
          </div>
          <span class="refresh-text">{{ locale.refreshing || '刷新中...' }}</span>
        </div>
      </transition>

      <!-- 背景图片 -->
      <transition name="image-fade">
        <img
          v-if="captchaData.originalImageBase64 && !isRefreshing"
          :src="formatImageData(captchaData.originalImageBase64)"
          class="captcha-bg-image"
          :class="{ 'image-loaded': imageLoaded }"
          alt="验证码背景"
          @load="handleImageLoad"
        />
      </transition>

      <!-- 拼图滑块图片 -->
      <img
        v-if="captchaData.jigsawImageBase64 && !isRefreshing"
        :src="formatImageData(captchaData.jigsawImageBase64)"
        class="captcha-jigsaw-image"
        :class="{ 'image-loaded': imageLoaded }"
        :style="{ left: jigsawLeft + 'px' }"
        alt="滑块"
      />
    </div>

    <!-- 滑块轨道区域 -->
    <div class="slider-container" :class="sliderClass">
      <!-- 滑块轨道背景 -->
      <div class="slider-track">
        <!-- 滑块填充条 -->
        <div class="slider-fill" :style="{ width: sliderLeft + 36 + 'px' }"></div>
      </div>

      <!-- 可拖动的滑块按钮 -->
      <div
        class="slider-thumb"
        :style="{ transform: `translateX(${sliderLeft}px)` }"
        @mousedown="handleStartDrag"
        @touchstart="handleStartDrag"
      >
        <svg viewBox="0 0 24 24" class="slider-arrow">
          <path
            d="M8.59 16.59L13.17 12L8.59 7.41L10 6L16 12L10 18L8.59 16.59Z"
            fill="currentColor"
          />
        </svg>
      </div>

      <!-- 提示文字 -->
      <span class="slider-hint">{{ locale.dragToVerify }}</span>

      <!-- 刷新按钮 -->
      <button class="refresh-btn" @click="handleRefresh" :disabled="isRefreshing">
        <svg
          viewBox="0 0 24 24"
          class="refresh-btn-icon"
          :class="{ 'is-spinning': isRefreshing }"
        >
          <path
            d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"
            fill="currentColor"
          />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { BlockPuzzleProps, BlockPuzzleEmits } from '../types'

/**
 * BlockPuzzle 滑块验证码子组件
 *
 * 纯 UI 层组件，负责显示滑块验证码的界面。
 * 所有交互逻辑由父组件通过 composable 处理。
 *
 * @author binblink
 * @since 2024-04-10
 */
defineOptions({
  name: 'BlockPuzzle',
})

const props = withDefaults(defineProps<BlockPuzzleProps>(), {
  captchaData: () => ({}),
  imageWidth: 310,
  imageHeight: 155,
  sliderMaxDistance: 266,
  sliderLeft: 0,
  jigsawLeft: 0,
  isRefreshing: false,
  sliderClass: '',
})

const emit = defineEmits<BlockPuzzleEmits>()

// 图片加载状态（组件内部维护）
const imageLoaded = ref(false)

// ========== 计算属性 ==========

/**
 * 图片容器样式
 */
const imageWrapperStyle = computed(() => ({
  width: `${props.imageWidth}px`,
  height: `${props.imageHeight}px`,
}))

// ========== 工具方法 ==========

/**
 * 格式化图片数据
 * 将 base64 字符串转换为完整的图片 URL
 *
 * @param base64 图片的 base64 数据
 * @returns 完整的图片 URL
 */
const formatImageData = (base64: string) => {
  return base64.startsWith('data:') ? base64 : `data:image/png;base64,${base64}`
}

// ========== 事件处理 ==========

/**
 * 处理图片加载完成事件
 */
const handleImageLoad = () => {
  imageLoaded.value = true
  emit('image-load')
}

/**
 * 处理刷新按钮点击事件
 */
const handleRefresh = () => {
  emit('refresh')
}

/**
 * 处理滑块开始拖动事件
 *
 * @param e 鼠标或触摸事件
 */
const handleStartDrag = (e: MouseEvent | TouchEvent) => {
  e.preventDefault()
  emit('start-drag', e)
}
</script>

<style scoped lang="scss">
// 滑块验证码容器
.block-puzzle-captcha {
  display: flex;
  flex-direction: column;
  align-items: center;
}

// 图片容器
.captcha-image-wrapper {
  position: relative;
  width: 310px;
  height: 155px;
  background: var(--bg-color-page, #f1f5f9);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
  border: 1px solid var(--border-color-light, #f1f5f9);

  &.is-refreshing {
    .captcha-bg-image,
    .captcha-jigsaw-image {
      opacity: 0.3;
      filter: blur(2px);
    }
  }
}

// 背景图片
.captcha-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition:
    opacity 0.3s ease,
    filter 0.3s ease,
    transform 0.3s ease;

  &.image-loaded {
    animation: image-zoom-in 0.4s var(--ease-out-expo, ease);
  }
}

// 拼图滑块图片
.captcha-jigsaw-image {
  position: absolute;
  top: 0;
  left: 0;
  // 重要：保持 jigsaw 图片的原始尺寸，不要强制缩放
  // anji-captcha 生成的 jigsaw 图片高度与背景图一致
  height: auto;
  max-height: 100%;
  width: auto;
  object-fit: contain;
  transition:
    opacity 0.3s ease,
    filter 0.3s ease;

  &.image-loaded {
    animation: slide-in-left 0.4s var(--ease-out-expo, ease);
  }
}

// 滑块轨道容器
.slider-container {
  position: relative;
  width: 310px;
  height: 44px;
  background: var(--bg-color-page, #f1f5f9);
  border-radius: 8px;
  overflow: hidden;
  margin: 0 auto;
  user-select: none;
  border: 1px solid var(--border-color-light, #f1f5f9);
  transition: border-color var(--duration-normal, 0.3s) ease;

  &:hover {
    border-color: var(--primary-color-light-5, rgba(59, 130, 246, 0.5));
  }
}

// 滑块轨道背景
.slider-track {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--border-color-lighter, #f8fafc);
  border-radius: 8px;
}

// 滑块填充条
.slider-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.2) 0%, rgba(59, 130, 246, 0.1) 100%);
  border-radius: 8px;
  transition: width 0.05s ease-out;
}

// 滑块按钮
.slider-thumb {
  position: absolute;
  top: 4px;
  left: 0;
  width: 36px;
  height: 36px;
  background: var(--gradient-primary, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: grab;
  z-index: 10;
  transition:
    transform 0.05s ease-out,
    box-shadow var(--duration-normal, 0.3s) ease;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);

  &:hover {
    box-shadow:
      var(--btn-hover-glow, 0 0 20px rgba(59, 130, 246, 0.4)),
      0 4px 12px rgba(102, 126, 234, 0.4);
  }

  &:active {
    cursor: grabbing;
    transform: scale(1.05);
    box-shadow: var(--btn-hover-glow, 0 0 25px rgba(59, 130, 246, 0.5));
  }
}

// 滑块箭头图标
.slider-arrow {
  width: 18px;
  height: 18px;
}

// 滑块提示文字
.slider-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-secondary, #64748b);
  pointer-events: none;
  user-select: none;
}

// 刷新按钮
.refresh-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--border-color-base, #e2e8f0);
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--duration-normal, 0.3s) ease;
  z-index: 10;

  &:hover:not(:disabled) {
    background: var(--primary-color, #3b82f6);
    border-color: var(--primary-color, #3b82f6);

    .refresh-btn-icon {
      color: #ffffff;
    }
  }

  &:active:not(:disabled) {
    transform: translateY(-50%) scale(0.95);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

// 刷新按钮图标
.refresh-btn-icon {
  width: 16px;
  height: 16px;
  color: var(--text-color-secondary, #64748b);
  transition: color var(--duration-normal, 0.3s) ease;

  &.is-spinning {
    animation: rotate 1s linear infinite;
  }
}

// ========== 刷新遮罩层 ==========

.refresh-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
  z-index: 20;
  border-radius: 8px;
}

.refresh-spinner {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.refresh-icon {
  width: 36px;
  height: 36px;
  color: var(--primary-color, #3b82f6);
  animation: rotate 1s linear infinite;
}

.refresh-text {
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
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

@keyframes image-zoom-in {
  0% {
    opacity: 0;
    transform: scale(1.05);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes slide-in-left {
  0% {
    opacity: 0;
    transform: translateX(-20px);
  }
  100% {
    opacity: 1;
    transform: translateX(0);
  }
}

// 刷新过渡动画
.refresh-fade-enter-active {
  animation: refresh-fade-in 0.3s ease;
}

.refresh-fade-leave-active {
  animation: refresh-fade-out 0.3s ease;
}

@keyframes refresh-fade-in {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

@keyframes refresh-fade-out {
  0% {
    opacity: 1;
  }
  100% {
    opacity: 0;
  }
}

// 图片过渡动画
.image-fade-enter-active {
  animation: image-fade-in 0.4s var(--ease-out-expo, ease);
}

.image-fade-leave-active {
  animation: image-fade-out 0.2s ease;
}

@keyframes image-fade-in {
  0% {
    opacity: 0;
    transform: scale(1.03);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes image-fade-out {
  0% {
    opacity: 1;
  }
  100% {
    opacity: 0;
  }
}

// ========== 深色模式适配 ==========

:root.dark,
html.dark,
html[data-theme='dark'] {
  .captcha-image-wrapper {
    background: var(--bg-color-page, #0f172a);
    border-color: var(--border-color-base, #334155);
  }

  .slider-container {
    background: var(--bg-color-page, #0f172a);
    border-color: var(--border-color-base, #334155);

    &:hover {
      border-color: rgba(59, 130, 246, 0.4);
    }
  }

  .slider-track {
    background: var(--border-color-lighter, #0f172a);
  }

  .slider-fill {
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.3) 0%, rgba(59, 130, 246, 0.15) 100%);
  }

  .slider-hint {
    color: var(--text-color-secondary, #94a3b8);
  }

  .refresh-btn {
    background: rgba(30, 41, 59, 0.9);
    border-color: var(--border-color-base, #334155);

    &:hover:not(:disabled) {
      background: var(--primary-color, #3b82f6);
      border-color: var(--primary-color, #3b82f6);
    }
  }

  .refresh-btn-icon {
    color: var(--text-color-secondary, #94a3b8);
  }

  .refresh-overlay {
    background: rgba(15, 23, 42, 0.85);
  }
}
</style>