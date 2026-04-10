# 验证码组件重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 CaptchaSlider 组件拆分为入口组件 + composable + 三个子组件，提高可维护性。

**Architecture:** 三层架构 - 入口组件组合触发器和弹窗，useCaptchaCore 集中管理状态，子组件负责纯 UI 层。

**Tech Stack:** Vue 3 Composition API, TypeScript, Element Plus, SCSS

---

## File Structure

```
src/components/CaptchaSlider/
├── index.vue                    # 入口组合组件（重构）
├── types.ts                     # 类型定义（更新）
├── composables/
│   └── useCaptchaCore.ts        # 核心状态和逻辑（新建）
└── components/
    ├── CaptchaDialog.vue        # 弹窗容器组件（新建）
    ├── BlockPuzzle.vue          # 滑块拼图验证（新建）
    └── ClickWord.vue            # 点选文字验证（新建）
```

---

## Task 1: 更新类型定义

**Files:**
- Modify: `src/components/CaptchaSlider/types.ts`

- [ ] **Step 1: 添加子组件 Props 和 Emits 类型**

在 `types.ts` 文件末尾添加以下类型定义：

```typescript
// ========== 子组件 Props ==========

/**
 * CaptchaDialog 组件 Props 类型
 */
export interface CaptchaDialogProps {
  modelValue: boolean
  captchaData: CaptchaData
  currentCaptchaType: CaptchaType | 'loading'
  loading: boolean
  isRefreshing: boolean
  imageWidth: number
  imageHeight: number
  sliderMaxDistance: number
  sliderLeft: number
  jigsawLeft: number
  clickedPoints: ClickPoint[]
  locale: CaptchaLocale
  dialogTitle: string
  dialogWidth: string | number
  sliderHint: string
  clickWordHint: string
  sliderClass: string
  dialogClass: string
}

/**
 * BlockPuzzle 组件 Props 类型
 */
export interface BlockPuzzleProps {
  captchaData: CaptchaData
  imageWidth: number
  imageHeight: number
  sliderMaxDistance: number
  sliderLeft: number
  jigsawLeft: number
  isRefreshing: boolean
  locale: CaptchaLocale
  sliderClass: string
}

/**
 * ClickWord 组件 Props 类型
 */
export interface ClickWordProps {
  captchaData: CaptchaData
  imageWidth: number
  imageHeight: number
  clickedPoints: ClickPoint[]
  isRefreshing: boolean
  locale: CaptchaLocale
}

// ========== 子组件 Emits ==========

/**
 * CaptchaDialog 组件 Emits 类型
 */
export interface CaptchaDialogEmits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'refresh'): void
  (e: 'image-load'): void
  (e: 'start-drag', event: MouseEvent | TouchEvent): void
  (e: 'on-drag', event: MouseEvent | TouchEvent): void
  (e: 'stop-drag'): void
  (e: 'word-click', event: MouseEvent): void
  (e: 'open'): void
  (e: 'closed'): void
}

/**
 * BlockPuzzle 组件 Emits 类型
 */
export interface BlockPuzzleEmits {
  (e: 'refresh'): void
  (e: 'image-load'): void
  (e: 'start-drag', event: MouseEvent | TouchEvent): void
  (e: 'on-drag', event: MouseEvent | TouchEvent): void
  (e: 'stop-drag'): void
}

/**
 * ClickWord 组件 Emits 类型
 */
export interface ClickWordEmits {
  (e: 'refresh'): void
  (e: 'image-load'): void
  (e: 'word-click', event: MouseEvent): void
}

// ========== Composable 类型 ==========

/**
 * useCaptchaCore composable 选项
 */
export interface UseCaptchaCoreOptions {
  captchaType: CaptchaType
  getCaptchaApi?: (params: CaptchaRequestParams) => Promise<CaptchaData>
  checkCaptchaApi?: (params: CaptchaCheckParams) => Promise<CaptchaCheckResult>
  imageWidth: number
  imageHeight: number
  sliderMaxDistance: number
  locale: CaptchaLocale
  autoCloseOnSuccess: boolean
  onSuccess: (result: CaptchaCheckResult) => void
  onFail: (result: CaptchaCheckResult) => void
}

/**
 * useCaptchaCore composable 返回值
 */
export interface UseCaptchaCoreReturn {
  // 状态
  captchaData: Ref<CaptchaData>
  currentCaptchaType: Ref<CaptchaType | 'loading'>
  loading: Ref<boolean>
  isRefreshing: Ref<boolean>
  imageLoaded: Ref<boolean>
  clientUid: Ref<string>

  // 滑块专用状态
  sliderLeft: Ref<number>
  jigsawLeft: Ref<number>

  // 点选专用状态
  clickedPoints: Ref<ClickPoint[]>

  // 核心方法
  fetchCaptcha: () => Promise<void>
  refreshCaptcha: () => Promise<void>
  submitSliderCaptcha: () => Promise<void>
  submitWordCaptcha: () => Promise<void>

  // 工具方法
  formatImageData: (base64: string) => string
  handleImageLoad: () => void

  // 拖动方法
  startDrag: (e: MouseEvent | TouchEvent) => void
  onDrag: (e: MouseEvent | TouchEvent) => void
  stopDrag: () => Promise<void>

  // 点击方法
  handleWordClick: (e: MouseEvent) => void

  // 清理方法
  cleanup: () => void
}
```

需要在文件顶部添加 `Ref` 类型的导入：

```typescript
import type { Ref } from 'vue'
```

- [ ] **Step 2: 验证类型文件**

运行: `cd /home/binblink/project/blink/frontend/packages/components && npx tsc --noEmit src/components/CaptchaSlider/types.ts 2>&1 | head -20`

预期: 无错误输出

- [ ] **Step 3: 提交类型更新**

```bash
git add src/components/CaptchaSlider/types.ts
git commit -m "feat(captcha): 添加子组件和 composable 类型定义

- 新增 CaptchaDialogProps/Emits
- 新增 BlockPuzzleProps/Emits
- 新增 ClickWordProps/Emits
- 新增 UseCaptchaCoreOptions/Return

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 创建 useCaptchaCore Composable

**Files:**
- Create: `src/components/CaptchaSlider/composables/useCaptchaCore.ts`

- [ ] **Step 1: 创建目录和文件**

```bash
mkdir -p /home/binblink/project/blink/frontend/packages/components/src/components/CaptchaSlider/composables
```

- [ ] **Step 2: 编写 composable 代码**

```typescript
// src/components/CaptchaSlider/composables/useCaptchaCore.ts
import { ref, onUnmounted, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  CaptchaType,
  CaptchaData,
  CaptchaCheckResult,
  ClickPoint,
  CaptchaLocale,
  UseCaptchaCoreOptions,
  UseCaptchaCoreReturn,
} from '../types'

/**
 * 生成 UUID
 */
function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/**
 * 验证码核心逻辑 Composable
 */
export function useCaptchaCore(options: UseCaptchaCoreOptions): UseCaptchaCoreReturn {
  const {
    captchaType,
    getCaptchaApi,
    checkCaptchaApi,
    imageWidth,
    imageHeight,
    sliderMaxDistance,
    locale,
    autoCloseOnSuccess,
    onSuccess,
    onFail,
  } = options

  // ========== 状态 ==========

  const captchaData = ref<CaptchaData>({})
  const currentCaptchaType = ref<CaptchaType | 'loading'>('loading')
  const loading = ref(false)
  const isRefreshing = ref(false)
  const imageLoaded = ref(false)
  const clientUid = ref('')

  // 滑块专用状态
  const sliderLeft = ref(0)
  const jigsawLeft = ref(0)

  // 点选专用状态
  const clickedPoints = ref<ClickPoint[]>([])

  // 拖动状态（非响应式）
  let isDragging = false
  let startX = 0
  let startLeft = 0
  let animationId: number | null = null
  let currentSliderLeft = 0

  // ========== 工具方法 ==========

  const formatImageData = (base64: string): string => {
    return base64.startsWith('data:') ? base64 : `data:image/png;base64,${base64}`
  }

  const handleImageLoad = (): void => {
    imageLoaded.value = true
  }

  // ========== 核心方法 ==========

  const fetchCaptcha = async (): Promise<void> => {
    if (!getCaptchaApi) {
      console.warn('CaptchaSlider: getCaptchaApi is not provided')
      return
    }

    try {
      if (!isRefreshing.value) {
        currentCaptchaType.value = 'loading'
      }
      imageLoaded.value = false
      clientUid.value = generateUUID()

      const data = await getCaptchaApi({
        captchaType: captchaType,
        clientUid: clientUid.value,
        ts: Date.now(),
      })

      captchaData.value = data || {}

      console.log('[CaptchaSlider] Fetch - captchaType:', data?.captchaType)
      console.log('[CaptchaSlider] Fetch - captchaId:', data?.captchaId)

      currentCaptchaType.value = (data?.captchaType as CaptchaType) || 'blockPuzzle'

      if (data?.pointJson) {
        try {
          const point = JSON.parse(data.pointJson)
          console.log('[CaptchaSlider] Fetch - parsed pointJson:', point)
          captchaData.value.backendY = point.y ?? 0
        } catch (e) {
          console.warn('[CaptchaSlider] Fetch - Failed to parse pointJson:', e)
          captchaData.value.backendY = 0
        }
      }

      jigsawLeft.value = 0
      sliderLeft.value = 0
      clickedPoints.value = []

      setTimeout(() => {
        isRefreshing.value = false
      }, 100)
    } catch (error) {
      ElMessage.error(locale.captchaLoadFailed)
      currentCaptchaType.value = 'loading'
      isRefreshing.value = false
    }
  }

  const refreshCaptcha = async (): Promise<void> => {
    isRefreshing.value = true
    await fetchCaptcha()
  }

  const handleCheckResult = (result: CaptchaCheckResult): void => {
    if (result?.result) {
      onSuccess(result)
      ElMessage.success(locale.captchaSuccess)
    } else {
      onFail(result)
      ElMessage.error(result?.msg || locale.captchaFailed)
      clickedPoints.value = []
      refreshCaptcha()
    }
  }

  const submitSliderCaptcha = async (): Promise<void> => {
    if (!checkCaptchaApi) {
      console.warn('CaptchaSlider: checkCaptchaApi is not provided')
      return
    }

    try {
      const captchaTypeValue = captchaData.value.captchaType || 'blockPuzzle'
      const backendY = captchaData.value.backendY ?? 0
      const pointJson = JSON.stringify({ x: jigsawLeft.value, y: backendY })
      const captchaId = captchaData.value.captchaId || captchaData.value.token

      console.log('[CaptchaSlider] Submit - captchaId:', captchaId)
      console.log('[CaptchaSlider] Submit - x:', jigsawLeft.value, ', y:', backendY)

      const result = await checkCaptchaApi({
        captchaId: captchaId,
        captchaType: captchaTypeValue,
        pointJson: pointJson,
        clientUid: clientUid.value,
        ts: Date.now(),
      })

      handleCheckResult(result)
    } catch (error) {
      ElMessage.error(locale.captchaFailed)
      await refreshCaptcha()
    }
  }

  const submitWordCaptcha = async (): Promise<void> => {
    if (clickedPoints.value.length === 0) {
      ElMessage.warning(locale.pleaseClickWords)
      return
    }

    if (!checkCaptchaApi) {
      console.warn('CaptchaSlider: checkCaptchaApi is not provided')
      return
    }

    if (loading.value) return
    loading.value = true

    try {
      const pointJson = JSON.stringify(clickedPoints.value.map((p) => ({ x: p.x, y: p.y })))
      const captchaId = captchaData.value.captchaId || captchaData.value.token

      console.log('[CaptchaSlider] Word Submit - captchaId:', captchaId)
      console.log('[CaptchaSlider] Word Submit - points:', clickedPoints.value)

      const result = await checkCaptchaApi({
        captchaId: captchaId,
        captchaType: captchaData.value.captchaType,
        pointJson: pointJson,
        clientUid: clientUid.value,
        ts: Date.now(),
      })

      handleCheckResult(result)
    } catch (error) {
      ElMessage.error(locale.captchaFailed)
      clickedPoints.value = []
      await refreshCaptcha()
    } finally {
      loading.value = false
    }
  }

  // ========== 拖动方法 ==========

  const onDrag = (e: MouseEvent | TouchEvent): void => {
    if (!isDragging) return
    e.preventDefault()

    const currentX = 'touches' in e ? (e.touches[0]?.clientX ?? 0) : e.clientX
    const diff = currentX - startX
    const maxLeft = sliderMaxDistance
    const newLeft = Math.max(0, Math.min(startLeft + diff, maxLeft))

    if (newLeft !== currentSliderLeft) {
      currentSliderLeft = newLeft
      if (animationId) {
        cancelAnimationFrame(animationId)
      }
      animationId = requestAnimationFrame(() => {
        sliderLeft.value = currentSliderLeft
        jigsawLeft.value = currentSliderLeft
      })
    }
  }

  const startDrag = (e: MouseEvent | TouchEvent): void => {
    e.preventDefault()

    const clientX = 'touches' in e ? (e.touches[0]?.clientX ?? 0) : e.clientX

    isDragging = true
    startX = clientX
    startLeft = sliderLeft.value
    currentSliderLeft = sliderLeft.value

    document.addEventListener('mousemove', onDrag)
    document.addEventListener('mouseup', stopDrag)
    document.addEventListener('touchmove', onDrag)
    document.addEventListener('touchend', stopDrag)
  }

  const stopDrag = async (): Promise<void> => {
    if (!isDragging) return
    isDragging = false

    if (animationId) {
      cancelAnimationFrame(animationId)
      animationId = null
    }

    document.removeEventListener('mousemove', onDrag)
    document.removeEventListener('mouseup', stopDrag)
    document.removeEventListener('touchmove', onDrag)
    document.removeEventListener('touchend', stopDrag)

    await submitSliderCaptcha()
  }

  // ========== 点击方法 ==========

  const handleWordClick = async (e: MouseEvent): void => {
    if (currentCaptchaType.value !== 'clickWord') return
    if (loading.value) return

    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    clickedPoints.value.push({ x, y })

    const requiredCount = captchaData.value.wordList?.length || 0

    if (clickedPoints.value.length >= requiredCount && requiredCount > 0) {
      await submitWordCaptcha()
    }
  }

  // ========== 清理方法 ==========

  const cleanup = (): void => {
    document.removeEventListener('mousemove', onDrag)
    document.removeEventListener('mouseup', stopDrag)
    document.removeEventListener('touchmove', onDrag)
    document.removeEventListener('touchend', stopDrag)
  }

  onUnmounted(() => {
    cleanup()
  })

  return {
    // 状态
    captchaData,
    currentCaptchaType,
    loading,
    isRefreshing,
    imageLoaded,
    clientUid,
    sliderLeft,
    jigsawLeft,
    clickedPoints,

    // 核心方法
    fetchCaptcha,
    refreshCaptcha,
    submitSliderCaptcha,
    submitWordCaptcha,

    // 工具方法
    formatImageData,
    handleImageLoad,

    // 拖动方法
    startDrag,
    onDrag,
    stopDrag,

    // 点击方法
    handleWordClick,

    // 清理方法
    cleanup,
  }
}
```

- [ ] **Step 3: 提交 composable**

```bash
git add src/components/CaptchaSlider/composables/useCaptchaCore.ts
git commit -m "feat(captcha): 创建 useCaptchaCore composable

- 集中管理验证码状态
- 封装 fetch/submit 核心逻辑
- 封装拖动和点击交互方法

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 创建 BlockPuzzle 子组件

**Files:**
- Create: `src/components/CaptchaSlider/components/BlockPuzzle.vue`

- [ ] **Step 1: 创建目录**

```bash
mkdir -p /home/binblink/project/blink/frontend/packages/components/src/components/CaptchaSlider/components
```

- [ ] **Step 2: 编写 BlockPuzzle.vue**

```vue
<!-- src/components/CaptchaSlider/components/BlockPuzzle.vue -->
<template>
  <div class="block-puzzle-captcha">
    <!-- 图片容器 -->
    <div
      class="captcha-image-wrapper"
      :style="{ width: `${imageWidth}px`, height: `${imageHeight}px` }"
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
          <span class="refresh-text">刷新中...</span>
        </div>
      </transition>

      <!-- 图片 -->
      <transition name="image-fade">
        <img
          v-if="captchaData.originalImageBase64 && !isRefreshing"
          :src="formatImageData(captchaData.originalImageBase64)"
          class="captcha-bg-image"
          alt="验证码背景"
          @load="$emit('image-load')"
        />
      </transition>
      <img
        v-if="captchaData.jigsawImageBase64 && !isRefreshing"
        :src="formatImageData(captchaData.jigsawImageBase64)"
        class="captcha-jigsaw-image"
        :style="{ left: jigsawLeft + 'px' }"
        alt="滑块"
      />
    </div>

    <!-- 滑块轨道 -->
    <div class="slider-container" :class="sliderClass">
      <div class="slider-track">
        <div class="slider-fill" :style="{ width: sliderLeft + 36 + 'px' }"></div>
      </div>
      <div
        class="slider-thumb"
        :style="{ transform: `translateX(${sliderLeft}px)` }"
        @mousedown="$emit('start-drag', $event)"
        @touchstart="$emit('start-drag', $event)"
      >
        <svg viewBox="0 0 24 24" class="slider-arrow">
          <path
            d="M8.59 16.59L13.17 12L8.59 7.41L10 6L16 12L10 18L8.59 16.59Z"
            fill="currentColor"
          />
        </svg>
      </div>
      <span class="slider-hint">{{ locale.dragToVerify }}</span>

      <!-- 刷新按钮 -->
      <button class="refresh-btn" @click="$emit('refresh')" :disabled="isRefreshing">
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
import { computed } from 'vue'
import type { BlockPuzzleProps, BlockPuzzleEmits, CaptchaData, CaptchaLocale } from '../types'

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

defineEmits<BlockPuzzleEmits>()

// 格式化图片数据
const formatImageData = (base64: string): string => {
  return base64.startsWith('data:') ? base64 : `data:image/png;base64,${base64}`
}
</script>

<style scoped lang="scss">
.block-puzzle-captcha {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.captcha-image-wrapper {
  position: relative;
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

.captcha-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition:
    opacity 0.3s ease,
    filter 0.3s ease,
    transform 0.3s ease;
}

.captcha-jigsaw-image {
  position: absolute;
  top: 0;
  left: 0;
  height: auto;
  max-height: 100%;
  width: auto;
  object-fit: contain;
  transition:
    opacity 0.3s ease,
    filter 0.3s ease;
}

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

.slider-track {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--border-color-lighter, #f8fafc);
  border-radius: 8px;
}

.slider-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.2) 0%, rgba(59, 130, 246, 0.1) 100%);
  border-radius: 8px;
  transition: width 0.05s ease-out;
}

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
    box-shadow: var(--btn-hover-glow, 0 0 25px rgba(59, 130, 246, 0.5));
  }
}

.slider-arrow {
  width: 18px;
  height: 18px;
}

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

// 刷新遮罩层
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

.refresh-btn-icon {
  width: 16px;
  height: 16px;
  color: var(--text-color-secondary, #64748b);
  transition: color var(--duration-normal, 0.3s) ease;

  &.is-spinning {
    animation: rotate 1s linear infinite;
  }
}

// 动画
@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

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

// 深色模式
:root.dark,
html.dark,
html[data-theme='dark'] {
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

  .captcha-image-wrapper {
    background: var(--bg-color-page, #0f172a);
    border-color: var(--border-color-base, #334155);
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
```

- [ ] **Step 3: 提交 BlockPuzzle 组件**

```bash
git add src/components/CaptchaSlider/components/BlockPuzzle.vue
git commit -m "feat(captcha): 创建 BlockPuzzle 滑块验证子组件

- 纯 UI 层，显示背景图、拼图块、滑块轨道
- 绑定拖动事件，触发父组件处理
- 包含刷新按钮和加载状态

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 创建 ClickWord 子组件

**Files:**
- Create: `src/components/CaptchaSlider/components/ClickWord.vue`

- [ ] **Step 1: 编写 ClickWord.vue**

```vue
<!-- src/components/CaptchaSlider/components/ClickWord.vue -->
<template>
  <div class="click-word-captcha">
    <!-- 文字提示 -->
    <div class="word-hint">
      {{ locale.clickWordHint }}:
      <span class="words">{{ captchaData.wordList?.join(', ') }}</span>
    </div>

    <!-- 图片容器 -->
    <div
      class="captcha-image-wrapper"
      :style="{ width: `${imageWidth}px`, height: `${imageHeight}px` }"
      :class="{ 'is-refreshing': isRefreshing }"
      @click="$emit('word-click', $event)"
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
          <span class="refresh-text">刷新中...</span>
        </div>
      </transition>

      <!-- 图片 -->
      <transition name="image-fade">
        <img
          v-if="captchaData.originalImageBase64 && !isRefreshing"
          :src="formatImageData(captchaData.originalImageBase64)"
          class="captcha-bg-image"
          alt="验证码"
          @load="$emit('image-load')"
        />
      </transition>

      <!-- 点击标记点 -->
      <div
        v-for="(point, index) in clickedPoints"
        :key="index"
        class="click-point"
        :style="{ left: point.x + 'px', top: point.y + 'px' }"
      >
        {{ index + 1 }}
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="click-actions">
      <el-button type="primary" @click="handleConfirm">{{ locale.confirm }}</el-button>
      <el-button @click="$emit('refresh')" :disabled="isRefreshing">
        <svg viewBox="0 0 24 24" class="btn-icon" :class="{ 'is-spinning': isRefreshing }">
          <path
            d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"
            fill="currentColor"
          />
        </svg>
        {{ locale.refresh }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ClickWordProps, ClickWordEmits } from '../types'

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

// 格式化图片数据
const formatImageData = (base64: string): string => {
  return base64.startsWith('data:') ? base64 : `data:image/png;base64,${base64}`
}

// 确认按钮（手动提交）
const handleConfirm = () => {
  // 这里需要触发父组件的提交方法
  // 由于 composable 已经在父组件中，这里通过事件传递
  emit('word-click', { type: 'submit' } as any)
}
</script>

<style scoped lang="scss">
.click-word-captcha {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.word-hint {
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-primary, #1e293b);
  text-align: center;
  padding: 10px 16px;
  background: var(--bg-color-page, #f1f5f9);
  border-radius: 8px;
  border: 1px solid var(--border-color-light, #f1f5f9);

  .words {
    color: var(--primary-color, #3b82f6);
    font-weight: 600;
    margin-left: 4px;
  }
}

.captcha-image-wrapper {
  position: relative;
  background: var(--bg-color-page, #f1f5f9);
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color-light, #f1f5f9);
  cursor: crosshair;

  &.is-refreshing {
    .captcha-bg-image {
      opacity: 0.3;
      filter: blur(2px);
    }
  }
}

.captcha-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition:
    opacity 0.3s ease,
    filter 0.3s ease,
    transform 0.3s ease;
}

.click-point {
  position: absolute;
  width: 26px;
  height: 26px;
  background: var(--gradient-primary, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: 600;
  transform: translate(-50%, -50%);
  animation: point-appear 0.3s var(--ease-out-back, cubic-bezier(0.34, 1.56, 0.64, 1));
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
  pointer-events: none;
}

@keyframes point-appear {
  0% {
    transform: translate(-50%, -50%) scale(0);
    opacity: 0;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.3);
  }
  100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
}

.click-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;

  :deep(.el-button) {
    border-radius: 8px;
    padding: 10px 24px;
    font-size: 14px;
    font-weight: 500;
    transition: all var(--duration-normal, 0.3s) var(--ease-out-expo, ease);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
    }

    &:active {
      transform: translateY(0);
    }
  }
}

.btn-icon {
  width: 14px;
  height: 14px;
  margin-right: 6px;

  &.is-spinning {
    animation: rotate 1s linear infinite;
  }
}

// 刷新遮罩层
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

// 动画
@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

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

// 深色模式
:root.dark,
html.dark,
html[data-theme='dark'] {
  .word-hint {
    color: var(--text-color-primary, #f1f5f9);
    background: var(--bg-color-page, #0f172a);
    border-color: var(--border-color-base, #334155);

    .words {
      color: var(--primary-color-light, #60a5fa);
    }
  }

  .captcha-image-wrapper {
    background: var(--bg-color-page, #0f172a);
    border-color: var(--border-color-base, #334155);
  }

  .refresh-overlay {
    background: rgba(15, 23, 42, 0.85);
  }
}
</style>
```

- [ ] **Step 2: 提交 ClickWord 组件**

```bash
git add src/components/CaptchaSlider/components/ClickWord.vue
git commit -m "feat(captcha): 创建 ClickWord 点选验证子组件

- 纯 UI 层，显示图片、文字提示、点击标记点
- 绑定点击事件，触发父组件处理
- 包含确认和刷新按钮

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 创建 CaptchaDialog 子组件

**Files:**
- Create: `src/components/CaptchaSlider/components/CaptchaDialog.vue`

- [ ] **Step 1: 编写 CaptchaDialog.vue**

```vue
<!-- src/components/CaptchaSlider/components/CaptchaDialog.vue -->
<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    :width="computedDialogWidth"
    :close-on-click-modal="false"
    :class="['captcha-dialog', dialogClass]"
    @update:model-value="$emit('update:modelValue', $event)"
    @open="$emit('open')"
    @closed="$emit('closed')"
  >
    <div class="captcha-container">
      <!-- 加载状态 -->
      <div v-if="currentCaptchaType === 'loading'" class="captcha-loading">
        <svg viewBox="0 0 24 24" class="loading-icon">
          <path d="M12 4V2C6.48 2 2 6.48 2 12H4C4 7.58 7.58 4 12 4Z" fill="currentColor" />
        </svg>
        <span>{{ locale.loading }}</span>
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
        @refresh="$emit('refresh')"
        @image-load="$emit('image-load')"
        @start-drag="$emit('start-drag', $event)"
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
        @refresh="$emit('refresh')"
        @image-load="$emit('image-load')"
        @word-click="$emit('word-click', $event)"
      />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CaptchaDialogProps, CaptchaDialogEmits } from '../types'
import BlockPuzzle from './BlockPuzzle.vue'
import ClickWord from './ClickWord.vue'

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
  dialogWidth: '400px',
  sliderHint: '',
  clickWordHint: '',
  sliderClass: '',
  dialogClass: '',
})

defineEmits<CaptchaDialogEmits>()

// 弹窗宽度
const computedDialogWidth = computed(() => {
  if (typeof props.dialogWidth === 'number') {
    return `${props.dialogWidth}px`
  }
  return props.dialogWidth
})
</script>

<style scoped lang="scss">
.captcha-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: fit-content;
}

// 弹窗样式
.captcha-dialog {
  :deep(.el-dialog__body) {
    padding: 20px 24px;
    display: flex;
    justify-content: center;
    background: var(--bg-color-card);
    border-radius: 0 0 16px 16px;
  }

  :deep(.el-dialog__header) {
    background: var(--bg-color-card);
    border-bottom: 1px solid var(--border-color-light);
  }

  :deep(.el-dialog__title) {
    color: var(--text-color-primary);
  }
}

// 加载状态
.captcha-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 0;

  .loading-icon {
    width: 36px;
    height: 36px;
    color: var(--primary-color, #3b82f6);
    animation: rotate 1s linear infinite;
  }

  span {
    color: var(--text-color-secondary, #64748b);
    font-size: 14px;
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// 深色模式
:root.dark,
html.dark,
html[data-theme='dark'] {
  .captcha-loading span {
    color: var(--text-color-secondary, #94a3b8);
  }

  .captcha-dialog {
    :deep(.el-dialog) {
      background-color: var(--bg-color-card, #1e293b);
      border-color: var(--border-color-base, #334155);
    }

    :deep(.el-dialog__header) {
      background: var(--bg-color-card, #1e293b);
      border-bottom-color: var(--border-color-base, #334155);
    }

    :deep(.el-dialog__title) {
      color: var(--text-color-primary, #f1f5f9);
    }

    :deep(.el-dialog__body) {
      background: var(--bg-color-card, #1e293b);
    }
  }
}
</style>
```

- [ ] **Step 2: 提交 CaptchaDialog 组件**

```bash
git add src/components/CaptchaSlider/components/CaptchaDialog.vue
git commit -m "feat(captcha): 创建 CaptchaDialog 弹窗容器子组件

- 包装 el-dialog，根据验证类型切换子组件
- 转发子组件事件到父组件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 6: 重构入口组件 index.vue

**Files:**
- Modify: `src/components/CaptchaSlider/index.vue`

- [ ] **Step 1: 重写 index.vue**

完全替换文件内容为：

```vue
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

    <!-- 验证码弹窗 -->
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
      :dialog-width="dialogWidth"
      :slider-hint="sliderHint"
      :click-word-hint="clickWordHint"
      :slider-class="sliderClass"
      :dialog-class="dialogClass"
      @refresh="handleRefresh"
      @image-load="handleImageLoad"
      @start-drag="handleStartDrag"
      @word-click="handleWordClick"
      @open="handleDialogOpen"
      @closed="handleDialogClosed"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  CaptchaSliderProps,
  CaptchaSliderEmits,
  CaptchaCheckResult,
  CaptchaLocale,
} from './types'
import { defaultZhCnLocale } from './types'
import { useCaptchaCore } from './composables/useCaptchaCore'
import CaptchaDialog from './components/CaptchaDialog.vue'

defineOptions({
  name: 'CaptchaSlider',
})

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

const emit = defineEmits<CaptchaSliderEmits>()

// ========== 状态 ==========

const dialogVisible = ref(false)

// 国际化文本
const localeTexts = computed<CaptchaLocale>(() => props.locale || defaultZhCnLocale)

// ========== 使用 composable ==========

const {
  captchaData,
  currentCaptchaType,
  loading,
  isRefreshing,
  imageLoaded,
  sliderLeft,
  jigsawLeft,
  clickedPoints,
  fetchCaptcha,
  refreshCaptcha,
  formatImageData,
  handleImageLoad: onImageLoad,
  startDrag,
  handleWordClick: onWordClick,
  cleanup,
} = useCaptchaCore({
  captchaType: props.captchaType,
  getCaptchaApi: props.getCaptchaApi,
  checkCaptchaApi: props.checkCaptchaApi,
  imageWidth: props.imageWidth,
  imageHeight: props.imageHeight,
  sliderMaxDistance: props.sliderMaxDistance,
  locale: localeTexts.value,
  autoCloseOnSuccess: props.autoCloseOnSuccess,
  onSuccess: (result: CaptchaCheckResult) => {
    emit('success', result)
    if (props.autoCloseOnSuccess) {
      dialogVisible.value = false
    }
  },
  onFail: (result: CaptchaCheckResult) => {
    emit('fail', result)
  },
})

// ========== 事件处理 ==========

const handleTriggerClick = () => {
  if (props.disabled || props.verified) return
  emit('trigger-click')
  dialogVisible.value = true
}

const handleDialogOpen = async () => {
  emit('open')
  clickedPoints.value = []
  await fetchCaptcha()
}

const handleDialogClosed = () => {
  emit('close')
}

const handleRefresh = async () => {
  emit('refresh')
  await refreshCaptcha()
}

const handleImageLoad = () => {
  onImageLoad()
}

const handleStartDrag = (e: MouseEvent | TouchEvent) => {
  if (props.verified) return
  startDrag(e)
}

const handleWordClick = (e: MouseEvent) => {
  onWordClick(e)
}

// ========== 监听器 ==========

watch(
  () => props.verified,
  (newVal) => {
    if (newVal && props.autoCloseOnSuccess) {
      dialogVisible.value = false
    }
  }
)

// ========== 公开方法 ==========

defineExpose({
  open: () => {
    if (!props.disabled && !props.verified) {
      dialogVisible.value = true
    }
  },
  close: () => {
    dialogVisible.value = false
  },
  refresh: refreshCaptcha,
  reset: () => {
    emit('update:verified', false)
    dialogVisible.value = false
    clickedPoints.value = []
    jigsawLeft.value = 0
    sliderLeft.value = 0
  },
  getVerificationData: () => captchaData.value,
})
</script>

<style scoped lang="scss">
.captcha-slider-wrapper {
  width: 100%;
}

// 触发器样式
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

.trigger-icon {
  width: 18px;
  height: 18px;

  &.success-icon {
    width: 16px;
    height: 16px;
  }
}

// 深色模式
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
```

- [ ] **Step 2: 提交重构后的入口组件**

```bash
git add src/components/CaptchaSlider/index.vue
git commit -m "refactor(captcha): 重构入口组件使用 composable 和子组件

- 使用 useCaptchaCore 管理状态
- 使用 CaptchaDialog 子组件
- 保持原有 Props/Emits/公开方法不变
- 简化代码从 1340 行到约 280 行

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 7: 验证和最终提交

- [ ] **Step 1: 检查 TypeScript 编译**

```bash
cd /home/binblink/project/blink/frontend/packages/components && npx vue-tsc --noEmit 2>&1 | head -50
```

预期: 无编译错误

- [ ] **Step 2: 启动开发服务器测试**

```bash
cd /home/binblink/project/blink/frontend/packages/components && npm run dev
```

访问组件演示页面验证功能。

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "feat(captcha): 完成验证码组件重构

拆分 CaptchaSlider 为三层架构：
- useCaptchaCore composable: 集中管理状态和逻辑
- CaptchaDialog: 弹窗容器组件
- BlockPuzzle: 滑块拼图验证组件
- ClickWord: 点选文字验证组件

保持向后兼容，入口组件 Props/Emits/公开方法不变

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```