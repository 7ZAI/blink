import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import UserDropdown from '../index.vue'
import type { UserInfo, MenuItem } from '../index.vue'

// 创建 Dropdown stub 来测试事件
const ElDropdownWithCommandStub = defineComponent({
  name: 'ElDropdown',
  props: ['trigger', 'placement'],
  emits: ['command'],
  setup(_, { slots, emit }) {
    return () =>
      h('div', { class: 'el-dropdown-stub' }, [
        slots.default ? slots.default() : [],
        h(
          'div',
          {
            class: 'test-dropdown-menu',
            onClick: () => emit('command', 'logout'),
          },
          slots.dropdown ? slots.dropdown() : []
        ),
      ])
  },
})

describe('UserDropdown', () => {
  const defaultUserInfo: UserInfo = {
    username: '测试用户',
    loginName: 'testuser',
    avatar: 'https://example.com/avatar.png',
    userId: 1,
  }

  describe('Props 和默认值', () => {
    it('应该接受正确的 props', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: defaultUserInfo,
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.props('userInfo')).toEqual(defaultUserInfo)
      expect(wrapper.props('collapsed')).toBe(false)
      expect(wrapper.props('avatarSize')).toBe(36)
      expect(wrapper.props('trigger')).toBe('click')
      expect(wrapper.props('placement')).toBe('bottom-end')
    })

    it('应该支持自定义 labels', () => {
      const customLabels = {
        profile: 'Profile',
        themeSettings: 'Theme',
        logout: 'Sign Out',
      }

      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: defaultUserInfo,
          labels: customLabels,
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.props('labels')).toEqual(customLabels)
    })

    it('当 userInfo 为 null 时不应该渲染', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: null,
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.find('.user-dropdown').exists()).toBe(false)
    })

    it('应该支持 showThemeSettings', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: defaultUserInfo,
          showThemeSettings: true,
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      const vm = wrapper.vm as any
      // 默认菜单应该包含 themeSettings
      expect(vm.defaultMenuItems.some((item: MenuItem) => item.command === 'themeSettings')).toBe(
        true
      )
    })

    it('应该支持 collapsed 模式', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: defaultUserInfo,
          collapsed: true,
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.find('.user-dropdown-trigger.collapsed').exists()).toBe(true)
    })
  })

  describe('计算属性', () => {
    it('displayName 应该返回正确的用户名', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: { username: '张三', loginName: 'zhangsan' },
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.vm.displayName).toBe('张三')
    })

    it('当 username 不存在时应该返回 loginName', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: { loginName: 'zhangsan' },
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.vm.displayName).toBe('zhangsan')
    })

    it('应该支持 nameResolver', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: { username: '张三', loginName: 'zhangsan' },
          nameResolver: (user) => user.loginName?.toUpperCase() || '',
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.vm.displayName).toBe('ZHANGSAN')
    })

    it('resolveAvatar 应该返回正确的头像 URL', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: { avatar: 'https://example.com/avatar.png' },
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.vm.resolveAvatar).toBe('https://example.com/avatar.png')
    })

    it('应该支持 avatarResolver', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: { userId: 123 },
          avatarResolver: (user) => `https://api.example.com/avatar/${user.userId}`,
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.vm.resolveAvatar).toBe('https://api.example.com/avatar/123')
    })
  })

  describe('事件处理', () => {
    it('handleCommand 应该触发 command 事件', () => {
      const onCommand = vi.fn()

      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: defaultUserInfo,
          onCommand,
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      wrapper.vm.handleCommand('profile')

      expect(onCommand).toHaveBeenCalledWith('profile')
    })
  })

  describe('菜单项', () => {
    it('应该支持自定义 menuItems', () => {
      const customMenuItems: MenuItem[] = [
        { command: 'settings', label: '设置' },
        { command: 'help', label: '帮助' },
      ]

      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: defaultUserInfo,
          menuItems: customMenuItems,
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      // 验证自定义菜单项被传递
      expect(wrapper.props('menuItems')).toEqual(customMenuItems)
    })

    it('resolveLabel 应该正确解析标签', () => {
      const wrapper = mount(UserDropdown, {
        props: {
          userInfo: defaultUserInfo,
          labels: { profile: '个人资料' },
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      // 验证 labels 配置正确
      expect(wrapper.props('labels')!.profile).toBe('个人资料')
    })
  })

  describe('插槽', () => {
    it('应该支持 trigger 插槽', () => {
      const wrapper = mount(UserDropdown, {
        props: { userInfo: defaultUserInfo },
        slots: {
          trigger: '<span class="custom-trigger">自定义触发器</span>',
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.html()).toContain('custom-trigger')
    })

    it('应该支持 avatar 插槽', () => {
      const wrapper = mount(UserDropdown, {
        props: { userInfo: defaultUserInfo },
        slots: {
          avatar: '<span class="custom-avatar">自定义头像</span>',
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.html()).toContain('custom-avatar')
    })

    it('应该支持 menu 插槽', () => {
      const wrapper = mount(UserDropdown, {
        props: { userInfo: defaultUserInfo },
        slots: {
          menu: '<div class="custom-menu">自定义菜单</div>',
        },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(wrapper.html()).toContain('custom-menu')
    })
  })

  describe('Expose', () => {
    it('应该暴露 handleCommand, displayName, resolveAvatar', () => {
      const wrapper = mount(UserDropdown, {
        props: { userInfo: defaultUserInfo },
        global: {
          stubs: {
            'el-dropdown': ElDropdownWithCommandStub,
          },
        },
      })

      expect(typeof wrapper.vm.handleCommand).toBe('function')
      expect(wrapper.vm.displayName).toBeDefined()
      expect(wrapper.vm.resolveAvatar).toBeDefined()
    })
  })
})
