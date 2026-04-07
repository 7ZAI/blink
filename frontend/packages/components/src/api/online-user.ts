import request from '@/utils/request'
import { getDefaultRequestBody } from '@/config/request.config'

export interface OnlineUser {
  userId: number
  loginName: string
  username: string
  token: string
  loginTime: string
}

export interface OnlineUserRsp {
  rows: OnlineUser[]
  total: number
}

export const getOnlineUserList = (): Promise<OnlineUserRsp> => {
  return request.post('/onlineUser/list', {
    ...getDefaultRequestBody(),
  }) as Promise<OnlineUserRsp>
}

export const kickoutUser = (token: string): Promise<void> => {
  return request.post('/onlineUser/kickout', {
    ...getDefaultRequestBody(),
    body: { token },
  })
}
