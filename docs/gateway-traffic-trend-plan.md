# 流量趋势监控实现计划

## 项目背景

当前流量趋势监控存在核心问题：
- 使用累计值 `totalRequests` 绘制趋势图，导致图表持续上升
- 无法反映真实的流量波动和 QPS 变化
- 缺乏历史数据持久化，无法查询历史趋势

## 实现方案

采用 **增量计算 + 分级聚合 + 历史持久化** 方案：

```
[gateway-reactive] 5s上报累计值
    ↓
[MetricsStreamConsumer] 计算增量 = 当前值 - 上次值
    ↓ Redis Sorted Set 存储
[TrafficAggregationService] 分钟级聚合定时任务
    ↓ 持久化 MySQL
[TrafficHistoryController] 查询历史趋势 API
    ↓
[前端 Dashboard] 初始化加载历史 + SSE实时追加
```

## 任务拆分

### Phase 1: 核心增量计算 (P0) ✅ 已完成

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 1.1 | 创建 Redis Key 常量 | ✅ 完成 | 定义流量趋势相关 Redis Key |
| 1.2 | 创建 TrafficIncrementService | ✅ 完成 | 增量计算核心逻辑 |
| 1.3 | 修改 MetricsStreamConsumer | ✅ 完成 | 集成增量计算，存储 Sorted Set |
| 1.4 | 修改 DashboardPushService | ✅ 完成 | 推送增量数据而非累计值 |
| 1.5 | 编写单元测试 | ✅ 完成 | TrafficIncrementService 测试 |

### Phase 2: 聚合和持久化 (P1) ✅ 已完成

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 2.1 | 创建 MySQL 表 | ✅ 完成 | gateway_traffic_history |
| 2.2 | 创建 Entity/Mapper | ✅ 完成 | GatewayTrafficHistoryDO, Mapper |
| 2.3 | 创建 TrafficAggregationService | ✅ 完成 | 分钟级聚合定时任务 |
| 2.4 | 创建 TrafficHistory API | ✅ 完成 | Controller, Service, DTO |
| 2.5 | 编写单元测试 | ✅ 完成 | 聚合服务测试 |

### Phase 3: 前端改进 (P2) ✅ 已完成

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 3.1 | 创建 TrafficHistory API | ✅ 完成 | 前端 API 接口 |
| 3.2 | 修改 dashboard store | ✅ 完成 | 支持时间范围、粒度、加载历史 |
| 3.3 | 修改 dashboard 页面 | ✅ 完成 | 时间范围选择器、粒度切换 |
| 3.4 | 国际化文案 | ✅ 完成 | zh-cn.ts 和 en-us.ts |
| 3.5 | 前端单元测试 | ✅ 完成 | vitest + dashboard store 测试（13个测试全部通过） |

## 进度记录

### 2026-04-14 Phase 1 完成

已完成核心增量计算机制：
- 创建 TrafficIncrementService 实现增量计算
- 修改 MetricsStreamConsumer 集成增量计算
- 存储增量数据到 Redis Sorted Set
- SSE 推送改为增量数据
- 单元测试编写完成

关键代码位置：
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/RedisKeyConstant.java`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/TrafficIncrementService.java`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/TrafficIncrementServiceImpl.java`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/MetricsStreamConsumer.java`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/DashboardPushServiceImpl.java`

### 2026-04-14 Phase 2 完成

已完成聚合持久化机制：
- 创建 MySQL 表 gateway_traffic_history
- 创建 Entity 和 Mapper
- 创建 TrafficAggregationService（分钟/小时聚合定时任务）
- 创建 TrafficHistory 查询 API
- 单元测试编写完成

关键代码位置：
- `blink-gateway/gateway-admin/src/main/resources/db/V20260414__init_gateway_traffic_history.sql`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/GatewayTrafficHistoryDO.java`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/mapper/GatewayTrafficHistoryMapper.java`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/TrafficAggregationService.java`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/TrafficAggregationServiceImpl.java`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/TrafficHistoryController.java`
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/TrafficHistoryService.java`

定时任务配置：
- 分钟聚合：`@Scheduled(cron = "0 * * * * ?")` - 每分钟执行
- 小时聚合：`@Scheduled(cron = "0 0 * * * ?")` - 每小时执行
- 数据清理：`@Scheduled(cron = "0 0 2 * * ?")` - 每天凌晨 2 点执行

---

*文档创建时间: 2026-04-14*
*最后更新: 2026-04-14 Phase 3 完成*

### 2026-04-14 Phase 3 完成

已完成前端流量趋势改进：
- 创建 TrafficHistory API 接口（monitor.ts）
- 扩展 dashboard store 支持历史查询（dashboard.ts）
- 添加时间范围选择器和粒度切换 UI（dashboard/index.vue）
- 添加中英文国际化文案

关键代码位置：
- `frontend/packages/gateway-admin/src/api/monitor.ts` - 新增 getTrafficHistory API
- `frontend/packages/gateway-admin/src/stores/dashboard.ts` - 新增 loadTrafficHistory、setGranularity、setTimeRange
- `frontend/packages/gateway-admin/src/views/dashboard/index.vue` - 新增时间范围选择器、粒度切换
- `frontend/packages/gateway-admin/src/locales/zh-cn.ts` - 新增流量趋势控制相关翻译
- `frontend/packages/gateway-admin/src/locales/en-us.ts` - 新增英文翻译
- `frontend/packages/gateway-admin/vitest.config.ts` - Vitest 测试配置
- `frontend/packages/gateway-admin/src/stores/__tests__/dashboard.test.ts` - Dashboard Store 单元测试（13个测试）

测试命令：
- `npm run test` - 运行测试（watch 模式）
- `npm run test:run` - 运行测试（单次）
- `npm run test:coverage` - 运行测试并生成覆盖率报告