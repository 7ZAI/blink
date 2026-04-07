import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ThemeToggle from '../index.vue'

describe('ThemeToggle', () => {
  describe('Props 和默认值', () => {
    it('应该接受正确的 props', () => {
      const wrapper = mount(ThemeToggle, {
        props: {
          theme: 'dark',
        },
      })

      expect(wrapper.props('theme')).toBe('dark')
      expect(wrapper.props('showLabel')).toBe(false)
      expect(wrapper.props('labels')).toEqual({
        dark: '深色模式',
        light: '浅色模式',
      })
    })

    it('应该正确计算 isDark', () => {
      const lightWrapper = mount(ThemeToggle, {
        props: { theme: 'light' },
      })
      expect(lightWrapper.vm.isDark).toBe(false)

      const darkWrapper = mount(ThemeToggle, {
        props: { theme: 'dark' },
      })
      expect(darkWrapper.vm.isDark).toBe(true)
    })

    it('应该正确计算 title', () => {
      const wrapper = mount(ThemeToggle, {
        props: {
          theme: 'light',
          labels: { dark: '切换到深色', light: '切换到浅色' },
        },
      })

      expect(wrapper.vm.title).toBe('切换到深色')
    })

    it('应该支持自定义 title', () => {
      const wrapper = mount(ThemeToggle, {
        props: {
          theme: 'light',
          title: '自定义标题',
        },
      })

      expect(wrapper.vm.title).toBe('自定义标题')
    })

    it('应该支持 showLabel', () => {
      const wrapper = mount(ThemeToggle, {
        props: {
          theme: 'dark',
          showLabel: true,
        },
      })

      // 验证标签显示
      expect(wrapper.find('.toggle-label').exists()).toBe(true)
    })
  })

  describe('事件处理', () => {
    it('点击应该触发 toggle 和 change 事件', async () => {
      const onToggle = vi.fn()
      const onChange = vi.fn()

      const wrapper = mount(ThemeToggle, {
        props: {
          theme: 'light',
          onToggle,
          onChange,
        },
      })

      await wrapper.find('.theme-toggle').trigger('click')

      expect(onToggle).toHaveBeenCalled()
      expect(onChange).toHaveBeenCalledWith('dark')
    })

    it('从 dark 切换应该触发 light', async () => {
      const onChange = vi.fn()

      const wrapper = mount(ThemeToggle, {
        props: {
          theme: 'dark',
          onChange,
        },
      })

      await wrapper.find('.theme-toggle').trigger('click')

      expect(onChange).toHaveBeenCalledWith('light')
    })
  })

  describe('插槽', () => {
    it('应该支持 default 插槽', () => {
      const wrapper = mount(ThemeToggle, {
        props: { theme: 'light' },
        slots: {
          default: '<span class="custom-toggle">自定义切换</span>',
        },
      })

      expect(wrapper.html()).toContain('custom-toggle')
    })

    it('default 插槽应该接收 isDark 和 toggle 参数', () => {
      const wrapper = mount(ThemeToggle, {
        props: { theme: 'dark' },
        slots: {
          default: ({ isDark, toggle }) => {
            return `<span data-dark="${isDark}" @click="${toggle}">Theme</span>`
          },
        },
      })

      expect(wrapper.html()).toContain('data-dark="true"')
    })
  })

  describe('Expose', () => {
    it('应该暴露 isDark 和 handleToggle', () => {
      const wrapper = mount(ThemeToggle, {
        props: { theme: 'dark' },
      })

      expect(wrapper.vm.isDark).toBe(true)
      expect(typeof wrapper.vm.handleToggle).toBe('function')
    })
  })

  describe('样式', () => {
    it('dark 状态应该有 is-dark 类', () => {
      const wrapper = mount(ThemeToggle, {
        props: { theme: 'dark' },
      })

      expect(wrapper.find('.theme-toggle.is-dark').exists()).toBe(true)
    })

    it('light 状态不应该有 is-dark 类', () => {
      const wrapper = mount(ThemeToggle, {
        props: { theme: 'light' },
      })

      expect(wrapper.find('.theme-toggle.is-dark').exists()).toBe(false)
    })
  })
})