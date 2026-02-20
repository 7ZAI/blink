# Blink 响应式网关

## 目录
- [✨ 简介](#-简介 )
- [🚀 快速开始](#-快速开始)
- [💡 功能介绍](#-功能介绍)
  - [过滤链](#过滤链)
  - [路由转发](#路由转发)
    - [动态路由](#动态路由) 
  - [认证与权限](#认证与权限)
  - [多渠道对接管理](#多渠道对接管理)
  - [报文组装](#报文组装)
  - [全局异常处理](#全局异常处理)
  - [负载均衡](#负载均衡) 
  - [配置参数](#配置参数)
  - [缓存](#缓存)
  - [监控](#监控)
- [🧪 测试](#-测试)



## 简介 

Blink响应式网关基于spring cloud gateway 实现的API网关。旨在淘汰同步网关，拥抱响应式的优点，提高资源利用率，提高吞吐量。

目标：打造适合中小团队、能够快速落地的轻量级API网关

该项目为个人对响应式编程和WebFlux学习实践的项目 持续更新

## 快速开始
 ### 环境准备
 Idea、Gradle 8.8、JDK 17、Redis 7.2.3、Nacos 2.3.0

### 步骤

1、 下载blink项目

2、根据自己的环境在blink根目录下build.gradle中修改仓库地址，如果没有私库改为本地仓库。

3、构建blink-framework-common、blink-redis-spring-boot-starter、blink-base-api等依赖包，并publish到私库或者本地maven库

4、修改配置文件，配置Redis、Nacos,然后运行BlinkReactiveGatewayApplication


## 功能介绍

### 过滤链

gateway的核心机制或者说本质，其实就是一条过滤链（Filter）。那么过滤链上过滤器的执行顺序，就显得十分重要了。

blink-gateway的过滤链顺序如下：

```java
   /**
     * 网关过滤链顺序：
     * 
     * 请求进入
     *   ↓
     * 日志 (LogFilter)
     *   ↓
     * IP过滤检查 (IpFilter)
     *   ↓
     * 请求头合法性验证 (RequestHeaderValidationFilter)
     *   ↓
     * Security认证鉴权
     *   ↓
     * 签名验证 (SignatureFilter)
     *   ↓
     * 防重放攻击 (ReplayAttackPreventionFilter)
     *   ↓
     * 加密解密 (CryptFilter)
     *   ↓
     * 元数据填充 (RewriteRequestBodyFilter)
     *   ↓
     * 转发到下游服务
     */

```

#### 合法性校验
    
对http请求进行合法性校验，校验内容包括：ip黑白名单，请求方法类型校验，必填请求头校验，请求头和请求体数据长度校验，渠道合法性检验，数据签名验证，防止请求重放校验
其中 ip、签名、请求重放可以设置参数配置开启或关闭，其余为必校验项；其他的一些校验边界也做了参数化配置；具体参数详情[配置参数](#配置参数)

请求方法类型校验：目前仅支持 method:POST Content-Type: application/json 的请求  后续会支持文件上传(TODO)；blink gateway设计是禁止GET请求的

必填请求头校验: 

| 请求头               | 描述                 | 必填             | 
|-------------------|--------------------|----------------|
| x-blink-appKey    | 由系统方发放的appKey，渠道凭证 | 必填             | 
| x-blink-token    | 登入token            | 必填 (登入请求时非必填)  |
| x-blink-nonce     | 调用方设置的UUID唯一值      | 必填             |
| x-blink-timestamp | 时间戳 long格式         | 必填             |
| x-blink-sign      | 签名                 | 必填             |
| x-blink-iv        | AES加密随机值           | 在渠道开启加密时必填 关闭非必填 |
| x-blink-key       | AES加密密钥（经过RSA加密后）  | 在渠道开启加密时必填 关闭非必填 |

其中x-blink-nonce、x-blink-timestamp、x-blink-sign虽然网关可以关闭相关检验功能，但是仍然设计为必填，这是为了让调用方兼容网关，方便网关动态开关功能


### 路由转发
    
spring cloud gateway 已经提供了一套路由配置规则 可以选择代码配置和配置文件配置的两种方式

其中提供了多种路由匹配predicates规则 如路径(Path)匹配、方法(Method)匹配、Header匹配、Cookie匹配、Query参数匹配、时间匹配、Host匹配等

路由配置示例： 注意服务发现的服务名对应的是uri 而不是路由id

```yml
spring:
  cloud:
    gateway:
      # 全局配置
      default-filters:
        - AddRequestHeader=X-Forwarded-From-Gateway, true
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin

      # 路由配置
      routes:
        - id: user-service
          # 微服务路由（服务发现）
          uri: lb://user-service
          predicates:
            - Path=/users/**
            - Method=GET,POST,PUT,DELETE
          filters:
            - StripPrefix=1 # 常用 按配置数截取url 如果api为 /api/v1/v2 配置为2 则转发路由为uri/v2
            - name: CircuitBreaker
              args:
                name: userService
                fallbackUri: forward:/fallback/user

        # API版本路由
        - id: api-v2
          uri: lb://api-service-v2
          predicates:
            - Path=/api/v2/**
            - Header=X-API-Version, 2.x
          filters:
            - RewritePath=/api/v2/(?<segment>.*), /$\{segment}

        # 静态资源路由
        - id: static-resources
          uri: file:/opt/resources/static
          predicates:
            - Path=/static/**

        # WebSocket路由
        - id: websocket-route
          uri: lb://ws-service
          predicates:
            - Path=/ws/**

        # 重定向路由
        - id: redirect-route
          uri: no://op
          predicates:
            - Path=/old/**
          filters:
            - RedirectTo=302, https://new-domain.com

        # 限流路由
        - id: rate-limit-route
          uri: http://service
          predicates:
            - Path=/public/api/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@ipKeyResolver}"
      

```
    
#### 动态路由

动态路由，即可以修改网关路由配置，并即时生效。

 blink网关提供动态路由两种实现方式：基于Nacos 配置文件实现和基于 Redis存储和 Redis stream消息实现。
 两种方式通过配置文件开启， 只能二择其一；均支持同组别网关实例，统一更新。

#### Nacos实现动态路由

 通过监听Nacos配置中心配置文件的改动，传递路由刷新事件，利用spring cloud gateway框架本身的路由刷新事件刷新路由 
 配置项如下

```yaml
blink:
  gateway:
    dynamicroute:
      mode: nacos
      nacos:
        dataId: gateway-routes
        group: DEFAULT_GROUP
```

#### Redis实现动态路由

redis实现的动态路由也是通过传递刷新事件来实现路由变化的实时生效，不同于Nacos的实现，路由仓库为redis缓存，路由修改事件
通过监听redis stream 消息来获取，而修改事件的触发，是调用base-app服务提供的管理接口触发。

最终流程为通过 调用base-app后台管理接口修改路由----> ---->base服务发送修改事件消息到Redis stream---->网关端获取事件消息---->刷新路由

启用Redis 动态路由配置 其中routeSuffix为redis保存路由信息的key后缀 通过它可以划分不同的gateway集群
```yaml
blink:
  gateway:
    dynamicroute:
      mode: redis
      redis:
        routeSuffix: default
```

 ### 认证与鉴权

blink gateway整合了spring security 来实现集中式登录认证和鉴权。


blink gateway实现了针对后台管理base-app服务的认证鉴权和对接入渠道的认证和鉴权

base-app服务认证鉴权： 采用登入名密码进行认证，通过认证签发带状态token 基于RBAC实现鉴权

token认证: 登入后通过UUID生成一个唯一Id作为用户token凭证，将token存在redis中，并设置30分钟的过期时间。用户每个请求都会拦截获取token，与redis的做比对，token一致则请求通过，否则拒绝。
自动续期：如果用户过期时间仍然剩5分钟时，仍然活跃 则将token过期时间延长30分钟.
这种方案是适用于管理后台管理系统的用户状态，方便实现 踢人 强制下线等功能。

相关类：[TokenAuthenticationConverter](src/main/java/com/blink/gateway/security/token/TokenAuthenticationConverter.java)、
[TokenAuthenticationManager](src/main/java/com/blink/gateway/security/token/TokenAuthenticationManager.java)、[BlinkAuthenticationSuccessHandler](src/main/java/com/blink/gateway/security/token/TokenAuthenticationSuccessHandler.java)


渠道（channel）的认证和鉴权: 采用类似oauth2 client_credentials的模式，渠道使用appkey、appSecret获取短期jwt，基于jwt进行认证
                          通过给渠道绑定用户、实现RBAC鉴权          

相关类：[JwtAuthenticationConverter](src/main/java/com/blink/gateway/security/jwt/JwtAuthenticationConverter.java)、
[JwtAuthenticationManager](src/main/java/com/blink/gateway/security/jwt/JwtAuthenticationManager.java)、[JwtAuthenticationSuccessHandler](src/main/java/com/blink/gateway/security/jwt/JwtAuthenticationSuccessHandler.java)



权限校验：是对RBAC权限管理模型的实现。通过redis缓存获取url对应的权限标识，校验用户登入成功时获取的权限集合，是否具有该标识，有则通过 无则拒绝。
        内部用户和渠道统一鉴权逻辑

 相关类：[BlinkAuthorizationManager](src/main/java/com/blink/gateway/security/BlinkAuthorizationManager.java)




 ### 多渠道对接管理

引入渠道管理机制，在base-app服务提供渠道curd接口，用于管理调用blink系统中api的第三方。为接入方创建一个渠道，包含appKey,appSecret、渠道RSA密钥对 系统RSA密钥对、功能开关等信息。
其中涉及相关密钥做了不入库处理，而是使用存于环境变量的密钥，对生成的所有密钥进行加密，并保存在nacos配置中，生产上不对密钥进行日志记录 
在gateway端一样从nacos获取配置，解密后缓存使用。

在网关通过appKey拿到redis中缓存的渠道（channel）信息，然后校验渠道开关，动态控制第三方能否接入、是否加解密、是否进行认证等等。

因为这是服务方于服务方之间的调用 所有第三方接入时相关的渠道信息由私下沟通告知，不在系统上动态发送渠道信息，包括是否加密传输需要沟通确定，而不是动态设置


#### 混合加解密 加签验签

   通过AES+RSA混合加密对请求体json字符串进行解密 ，响应体加密； 及SHA-256数字签名技术，确保第三方接口数据传输的机密性、完整性和不可否认性，提升系统安全防护等级。

    
//TODO 后期对加密解密行为进行抽象，方便更换加密解密的实现

 ### 报文组装

  因为blink系统设计了统一的请求DTO的格式，所以要在网关端集中填充；包括请求id,追踪id,日期 token 登入用户名等等 json格式如下：
  
```json
{
    "requestId": "45",
    "traceId": "10",
    "version": "v1",
    "spanId": "3",
    "parentSpanId": "56",
    "reqDate": "1999-09-15",
    "startDateTime": "2016-02-11 23:02:26",
    "endDateTime": "1982-01-10 07:12:01",
    "userId": "78",
    "clientIp": "89.153.145.25",
    "source": "consequat ut nostrud",
    "channel": "nisi sed",
    "uri": "pariatur in",
    "timeout": 177693,
    "token": "amet",
    "loginName": "藤宇轩",
    "extensions": "扩展字段",
    "body": { "DTO": "实际业务数据" }
       
}

```

链路追踪：生成分布式id 组装进报文


相关类：[RewriteRequestBodyFilter.java](src/main/java/com/blink/gateway/filter/RewriteRequestBodyFilter.java)

 ### 全局异常处理

 全局异常处理 这里只针对在网关产生的异常进行处理 其他服务产生的异常由服务自身处理
 错误信息组装所有的错误都应该设置msg为错误码的形式，然后由全局异常处理统一根据错误码设置具体的错误信息。
 在blink框架中 采用HTTP 200 + 业务错误码的方案 业务上的错误码和http的状态码是分开设置，即业务上抛出错误，http状态码也是200.

只有在系统上的产生的错误和http状态码 语义上统一时，才会设置http对应的错误码;如未知异常 返回500 未授权返回403等等

这里的错误信息是根据请求头local获取对应的语言，以错误码+语言为key 去缓存获取信息，以实现错误提示语国际化

相关类:[GlobalExceptionHandlerFilter.java](src/main/java/com/blink/gateway/filter/GlobalExceptionHandlerFilter.java)


### 负载均衡

目前引入LoadBalancer依赖 按默认配置即按轮询的方式进行负载均衡。通过路由配置lb://服务名 前缀来启动负载均衡

 ### 配置参数



对gateway的一些系统参数进行动态修改，实时生效。这些参数一般为：请求报文大小限制、本地缓存开关、防重放开关、有效时间、ip黑白名单等等
 目前有两种方案 一、Reids stream 消息进行本地缓存同步 好处是可以中后台系统搭建页面进行可视化管理
              二、Nacos 监听参数文件的 好处是实现简单
 虽然技术上stream的方案更有挑战，但是实际场景中 gateway的配置参数不会经常变动 所以从实际触发采用Nacos配置文件的方案
 当前Reids stream已经走通 未完善、关于gateway的配置参数文件也未设置



  #### 参数列表
| 配置名                                     | 描述   | 默认值   |
|-----------------------------------------|----|-------|
| signture_enable                         | 报文签名开关	         | true  |
| srequest_replay_defend_enable           | 防止请求重放开关       | false |
| request_replay_defend_effect_time       | 请求有效时间          | 6000  |
| request_replay_defend_nonce_expire_time | 请求随机值过期时间分钟(毫秒) | 6000  |
| api_disable_switch                      | 数据校验支持          | false |
| api_disable_list                        | 临时下线api集合       | {}    |
| ip_filter_enable                        | ip 过滤开关         | false |
| white_list_ips                          | 白名单ip地址集合       | {}    |
| black_list_ips                          | 黑名单ip地址集合       | {}    |


### 缓存

为了减少gateway与redis、其他内部服务的调用，减少网络开销；设计实现了依次从本地缓存、redis、远程服务数据源获取数据的多级缓存组件。

使用Caffeine本地缓存、缓存网关固定需要获取的数据、如上面的参数配置。

设计了多级缓存组件 使用Caffeine cache 异步缓存 并用工具类将其转换为响应式方式调用

同步机制：目前使用 redis stream 实现类似消息队列的功能 通过发送同步消息事件本地缓存的同步

 ### 流量控制

限流：适用了spring cloud gateway原生的基于Redis令牌通限流 

熔断：


使用spring cloud gateway自带的RedisRateLimiter 木桶令牌算法进行限流，在blink网关只进行粗粒度的限流，更细粒度的限流根据业务场景，由各个业务自己来实现
TODO 


 ### 灰度发布 
   通过动态路由已经可以实现一定程度的灰度发布。更细致的关于灰度发布的相关功能 暂时搁置 TODO

 **TODO**

 ### 监控

 **TODO**

### 其他功能

 #### ip黑白名单
   
可以设置ip 白名单和黑名单，白名单黑名单同时开启时 优先校验白名单   

支持设置ipv4和ipv6 

支持网段设置

详情[IpFilter](src/main/java/com/blink/gateway/security/filter/IpFilter.java)    
 #### 临时功能下线
TODO

## 测试

    
 apifox测试脚本
前置脚本[preHandle](apifoxScript/preHandle.js)
 后置脚本[afterHandle](apifoxScript/afterHandle.js)
