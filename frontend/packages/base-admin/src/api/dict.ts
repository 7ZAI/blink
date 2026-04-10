import request from '@/utils/request'
import type { ApiResponse } from '@/types'

/**
 * 字典类型信息
 */
export interface DictTypeInfo {
  dictId: number
  dictName: string
  dictType: string
  status: number
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
  remark: string
  locale: string
}

/**
 * 字典数据信息
 */
export interface DictDataInfo {
  dictCode: number
  dictSort: number
  dictLabel: string
  dictValue: string
  dictType: string
  cssClass: string
  listClass: string
  isDefault: number
  status: number
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
  remark: string
  locale: string
}

/**
 * 查询字典类型参数
 */
export interface QueryDictTypeParams {
  pageNum?: number
  pageSize?: number
  dictName?: string
  dictType?: string
  status?: number
  locale?: string
}

/**
 * 查询字典数据参数
 */
export interface QueryDictDataParams {
  pageNum?: number
  pageSize?: number
  dictLabel?: string
  dictType?: string
  status?: number
  locale?: string
}

/**
 * 新增字典类型参数
 */
export interface AddDictTypeParams {
  dictName: string
  dictType: string
  status: number
  remark?: string
  locale?: string
}

/**
 * 修改字典类型参数
 */
export interface UpdateDictTypeParams {
  dictId: number
  dictName?: string
  dictType?: string
  status?: number
  remark?: string
  locale?: string
}

/**
 * 删除字典类型参数
 */
export interface DeleteDictTypeParams {
  deleteId?: number
  idList?: number[]
  batchDelete: boolean
}

/**
 * 新增字典数据参数
 */
export interface AddDictDataParams {
  dictSort: number
  dictLabel: string
  dictValue: string
  dictType: string
  cssClass?: string
  listClass?: string
  isDefault: number
  status: number
  remark?: string
  locale?: string
}

/**
 * 修改字典数据参数
 */
export interface UpdateDictDataParams {
  dictCode: number
  dictSort?: number
  dictLabel?: string
  dictValue?: string
  dictType?: string
  cssClass?: string
  listClass?: string
  isDefault?: number
  status?: number
  remark?: string
  locale?: string
}

/**
 * 删除字典数据参数
 */
export interface DeleteDictDataParams {
  deleteId?: number
  idList?: number[]
  batchDelete: boolean
}

/**
 * 查询字典类型列表响应
 */
export interface QueryDictTypeRsp {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  rows: DictTypeInfo[]
}

/**
 * 查询字典数据列表响应
 */
export interface QueryDictDataRsp {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  rows: DictDataInfo[]
}

/**
 * 获取字典类型列表
 * @param params 查询参数
 * @returns 字典类型列表
 */
export const getDictTypeList = (params?: QueryDictTypeParams): Promise<QueryDictTypeRsp> => {
  return request.post('/sysDictType/getSysDictTypeList', {
    body: params || {},
  }) as Promise<QueryDictTypeRsp>
}

/**
 * 新增字典类型
 * @param params 新增参数
 * @returns 新增的字典类型
 */
export const addDictType = (params: AddDictTypeParams): Promise<DictTypeInfo> => {
  return request.post('/sysDictType/saveSysDictType', { body: params }) as Promise<DictTypeInfo>
}

/**
 * 修改字典类型
 * @param params 修改参数
 * @returns 修改后的字典类型
 */
export const updateDictType = (params: UpdateDictTypeParams): Promise<DictTypeInfo> => {
  return request.post('/sysDictType/modifySysDictType', { body: params }) as Promise<DictTypeInfo>
}

/**
 * 删除字典类型
 * @param params 删除参数
 */
export const deleteDictType = (params: DeleteDictTypeParams): Promise<void> => {
  return request.post('/sysDictType/deleteSysDictType', { body: params }) as Promise<void>
}

/**
 * 获取字典数据列表
 * @param params 查询参数
 * @returns 字典数据列表
 */
export const getDictDataList = (params?: QueryDictDataParams): Promise<QueryDictDataRsp> => {
  return request.post('/sysDictData/getSysDictDataList', {
    body: params || {},
  }) as Promise<QueryDictDataRsp>
}

/**
 * 根据字典类型获取字典数据
 * @param dictType 字典类型
 * @returns 字典数据列表
 */
export const getDictDataByType = (dictType: string): Promise<DictDataInfo[]> => {
  return request.post('/sysDictData/getDictDataByType', { body: { dictType } }) as Promise<
    DictDataInfo[]
  >
}

/**
 * 新增字典数据
 * @param params 新增参数
 * @returns 新增的字典数据
 */
export const addDictData = (params: AddDictDataParams): Promise<DictDataInfo> => {
  return request.post('/sysDictData/saveSysDictData', { body: params }) as Promise<DictDataInfo>
}

/**
 * 修改字典数据
 * @param params 修改参数
 * @returns 修改后的字典数据
 */
export const updateDictData = (params: UpdateDictDataParams): Promise<DictDataInfo> => {
  return request.post('/sysDictData/modifySysDictData', { body: params }) as Promise<DictDataInfo>
}

/**
 * 删除字典数据
 * @param params 删除参数
 */
export const deleteDictData = (params: DeleteDictDataParams): Promise<void> => {
  return request.post('/sysDictData/deleteSysDictData', { body: params }) as Promise<void>
}

/**
 * 批量获取字典数据参数
 */
export interface GetDictDataByTypesParams {
  dictTypes: string[]
}

/**
 * 字典数据项
 */
export interface DictDataItem {
  dictValue: string
  dictLabel: string
  listClass: string
  isDefault: boolean
}

/**
 * 批量获取字典数据响应
 */
export interface DictDataMapRsp {
  dictDataMap: Record<string, DictDataItem[]>
}

/**
 * 批量根据字典类型编码获取字典数据
 * @param params 包含dictTypes列表的参数
 * @returns 字典数据Map
 */
export const getDictDataByTypes = (params: GetDictDataByTypesParams): Promise<DictDataMapRsp> => {
  return request.post('/sysDictData/getDictDataByTypes', {
    body: params,
  }) as Promise<DictDataMapRsp>
}
