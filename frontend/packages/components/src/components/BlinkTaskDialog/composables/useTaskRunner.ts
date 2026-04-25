// src/components/BlinkTaskDialog/composables/useTaskRunner.ts
import { ref, onUnmounted } from 'vue'
import {
  TaskStatus,
  StepStatus,
  type TaskState,
  type TaskProgress,
  type TaskRunnerOptions,
  type TaskRunnerReturn,
  type StartOptions,
  type ProgressUpdate,
  type TaskFunction,
} from '../types'

/**
 * 计时器更新间隔（毫秒）
 */
const TIMER_UPDATE_INTERVAL = 100

/**
 * 创建默认进度状态
 */
export function createDefaultProgress(): TaskProgress {
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
export function createDefaultState(): TaskState {
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
 * 核心任务状态管理 Composable
 * 负责任务执行的整个生命周期管理，包括进度追踪、取消/暂停/恢复、错误处理和计时器管理
 *
 * @param options 配置选项
 * @returns TaskRunnerReturn
 */
export function useTaskRunner(options: TaskRunnerOptions = {}): TaskRunnerReturn {
  // 响应式状态
  const state = ref<TaskState>(createDefaultState())

  // 内部状态管理
  let abortController: AbortController | null = null
  let timerInterval: number | null = null
  let autoCloseTimer: ReturnType<typeof setTimeout> | null = null
  let startTimestamp: number = 0
  let pauseTimestamp: number = 0
  let pausedElapsedTime: number = 0
  let currentTaskOptions: StartOptions | null = null

  /**
   * 清理自动关闭计时器
   */
  function clearAutoCloseTimer(): void {
    if (autoCloseTimer !== null) {
      clearTimeout(autoCloseTimer)
      autoCloseTimer = null
    }
  }

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
   * 启动计时器
   */
  function startTimer() {
    clearTimer()
    startTimestamp = Date.now()
    timerInterval = window.setInterval(() => {
      if (state.value.status === TaskStatus.RUNNING) {
        state.value.elapsedTime = Date.now() - startTimestamp + pausedElapsedTime
      }
    }, TIMER_UPDATE_INTERVAL) // 每100ms更新一次
  }

  /**
   * 检测进度类型
   * 根据传入的 steps 或 progressType 自动确定进度展示类型
   */
  function detectProgressType(opts: StartOptions): TaskProgress {
    if (opts.progressType) {
      // 明确指定了进度类型
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

    // 自动检测：有 steps 则使用 steps 模式
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

    // 默认使用 indeterminate 模式
    return createDefaultProgress()
  }

  /**
   * 更新进度
   */
  function updateProgress(update: ProgressUpdate): void {
    const progress = state.value.progress

    // 更新消息
    if (update.message !== undefined) {
      state.value.message = update.message
    }

    // 更新预估时间
    if (update.estimatedTime !== undefined) {
      state.value.estimatedTime = update.estimatedTime
    }

    // 根据进度类型处理进度值
    if (progress.type === 'percent' && update.percent !== undefined) {
      progress.value = Math.min(100, Math.max(0, update.percent))
    } else if (progress.type === 'steps' && progress.steps && update.step !== undefined) {
      // 更新步骤状态
      const stepIndex = update.step
      if (stepIndex >= 0 && stepIndex < progress.steps.length) {
        // 标记之前的步骤为完成
        for (let i = 0; i < stepIndex; i++) {
          if (progress.steps[i].status !== StepStatus.FAILED) {
            progress.steps[i].status = StepStatus.COMPLETED
          }
        }
        // 标记当前步骤为执行中
        progress.steps[stepIndex].status = StepStatus.RUNNING
        progress.currentStep = stepIndex

        // 更新步骤消息
        if (update.stepMessage !== undefined) {
          progress.steps[stepIndex].message = update.stepMessage
        }

        // 计算百分比进度（可选）
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

    // 调用完成回调
    if (options.onComplete) {
      options.onComplete(result)
    }

    // 处理完成行为
    const behavior = currentTaskOptions?.onCompleteBehavior || 'auto-close'
    const delay = currentTaskOptions?.autoCloseDelay ?? options.autoCloseDelay ?? 1500

    if (behavior === 'auto-close') {
      autoCloseTimer = setTimeout(() => {
        state.value.visible = false
        autoCloseTimer = null
      }, delay)
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

    // 尝试标记当前步骤失败
    const currentStep = state.value.progress.currentStep
    if (currentStep !== null) {
      failStep(currentStep)
    }

    clearTimer()

    // 调用错误回调
    if (options.onError) {
      options.onError(error)
    }
  }

  /**
   * 启动任务
   */
  async function start(startOptions: StartOptions): Promise<any> {
    // 防止重复启动
    if (state.value.status === TaskStatus.RUNNING || state.value.status === TaskStatus.PAUSED) {
      throw new Error('任务正在执行中，无法启动新任务')
    }

    // 保存当前任务配置
    currentTaskOptions = startOptions

    // 初始化状态
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

    // 重置暂停相关状态
    pausedElapsedTime = 0
    pauseTimestamp = 0

    // 创建新的 AbortController
    abortController = new AbortController()

    // 启动计时器
    startTimer()

    try {
      // 执行任务函数
      const taskFn = startOptions.task as TaskFunction
      const result = await taskFn(updateProgress, abortController.signal)

      // 检查是否被取消
      if (abortController?.signal.aborted) {
        return null
      }

      // 任务完成
      handleComplete(result)
      return result
    } catch (error: unknown) {
      // 区分取消和真正的错误
      if (
        (error instanceof Error && error.name === 'AbortError') ||
        abortController?.signal.aborted
      ) {
        // 任务被取消，不作为失败处理
        return null
      }

      if (error instanceof Error) {
        handleFailure(error)
        throw error
      }

      // 非Error类型的异常，包装为Error
      const wrappedError = new Error(String(error))
      handleFailure(wrappedError)
      throw wrappedError
    }
  }

  /**
   * 取消任务
   */
  function cancel(): void {
    if (state.value.status !== TaskStatus.RUNNING && state.value.status !== TaskStatus.PAUSED) {
      return
    }

    // 中止任务
    if (abortController) {
      abortController.abort()
    }

    // 更新状态
    state.value.status = TaskStatus.CANCELLED
    state.value.message = '任务已取消'
    clearTimer()

    // 调用取消回调
    if (options.onCancel) {
      options.onCancel()
    }

    // 清理引用
    abortController = null
    currentTaskOptions = null
  }

  /**
   * 暂停任务
   * 注意：暂停功能依赖于任务函数内部对 signal 的处理，
   * 如果任务函数不支持暂停检测，此方法仅会记录暂停状态
   */
  function pause(): void {
    if (state.value.status !== TaskStatus.RUNNING) {
      return
    }

    // 记录暂停时刻的已执行时间
    pauseTimestamp = Date.now()
    pausedElapsedTime = state.value.elapsedTime

    // 更新状态
    state.value.status = TaskStatus.PAUSED
    state.value.message = '任务已暂停'
    clearTimer()
  }

  /**
   * 恢复任务
   * 注意：恢复功能需要任务函数内部检测暂停状态并继续执行
   */
  function resume(): void {
    if (state.value.status !== TaskStatus.PAUSED) {
      return
    }

    // 更新状态
    state.value.status = TaskStatus.RUNNING
    state.value.message = '继续执行任务...'

    // 重启计时器（从暂停时刻继续）
    startTimestamp = Date.now()
    startTimer()
  }

  /**
   * 重置状态
   */
  function reset(): void {
    // 取消正在进行的任务
    if (state.value.status === TaskStatus.RUNNING || state.value.status === TaskStatus.PAUSED) {
      if (abortController) {
        abortController.abort()
      }
    }

    // 清理计时器
    clearTimer()
    clearAutoCloseTimer()

    // 重置状态
    state.value = createDefaultState()

    // 清理引用
    abortController = null
    currentTaskOptions = null
    pausedElapsedTime = 0
    pauseTimestamp = 0
    startTimestamp = 0
  }

  // 组件卸载时清理
  onUnmounted(() => {
    clearTimer()
    clearAutoCloseTimer()
    if (abortController && state.value.status === TaskStatus.RUNNING) {
      abortController.abort()
    }
  })

  return {
    state,
    start,
    cancel,
    pause,
    resume,
    updateProgress,
    reset,
  }
}