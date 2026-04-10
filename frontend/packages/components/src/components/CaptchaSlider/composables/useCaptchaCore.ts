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
    getCaptchaType,
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
        captchaType: getCaptchaType(),
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

  const handleWordClick = (e: MouseEvent): void => {
    if (currentCaptchaType.value !== 'clickWord') return
    if (loading.value) return

    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    clickedPoints.value.push({ x, y })

    const requiredCount = captchaData.value.wordList?.length || 0

    if (clickedPoints.value.length >= requiredCount && requiredCount > 0) {
      submitWordCaptcha()
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