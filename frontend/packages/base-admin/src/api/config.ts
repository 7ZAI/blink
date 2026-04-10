import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface ConfigItem {
  id: number
  configKey: string
  configName: string
  configValue: string
  configType: number
  groupId: number
  description: string
  readonly: number
  status: number
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
  remark: string
}

export interface ConfigGroup {
  groupId: number
  groupKey: string
  groupName: string
  configs: ConfigItem[]
}

export interface UpdateConfigParams {
  id: number
  configKey: string
  configValue: string
  configName?: string
  configType?: number
  description?: string
}

export const getConfigsByGroupKey = (groupKey: string): Promise<ConfigGroup> => {
  return request.post('/sysConfig/getConfigsByGroupKey', {
    body: { groupKey },
  }) as Promise<ConfigGroup>
}

export const batchUpdateConfigs = (configs: UpdateConfigParams[]): Promise<void> => {
  return request.post('/sysConfig/batchUpdateConfigs', { body: { configs } }) as Promise<void>
}

export const updateConfig = (params: UpdateConfigParams): Promise<void> => {
  return request.post('/sysConfig/modifySysConfig', { body: params }) as Promise<void>
}
