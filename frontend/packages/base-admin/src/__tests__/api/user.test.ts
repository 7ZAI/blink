import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'

// Mock request
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('User API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getUserList', () => {
    it('should call correct endpoint with params', async () => {
      const mockResponse = {
        pageNum: 1,
        pageSize: 10,
        total: 100,
        pages: 10,
        rows: [],
      }
      vi.mocked(request.post).mockResolvedValue(mockResponse)

      const { getUserList } = await import('@/api/user')
      const params = { pageNum: 1, pageSize: 10, loginName: 'test' }
      const result = await getUserList(params)

      expect(request.post).toHaveBeenCalledWith('/sysUser/getSysUserList', { body: params })
      expect(result).toEqual(mockResponse)
    })
  })

  describe('addUser', () => {
    it('should call save endpoint with user data', async () => {
      vi.mocked(request.post).mockResolvedValue({ msgCode: 'BLINK0000', msgInfo: 'success' })

      const { addUser } = await import('@/api/user')
      const params = { loginName: 'newuser', sex: 1, phone: '13800138000' }
      await addUser(params)

      expect(request.post).toHaveBeenCalledWith('/sysUser/saveSysUser', { body: params })
    })
  })

  describe('lockUser', () => {
    it('should call lock endpoint with user id and status', async () => {
      vi.mocked(request.post).mockResolvedValue(undefined)

      const { lockUser } = await import('@/api/user')
      await lockUser({ userId: 1, locked: 1 })

      expect(request.post).toHaveBeenCalledWith('/sysUser/lockUser', {
        body: { userId: 1, locked: 1 },
      })
    })
  })
})
