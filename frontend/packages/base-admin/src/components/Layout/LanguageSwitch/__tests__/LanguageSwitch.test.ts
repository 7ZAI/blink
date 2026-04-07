import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import LanguageSwitch from '../index.vue'
import type { LanguageOption } from '../index.vue'

describe('LanguageSwitch', () => {
  describe('Props 和默认值', () => {
    it('应该接受正确的 props', () => {
      const wrapper = mount(LanguageSwitch, {
        props: {
          currentLanguage: 'en_us',
        },
      })

      expect(wrapper.props('currentLanguage')).toBe('en_us')
      expect(wrapper.props('placement')).toBe('bottom-end')
      expect(wrapper.props('languages')).toEqual([
        { code: 'zh_cn', label: '中文', nativeLabel: '简体中文' },
        { code: 'en_us', label: 'EN', nativeLabel: 'English' },
      ])
    })

    it('应该支持自定义语言列表', () => {
      const customLanguages: LanguageOption[] = [
        { code: 'ja', label: '日本語', nativeLabel: 'Japanese' },
        { code: 'ko', label: '한국어', nativeLabel: 'Korean' },
      ]

      const wrapper = mount(LanguageSwitch, {
        props: {
          currentLanguage: 'ja',
          languages: customLanguages,
        },
      })

      expect(wrapper.props('languages')).toEqual(customLanguages)
    })

    it('应该正确计算 currentLanguageLabel', () => {
      const wrapper = mount(LanguageSwitch, {
        props: {
          currentLanguage: 'zh_cn',
          languages: [
            { code: 'zh_cn', label: '中文' },
            { code: 'en_us', label: 'EN' },
          ],
        },
      })

      expect(wrapper.vm.currentLanguageLabel).toBe('中文')
    })

    it('当找不到语言时应该返回原始值', () => {
      const wrapper = mount(LanguageSwitch, {
        props: {
          currentLanguage: 'fr',
          languages: [
            { code: 'zh_cn', label: '中文' },
            { code: 'en_us', label: 'EN' },
          ],
        },
      })

      expect(wrapper.vm.currentLanguageLabel).toBe('fr')
    })
  })

  describe('事件处理', () => {
    it('handleCommand 应该触发 change 事件', () => {
      const onChange = vi.fn()

      const wrapper = mount(LanguageSwitch, {
        props: {
          currentLanguage: 'zh_cn',
          onChange,
        },
      })

      wrapper.vm.handleCommand('en_us')

      expect(onChange).toHaveBeenCalledWith('en_us')
    })
  })

  describe('插槽', () => {
    it('应该支持 default 插槽', () => {
      const wrapper = mount(LanguageSwitch, {
        props: { currentLanguage: 'zh_cn' },
        slots: {
          default: '<span class="custom-trigger">自定义触发器</span>',
        },
      })

      expect(wrapper.html()).toContain('custom-trigger')
    })

    it('default 插槽应该接收 currentLanguage 和 languages 参数', () => {
      const wrapper = mount(LanguageSwitch, {
        props: {
          currentLanguage: 'en_us',
          languages: [
            { code: 'zh_cn', label: '中文' },
            { code: 'en_us', label: 'EN' },
          ],
        },
        slots: {
          default: ({ currentLanguage, languages }) => {
            return `<span data-lang="${currentLanguage}" data-count="${languages.length}">Lang</span>`
          },
        },
      })

      expect(wrapper.html()).toContain('data-lang="en_us"')
      expect(wrapper.html()).toContain('data-count="2"')
    })

    it('应该支持 menu 插槽', () => {
      const wrapper = mount(LanguageSwitch, {
        props: { currentLanguage: 'zh_cn' },
        slots: {
          menu: '<div class="custom-menu">自定义菜单</div>',
        },
      })

      expect(wrapper.html()).toContain('custom-menu')
    })
  })

  describe('Expose', () => {
    it('应该暴露 currentLanguageLabel 和 handleCommand', () => {
      const wrapper = mount(LanguageSwitch, {
        props: { currentLanguage: 'zh_cn' },
      })

      expect(wrapper.vm.currentLanguageLabel).toBe('中文')
      expect(typeof wrapper.vm.handleCommand).toBe('function')
    })
  })
})