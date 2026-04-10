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
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle || localeTexts.captchaTitle"
      :width="computedDialogWidth"
      :close-on-click-modal="false"
      :class="['captcha-dialog', dialogClass]"
      @open="handleDialogOpen"
      @closed="handleDialogClosed"
    >
      <div class="captcha-container">
        <!-- 滑块验证码 -->
        <div v-if="currentCaptchaType === 'blockPuzzle'" class="block-puzzle-captcha">
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
                <span class="refresh-text">{{ localeTexts.refreshing || '刷新中...' }}</span>
              </div>
            </transition>
            <!-- 图片容器 -->
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
            <img
              v-if="captchaData.jigsawImageBase64 && !isRefreshing"
              :src="formatImageData(captchaData.jigsawImageBase64)"
              class="captcha-jigsaw-image"
              :class="{ 'image-loaded': imageLoaded }"
              :style="{ left: jigsawLeft + 'px' }"
              alt="滑块"
            />
          </div>
          <div class="slider-container" :class="sliderClass">
            <div class="slider-track">
              <div class="slider-fill" :style="{ width: sliderLeft + 36 + 'px' }"></div>
            </div>
            <div
              class="slider-thumb"
              :style="{ transform: `translateX(${sliderLeft}px)` }"
              @mousedown="startDrag"
              @touchstart="startDrag"
            >
              <svg viewBox="0 0 24 24" class="slider-arrow">
                <path
                  d="M8.59 16.59L13.17 12L8.59 7.41L10 6L16 12L10 18L8.59 16.59Z"
                  fill="currentColor"
                />
              </svg>
            </div>
            <span class="slider-hint">{{ sliderHint || localeTexts.dragToVerify }}</span>
            <!-- 刷新按钮 -->
            <button class="refresh-btn" @click="refreshCaptcha" :disabled="isRefreshing">
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

        <!-- 点选验证码 -->
        <div v-else-if="currentCaptchaType === 'clickWord'" class="click-word-captcha">
          <div class="word-hint">
            {{ clickWordHint || localeTexts.clickWordHint }}:
            <span class="words">{{ captchaData.wordList?.join(', ') }}</span>
          </div>
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
                <span class="refresh-text">{{ localeTexts.refreshing || '刷新中...' }}</span>
              </div>
            </transition>
            <transition name="image-fade">
              <img
                v-if="captchaData.originalImageBase64 && !isRefreshing"
                :src="formatImageData(captchaData.originalImageBase64)"
                class="captcha-bg-image"
                :class="{ 'image-loaded': imageLoaded }"
                alt="验证码"
                @load="handleImageLoad"
              />
            </transition>
            <div
              v-for="(point, index) in clickedPoints"
              :key="index"
              class="click-point"
              :style="{ left: point.x + 'px', top: point.y + 'px' }"
            >
              {{ index + 1 }}
            </div>
          </div>
          <div class="click-actions">
            <el-button type="primary" @click="submitWordCaptcha">
              {{ localeTexts.confirm }}
            </el-button>
            <el-button @click="refreshCaptcha" :disabled="isRefreshing">
              <svg viewBox="0 0 24 24" class="btn-icon" :class="{ 'is-spinning': isRefreshing }">
                <path
                  d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"
                  fill="currentColor"
                />
              </svg>
              {{ localeTexts.refresh }}
            </el-button>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-else class="captcha-loading">
          <svg viewBox="0 0 24 24" class="loading-icon">
            <path d="M12 4V2C6.48 2 2 6.48 2 12H4C4 7.58 7.58 4 12 4Z" fill="currentColor" />
          </svg>
          <span>{{ localeTexts.loading }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  CaptchaSliderProps,
  CaptchaSliderEmits,
  CaptchaData,
  CaptchaCheckResult,
  ClickPoint,
  CaptchaLocale,
  CaptchaType,
} from './types'
import { defaultZhCnLocale } from './types'

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

// ========== 状态管理 ==========

const dialogVisible = ref(false)
const currentCaptchaType = ref<CaptchaType | 'loading'>('loading')
const captchaData = ref<CaptchaData>({})
const jigsawLeft = ref(0)
const sliderLeft = ref(0)
const clickedPoints = ref<ClickPoint[]>([])
const loading = ref(false)
const clientUid = ref('')
const isRefreshing = ref(false)
const imageLoaded = ref(false)

// ========== 计算属性 ==========

// 国际化文本
const localeTexts = computed<CaptchaLocale>(() => props.locale || defaultZhCnLocale)

// 弹窗宽度
const computedDialogWidth = computed(() => {
  if (typeof props.dialogWidth === 'number') {
    return `${props.dialogWidth}px`
  }
  return props.dialogWidth
})

// 图片容器样式
const imageWrapperStyle = computed(() => ({
  width: `${props.imageWidth}px`,
  height: `${props.imageHeight}px`,
}))

// ========== 工具方法 ==========

// 格式化图片数据
const formatImageData = (base64: string) => {
  return base64.startsWith('data:') ? base64 : `data:image/png;base64,${base64}`
}

// 生成UUID
const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// ========== 验证码相关 ==========

// 获取验证码
const fetchCaptcha = async () => {
  if (!props.getCaptchaApi) {
    console.warn('CaptchaSlider: getCaptchaApi is not provided')
    return
  }

  try {
    // 首次加载显示全屏loading
    if (!isRefreshing.value) {
      currentCaptchaType.value = 'loading'
    }
    imageLoaded.value = false
    clientUid.value = generateUUID()

    const data = await props.getCaptchaApi({
      captchaType: props.captchaType,
      clientUid: clientUid.value,
      ts: Date.now(),
    })

    captchaData.value = data || {}

    // 调试日志 - 输出后端返回的原始数据
    console.log('[CaptchaSlider] Fetch - captchaType:', data?.captchaType)
    console.log('[CaptchaSlider] Fetch - captchaId:', data?.captchaId)
    console.log('[CaptchaSlider] Fetch - token:', data?.token)
    console.log('[CaptchaSlider] Fetch - pointJson:', data?.pointJson)
    console.log(
      '[CaptchaSlider] Fetch - jigsawImageBase64 length:',
      data?.jigsawImageBase64?.length
    )
    console.log(
      '[CaptchaSlider] Fetch - originalImageBase64 length:',
      data?.originalImageBase64?.length
    )

    // 根据返回的类型设置验证码类型
    currentCaptchaType.value = (data?.captchaType as CaptchaType) || 'blockPuzzle'

    // 解析后端返回的 y 坐标（滑块验证码需要）
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

    // 重置状态
    jigsawLeft.value = 0
    sliderLeft.value = 0
    clickedPoints.value = []

    // 延迟关闭刷新状态，让图片有加载时间
    setTimeout(() => {
      isRefreshing.value = false
    }, 100)
  } catch (error) {
    ElMessage.error(localeTexts.value.captchaLoadFailed)
    currentCaptchaType.value = 'loading'
    isRefreshing.value = false
  }
}

// 刷新验证码
const refreshCaptcha = async () => {
  emit('refresh')
  isRefreshing.value = true
  await fetchCaptcha()
}

// 图片加载完成
const handleImageLoad = () => {
  imageLoaded.value = true
}

// 校验滑块验证码
const submitSliderCaptcha = async () => {
  if (!props.checkCaptchaApi) {
    console.warn('CaptchaSlider: checkCaptchaApi is not provided')
    return
  }

  try {
    const captchaTypeValue = captchaData.value.captchaType || 'blockPuzzle'

    // anji-captcha 的 y 坐标在后端校验时需要匹配
    // 使用后端返回的 y 坐标，而不是固定的 0
    const backendY = captchaData.value.backendY ?? 0

    // 重要：jigsawLeft 就是滑块拖动的距离，直接作为 x 坐标
    // 后端期望的是滑块相对于起始位置(0)的偏移量
    const pointJson = JSON.stringify({ x: jigsawLeft.value, y: backendY })

    // captchaId 使用后端返回的 token 字段（anji-captcha 使用 token 作为验证码标识）
    const captchaId = captchaData.value.captchaId || captchaData.value.token

    // 调试日志 - 输出关键参数
    console.log('[CaptchaSlider] Submit - captchaId:', captchaId)
    console.log('[CaptchaSlider] Submit - x:', jigsawLeft.value, ', y:', backendY)
    console.log('[CaptchaSlider] Submit - pointJson:', pointJson)
    console.log('[CaptchaSlider] Submit - imageWidth:', props.imageWidth)
    console.log('[CaptchaSlider] Submit - sliderMaxDistance:', props.sliderMaxDistance)

    const result = await props.checkCaptchaApi({
      captchaId: captchaId,
      captchaType: captchaTypeValue,
      pointJson: pointJson,
      clientUid: clientUid.value,
      ts: Date.now(),
    })

    handleCheckResult(result)
  } catch (error) {
    ElMessage.error(localeTexts.value.captchaFailed)
    await refreshCaptcha()
  }
}

// 校验点选验证码
const submitWordCaptcha = async () => {
  if (clickedPoints.value.length === 0) {
    ElMessage.warning(localeTexts.value.pleaseClickWords)
    return
  }

  if (!props.checkCaptchaApi) {
    console.warn('CaptchaSlider: checkCaptchaApi is not provided')
    return
  }

  // 防止重复提交
  if (loading.value) return
  loading.value = true

  try {
    const pointJson = JSON.stringify(clickedPoints.value.map((p) => ({ x: p.x, y: p.y })))
    const captchaId = captchaData.value.captchaId || captchaData.value.token

    // 调试日志
    console.log('[CaptchaSlider] Word Submit - captchaId:', captchaId)
    console.log('[CaptchaSlider] Word Submit - points:', clickedPoints.value)

    const result = await props.checkCaptchaApi({
      captchaId: captchaId,
      captchaType: captchaData.value.captchaType,
      pointJson: pointJson,
      clientUid: clientUid.value,
      ts: Date.now(),
    })

    handleCheckResult(result)
  } catch (error) {
    ElMessage.error(localeTexts.value.captchaFailed)
    clickedPoints.value = []
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

// 处理校验结果
const handleCheckResult = (result: CaptchaCheckResult) => {
  if (result?.result) {
    // 验证成功
    emit('update:verified', true)
    emit('success', result)
    ElMessage.success(localeTexts.value.captchaSuccess)

    if (props.autoCloseOnSuccess) {
      dialogVisible.value = false
    }
  } else {
    // 验证失败
    emit('fail', result)
    ElMessage.error(result?.msg || localeTexts.value.captchaFailed)

    // 重置状态
    clickedPoints.value = []
    refreshCaptcha()
  }
}

// ========== 滑块拖动 ==========

let isDragging = false
let startX = 0
let startLeft = 0
let animationId: number | null = null
let currentSliderLeft = 0

const startDrag = (e: MouseEvent | TouchEvent) => {
  if (props.verified) return
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

const onDrag = (e: MouseEvent | TouchEvent) => {
  if (!isDragging) return
  e.preventDefault()

  const currentX = 'touches' in e ? (e.touches[0]?.clientX ?? 0) : e.clientX
  // 计算滑块左边应该到达的位置
  const diff = currentX - startX
  const maxLeft = props.sliderMaxDistance
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

const stopDrag = async () => {
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

  // 提交验证
  await submitSliderCaptcha()
}

// ========== 点选验证 ==========

const handleWordClick = async (e: MouseEvent) => {
  if (currentCaptchaType.value !== 'clickWord') return

  // 防止在验证中重复点击
  if (loading.value) return

  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  clickedPoints.value.push({ x, y })

  // 获取需要点击的文字数量
  const requiredCount = captchaData.value.wordList?.length || 0

  // 当点击次数达到要求时，自动提交验证
  if (clickedPoints.value.length >= requiredCount && requiredCount > 0) {
    await submitWordCaptcha()
  }
}

// ========== 事件处理 ==========

// 点击触发器
const handleTriggerClick = () => {
  if (props.disabled || props.verified) return
  emit('trigger-click')
  dialogVisible.value = true
}

// 弹窗打开
const handleDialogOpen = async () => {
  emit('open')
  clickedPoints.value = []
  await fetchCaptcha()
}

// 弹窗关闭
const handleDialogClosed = () => {
  emit('close')
}

// ========== 监听器 ==========

// 监听 verified 状态变化
watch(
  () => props.verified,
  (newVal) => {
    if (newVal && props.autoCloseOnSuccess) {
      dialogVisible.value = false
    }
  }
)

// ========== 生命周期 ==========

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
})

// ========== 公开方法 ==========

defineExpose({
  // 打开弹窗
  open: () => {
    if (!props.disabled && !props.verified) {
      dialogVisible.value = true
    }
  },
  // 关闭弹窗
  close: () => {
    dialogVisible.value = false
  },
  // 刷新验证码
  refresh: refreshCaptcha,
  // 重置验证状态
  reset: () => {
    emit('update:verified', false)
    dialogVisible.value = false
    clickedPoints.value = []
    jigsawLeft.value = 0
    sliderLeft.value = 0
  },
  // 获取验证结果数据
  getVerificationData: () => captchaData.value,
})
</script>

<style scoped lang="scss">
.captcha-slider-wrapper {
  width: 100%;
}

// 触发器样式 - 优化设计
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

.captcha-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: fit-content;
}

// 滑块验证码
.block-puzzle-captcha {
  display: flex;
  flex-direction: column;
  align-items: center;
}

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
    transform: scale(1.05);
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

// 点选验证码
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

// 动画定义

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

// 按钮内图标
.btn-icon {
  width: 14px;
  height: 14px;
  margin-right: 6px;

  &.is-spinning {
    animation: rotate 1s linear infinite;
  }
}

// 深色模式适配（合并 :root.dark 和 html.dark 选择器）
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

  .word-hint {
    color: var(--text-color-primary, #f1f5f9);
    background: var(--bg-color-page, #0f172a);
    border-color: var(--border-color-base, #334155);

    .words {
      color: var(--primary-color-light, #60a5fa);
    }
  }

  .captcha-loading span {
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
