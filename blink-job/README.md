# Blink-Job 定时任务调度框架

基于 Spring Boot 3.2 的统一定时任务调度抽象框架，支持多种调度实现方式，提供一致的任务管理体验。

## 架构概览

```
blink-job/
├── blink-job-api              # API 层 - 注解、接口、枚举、DTO 定义
├── blink-job-core             # 核心层 - 任务注册、执行器、告警处理
├── blink-job-spring-starter   # Spring 原生调度实现
├── blink-job-quartz-starter   # Quartz 调度实现
├── blink-job-xxljob-starter   # XXL-Job 分布式调度实现
└── build.gradle               # 父模块构建配置
```

## 模块说明

### 1. blink-job-api

**核心 API 定义模块**，定义了任务调度的统一抽象接口。

```
com.blink.job.api/
├── annotation/
│   └── BlinkScheduled.java    # 任务注解，类似 @Scheduled
├── enums/
│   ├── JobStatus.java         # 任务状态枚举
│   ├── JobType.java           # 任务类型枚举
│   └── AlarmType.java         # 告警类型枚举
├── dto/
│   ├── JobInfo.java           # 任务信息 DTO
│   ├── JobLog.java            # 任务日志 DTO
│   ├── JobContext.java        # 执行上下文
│   └── JobExecutionResult.java # 执行结果
├── job/
│   ├── BlinkJob.java          # 任务接口（接口驱动模式）
│   └── JobContext.java        # 执行上下文（传递参数）
```

**两种任务定义方式：**

1. **注解驱动**：使用 `@BlinkScheduled` 注解标记方法
2. **接口驱动**：实现 `BlinkJob` 接口

### 2. blink-job-core

**核心实现模块**，提供任务注册、执行、告警等核心功能。

```
com.blink.job.core/
├── registry/
│   ├── JobRegistry.java           # 任务注册中心接口
│   └── DefaultJobRegistry.java    # 默认注册实现（内存存储）
├── executor/
│   └── JobExecutor.java           # 任务执行器（含重试机制）
├── processor/
│   └── JobAnnotationProcessor.java # 注解扫描处理器
├── alarm/
│   ├── JobAlarmHandler.java       # 告警处理器接口
│   └── LogAlarmHandler.java       # 日志告警实现
├── config/
│   ├── JobProperties.java         # 配置属性类
│   └── JobCoreAutoConfiguration.java # 自动配置类
```

**核心流程：**

```
任务定义 → 注解扫描/接口注册 → JobRegistry → JobExecutor → 执行回调 → 告警处理
```

### 3. blink-job-spring-starter

**Spring 原生调度实现**，基于 Spring 的 `@Scheduled` 注解。

**特点：**
- 轻量级，无需额外依赖
- 单机运行，适合小型项目
- 支持 Cron、FixedRate、FixedDelay 三种调度方式

**配置示例：**

```yaml
blink:
  job:
    type: spring
    enabled: true
```

### 4. blink-job-quartz-starter

**Quartz 调度实现**，基于 Quartz 框架。

**特点：**
- 功能强大，支持持久化
- 支持集群部署
- 任务状态管理、错过触发处理
- 适合中型项目

**配置示例：**

```yaml
blink:
  job:
    type: quartz
    enabled: true
  quartz:
    jdbc-store: true
    cluster: true
```

### 5. blink-job-xxljob-starter

**XXL-Job 分布式调度实现**，对接 XXL-Job 执行器。

**特点：**
- 分布式任务调度
- 可视化管理界面
- 任务分片、故障转移
- 适合大型分布式系统

**配置示例：**

```yaml
blink:
  job:
    type: xxljob
    enabled: true
  xxl-job:
    admin-addresses: http://xxl-job-admin:8080/xxl-job-admin
    executor:
      appname: blink-executor
      port: 9999
```

## 使用方式

### 方式一：注解驱动

```java
@Service
public class MyService {

    @BlinkScheduled(cron = "0 0 1 * * ?", description = "每日凌晨1点执行")
    public void dailyTask() {
        // 任务逻辑
    }

    @BlinkScheduled(
        cron = "0/30 * * * * ?",
        description = "每30秒执行",
        timeout = 5000,
        retryCount = 3,
        retryInterval = 1000
    )
    public void frequentTask(JobContext context) {
        // 带上下文的任务
        context.getLogger().info("执行任务: {}", context.getJobName());
    }
}
```

### 方式二：接口驱动

```java
@Component
public class DataSyncJob implements BlinkJob {

    @Override
    public String getJobName() {
        return "dataSyncJob";
    }

    @Override
    public String getJobGroup() {
        return "sync";
    }

    @Override
    public String getCronExpression() {
        return "0 0 2 * * ?";
    }

    @Override
    public JobExecutionResult execute(JobContext context) {
        try {
            // 同步逻辑
            syncData();
            return JobExecutionResult.success("同步完成，处理 100 条数据");
        } catch (Exception e) {
            return JobExecutionResult.failure(e.getMessage(), e);
        }
    }
}
```

## 数据库设计

任务持久化表结构：

### sys_job（任务定义表）

| 字段 | 类型 | 说明 |
|------|------|------|
| job_id | BIGINT | 任务 ID（主键） |
| job_name | VARCHAR(64) | 任务名称 |
| job_group | VARCHAR(64) | 任务分组 |
| cron_expression | VARCHAR(64) | Cron 表达式 |
| job_status | TINYINT | 状态：0-暂停，1-正常 |
| job_type | TINYINT | 类型：1-注解，2-接口 |
| target_bean | VARCHAR(128) | 目标 Bean 名称 |
| target_method | VARCHAR(64) | 目标方法名 |
| timeout | BIGINT | 超时时间（毫秒），-1 不超时 |
| retry_count | INT | 重试次数 |
| retry_interval | BIGINT | 重试间隔（毫秒） |

### sys_job_log（执行日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| log_id | BIGINT | 日志 ID（主键） |
| job_id | BIGINT | 任务 ID |
| trigger_time | DATETIME | 触发时间 |
| finish_time | DATETIME | 完成时间 |
| duration | BIGINT | 执行耗时（毫秒） |
| status | TINYINT | 状态：0-执行中，1-成功，2-失败 |
| result_message | TEXT | 执行结果消息 |
| error_message | TEXT | 错误信息 |

## 前端管理界面

任务管理页面路径：`/system/job`

**功能：**
- 任务列表查询（分页、筛选）
- 新增/编辑任务
- 暂停/恢复任务
- 手动触发执行
- 执行日志查看
- 批量删除

**技术栈：**
- Vue 3 + TypeScript
- Element Plus UI 组件库
- Vue Router 路由管理
- Axios HTTP 请求

## 配置属性

```yaml
blink:
  job:
    # 调度类型：spring / quartz / xxljob
    type: spring
    # 是否启用
    enabled: true
    # 日志保留天数
    log-retention-days: 30
    # 任务执行线程池大小
    thread-pool-size: 10
    # 是否记录执行日志
    record-log: true
    # 告警类型：log / email / webhook
    alarm-type: log
```

## 执行器特性

### 重试机制

任务执行失败时自动重试：

```java
@BlinkScheduled(
    cron = "0 0 1 * * ?",
    retryCount = 3,        // 重试次数
    retryInterval = 1000   // 重试间隔（毫秒）
)
public void retryableTask() {
    // 失败后自动重试 3 次，每次间隔 1 秒
}
```

### 超时控制

任务超时自动中断：

```java
@BlinkScheduled(
    cron = "0 0 1 * * ?",
    timeout = 30000  // 超时时间 30 秒
)
public void timeoutTask() {
    // 超过 30 秒自动中断
}
```

### 告警处理

支持多种告警方式：

- **LogAlarmHandler**：记录告警日志
- **EmailAlarmHandler**：发送邮件告警（需实现）
- **WebhookAlarmHandler**：HTTP 回调告警（需实现）

## 扩展开发

### 自定义告警处理器

```java
@Component
public class CustomAlarmHandler implements JobAlarmHandler {

    @Override
    public AlarmType getAlarmType() {
        return AlarmType.WEBHOOK;
    }

    @Override
    public void handleAlarm(JobInfo jobInfo, JobLog jobLog, Throwable error) {
        // 自定义告警逻辑
        sendToWebhook(jobInfo, jobLog, error);
    }
}
```

### 自定义任务注册中心

```java
@Component
public class DatabaseJobRegistry implements JobRegistry {

    @Override
    public void register(JobInfo jobInfo) {
        // 存储到数据库
    }

    @Override
    public JobInfo getJob(String jobName, String jobGroup) {
        // 从数据库查询
    }
}
```

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/job/getJobList` | POST | 查询任务列表 |
| `/job/addJob` | POST | 新增任务 |
| `/job/updateJob` | POST | 更新任务 |
| `/job/deleteJob` | POST | 删除任务 |
| `/job/pauseJob` | POST | 暂停任务 |
| `/job/resumeJob` | POST | 恢复任务 |
| `/job/triggerJob` | POST | 手动触发 |
| `/job/getLogList` | POST | 查询日志 |

## 依赖关系

```
blink-job-api (基础 API)
    ↓
blink-job-core (核心实现)
    ↓
┌─────────────────────────────────────┐
│  blink-job-spring-starter           │
│  blink-job-quartz-starter           │
│  blink-job-xxljob-starter           │
└─────────────────────────────────────┘
    ↓
blink-base-app (持久化与管理)
    ↓
frontend (前端管理界面)
```

## 版本要求

| 组件 | 版本 |
|------|------|
| JDK | 17+ |
| Spring Boot | 3.2+ |
| MyBatis-Plus | 3.5+ |
| Quartz | 2.3+ (可选) |
| XXL-Job | 2.4+ (可选) |
| Vue | 3.x |
| Element Plus | 2.x |

## 快速开始

1. **添加依赖**

```groovy
// build.gradle
implementation 'com.blink:blink-job-spring-starter:1.0.0-SNAPSHOT'
// 或选择其他实现
implementation 'com.blink:blink-job-quartz-starter:1.0.0-SNAPSHOT'
implementation 'com.blink:blink-job-xxljob-starter:1.0.0-SNAPSHOT'
```

2. **配置应用**

```yaml
blink:
  job:
    type: spring
    enabled: true
```

3. **定义任务**

```java
@Service
public class MyTask {

    @BlinkScheduled(cron = "0 0 1 * * ?", description = "每日任务")
    public void dailyJob() {
        log.info("执行每日任务");
    }
}
```

4. **启动应用**

任务将自动注册并按 Cron 表达式调度执行。

---

**作者**: binblink  
**版本**: 1.0.0-SNAPSHOT  
**更新日期**: 2026-04-16