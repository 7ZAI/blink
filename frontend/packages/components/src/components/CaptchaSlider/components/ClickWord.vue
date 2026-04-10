<!-- src/components/CaptchaSlider/components/ClickWord.vue -->
<template>
  <div class="click-word-captcha">
    <!-- 文字提示区域 -->
    <div class="word-hint-area">
      <span class="hint-text">{{ locale.clickWordHint }}</span>
      <span class="word-list">
        <span
          v-for="(word, index) in captchaData.wordList"
          :key="index"
          class="hint-word"
          :class="{ 'word-clicked': index < clickedPoints.length }"
        >
          {{ word }}
        </span>
      </span>
    </div>

    <!-- 图片容器区域 -->
    <div
      class="captcha-image-wrapper"
      :style="imageWrapperStyle"
      :class="{ 'is-refreshing': isRefreshing }"
      @click="handleWordClick"
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
          alt="点选验证码背景"
          @load="handleImageLoad"
        />
      </transition>

      <!-- 已点击的标记点 -->
      <transition-group name="point-pop" tag="div" class="clicked-points-container">
        <div
          v-for="(point, index) in clickedPoints"
          :key="`${point.x}-${point.y}-${index}`"
          class="click-point-marker"
          :style="{ left: `${point.x}px`, top: `${point.y}px` }"
        >
          <span class="point-number">{{ index + 1 }}</span>
        </div>
      </transition-group>
    </div>

    <!-- 操作按钮区域 -->
    <div class="action-buttons-area">
      <!-- 确认按钮 -->
      <button
        class="confirm-btn"
        :disabled="clickedPoints.length === 0 || isRefreshing"
        @click="handleConfirm"
      >
        {{ locale.confirm }}
      </button>

      <!-- 刷新按钮 -->
      <button class="refresh-btn" :disabled="isRefreshing" @click="handleRefresh">
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
import type { ClickWordProps, ClickWordEmits } from '../types'

/**
 * ClickWord 点选验证码子组件
 *
 * 纯 UI 层组件，负责显示点选验证码的界面。
 * 所有交互逻辑由父组件通过 composable 处理。
 *
 * @author binblink
 * @since 2024-04-10
 */
defineOptions({
  name: 'ClickWord',
})

const props = withDefaults(defineProps<ClickWordProps>(), {
  captchaData: () => ({}),
  imageWidth: 310,
  imageHeight: 155,
  clickedPoints: () => [],
  isRefreshing: false,
})

const emit = defineEmits<ClickWordEmits>()

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
 * 处理图片区域点击事件
 *
 * @param e 鼠标事件
 */
const handleWordClick = (e: MouseEvent) => {
  // 如果正在刷新，不处理点击
  if (props.isRefreshing) {
    return
  }
  emit('word-click', e)
}

/**
 * 处理确认按钮点击事件
 * 注意：确认按钮触发后，父组件需要调用 submitWordCaptcha 进行验证
 */
const handleConfirm = () => {
  // 确认按钮的点击事件通过 word-click 事件传递，
  // 父组件会根据点击的元素判断是否是确认操作
  // 或者父组件直接监听这个按钮的点击来触发验证
}
</script>

<style scoped lang="scss">
// 点选验证码容器
.click-word-captcha {
  display: flex;
  flex-direction: column;
  align-items: center;
}

// 文字提示区域
.word-hint-area {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 16px;
  background: var(--bg-color-page, #f1f5f9);
  border-radius: 8px;
  border: 1px solid var(--border-color-light, #f1f5f9);
}

.hint-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-primary, #1e293b);
}

.word-list {
  display: flex;
  align-items: center;
  gap: 6px;
}

.hint-word {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 4px 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-color, #3b82f6);
  background: rgba(59, 130, 246, 0.1);
  border-radius: 4px;
  border: 1px solid rgba(59, 130, 246, 0.3);
  transition: all var(--duration-normal, 0.3s) ease;

  &.word-clicked {
    color: #ffffff;
    background: var(--primary-color, #3b82f6);
    border-color: var(--primary-color, #3b82f6);
  }
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
  cursor: crosshair;

  &.is-refreshing {
    cursor: not-allowed;

    .captcha-bg-image {
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

// 已点击标记点容器
.clicked-points-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

// 点击标记点
.click-point-marker {
  position: absolute;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translate(-50%, -50%);
  background: var(--primary-color, #3b82f6);
  border-radius: 50%;
  border: 2px solid #ffffff;
  box-shadow:
    0 2px 8px rgba(59, 130, 246, 0.4),
    0 0 0 2px rgba(59, 130, 246, 0.2);
  z-index: 10;
  animation: point-pop-in 0.3s var(--ease-out-expo, ease);
}

// 标记点序号
.point-number {
  font-size: 12px;
  font-weight: 600;
  color: #ffffff;
  line-height: 1;
}

// 操作按钮区域
.action-buttons-area {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 310px;
}

// 确认按钮
.confirm-btn {
  flex: 1;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  background: var(--gradient-primary, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--duration-normal, 0.3s) ease;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow:
      var(--btn-hover-glow, 0 0 20px rgba(59, 130, 246, 0.4)),
      0 4px 12px rgba(102, 126, 234, 0.4);
  }

  &:active:not(:disabled) {
    transform: translateY(0) scale(0.98);
    box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }
}

// 刷新按钮
.refresh-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--border-color-base, #e2e8f0);
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--duration-normal, 0.3s) ease;

  &:hover:not(:disabled) {
    background: var(--primary-color, #3b82f6);
    border-color: var(--primary-color, #3b82f6);

    .refresh-btn-icon {
      color: #ffffff;
    }
  }

  &:active:not(:disabled) {
    transform: scale(0.95);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

// 刷新按钮图标
.refresh-btn-icon {
  width: 18px;
  height: 18px;
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

@keyframes point-pop-in {
  0% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0);
  }
  60% {
    transform: translate(-50%, -50%) scale(1.2);
  }
  100% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
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

// 点击标记点过渡动画
.point-pop-enter-active {
  animation: point-pop-in 0.3s var(--ease-out-expo, ease);
}

.point-pop-leave-active {
  animation: point-pop-out 0.2s ease;
}

@keyframes point-pop-out {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0);
  }
}

// ========== 深色模式适配 ==========

:root.dark,
html.dark,
html[data-theme='dark'] {
  .word-hint-area {
    background: var(--bg-color-page, #0f172a);
    border-color: var(--border-color-base, #334155);
  }

  .hint-text {
    color: var(--text-color-primary, #f1f5f9);
  }

  .hint-word {
    color: var(--primary-color, #3b82f6);
    background: rgba(59, 130, 246, 0.2);
    border-color: rgba(59, 130, 246, 0.4);

    &.word-clicked {
      color: #ffffff;
      background: var(--primary-color, #3b82f6);
      border-color: var(--primary-color, #3b82f6);
    }
  }

  .captcha-image-wrapper {
    background: var(--bg-color-page, #0f172a);
    border-color: var(--border-color-base, #334155);
  }

  .click-point-marker {
    background: var(--primary-color, #3b82f6);
    border-color: var(--border-color-lighter, #e2e8f0);
    box-shadow:
      0 2px 8px rgba(59, 130, 246, 0.5),
      0 0 0 2px rgba(59, 130, 246, 0.3);
  }

  .confirm-btn {
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);

    &:hover:not(:disabled) {
      box-shadow:
        0 0 20px rgba(59, 130, 246, 0.5),
        0 4px 12px rgba(102, 126, 234, 0.5);
    }
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