import request from '@/utils/request'
import { getDefaultRequestBody } from '@/config/request.config'

/**
 * Dashboard 统计数据响应
 */
export interface DashboardData {
  totalUsers: number
  onlineUsers: number
  totalRoles: number
  totalMenus: number
}

/**
 * 获取 Dashboard 统计数据
 */
export const getDashboardData = (): Promise<DashboardData> => {
  return request.post('/dashboard/getData', {
    ...getDefaultRequestBody(),
  }) as Promise<DashboardData>
}
