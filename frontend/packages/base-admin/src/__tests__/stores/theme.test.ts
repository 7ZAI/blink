import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '@/stores/theme'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key]
    }),
    clear: vi.fn(() => {
      store = {}
    }),
  }
})()

Object.defineProperty(window, 'localStorage', { value: localStorageMock })

describe('Theme Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorageMock.clear()
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have light theme by default', () => {
      const store = useThemeStore()
      expect(store.theme).toBe('light')
    })
  })

  describe('setTheme', () => {
    it('should set theme and update localStorage', () => {
      const store = useThemeStore()

      store.setTheme('dark')

      expect(store.theme).toBe('dark')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('theme', 'dark')
    })

    it('should add dark class to document when dark theme is set', () => {
      const store = useThemeStore()

      store.setTheme('dark')

      expect(document.documentElement.classList.contains('dark')).toBe(true)
    })

    it('should remove dark class when light theme is set', () => {
      const store = useThemeStore()

      document.documentElement.classList.add('dark')
      store.setTheme('light')

      expect(document.documentElement.classList.contains('dark')).toBe(false)
    })
  })

  describe('toggleTheme', () => {
    it('should toggle from light to dark', () => {
      const store = useThemeStore()
      store.setTheme('light')

      store.toggleTheme()

      expect(store.theme).toBe('dark')
    })

    it('should toggle from dark to light', () => {
      const store = useThemeStore()
      store.setTheme('dark')

      store.toggleTheme()

      expect(store.theme).toBe('light')
    })
  })

  describe('initTheme', () => {
    it('should restore theme from localStorage', () => {
      localStorageMock.getItem.mockReturnValue('dark')

      const store = useThemeStore()
      store.initTheme()

      expect(store.theme).toBe('dark')
    })
  })
})
