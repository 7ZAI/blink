# 全局异常处理接口抽象设计

## 概述

将 `GlobalExceptionHandler` 中的错误信息查询逻辑抽象为接口，实现基础模块与具体服务的解耦，使其他服务能够优雅地配置全局异常处理。

## 问题背景

当前架构存在以下问题：

1. `GlobalExceptionHandler` 在 `blink-web-starter` 基础模块中
2. 错误信息查询依赖 `SysMsgInfoMapper`，但该 Mapper 在 `blink-base-app` 具体服务中
3. `getSupplier()` 方法中直接引用未注入的 `sysMsgInfoMapper`，存在 bug
4. 构建新服务时需要重新实现错误信息查询逻辑

## 设计决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 接口放置位置 | `blink-web-starter` | 与 `GlobalExceptionHandler` 同模块，职责内聚 |
| 错误码不存在时 | 返回 `SYS00001` | 统一兜底处理 |
| 非中文语言处理 | 返回英文 | 国际化简化处理 |
| 默认实现缓存 | 不使用缓存 | 无外部依赖，简洁可靠 |
| CacheComponent 依赖 | 从 GlobalExceptionHandler 移除 | 缓存是实现细节，封装在 Provider 内部 |

## 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│  blink-web-starter                                              │
│  ├── ErrMsgProvider (接口)                                      │
│  ├── DefaultErrMsgProvider (默认实现)                           │
│  ├── GlobalExceptionHandler (重构)                              │
│  │   └── 仅依赖 ErrMsgProvider 接口                             │
│  └── WebStarterAutoConfiguration                               │
│      └── @ConditionalOnMissingBean -> DefaultErrMsgProvider    │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ 实现接口
┌─────────────────────────────┴───────────────────────────────────┐
│  blink-base-app                                                 │
│  ├── DbErrMsgProvider implements ErrMsgProvider                 │
│  │   ├── 注入 CacheComponent + SysMsgInfoMapper                 │
│  │   └── 实现数据库查询 + 缓存逻辑                              │
│  └── @Component 注入                                            │
└─────────────────────────────────────────────────────────────────┘
```

## 组件详细设计

### 1. ErrMsgProvider 接口

```java
package com.blink.framework.core.exception;

/**
 * 错误信息提供者接口
 * 用于获取错误码对应的多语言错误信息
 *
 * @author binblink
 */
public interface ErrMsgProvider {

    /**
     * 根据错误码和语言获取错误信息
     *
     * @param msgCode 错误码
     * @param lang    语言代码（如 zh_cn, en_us）
     * @return 错误信息
     */
    String getErrMsg(String msgCode, String lang);
}
```

### 2. DefaultErrMsgProvider 默认实现

- 无外部依赖，开箱即用
- 错误码不存在时返回 `SYS00001` 对应的默认消息
- 语言处理：
  - `zh_cn` → "系统错误，请稍后重试"
  - 其他 → "System error, please try again later"

### 3. DbErrMsgProvider 数据库实现

- 查询 `sys_msg_info` 表
- 使用 `CacheComponent` 实现两级缓存（本地 + Redis）
- 错误码不存在时返回 `SYS00001` 的消息
- 语言处理同默认实现

### 4. GlobalExceptionHandler 重构

**移除：**
- `CacheComponent` 依赖
- `getSupplier()` 方法
- `sysMsgInfoMapper` 引用

**修改：**
- `getMsgInfo()` 方法改为调用 `errMsgProvider.getErrMsg()`

**新增：**
- `@Autowired ErrMsgProvider errMsgProvider`

### 5. WebStarterAutoConfiguration

```java
@Configuration
public class WebStarterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ErrMsgProvider.class)
    public ErrMsgProvider defaultErrMsgProvider() {
        return new DefaultErrMsgProvider();
    }
}
```

## 文件变更清单

| 模块 | 操作 | 文件路径 |
|------|------|----------|
| `blink-web-starter` | 新增 | `src/main/java/com/blink/framework/core/exception/ErrMsgProvider.java` |
| `blink-web-starter` | 新增 | `src/main/java/com/blink/framework/core/exception/DefaultErrMsgProvider.java` |
| `blink-web-starter` | 新增 | `src/main/java/com/blink/framework/core/config/WebStarterAutoConfiguration.java` |
| `blink-web-starter` | 修改 | `src/main/java/com/blink/framework/core/config/GlobalExceptionHandler.java` |
| `blink-base-app` | 新增 | `src/main/java/com/blink/gateway/component/DbErrMsgProvider.java` |

## 使用示例

### 新服务使用默认实现

只需引入 `blink-web-starter` 依赖，无需额外配置。

### 新服务使用数据库实现

1. 创建 `sys_msg_info` 表
2. 实现 `ErrMsgProvider` 接口，注入为 Bean
3. Spring 自动覆盖默认实现

## 测试要点

1. 默认实现：验证错误码不存在时返回 `SYS00001` 消息
2. 默认实现：验证中英文语言切换
3. 数据库实现：验证缓存命中和未命中场景
4. 数据库实现：验证错误码不存在时的兜底处理