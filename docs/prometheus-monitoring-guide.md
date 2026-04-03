# Prometheus 监控 Gateway 指标指南

本文档介绍如何使用 Prometheus 监控 blink-gateway-reactive 服务，包括配置方法、可用指标、查询语法和常用查询示例。

## 目录

- [Prometheus 配置](#prometheus-配置)
- [可用指标列表](#可用指标列表)
- [PromQL 查询语法](#promql-查询语法)
- [常用查询示例](#常用查询示例)
- [Grafana 可视化](#grafana-可视化)

---

## Prometheus 配置

### Gateway 已有配置

gateway-reactive 已配置好 Prometheus 监控基础：

- **依赖**: `spring-boot-starter-actuator` + `micrometer-registry-prometheus`
- **端点**: `/actuator/prometheus` 已启用
- **认证**: HTTP Basic (用户名: `admin`, 密码: `123456`)

### prometheus.yml 配置文件

```yaml
# my global config
global:
  scrape_interval: 15s
  evaluation_interval: 15s

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          # - alertmanager:9093

rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

scrape_configs:
  # Prometheus 自身监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Gateway 监控 - 使用静态配置
  - job_name: 'blink-gateway-reactive'
    metrics_path: '/actuator/prometheus'
    basic_auth:
      username: admin
      password: 123456
    static_configs:
      - targets: ['host.docker.internal:8002']  # Windows/Mac Docker
        # Linux Docker 使用宿主机实际 IP，如：
        # - targets: ['192.168.1.100:8002']
        labels:
          instance: 'gateway-reactive-1'
          env: 'dev'
```

### Docker 启动命令

**Windows/Mac:**

```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v /path/to/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

**Linux:**

```bash
docker run -d \
  --name prometheus \
  --add-host=host.docker.internal:host-gateway \
  -p 9090:9090 \
  -v /path/to/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

---

## 可用指标列表

### HTTP 请求指标

| 指标名 | 说明 |
|--------|------|
| `http_server_requests_seconds_count` | 请求总数 |
| `http_server_requests_seconds_sum` | 请求总耗时 |
| `http_server_requests_seconds_max` | 请求最大耗时 |
| `http_server_requests_active_seconds_active_count` | 当前活跃请求数 |

**标签:**
- `method`: HTTP 方法 (GET, POST, etc.)
- `status`: HTTP 状态码 (200, 401, 500, etc.)
- `uri`: 请求路径
- `outcome`: 结果类型 (SUCCESS, CLIENT_ERROR, SERVER_ERROR)
- `exception`: 异常类型

### Gateway 路由指标

| 指标名 | 说明 |
|--------|------|
| `spring_cloud_gateway_routes_count` | 路由规则数量 |

### Spring Security 指标

| 指标名 | 说明 |
|--------|------|
| `spring_security_authentications_seconds_count` | 认证请求次数 |
| `spring_security_authentications_seconds_sum` | 认证总耗时 |
| `spring_security_authorizations_seconds_count` | 授权请求次数 |
| `spring_security_authorizations_seconds_sum` | 授权总耗时 |
| `spring_security_http_secured_requests_seconds_count` | 安全拦截请求次数 |
| `spring_security_filterchains_seconds_count` | Filter Chain 执行次数 |

### Resilience4j 熔断器指标

| 指标名 | 说明 |
|--------|------|
| `resilience4j_circuitbreaker_state` | 熔断器状态 |
| `resilience4j_circuitbreaker_failure_rate` | 失败率 |
| `resilience4j_circuitbreaker_slow_call_rate` | 慢调用率 |
| `resilience4j_circuitbreaker_calls_seconds_count` | 调用次数 |
| `resilience4j_circuitbreaker_not_permitted_calls_total` | 被拒绝调用数 |
| `resilience4j_circuitbreaker_buffered_calls` | 缓冲调用数 |

**状态值:**
- `0`: closed (正常)
- `1`: open (熔断)
- `2`: half_open (半开)

**调用类型 (kind 标签):**
- `successful`: 成功
- `failed`: 失败
- `ignored`: 忽略

### Redis (Lettuce) 指标

| 指标名 | 说明 |
|--------|------|
| `lettuce_command_completion_seconds_count` | Redis 命令完成次数 |
| `lettuce_command_completion_seconds_sum` | Redis 命令总耗时 |
| `lettuce_command_firstresponse_seconds_count` | Redis 首次响应次数 |

### JVM 指标

| 指标名 | 说明 |
|--------|------|
| `jvm_memory_used_bytes` | JVM 内存使用 |
| `jvm_memory_max_bytes` | JVM 最大内存 |
| `jvm_memory_committed_bytes` | JVM 已分配内存 |
| `jvm_gc_pause_seconds_count` | GC 次数 |
| `jvm_gc_pause_seconds_sum` | GC 总耗时 |
| `jvm_gc_pause_seconds_max` | GC 最大耗时 |
| `jvm_threads_live_threads` | 当前线程数 |
| `jvm_threads_peak_threads` | 线程峰值数 |
| `jvm_threads_states_threads` | 线程状态分布 |
| `jvm_classes_loaded_classes` | 已加载类数量 |

**内存区域 (area 标签):**
- `heap`: 堆内存
- `nonheap`: 非堆内存

### 系统指标

| 指标名 | 说明 |
|--------|------|
| `system_cpu_usage` | 系统 CPU 使用率 |
| `system_cpu_count` | CPU 核心数 |
| `process_cpu_usage` | 进程 CPU 使用率 |
| `process_uptime_seconds` | 进程运行时间 |
| `process_start_time_seconds` | 进程启动时间 |
| `disk_free_bytes` | 磁盘剩余空间 |
| `disk_total_bytes` | 磁盘总空间 |

### 线程池指标

| 指标名 | 说明 |
|--------|------|
| `executor_active_threads` | 活跃线程数 |
| `executor_pool_size_threads` | 线程池大小 |
| `executor_pool_core_threads` | 核心线程数 |
| `executor_pool_max_threads` | 最大线程数 |
| `executor_queued_tasks` | 队列中任务数 |
| `executor_completed_tasks_total` | 已完成任务数 |

---

## PromQL 查询语法

### 基本概念

时间序列结构:
```
指标名称{标签1="值1", 标签2="值2"}  值
http_server_requests_seconds_count{method="POST",status="200"}  1234
```

### 基本查询

```promql
# 查询某个指标的所有时间序列
http_server_requests_seconds_count

# 查询特定标签
http_server_requests_seconds_count{status="200"}

# 多标签组合
http_server_requests_seconds_count{method="POST",status="200"}
```

### 标签匹配操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `=` | 精确匹配 | `status="200"` |
| `!=` | 不匹配 | `status!="200"` |
| `=~` | 正则匹配 | `status=~"2.."` |
| `!~` | 正则不匹配 | `uri!~"/actuator.*"` |

```promql
# 正则匹配多个状态码
http_server_requests_seconds_count{status=~"2..|4.."}

# 排除 actuator 端点
http_server_requests_seconds_count{uri!~"/actuator.*"}

# 匹配所有 POST/PUT 请求
http_server_requests_seconds_count{method=~"POST|PUT"}
```

### 范围查询

使用 `[时间]` 选择时间范围内的数据:

| 单位 | 符号 | 示例 |
|------|------|------|
| 秒 | `s` | `[5s]` |
| 分钟 | `m` | `[5m]` |
| 小时 | `h` | `[1h]` |
| 天 | `d` | `[1d]` |
| 周 | `w` | `[1w]` |

```promql
# 最近 5 分钟的数据
http_server_requests_seconds_count[5m]

# 偏移查询（5 分钟前的数据）
http_server_requests_seconds_count[5m] offset 5m
```

### 聚合操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `sum` | 求和 | `sum(metric)` |
| `avg` | 平均值 | `avg(metric)` |
| `min` | 最小值 | `min(metric)` |
| `max` | 最大值 | `max(metric)` |
| `count` | 计数 | `count(metric)` |
| `stddev` | 标准差 | `stddev(metric)` |

#### 分组聚合

```promql
# 按 URI 分组求和
sum by (uri) (http_server_requests_seconds_count)

# 按 URI 和状态码分组
sum by (uri, status) (http_server_requests_seconds_count)

# 忽略某些标签
sum without (uri) (http_server_requests_seconds_count)
```

#### TOP/BOTTOM 查询

```promql
# TOP 10 最热门接口
topk(10, sum by (uri) (http_server_requests_seconds_count))

# BOTTOM 5 最少请求接口
bottomk(5, sum by (uri) (http_server_requests_seconds_count))
```

### 函数

#### 计数器函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `rate()` | 每秒平均增长率 | `rate(metric[5m])` |
| `irate()` | 瞬时增长率 | `irate(metric[1m])` |
| `increase()` | 时间范围内增量 | `increase(metric[1h])` |

```promql
# 每秒请求速率
rate(http_server_requests_seconds_count[5m])

# 最近 1 小时总请求数
increase(http_server_requests_seconds_count[1h])
```

#### Gauge 函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `delta()` | 计算变化量 | `delta(metric[1h])` |
| `deriv()` | 计算导数 | `deriv(metric[1h])` |
| `predict_linear()` | 预测未来值 | `predict_linear(metric[1h], 4h)` |

```promql
# 内存变化趋势
delta(jvm_memory_used_bytes{area="heap"}[1h])

# 预测 4 小时后内存使用
predict_linear(jvm_memory_used_bytes{area="heap"}[1h], 4h)
```

#### 数学函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `abs()` | 绝对值 | `abs(metric)` |
| `ceil()` | 向上取整 | `ceil(123.45)` |
| `floor()` | 向下取整 | `floor(123.45)` |
| `round()` | 四舍五入 | `round(123.45)` |
| `sqrt()` | 平方根 | `sqrt(metric)` |
| `log2()` | 以2为底的对数 | `log2(metric)` |

#### 时间函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `time()` | 当前时间戳 | `time()` |
| `hour()` | 当前小时 (0-23) | `hour()` |
| `minute()` | 当前分钟 (0-59) | `minute()` |
| `day_of_week()` | 星期 (0-6) | `day_of_week()` |
| `day_of_month()` | 日期 (1-31) | `day_of_month()` |

```promql
# 只在工作时间 (9:00-18:00) 的请求
http_server_requests_seconds_count and hour() >= 9 and hour() < 18
```

### 操作符

#### 数学操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `+` | 加法 | `metric_a + metric_b` |
| `-` | 减法 | `metric_a - metric_b` |
| `*` | 乘法 | `metric * 100` |
| `/` | 除法 | `metric_a / metric_b` |
| `%` | 取模 | `metric % 10` |
| `^` | 幂运算 | `metric ^ 2` |

```promql
# 内存使用百分比
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# 平均响应时间
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```

#### 比较操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `==` | 等于 | `metric == 100` |
| `!=` | 不等于 | `metric != 100` |
| `>` | 大于 | `metric > 100` |
| `<` | 小于 | `metric < 100` |
| `>=` | 大于等于 | `metric >= 100` |
| `<=` | 小于等于 | `metric <= 100` |

```promql
# CPU 使用率超过 80%
process_cpu_usage > 0.8

# 失败率大于 50%
resilience4j_circuitbreaker_failure_rate > 50
```

#### 逻辑操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `and` | 交集 | `metric_a and metric_b` |
| `or` | 并集 | `metric_a or metric_b` |
| `unless` | 补集 | `metric_a unless metric_b` |

```promql
# 同时满足两个条件
http_server_requests_seconds_count{method="POST"} 
  and http_server_requests_seconds_count{status="200"}
```

### Histogram 查询

```promql
# 计算百分位数（需要 histogram 数据）
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# P50 响应时间
histogram_quantile(0.50, rate(http_server_requests_seconds_bucket[5m]))

# P99 响应时间
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

### 子查询

```promql
# 最近 1 小时内，每 5 分钟计算一次 1 分钟的平均值
avg_over_time(rate(http_server_requests_seconds_count[1m])[1h:5m])

# 最近 1 天内的最大 QPS
max_over_time(rate(http_server_requests_seconds_count[5m])[1d])
```

---

## 常用查询示例

### QPS 监控

```promql
# 总 QPS
sum(rate(http_server_requests_seconds_count[1m]))

# 按 URI 分组 QPS
sum by (uri) (rate(http_server_requests_seconds_count[1m]))

# TOP 10 接口 QPS
topk(10, sum by (uri) (rate(http_server_requests_seconds_count[5m])))

# 按 HTTP 方法分组 QPS
sum by (method) (rate(http_server_requests_seconds_count[5m]))
```

### 错误率监控

```promql
# 错误请求 QPS
sum(rate(http_server_requests_seconds_count{status=~"[45].."}[5m]))

# 错误率百分比
sum(rate(http_server_requests_seconds_count{status=~"[45].."}[5m])) 
  / sum(rate(http_server_requests_seconds_count[5m])) * 100

# 按 URI 分组错误率
sum by (uri) (rate(http_server_requests_seconds_count{status=~"[45].."}[5m])) 
  / sum by (uri) (rate(http_server_requests_seconds_count[5m])) * 100

# 4xx 错误率
sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m])) 
  / sum(rate(http_server_requests_seconds_count[5m])) * 100

# 5xx 错误率
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) 
  / sum(rate(http_server_requests_seconds_count[5m])) * 100
```

### 响应时间监控

```promql
# 平均响应时间（秒）
rate(http_server_requests_seconds_sum[5m]) 
  / rate(http_server_requests_seconds_count[5m])

# 平均响应时间（毫秒）
rate(http_server_requests_seconds_sum[5m]) 
  / rate(http_server_requests_seconds_count[5m]) * 1000

# 按 URI 分组响应时间
sum by (uri) (rate(http_server_requests_seconds_sum[5m])) 
  / sum by (uri) (rate(http_server_requests_seconds_count[5m]))

# TOP 5 慢接口
topk(5, 
  rate(http_server_requests_seconds_sum[5m]) 
  / rate(http_server_requests_seconds_count[5m])
)

# 当前活跃请求数
http_server_requests_active_seconds_active_count
```

### 成功率监控

```promql
# 成功请求率 (2xx)
sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m])) 
  / sum(rate(http_server_requests_seconds_count[5m])) * 100

# 按状态码分布
sum by (status) (rate(http_server_requests_seconds_count[5m]))
```

### 熔断器监控

```promql
# 状态 (closed=0, open=1, half_open=2)
resilience4j_circuitbreaker_state{name="protectedCircuitBreaker"}

# 失败率百分比
resilience4j_circuitbreaker_failure_rate{name="protectedCircuitBreaker"}

# 慢调用率百分比
resilience4j_circuitbreaker_slow_call_rate{name="protectedCircuitBreaker"}

# 成功调用数
resilience4j_circuitbreaker_calls_seconds_count{name="protectedCircuitBreaker",kind="successful"}

# 失败调用数
resilience4j_circuitbreaker_calls_seconds_count{name="protectedCircuitBreaker",kind="failed"}

# 被熔断拒绝的请求数
resilience4j_circuitbreaker_not_permitted_calls_total{name="protectedCircuitBreaker"}

# 熔断器调用速率
rate(resilience4j_circuitbreaker_calls_seconds_count{name="protectedCircuitBreaker"}[5m])

# 所有熔断器状态一览
sum by (name) (resilience4j_circuitbreaker_state)
```

### JVM 内存监控

```promql
# Heap 内存使用率百分比
jvm_memory_used_bytes{area="heap"} 
  / jvm_memory_max_bytes{area="heap"} * 100

# Heap 内存使用量 (MB)
jvm_memory_used_bytes{area="heap"} / 1024 / 1024

# Non-heap 内存使用量 (MB)
jvm_memory_used_bytes{area="nonheap"} / 1024 / 1024

# 内存使用趋势（最近 1 小时变化）
delta(jvm_memory_used_bytes{area="heap"}[1h])

# 预测 4 小时后内存使用
predict_linear(jvm_memory_used_bytes{area="heap"}[1h], 4h)
```

### GC 监控

```promql
# GC 频率（每分钟 GC 次数）
rate(jvm_gc_pause_seconds_count[1m]) * 60

# GC 平均耗时
rate(jvm_gc_pause_seconds_sum[5m]) 
  / rate(jvm_gc_pause_seconds_count[5m])

# GC 总耗时（最近 1 小时）
increase(jvm_gc_pause_seconds_sum[1h])

# 按 GC 类型分组
sum by (action, cause) (rate(jvm_gc_pause_seconds_count[5m]))
```

### 线程监控

```promql
# 当前线程数
jvm_threads_live_threads

# 线程峰值数
jvm_threads_peak_threads

# 线程状态分布
jvm_threads_states_threads

# 已启动线程总数
jvm_threads_started_threads_total
```

### CPU 监控

```promql
# 系统 CPU 使用率百分比
system_cpu_usage * 100

# 进程 CPU 使用率百分比
process_cpu_usage * 100

# CPU 核心数
system_cpu_count
```

### Redis 监控

```promql
# Redis 命令执行速率
rate(lettuce_command_completion_seconds_count[5m])

# Redis 响应耗时
rate(lettuce_command_completion_seconds_sum[5m]) 
  / rate(lettuce_command_completion_seconds_count[5m])

# Redis 首次响应耗时
rate(lettuce_command_firstresponse_seconds_sum[5m]) 
  / rate(lettuce_command_firstresponse_seconds_count[5m])
```

### Spring Security 监控

```promql
# 认证请求速率
rate(spring_security_authentications_seconds_count[5m])

# 认证平均耗时
rate(spring_security_authentications_seconds_sum[5m]) 
  / rate(spring_security_authentications_seconds_count[5m])

# 授权请求速率
rate(spring_security_authorizations_seconds_count[5m])

# 授权平均耗时
rate(spring_security_authorizations_seconds_sum[5m]) 
  / rate(spring_security_authorizations_seconds_count[5m])

# 当前活跃认证数
spring_security_authentications_active_seconds_active_count
```

### 系统资源监控

```promql
# 应用运行时长（天）
process_uptime_seconds / 86400

# 磁盘使用率百分比
(disk_total_bytes - disk_free_bytes) / disk_total_bytes * 100

# 磁盘剩余空间 (GB)
disk_free_bytes / 1024 / 1024 / 1024
```

---

## Grafana 可视化

### 启动 Grafana

```bash
docker run -d \
  --name grafana \
  -p 3000:3000 \
  grafana/grafana
```

访问 **http://localhost:3000** (默认账号: admin/admin)

### 添加 Prometheus 数据源

1. Configuration → Data Sources → Add data source
2. 选择 Prometheus
3. URL: `http://prometheus:9090` 或 `http://host.docker.internal:9090`
4. 点击 Save & Test

### 导入 Dashboard

推荐 Dashboard ID:
- **11506** - Spring Cloud Gateway Dashboard
- **4701** - JVM Micrometer Dashboard
- **12900** - Spring Boot Dashboard

导入步骤:
1. Dashboards → Import
2. 输入 Dashboard ID
3. 选择 Prometheus 数据源
4. 点击 Import

### 自定义 Panel 配置

#### Panel 1: 实时 QPS

```promql
sum(rate(http_server_requests_seconds_count[1m]))
```

- Visualization: Stat
- Unit: requests/sec
- Thresholds: Warning 100, Critical 500

#### Panel 2: 请求趋势图

```promql
sum(rate(http_server_requests_seconds_count[5m]))
```

- Visualization: Time series
- Legend: Show

#### Panel 3: 状态码分布

```promql
sum by (status) (rate(http_server_requests_seconds_count[5m]))
```

- Visualization: Pie chart

#### Panel 4: TOP 10 接口

```promql
topk(10, sum by (uri) (rate(http_server_requests_seconds_count[5m])))
```

- Visualization: Bar gauge

#### Panel 5: 熔断器状态

```promql
resilience4j_circuitbreaker_state{name="protectedCircuitBreaker"}
```

- Visualization: Stat
- Value mappings:
  - 0 → green "正常"
  - 1 → red "熔断"
  - 2 → yellow "半开"

#### Panel 6: JVM 内存

```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

- Visualization: Gauge
- Unit: Percent (0-100)
- Thresholds: Warning 70, Critical 90

#### Panel 7: 错误率

```promql
sum(rate(http_server_requests_seconds_count{status=~"[45].."}[5m])) 
  / sum(rate(http_server_requests_seconds_count[5m])) * 100
```

- Visualization: Stat
- Unit: Percent (0-100)
- Thresholds: Warning 1, Critical 5

#### Panel 8: 平均响应时间

```promql
rate(http_server_requests_seconds_sum[5m]) 
  / rate(http_server_requests_seconds_count[5m]) * 1000
```

- Visualization: Time series
- Unit: milliseconds
- Thresholds: Warning 500, Critical 1000

---

## API 查询

### 立时查询

```bash
# 查询当前值
curl 'http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count'

# 查询 QPS
curl 'http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count[1m]))'

# 按 URI 分组
curl 'http://localhost:9090/api/v1/query?query=sum%20by%20(uri)%20(rate(http_server_requests_seconds_count[5m]))'
```

### 范围查询

```bash
# 查询最近 1 小时的数据，每 60 秒一个点
curl 'http://localhost:9090/api/v1/query_range?query=http_server_requests_seconds_count&start=2026-03-16T00:00:00Z&end=2026-03-16T01:00:00Z&step=60s'

# 使用相对时间（最近 30 分钟）
curl 'http://localhost:9090/api/v1/query_range?query=http_server_requests_seconds_count&start=-30m&end=now&step=30s'
```

### 查看所有指标

```bash
# 查询所有指标名称
curl 'http://localhost:9090/api/v1/label/__name__/values'
```

---

## 快速验证

### 检查 Target 状态

访问 **http://localhost:9090/targets** 查看采集状态。

### 直接查询 Gateway 指标

```bash
# 查看 Gateway 暴露的所有指标名
curl -s -u admin:123456 "http://localhost:8002/actuator/prometheus" | grep -v "^#" | cut -d'{' -f1 | sort -u

# 查看 Prometheus 是否成功采集
curl 'http://localhost:9090/api/v1/query?query=up{job="blink-gateway-reactive"}'

# 查看最近 5 分钟是否有数据
curl 'http://localhost:9090/api/v1/query?query=sum(increase(http_server_requests_seconds_count[5m]))'
```