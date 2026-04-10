import type { Ref } from 'vue'

// src/components/CaptchaSlider/types.ts

/**
 * 验证码类型
 */
export type CaptchaType = 'blockPuzzle' | 'clickWord' | 'default'

/**
 * 验证码数据
 */
export interface CaptchaData {
  captchaId?: string
  captchaType?: CaptchaType
  originalImageBase64?: string
  jigsawImageBase64?: string
  wordList?: string[]
  pointJson?: string
  token?: string
  captchaVerification?: string
  /** 后端返回的 y 坐标（滑块验证码需要） */
  backendY?: number
}

/**
 * 验证码校验结果
 */
export interface CaptchaCheckResult {
  result: boolean
  msg: string
  captchaId?: string
  captchaVerification?: string
}

/**
 * 点击坐标点
 */
export interface ClickPoint {
  x: number
  y: number
}

/**
 * CaptchaSlider 组件 Props 类型
 */
export interface CaptchaSliderProps {
  // 验证码类型（default 表示由后端决定）
  captchaType?: CaptchaType

  // 是否启用验证码
  enabled?: boolean

  // 是否已验证通过
  verified?: boolean

  // 弹窗标题
  dialogTitle?: string

  // 弹窗宽度
  dialogWidth?: string | number

  // 自定义获取验证码 API
  getCaptchaApi?: (params: CaptchaRequestParams) => Promise<CaptchaData>

  // 自定义校验验证码 API
  checkCaptchaApi?: (params: CaptchaCheckParams) => Promise<CaptchaCheckResult>

  // 滑块最大移动距离（用于计算滑块范围）
  sliderMaxDistance?: number

  // 图片容器宽度
  imageWidth?: number

  // 图片容器高度
  imageHeight?: number

  // 是否在验证成功后自动关闭弹窗
  autoCloseOnSuccess?: boolean

  // 验证成功后是否自动执行回调
  autoSubmitOnSuccess?: boolean

  // 自定义样式类名
  customClass?: string

  // 自定义滑块样式类名
  sliderClass?: string

  // 自定义弹窗样式类名
  dialogClass?: string

  // 是否显示点击触发器（滑块入口）
  showTrigger?: boolean

  // 触发器文字（未验证时）
  triggerText?: string

  // 触发器文字（已验证时）
  triggerVerifiedText?: string

  // 滑块提示文字
  sliderHint?: string

  // 点选验证提示文字
  clickWordHint?: string

  // 是否禁用
  disabled?: boolean

  // 国际化文本（可选，不传则使用默认中文）
  locale?: CaptchaLocale
}

/**
 * 验证码请求参数
 */
export interface CaptchaRequestParams {
  captchaType: CaptchaType
  clientUid?: string
  ts?: number
}

/**
 * 验证码校验参数
 */
export interface CaptchaCheckParams {
  captchaId?: string
  captchaType?: CaptchaType
  pointJson: string
  clientUid?: string
  ts?: number
}

/**
 * CaptchaSlider 组件 Emits 类型
 *
 * 事件说明：
 * - update:verified: 双向绑定验证状态更新
 * - success: 验证成功，返回验证结果数据
 * - fail: 验证失败
 * - refresh: 刷新验证码
 * - open: 弹窗打开
 * - close: 弹窗关闭
 * - trigger-click: 点击触发器
 */
export interface CaptchaSliderEmits {
  (e: 'update:verified', value: boolean): void
  (e: 'success', result: CaptchaCheckResult): void
  (e: 'fail', result: CaptchaCheckResult): void
  (e: 'refresh'): void
  (e: 'open'): void
  (e: 'close'): void
  (e: 'trigger-click'): void
}

/**
 * 国际化文本配置
 */
export interface CaptchaLocale {
  // 触发器相关
  clickToVerify: string
  captchaVerified: string

  // 弹窗相关
  captchaTitle: string
  dragToVerify: string
  clickWordHint: string

  // 操作按钮
  confirm: string
  refresh: string
  refreshing?: string
  loading: string

  // 提示信息
  captchaSuccess: string
  captchaFailed: string
  captchaLoadFailed: string
  pleaseClickWords: string
}

/**
 * 默认中文国际化配置
 */
export const defaultZhCnLocale: CaptchaLocale = {
  clickToVerify: '点击完成验证',
  captchaVerified: '验证通过',
  captchaTitle: '安全验证',
  dragToVerify: '拖动滑块完成验证',
  clickWordHint: '请按顺序点击',
  confirm: '确定',
  refresh: '刷新',
  refreshing: '刷新中...',
  loading: '加载中...',
  captchaSuccess: '验证成功',
  captchaFailed: '验证失败，请重试',
  captchaLoadFailed: '验证码加载失败',
  pleaseClickWords: '请点击文字',
}

/**
 * 默认英文国际化配置
 */
export const defaultEnUsLocale: CaptchaLocale = {
  clickToVerify: 'Click to verify',
  captchaVerified: 'Verified',
  captchaTitle: 'Security Verification',
  dragToVerify: 'Drag slider to verify',
  clickWordHint: 'Click in order',
  confirm: 'Confirm',
  refresh: 'Refresh',
  refreshing: 'Refreshing...',
  loading: 'Loading...',
  captchaSuccess: 'Verification successful',
  captchaFailed: 'Verification failed, please retry',
  captchaLoadFailed: 'Captcha loading failed',
  pleaseClickWords: 'Please click the words',
}

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
  // 使用 getter 函数获取最新的 captchaType，确保响应式更新
  getCaptchaType: () => CaptchaType
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
