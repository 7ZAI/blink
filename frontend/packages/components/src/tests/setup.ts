import { defineComponent, h } from 'vue'
import { config } from '@vue/test-utils'

// 使用 defineComponent 创建带有 name 的 stub 组件
const ElDialogStub = defineComponent({
  name: 'ElDialog',
  props: ['modelValue', 'title', 'width', 'showFooter', 'beforeClose', 'closeOnClickModal', 'closeOnPressEscape', 'showClose', 'lockScroll', 'destroyOnClose'],
  emits: ['update:modelValue', 'open', 'opened', 'close', 'closed'],
  setup(props, { slots }) {
    return () => {
      if (!props.modelValue) return null
      return h('div', { class: 'el-dialog-stub' }, [
        h('div', { class: 'el-dialog__header' }, slots.header ? slots.header() : h('span', { class: 'el-dialog__title' }, props.title)),
        h('div', { class: 'el-dialog__body' }, slots.default ? slots.default() : []),
        props.showFooter !== false ? h('div', { class: 'el-dialog__footer' }, slots.footer ? slots.footer() : []) : null,
      ])
    }
  },
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data', 'rowKey', 'loading', 'stripe', 'border'],
  emits: ['selection-change', 'select', 'select-all', 'sort-change', 'row-click', 'row-dblclick'],
  setup(props, { slots }) {
    return () => h('table', { class: 'el-table-stub' }, slots.default ? slots.default() : [])
  },
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['prop', 'label', 'width', 'type', 'fixed', 'sortable', 'showOverflowTooltip', 'align', 'minWidth'],
  setup(_, { slots }) {
    return () => h('th', { class: 'el-table-column-stub' }, [
      slots.default ? slots.default({ row: {}, column: {}, $index: 0 }) : [],
      slots.header ? slots.header({ column: {}, $index: 0 }) : [],
    ])
  },
})

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: ['type', 'loading', 'disabled', 'link'],
  emits: ['click'],
  setup(props, { slots, emit }) {
    return () => h('button', {
      class: 'el-button-stub',
      disabled: props.disabled,
      onClick: () => emit('click'),
    }, slots.default ? slots.default() : [])
  },
})

const ElTagStub = defineComponent({
  name: 'ElTag',
  props: ['type'],
  setup(props, { slots }) {
    return () => h('span', { class: `el-tag-stub el-tag--${props.type || 'info'}` }, slots.default ? slots.default() : [])
  },
})

const ElImageStub = defineComponent({
  name: 'ElImage',
  props: ['src'],
  setup(props) {
    return () => h('img', { class: 'el-image-stub', src: props.src })
  },
})

const ElSwitchStub = defineComponent({
  name: 'ElSwitch',
  props: ['modelValue', 'disabled'],
  emits: ['update:modelValue', 'change'],
  setup(props, { slots, emit }) {
    return () => h('div', {
      class: 'el-switch-stub',
      role: 'switch',
      'aria-checked': props.modelValue,
      onClick: () => {
        if (!props.disabled) {
          const newValue = !props.modelValue
          emit('update:modelValue', newValue)
          emit('change', newValue)
        }
      },
    }, slots.default ? slots.default() : [])
  },
})

const ElFormStub = defineComponent({
  name: 'ElForm',
  props: ['model', 'rules', 'labelPosition', 'labelWidth'],
  emits: ['validate'],
  setup(props, { slots }) {
    return () => h('form', { class: 'el-form-stub' }, slots.default ? slots.default() : [])
  },
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  props: ['label', 'prop'],
  setup(_, { slots }) {
    return () => h('div', { class: 'el-form-item-stub' }, slots.default ? slots.default() : [])
  },
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'placeholder', 'maxlength', 'showWordLimit', 'disabled'],
  emits: ['update:modelValue', 'change'],
  setup(props, { slots, emit }) {
    return () => h('input', {
      class: 'el-input-stub',
      value: props.modelValue,
      placeholder: props.placeholder,
      disabled: props.disabled,
      onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLInputElement).value),
      onChange: (e: Event) => emit('change', (e.target as HTMLInputElement).value),
    }, slots.prefix ? slots.prefix() : [])
  },
})

const ElColorPickerStub = defineComponent({
  name: 'ElColorPicker',
  props: ['modelValue', 'predefine', 'disabled'],
  emits: ['update:modelValue', 'change'],
  setup(props, { emit }) {
    return () => h('div', {
      class: 'el-color-picker-stub',
      onClick: () => {
        if (!props.disabled) {
          emit('change', props.modelValue)
        }
      },
    })
  },
})

const ElDropdownStub = defineComponent({
  name: 'ElDropdown',
  props: ['trigger', 'placement'],
  emits: ['command'],
  setup(props, { slots, emit }) {
    return () => h('div', {
      class: 'el-dropdown-stub',
      onClick: () => emit('command', 'test'),
    }, [
      slots.default ? slots.default() : [],
      slots.dropdown ? slots.dropdown() : [],
    ])
  },
})

const ElDropdownMenuStub = defineComponent({
  name: 'ElDropdownMenu',
  setup(_, { slots }) {
    return () => h('div', { class: 'el-dropdown-menu-stub' }, slots.default ? slots.default() : [])
  },
})

const ElDropdownItemStub = defineComponent({
  name: 'ElDropdownItem',
  props: ['command', 'divided', 'disabled'],
  emits: ['click'],
  setup(props, { slots, emit }) {
    return () => h('div', {
      class: 'el-dropdown-item-stub',
      onClick: () => emit('click', props.command),
    }, slots.default ? slots.default() : [])
  },
})

const ElAvatarStub = defineComponent({
  name: 'ElAvatar',
  props: ['src', 'size'],
  setup(_, { slots }) {
    return () => h('div', { class: 'el-avatar-stub' }, slots.default ? slots.default() : [])
  },
})

const ElIconStub = defineComponent({
  name: 'ElIcon',
  props: ['class'],
  setup(_, { slots }) {
    return () => h('i', { class: 'el-icon-stub' }, slots.default ? slots.default() : [])
  },
})

// 全局注册 Element Plus stub 组件
config.global.stubs = {
  'el-dialog': ElDialogStub,
  'el-table': ElTableStub,
  'el-table-column': ElTableColumnStub,
  'el-button': ElButtonStub,
  'el-tag': ElTagStub,
  'el-image': ElImageStub,
  'el-switch': ElSwitchStub,
  'el-form': ElFormStub,
  'el-form-item': ElFormItemStub,
  'el-input': ElInputStub,
  'el-color-picker': ElColorPickerStub,
  'el-dropdown': ElDropdownStub,
  'el-dropdown-menu': ElDropdownMenuStub,
  'el-dropdown-item': ElDropdownItemStub,
  'el-avatar': ElAvatarStub,
  'el-icon': ElIconStub,
}

// 全局注册 loading 指令
config.global.directives = {
  loading: {
    mounted: () => {},
    updated: () => {},
  },
}

// Mock i18n
config.global.mocks = {
  $t: (key: string) => key,
  t: (key: string) => key,
  locale: 'zh_cn',
}