# Task 5: 前端 API 模块和类型定义

**依赖:** Task 3 (后端 Controller 接口)

**目标:** 创建前端实例管理 API 模块和 TypeScript 类型定义

---

## 文件清单

- 新增: `frontend/packages/gateway-admin/src/api/instance.ts`

---

### Task 5.1: 创建 instance.ts API 模块

- [ ] **Step 1: 创建完整的 API 文件**

文件: `frontend/packages/gateway-admin/src/api/instance.ts`

**注意:** `monitor.ts` 中已有部分类型定义（`InstanceInfo`、`OfflineInstanceParams`、`OnlineInstanceParams`）。由于字段不完全相同，`instance.ts` 作为实例管理的统一来源，定义完整类型。

需要在 `monitor.ts` 中修改导入，复用 `instance.ts` 的类型（在 Task 6 实现页面时处理）。

```typescript
import request from '@/utils/request'

// ==================== 类型定义 ====================

/**
 * 实例状态
 */
export type InstanceStatus = 'online' | 'offline' | 'shutdown'

/**
 * 实例状态码
 */
export const INSTANCE_STATUS = {
  ONLINE: 0,
  OFFLINE: 1,
  SHUTDOWN: 2,
} as const

/**
 * 实例基本信息
 */
export interface InstanceInfo {
  id: number
  instanceId: string
  serviceId: string
  host: string
  port: number
  uri: string
  metadata?: string
  status: number
  statusDesc: string
  onlineTime?: string
  offlineTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * JVM 监控指标
 */
export interface JvmMetrics {
  heapUsed?: number
  heapMax?: number
  heapUsagePercent?: number
  nonHeapUsed?: number
  youngGcCount?: number
  youngGcTime?: number
  oldGcCount?: number
  oldGcTime?: number
  liveThreads?: number
  peakThreads?: number
  daemonThreads?: number
  timestamp?: number
}

/**
 * 组件健康状态
 */
export interface ComponentHealth {
  name: string
  status: string
  details?: Record<string, unknown>
}

/**
 * 健康状态详情
 */
export interface HealthDetail {
  status: string
  components?: ComponentHealth[]
}

/**
 * HTTP 请求统计
 */
export interface HttpMetrics {
  totalRequests?: number
  successRequests?: number
  failedRequests?: number
  successRate?: number
  avgResponseTime?: number
  timestamp?: number
}

/**
 * 实例详情响应
 */
export interface InstanceDetail {
  instanceInfo: InstanceInfo
  healthDetail?: HealthDetail
  jvmMetrics?: JvmMetrics
  httpMetrics?: HttpMetrics
}

/**
 * 分页查询实例请求参数
 */
export interface QueryInstanceParams {
  pageNum?: number
  pageSize?: number
  serviceId?: string
  host?: string
  status?: number
}

/**
 * 分页查询实例列表响应
 */
export interface QueryInstanceListResult {
  rows: InstanceInfo[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/**
 * 保存实例请求参数
 */
export interface SaveInstanceParams {
  id?: number
  serviceId: string
  host: string
  port: number
  metadata?: string
}

/**
 * 删除实例请求参数
 */
export interface DeleteInstanceParams {
  id: number
}

/**
 * 获取实例详情请求参数
 */
export interface GetInstanceDetailParams {
  id: number
}

/**
 * 上线实例请求参数
 */
export interface OnlineInstanceParams {
  instanceId: string
}

/**
 * 下线实例请求参数
 */
export interface OfflineInstanceParams {
  instanceId: string
  reason?: string
}

// ==================== API 函数 ====================

/**
 * 分页查询实例列表
 */
export const queryInstanceList = (params: QueryInstanceParams = {}): Promise<QueryInstanceListResult> => {
  return request.post('/gatewayInstance/queryInstanceList', { body: params })
}

/**
 * 保存实例（新增/编辑）
 */
export const saveInstance = (params: SaveInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/saveInstance', { body: params })
}

/**
 * 删除实例
 */
export const deleteInstance = (params: DeleteInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/deleteInstance', { body: params })
}

/**
 * 获取实例详情（含监控指标）
 */
export const getInstanceDetailWithMetrics = (params: GetInstanceDetailParams): Promise<InstanceDetail> => {
  return request.post('/gatewayInstance/getInstanceDetailWithMetrics', { body: params })
}

/**
 * 上线实例
 */
export const onlineInstance = (params: OnlineInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/onlineInstance', { body: params })
}

/**
 * 下线实例
 */
export const offlineInstance = (params: OfflineInstanceParams): Promise<void> => {
  return request.post('/gatewayInstance/offlineInstance', { body: params })
}

// ==================== API 对象导出 ====================

export const instanceApi = {
  queryInstanceList,
  saveInstance,
  deleteInstance,
  getInstanceDetailWithMetrics,
  onlineInstance,
  offlineInstance,
}
```

---

### Task 5.2: 提交更改

- [ ] **Step 2: Git 提交**

```bash
git add frontend/packages/gateway-admin/src/api/instance.ts
git commit -m "feat(instance): 新增前端实例管理 API 模块

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验收检查

| 检查项 | 状态 |
|--------|------|
| instance.ts 文件已创建 | [ ] |
| 所有 TypeScript 类型已定义 | [ ] |
| 所有 API 函数已实现 | [ ] |
| API 函数使用正确的请求格式 `{ body: params }` | [ ] |
| 与 monitor.ts 类型冲突已处理 | [ ] |
| Git 提交成功 | [ ] |