import type { Ref } from 'vue'

/**
 * 任务状态枚举
 */
export enum TaskStatus {
  IDLE = 'idle',           // 空闲
  RUNNING = 'running',     // 执行中
  PAUSED = 'paused',       // 已暂停
  COMPLETED = 'completed', // 已完成
  FAILED = 'failed',       // 已失败
  CANCELLED = 'cancelled'  // 已取消
}

/**
 * 步骤状态枚举
 */
export enum StepStatus {
  PENDING = 'pending',     // 待执行
  RUNNING = 'running',     // 执行中
  COMPLETED = 'completed', // 已完成
  FAILED = 'failed'        // 已失败
}

/**
 * 进度类型
 */
export type ProgressType = 'percent' | 'steps' | 'indeterminate'

/**
 * 步骤信息
 */
export interface StepInfo {
  name: string           // 步骤名称
  status: StepStatus     // 步骤状态
  message?: string       // 步骤消息
}

/**
 * 进度信息（混合模式支持）
 */
export interface TaskProgress {
  type: ProgressType                     // 进度类型
  value: number | null                   // 百分比进度值 (0-100)
  steps: StepInfo[] | null               // 步骤信息
  currentStep: number | null             // 当前步骤索引
}

/**
 * 结果操作按钮
 */
export interface ResultAction {
  label: string         // 按钮文字
  type: 'primary' | 'default' | 'link'  // 按钮类型
  handler: () => void   // 点击处理
}

/**
 * 任务结果
 */
export interface TaskResult {
  success: boolean              // 是否成功
  data?: any                    // 结果数据
  summary?: string              // 结果摘要
  actions?: ResultAction[]      // 后续操作按钮
}

/**
 * 任务状态（响应式）
 */
export interface TaskState {
  visible: boolean              // 弹窗是否显示
  status: TaskStatus            // 任务状态
  progress: TaskProgress        // 进度信息
  title: string                 // 任务标题
  message: string               // 当前消息
  result: TaskResult | null     // 任务结果
  error: Error | null           // 错误信息
  elapsedTime: number           // 已耗时（毫秒）
  estimatedTime: number | null  // 预计剩余时间（毫秒）
}

/**
 * 进度更新回调参数
 */
export interface ProgressUpdate {
  percent?: number              // 百分比进度
  message?: string              // 当前消息
  step?: number                 // 当前步骤索引
  stepMessage?: string          // 步骤消息
  estimatedTime?: number        // 预估剩余时间（毫秒）
}

/**
 * 任务函数类型
 * @param onProgress 进度更新回调
 * @param signal 取消信号（AbortSignal）
 */
export type TaskFunction<T = any> = (
  onProgress: (progress: ProgressUpdate) => void,
  signal?: AbortSignal
) => Promise<T>

/**
 * useTaskRunner 参数
 */
export interface TaskRunnerOptions {
  onComplete?: (result: any) => void      // 完成回调
  onCancel?: () => void                   // 取消回调
  onError?: (error: Error) => void        // 错误回调
  autoCloseDelay?: number                 // 成功后自动关闭延迟（毫秒）
  notifyOnComplete?: boolean              // 后台执行时是否通知
}

/**
 * 启动参数
 */
export interface StartOptions {
  task: TaskFunction | (() => Promise<any>)  // 任务函数
  title?: string                              // 任务标题
  message?: string                            // 初始消息
  progressType?: ProgressType                 // 进度类型（可选，自动检测）
  steps?: string[]                            // 步骤名称列表（steps 模式）
  cancellable?: boolean                       // 是否可取消（默认 false）
  backgroundable?: boolean                    // 是否可后台执行（默认 false）
  onCompleteBehavior?: 'auto-close' | 'show-result' | 'show-actions'  // 完成行为
  autoCloseDelay?: number                     // 自动关闭延迟（默认 1500ms）
  resultActions?: ResultAction[]              // 完成后操作按钮
}

/**
 * useTaskRunner 返回值
 */
export interface TaskRunnerReturn {
  state: Ref<TaskState>                   // 任务状态（响应式）
  start: (options: StartOptions) => Promise<any>  // 启动任务
  cancel: () => void                      // 取消任务
  pause: () => void                       // 暂停任务
  resume: () => void                      // 继续任务
  updateProgress: (update: ProgressUpdate) => void  // 手动更新进度
  reset: () => void                       // 重置状态
}

/**
 * BlinkTaskDialog Props
 */
export interface BlinkTaskDialogProps {
  // 状态控制
  modelValue: boolean                     // 弹窗显示状态
  status: TaskStatus                      // 任务状态
  progress: TaskProgress                  // 进度信息

  // 内容配置
  title?: string                          // 任务标题
  message?: string                        // 当前消息
  elapsedTime?: number                    // 已耗时（毫秒）
  estimatedTime?: number | null           // 预估剩余时间

  // 结果展示
  result?: TaskResult | null              // 任务结果
  error?: Error | null                    // 错误信息

  // 交互配置
  cancellable?: boolean                   // 是否可取消（默认 false）
  backgroundable?: boolean                // 是否可后台执行（默认 false）
  closeOnClickModal?: boolean             // 点击遮罩关闭（默认 false）
  showCloseButton?: boolean               // 显示关闭按钮

  // 样式配置
  width?: string | number                 // 弹窗宽度（默认 400px）
  customClass?: string                    // 自定义样式类
}

/**
 * BlinkTaskDialog Emits
 */
export interface BlinkTaskDialogEmits {
  (e: 'update:modelValue', value: boolean): void  // 更新显示状态
  (e: 'cancel'): void                             // 取消任务
  (e: 'background'): void                         // 后台执行
  (e: 'close'): void                              // 关闭弹窗
  (e: 'action', action: ResultAction): void       // 点击结果操作按钮
}

/**
 * runTaskDialog 返回值
 */
export interface RunTaskDialogResult<T = any> {
  success: boolean
  data: T | null
  cancelled: boolean
  error: Error | null
}

/**
 * showTaskDialog 参数（继承 StartOptions）
 */
export interface ShowTaskDialogOptions extends StartOptions {
  // 继承所有 StartOptions 字段
}