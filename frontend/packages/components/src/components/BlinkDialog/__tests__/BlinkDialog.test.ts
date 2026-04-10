import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BlinkDialog from '../index.vue'

describe('BlinkDialog', () => {
  describe('Props 和默认值', () => {
    it('应该接受正确的 props', () => {
      const wrapper = mount(BlinkDialog, {
        props: {
          modelValue: true,
          title: '测试对话框',
          width: '600px',
        },
      })

      expect(wrapper.props('modelValue')).toBe(true)
      expect(wrapper.props('title')).toBe('测试对话框')
      expect(wrapper.props('width')).toBe('600px')
    })

    it('应该有正确的默认值', () => {
      const wrapper = mount(BlinkDialog, {
        props: { modelValue: false },
      })

      expect(wrapper.props('closeOnClickModal')).toBe(false)
      expect(wrapper.props('showFooter')).toBe(true)
      expect(wrapper.props('showCancel')).toBe(true)
      expect(wrapper.props('showConfirm')).toBe(true)
      expect(wrapper.props('cancelText')).toBe('取消')
      expect(wrapper.props('confirmText')).toBe('确定')
    })
  })

  describe('宽度处理', () => {
    it('应该将数字宽度转换为 px', async () => {
      const wrapper = mount(BlinkDialog, {
        props: {
          modelValue: true,
          width: 600,
        },
      })

      // 检查传递给 el-dialog 的 width prop
      const dialog = wrapper.find('.el-dialog-stub')
      expect(dialog.exists()).toBe(true)

      // 通过组件内部状态验证
      const vm = wrapper.vm as any
      // computedWidth 是内部计算属性
      expect(vm.computedWidth).toBe('600px')
    })

    it('应该保持字符串宽度不变', async () => {
      const wrapper = mount(BlinkDialog, {
        props: {
          modelValue: true,
          width: '50%',
        },
      })

      const vm = wrapper.vm as any
      expect(vm.computedWidth).toBe('50%')
    })
  })

  describe('事件处理', () => {
    it('点击取消按钮应该触发 cancel 和 close 事件', async () => {
      const onCancel = vi.fn()
      const onClose = vi.fn()

      const wrapper = mount(BlinkDialog, {
        props: {
          modelValue: true,
          showFooter: true,
          showCancel: true,
          onCancel,
          onClose,
          'onUpdate:modelValue': (value: boolean) => wrapper.setProps({ modelValue: value }),
        },
      })

      const buttons = wrapper.findAll('.el-button-stub')
      const cancelButton = buttons[0]!
      await cancelButton.trigger('click')

      expect(onCancel).toHaveBeenCalled()
      expect(onClose).toHaveBeenCalled()
      expect(wrapper.props('modelValue')).toBe(false)
    })

    it('点击确认按钮应该触发 confirm 事件（不关闭对话框）', async () => {
      const onConfirm = vi.fn()

      const wrapper = mount(BlinkDialog, {
        props: {
          modelValue: true,
          showFooter: true,
          showConfirm: true,
          onConfirm,
        },
      })

      const buttons = wrapper.findAll('.el-button-stub')
      const confirmButton = buttons[buttons.length - 1]!
      await confirmButton.trigger('click')

      expect(onConfirm).toHaveBeenCalled()
      // 确认按钮不应该自动关闭对话框
      expect(wrapper.props('modelValue')).toBe(true)
    })

    it('应该正确触发 open 和 opened 事件', async () => {
      const onOpen = vi.fn()
      const onOpened = vi.fn()

      const wrapper = mount(BlinkDialog, {
        props: {
          modelValue: false,
          onOpen,
          onOpened,
        },
      })

      // 模拟 el-dialog emit open 和 opened
      const dialog = wrapper.findComponent({ name: 'el-dialog' })
      await dialog.vm.$emit('open')
      await dialog.vm.$emit('opened')

      expect(onOpen).toHaveBeenCalled()
      expect(onOpened).toHaveBeenCalled()
    })

    it('应该正确触发 closed 事件（不触发 close）', async () => {
      const onClosed = vi.fn()
      const onClose = vi.fn()

      const wrapper = mount(BlinkDialog, {
        props: {
          modelValue: true,
          onClosed,
          onClose,
        },
      })

      const dialog = wrapper.findComponent({ name: 'el-dialog' })
      await dialog.vm.$emit('closed')

      // closed 事件只触发 onClosed，不触发 onClose
      expect(onClosed).toHaveBeenCalled()
      expect(onClose).not.toHaveBeenCalled()
    })
  })

  describe('beforeClose 回调', () => {
    it('当没有 beforeClose 时应该直接调用 done', async () => {
      const wrapper = mount(BlinkDialog, {
        props: { modelValue: true },
      })

      const done = vi.fn()
      // 直接调用 handleBeforeClose 方法
      const vm = wrapper.vm as any
      vm.handleBeforeClose(done)

      expect(done).toHaveBeenCalled()
    })

    it('当有 beforeClose 时应该调用它', async () => {
      const done = vi.fn()
      const beforeClose = vi.fn((callback: () => void) => {
        callback()
      })

      const wrapper = mount(BlinkDialog, {
        props: {
          modelValue: true,
          beforeClose,
        },
      })

      // 验证 beforeClose 函数被正确传递
      const dialog = wrapper.findComponent({ name: 'el-dialog' })
      const passedBeforeClose = dialog.props('beforeClose')

      // 调用传递的 beforeClose
      passedBeforeClose(done)

      expect(beforeClose).toHaveBeenCalled()
      expect(done).toHaveBeenCalled()
    })
  })

  describe('插槽', () => {
    it('应该支持 default 插槽', () => {
      const wrapper = mount(BlinkDialog, {
        props: { modelValue: true },
        slots: {
          default: '<div class="custom-content">自定义内容</div>',
        },
      })

      expect(wrapper.html()).toContain('自定义内容')
    })

    it('应该支持 header 插槽', () => {
      const wrapper = mount(BlinkDialog, {
        props: { modelValue: true },
        slots: {
          header: '<div class="custom-header">自定义标题</div>',
        },
      })

      expect(wrapper.html()).toContain('自定义标题')
    })

    it('应该支持 footer 插槽', () => {
      const wrapper = mount(BlinkDialog, {
        props: { modelValue: true, showFooter: true },
        slots: {
          footer: '<div class="custom-footer">自定义底部</div>',
        },
      })

      expect(wrapper.html()).toContain('自定义底部')
    })
  })
})
