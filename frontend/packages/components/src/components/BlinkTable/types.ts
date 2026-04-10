// src/components/BlinkTable/types.ts

/**
 * 列配置类型
 */
export interface TableColumn<T = any> {
  /** 字段名 */
  prop: string
  /** 列标题 */
  label: string
  /** 列宽 */
  width?: string | number
  /** 最小列宽 */
  minWidth?: string | number
  /** 对齐方式 */
  align?: 'left' | 'center' | 'right'
  /** 固定列 */
  fixed?: 'left' | 'right' | boolean

  /** 特殊列类型 */
  type?: 'index' | 'selection' | 'expand' | 'tag' | 'image' | 'datetime'
  /** 格式化函数 */
  formatter?: (row: T, column: any, cellValue: any) => string

  /** 标签类型（type='tag' 时） */
  tagType?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | ((row: T) => string)
  /** 标签选项映射 */
  tagOptions?: { label: string; value: any; type?: string }[]

  /** 图片宽度（type='image' 时） */
  imageWidth?: number
  /** 图片高度（type='image' 时） */
  imageHeight?: number

  /** 排序 */
  sortable?: boolean | 'custom'
  /** 是否显示溢出 tooltip */
  showOverflowTooltip?: boolean

  /** 自定义插槽名 */
  slot?: string
}

/**
 * BlinkTableColumn Props 类型（插槽模式使用）
 */
export interface BlinkTableColumnProps {
  /** 字段名 */
  prop?: string
  /** 列标题 */
  label?: string
  /** 列宽 */
  width?: string | number
  /** 最小列宽 */
  minWidth?: string | number
  /** 对齐方式 */
  align?: 'left' | 'center' | 'right'
  /** 固定列 */
  fixed?: 'left' | 'right' | boolean
  /** 排序 */
  sortable?: boolean | 'custom'
  /** 列类型 */
  type?: 'index' | 'selection' | 'expand'
  /** 是否显示溢出 tooltip */
  showOverflowTooltip?: boolean
  /** 格式化函数 */
  formatter?: (row: any, column: any, cellValue: any) => string
}

/**
 * 操作按钮配置
 */
export interface TableOperation<T = any> {
  /** 按钮文本 */
  label: string
  /** 按钮类型 */
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  /** 图标 */
  icon?: string
  /** 是否为链接样式 */
  link?: boolean
  /** 是否显示 */
  visible?: (row: T) => boolean
  /** 是否禁用 */
  disabled?: (row: T) => boolean
  /** 点击回调（row: 当前行数据, index: 行索引） */
  onClick: (row: T, index: number) => void
  /**
   * 权限标识
   * 注意：此字段需要在父组件配合 AuthButton 或权限指令使用
   * 组件本身不直接实现权限校验
   */
  permission?: string
}

/**
 * BlinkTable Props 类型
 */
export interface BlinkTableProps<T = any> {
  /** 表格数据 */
  data: T[]
  /** 行唯一标识 */
  rowKey?: string | ((row: T) => string)

  /** 列配置（配置模式） */
  columns?: TableColumn<T>[]

  /** 是否可选择 */
  selectable?: boolean
  /** 选中的行 keys */
  selectedKeys?: (string | number)[]
  /**
   * 选择类型
   * 当前仅支持 'checkbox'，多选模式
   */
  selectType?: 'checkbox'
  /** 判断行是否可选 */
  checkSelectable?: (row: T) => boolean

  /** 是否启用排序 */
  sortable?: boolean
  /** 默认排序 */
  defaultSort?: { prop: string; order: 'ascending' | 'descending' }

  /** 加载状态 */
  loading?: boolean
  /** 空数据提示 */
  emptyText?: string

  /** 表格高度 */
  height?: string | number
  /** 最大高度 */
  maxHeight?: string | number
  /** 斑马纹 */
  stripe?: boolean
  /** 边框 */
  border?: boolean
  /** 内容溢出 tooltip */
  showOverflowTooltip?: boolean

  /** 操作列配置 */
  operations?: TableOperation<T>[]
  /** 操作列宽度 */
  operationWidth?: number
  /** 操作列固定 */
  operationFixed?: 'left' | 'right' | boolean
}

/**
 * BlinkTable Emits 类型
 */
export interface BlinkTableEmits<T = any> {
  (e: 'update:selectedKeys', keys: (string | number)[]): void
  (e: 'select', selection: T[], row: T): void
  (e: 'select-all', selection: T[]): void
  (e: 'selection-change', selection: T[]): void
  (e: 'sort-change', sort: { prop: string; order: 'ascending' | 'descending' | '' }): void
  (e: 'row-click', row: T, column: any, event: Event): void
  (e: 'row-dblclick', row: T, column: any, event: Event): void
}

/**
 * BlinkTable Expose 方法
 */
export interface BlinkTableExpose {
  clearSelection: () => void
  toggleRowSelection: (row: any) => void
  toggleAllSelection: () => void
  setCurrentRow: (row: any) => void
  clearSort: () => void
  clearFilter: () => void
  doLayout: () => void
}
