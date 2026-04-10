import request from '@/utils/request'
import type { PageResult } from '@/types'

// ==================== 数据类型定义 ====================

/**
 * 数据过滤规则信息
 */
export interface DataFilterInfo {
  dataFilterId: number
  dataFilterName: string
  dataFilterEnName: string
  entityClass: string
  tableName: string
  ruleType: string
  ruleConfig: string
  status: number
  remark: string
  createBy: string
  createTime: string
}

/**
 * 关联关系信息
 */
export interface RelationInfoVO {
  name: string
  enName: string
  relationTable: string
  sourceField: string
  relationSourceField: string
  relationTargetField: string
  targetTable: string
  targetField: string
  targetName: string
  supportMatchTypes: string[]
  matchTypeLabels: Record<string, string>
}

/**
 * 已注册实体信息
 */
export interface EntityInfo {
  entityClass: string
  entityName: string
  entityEnName: string
  tableName: string
  relations?: RelationInfoVO[]
}

/**
 * 已注册实体列表响应
 */
export interface EntityListRsp {
  entities: EntityInfo[]
}

/**
 * 实体字段信息
 */
export interface EntityFieldVO {
  fieldName: string
  columnName: string
  fieldType: string
}

/**
 * 获取实体字段响应
 */
export interface EntityFieldsRsp {
  fields: EntityFieldVO[]
}

/**
 * 匹配类型选项
 */
export interface MatchTypeOption {
  value: string
  label: string
  dynamic: boolean
}

/**
 * 匹配类型响应
 */
export interface MatchTypesRsp {
  options: MatchTypeOption[]
}

/**
 * 查询参数
 */
export interface QueryDataFilterParams {
  pageNum?: number
  pageSize?: number
  dataFilterName?: string
  entityClass?: string
  ruleType?: string
  status?: number
}

/**
 * 新增参数
 */
export interface AddDataFilterParams {
  dataFilterName: string
  dataFilterEnName?: string
  entityClass: string
  tableName: string
  ruleType: string
  ruleConfig: string
  remark?: string
}

/**
 * 更新参数
 */
export interface UpdateDataFilterParams {
  dataFilterId: number
  dataFilterName: string
  dataFilterEnName?: string
  ruleConfig: string
  status?: number
  remark?: string
}

// ==================== API函数定义 ====================

/**
 * 分页查询数据过滤规则列表
 */
export const getDataFilterList = (
  params: QueryDataFilterParams
): Promise<PageResult<DataFilterInfo>> => {
  return request.post('/sysDataFilter/queryDataFilterList', { body: params }) as Promise<
    PageResult<DataFilterInfo>
  >
}

/**
 * 新增数据过滤规则
 */
export const addDataFilter = (params: AddDataFilterParams): Promise<void> => {
  return request.post('/sysDataFilter/addDataFilter', { body: params }) as Promise<void>
}

/**
 * 更新数据过滤规则
 */
export const updateDataFilter = (params: UpdateDataFilterParams): Promise<void> => {
  return request.post('/sysDataFilter/updateDataFilter', { body: params }) as Promise<void>
}

/**
 * 删除数据过滤规则
 */
export const deleteDataFilter = (dataFilterId: number): Promise<void> => {
  return request.post('/sysDataFilter/deleteDataFilter', {
    body: { dataFilterId },
  }) as Promise<void>
}

/**
 * 获取规则详情
 */
export const getDataFilterDetail = (dataFilterId: number): Promise<DataFilterInfo> => {
  return request.post('/sysDataFilter/getDataFilterDetail', {
    body: { dataFilterId },
  }) as Promise<DataFilterInfo>
}

/**
 * 获取已注册实体列表
 */
export const getEntityList = (): Promise<EntityInfo[]> => {
  return request.post('/sysDataFilter/getEntityList', {}).then((res) => {
    return (res as unknown as EntityListRsp).entities || []
  })
}

/**
 * 获取实体字段列表
 */
export const getEntityFields = (entityClass: string): Promise<EntityFieldsRsp> => {
  return request.post('/sysDataFilter/getEntityFields', {
    body: { entityClass },
  }) as Promise<EntityFieldsRsp>
}

/**
 * 刷新缓存
 */
export const refreshCache = (): Promise<void> => {
  return request.post('/sysDataFilter/refreshCache', {}) as Promise<void>
}

/**
 * 获取匹配类型选项
 * 根据过滤对象和关联关系返回可用的匹配类型
 */
export const getMatchTypes = (tableName: string, relationName: string): Promise<MatchTypesRsp> => {
  return request.post('/sysDataFilter/getMatchTypes', {
    body: { tableName, relationName },
  }) as Promise<MatchTypesRsp>
}
