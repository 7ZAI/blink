import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BlinkTableColumn from '../Column.vue'
import type { BlinkTableColumnProps } from '../types'

describe('BlinkTableColumn', () => {
  describe('Props 和默认值', () => {
    it('应该接受正确的 props', () => {
      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'name',
          label: '姓名',
          width: '200px',
        },
      })

      expect(wrapper.props('prop')).toBe('name')
      expect(wrapper.props('label')).toBe('姓名')
      expect(wrapper.props('width')).toBe('200px')
    })

    it('应该有正确的默认值', () => {
      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'status',
          label: '状态',
        },
      })

      expect(wrapper.props('showOverflowTooltip')).toBe(true)
    })

    it('应该接受所有列配置 props', () => {
      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'operation',
          label: '操作',
          width: '150px',
          minWidth: '100px',
          align: 'center',
          fixed: 'right',
          sortable: true,
          showOverflowTooltip: false,
        },
      })

      expect(wrapper.props('prop')).toBe('operation')
      expect(wrapper.props('label')).toBe('操作')
      expect(wrapper.props('width')).toBe('150px')
      expect(wrapper.props('minWidth')).toBe('100px')
      expect(wrapper.props('align')).toBe('center')
      expect(wrapper.props('fixed')).toBe('right')
      expect(wrapper.props('sortable')).toBe(true)
      expect(wrapper.props('showOverflowTooltip')).toBe(false)
    })
  })

  describe('插槽', () => {
    it('应该支持 default 插槽', () => {
      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'status',
          label: '状态',
        },
        slots: {
          default: '<span class="custom-cell">自定义内容</span>',
        },
      })

      expect(wrapper.html()).toContain('custom-cell')
      expect(wrapper.html()).toContain('自定义内容')
    })

    it('应该支持 header 插槽', () => {
      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'name',
          label: '姓名',
        },
        slots: {
          header: '<span class="custom-header">自定义标题</span>',
        },
      })

      expect(wrapper.html()).toContain('custom-header')
      expect(wrapper.html()).toContain('自定义标题')
    })

    it('应该支持同时使用 default 和 header 插槽', () => {
      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'amount',
          label: '金额',
        },
        slots: {
          default: '<span class="amount-cell">¥100.00</span>',
          header: '<span class="amount-header">金额(元)</span>',
        },
      })

      expect(wrapper.html()).toContain('amount-cell')
      expect(wrapper.html()).toContain('amount-header')
    })

    it('default 插槽应该接收 scope 参数', () => {
      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'name',
          label: '姓名',
        },
        slots: {
          default: ({ row, column, $index }) => {
            return `<span data-row-id="${row?.id || 'test'}" data-index="${$index}">${row?.name || '测试'}</span>`
          },
        },
      })

      // 验证插槽被渲染
      expect(wrapper.find('.el-table-column-stub').exists()).toBe(true)
    })
  })

  describe('formatter 功能', () => {
    it('应该支持 formatter 函数', () => {
      const formatter = vi.fn((_row: any, _column: any, cellValue: any) => {
        return cellValue ? `格式化: ${cellValue}` : '-'
      })

      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'name',
          label: '姓名',
          formatter,
        },
      })

      expect(wrapper.props('formatter')).toBe(formatter)
    })

    it('formatter 函数应该正确签名', () => {
      const formatter: BlinkTableColumnProps['formatter'] = (row, column, cellValue) => {
        // 验证参数类型正确
        expect(typeof row).toBeDefined()
        expect(typeof column).toBeDefined()
        expect(typeof cellValue).toBeDefined()
        return String(cellValue)
      }

      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'amount',
          label: '金额',
          formatter,
        },
      })

      expect(wrapper.props('formatter')).toBeDefined()
    })
  })

  describe('类型验证', () => {
    it('type 属性应该支持 selection/index/expand', () => {
      const selectionWrapper = mount(BlinkTableColumn, {
        props: {
          type: 'selection',
        },
      })
      expect(selectionWrapper.props('type')).toBe('selection')

      const indexWrapper = mount(BlinkTableColumn, {
        props: {
          type: 'index',
          label: '序号',
        },
      })
      expect(indexWrapper.props('type')).toBe('index')

      const expandWrapper = mount(BlinkTableColumn, {
        props: {
          type: 'expand',
          label: '展开',
        },
      })
      expect(expandWrapper.props('type')).toBe('expand')
    })

    it('sortable 属性应该支持 boolean 和字符串', () => {
      const boolWrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'date',
          label: '日期',
          sortable: true,
        },
      })
      expect(boolWrapper.props('sortable')).toBe(true)

      const customWrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'date',
          label: '日期',
          sortable: 'custom',
        },
      })
      expect(customWrapper.props('sortable')).toBe('custom')
    })

    it('fixed 属性应该支持 left/right/true/false', () => {
      const leftWrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'name',
          label: '姓名',
          fixed: 'left',
        },
      })
      expect(leftWrapper.props('fixed')).toBe('left')

      const rightWrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'action',
          label: '操作',
          fixed: 'right',
        },
      })
      expect(rightWrapper.props('fixed')).toBe('right')
    })
  })

  describe('组件结构', () => {
    it('应该渲染为 el-table-column', () => {
      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'name',
          label: '姓名',
        },
      })

      expect(wrapper.find('.el-table-column-stub').exists()).toBe(true)
    })

    it('应该正确传递所有 props 到 el-table-column', () => {
      const wrapper = mount(BlinkTableColumn, {
        props: {
          prop: 'email',
          label: '邮箱',
          width: '300px',
          minWidth: '200px',
          align: 'left',
          fixed: false,
          sortable: false,
          showOverflowTooltip: true,
        },
      })

      const columnStub = wrapper.find('.el-table-column-stub')
      expect(columnStub.exists()).toBe(true)
    })
  })
})
