import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import FullscreenToggle from '../index.vue'

// Mock fullscreen API
const mockFullscreenElement = { value: null as Element | null }
const mockRequestFullscreen = vi.fn(() => Promise.resolve())
const mockExitFullscreen = vi.fn(() => Promise.resolve())

Object.defineProperty(document, 'fullscreenElement', {
  get: () => mockFullscreenElement.value,
  configurable: true,
})

Object.defineProperty(document.documentElement, 'requestFullscreen', {
  value: mockRequestFullscreen,
  configurable: true,
})

Object.defineProperty(document, 'exitFullscreen', {
  value: mockExitFullscreen,
  configurable: true,
})

describe('FullscreenToggle', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockFullscreenElement.value = null
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('Props 和默认值', () => {
    it('应该接受正确的 props', () => {
      const wrapper = mount(FullscreenToggle)

      expect(wrapper.props('showLabel')).toBe(false)
      expect(wrapper.props('labels')).toEqual({
        enter: '全屏',
        exit: '退出全屏',
      })
      expect(wrapper.props('title')).toBe('')
    })

    it('应该支持 showLabel', () => {
      const wrapper = mount(FullscreenToggle, {
        props: {
          showLabel: true,
        },
      })

      expect(wrapper.find('.toggle-label').exists()).toBe(true)
    })

    it('应该支持自定义 labels', () => {
      const wrapper = mount(FullscreenToggle, {
        props: {
          showLabel: true,
          labels: {
            enter: 'Enter Fullscreen',
            exit: 'Exit Fullscreen',
          },
        },
      })

      expect(wrapper.props('labels')!.enter).toBe('Enter Fullscreen')
    })

    it('应该正确计算 title', () => {
      const wrapper = mount(FullscreenToggle, {
        props: {
          labels: { enter: '全屏模式', exit: '退出' },
        },
      })

      // 非全屏状态
      expect(wrapper.vm.title).toBe('全屏模式')
    })

    it('应该支持自定义 title', () => {
      const wrapper = mount(FullscreenToggle, {
        props: {
          title: '自定义标题',
        },
      })

      expect(wrapper.vm.title).toBe('自定义标题')
    })
  })

  describe('事件处理', () => {
    it('点击应该触发 toggle 和 change 事件', async () => {
      const onToggle = vi.fn()
      const onChange = vi.fn()

      const wrapper = mount(FullscreenToggle, {
        props: {
          onToggle,
          onChange,
        },
      })

      await wrapper.find('.fullscreen-toggle').trigger('click')

      expect(onToggle).toHaveBeenCalled()
      expect(onChange).toHaveBeenCalledWith(true)
      expect(mockRequestFullscreen).toHaveBeenCalled()
    })

    it('全屏状态下点击应该退出', async () => {
      // 设置为全屏状态
      mockFullscreenElement.value = document.documentElement

      const onToggle = vi.fn()
      const onChange = vi.fn()

      const wrapper = mount(FullscreenToggle, {
        props: {
          onToggle,
          onChange,
        },
      })

      // 手动设置内部状态为全屏
      wrapper.vm.isFullscreen = true

      await wrapper.find('.fullscreen-toggle').trigger('click')

      // 验证事件被触发
      expect(onToggle).toHaveBeenCalled()
      expect(onChange).toHaveBeenCalled()
    })
  })

  describe('Expose', () => {
    it('应该暴露 isFullscreen 和 handleToggle', () => {
      const wrapper = mount(FullscreenToggle)

      expect(typeof wrapper.vm.isFullscreen).toBe('boolean')
      expect(typeof wrapper.vm.handleToggle).toBe('function')
    })
  })

  describe('插槽', () => {
    it('应该支持 default 插槽', () => {
      const wrapper = mount(FullscreenToggle, {
        slots: {
          default: '<span class="custom-toggle">自定义切换</span>',
        },
      })

      expect(wrapper.html()).toContain('custom-toggle')
    })

    it('default 插槽应该接收 isFullscreen 和 toggle 参数', () => {
      const wrapper = mount(FullscreenToggle, {
        slots: {
          default: ({ isFullscreen, toggle }) => {
            return `<span data-fullscreen="${isFullscreen}">FS</span>`
          },
        },
      })

      expect(wrapper.html()).toContain('data-fullscreen')
    })
  })
})
