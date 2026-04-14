/**
 * Vitest 测试环境设置
 *
 * 在所有测试运行前执行的设置文件
 */

import { config } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// 全局设置：每个测试前创建新的 Pinia 实例
beforeEach(() => {
  setActivePinia(createPinia())
})

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
}
Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// Mock sessionStorage
const sessionStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
}
Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock })