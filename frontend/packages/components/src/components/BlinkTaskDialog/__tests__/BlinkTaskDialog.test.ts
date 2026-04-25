import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BlinkTaskDialog from '../index.vue'
import { TaskStatus, StepStatus } from '../types'
import type { TaskProgress, TaskResult, StepInfo, ResultAction } from '../types'

// Mock sub-components - must be defined inside mock factory to avoid hoisting issues
vi.mock('../components/ProgressBar.vue', () => ({
  default: {
    name: 'ProgressBar',
    props: ['percent', 'estimatedTime'],
    template: '<div class="progress-bar-stub">{{ percent }}%</div>',
  },
}))

vi.mock('../components/Spinner.vue', () => ({
  default: {
    name: 'Spinner',
    props: ['size'],
    template: '<div class="spinner-stub">Loading...</div>',
  },
}))

vi.mock('../components/StepsIndicator.vue', () => ({
  default: {
    name: 'StepsIndicator',
    props: ['steps'],
    template: '<div class="steps-indicator-stub">Steps: {{ steps.length }}</div>',
  },
}))

vi.mock('../components/ResultPanel.vue', () => ({
  default: {
    name: 'ResultPanel',
    props: ['status', 'result', 'error'],
    emits: ['action'],
    template: '<div class="result-panel-stub">Result: {{ status }}</div>',
  },
}))

// Helper function for creating default progress
const createDefaultProgress = (type: 'percent' | 'steps' | 'indeterminate' = 'indeterminate', value?: number | null, steps?: StepInfo[] | null): TaskProgress => ({
  type,
  value: value ?? null,
  steps: steps ?? null,
  currentStep: null,
})

describe('BlinkTaskDialog', () => {
  describe('Props 和默认值', () => {
    it('应该接受正确的 props', () => {
      const progress: TaskProgress = createDefaultProgress('percent', 50)

      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress,
          title: '数据导入',
          message: '正在导入数据...',
          elapsedTime: 5000,
          estimatedTime: 10000,
        },
      })

      expect(wrapper.props('modelValue')).toBe(true)
      expect(wrapper.props('status')).toBe(TaskStatus.RUNNING)
      expect(wrapper.props('progress')).toEqual(progress)
      expect(wrapper.props('title')).toBe('数据导入')
      expect(wrapper.props('message')).toBe('正在导入数据...')
      expect(wrapper.props('elapsedTime')).toBe(5000)
      expect(wrapper.props('estimatedTime')).toBe(10000)
    })

    it('应该有正确的默认值', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: false,
          status: TaskStatus.IDLE,
          progress: createDefaultProgress(),
        },
      })

      expect(wrapper.props('status')).toBe(TaskStatus.IDLE)
      expect(wrapper.props('title')).toBe('任务执行')
      expect(wrapper.props('message')).toBe('')
      expect(wrapper.props('elapsedTime')).toBe(0)
      expect(wrapper.props('estimatedTime')).toBeNull()
      expect(wrapper.props('result')).toBeNull()
      expect(wrapper.props('error')).toBeNull()
      expect(wrapper.props('cancellable')).toBe(false)
      expect(wrapper.props('backgroundable')).toBe(false)
      expect(wrapper.props('closeOnClickModal')).toBe(false)
      expect(wrapper.props('showCloseButton')).toBe(false)
      expect(wrapper.props('width')).toBe('400px')
      expect(wrapper.props('customClass')).toBe('')
    })
  })

  describe('状态展示', () => {
    it('运行中状态应该显示 Spinner（不确定进度）', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
        },
      })

      expect(wrapper.find('.spinner-stub').exists()).toBe(true)
      expect(wrapper.find('.progress-bar-stub').exists()).toBe(false)
      expect(wrapper.find('.steps-indicator-stub').exists()).toBe(false)
      expect(wrapper.find('.result-panel-stub').exists()).toBe(false)
    })

    it('暂停状态应该显示为运行中', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.PAUSED,
          progress: createDefaultProgress(),
        },
      })

      // PAUSED 状态被视为 isRunning
      expect(wrapper.find('.blink-task-dialog__running').exists()).toBe(true)
      expect(wrapper.find('.spinner-stub').exists()).toBe(true)
    })

    it('完成状态应该显示 ResultPanel', () => {
      const result: TaskResult = {
        success: true,
        summary: '导入完成，共处理 100 条数据',
      }

      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.COMPLETED,
          progress: createDefaultProgress(),
          result,
        },
      })

      expect(wrapper.find('.result-panel-stub').exists()).toBe(true)
      expect(wrapper.find('.spinner-stub').exists()).toBe(false)
    })

    it('失败状态应该显示 ResultPanel 并传递错误信息', () => {
      const error = new Error('网络连接失败')

      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.FAILED,
          progress: createDefaultProgress(),
          error,
        },
      })

      expect(wrapper.find('.result-panel-stub').exists()).toBe(true)
      expect(wrapper.find('.spinner-stub').exists()).toBe(false)
    })

    it('取消状态应该显示 ResultPanel', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.CANCELLED,
          progress: createDefaultProgress(),
        },
      })

      expect(wrapper.find('.result-panel-stub').exists()).toBe(true)
    })
  })

  describe('进度展示', () => {
    it('百分比进度应该显示 ProgressBar', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress('percent', 75),
          estimatedTime: 5000,
        },
      })

      expect(wrapper.find('.progress-bar-stub').exists()).toBe(true)
      expect(wrapper.find('.progress-bar-stub').text()).toContain('75')
      expect(wrapper.find('.spinner-stub').exists()).toBe(false)
      expect(wrapper.find('.steps-indicator-stub').exists()).toBe(false)
    })

    it('步骤进度应该显示 StepsIndicator', () => {
      const steps: StepInfo[] = [
        { name: '读取文件', status: StepStatus.COMPLETED },
        { name: '解析数据', status: StepStatus.RUNNING },
        { name: '写入数据库', status: StepStatus.PENDING },
      ]

      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress('steps', null, steps),
        },
      })

      expect(wrapper.find('.steps-indicator-stub').exists()).toBe(true)
      expect(wrapper.find('.steps-indicator-stub').text()).toContain('3')
      expect(wrapper.find('.progress-bar-stub').exists()).toBe(false)
      expect(wrapper.find('.spinner-stub').exists()).toBe(false)
    })

    it('不确定时长进度应该显示 Spinner', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
        },
      })

      expect(wrapper.find('.spinner-stub').exists()).toBe(true)
      expect(wrapper.find('.progress-bar-stub').exists()).toBe(false)
      expect(wrapper.find('.steps-indicator-stub').exists()).toBe(false)
    })
  })

  describe('事件处理', () => {
    it('点击取消按钮应该触发 cancel 事件', async () => {
      const onCancel = vi.fn()

      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          cancellable: true,
          onCancel,
        },
      })

      const buttons = wrapper.findAll('.el-button-stub')
      const cancelButton = buttons[0]
      await cancelButton!.trigger('click')

      expect(onCancel).toHaveBeenCalled()
    })

    it('点击后台执行按钮应该触发 background 事件并关闭弹窗', async () => {
      const onBackground = vi.fn()

      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          backgroundable: true,
          onBackground,
          'onUpdate:modelValue': (value: boolean) => wrapper.setProps({ modelValue: value }),
        },
      })

      const buttons = wrapper.findAll('.el-button-stub')
      const backgroundButton = buttons.find((btn) => btn.text().includes('后台执行'))
      await backgroundButton!.trigger('click')

      expect(onBackground).toHaveBeenCalled()
      expect(wrapper.props('modelValue')).toBe(false)
    })

    it('点击关闭按钮应该触发 close 事件并关闭弹窗', async () => {
      const onClose = vi.fn()

      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.COMPLETED,
          progress: createDefaultProgress(),
          onClose,
          'onUpdate:modelValue': (value: boolean) => wrapper.setProps({ modelValue: value }),
        },
      })

      const buttons = wrapper.findAll('.el-button-stub')
      const closeButton = buttons.find((btn) => btn.text().includes('关闭'))
      await closeButton!.trigger('click')

      expect(onClose).toHaveBeenCalled()
      expect(wrapper.props('modelValue')).toBe(false)
    })

    it('不可取消时不应该显示取消按钮', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          cancellable: false,
        },
      })

      const buttons = wrapper.findAll('.el-button-stub')
      const cancelButton = buttons.find((btn) => btn.text().includes('取消任务'))
      expect(cancelButton).toBeUndefined()
    })

    it('不可后台执行时不应该显示后台执行按钮', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          backgroundable: false,
        },
      })

      const buttons = wrapper.findAll('.el-button-stub')
      const backgroundButton = buttons.find((btn) => btn.text().includes('后台执行'))
      expect(backgroundButton).toBeUndefined()
    })

    it('点击 ResultPanel 操作按钮应该触发 action 事件', async () => {
      const onAction = vi.fn()
      const action: ResultAction = { label: '查看详情', type: 'primary', handler: vi.fn() }

      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.COMPLETED,
          progress: createDefaultProgress(),
          result: { success: true, actions: [action] },
          onAction,
        },
      })

      const resultPanel = wrapper.findComponent({ name: 'ResultPanel' })
      await resultPanel.vm.$emit('action', action)

      expect(onAction).toHaveBeenCalledWith(action)
    })
  })

  describe('宽度处理', () => {
    it('应该将数字宽度转换为 px', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          width: 500,
        },
      })

      const vm = wrapper.vm as any
      expect(vm.computedWidth).toBe('500px')
    })

    it('应该保持字符串宽度不变', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          width: '600px',
        },
      })

      const vm = wrapper.vm as any
      expect(vm.computedWidth).toBe('600px')
    })
  })

  describe('标题动态显示', () => {
    it('完成时标题应该添加 "- 完成" 后缀', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.COMPLETED,
          progress: createDefaultProgress(),
          result: { success: true },
          title: '数据导入',
        },
      })

      const vm = wrapper.vm as any
      expect(vm.displayTitle).toBe('数据导入 - 完成')
    })

    it('失败时标题应该添加 "- 失败" 后缀', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.FAILED,
          progress: createDefaultProgress(),
          title: '数据导入',
        },
      })

      const vm = wrapper.vm as any
      expect(vm.displayTitle).toBe('数据导入 - 失败')
    })

    it('取消时标题应该添加 "- 已取消" 后缀', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.CANCELLED,
          progress: createDefaultProgress(),
          title: '数据导入',
        },
      })

      const vm = wrapper.vm as any
      expect(vm.displayTitle).toBe('数据导入 - 已取消')
    })

    it('运行中时标题保持不变', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          title: '数据导入',
        },
      })

      const vm = wrapper.vm as any
      expect(vm.displayTitle).toBe('数据导入')
    })
  })

  describe('时间显示', () => {
    it('应该显示已用时信息', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          elapsedTime: 30000,
        },
      })

      expect(wrapper.find('.blink-task-dialog__elapsed').exists()).toBe(true)
      expect(wrapper.find('.blink-task-dialog__elapsed').text()).toContain('30秒')
    })

    it('应该显示预计剩余时间（百分比进度模式）', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress('percent', 50),
          estimatedTime: 60000,
        },
      })

      expect(wrapper.find('.blink-task-dialog__estimated').exists()).toBe(true)
      expect(wrapper.find('.blink-task-dialog__estimated').text()).toContain('1分钟')
    })

    it('大于60秒应该显示分钟格式', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          elapsedTime: 65000,
        },
      })

      expect(wrapper.find('.blink-task-dialog__elapsed').text()).toContain('1分5秒')
    })
  })

  describe('消息显示', () => {
    it('应该显示消息内容', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          message: '正在处理数据...',
        },
      })

      expect(wrapper.find('.blink-task-dialog__message').exists()).toBe(true)
      expect(wrapper.find('.blink-task-dialog__message').text()).toBe('正在处理数据...')
    })

    it('消息为空时不显示消息区域', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          message: '',
        },
      })

      expect(wrapper.find('.blink-task-dialog__message').exists()).toBe(false)
    })
  })

  describe('自定义样式', () => {
    it('应该应用自定义 class', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          customClass: 'my-custom-dialog',
        },
      })

      const dialog = wrapper.find('.el-dialog-stub')
      expect(dialog.classes()).toContain('blink-task-dialog')
      expect(dialog.classes()).toContain('my-custom-dialog')
    })
  })

  describe('ESC 键关闭控制', () => {
    it('运行中时应该禁用 ESC 关闭', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
        },
      })

      const dialog = wrapper.findComponent({ name: 'ElDialog' })
      expect(dialog.props('closeOnPressEscape')).toBe(false)
    })

    it('非运行中时应该允许 ESC 关闭', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.COMPLETED,
          progress: createDefaultProgress(),
        },
      })

      const dialog = wrapper.findComponent({ name: 'ElDialog' })
      expect(dialog.props('closeOnPressEscape')).toBe(true)
    })
  })

  describe('关闭按钮控制', () => {
    it('运行中时不应该显示关闭按钮', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.RUNNING,
          progress: createDefaultProgress(),
          showCloseButton: false,
        },
      })

      const dialog = wrapper.findComponent({ name: 'ElDialog' })
      expect(dialog.props('showClose')).toBe(false)
    })

    it('配置显示关闭按钮时应该显示', () => {
      const wrapper = mount(BlinkTaskDialog, {
        props: {
          modelValue: true,
          status: TaskStatus.COMPLETED,
          progress: createDefaultProgress(),
          showCloseButton: true,
        },
      })

      const dialog = wrapper.findComponent({ name: 'ElDialog' })
      expect(dialog.props('showClose')).toBe(true)
    })
  })
})