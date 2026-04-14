# Gateway Admin Frontend

网关管理后台前端应用，基于 Vue 3 + TypeScript + Element Plus 构建。

## 技术栈

- **框架**: Vue 3.4 + TypeScript 5
- **构建**: Vite 5
- **UI**: Element Plus 2.5
- **状态管理**: Pinia 2.1
- **路由**: Vue Router 4
- **HTTP**: Axios
- **图表**: ECharts 5 + vue-echarts
- **国际化**: Vue I18n

## 目录结构

```
src/
├── api/              # HTTP API 接口
├── assets/           # 静态资源
├── components/       # 公共组件
├── composables/      # 组合式函数（Hooks）
├── layouts/          # 布局组件
├── router/           # 路由配置
├── stores/           # Pinia 状态管理
├── styles/           # 全局样式
├── utils/            # 工具函数
└── views/            # 页面组件
```

## 开发指南

### 启动开发服务器

```bash
npm install
npm run dev
```

### 构建生产版本

```bash
npm run build
```

---

## SSE 实时推送架构

### 架概概述

Gateway Admin 使用 **SSE (Server-Sent Events)** 实现服务器到客户端的实时数据推送，采用**消息总线模式**，确保 SSE 连接的统一管理。

```
┌─────────────────────────────────────────────────────────────────┐
│                         MainLayout                               │
│  onMounted: notificationStore.connectSse()                       │
│  onUnmounted: notificationStore.disconnectSse()                  │
│                    SSE 连接唯一管理者                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼ SSE 连接
┌─────────────────────────────────────────────────────────────────┐
│                     Notification Store                           │
│  connectSse() - 防重复连接（connected/connecting 时跳过）         │
│  disconnectSse() - 断开连接                                      │
│  reconnectSse() - 强制重连（特殊情况）                            │
│                         消息分发中心                              │
│  handleSseMessage()                                              │
│    ├─ heartbeat → 忽略                                           │
│    ├─ notification → 本 store 管理                               │
│    ├─ instance_status → 回调列表分发                              │
│    └─ dashboard_data → dashboardStore.handleDashboardData()     │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Dashboard Store │  │useInstanceStatus│  │NotificationCenter│
│                 │  │   (回调订阅)     │  │    (监听数据)     │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Dashboard 页面   │  │ Monitor 页面    │  │ 通知下拉组件     │
│ 只监听 store    │  │ 只监听 composable│  │ 只监听 store    │
│ 不操作 SSE      │  │ 不操作 SSE       │  │ 不操作 SSE      │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### 核心原则

1. **SSE 连接由顶层统一管理** - 只有 `MainLayout` 调用 `connectSse()` 和 `disconnectSse()`
2. **其他页面只监听数据，不操作 SSE** - Dashboard、Monitor 等页面只订阅 Store 数据
3. **消息通过回调列表分发** - 实现订阅/发布模式，避免组件直接操作 SSE

### 消息类型

| 类型 | 说明 | 分发方式 |
|------|------|---------|
| `heartbeat` | 心跳消息 | 忽略，不处理 |
| `notification` | 用户通知消息 | 存入 notification store |
| `instance_status` | 实例状态变化 | 回调列表分发到 useInstanceStatus |
| `dashboard_data` | 仪表盘数据（统计信息、流量历史） | 调用 dashboardStore.handleDashboardData() |

**注意**：仪表盘的实例列表通过 API (`/gatewayInstance/getGatewayInstances`) 查询获取，不通过 SSE 推送。这样确保实例数据来源一致、准确可靠。

### 文件职责

| 文件 | 职责 |
|------|------|
| `stores/notification.ts` | SSE 连接管理、消息分发中心 |
| `stores/dashboard.ts` | 仪表盘数据存储，接收 SSE 推送 |
| `composables/useInstanceStatus.ts` | 实例状态订阅，通过回调列表接收数据 |
| `layouts/MainLayout.vue` | SSE 连接生命周期管理（唯一管理者） |
| `composables/useSseConnection.ts` | SSE 底层连接封装（fetch-event-source） |

### 使用示例

#### 1. Dashboard 页面 - 监听 Store 数据

```vue
<script setup lang="ts">
import { useDashboardStore } from '@/stores/dashboard'
import { useNotificationStore } from '@/stores/notification'

const dashboardStore = useDashboardStore()
const notificationStore = useNotificationStore()

// 从 store 获取数据（响应式）
// 统计信息、流量历史：由 SSE 推送更新
const statistics = computed(() => dashboardStore.statistics)
const trafficHistory = computed(() => dashboardStore.trafficHistory)

// 实例列表：由 API 查询获取（更准确）
const instances = computed(() => dashboardStore.instances)
const instancesLoading = computed(() => dashboardStore.instancesLoading)

onMounted(() => {
  // SSE 连接由 MainLayout 统一管理，无需手动连接
  // 组件挂载时获取实例列表（通过 API）
  dashboardStore.fetchInstances()
})
</script>
```

#### 2. Monitor 页面 - 使用 Composable

```vue
<script setup lang="ts">
import { useInstanceStatus } from '@/composables/useInstanceStatus'

// 获取实例状态（自动订阅 SSE 推送）
const { instances, stats, isConnected } = useInstanceStatus({
  onStatusChange: (data) => {
    console.log('状态变化:', data)
  }
})

// SSE 连接由 MainLayout 管理，无需调用 connect()
</script>
```

#### 3. 通知组件 - 监听 Store

```vue
<script setup lang="ts">
import { useNotificationStore } from '@/stores/notification'

const notificationStore = useNotificationStore()

// 获取通知数据（自动更新）
const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)

// SSE 连接由 MainLayout 管理，无需操作
</script>
```

### 开发规范

#### 禁止操作

```typescript
// ❌ 错误 - 页面组件不应直接操作 SSE
const notificationStore = useNotificationStore()
notificationStore.connectSse()  // 禁止在页面中调用

// ❌ 错误 - 在 composable 中调用 connectSse
if (notificationStore.sseStatus === 'disconnected') {
  notificationStore.connectSse()  // 禁止
}
```

#### 正确做法

```typescript
// ✅ 正确 - 只监听 store 数据变化
const dashboardStore = useDashboardStore()
const statistics = computed(() => dashboardStore.statistics)

// ✅ 正确 - 通过回调列表订阅消息
const { instances } = useInstanceStatus({
  onStatusChange: (data) => { /* 处理数据 */ }
})

// ✅ 正确 - 检查 SSE 状态用于显示提示
if (notificationStore.sseStatus !== 'connected') {
  ElMessage.warning('实时推送未连接')
}
```

### 添加新的 SSE 消息类型

1. **后端** - 在 `SseMessageType.java` 添加新类型常量
2. **后端** - 创建对应的 Payload 类
3. **后端** - 在 `SseMessage.java` 添加工厂方法
4. **前端** - 在 `notification.ts` 的 `SSE_MESSAGE_TYPE` 添加常量
5. **前端** - 在 `handleSseMessage()` 添加分发逻辑
6. **前端** - 创建对应的 Store 或使用回调列表分发

### SSE 连接参数

```typescript
// application.ts 中的配置
const sseUrl = `${baseUrl}/gateway-admin/notification/sse/connect`

// 连接参数（useSseConnection）
maxRetries: 10       // 最大重试次数
retryDelay: 1000     // 重试延迟（毫秒）
```

### 断线重连机制

- 自动重连：`useSseConnection` 实现指数退避重连（1s → 2s → 4s ... 最大 30s）
- 最大重试：10 次后停止重连，显示提示
- 手动重连：特殊情况可调用 `notificationStore.reconnectSse()`

---

## 状态管理 (Pinia Stores)

### Store 列表

| Store | 文件 | 职责 |
|-------|------|------|
| `app` | `stores/app.ts` | 应用全局状态（语言、侧边栏） |
| `user` | `stores/user.ts` | 用户信息、登录状态、权限 |
| `theme` | `stores/theme.ts` | 主题配置、颜色、字体 |
| `tabs` | `stores/tabs.ts` | 标签页管理、缓存视图 |
| `notification` | `stores/notification.ts` | SSE 连接、通知消息 |
| `dashboard` | `stores/dashboard.ts` | 仪表盘数据（统计信息、流量历史由 SSE 推送，实例列表由 API 查询） |
| `systemConfig` | `stores/systemConfig.ts` | 系统配置 |

### Store 使用规范

```typescript
// ✅ 正确 - 在 setup 组件中使用
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

// ❌ 错误 - 不要在组件外直接使用
const store = useUserStore()  // 组件外调用会报错
```

---

## 组合式函数 (Composables)

### useInstanceStatus

实例状态实时更新订阅。

```typescript
const { instances, stats, isConnected } = useInstanceStatus()

// instances: InstanceSummary[] - 实例列表
// stats: InstanceStats - 统计信息
// isConnected: boolean - SSE 连接状态
```

### useSseConnection

底层 SSE 连接封装，基于 `@microsoft/fetch-event-source`。

```typescript
const connection = useSseConnection({
  url: '/sse/connect',
  token: 'xxx',
  onMessage: (data) => { },
  onConnect: () => { },
  onDisconnect: () => { },
  maxRetries: 10,
  retryDelay: 1000,
})
connection.connect()
connection.disconnect()
```

---

## 相关文档

- [前端开发规范](../../RULES.md)
- [后端 SSE 架构](../../../docs/knowledge/gateway-sse-architecture.md)