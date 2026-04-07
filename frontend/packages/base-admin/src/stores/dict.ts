import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDictDataByTypes, type DictDataItem } from '@/api/dict'

// 重新导出 DictDataItem 类型，供其他模块使用
export type { DictDataItem } from '@/api/dict'

/**
 * 字典数据Store
 * 管理系统中所有字典数据的缓存和获取
 */
export const useDictStore = defineStore('dict', () => {
  // 字典数据缓存
  const dictDataMap = ref<Record<string, DictDataItem[]>>({})

  // 已加载的字典类型
  const loadedTypes = ref<Set<string>>(new Set())

  // 加载状态
  const loading = ref(false)

  /**
   * 加载指定类型的字典数据
   * @param dictTypes 字典类型列表
   */
  const loadDictData = async (dictTypes: string[]) => {
    // 过滤出未加载的类型
    const typesToLoad = dictTypes.filter(type => !loadedTypes.value.has(type))

    if (typesToLoad.length === 0) {
      return
    }

    loading.value = true
    try {
      const response = await getDictDataByTypes({ dictTypes: typesToLoad })

      // 合并到缓存
      if (response?.dictDataMap) {
        Object.assign(dictDataMap.value, response.dictDataMap)

        // 标记为已加载
        typesToLoad.forEach(type => loadedTypes.value.add(type))
      }
    } catch (error) {
      console.error('[DictStore] 加载字典数据失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取指定类型的字典数据
   * @param dictType 字典类型
   * @returns 字典数据列表
   */
  const getDictData = (dictType: string): DictDataItem[] => {
    return dictDataMap.value[dictType] || []
  }

  /**
   * 根据字典值获取标签
   * @param dictType 字典类型
   * @param value 字典值
   * @returns 字典标签
   */
  const getLabelByValue = (dictType: string, value: string | number): string => {
    const items = dictDataMap.value[dictType]
    if (!items) return String(value)

    const item = items.find(item => item.dictValue === String(value))
    return item?.dictLabel || String(value)
  }

  /**
   * 根据字典标签获取值
   * @param dictType 字典类型
   * @param label 字典标签
   * @returns 字典值
   */
  const getValueByLabel = (dictType: string, label: string): string => {
    const items = dictDataMap.value[dictType]
    if (!items) return ''

    const item = items.find(item => item.dictLabel === label)
    return item?.dictValue || ''
  }

  /**
   * 获取字典数据的listClass
   * @param dictType 字典类型
   * @param value 字典值
   * @returns listClass
   */
  const getListClass = (dictType: string, value: string | number): string => {
    const items = dictDataMap.value[dictType]
    if (!items) return ''

    const item = items.find(item => item.dictValue === String(value))
    return item?.listClass || ''
  }

  /**
   * 清除字典缓存
   */
  const clearCache = () => {
    dictDataMap.value = {}
    loadedTypes.value.clear()
  }

  /**
   * 刷新指定类型的字典数据
   * @param dictTypes 字典类型列表
   */
  const refreshDictData = async (dictTypes: string[]) => {
    // 从已加载集合中移除
    dictTypes.forEach(type => loadedTypes.value.delete(type))

    // 重新加载
    await loadDictData(dictTypes)
  }

  return {
    dictDataMap,
    loading,
    loadDictData,
    getDictData,
    getLabelByValue,
    getValueByLabel,
    getListClass,
    clearCache,
    refreshDictData
  }
})