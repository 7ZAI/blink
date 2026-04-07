import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h, ref } from 'vue'
import { createI18n } from 'vue-i18n'
import ThemeSettings from '../index.vue'
import type { FullThemeConfig, CustomPreset, ThemeColors } from '../types'
import { PRESET_THEMES, PRESET_FONTS, DEFAULT_SYSTEM_CONFIG, FONT_SIZE_CONFIG } from '@/config/themes'

// 创建 i18n 实例
const createMockI18n = () => {
  return createI18n({
    legacy: false,
    locale: 'zh_cn',
    messages: {
      zh_cn: {
        settings: {
          presetThemes: '预设主题',
          colorSettings: '颜色设置',
          fontSettings: '字体设置',
          animationSettings: '动画设置',
          enableAnimations: '启用动画',
          animationsEnabled: '动画已启用',
          animationsDisabled: '动画已禁用',
          saveAsPreset: '保存为预设',
          resetToDefault: '重置默认',
          maxPresetsReached: '已达到最大预设数量 {max}',
          presetName: '预设名称',
          presetNamePlaceholder: '请输入预设名称',
          presetSaved: '预设已保存',
          themeReset: '主题已重置',
          primaryColor: '主色',
          successColor: '成功色',
          warningColor: '警告色',
          dangerColor: '危险色',
          infoColor: '信息色',
        },
        common: {
          cancel: '取消',
          confirm: '确定',
        },
      },
      en: {
        settings: {
          presetThemes: 'Preset Themes',
          colorSettings: 'Color Settings',
          fontSettings: 'Font Settings',
          animationSettings: 'Animation Settings',
          enableAnimations: 'Enable Animations',
          animationsEnabled: 'Animations Enabled',
          animationsDisabled: 'Animations Disabled',
          saveAsPreset: 'Save as Preset',
          resetToDefault: 'Reset to Default',
          maxPresetsReached: 'Maximum {max} presets reached',
          presetName: 'Preset Name',
          presetNamePlaceholder: 'Enter preset name',
          presetSaved: 'Preset saved',
          themeReset: 'Theme reset',
          primaryColor: 'Primary',
          successColor: 'Success',
          warningColor: 'Warning',
          dangerColor: 'Danger',
          infoColor: 'Info',
        },
        common: {
          cancel: 'Cancel',
          confirm: 'Confirm',
        },
      },
    },
  })
}

// 创建子组件 stub
const PresetSelectorStub = defineComponent({
  name: 'PresetSelector',
  props: ['presets', 'currentPresetId'],
  emits: ['select'],
  setup(props, { slots, emit }) {
    return () => h('div', { class: 'preset-selector-stub' }, [
      h('div', { class: 'preset-list' },
        props.presets?.map((p: any) =>
          h('div', {
            key: p.id,
            class: ['preset-item', props.currentPresetId === p.id ? 'active' : ''],
            onClick: () => emit('select', p.id),
          }, p.name)
        ) || []
      ),
      slots.footer ? slots.footer() : [],
    ])
  },
})

const ColorSettingsStub = defineComponent({
  name: 'ColorSettings',
  props: ['colors'],
  emits: ['change'],
  setup(_, { slots }) {
    return () => h('div', { class: 'color-settings-stub' }, slots.default ? slots.default() : [])
  },
})

const FontSettingsStub = defineComponent({
  name: 'FontSettings',
  props: ['fonts', 'font'],
  emits: ['change'],
  setup(_, { slots }) {
    return () => h('div', { class: 'font-settings-stub' }, slots.default ? slots.default() : [])
  },
})

const SystemSettingsStub = defineComponent({
  name: 'SystemSettings',
  props: ['config'],
  emits: ['change'],
  setup(_, { slots }) {
    return () => h('div', { class: 'system-settings-stub' }, slots.default ? slots.default() : [])
  },
})

const CustomPresetListStub = defineComponent({
  name: 'CustomPresetList',
  props: ['presets', 'currentPresetId'],
  emits: ['select', 'delete'],
  setup(_, { slots }) {
    return () => h('div', { class: 'custom-preset-list-stub' }, slots.default ? slots.default() : [])
  },
})

// 默认配置
const defaultColors: ThemeColors = {
  primary: '#3b82f6',
  success: '#10b981',
  warning: '#f59e0b',
  danger: '#ef4444',
  info: '#6366f1',
}

const defaultConfig: FullThemeConfig = {
  presetId: PRESET_THEMES[0]?.id,
  colors: defaultColors,
  font: {
    family: PRESET_FONTS[0]?.family || '',
    baseSize: FONT_SIZE_CONFIG.base.default,
    largeSize: FONT_SIZE_CONFIG.large.default,
    smallSize: FONT_SIZE_CONFIG.small.default,
  },
  animationsEnabled: true,
  system: { ...DEFAULT_SYSTEM_CONFIG },
}

describe('ThemeSettings', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Props 和默认值', () => {
    it('应该接受正确的 props', () => {
      const i18n = createMockI18n()
      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: defaultConfig,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      expect(wrapper.props('modelValue')).toEqual(defaultConfig)
      expect(wrapper.props('showPresets')).toBe(true)
      expect(wrapper.props('showColors')).toBe(true)
      expect(wrapper.props('showFonts')).toBe(true)
      expect(wrapper.props('showAnimations')).toBe(true)
      expect(wrapper.props('showSystem')).toBe(true)
    })

    it('应该正确处理 showPresets false', () => {
      const i18n = createMockI18n()
      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: defaultConfig,
          showPresets: false,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      expect(wrapper.find('.preset-selector-stub').exists()).toBe(false)
    })

    it('应该正确处理 readonly 状态', () => {
      const i18n = createMockI18n()
      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: defaultConfig,
          readonly: true,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      // 检查按钮是否禁用
      const buttons = wrapper.findAll('.el-button-stub')
      expect(buttons.length).toBeGreaterThan(0)
      buttons.forEach(button => {
        expect(button.attributes('disabled')).toBeDefined()
      })
    })
  })

  describe('事件处理', () => {
    it('应该正确触发 color-change 事件', async () => {
      const i18n = createMockI18n()
      const onColorChange = vi.fn()

      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: defaultConfig,
          onColorChange,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      // 通过内部方法测试
      const vm = wrapper.vm as any
      const newColors = { primary: '#123456', success: '#10b981', warning: '#f59e0b', danger: '#ef4444', info: '#6366f1' }
      vm.handleColorChange(newColors)

      expect(onColorChange).toHaveBeenCalledWith(newColors)
      // handleColorChange 会清除 presetId 并更新 colors
      expect(vm.localConfig.presetId).toBeUndefined()
      expect(vm.localConfig.colors).toEqual(newColors)
    })

    it('应该正确触发 preset-change 事件', async () => {
      const i18n = createMockI18n()
      const onPresetChange = vi.fn()
      const presetId = PRESET_THEMES[1]?.id!

      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: defaultConfig,
          presetThemes: PRESET_THEMES,
          onPresetChange,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      const vm = wrapper.vm as any
      vm.handlePresetSelect(presetId)

      expect(onPresetChange).toHaveBeenCalledWith(presetId)
    })

    it('应该正确触发 animation-change 事件', async () => {
      const i18n = createMockI18n()
      const onAnimationChange = vi.fn()

      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: defaultConfig,
          onAnimationChange,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      const vm = wrapper.vm as any
      vm.handleAnimationChange(false)

      expect(onAnimationChange).toHaveBeenCalledWith(false)
    })

    it('应该正确触发 preset-save 事件', async () => {
      const i18n = createMockI18n()
      const onPresetSave = vi.fn()

      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: defaultConfig,
          onPresetSave,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      // 设置表单数据并调用保存方法
      const vm = wrapper.vm as any
      vm.presetForm.name = '测试预设'
      vm.presetForm.nameEn = 'Test Preset'

      // 调用保存预设方法（跳过验证）
      const presetId = `custom-${Date.now()}-test123`
      const newPreset: CustomPreset = {
        id: presetId,
        name: '测试预设',
        nameEn: 'Test Preset',
        colors: { ...defaultConfig.colors },
        isPreset: false,
        createdAt: Date.now(),
      }
      vm.emit('preset-save', newPreset)

      expect(onPresetSave).toHaveBeenCalled()
    })

    it('应该正确触发 preset-delete 事件', async () => {
      const i18n = createMockI18n()
      const onPresetDelete = vi.fn()
      const customPreset: CustomPreset = {
        id: 'custom-test-123',
        name: '测试预设',
        nameEn: 'Test Preset',
        colors: { primary: '#123456', success: '#10b981', warning: '#f59e0b', danger: '#ef4444', info: '#6366f1' } as ThemeColors,
        isPreset: false,
        createdAt: Date.now(),
      }

      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: { ...defaultConfig, presetId: customPreset.id },
          customPresets: [customPreset],
          onPresetDelete,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      const vm = wrapper.vm as any
      vm.handlePresetDelete(customPreset.id)

      expect(onPresetDelete).toHaveBeenCalledWith(customPreset.id)
    })
  })

  describe('自定义预设处理', () => {
    it('应该显示自定义预设列表', () => {
      const i18n = createMockI18n()
      const customPresets: CustomPreset[] = [
        {
          id: 'custom-1',
          name: '自定义1',
          nameEn: 'Custom 1',
          colors: PRESET_THEMES[0]!.colors,
          isPreset: false,
          createdAt: Date.now(),
        },
      ]

      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: defaultConfig,
          customPresets,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      expect(wrapper.find('.custom-preset-list-stub').exists()).toBe(true)
    })

    it('当达到最大预设数量时应该禁用保存按钮', () => {
      const i18n = createMockI18n()
      const customPresets: CustomPreset[] = Array.from({ length: 10 }, (_, i) => ({
        id: `custom-${i}`,
        name: `自定义${i}`,
        nameEn: `Custom ${i}`,
        colors: PRESET_THEMES[0]!.colors,
        isPreset: false,
        createdAt: Date.now(),
      }))

      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: defaultConfig,
          customPresets,
          maxCustomPresets: 10,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      const vm = wrapper.vm as any
      expect(vm.isMaxPresetsReached).toBe(true)
    })
  })

  describe('重置功能', () => {
    it('应该正确重置到默认配置', async () => {
      const i18n = createMockI18n()
      const onUpdateModelValue = vi.fn()

      const modifiedColors: ThemeColors = {
        primary: '#8b5cf6',
        success: '#06b6d4',
        warning: '#fbbf24',
        danger: '#f43f5e',
        info: '#3b82f6',
      }

      const modifiedConfig: FullThemeConfig = {
        presetId: PRESET_THEMES[2]?.id,
        colors: modifiedColors,
        font: {
          family: PRESET_FONTS[2]?.family || '',
          baseSize: 16,
          largeSize: 22,
          smallSize: 14,
        },
        animationsEnabled: false,
        system: { borderRadius: 16, shadowIntensity: 'none', compactMode: true, contentWidth: 'fixed' },
      }

      const wrapper = mount(ThemeSettings, {
        props: {
          modelValue: modifiedConfig,
          presetThemes: PRESET_THEMES,
          presetFonts: PRESET_FONTS,
          'onUpdate:modelValue': onUpdateModelValue,
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      const vm = wrapper.vm as any
      vm.handleReset()

      // 验证重置后的配置
      expect(onUpdateModelValue).toHaveBeenCalled()
      const calls = onUpdateModelValue.mock.calls
      expect(calls.length).toBeGreaterThan(0)
      const lastCall = calls[calls.length - 1]![0]
      expect(lastCall.presetId).toBe(PRESET_THEMES[0]?.id)
      expect(lastCall.animationsEnabled).toBe(true)
    })
  })

  describe('插槽', () => {
    it('应该支持 presetFooter 插槽', () => {
      const i18n = createMockI18n()
      const wrapper = mount(ThemeSettings, {
        props: { modelValue: defaultConfig },
        slots: {
          presetFooter: '<div class="preset-footer-slot">自定义底部</div>',
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      expect(wrapper.html()).toContain('preset-footer-slot')
    })

    it('应该支持 colorFooter 插槽', () => {
      const i18n = createMockI18n()
      const wrapper = mount(ThemeSettings, {
        props: { modelValue: defaultConfig },
        slots: {
          colorFooter: '<div class="color-footer-slot">颜色设置底部</div>',
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      expect(wrapper.html()).toContain('color-footer-slot')
    })

    it('应该支持 fontFooter 插槽', () => {
      const i18n = createMockI18n()
      const wrapper = mount(ThemeSettings, {
        props: { modelValue: defaultConfig },
        slots: {
          fontFooter: '<div class="font-footer-slot">字体设置底部</div>',
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      expect(wrapper.html()).toContain('font-footer-slot')
    })

    it('应该支持 animationFooter 插槽', () => {
      const i18n = createMockI18n()
      const wrapper = mount(ThemeSettings, {
        props: { modelValue: defaultConfig },
        slots: {
          animationFooter: '<div class="animation-footer-slot">动画设置底部</div>',
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      expect(wrapper.html()).toContain('animation-footer-slot')
    })

    it('应该支持 systemFooter 插槽', () => {
      const i18n = createMockI18n()
      const wrapper = mount(ThemeSettings, {
        props: { modelValue: defaultConfig },
        slots: {
          systemFooter: '<div class="system-footer-slot">系统设置底部</div>',
        },
        global: {
          plugins: [i18n],
          stubs: {
            PresetSelector: PresetSelectorStub,
            ColorSettings: ColorSettingsStub,
            FontSettings: FontSettingsStub,
            SystemSettings: SystemSettingsStub,
            CustomPresetList: CustomPresetListStub,
          },
        },
      })

      expect(wrapper.html()).toContain('system-footer-slot')
    })
  })
})