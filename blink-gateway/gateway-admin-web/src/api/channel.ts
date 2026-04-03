import request from '@/utils/request'

// Channel Query Params
export interface QueryChannelParams {
  pageNum?: number
  pageSize?: number
  channelId?: string
  channelName?: string
  appKey?: string
  enable?: number
  encryptionSwitch?: number
  tokenType?: number
  authoritySwitch?: number
}

// Channel Info
export interface ChannelInfo {
  channelId: string
  channelName: string
  appKey: string
  appSecret: string
  relaUserId: string
  accessToken: string
  systemPublickey: string
  systemPrivatekey: string
  channelPublickey: string
  channelPrivatekey: string
  enable: number
  encryptionSwitch: number
  tokenType: number
  authoritySwitch: number
  remark: string
  createBy: string
  updateBy: string
  createTime: string
  updateTime: string
}

// Add Channel Params
export interface AddChannelParams {
  channelName: string
  relaUserId: string
  enable?: number
  encryptionSwitch: number
  tokenType: number
  authoritySwitch: number
  remark?: string
}

// Update Channel Params
export interface UpdateChannelParams {
  channelId: string
  channelName: string
  relaUserId: string
  enable: number
  encryptionSwitch: number
  tokenType: number
  authoritySwitch: number
  remark?: string
}

// Delete Channel Params
export interface DeleteChannelParams {
  channelId: string
}

// Refresh Key Params
export interface RefreshKeyParams {
  channelId: string
}

// Issue Token Params
export interface IssueTokenParams {
  channelId: string
  expireMinutes?: number
}

// Issue Token Result
export interface IssueTokenResult {
  token: string
  expireTime: string
  expiresIn: number
}

// Page Result
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  orderBy: string
  rows: T[]
}

// Get channel list
export const getChannelList = (params: QueryChannelParams): Promise<PageResult<ChannelInfo>> => {
  return request.post('/channel/getChannelList', { body: params })
}

// Get single channel
export const getChannel = (params: { channelId: string }): Promise<ChannelInfo> => {
  return request.post('/channel/getChannel', { body: params })
}

// Save channel
export const saveChannel = (params: AddChannelParams): Promise<void> => {
  return request.post('/channel/saveChannel', { body: params })
}

// Update channel
export const updateChannel = (params: UpdateChannelParams): Promise<void> => {
  return request.post('/channel/modifyChannel', { body: params })
}

// Delete channel
export const deleteChannel = (params: DeleteChannelParams): Promise<void> => {
  return request.post('/channel/deleteChannel', { body: params })
}

// Refresh channel key
export const refreshChannelKey = (params: RefreshKeyParams): Promise<ChannelInfo> => {
  return request.post('/channel/refreshChannelKey', { body: params })
}

// Refresh system key
export const refreshSystemKey = (params: RefreshKeyParams): Promise<ChannelInfo> => {
  return request.post('/channel/refreshSystemKey', { body: params })
}

// Issue channel token
export const issueChannelToken = (params: IssueTokenParams): Promise<IssueTokenResult> => {
  return request.post('/channel/issueChannelToken', { body: params })
}
