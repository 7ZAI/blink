Spring Boot Actuator 提供了一系列监控和管理生产级Spring Boot应用的端点（endpoints），让你能深入了解应用的运行状况。以下表格汇总了常见的Actuator端点及其主要功能：


例子 http://localhost:8002/actuator/health

在网关中 路由
actuator 提供的端点
/actuator/gateway/routes	GET	查看当前所有路由定义的详细信息。	返回列表包含通过配置文件和API添加的所有路由。
/actuator/gateway/routes/{id}	GET	查看指定ID的单个路由信息。
/actuator/gateway/routes/{id}	POST	添加一个新的路由或替换现有路由。	请求体为JSON格式的路由定义（RouteDefinition）。
/actuator/gateway/routes/{id}	DELETE	删除一个已存在的路由。
/actuator/gateway/refresh	POST	刷新路由缓存，使新增或删除的路由立即生效。

| 端点 ID | 描述 | 默认HTTP暴露 |
| :--- | :--- | :--- |
| `auditevents` | 显示当前应用程序的**审计事件**信息 | 否 |
| `beans` | 显示应用程序中所有 **Spring Bean** 的完整列表 | 否 |
| `caches` | 提供对可用**缓存**的管理 | 否 |
| `conditions` | 显示在配置和自动配置类上评估的**条件及匹配情况** | 否 |
| `configprops` | 显示所有 **`@ConfigurationProperties`** 的对照列表 | 否 |
| `env` | 从Spring的 **`ConfigurableEnvironment`** 中**暴露属性** | 否 |
| `flyway` | 显示已应用的 **Flyway 数据库迁移** | 否 |
| `health` | 显示应用程序**健康信息** | **是** |
| `httptrace` | 显示 **HTTP 跟踪信息** (默认最后100个请求) | 否 |
| `info` | 显示**任意的应用程序信息** | **是** |
| `integrationgraph` | 显示 **Spring 集成图** | 否 |
| `loggers` | **显示和修改**应用程序中**记录器的配置** | 否 |
| `liquibase` | 显示已应用的 **Liquibase 数据库迁移** | 否 |
| `metrics` | 显示当前应用程序的**指标信息** | 否 |
| `mappings` | 显示所有 **`@RequestMapping` 路径**的对照列表 | 否 |
| `quartz` | 显示有关 **Quartz 调度程序作业**的信息 | 否 |
| `scheduledtasks` | 显示应用程序中的**计划任务** | 否 |
| `sessions` | 允许从 **Spring Session** 支持的会话存储中**检索和删除用户会话** | 否 |
| `shutdown` | 让应用程序**优雅地关闭** | 否 |
| `startup` | 显示由 **`ApplicationStartup`** 收集的**启动步骤数据** | 否 |
| `threaddump` | 执行**线程转储** | 否 |

如果你的应用是 **Web 应用程序**（Spring MVC、Spring WebFlux 或 Jersey），还可以使用以下附加端点：

| 端点 ID | 描述 | 默认HTTP暴露 |
| :--- | :--- | :--- |
| `heapdump` | 返回一个 **GZip 压缩的 hprof 堆转储文件** | 否 |
| `jolokia` | 通过 **HTTP 暴露 JMX bean**（Jolokia 在类路径上时，WebFlux不可用） | 否 |
| `logfile` | 返回**日志文件的内容** | 否 |
| `prometheus` | 以 **Prometheus** 服务器可以抓取的**格式暴露指标** | 否 |

### ⚙️ 端点配置与管理

要使用这些端点，你需要在项目的 `pom.xml` 中引入 Actuator 依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

默认情况下，大多数端点已启用，但需要通过 **HTTP** 或 **JMX** 暴露才能远程访问。

- **启用或禁用端点**：你可以通过配置 `management.endpoint.<id>.enabled` 来针对特定端点进行设置。例如，要启用 `shutdown` 端点：
  ```yaml
  management:
    endpoint:
      shutdown:
        enabled: true
  ```

- **暴露端点**（以 HTTP 为例）：在 Spring Boot 2.x 及以后版本中，**默认只有 `health` 和 `info` 端点通过 HTTP 暴露**。要暴露更多端点，需要使用 `management.endpoints.web.exposure.include` 和 `exclude` 属性。例如，以下配置暴露了所有端点（`health` 和 `info` 之外的那些端点通常也需要暴露才能访问）：
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: "*"  # 暴露所有端点
          # exclude: env,beans  # 排除特定端点
  ```

### 🔒 安全提醒

**请注意**，许多端点可能包含敏感信息。在生产环境中，**务必确保对端点进行安全保护**，例如通过 Spring Security 进行访问控制，并谨慎选择要暴露的端点。

### 💎 主要端点功能小结

- **应用配置类**：`beans`, `conditions`, `configprops`, `mappings` 等端点帮助你深入了解应用的配置情况。
- **度量指标类**：`metrics`, `health`, `httptrace` 等端点用于监控应用运行状态和性能。
- **操作控制类**：`loggers`（动态修改日志级别），`shutdown`（优雅关闭）等端点允许你动态调整应用行为（请谨慎使用并确保有安全措施）。

希望这些信息能帮助你更好地使用 Spring Boot Actuator！如果你对特定端点的使用或配置有更多疑问，我很乐意提供进一步的信息。