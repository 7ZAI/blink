import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BlinkTable from '../index.vue'
import type { TableOperation, TableColumn } from '../types'

describe('BlinkTable', () => {
  const mockData = [
    { id: 1, name: '张三', status: 1 },
    { id: 2, name: '李四', status: 2 },
    { id: 3, name: '王五', status: 1 },
  ]

  describe('Props 和默认值', () => {
    it('应该接受正确的 props', () => {
      const wrapper = mount(BlinkTable, {
        props: { data: mockData },
      })

      expect(wrapper.props('data')).toEqual(mockData)
      expect(wrapper.props('rowKey')).toBe('id')
      expect(wrapper.props('stripe')).toBe(true)
      expect(wrapper.props('border')).toBe(true)
      expect(wrapper.props('showOverflowTooltip')).toBe(true)
    })

    it('selectType 默认值应为 checkbox', () => {
      const wrapper = mount(BlinkTable, {
        props: { data: mockData, selectable: true },
      })

      expect(wrapper.props('selectType')).toBe('checkbox')
    })
  })

  describe('排序事件', () => {
    it('应该正确处理 sort-change 事件，order 为 ascending', async () => {
      const onSortChange = vi.fn()

      const wrapper = mount(BlinkTable, {
        props: {
          data: mockData,
          onSortChange,
        },
      })

      const table = wrapper.findComponent({ name: 'el-table' })
      await table.vm.$emit('sort-change', { prop: 'name', order: 'ascending' })

      expect(onSortChange).toHaveBeenCalledWith({ prop: 'name', order: 'ascending' })
    })

    it('当 order 为 null 时应该转换为空字符串', async () => {
      const onSortChange = vi.fn()

      const wrapper = mount(BlinkTable, {
        props: {
          data: mockData,
          onSortChange,
        },
      })

      const table = wrapper.findComponent({ name: 'el-table' })
      await table.vm.$emit('sort-change', { prop: 'name', order: null })

      expect(onSortChange).toHaveBeenCalledWith({ prop: 'name', order: '' })
    })

    it('当 order 为 descending 时应该保持不变', async () => {
      const onSortChange = vi.fn()

      const wrapper = mount(BlinkTable, {
        props: {
          data: mockData,
          onSortChange,
        },
      })

      const table = wrapper.findComponent({ name: 'el-table' })
      await table.vm.$emit('sort-change', { prop: 'status', order: 'descending' })

      expect(onSortChange).toHaveBeenCalledWith({ prop: 'status', order: 'descending' })
    })
  })

  describe('行事件', () => {
    it('应该正确处理 row-click 事件', async () => {
      const onRowClick = vi.fn()

      const wrapper = mount(BlinkTable, {
        props: {
          data: mockData,
          onRowClick,
        },
      })

      const table = wrapper.findComponent({ name: 'el-table' })
      const mockEvent = new Event('click')
      await table.vm.$emit('row-click', mockData[0], null, mockEvent)

      expect(onRowClick).toHaveBeenCalledWith(mockData[0], null, mockEvent)
    })

    it('应该正确处理 row-dblclick 事件', async () => {
      const onRowDblclick = vi.fn()

      const wrapper = mount(BlinkTable, {
        props: {
          data: mockData,
          onRowDblclick,
        },
      })

      const table = wrapper.findComponent({ name: 'el-table' })
      const mockEvent = new Event('dblclick')
      await table.vm.$emit('row-dblclick', mockData[1], null, mockEvent)

      expect(onRowDblclick).toHaveBeenCalledWith(mockData[1], null, mockEvent)
    })
  })

  describe('选择事件', () => {
    it('应该正确处理 selection-change 事件', async () => {
      const onSelectionChange = vi.fn()
      const onUpdateSelectedKeys = vi.fn()

      const wrapper = mount(BlinkTable, {
        props: {
          data: mockData,
          selectable: true,
          onSelectionChange,
          'onUpdate:selectedKeys': onUpdateSelectedKeys,
        },
      })

      const table = wrapper.findComponent({ name: 'el-table' })
      await table.vm.$emit('selection-change', mockData)

      expect(onSelectionChange).toHaveBeenCalledWith(mockData)
      expect(onUpdateSelectedKeys).toHaveBeenCalledWith([1, 2, 3])
    })
  })

  describe('Expose 方法', () => {
    it('应该暴露 clearSelection 方法', () => {
      const wrapper = mount(BlinkTable, {
        props: { data: mockData },
      })

      expect(typeof wrapper.vm.clearSelection).toBe('function')
    })

    it('应该暴露所有表格方法', () => {
      const wrapper = mount(BlinkTable, {
        props: { data: mockData },
      })

      const vm = wrapper.vm as any
      expect(typeof vm.clearSelection).toBe('function')
      expect(typeof vm.toggleRowSelection).toBe('function')
      expect(typeof vm.toggleAllSelection).toBe('function')
      expect(typeof vm.setCurrentRow).toBe('function')
      expect(typeof vm.clearSort).toBe('function')
      expect(typeof vm.clearFilter).toBe('function')
      expect(typeof vm.doLayout).toBe('function')
    })
  })

  describe('操作按钮配置', () => {
    it('onClick 回调签名应包含 row 和 index', () => {
      const mockOnClick = vi.fn()
      const operations: TableOperation[] = [
        {
          label: '编辑',
          onClick: mockOnClick,
        },
      ]

      // 验证类型定义正确（编译时检查）
      // TableOperation.onClick 类型应为 (row: T, index: number) => void
      expect(operations[0]!.onClick).toBeDefined()
      expect(typeof operations[0]!.onClick).toBe('function')
    })

    it('visible 和 disabled 函数应正确工作', () => {
      const operations: TableOperation[] = [
        {
          label: '删除',
          visible: (row: any) => row.status === 1,
          disabled: (row: any) => row.status === 2,
          onClick: vi.fn(),
        },
      ]

      // 测试 visible 函数
      expect(operations[0]!.visible?.(mockData[0])).toBe(true)
      expect(operations[0]!.visible?.(mockData[1])).toBe(false)

      // 测试 disabled 函数
      expect(operations[0]!.disabled?.(mockData[0])).toBe(false)
      expect(operations[0]!.disabled?.(mockData[1])).toBe(true)
    })
  })

  describe('列类型验证', () => {
    it('tagOptions 应正确映射', () => {
      const columns: TableColumn[] = [
        {
          prop: 'status',
          label: '状态',
          type: 'tag',
          tagOptions: [
            { value: 1, label: '启用', type: 'success' },
            { value: 2, label: '禁用', type: 'danger' },
          ],
        },
      ]

      const tagOptions = columns[0]!.tagOptions!
      expect(tagOptions.length).toBe(2)
      expect(tagOptions[0]!.value).toBe(1)
      expect(tagOptions[0]!.label).toBe('启用')
      expect(tagOptions[0]!.type).toBe('success')
    })

    it('formatter 函数应正确定义', () => {
      const columns: TableColumn[] = [
        {
          prop: 'name',
          label: '姓名',
          formatter: (_row: any, _column: any, cellValue: any) => {
            return cellValue ? `用户: ${cellValue}` : '-'
          },
        },
      ]

      const result = columns[0]!.formatter!({ name: '张三' }, {}, '张三')
      expect(result).toBe('用户: 张三')
    })
  })
})
