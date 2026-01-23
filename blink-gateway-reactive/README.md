# Blink 响应式网关

## 目录
- [✨ 简介](#-简介 )
- [🚀 快速开始](#-快速开始)
- [📦 安装](#-安装)
- [💡 功能介绍](#-功能介绍)
  - [动态路由](#动态路由)
  - [认证与权限](#认证与权限)
  - [多渠道对接管理](#多渠道对接管理)
  - [报文组装](#报文组装)
  - [全局异常处理](#全局异常处理)
  - [负载均衡](#负载均衡) 
- [🧪 测试](#-测试)



## 简介 

响应式网关
  Blink 框架下的基于spring cloud gateway 实现响应式API网关。旨在淘汰同步网关，拥抱响应式的优点，提高资源利用率，提高吞吐量。


  在Blink 网关中还集成了 spring security 提供安全认证 权限拦截等；该网关具有针对多渠道（外部系统）接入管理功能，
  统一报文格式，提供RSA + AES报文混合加密。


 目前网关仅提供post请求支持，拒绝get请求和其他请求方法，因为目前是适配base-app报文的形式进行拦截的，没有通用性，后续可能将具体的校验逻辑插件化，以达到通用的目的
 
该项目为个人对WebFlux学习实践的项目

## 快速开始
 ### 环境准备
 Idea、Gradle 8.8、JDK 17、Redis 7.2.3、Nacos 2.3.0
 ### 步骤

     1、下载blink项目，包含子模块blink-gateway-reactive、blink-framework-common、blink-redis-starter
     2、根据自己的环境在blink根目录下build.gradle中修改仓库地址，如果没有私库改为本地仓库。
     3、构建blink-framework-common、blink-redis-spring-boot-starter依赖包
     4、修改配置文件，配置Redis、Nacos,然后运行BlinkReactiveGatewayApplication

## 功能介绍

### 请求合法性校验

   ip黑白名单 请求类型校验 请求头校验 渠道检验 签名验证

### 动态路由
 blink网关提供动态路由两种实现方式：基于Nacos 配置文件实现 和基于Redis存储和Redis stream消息实现。两种方式通过配置文件开启，
 只能二择其一
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
通过监听redis stream 消息来获取，而修改事件的触发，是调用base-app服务提供的管理接口触发。最终流程为通过base-app后台管理页面修改路由，
，base服务发送修改事件消息到Redis stream，网关端获取事件消息 刷新路由。
```yaml
blink:
  gateway:
    dynamicroute:
      mode: redis
      redis:
        routeSuffix: default
        groupId: route-consumer-1
        streamkey: blink:stream:gateway:route
```
 ### 认证与权限
blink gateway整合了spring security 来实现登录认证和权限校验。目前实现了base-app服务的用户名密码登录认证和权限校验。

token认证 登入后通过UUID生成一个唯一Id作为用户token凭证，将token存在redis中，并设置30分钟的过期时间。用户每个请求都会拦截获取token，与redis的做比对，token一致则请求通过，否则拒绝。
自动续期：如果用户则过期时间仍然剩10分钟时，仍然活跃 则将token过期时间延长30分钟.

这种方案是为了管理后台管理系统的用户状态，方便实现 踢人 强制下线等功能。

相关类：[TokenServerAuthenticationConverter](src/main/java/com/blink/gateway/security/TokenServerAuthenticationConverter.java)、
[BlinkAuthorizationManager](src/main/java/com/blink/gateway/security/BlinkAuthorizationManager.java) 、[BlinkAuthenticationSuccessHandler](src/main/java/com/blink/gateway/security/BlinkAuthenticationSuccessHandler.java)

权限校验：是对RBAC权限管理模型的实现。通过redis缓存获取url对应的权限标识，校验用户登入成功时获取的权限集合，是否具有该标识，有则通过 无则拒绝。
 相关类：[TokenAuthenticationManager](src/main/java/com/blink/gateway/security/TokenAuthenticationManager.java)

TODO 未来支持
如果不追求管理用户登录状态，可以采用jwt 双token的方案



 ### 多渠道对接管理

引入渠道管理机制，在base-app服务提供渠道curd接口，用于管理接入blink系统中的第三方。为接入方创建一个渠道，包含appKey,appSecret、渠道密钥对 系统密钥对功能开关等信息。
在网关通过appKey拿到redis中缓存的渠道（channel）信息，然后校验渠道开关，动态控制第三方能否接入、是否加解密、是否进行认证等等。

#### 混合加解密 加签验签

   通过AES+RSA混合加密及SHA-256数字签名技术，确保第三方接口数据传输的机密性、完整性和不可否认性，提升系统安全防护等级。

 ### 报文组装


  因为blink系统设计了统一的请求DTO的格式，所以要在网关端进行填充；包括请求id,追踪id,日期 token 登入用户名等等 json格式如下：
  
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
    "body": "实际业务数据"
       
}

```

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

 未来方案 参考开源项目 [shenyu](https://github.com/apache/shenyu) 的网关实现，在网关采用内存微型数据库搭建后台管理和监控系统一起，直接垂直管理。
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



 ### 流量控制
 使用spring cloud gateway自带的RedisRateLimiter 木桶令牌算法进行限流，在blink网关只进行粗粒度的限流，更细粒度的限流根据业务场景，由各个业务自己来实现
TODO sentinel
 ### 灰度发布 
   通过动态路由已经可以实现一定程度的灰度发布。更细致的关于灰度发布的相关功能 暂时搁置 TODO
 **TODO**
 ### 性能参数监控
    TODO

 **TODO**
### 其他功能
 #### ip黑白名单
TODO
 #### 临时功能下线
TODO

## 测试
 apifox测试脚本
前置脚本[preHandle](apifoxScript/preHandle.js)
 后置脚本[afterHandle](apifoxScript/afterHandle.js)
