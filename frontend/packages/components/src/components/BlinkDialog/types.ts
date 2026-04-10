// src/components/BlinkDialog/types.ts

/**
 * BlinkDialog 组件 Props 类型
 */
export interface BlinkDialogProps {
  // 基础配置
  modelValue: boolean
  title?: string
  width?: string | number

  // 行为配置
  closeOnClickModal?: boolean
  closeOnPressEscape?: boolean
  showClose?: boolean
  lockScroll?: boolean

  // 状态
  loading?: boolean
  confirmLoading?: boolean

  // 底部按钮
  showFooter?: boolean
  showCancel?: boolean
  showConfirm?: boolean
  cancelText?: string
  confirmText?: string
  confirmType?: 'primary' | 'success' | 'warning' | 'danger'

  // 关闭确认
  beforeClose?: (done: () => void) => void

  // 样式
  customClass?: string
  destroyOnClose?: boolean
}

/**
 * BlinkDialog 组件 Emits 类型
 *
 * 事件说明：
 * - update:modelValue: 双向绑定更新
 * - confirm: 确认按钮点击
 * - cancel: 取消按钮点击
 * - close: 关闭动作触发（点击取消按钮或关闭按钮时）
 * - open: 对话框打开时
 * - opened: 对话框打开动画完成后
 * - closed: 对话框关闭动画完成后
 */
export interface BlinkDialogEmits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
  (e: 'close'): void
  (e: 'open'): void
  (e: 'opened'): void
  (e: 'closed'): void
}
