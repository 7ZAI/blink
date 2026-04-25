// src/components/BlinkTaskDialog/functions/index.ts
/**
 * BlinkTaskDialog 便捷函数
 *
 * 提供 runTaskDialog 和 showTaskDialog 两个便捷函数，
 * 用于快速启动任务对话框，无需在组件中手动管理状态。
 *
 * @author binblink
 * @since 2026-04-25
 */
import { createApp, h, ref, onUnmounted } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import BlinkTaskDialog from '../index.vue'
import {
  TaskStatus,
  StepStatus,
  type RunTaskDialogResult,
  type StartOptions,
  type TaskState,
  type TaskProgress,
  type TaskRunnerOptions,
  type ProgressUpdate,
  type TaskFunction,
} from '../types'

/**
 * 默认自动关闭延迟（毫秒）
 */
const DEFAULT_AUTO_CLOSE_DELAY = 1500

/**
 * 清理延迟（毫秒）
 * 确保动画完成后再卸载组件
 */
const CLEANUP_DELAY = 100

/**
 * 创建默认进度状态
 */
function createDefaultProgress(): TaskProgress {
  return {
    type: 'indeterminate',
    value: null,
    steps: null,
    currentStep: null,
  }
}

/**
 * 创建默认任务状态
 */
function createDefaultState(): TaskState {
  return {
    visible: false,
    status: TaskStatus.IDLE,
    progress: createDefaultProgress(),
    title: '',
    message: '',
    result: null,
    error: null,
    elapsedTime: 0,
    estimatedTime: null,
  }
}

/**
 * 计时器更新间隔（毫秒）
 */
const TIMER_UPDATE_INTERVAL = 100

/**
 * 检测进度类型
 */
function detectProgressType(opts: StartOptions): TaskProgress {
  if (opts.progressType) {
    if (opts.progressType === 'steps' && opts.steps) {
      return {
        type: 'steps',
        value: null,
        steps: opts.steps.map((name) => ({
          name,
          status: StepStatus.PENDING,
          message: undefined,
        })),
        currentStep: null,
      }
    }
    return {
      type: opts.progressType,
      value: opts.progressType === 'percent' ? 0 : null,
      steps: null,
      currentStep: null,
    }
  }

  if (opts.steps && opts.steps.length > 0) {
    return {
      type: 'steps',
      value: null,
      steps: opts.steps.map((name) => ({
        name,
        status: StepStatus.PENDING,
        message: undefined,
      })),
      currentStep: null,
    }
  }

  return createDefaultProgress()
}

/**
 * 内部任务运行器实现
 *
 * @param options 启动参数
 * @param resolve Promise resolve 函数
 * @param cleanup 清理函数
 * @returns 任务状态和控制器
 */
function createInternalTaskRunner<T>(
  options: StartOptions,
  resolve: (result: RunTaskDialogResult<T>) => void,
  cleanup: () => void
) {
  const state = ref<TaskState>(createDefaultState())
  let abortController: AbortController | null = null
  let timerInterval: number | null = null
  let autoCloseTimer: ReturnType<typeof setTimeout> | null = null
  let startTimestamp: number = 0
  let currentTaskOptions: StartOptions | null = null

  /**
   * 清理计时器
   */
  function clearTimer() {
    if (timerInterval !== null) {
      clearInterval(timerInterval)
      timerInterval = null
    }
  }

  /**
   * 清理自动关闭计时器
   */
  function clearAutoCloseTimer() {
    if (autoCloseTimer !== null) {
      clearTimeout(autoCloseTimer)
      autoCloseTimer = null
    }
  }

  /**
   * 启动计时器
   */
  function startTimer() {
    clearTimer()
    startTimestamp = Date.now()
    timerInterval = window.setInterval(() => {
      if (state.value.status === TaskStatus.RUNNING) {
        state.value.elapsedTime = Date.now() - startTimestamp
      }
    }, TIMER_UPDATE_INTERVAL)
  }

  /**
   * 更新进度
   */
  function updateProgress(update: ProgressUpdate): void {
    const progress = state.value.progress

    if (update.message !== undefined) {
      state.value.message = update.message
    }

    if (update.estimatedTime !== undefined) {
      state.value.estimatedTime = update.estimatedTime
    }

    if (progress.type === 'percent' && update.percent !== undefined) {
      progress.value = Math.min(100, Math.max(0, update.percent))
    } else if (progress.type === 'steps' && progress.steps && update.step !== undefined) {
      const stepIndex = update.step
      if (stepIndex >= 0 && stepIndex < progress.steps.length) {
        for (let i = 0; i < stepIndex; i++) {
          if (progress.steps[i].status !== StepStatus.FAILED) {
            progress.steps[i].status = StepStatus.COMPLETED
          }
        }
        progress.steps[stepIndex].status = StepStatus.RUNNING
        progress.currentStep = stepIndex

        if (update.stepMessage !== undefined) {
          progress.steps[stepIndex].message = update.stepMessage
        }

        progress.value = Math.round((stepIndex / progress.steps.length) * 100)
      }
    }
  }

  /**
   * 完成所有步骤
   */
  function completeAllSteps(): void {
    const progress = state.value.progress
    if (progress.type === 'steps' && progress.steps) {
      for (const step of progress.steps) {
        if (step.status !== StepStatus.FAILED) {
          step.status = StepStatus.COMPLETED
        }
      }
      progress.currentStep = progress.steps.length - 1
      progress.value = 100
    }
  }

  /**
   * 标记步骤失败
   */
  function failStep(stepIndex: number): void {
    const progress = state.value.progress
    if (progress.type === 'steps' && progress.steps && stepIndex >= 0 && stepIndex < progress.steps.length) {
      progress.steps[stepIndex].status = StepStatus.FAILED
    }
  }

  /**
   * 处理任务完成
   */
  function handleComplete(result: any): void {
    state.value.status = TaskStatus.COMPLETED
    state.value.result = {
      success: true,
      data: result,
      summary: currentTaskOptions?.message || '任务执行成功',
      actions: currentTaskOptions?.resultActions,
    }
    completeAllSteps()
    clearTimer()

    const behavior = currentTaskOptions?.onCompleteBehavior || 'auto-close'
    const delay = currentTaskOptions?.autoCloseDelay ?? DEFAULT_AUTO_CLOSE_DELAY

    if (behavior === 'auto-close') {
      autoCloseTimer = setTimeout(() => {
        state.value.visible = false
        resolve({ success: true, data: result as T, cancelled: false, error: null })
        setTimeout(cleanup, CLEANUP_DELAY)
        autoCloseTimer = null
      }, delay)
    } else {
      // 对于 show-result 或 show-actions，需要用户手动关闭
      // 但我们仍然 resolve promise，让用户可以继续操作
      resolve({ success: true, data: result as T, cancelled: false, error: null })
    }
  }

  /**
   * 处理任务失败
   */
  function handleFailure(error: Error): void {
    state.value.status = TaskStatus.FAILED
    state.value.error = error
    state.value.result = {
      success: false,
      summary: error.message,
    }

    const currentStep = state.value.progress.currentStep
    if (currentStep !== null) {
      failStep(currentStep)
    }

    clearTimer()
    resolve({ success: false, data: null, cancelled: false, error })
    setTimeout(cleanup, CLEANUP_DELAY)
  }

  /**
   * 启动任务
   */
  async function start(startOptions: StartOptions): Promise<void> {
    if (state.value.status === TaskStatus.RUNNING || state.value.status === TaskStatus.PAUSED) {
      throw new Error('任务正在执行中，无法启动新任务')
    }

    currentTaskOptions = startOptions

    state.value = {
      visible: true,
      status: TaskStatus.RUNNING,
      progress: detectProgressType(startOptions),
      title: startOptions.title || '执行任务',
      message: startOptions.message || '正在执行...',
      result: null,
      error: null,
      elapsedTime: 0,
      estimatedTime: null,
    }

    abortController = new AbortController()
    startTimer()

    try {
      const taskFn = startOptions.task as TaskFunction
      const result = await taskFn(updateProgress, abortController.signal)

      if (abortController.signal.aborted) {
        return
      }

      handleComplete(result)
    } catch (error: unknown) {
      if (
        (error instanceof Error && error.name === 'AbortError') ||
        abortController?.signal.aborted
      ) {
        return
      }

      if (error instanceof Error) {
        handleFailure(error)
      } else {
        const wrappedError = new Error(String(error))
        handleFailure(wrappedError)
      }
    }
  }

  /**
   * 取消任务
   */
  function cancel(): void {
    if (state.value.status !== TaskStatus.RUNNING && state.value.status !== TaskStatus.PAUSED) {
      return
    }

    if (abortController) {
      abortController.abort()
    }

    state.value.status = TaskStatus.CANCELLED
    state.value.message = '任务已取消'
    clearTimer()
    clearAutoCloseTimer()

    resolve({ success: false, data: null, cancelled: true, error: null })
    setTimeout(cleanup, CLEANUP_DELAY)

    abortController = null
    currentTaskOptions = null
  }

  /**
   * 关闭弹窗（手动关闭）
   */
  function close(): void {
    state.value.visible = false
    clearTimer()
    clearAutoCloseTimer()
    setTimeout(cleanup, CLEANUP_DELAY)
  }

  return {
    state,
    start,
    cancel,
    close,
  }
}

/**
 * 快速启动任务对话框
 *
 * 简化版本，默认使用 auto-close 行为，成功后自动关闭弹窗。
 * 返回 Promise，包含任务执行结果。
 *
 * @example
 * ```ts
 * // 简单用法
 * const result = await runTaskDialog({
 *   task: async (onProgress) => {
 *     onProgress({ percent: 50, message: '处理中...' })
 *     const data = await fetchData()
 *     return data
 *   },
 *   title: '数据加载',
 * })
 *
 * if (result.success) {
 *   console.log('数据:', result.data)
 * }
 * ```
 *
 * @param options 启动参数
 * @returns Promise<RunTaskDialogResult<T>>
 */
export function runTaskDialog<T = any>(
  options: StartOptions
): Promise<RunTaskDialogResult<T>> {
  return new Promise((resolve) => {
    // 创建容器元素
    const container = document.createElement('div')
    document.body.appendChild(container)

    // 清理函数
    const cleanup = () => {
      setTimeout(() => {
        app.unmount()
        container.remove()
      }, CLEANUP_DELAY)
    }

    // 创建内部任务运行器
    const runner = createInternalTaskRunner<T>(options, resolve, cleanup)

    // 创建 Vue 应用实例
    const app = createApp({
      setup() {
        // 启动任务
        runner.start({
          ...options,
          onCompleteBehavior: options.onCompleteBehavior ?? 'auto-close',
          autoCloseDelay: options.autoCloseDelay ?? DEFAULT_AUTO_CLOSE_DELAY,
        })

        // 渲染 BlinkTaskDialog 组件
        return () => h(BlinkTaskDialog, {
          modelValue: runner.state.value.visible,
          status: runner.state.value.status,
          progress: runner.state.value.progress,
          title: runner.state.value.title,
          message: runner.state.value.message,
          elapsedTime: runner.state.value.elapsedTime,
          estimatedTime: runner.state.value.estimatedTime,
          result: runner.state.value.result,
          error: runner.state.value.error,
          cancellable: options.cancellable ?? false,
          backgroundable: options.backgroundable ?? false,
          closeOnClickModal: false,
          showCloseButton: !runner.state.value.visible || runner.state.value.status !== TaskStatus.RUNNING,
          width: options.width ?? '400px',
          'onUpdate:modelValue': (val: boolean) => {
            runner.state.value.visible = val
            if (!val) {
              runner.close()
            }
          },
          onCancel: () => runner.cancel(),
          onBackground: () => {
            runner.state.value.visible = false
            // 后台执行：不清理，让任务继续运行
            // 用户需要自己处理后台任务的结果
          },
          onClose: () => runner.close(),
        })
      },
    })

    // 安装 Element Plus
    app.use(ElementPlus)

    // 挂载应用
    app.mount(container)
  })
}

/**
 * 启动任务对话框（完整版）
 *
 * 支持所有 StartOptions 配置，可自定义完成行为。
 * 返回 Promise，包含任务执行结果。
 *
 * @example
 * ```ts
 * // 显示结果面板
 * const result = await showTaskDialog({
 *   task: async (onProgress) => {
 *     onProgress({ percent: 30, message: '步骤1...' })
 *     await step1()
 *     onProgress({ percent: 60, message: '步骤2...' })
 *     await step2()
 *     onProgress({ percent: 100, message: '完成!' })
 *     return { items: [1, 2, 3] }
 *   },
 *   title: '数据处理',
 *   message: '正在处理数据...',
 *   progressType: 'percent',
 *   onCompleteBehavior: 'show-result',
 *   resultActions: [
 *     { label: '查看详情', type: 'primary', handler: () => showDetail() },
 *     { label: '关闭', type: 'default', handler: () => {} },
 *   ],
 * })
 * ```
 *
 * @example
 * ```ts
 * // 步骤模式
 * const result = await showTaskDialog({
 *   task: async (onProgress) => {
 *     onProgress({ step: 0, stepMessage: '解析文件...' })
 *     await parseFile()
 *     onProgress({ step: 1, stepMessage: '处理数据...' })
 *     await processData()
 *     onProgress({ step: 2, stepMessage: '保存结果...' })
 *     await saveResult()
 *     return { count: 100 }
 *   },
 *   title: '文件导入',
 *   steps: ['解析文件', '处理数据', '保存结果'],
 *   cancellable: true,
 * })
 * ```
 *
 * @param options 启动参数
 * @returns Promise<RunTaskDialogResult<T>>
 */
export async function showTaskDialog<T = any>(
  options: StartOptions
): Promise<RunTaskDialogResult<T>> {
  return runTaskDialog<T>(options)
}