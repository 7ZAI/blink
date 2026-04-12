# blink-redis-starter 模块单元测试计划

> 创建时间：2026-04-13
> 目标：为 blink-redis-starter 模块补充完善的单元测试覆盖

---

## 任务总览

| 任务编号 | 任务名称 | 目标类 | 优先级 | 预计工作量 | 状态 |
|----------|----------|--------|--------|------------|------|
| Task-01 | RedisClient String操作测试 | RedisClient | 🔴 高 | 2h | ✅ 已完成 |
| Task-02 | RedisClient Hash操作测试 | RedisClient | 🔴 高 | 1.5h | ✅ 已完成 |
| Task-03 | RedisClient List/Set/ZSet操作测试 | RedisClient | 🔴 高 | 2h | ✅ 已完成 |
| Task-04 | RedisClient Stream操作测试 | RedisClient | 🔴 高 | 2h | ✅ 已完成 |
| Task-05 | RedisClient 批量操作与Lua脚本测试 | RedisClient | 🔴 高 | 1.5h | ✅ 已完成 |
| Task-06 | ReactiveRedisClient 基础操作测试 | ReactiveRedisClient | 🔴 高 | 2h | ✅ 已完成 |
| Task-07 | ReactiveRedisClient 高级操作测试 | ReactiveRedisClient | 🔴 高 | 2h | ✅ 已完成 |
| Task-08 | CacheComponent 多级缓存测试 | CacheComponent | 🔴 高 | 2h | ✅ 已完成 |
| Task-09 | SeqGenerator 序列号生成器测试 | SeqGenerator | 🟡 中 | 2h | ✅ 已完成 |
| Task-10 | 工具类与序列化器测试 | IdStrUtils, LongRedisSerializer | 🟡 中 | 1h | ✅ 已完成 |

---

## Task-01: RedisClient String操作测试

### 目标类
`com.blink.framework.redis.component.RedisClient`

### 测试范围
String 类型相关操作方法

### 测试用例清单

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 01-01 | `testSetAndGet` | 设置并获取字符串值 | 成功设置并返回正确值 |
| 01-02 | `testSetAndGetNull` | 获取不存在的key | 返回null |
| 01-03 | `testSetExWithDuration` | 设置带过期时间的值（Duration） | 值正确设置并有过期时间 |
| 01-04 | `testSetExWithSeconds` | 设置带过期时间的值（秒） | 值正确设置并有过期时间 |
| 01-05 | `testSetIfAbsent_Success` | key不存在时设置 | 返回true，值设置成功 |
| 01-06 | `testSetIfAbsent_Fail` | key已存在时设置 | 返回false，原值不变 |
| 01-07 | `testSetIfAbsentWithExpire` | key不存在时设置带过期时间 | 返回true，值有过期时间 |
| 01-08 | `testSetIfPresent_Success` | key存在时更新 | 返回true，值更新成功 |
| 01-09 | `testSetIfPresent_Fail` | key不存在时设置 | 返回false |
| 01-10 | `testGetAndSet` | 获取旧值并设置新值 | 返回旧值，key更新为新值 |
| 01-11 | `testIncrement` | 整数递增操作 | 返回递增后的值 |
| 01-12 | `testIncrementBy` | 整数递增指定步长 | 返回递增后的值 |
| 01-13 | `testIncrementByDouble` | 浮点数递增 | 返回正确的浮点数值 |
| 01-14 | `testDecrement` | 整数递减操作 | 返回递减后的值 |
| 01-15 | `testDecrementBy` | 整数递减指定步长 | 返回递减后的值 |
| 01-16 | `testStrLen` | 获取字符串长度 | 返回正确长度 |
| 01-17 | `testAppend` | 追加字符串 | 返回追加后的总长度 |
| 01-18 | `testGetOrDefault_Exists` | key存在时获取默认值 | 返回实际值 |
| 01-19 | `testGetOrDefault_NotExists` | key不存在时获取默认值 | 返回默认值 |
| 01-20 | `testSetWithRetry_Success` | 重试机制-首次成功 | 返回true |
| 01-21 | `testSetWithRetry_RetrySuccess` | 重试机制-重试后成功 | 返回true |
| 01-22 | `testSetWithRetry_AllFail` | 重试机制-全部失败 | 返回false |
| 01-23 | `testExists_True` | key存在判断 | 返回true |
| 01-24 | `testExists_False` | key不存在判断 | 返回false |
| 01-25 | `testDelete_Success` | 删除存在的key | 返回true |
| 01-26 | `testDelete_NotExists` | 删除不存在的key | 返回false |
| 01-27 | `testDeleteMultiple` | 批量删除多个key | 返回删除数量 |
| 01-28 | `testExpire` | 设置过期时间 | 返回true |
| 01-29 | `testTtl` | 获取剩余过期时间 | 返回正确秒数 |
| 01-30 | `testPersist` | 移除过期时间 | 返回true |
| 01-31 | `testType` | 获取key类型 | 返回"STRING" |

### 技术要点
- 使用 Mockito mock `RedisTemplate` 和 `ValueOperations`
- 使用 `@BeforeEach` 初始化测试环境
- 验证方法调用参数和返回值

---

## Task-02: RedisClient Hash操作测试

### 目标类
`com.blink.framework.redis.component.RedisClient`

### 测试范围
Hash 类型相关操作方法

### 测试用例清单

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 02-01 | `testHPutField` | 设置单个hash字段 | 成功设置 |
| 02-02 | `testHSet` | 批量设置hash字段 | 成功设置 |
| 02-03 | `testHGetField_Exists` | 获取存在的hash字段 | 返回正确值 |
| 02-04 | `testHGetField_NotExists` | 获取不存在的hash字段 | 返回null |
| 02-05 | `testHMultiGetFields` | 批量获取多个hash字段 | 返回值列表 |
| 02-06 | `testHGet` | 获取整个hash | 返回Map |
| 02-07 | `testHGetStringMap` | 获取String类型key的hash | 返回Map<String, Object> |
| 02-08 | `testHGetStringMap_Empty` | 空hash返回空Map | 返回空Map |
| 02-09 | `testHDeleteFields` | 删除hash字段 | 返回删除数量 |
| 02-10 | `testHExists_True` | hash字段存在判断 | 返回true |
| 02-11 | `testHExists_False` | hash字段不存在判断 | 返回false |
| 02-12 | `testHSize` | 获取hash字段数量 | 返回正确数量 |
| 02-13 | `testHIncrement` | hash字段递增 | 返回递增后的值 |

### 技术要点
- Mock `HashOperations`
- 测试类型转换逻辑（`hGetStringMap`）

---

## Task-03: RedisClient List/Set/ZSet操作测试

### 目标类
`com.blink.framework.redis.component.RedisClient`

### 测试范围
List、Set、ZSet 类型相关操作方法

### 测试用例清单

#### List 操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 03-01 | `testLPush` | 左侧推入多个值 | 返回列表长度 |
| 03-02 | `testRPush` | 右侧推入多个值 | 返回列表长度 |
| 03-03 | `testLPop` | 左侧弹出 | 返回弹出的值 |
| 03-04 | `testRPop` | 右侧弹出 | 返回弹出的值 |
| 03-05 | `testLPopWithTimeout` | 带超时的左侧弹出 | 返回值或null |
| 03-06 | `testRPopWithTimeout` | 带超时的右侧弹出 | 返回值或null |
| 03-07 | `testLRange` | 获取范围内元素 | 返回元素列表 |
| 03-08 | `testLLen` | 获取列表长度 | 返回长度 |
| 03-09 | `testLIndex` | 按索引获取元素 | 返回元素 |
| 03-10 | `testLSet` | 设置指定索引元素 | 成功设置 |
| 03-11 | `testLRemove` | 移除指定数量元素 | 返回移除数量 |
| 03-12 | `testLTrim` | 修剪列表 | 成功修剪 |

#### Set 操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 03-13 | `testSAdd` | 添加集合成员 | 返回添加数量 |
| 03-14 | `testSMembers` | 获取所有成员 | 返回Set |
| 03-15 | `testSIsMember_True` | 成员存在判断-存在 | 返回true |
| 03-16 | `testSIsMember_False` | 成员存在判断-不存在 | 返回false |
| 03-17 | `testSSize` | 获取集合大小 | 返回大小 |
| 03-18 | `testSRemove` | 移除成员 | 返回移除数量 |
| 03-19 | `testSPop` | 随机弹出成员 | 返回弹出的成员 |
| 03-20 | `testSRandomMembers` | 随机获取多个成员 | 返回成员列表 |
| 03-21 | `testSUnion` | 集合并集 | 返回并集结果 |
| 03-22 | `testSIntersect` | 集合交集 | 返回交集结果 |
| 03-23 | `testSDifference` | 集合差集 | 返回差集结果 |

#### ZSet 操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 03-24 | `testZAdd` | 添加有序集合成员 | 返回true/false |
| 03-25 | `testZAddBatch` | 批量添加有序集合成员 | 返回添加数量 |
| 03-26 | `testZRange` | 按索引范围获取成员 | 返回成员集合 |
| 03-27 | `testZRangeByScore` | 按分数范围获取成员 | 返回成员集合 |
| 03-28 | `testZRangeByScoreWithOffset` | 带偏移量和数量限制 | 返回成员集合 |
| 03-29 | `testZScore` | 获取成员分数 | 返回分数 |
| 03-30 | `testZSize` | 获取成员数量 | 返回数量 |
| 03-31 | `testZCount` | 按分数范围统计数量 | 返回数量 |
| 03-32 | `testZRank` | 获取成员排名（升序） | 返回排名 |
| 03-33 | `testZReverseRank` | 获取成员排名（降序） | 返回排名 |
| 03-34 | `testZRemove` | 移除成员 | 返回移除数量 |
| 03-35 | `testZRemoveRange` | 按排名范围移除 | 返回移除数量 |
| 03-36 | `testZRemoveRangeByScore` | 按分数范围移除 | 返回移除数量 |
| 03-37 | `testZIncrementScore` | 增加成员分数 | 返回新分数 |

---

## Task-04: RedisClient Stream操作测试

### 目标类
`com.blink.framework.redis.component.RedisClient`

### 测试范围
Redis Stream 相关操作方法

### 测试用例清单

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 04-01 | `testXAdd_WithMap` | 发送消息（Map格式） | 返回消息ID |
| 04-02 | `testXAdd_WithObject` | 发送消息（Object格式） | 返回消息ID |
| 04-03 | `testXRead` | 读取消息 | 返回消息列表 |
| 04-04 | `testXRead_EmptyStream` | 读取空Stream | 返回空列表 |
| 04-05 | `testXGroupCreate_Success` | 创建消费者组 | 返回true |
| 04-06 | `testXGroupCreate_AlreadyExists` | 消费者组已存在 | 返回false |
| 04-07 | `testXReadGroup` | 从消费者组读取消息 | 返回消息列表 |
| 04-08 | `testXAck_Success` | 确认消息处理 | 返回true |
| 04-09 | `testXAck_Fail` | 确认不存在的消息 | 返回false |
| 04-10 | `testXInfo` | 获取Stream信息 | 返回XInfoStream |
| 04-11 | `testXDel` | 删除消息 | 返回删除数量 |
| 04-12 | `testXTrim` | 修剪Stream | 返回删除数量 |
| 04-13 | `testXInfoGroups` | 获取消费者组信息 | 返回XInfoGroups |
| 04-14 | `testXInfoConsumers` | 获取消费者信息 | 返回XInfoConsumers |

### 技术要点
- Mock `StreamOperations`
- 测试异常处理（`RedisBusyException`）

---

## Task-05: RedisClient 批量操作与Lua脚本测试

### 目标类
`com.blink.framework.redis.component.RedisClient`

### 测试范围
批量操作、Lua脚本、管道操作、扫描操作

### 测试用例清单

#### 批量操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 05-01 | `testMultiGet` | 批量获取多个key | 返回值列表 |
| 05-02 | `testBatchGet` | 批量获取并转为Map | 返回Map |
| 05-03 | `testBatchGet_EmptyKeys` | 空key列表 | 返回空Map |
| 05-04 | `testBatchSet` | 批量设置多个key | 成功设置 |
| 05-05 | `testBatchSetWithExpire` | 批量设置带过期时间 | 返回true |
| 05-06 | `testMultiSetIfAbsent_Success` | 所有key不存在时设置 | 返回true |
| 05-07 | `testMultiSetIfAbsent_Fail` | 有key存在时设置 | 返回false |

#### 批量删除

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 05-08 | `testDeleteByPrefix_Deprecated` | 按前缀删除（已废弃） | 返回删除数量 |
| 05-09 | `testDeleteByPrefixScan` | 按前缀扫描删除 | 返回删除数量 |
| 05-10 | `testDeleteByPrefixPipeline` | 按前缀管道删除 | 返回删除数量 |
| 05-11 | `testDeleteByPrefixLua` | 按前缀Lua删除 | 返回删除数量 |
| 05-12 | `testDeleteByPrefixSafely_Success` | 安全删除-成功 | 返回true |
| 05-13 | `testDeleteByPrefixSafely_RetrySuccess` | 安全删除-重试成功 | 返回true |
| 05-14 | `testDeleteKeys` | 删除指定key列表 | 返回删除数量 |
| 05-15 | `testCountByPrefix` | 统计前缀匹配数量 | 返回数量 |
| 05-16 | `testGetKeysByPrefix` | 分页获取前缀匹配key | 返回key列表 |

#### Lua脚本

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 05-17 | `testExecute_WithRedisScript` | 执行RedisScript对象 | 返回正确结果 |
| 05-18 | `testExecute_WithScriptString` | 执行Lua脚本字符串 | 返回正确结果 |
| 05-19 | `testExecute_WithSerializer` | 带序列化器执行脚本 | 返回正确结果 |
| 05-20 | `testExecuteWithStringSerializer` | 使用String序列化器 | 返回正确结果 |

#### 管道操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 05-21 | `testExecutePipelined_WithRedisCallback` | 管道执行RedisCallback | 返回结果 |
| 05-22 | `testExecutePipelined_WithSessionCallback` | 管道执行SessionCallback | 返回结果列表 |

#### 扫描操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 05-23 | `testScan` | 扫描匹配key | 返回Cursor |

---

## Task-06: ReactiveRedisClient 基础操作测试

### 目标类
`com.blink.framework.redis.component.ReactiveRedisClient`

### 测试范围
通用操作、String操作、Hash操作

### 测试用例清单

#### 通用操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 06-01 | `testExists_True` | key存在判断-存在 | 返回Mono<Boolean>=true |
| 06-02 | `testExists_False` | key存在判断-不存在 | 返回Mono<Boolean>=false |
| 06-03 | `testDelete_Multiple` | 删除多个key | 返回Mono<Long> |
| 06-04 | `testDelete_Single` | 删除单个key | 返回Mono<Boolean> |
| 06-05 | `testExpire` | 设置过期时间 | 返回Mono<Boolean> |
| 06-06 | `testTtl` | 获取剩余过期时间 | 返回Mono<Duration> |
| 06-07 | `testPersist` | 移除过期时间 | 返回Mono<Boolean> |
| 06-08 | `testKeys` | 获取匹配模式的所有key | 返回Flux<String> |
| 06-09 | `testScan` | 扫描匹配key | 返回Flux<String> |
| 06-10 | `testType` | 获取key类型 | 返回Mono<String> |

#### String 操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 06-11 | `testSet` | 设置值 | 返回Mono<Boolean>=true |
| 06-12 | `testSetEx` | 设置带过期时间的值 | 返回Mono<Boolean>=true |
| 06-13 | `testSetIfAbsent_Success` | key不存在时设置 | 返回Mono<Boolean>=true |
| 06-14 | `testSetIfAbsent_Fail` | key已存在时设置 | 返回Mono<Boolean>=false |
| 06-15 | `testSetIfAbsentWithExpire` | key不存在时设置带过期时间 | 返回Mono<Boolean> |
| 06-16 | `testSetIfPresent_Success` | key存在时更新 | 返回Mono<Boolean>=true |
| 06-17 | `testSetIfPresent_Fail` | key不存在时设置 | 返回Mono<Boolean>=false |
| 06-18 | `testGet` | 获取值 | 返回Mono<Object> |
| 06-19 | `testGetAndSet` | 获取并设置新值 | 返回Mono<Object> |
| 06-20 | `testIncrement` | 递增操作 | 返回Mono<Long> |
| 06-21 | `testIncrementBy` | 递增指定步长 | 返回Mono<Long> |
| 06-22 | `testDecrement` | 递减操作 | 返回Mono<Long> |
| 06-23 | `testDecrementBy` | 递减指定步长 | 返回Mono<Long> |
| 06-24 | `testStrLen` | 获取字符串长度 | 返回Mono<Long> |
| 06-25 | `testGetOrDefault_Exists` | key存在时获取默认值 | 返回实际值 |
| 06-26 | `testGetOrDefault_NotExists` | key不存在时获取默认值 | 返回默认值 |
| 06-27 | `testSetWithRetry_Success` | 重试机制-成功 | 返回Mono<Boolean>=true |
| 06-28 | `testSetWithRetry_AllFail` | 重试机制-全部失败 | 返回Mono<Boolean>=false |

#### Hash 操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 06-29 | `testHPut` | 设置hash字段 | 返回Mono<Boolean> |
| 06-30 | `testHPutAll` | 批量设置hash字段 | 返回Mono<Boolean> |
| 06-31 | `testHGet` | 获取hash字段值 | 返回Mono<Object> |
| 06-32 | `testHMultiGet` | 批量获取hash字段 | 返回Mono<List<Object>> |
| 06-33 | `testHEntries` | 获取所有字段和值 | 返回Flux<Entry> |
| 06-34 | `testHKeys` | 获取所有字段名 | 返回Flux<Object> |
| 06-35 | `testHValues` | 获取所有字段值 | 返回Flux<Object> |
| 06-36 | `testHDelete` | 删除hash字段 | 返回Mono<Long> |
| 06-37 | `testHExists_True` | hash字段存在判断-存在 | 返回Mono<Boolean>=true |
| 06-38 | `testHExists_False` | hash字段存在判断-不存在 | 返回Mono<Boolean>=false |
| 06-39 | `testHSize` | 获取hash字段数量 | 返回Mono<Long> |
| 06-40 | `testHIncrement` | hash字段递增 | 返回Mono<Long> |

---

## Task-07: ReactiveRedisClient 高级操作测试

### 目标类
`com.blink.framework.redis.component.ReactiveRedisClient`

### 测试范围
List、Set、ZSet、Stream、Lua脚本、管道操作

### 测试用例清单

#### List 操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 07-01 | `testLPush` | 左侧推入 | 返回Mono<Long> |
| 07-02 | `testRPush` | 右侧推入 | 返回Mono<Long> |
| 07-03 | `testLPop` | 左侧弹出 | 返回Mono<Object> |
| 07-04 | `testRPop` | 右侧弹出 | 返回Mono<Object> |
| 07-05 | `testLRange` | 获取范围元素 | 返回Flux<Object> |
| 07-06 | `testLLen` | 获取列表长度 | 返回Mono<Long> |
| 07-07 | `testLIndex` | 按索引获取元素 | 返回Mono<Object> |
| 07-08 | `testLSet` | 设置指定索引元素 | 返回Mono<Boolean> |

#### Set 操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 07-09 | `testSAdd` | 添加集合成员 | 返回Mono<Long> |
| 07-10 | `testSMembers` | 获取所有成员 | 返回Flux<Object> |
| 07-11 | `testSIsMember` | 成员存在判断 | 返回Mono<Boolean> |
| 07-12 | `testSSize` | 获取集合大小 | 返回Mono<Long> |
| 07-13 | `testSRemove` | 移除成员 | 返回Mono<Long> |
| 07-14 | `testSPop` | 随机弹出成员 | 返回Mono<Object> |

#### ZSet 操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 07-15 | `testZAdd` | 添加有序集合成员 | 返回Mono<Boolean> |
| 07-16 | `testZAddBatch` | 批量添加 | 返回Mono<Long> |
| 07-17 | `testZRange` | 按索引范围获取 | 返回Flux<Object> |
| 07-18 | `testZRangeByScore` | 按分数范围获取 | 返回Flux<Object> |
| 07-19 | `testZScore` | 获取成员分数 | 返回Mono<Double> |
| 07-20 | `testZSize` | 获取成员数量 | 返回Mono<Long> |
| 07-21 | `testZRank` | 获取成员排名 | 返回Mono<Long> |
| 07-22 | `testZRemove` | 移除成员 | 返回Mono<Long> |

#### Stream 操作

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 07-23 | `testXAdd_SingleField` | 发送单字段消息 | 返回Mono<String> |
| 07-24 | `testXAdd_MultiField` | 发送多字段消息 | 返回Mono<String> |
| 07-25 | `testXLen` | 获取Stream长度 | 返回Mono<Long> |
| 07-26 | `testXDel` | 删除消息 | 返回Mono<Long> |
| 07-27 | `testXRange` | 范围查询 | 返回Flux<MapRecord> |
| 07-28 | `testXRangeWithCount` | 带数量限制的范围查询 | 返回Flux<MapRecord> |
| 07-29 | `testXRevRange` | 反向范围查询 | 返回Flux<MapRecord> |
| 07-30 | `testXGroupCreate` | 创建消费者组 | 返回Mono<String> |
| 07-31 | `testXGroupDestroy` | 删除消费者组 | 返回Mono<String> |
| 07-32 | `testXReadGroup` | 从消费者组读取 | 返回Flux<MapRecord> |
| 07-33 | `testXAck` | 确认消息 | 返回Mono<Long> |
| 07-34 | `testXPending` | 查看待处理消息 | 返回Mono<PendingMessagesSummary> |
| 07-35 | `testXTrim` | 修剪Stream | 返回Mono<Long> |
| 07-36 | `testXInfo` | 获取Stream信息 | 返回Mono<XInfoStream> |
| 07-37 | `testXInfoGroups` | 获取消费者组信息 | 返回Flux<XInfoGroup> |
| 07-38 | `testXInfoConsumers` | 获取消费者信息 | 返回Flux<XInfoConsumer> |
| 07-39 | `testXClaim` | 转移消息所有权 | 返回Flux<MapRecord> |
| 07-40 | `testXAddBatch` | 批量添加消息 | 返回Flux<String> |
| 07-41 | `testXReadGroupAndAck` | 读取并确认消息 | 返回Flux<MapRecord> |

#### 发布订阅 & Lua & 管道

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 07-42 | `testPublish` | 发布消息 | 返回Mono<Long> |
| 07-43 | `testExecute_WithString` | 执行Lua脚本字符串 | 返回Flux<T> |
| 07-44 | `testExecute_WithRedisScript` | 执行RedisScript | 返回Flux<T> |
| 07-45 | `testExecuteForMono` | 执行Lua返回单个结果 | 返回Mono<T> |
| 07-46 | `testExecutePipelined` | 管道执行 | 返回Flux<T> |

---

## Task-08: CacheComponent 多级缓存测试

### 目标类
`com.blink.framework.redis.component.CacheComponent`

### 测试范围
多级缓存协调、缓存穿透回源、批量加载

### 测试用例清单

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 08-01 | `testGetFromAllCache_LocalCacheHit` | 本地缓存命中 | 直接返回本地缓存值，不查Redis |
| 08-02 | `testGetFromAllCache_RedisCacheHit` | Redis缓存命中 | 返回Redis值并回填本地缓存 |
| 08-03 | `testGetFromAllCache_BothMiss` | 两级缓存都未命中 | 返回null |
| 08-04 | `testGetFromAllCache_LocalCacheDisabled` | 本地缓存禁用 | 只查Redis |
| 08-05 | `testGetFromCacheOrDB_CacheHit` | 缓存命中 | 直接返回缓存值 |
| 08-06 | `testGetFromCacheOrDB_CacheMiss_DBSuccess` | 缓存未命中，DB查询成功 | 返回DB值并写入缓存 |
| 08-07 | `testGetFromCacheOrDB_CacheMiss_DBReturnsNull` | 缓存未命中，DB返回null | 返回null，不写入缓存 |
| 08-08 | `testGetFromCacheOrDB_Exception` | 缓存操作异常 | 抛出BlinkException |
| 08-09 | `testResetCache_LocalCacheEnabled` | 重置缓存-本地缓存启用 | 更新本地缓存和Redis |
| 08-10 | `testResetCache_LocalCacheDisabled` | 重置缓存-本地缓存禁用 | 只更新Redis |
| 08-11 | `testLoadCacheFromDB` | 从DB批量加载缓存 | 删除旧缓存，设置新缓存，加载到本地 |
| 08-12 | `testLoadCacheFromDB_EmptyMap` | 从DB加载空Map | 正常处理 |
| 08-13 | `testClearLocalCache_SingleKey` | 清除单个本地缓存key | 本地缓存被清除 |
| 08-14 | `testClearLocalCache_MultipleKeys` | 清除多个本地缓存key | 本地缓存被清除 |
| 08-15 | `testClearLocalCache_EmptyList` | 清除空key列表 | 无操作 |
| 08-16 | `testClearLocalCache_LocalCacheDisabled` | 本地缓存禁用时清除 | 无操作 |

### 技术要点
- Mock `RedisClient` 和 `Caffeine Cache`
- Mock `ApplicationContextUtil` 获取本地缓存
- 测试本地缓存启用/禁用两种场景

---

## Task-09: SeqGenerator 序列号生成器测试

### 目标类
`com.blink.framework.redis.id.SeqGenerator`

### 测试范围
分段分配、并发安全、异步预取、异常重试

### 测试用例清单

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 09-01 | `testGenerateSeq_Success` | 正常生成序列号 | 返回递增序列号 |
| 09-02 | `testGenerateSeq_BlankKey` | key为空 | 抛出BlinkException |
| 09-03 | `testGenerateSeq_KeyNotConfigured` | key未配置 | 抛出BlinkException |
| 09-04 | `testGenerateSeq_SegmentExhausted` | 分段耗尽 | 从Redis获取新分段 |
| 09-05 | `testGenerateSeq_ConcurrentUniqueness` | 并发生成唯一性 | 所有ID唯一 |
| 09-06 | `testGenerateSeq_PrefetchTriggered` | 触发异步预取 | 预取分段被创建 |
| 09-07 | `testGenerateSeq_PrefetchPromoted` | 预取分段被提升 | 使用预取分段 |
| 09-08 | `testGenerateSeq_RetrySuccess` | Redis失败后重试成功 | 返回序列号 |
| 09-09 | `testGenerateSeq_AllRetriesFailed` | 重试全部失败 | 抛出异常 |
| 09-10 | `testInit` | 初始化预分配缓存 | 缓存被初始化 |
| 09-11 | `testDestroy` | 优雅关闭 | 线程池正常关闭 |
| 09-12 | `testDestroy_Interrupted` | 关闭被中断 | 强制关闭 |
| 09-13 | `testShouldPrefetch` | 判断是否需要预取 | 返回正确判断结果 |
| 09-14 | `testSeqSegment_GetNextSeq` | 分段获取下一个序列号 | 返回正确序列号 |
| 09-15 | `testSeqSegment_Exhausted` | 分段耗尽 | 返回-1 |
| 09-16 | `testSeqSegment_UsageRate` | 计算使用率 | 返回正确比例 |

### 技术要点
- Mock `RedisClient` 和 `BlinkRedisProperties`
- 使用 `CountDownLatch` 测试并发场景
- 使用 `CompletableFuture` 测试异步预取

---

## Task-10: 工具类与序列化器测试

### 目标类
- `com.blink.framework.redis.id.IdStrUtils`
- `com.blink.framework.redis.serializer.LongRedisSerializer`

### 测试范围
字符串工具方法、Long序列化器

### 测试用例清单

#### IdStrUtils

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 10-01 | `testStringFillAuto_PadLeft` | 左侧补位 | 返回补位后字符串 |
| 10-02 | `testStringFillAuto_NoPadding` | 无需补位 | 返回原字符串 |
| 10-03 | `testStringFillAuto_EmptyString` | 空字符串补位 | 返回全补位字符串 |
| 10-04 | `testGetMaxValue` | 计算最大值 | 返回正确最大值 |
| 10-05 | `testGetMaxIdGenLength` | 计算ID生成长度 | 返回正确长度 |
| 10-06 | `testGetDateTimeString` | 获取日期时间字符串 | 返回正确格式 |

#### LongRedisSerializer

| 编号 | 测试方法 | 测试场景 | 预期结果 |
|------|----------|----------|----------|
| 10-07 | `testSerialize` | 序列化Long | 返回正确字节数组 |
| 10-08 | `testDeserialize` | 反序列化字节数组 | 返回正确Long值 |
| 10-09 | `testSerialize_Zero` | 序列化0 | 返回"0"的字节数组 |
| 10-10 | `testDeserialize_Zero` | 反序列化"0" | 返回0L |
| 10-11 | `testSerialize_MaxValue` | 序列化Long.MAX_VALUE | 返回正确字节数组 |
| 10-12 | `testDeserialize_MaxValue` | 反序列化最大值 | 返回Long.MAX_VALUE |
| 10-13 | `testSerialize_MinValue` | 序列化Long.MIN_VALUE | 返回正确字节数组 |
| 10-14 | `testDeserialize_MinValue` | 反序列化最小值 | 返回Long.MIN_VALUE |
| 10-15 | `testRoundTrip` | 序列化后反序列化 | 返回原值 |

---

## 测试进度追踪

- [x] Task-01: RedisClient String操作测试 ✅ 31个测试用例通过
- [x] Task-02: RedisClient Hash操作测试 ✅ 14个测试用例通过
- [x] Task-03: RedisClient List/Set/ZSet操作测试 ✅ 37个测试用例通过
- [x] Task-04: RedisClient Stream操作测试 ✅ 14个测试用例通过
- [x] Task-05: RedisClient 批量操作与Lua脚本测试 ✅ 22个测试用例通过
- [x] Task-06: ReactiveRedisClient 基础操作测试 ✅ 40个测试用例通过
- [x] Task-07: ReactiveRedisClient 高级操作测试 ✅ 46个测试用例通过
- [x] Task-08: CacheComponent 多级缓存测试 ✅ 16个测试用例通过
- [x] Task-09: SeqGenerator 序列号生成器测试 ✅ 16个测试用例通过
- [x] Task-10: 工具类与序列化器测试 ✅ 17个测试用例通过

**总计: 253个测试用例全部通过**

---

## 测试规范

### 测试类命名规范
- 测试类命名：`{被测试类名}Test`
- 测试类位置：`src/test/java/{包路径}/`

### 测试方法命名规范
- 测试方法命名：`test{方法名}_{场景}`
- 使用 `@DisplayName` 注解提供中文描述

### 测试框架
- JUnit 5
- Mockito
- AssertJ（可选）

### Mock 原则
- 不 mock 被测试类本身
- 只 mock 外部依赖（RedisTemplate、RedissonClient等）
- 验证关键方法的调用和参数

### 测试覆盖要求
- 正常场景：必须覆盖
- 边界场景：必须覆盖
- 异常场景：必须覆盖
- 并发场景：按需覆盖

---

## 更新日志

| 日期 | 更新内容 | 更新人 |
|------|----------|--------|
| 2026-04-13 | 创建测试计划文档 | Claude |
