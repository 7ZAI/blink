import { computed, onMounted, ref, type Ref } from 'vue'
import { useDictStore, type DictDataItem } from '@/stores/dict'

export interface DictOption {
  label: string
  value: string | number
  listClass?: string
  isDefault?: boolean
}

export interface UseDictReturn {
  /** 字典选项列表，格式为 { label, value } */
  options: Ref<DictOption[]>
  /** 原始字典数据列表 */
  dictData: Ref<DictDataItem[]>
  /** 根据值获取标签 */
  getLabel: (value: string | number) => string
  /** 根据标签获取值 */
  getValue: (label: string) => string
  /** 根据值获取listClass */
  getListClass: (value: string | number) => string
  /** 加载状态 */
  loading: Ref<boolean>
}

/**
 * 字典组合式函数
 * 提供统一的字典数据访问接口
 *
 * @param dictType 字典类型编码
 * @param autoLoad 是否自动加载，默认true
 * @returns 字典数据和相关方法
 *
 * @example
 * ```ts
 * const { options, getLabel, getListClass } = useDict('sys_sex')
 *
 * // options.value = [{ label: '男', value: '1' }, { label: '女', value: '2' }, ...]
 * // getLabel(1) => '男'
 * // getListClass(1) => 'primary'
 * ```
 */
export function useDict(dictType: string, autoLoad: boolean = true): UseDictReturn {
  const dictStore = useDictStore()

  // 字典数据
  const dictData = computed(() => dictStore.getDictData(dictType))

  // 转换为选项格式
  const options = computed<DictOption[]>(() => {
    return dictData.value.map(item => ({
      label: item.dictLabel,
      value: item.dictValue,
      listClass: item.listClass,
      isDefault: item.isDefault
    }))
  })

  // 根据值获取标签
  const getLabel = (value: string | number): string => {
    return dictStore.getLabelByValue(dictType, value)
  }

  // 根据标签获取值
  const getValue = (label: string): string => {
    return dictStore.getValueByLabel(dictType, label)
  }

  // 根据值获取listClass
  const getListClass = (value: string | number): string => {
    return dictStore.getListClass(dictType, value)
  }

  // 加载状态
  const loading = computed(() => dictStore.loading)

  // 自动加载
  if (autoLoad) {
    onMounted(() => {
      dictStore.loadDictData([dictType])
    })
  }

  return {
    options,
    dictData,
    getLabel,
    getValue,
    getListClass,
    loading
  }
}

/**
 * 批量加载多个字典类型
 *
 * @param dictTypes 字典类型编码列表
 *
 * @example
 * ```ts
 * // 在应用初始化或路由守卫中预加载
 * await loadDicts(['sys_sex', 'sys_normal_status', 'sys_menu_type'])
 * ```
 */
export async function loadDicts(dictTypes: string[]): Promise<void> {
  const dictStore = useDictStore()
  await dictStore.loadDictData(dictTypes)
}