# Blink 微服务开发框架

该项目目的提供一套可以快速开发微服务的框架，并作为个人工作经验技术的总结和提炼。

**目标：向企业级开发框架靠拢**

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | JDK、Spring Boot、Spring Cloud Alibaba | 17+、3.2.7、2023.0.3.2 |
| 数据存储 | MySQL、Redis | 8.0+、7.0+ |
| 消息队列 | RabbitMQ | 3.12+ |
| 微服务 | Nacos（注册中心/配置中心）、Dubbo（RPC框架） | 2.3+、3.3.0（Triple协议） |
| ORM框架 | MyBatis-Plus | 3.5.16 |
| 工具库 | Hutool | 5.8.29 |
| 前端技术 | Vue 3、TypeScript、Vite、Element Plus | 3.4+、5.0+、2.6+ |
| 构建工具 | Gradle | 8.8+ |

具体版本详情查看 [build.gradle](build.gradle) 及各模块 build.gradle

## 核心特性

- **模块化设计**：按功能维度模块化拆分，实现功能依赖可插拔引用
- **Starter封装**：类似Spring Boot Starter，内置自动化配置能力
- **响应式网关**：基于Spring Cloud Gateway的非阻塞响应式网关
- **网关运维平台**：独立的网关管理后台，支持渠道/路由/配置的动态管理
- **Dubbo RPC**：支持Triple协议的Dubbo服务调用，实现同步Provider与响应式Consumer的无缝对接
- **多渠道接入**：网关支持多渠道对接，包含签名验证、报文加解密等功能
- **操作日志**：通过注解自动记录操作日志，支持敏感数据脱敏
- **代码生成**：内置代码生成器，快速生成CRUD代码
- **配置热更新**：通过Nacos实现配置的动态推送和热更新

## 模块封装

采用企业级工程划分：按功能维度模块化拆分策略，将各核心能力封装为独立可发布的依赖库（类似 Spring Boot Starter）：
- 实现模块间依赖隔离与单一功能聚合，实现功能依赖可插拔引用，降低模块耦合性
- Starter 内置自动化配置能力，外部引用时可通过自定义配置灵活覆盖依赖库默认参数
- 后续功能迭代或重大调整可通过发布新版本依赖包支撑持续交付，最终使工程架构达成高内聚、低耦合的管理目标

### 基础模块 (Starter)

| 模块名称 | 功能 | 说明 |
|---------|------|------|
| [blink-framework-common](blink-framework-common/README.md) | 通用类封装 | 工具类、常量、基础DTO等 |
| [blink-datasource-starter](blink-datasource-starter/README.md) | MySQL数据库支持 | 数据源配置、MyBatis-Plus、代码生成器 |
| [blink-redis-starter](blink-redis-starter/README.md) | Redis客户端支持 | 缓存操作、分布式ID生成 |
| [blink-framework-validation](blink-framework-validation/README.md) | 数据校验支持 | 自定义校验注解 |
| [blink-framework-mq](blink-framework-mq/README.md) | RabbitMQ支持 | 消息队列封装 |
| [blink-framework-openfeign](blink-framework-openfeign/README.md) | RPC调用封装 | OpenFeign配置（同步服务调用） |
| [blink-web-starter](blink-web-starter/README.md) | WebApp通用功能 | 全局异常处理、日志、缓存预热等 |
| [blink-log-starter](blink-log-starter/README.md) | 日志组件 | 操作日志入库、敏感数据脱敏、Controller日志 |

### 应用模块

| 模块名称 | 功能 | 说明 |
|---------|------|------|
| **blink-base** | RBAC后台管理服务 | 用户、角色、权限、菜单、字典管理等 |
| ├─ [blink-base-app](blink-base/blink-base-app/README.md) | 后端实现 | Spring Boot 服务实现 |
| ├─ [blink-base-api-dubbo](blink-base/blink-base-api-dubbo/README.md) | Dubbo接口定义 | BaseDubboService接口及DTO |
| ├─ [blink-base-api](blink-base/blink-base-api/README.md) | Feign接口定义 | OpenFeign接口定义（同步调用） |
| └─ [blink-base-web](blink-base/blink-base-web/README.md) | 管理前端 | Vue3 + TypeScript + Element Plus |
| **blink-gateway** | 网关服务集群 | API网关与运维管理 |
| ├─ [blink-gateway-reactive](blink-gateway/blink-gateway-reactive/README.md) | 响应式网关 | 路由转发、认证鉴权、加解密、限流 |
| ├─ [gateway-admin](blink-gateway/gateway-admin/README.md) | 网关运维平台 | 渠道/路由/配置管理、监控运维 |
| ├─ [gateway-admin-web](blink-gateway/gateway-admin-web/README.md) | 网关运维前端 | Vue3 + TypeScript + Element Plus |
| └─ [blink-gateway-admin-api-dubbo](blink-gateway/blink-gateway-admin-api-dubbo/README.md) | Dubbo接口定义 | GatewayAdminDubboService |

各个模块的具体功能详情，请查看各个模块的README文档

## Dubbo服务架构

本项目采用Dubbo 3.3作为RPC框架，支持Triple协议。

### 服务提供者（Provider）

`blink-base-app` 作为服务提供者，实现 `BaseDubboService` 接口：

```java
@DubboService(async = true, timeout = 10000)
public class BaseDubboServiceImpl implements BaseDubboService {
    // 实现方法...
}
```

### 服务消费者（Consumer）

`blink-gateway-reactive` 作为响应式消费者，通过异步调用实现响应式返回：

```java
public class BaseAppDubboService {
    private final BaseDubboService baseDubboService;
    
    public Mono<SomeResult> someMethod() {
        CompletableFuture<SomeResult> future = CompletableFuture.supplyAsync(
            () -> baseDubboService.someMethod()
        );
        return Mono.fromFuture(future);
    }
}
```

### 配置示例

```yaml
dubbo:
  application:
    name: base-app
  registry:
    address: nacos://localhost:8848
  protocol:
    name: tri
    port: 20880
```

## 快速构建

### 1. 环境准备

| 工具 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Gradle | 8.8+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |
| Nacos | 2.3+ |
| Nexus（可选） | 3.x |

下载项目后，可以在 [build.gradle](build.gradle) 修改私库配置，gradle执行publish任务打包发布各模块到私库。如果没有私库，可以配置为本地仓库。

### 2. 添加依赖

使用IDEA创建新工程，或者下载本项目后在根目录上添加新模块，编写gradle构建脚本引入依赖：

```groovy
dependencies {
    implementation 'com.blink:blink-web-spring-boot-starter:1.0.0-SNAPSHOT'
    implementation 'com.blink:blink-redis-spring-boot-starter:1.0.0-SNAPSHOT'
    implementation 'com.blink:blink-dataSource-spring-boot-starter:1.0.0-SNAPSHOT'
    implementation 'com.blink:blink-log-spring-boot-starter:1.0.0-SNAPSHOT'

    implementation 'com.blink:blink-framework-common:1.0.0-SNAPSHOT'
    implementation 'com.blink:blink-framework-validation:1.0.0-SNAPSHOT'
}
```

参考 `blink-base-app` 或 `blink-gateway-reactive` 的配置文件，配置 Redis、Nacos、MySQL 等中间件连接信息。

### 3. 生成模板代码

使用类 [CodeGenerator](blink-datasource-starter/src/main/java/com/blink/datasource/code/CodeGenerator.java) 生成代码：

```java
public static void main(String[] args) {
    String url = "jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";
    String username = "root";
    String password = "123456";
    
    var codeGenerator = new CodeGenerator();
    codeGenerator.generateByCustomTemplate(url, username, password);
}
```

运行后按输入数据表名可根据数据表信息自动生成：
- Controller、Service、ServiceImpl、Mapper、mapper.xml、DO实体
- CRUD相关的DTO

生成成功控制台显示：

```text
请输入作者名称！
blink
请输入应用包名（已有前缀com.blink）！

请输入表名，多个英文逗号分隔 生成所有表则输入all ！
sys_user
请输入要过滤的表前缀，多个英文逗号分隔 都不过滤输入none字符串！
none
请选择模板类型输入1或者2（1.DTO 2.record）
1
18:21:57.113 [main] DEBUG com.baomidou.mybatisplus.generator.AutoGenerator -- ==========================文件生成完成！！！==========================
```

默认生成地址在项目根目录中，随后将生成的代码拖入项目对应目录即可。

### 4. 启动服务

#### 启动 blink-base-app

```java
@SpringBootApplication
@EnableDiscoveryClient
public class BlinkBaseAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlinkBaseAppApplication.class, args);
    }
}
```

运行启动类，到此微服务应用构建完毕，可以进行业务开发了。

### 5. 启动前端项目

#### blink-base-web（RBAC管理前端）

```bash
cd blink-base/blink-base-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问地址：http://localhost:5173

#### gateway-admin-web（网关运维前端）

```bash
cd blink-gateway/gateway-admin-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问地址：http://localhost:5174

## 应用服务

### blink-base

RBAC后台管理服务，具有用户、角色、权限、菜单、字典、操作日志等CRUD接口，是企业中必不可少的应用。除了RBAC外还提供：
- 系统参数管理
- 外接渠道管理
- 数据字典管理
- 错误消息管理
- 操作日志记录

**模块结构：**
- `blink-base-app`: 后端服务实现
- `blink-base-api-dubbo`: Dubbo服务接口定义
- `blink-base-api`: Feign接口定义（同步调用）
- `blink-base-web`: 前端管理界面

详细文档：[blink-base-app](blink-base/blink-base-app/README.md) | [blink-base-web](blink-base/blink-base-web/README.md)

### blink-gateway

网关服务集群，包含响应式API网关和运维管理平台。

#### blink-gateway-reactive

基于Spring Cloud Gateway实现的响应式非阻塞网关：
- 请求路由转发（支持动态路由）
- 集中认证鉴权（JWT/Token）
- 多渠道接入管理
- 报文加密解密（AES+RSA混合加密）
- 签名验证（SHA-256）
- 限流熔断（Redis令牌桶）
- Dubbo服务响应式调用
- 防重放攻击保护

详细文档：[blink-gateway-reactive](blink-gateway/blink-gateway-reactive/README.md)

#### gateway-admin

网关运维管理平台，提供渠道管理、路由管理、配置管理、监控运维等核心功能：
- **渠道管理**：外部渠道接入、密钥管理、Token签发
- **路由管理**：动态路由配置、路由刷新
- **配置管理**：系统参数配置、IP黑白名单
- **配置推送**：Nacos配置推送、版本管理、配置回滚
- **数据同步**：配置数据同步到网关实例
- **实例管理**：网关实例上下线、健康监控
- **监控中心**：实例状态、统计数据、健康检查

通过 Dubbo RPC 与网关实例通信，实现配置的动态推送和实时监控。

详细文档：[gateway-admin](blink-gateway/gateway-admin/README.md)

#### gateway-admin-web

网关运维管理前端项目，基于 Vue 3 + TypeScript + Vite 构建：
- **技术栈**：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router
- **功能模块**：
  - 仪表盘：网关概览、实例状态
  - 渠道管理：渠道CRUD、密钥管理、Token签发
  - 路由管理：动态路由配置
  - 配置管理：网关参数配置、IP黑白名单
  - 监控中心：实例列表、健康状态

详细文档：[gateway-admin-web](blink-gateway/gateway-admin-web/README.md)

## 系统架构

### 当前环境架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端层 (Frontend)                                │
├─────────────────────────────┬───────────────────────────────────────────────┤
│    blink-base-web           │      gateway-admin-web                        │
│   (RBAC管理前端)            │     (网关运维前端)                             │
│   Vue3 + Element Plus       │     Vue3 + Element Plus                       │
└───────────┬─────────────────┴───────────────────────┬───────────────────────┘
            │                                         │
            │         HTTP / HTTPS                    │
            │         (经过网关统一入口)                │
            ▼                                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      blink-gateway-reactive (响应式网关)                       │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │LogFilter│→│IpFilter │→│Header   │→│Security │→│Sign     │→│Route    │   │
│  │         │ │         │ │Validate │→│Filter   │→│Filter   │→│Filter   │   │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
│                                                                             │
│  功能: 路由转发 / 认证鉴权 / 加解密 / 签名验证 / 限流熔断 / 防重放攻击              │
└──────────────────────┬──────────────────────────────────────────────────────┘
                       │
                       │ 路由转发
                       │
         ┌─────────────┼─────────────┐
         │             │             │
         ▼             ▼             ▼
┌────────────────┐ ┌────────────────┐ ┌────────────────┐
│ blink-base-app │ │ gateway-admin  │ │  其他业务服务   │
│ (RBAC后端服务)  │ │ (网关运维平台)  │ │                │
│ 端口: 8001      │ │ 端口: 8008      │ │                │
└────────────────┘ └────────────────┘ └────────────────┘
         │             │
         │ Dubbo RPC   │ Dubbo RPC / Nacos
         │             │
         ▼             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         基础设施层 (Infrastructure)                           │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │  Nacos  │  │  MySQL  │  │  Redis  │  │RabbitMQ │  │  Dubbo  │            │
│  │注册/配置│  │  数据   │  │  缓存   │  │  消息   │  │  RPC    │            │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘            │
└─────────────────────────────────────────────────────────────────────────────┘
```
正式生产环境 会添加一层 Nginx
### 开发环境架构 (直连模式)

开发测试环境为了方便调试，前端可直接连接后端服务：

```
┌─────────────────────────────┐         ┌─────────────────────────────┐
│    blink-base-web           │────────▶│   blink-base-app            │
│   (RBAC管理前端)            │         │   (RBAC后端服务)             │
└─────────────────────────────┘         └─────────────────────────────┘

┌─────────────────────────────┐         ┌─────────────────────────────┐
│  gateway-admin-web          │────────▶│    gateway-admin            │
│  (网关运维前端)              │         │   (网关运维平台)             │
└─────────────────────────────┘         └─────────────────────────────┘
```

## 项目规则

Vibe Coding项目开发规范请查看：[CLAUDE.md](CLAUDE.md)

---

**blink项目处于持续开发的状态，后续持续更新**

*另外求职 base深圳*
