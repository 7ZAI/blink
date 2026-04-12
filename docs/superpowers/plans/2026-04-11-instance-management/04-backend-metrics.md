# Task 4: MetricsCollectorService 扩展（GC/线程指标）

**依赖:** Task 1 (后端 DTO 和常量定义)

**目标:** 扩展 MetricsCollectorServiceImpl，增加 GC 统计、线程指标采集

---

## 文件清单

- 修改: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/MetricsCollectorServiceImpl.java`

---

### Task 4.1: 扩展 InstanceMetrics 内部类

- [ ] **Step 1: 在 InstanceMetrics 类中新增字段**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/MetricsCollectorServiceImpl.java`

找到 `InstanceMetrics` 内部类（约在文件末尾），在现有字段之后添加以下新字段：

```java
        /** 堆内存使用量 (bytes) */
        Long heapUsed;
        /** 堆内存最大值 (bytes) */
        Long heapMax;
        /** 非堆内存使用量 (bytes) */
        Long nonHeapUsed;
        /** 年轻代 GC 次数 */
        Long youngGcCount;
        /** 年轻代 GC 时间 (ms) */
        Long youngGcTime;
        /** 老年代 GC 次数 */
        Long oldGcCount;
        /** 老年代 GC 时间 (ms) */
        Long oldGcTime;
        /** 活跃线程数 */
        Integer liveThreads;
        /** 峰值线程数 */
        Integer peakThreads;
        /** 守护线程数 */
        Integer daemonThreads;
```

---

### Task 4.2: 扩展 collectInstanceMetrics 方法

- [ ] **Step 2: 在 collectInstanceMetrics 方法中添加 GC 指标采集**

找到 `collectInstanceMetrics` 方法，在获取 JVM 内存指标之后，添加 GC 指标采集代码：

```java
            // 3. 获取堆内存和非堆内存指标（新增）
            // 堆内存使用
            Map<String, Object> heapUsedMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.memory.used?tag=area:heap");
            if (MapUtil.isNotEmpty(heapUsedMetrics)) {
                metrics.heapUsed = extractMeasureValue(heapUsedMetrics);
            }
            
            // 堆内存最大
            Map<String, Object> heapMaxMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.memory.max?tag=area:heap");
            if (MapUtil.isNotEmpty(heapMaxMetrics)) {
                metrics.heapMax = extractMeasureValue(heapMaxMetrics);
            }
            
            // 非堆内存使用
            Map<String, Object> nonHeapMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.memory.used?tag=area:nonheap");
            if (MapUtil.isNotEmpty(nonHeapMetrics)) {
                metrics.nonHeapUsed = extractMeasureValue(nonHeapMetrics);
            }

            // 4. 获取 GC 指标（新增）
            // 年轻代 GC - 使用 jvm.gc.pause 指标
            try {
                Map<String, Object> youngGcMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.gc.count?tag=gc:G1 Young Generation");
                if (MapUtil.isNotEmpty(youngGcMetrics)) {
                    metrics.youngGcCount = extractMeasureValue(youngGcMetrics);
                }
                
                Map<String, Object> youngGcTimeMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.gc.time?tag=gc:G1 Young Generation");
                if (MapUtil.isNotEmpty(youngGcTimeMetrics)) {
                    metrics.youngGcTime = extractMeasureValue(youngGcTimeMetrics);
                }
            } catch (Exception e) {
                log.debug("[MetricsCollector] 获取年轻代 GC 指标失败 | instanceId: {}", instanceId);
            }
            
            // 老年代 GC
            try {
                Map<String, Object> oldGcMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.gc.count?tag=gc:G1 Old Generation");
                if (MapUtil.isNotEmpty(oldGcMetrics)) {
                    metrics.oldGcCount = extractMeasureValue(oldGcMetrics);
                }
                
                Map<String, Object> oldGcTimeMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.gc.time?tag=gc:G1 Old Generation");
                if (MapUtil.isNotEmpty(oldGcTimeMetrics)) {
                    metrics.oldGcTime = extractMeasureValue(oldGcTimeMetrics);
                }
            } catch (Exception e) {
                log.debug("[MetricsCollector] 获取老年代 GC 指标失败 | instanceId: {}", instanceId);
            }

            // 5. 获取线程指标（新增）
            try {
                Map<String, Object> liveThreadsMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.threads.live");
                if (MapUtil.isNotEmpty(liveThreadsMetrics)) {
                    Object value = extractMeasureValueAsObject(liveThreadsMetrics);
                    if (ObjectUtil.isNotNull(value)) {
                        metrics.liveThreads = ((Number) value).intValue();
                    }
                }
                
                Map<String, Object> peakThreadsMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.threads.peak");
                if (MapUtil.isNotEmpty(peakThreadsMetrics)) {
                    Object value = extractMeasureValueAsObject(peakThreadsMetrics);
                    if (ObjectUtil.isNotNull(value)) {
                        metrics.peakThreads = ((Number) value).intValue();
                    }
                }
                
                Map<String, Object> daemonThreadsMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.threads.daemon");
                if (MapUtil.isNotEmpty(daemonThreadsMetrics)) {
                    Object value = extractMeasureValueAsObject(daemonThreadsMetrics);
                    if (ObjectUtil.isNotNull(value)) {
                        metrics.daemonThreads = ((Number) value).intValue();
                    }
                }
            } catch (Exception e) {
                log.debug("[MetricsCollector] 获取线程指标失败 | instanceId: {}", instanceId);
            }
```

---

- [ ] **Step 3: 添加 extractMeasureValueAsObject 辅助方法**

在辅助方法区域添加：

```java
    /**
     * 从 metrics 响应中提取测量值（Object 类型）
     */
    @SuppressWarnings("unchecked")
    private Object extractMeasureValueAsObject(Map<String, Object> metrics) {
        try {
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) metrics.get("measurements");
            if (CollUtil.isNotEmpty(measurements)) {
                return measurements.get(0).get("value");
            }
        } catch (Exception e) {
            log.debug("[MetricsCollector] 提取测量值失败: {}", e.getMessage());
        }
        return null;
    }
```

---

### Task 4.3: 扩展 saveToRedis 方法

- [ ] **Step 4: 在 saveToRedis 方法中添加新指标存储**

找到 `saveToRedis` 方法，在现有指标存储之后添加：

```java
        // 存储新增的 JVM 指标
        if (ObjectUtil.isNotNull(metrics.heapUsed)) {
            data.put("heapUsed", metrics.heapUsed);
        }
        if (ObjectUtil.isNotNull(metrics.heapMax)) {
            data.put("heapMax", metrics.heapMax);
        }
        if (ObjectUtil.isNotNull(metrics.nonHeapUsed)) {
            data.put("nonHeapUsed", metrics.nonHeapUsed);
        }
        
        // 存储 GC 指标
        if (ObjectUtil.isNotNull(metrics.youngGcCount)) {
            data.put("youngGcCount", metrics.youngGcCount);
        }
        if (ObjectUtil.isNotNull(metrics.youngGcTime)) {
            data.put("youngGcTime", metrics.youngGcTime);
        }
        if (ObjectUtil.isNotNull(metrics.oldGcCount)) {
            data.put("oldGcCount", metrics.oldGcCount);
        }
        if (ObjectUtil.isNotNull(metrics.oldGcTime)) {
            data.put("oldGcTime", metrics.oldGcTime);
        }
        
        // 存储线程指标
        if (ObjectUtil.isNotNull(metrics.liveThreads)) {
            data.put("liveThreads", metrics.liveThreads);
        }
        if (ObjectUtil.isNotNull(metrics.peakThreads)) {
            data.put("peakThreads", metrics.peakThreads);
        }
        if (ObjectUtil.isNotNull(metrics.daemonThreads)) {
            data.put("daemonThreads", metrics.daemonThreads);
        }
```

---

### Task 4.4: 提交更改

- [ ] **Step 5: Git 提交**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/MetricsCollectorServiceImpl.java
git commit -m "feat(metrics): 扩展 MetricsCollectorService 增加 GC 和线程指标采集

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验收检查

| 检查项 | 状态 |
|--------|------|
| InstanceMetrics 类新增 GC/线程字段 | [ ] |
| collectInstanceMetrics 采集 GC 指标 | [ ] |
| collectInstanceMetrics 采集线程指标 | [ ] |
| collectInstanceMetrics 采集堆/非堆内存 | [ ] |
| saveToRedis 存储新指标到 Redis | [ ] |
| 辅助方法已添加 | [ ] |
| Git 提交成功 | [ ] |

---

## 备注

- GC 指标使用 `jvm.gc.count` 和 `jvm.gc.time`，按 GC 类型标签过滤
- 对于不同的 JVM（G1GC、ParallelGC、CMS），GC 类型名称可能不同，需要根据实际情况调整
- 线程指标使用 `jvm.threads.live`、`jvm.threads.peak`、`jvm.threads.daemon`
- 如果某些指标获取失败，不影响其他指标的采集