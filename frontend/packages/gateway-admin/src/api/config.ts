import request from '@/utils/request'

// ============== 系统配置 API ==============

export interface ConfigItem {
  id: number
  configKey: string
  configName: string
  configValue: string
  configType: number // 0-字符串 1-数字 2-布尔 3-JSON 4-数组
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

/** 网关实例 VO */
export interface GatewayInstanceVO {
  instanceId: string
  serviceId: string
  host: string
  port: number
  uri: string
  status: number
  statusDesc: string
}

/**
 * 根据分组键名查询配置
 * @param groupKey 分组键名 (base, system, security, login, log)
 */
export const getConfigsByGroupKey = (groupKey: string): Promise<ConfigGroup> => {
  return request.post('/sysConfig/getConfigsByGroupKey', {
    body: { groupKey },
  }) as Promise<ConfigGroup>
}

/**
 * 批量更新配置值
 * @param configs 配置列表
 */
export const batchUpdateConfigs = (configs: UpdateConfigParams[]): Promise<void> => {
  return request.post('/sysConfig/batchUpdateConfigs', { body: { configs } }) as Promise<void>
}

/**
 * 更新单个配置
 * @param params 配置参数
 */
export const updateConfig = (params: UpdateConfigParams): Promise<void> => {
  return request.post('/sysConfig/modifySysConfig', { body: params }) as Promise<void>
}

// ============== 网关配置 API ==============
export interface GatewayConfig {
  instanceId?: string
  signatureSwitch?: number // 签名校验开关 0 开启 1关闭
  replaySwitch?: number // 防重放开关 0 开启 1关闭
  encryptionSwitch?: number // 加密开关 0 开启 1关闭
  requestTimeout?: number // 请求超时时间（毫秒）
  rateLimitSwitch?: number // 限流开关 0 开启 1关闭
  rateLimitThreshold?: number // 限流阈值
}

// 网关配置查询参数
export interface QueryGatewayConfigParams {
  instanceId?: string
}

// 网关配置更新参数
export interface UpdateGatewayConfigParams extends GatewayConfig {}

// IP列表查询参数 - 与后端 QueryIpListReq 一致
export interface QueryIpListParams {
  listType?: string // 列表类型：white-白名单 black-黑名单
}

// IP列表更新参数 - 与后端 UpdateIpListReq 一致
export interface UpdateIpListParams {
  listType: string // 列表类型：white-白名单 black-黑名单
  ipList: string[] // IP列表
}

// 推送配置参数 - 与后端 PushConfigReq 一致
export interface PushConfigParams {
  dataId?: string
  group?: string
  content?: string
}

// 获取网关配置
export const getGatewayConfig = (params: QueryGatewayConfigParams = {}): Promise<GatewayConfig> => {
  return request.post('/gatewayConfig/getGatewayConfig', { body: params })
}

// 更新网关配置
export const updateGatewayConfig = (params: UpdateGatewayConfigParams): Promise<void> => {
  return request.post('/gatewayConfig/updateGatewayConfig', { body: params })
}

// 获取IP列表
export const getIpList = (params: QueryIpListParams): Promise<string[]> => {
  return request.post('/gatewayConfig/getIpList', { body: params })
}

// 更新IP列表
export const updateIpList = (params: UpdateIpListParams): Promise<void> => {
  return request.post('/gatewayConfig/updateIpList', { body: params })
}

// Sync config to gateway instances
export const syncConfig = (params: PushConfigParams = {}): Promise<void> => {
  return request.post('/configPush/pushConfig', { body: params })
}

// 获取实例当前配置
export interface QueryInstanceConfigParams {
  instanceId: string
}

export const getInstanceConfig = (params: QueryInstanceConfigParams): Promise<GatewayConfig> => {
  return request.post('/gatewayConfig/getInstanceConfig', { body: params })
}

// 推送配置到指定实例
export interface PushConfigToInstanceParams {
  instanceIds?: string[]
  broadcast?: boolean
}

export const pushConfigToInstance = (params: PushConfigToInstanceParams): Promise<void> => {
  return request.post('/gatewayConfig/pushConfigToInstance', { body: params })
}

// Config API object (for component using configApi.xxx pattern)
export const configApi = {
  getGatewayConfig,
  updateGatewayConfig,
  getIpList,
  updateIpList,
  syncConfig,
  getInstanceConfig,
  pushConfigToInstance,
}
