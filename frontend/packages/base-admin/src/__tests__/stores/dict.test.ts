import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDictStore } from '@/stores/dict'
import type { DictDataItem, DictDataMapRsp } from '@/api/dict'

// Mock dict API
vi.mock('@/api/dict', () => ({
  getDictDataByTypes: vi.fn(),
}))

import { getDictDataByTypes } from '@/api/dict'

describe('Dict Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have empty dictDataMap by default', () => {
      const store = useDictStore()
      expect(store.dictDataMap).toEqual({})
      expect(store.loading).toBe(false)
    })
  })

  describe('getDictData', () => {
    it('should return empty array for non-existent dict type', () => {
      const store = useDictStore()
      expect(store.getDictData('non_existent')).toEqual([])
    })

    it('should return dict data for existing dict type', () => {
      const store = useDictStore()
      const mockData: DictDataItem[] = [
        { dictValue: '1', dictLabel: '启用', listClass: 'success', isDefault: false },
        { dictValue: '0', dictLabel: '禁用', listClass: 'danger', isDefault: false },
      ]
      store.dictDataMap = { status: mockData }

      expect(store.getDictData('status')).toEqual(mockData)
    })
  })

  describe('loadDictData', () => {
    it('should call API and store dict data', async () => {
      const store = useDictStore()
      const mockResponse: DictDataMapRsp = {
        dictDataMap: {
          status: [
            { dictValue: '1', dictLabel: '启用', listClass: 'success', isDefault: false },
          ],
        },
      }
      vi.mocked(getDictDataByTypes).mockResolvedValue(mockResponse)

      await store.loadDictData(['status'])

      expect(getDictDataByTypes).toHaveBeenCalledWith({ dictTypes: ['status'] })
      expect(store.dictDataMap.status).toBeDefined()
    })

    it('should handle API error gracefully', async () => {
      const store = useDictStore()
      vi.mocked(getDictDataByTypes).mockRejectedValue(new Error('Network error'))

      await store.loadDictData(['status'])

      expect(store.dictDataMap.status).toBeUndefined()
      expect(store.loading).toBe(false)
    })

    it('should not reload already loaded dict types', async () => {
      const store = useDictStore()
      const mockResponse: DictDataMapRsp = {
        dictDataMap: {
          status: [
            { dictValue: '1', dictLabel: '启用', listClass: 'success', isDefault: false },
          ],
        },
      }
      vi.mocked(getDictDataByTypes).mockResolvedValue(mockResponse)

      // 第一次加载
      await store.loadDictData(['status'])
      vi.clearAllMocks()

      // 第二次加载相同类型 - 应该不会调用 API
      await store.loadDictData(['status'])

      expect(getDictDataByTypes).not.toHaveBeenCalled()
    })
  })

  describe('getLabelByValue', () => {
    it('should return string value when dict type not found', () => {
      const store = useDictStore()
      expect(store.getLabelByValue('non_existent', '1')).toBe('1')
    })

    it('should return label for matching value', () => {
      const store = useDictStore()
      store.dictDataMap = {
        status: [
          { dictValue: '1', dictLabel: '启用', listClass: '', isDefault: false },
        ],
      }

      expect(store.getLabelByValue('status', '1')).toBe('启用')
    })

    it('should return string value when value not found', () => {
      const store = useDictStore()
      store.dictDataMap = {
        status: [
          { dictValue: '1', dictLabel: '启用', listClass: '', isDefault: false },
        ],
      }

      expect(store.getLabelByValue('status', '999')).toBe('999')
    })
  })

  describe('getValueByLabel', () => {
    it('should return empty string when dict type not found', () => {
      const store = useDictStore()
      expect(store.getValueByLabel('non_existent', '启用')).toBe('')
    })

    it('should return value for matching label', () => {
      const store = useDictStore()
      store.dictDataMap = {
        status: [
          { dictValue: '1', dictLabel: '启用', listClass: '', isDefault: false },
        ],
      }

      expect(store.getValueByLabel('status', '启用')).toBe('1')
    })

    it('should return empty string when label not found', () => {
      const store = useDictStore()
      store.dictDataMap = {
        status: [
          { dictValue: '1', dictLabel: '启用', listClass: '', isDefault: false },
        ],
      }

      expect(store.getValueByLabel('status', '未知')).toBe('')
    })
  })

  describe('getListClass', () => {
    it('should return empty string when dict type not found', () => {
      const store = useDictStore()
      expect(store.getListClass('non_existent', '1')).toBe('')
    })

    it('should return listClass for matching value', () => {
      const store = useDictStore()
      store.dictDataMap = {
        status: [
          { dictValue: '1', dictLabel: '启用', listClass: 'success', isDefault: false },
        ],
      }

      expect(store.getListClass('status', '1')).toBe('success')
    })
  })

  describe('clearCache', () => {
    it('should clear all dict data', () => {
      const store = useDictStore()
      store.dictDataMap = {
        status: [{ dictValue: '1', dictLabel: '启用', listClass: '', isDefault: false }],
      }

      store.clearCache()

      expect(store.dictDataMap).toEqual({})
    })
  })

  describe('refreshDictData', () => {
    it('should reload dict data from API', async () => {
      const store = useDictStore()
      // 先设置已加载状态
      store.dictDataMap = {
        status: [{ dictValue: '0', dictLabel: '旧数据', listClass: '', isDefault: false }],
      }

      const mockResponse: DictDataMapRsp = {
        dictDataMap: {
          status: [{ dictValue: '1', dictLabel: '新数据', listClass: '', isDefault: false }],
        },
      }
      vi.mocked(getDictDataByTypes).mockResolvedValue(mockResponse)

      await store.refreshDictData(['status'])

      expect(getDictDataByTypes).toHaveBeenCalled()
    })
  })
})
