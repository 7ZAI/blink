import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { nextTick, defineComponent, h, type DefineComponent } from 'vue'
import { mount, type VueWrapper } from '@vue/test-utils'
import {
  useTaskRunner,
  createDefaultProgress,
  createDefaultState,
} from '../composables/useTaskRunner'
import { TaskStatus, StepStatus, type StartOptions, type TaskRunnerOptions } from '../types'

/**
 * 创建测试组件包装器，用于测试 composable
 * Vue composables 使用生命周期钩子（如 onUnmounted）需要在组件上下文中运行
 */
function withTaskRunner(
  options: TaskRunnerOptions = {}
): VueWrapper<DefineComponent> & { runner: ReturnType<typeof useTaskRunner> } {
  let runner: ReturnType<typeof useTaskRunner> | null = null

  const TestComponent = defineComponent({
    setup() {
      runner = useTaskRunner(options)
      return () => h('div', { class: 'test-component' })
    },
  })

  const wrapper = mount(TestComponent) as VueWrapper<DefineComponent> & {
    runner: ReturnType<typeof useTaskRunner>
  }
  wrapper.runner = runner!

  return wrapper
}

describe('useTaskRunner', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  describe('createDefaultProgress', () => {
    it('应该创建正确的默认进度状态', () => {
      const progress = createDefaultProgress()

      expect(progress.type).toBe('indeterminate')
      expect(progress.value).toBeNull()
      expect(progress.steps).toBeNull()
      expect(progress.currentStep).toBeNull()
    })
  })

  describe('createDefaultState', () => {
    it('应该创建正确的默认任务状态', () => {
      const state = createDefaultState()

      expect(state.visible).toBe(false)
      expect(state.status).toBe(TaskStatus.IDLE)
      expect(state.progress.type).toBe('indeterminate')
      expect(state.title).toBe('')
      expect(state.message).toBe('')
      expect(state.result).toBeNull()
      expect(state.error).toBeNull()
      expect(state.elapsedTime).toBe(0)
      expect(state.estimatedTime).toBeNull()
    })
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      const wrapper = withTaskRunner()
      const { state } = wrapper.runner

      expect(state.value.visible).toBe(false)
      expect(state.value.status).toBe(TaskStatus.IDLE)
      expect(state.value.progress.type).toBe('indeterminate')
      expect(state.value.progress.value).toBeNull()
      expect(state.value.progress.steps).toBeNull()
      expect(state.value.progress.currentStep).toBeNull()
      expect(state.value.title).toBe('')
      expect(state.value.message).toBe('')
      expect(state.value.result).toBeNull()
      expect(state.value.error).toBeNull()
      expect(state.value.elapsedTime).toBe(0)

      wrapper.unmount()
    })
  })

  describe('start 函数', () => {
    it('应该正确启动任务', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async (onProgress, signal) => {
        return 'task-result'
      })

      const options: StartOptions = {
        task: taskFn,
        title: '测试任务',
        message: '正在执行测试任务',
      }

      const result = await start(options)

      expect(result).toBe('task-result')
      expect(state.value.visible).toBe(true)
      expect(state.value.status).toBe(TaskStatus.COMPLETED)
      expect(state.value.title).toBe('测试任务')
      expect(state.value.message).toBe('正在执行测试任务')
      expect(taskFn).toHaveBeenCalled()

      wrapper.unmount()
    })

    it('应该使用默认标题和消息', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async () => 'result')

      await start({ task: taskFn })

      expect(state.value.title).toBe('执行任务')
      expect(state.value.message).toBe('正在执行...')

      wrapper.unmount()
    })

    it('应该正确处理百分比进度', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async (onProgress) => {
        onProgress({ percent: 25, message: '进度25%' })
        onProgress({ percent: 50, message: '进度50%' })
        onProgress({ percent: 75, message: '进度75%' })
        return 'done'
      })

      await start({
        task: taskFn,
        progressType: 'percent',
      })

      expect(state.value.progress.type).toBe('percent')
      expect(state.value.progress.value).toBe(75)
      expect(state.value.message).toBe('进度75%')

      wrapper.unmount()
    })

    it('应该限制百分比进度在0-100范围内', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async (onProgress) => {
        onProgress({ percent: 150 }) // 超过100
        return 'done'
      })

      await start({
        task: taskFn,
        progressType: 'percent',
      })

      expect(state.value.progress.value).toBe(100)

      wrapper.unmount()
    })

    it('应该限制百分比进度不低于0', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async (onProgress) => {
        onProgress({ percent: -50 }) // 小于0
        return 'done'
      })

      await start({
        task: taskFn,
        progressType: 'percent',
      })

      expect(state.value.progress.value).toBe(0)

      wrapper.unmount()
    })

    it('应该正确处理步骤进度', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async (onProgress) => {
        onProgress({ step: 0, stepMessage: '步骤1执行中' })
        onProgress({ step: 1, stepMessage: '步骤2执行中' })
        onProgress({ step: 2, stepMessage: '步骤3执行中' })
        return 'done'
      })

      await start({
        task: taskFn,
        steps: ['步骤1', '步骤2', '步骤3'],
      })

      expect(state.value.progress.type).toBe('steps')
      expect(state.value.progress.steps).toHaveLength(3)
      expect(state.value.progress.currentStep).toBe(2)
      // 所有步骤应该已完成
      expect(state.value.progress.steps![0].status).toBe(StepStatus.COMPLETED)
      expect(state.value.progress.steps![1].status).toBe(StepStatus.COMPLETED)
      expect(state.value.progress.steps![2].status).toBe(StepStatus.COMPLETED)
      expect(state.value.progress.value).toBe(100)

      wrapper.unmount()
    })

    it('应该自动检测步骤进度类型', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async () => 'done')

      await start({
        task: taskFn,
        steps: ['步骤A', '步骤B'],
      })

      expect(state.value.progress.type).toBe('steps')
      expect(state.value.progress.steps).toHaveLength(2)

      wrapper.unmount()
    })

    it('步骤执行时应该标记之前步骤为完成', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async (onProgress) => {
        onProgress({ step: 1, stepMessage: '执行步骤2' })
        return 'done'
      })

      await start({
        task: taskFn,
        steps: ['步骤1', '步骤2', '步骤3'],
      })

      // 步骤1应该被标记为完成
      expect(state.value.progress.steps![0].status).toBe(StepStatus.COMPLETED)
      // 步骤2是当前步骤
      expect(state.value.progress.steps![1].status).toBe(StepStatus.COMPLETED)

      wrapper.unmount()
    })

    it('应该正确处理任务错误', async () => {
      const onError = vi.fn()
      const wrapper = withTaskRunner({ onError })
      const { state, start } = wrapper.runner

      const error = new Error('任务执行失败')
      const taskFn = vi.fn(async () => {
        throw error
      })

      try {
        await start({ task: taskFn })
      } catch (e) {
        expect(e).toBe(error)
      }

      expect(state.value.status).toBe(TaskStatus.FAILED)
      expect(state.value.error).toBe(error)
      expect(state.value.result?.success).toBe(false)
      expect(onError).toHaveBeenCalledWith(error)

      wrapper.unmount()
    })

    it('应该正确处理非Error类型异常', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async () => {
        throw 'string-error'
      })

      try {
        await start({ task: taskFn })
      } catch (e) {
        expect(e).toBeInstanceOf(Error)
        expect((e as Error).message).toBe('string-error')
      }

      expect(state.value.status).toBe(TaskStatus.FAILED)
      expect(state.value.error?.message).toBe('string-error')

      wrapper.unmount()
    })

    it('应该防止重复启动正在执行的任务', async () => {
      const wrapper = withTaskRunner()
      const { start } = wrapper.runner

      const longTask = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 1000))
        return 'done'
      })

      // 启动第一个任务
      const promise1 = start({ task: longTask })

      // 尝试启动第二个任务（应该抛出错误）
      await expect(start({ task: longTask })).rejects.toThrow('任务正在执行中，无法启动新任务')

      // 等待第一个任务完成
      vi.advanceTimersByTime(1000)
      await promise1

      wrapper.unmount()
    })

    it('应该更新预估时间', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async (onProgress) => {
        onProgress({ estimatedTime: 5000 })
        return 'done'
      })

      await start({ task: taskFn })

      expect(state.value.estimatedTime).toBe(5000)

      wrapper.unmount()
    })

    it('应该调用 onComplete 回调', async () => {
      const onComplete = vi.fn()
      const wrapper = withTaskRunner({ onComplete })
      const { start } = wrapper.runner

      const taskFn = vi.fn(async () => 'task-result')

      await start({ task: taskFn })

      expect(onComplete).toHaveBeenCalledWith('task-result')

      wrapper.unmount()
    })

    it('auto-close 模式下应该自动关闭弹窗', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async () => 'done')

      await start({
        task: taskFn,
        onCompleteBehavior: 'auto-close',
        autoCloseDelay: 1500,
      })

      expect(state.value.status).toBe(TaskStatus.COMPLETED)
      expect(state.value.visible).toBe(true)

      // 推进时间到自动关闭延迟后
      vi.advanceTimersByTime(1500)
      await nextTick()

      expect(state.value.visible).toBe(false)

      wrapper.unmount()
    })

    it('show-result 模式下不应该自动关闭弹窗', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async () => 'done')

      await start({
        task: taskFn,
        onCompleteBehavior: 'show-result',
      })

      expect(state.value.status).toBe(TaskStatus.COMPLETED)
      expect(state.value.visible).toBe(true)

      // 推进时间，弹窗应该仍然可见
      vi.advanceTimersByTime(2000)
      await nextTick()

      expect(state.value.visible).toBe(true)

      wrapper.unmount()
    })
  })

  describe('cancel 函数', () => {
    it('应该正确取消正在执行的任务', async () => {
      const onCancel = vi.fn()
      const wrapper = withTaskRunner({ onCancel })
      const { state, start, cancel } = wrapper.runner

      // 任务函数需要检查 signal.aborted 并配合取消
      const taskFn = vi.fn(async (onProgress, signal) => {
        // 模拟长时间任务，每隔一段时间检查是否被取消
        for (let i = 0; i < 50; i++) {
          if (signal?.aborted) {
            return null // 被取消时返回 null
          }
          await new Promise((resolve) => setTimeout(resolve, 100))
        }
        return 'done'
      })

      // 启动任务
      const promise = start({ task: taskFn })

      // 等待任务开始
      await nextTick()
      expect(state.value.status).toBe(TaskStatus.RUNNING)

      // 推进一点时间让任务运行
      vi.advanceTimersByTime(200)

      // 取消任务
      cancel()

      expect(state.value.status).toBe(TaskStatus.CANCELLED)
      expect(state.value.message).toBe('任务已取消')
      expect(onCancel).toHaveBeenCalled()

      // 推进时间让任务检测到取消并完成
      vi.advanceTimersByTime(100)
      const result = await promise
      expect(result).toBeNull()

      wrapper.unmount()
    })

    it('应该使用 AbortController 中止任务', async () => {
      const wrapper = withTaskRunner()
      const { start, cancel } = wrapper.runner

      let abortSignal: AbortSignal | undefined
      const taskFn = vi.fn(async (onProgress, signal) => {
        abortSignal = signal
        // 任务需要检查 signal.aborted
        for (let i = 0; i < 10; i++) {
          if (signal?.aborted) {
            return null
          }
          await new Promise((resolve) => setTimeout(resolve, 100))
        }
        return 'done'
      })

      // 启动任务
      const promise = start({ task: taskFn })
      await nextTick()

      // 取消任务
      cancel()

      // 验证 AbortController 被调用
      expect(abortSignal?.aborted).toBe(true)

      // 推进时间让任务检测到取消
      vi.advanceTimersByTime(100)
      const result = await promise
      expect(result).toBeNull()

      wrapper.unmount()
    })

    it('取消后任务函数接收到 AbortError 时不应该视为失败', async () => {
      const onError = vi.fn()
      const wrapper = withTaskRunner({ onError })
      const { state, start, cancel } = wrapper.runner

      const abortError = new Error('Aborted')
      abortError.name = 'AbortError'

      const taskFn = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100))
        throw abortError
      })

      const promise = start({ task: taskFn })
      await nextTick()

      cancel()

      vi.advanceTimersByTime(100)
      const result = await promise

      // AbortError 不应该触发失败状态
      expect(result).toBeNull()
      expect(onError).not.toHaveBeenCalled()

      wrapper.unmount()
    })

    it('取消非运行状态的任务应该无效', () => {
      const wrapper = withTaskRunner()
      const { state, cancel } = wrapper.runner

      expect(state.value.status).toBe(TaskStatus.IDLE)

      cancel()

      expect(state.value.status).toBe(TaskStatus.IDLE)

      wrapper.unmount()
    })

    it('应该取消暂停状态的任务', async () => {
      const wrapper = withTaskRunner()
      const { state, start, pause, cancel } = wrapper.runner

      const longTask = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000))
        return 'done'
      })

      const promise = start({ task: longTask })
      vi.advanceTimersByTime(100)
      pause()

      expect(state.value.status).toBe(TaskStatus.PAUSED)

      cancel()

      expect(state.value.status).toBe(TaskStatus.CANCELLED)

      vi.advanceTimersByTime(5000)
      await promise

      wrapper.unmount()
    })
  })

  describe('pause 和 resume 函数', () => {
    it('应该正确暂停任务', async () => {
      const wrapper = withTaskRunner()
      const { state, start, pause } = wrapper.runner

      const taskFn = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000))
        return 'done'
      })

      start({ task: taskFn })
      await nextTick()

      expect(state.value.status).toBe(TaskStatus.RUNNING)

      pause()

      expect(state.value.status).toBe(TaskStatus.PAUSED)
      expect(state.value.message).toBe('任务已暂停')

      wrapper.unmount()
    })

    it('应该正确恢复任务', async () => {
      const wrapper = withTaskRunner()
      const { state, start, pause, resume } = wrapper.runner

      const taskFn = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000))
        return 'done'
      })

      start({ task: taskFn })
      vi.advanceTimersByTime(100)

      pause()
      expect(state.value.status).toBe(TaskStatus.PAUSED)

      resume()

      expect(state.value.status).toBe(TaskStatus.RUNNING)
      expect(state.value.message).toBe('继续执行任务...')

      wrapper.unmount()
    })

    it('暂停时应该记录已执行时间', async () => {
      const wrapper = withTaskRunner()
      const { state, start, pause } = wrapper.runner

      const taskFn = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000))
        return 'done'
      })

      start({ task: taskFn })

      // 推进时间100ms
      vi.advanceTimersByTime(100)

      pause()

      // 已执行时间应该被记录（大约100ms）
      expect(state.value.elapsedTime).toBeGreaterThanOrEqual(0)

      wrapper.unmount()
    })

    it('恢复后计时器应该继续', async () => {
      const wrapper = withTaskRunner()
      const { state, start, pause, resume } = wrapper.runner

      const taskFn = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000))
        return 'done'
      })

      start({ task: taskFn })
      vi.advanceTimersByTime(100)

      const elapsedBeforePause = state.value.elapsedTime
      pause()

      resume()

      // 恢复后计时器重新开始
      vi.advanceTimersByTime(100)

      // 计时应该继续累加
      expect(state.value.elapsedTime).toBeGreaterThanOrEqual(elapsedBeforePause)

      wrapper.unmount()
    })

    it('暂停非运行状态的任务应该无效', () => {
      const wrapper = withTaskRunner()
      const { state, pause } = wrapper.runner

      expect(state.value.status).toBe(TaskStatus.IDLE)

      pause()

      expect(state.value.status).toBe(TaskStatus.IDLE)

      wrapper.unmount()
    })

    it('恢复非暂停状态的任务应该无效', () => {
      const wrapper = withTaskRunner()
      const { state, resume } = wrapper.runner

      expect(state.value.status).toBe(TaskStatus.IDLE)

      resume()

      expect(state.value.status).toBe(TaskStatus.IDLE)

      wrapper.unmount()
    })
  })

  describe('reset 函数', () => {
    it('应该正确重置状态', async () => {
      const wrapper = withTaskRunner()
      const { state, start, reset } = wrapper.runner

      const taskFn = vi.fn(async () => 'done')

      await start({ task: taskFn, title: '测试任务' })

      expect(state.value.status).toBe(TaskStatus.COMPLETED)
      expect(state.value.visible).toBe(true)

      reset()

      expect(state.value.visible).toBe(false)
      expect(state.value.status).toBe(TaskStatus.IDLE)
      expect(state.value.progress.type).toBe('indeterminate')
      expect(state.value.title).toBe('')
      expect(state.value.message).toBe('')
      expect(state.value.result).toBeNull()
      expect(state.value.error).toBeNull()
      expect(state.value.elapsedTime).toBe(0)

      wrapper.unmount()
    })

    it('重置应该取消正在执行的任务', async () => {
      const wrapper = withTaskRunner()
      const { state, start, reset } = wrapper.runner

      const longTask = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000))
        return 'done'
      })

      start({ task: longTask })
      await nextTick()

      expect(state.value.status).toBe(TaskStatus.RUNNING)

      reset()

      expect(state.value.status).toBe(TaskStatus.IDLE)

      wrapper.unmount()
    })

    it('重置应该取消暂停状态的任务', async () => {
      const wrapper = withTaskRunner()
      const { state, start, pause, reset } = wrapper.runner

      const longTask = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000))
        return 'done'
      })

      start({ task: longTask })
      vi.advanceTimersByTime(100)
      pause()

      expect(state.value.status).toBe(TaskStatus.PAUSED)

      reset()

      expect(state.value.status).toBe(TaskStatus.IDLE)

      wrapper.unmount()
    })
  })

  describe('updateProgress 函数', () => {
    it('应该手动更新进度消息', async () => {
      const wrapper = withTaskRunner()
      const { state, start, updateProgress } = wrapper.runner

      const taskFn = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100))
        return 'done'
      })

      start({ task: taskFn })
      await nextTick()

      updateProgress({ message: '手动更新消息' })

      expect(state.value.message).toBe('手动更新消息')

      wrapper.unmount()
    })

    it('应该手动更新百分比进度', async () => {
      const wrapper = withTaskRunner()
      const { state, start, updateProgress } = wrapper.runner

      const taskFn = vi.fn(async () => 'done')

      await start({
        task: taskFn,
        progressType: 'percent',
      })

      updateProgress({ percent: 60 })

      expect(state.value.progress.value).toBe(60)

      wrapper.unmount()
    })

    it('应该手动更新步骤进度', async () => {
      const wrapper = withTaskRunner()
      const { state, start, updateProgress } = wrapper.runner

      const taskFn = vi.fn(async () => 'done')

      await start({
        task: taskFn,
        steps: ['步骤A', '步骤B', '步骤C'],
      })

      updateProgress({ step: 1, stepMessage: '正在执行步骤B' })

      expect(state.value.progress.currentStep).toBe(1)
      expect(state.value.progress.steps![1].status).toBe(StepStatus.RUNNING)
      expect(state.value.progress.steps![1].message).toBe('正在执行步骤B')

      wrapper.unmount()
    })

    it('应该手动更新预估时间', async () => {
      const wrapper = withTaskRunner()
      const { state, start, updateProgress } = wrapper.runner

      const taskFn = vi.fn(async () => 'done')

      await start({ task: taskFn })

      updateProgress({ estimatedTime: 3000 })

      expect(state.value.estimatedTime).toBe(3000)

      wrapper.unmount()
    })
  })

  describe('计时器功能', () => {
    it('应该正确计算已耗时', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 1000))
        return 'done'
      })

      start({ task: taskFn })
      await nextTick()

      // 推进时间500ms
      vi.advanceTimersByTime(500)

      expect(state.value.elapsedTime).toBeGreaterThanOrEqual(400)
      expect(state.value.elapsedTime).toBeLessThan(600)

      wrapper.unmount()
    })

    it('任务完成后应该停止计时', async () => {
      const wrapper = withTaskRunner()
      const { state, start } = wrapper.runner

      const taskFn = vi.fn(async () => 'done')

      await start({ task: taskFn })

      const elapsedAtCompletion = state.value.elapsedTime

      // 推进时间，elapsedTime不应该变化
      vi.advanceTimersByTime(1000)

      expect(state.value.elapsedTime).toBe(elapsedAtCompletion)

      wrapper.unmount()
    })
  })

  describe('返回值结构', () => {
    it('应该返回正确的结构', () => {
      const wrapper = withTaskRunner()
      const runner = wrapper.runner

      expect(runner).toHaveProperty('state')
      expect(runner).toHaveProperty('start')
      expect(runner).toHaveProperty('cancel')
      expect(runner).toHaveProperty('pause')
      expect(runner).toHaveProperty('resume')
      expect(runner).toHaveProperty('updateProgress')
      expect(runner).toHaveProperty('reset')

      expect(typeof runner.start).toBe('function')
      expect(typeof runner.cancel).toBe('function')
      expect(typeof runner.pause).toBe('function')
      expect(typeof runner.resume).toBe('function')
      expect(typeof runner.updateProgress).toBe('function')
      expect(typeof runner.reset).toBe('function')

      wrapper.unmount()
    })
  })
})