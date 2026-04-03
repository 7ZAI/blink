# ErrMsgProvider 接口抽象实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 GlobalExceptionHandler 中的错误信息查询逻辑抽象为 ErrMsgProvider 接口，实现基础模块与具体服务的解耦。

**Architecture:** 采用策略模式，在 blink-web-starter 中定义 ErrMsgProvider 接口和默认实现，blink-base-app 中提供数据库查询实现。Spring 自动配置确保向后兼容。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, Spring Auto-Configuration

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `blink-web-starter/src/main/java/com/blink/framework/core/exception/ErrMsgProvider.java` | 新增 | 错误信息提供者接口 |
| `blink-web-starter/src/main/java/com/blink/framework/core/exception/DefaultErrMsgProvider.java` | 新增 | 默认实现（无外部依赖） |
| `blink-web-starter/src/main/java/com/blink/framework/core/config/WebStarterAutoConfiguration.java` | 新增 | 自动配置类 |
| `blink-web-starter/src/main/java/com/blink/framework/core/config/GlobalExceptionHandler.java` | 修改 | 重构，移除数据库依赖 |
| `blink-base-app/src/main/java/com/blink/gateway/component/DbErrMsgProvider.java` | 新增 | 数据库实现（带缓存） |

---

## Chunk 1: ErrMsgProvider 接口与默认实现

### Task 1: 创建 ErrMsgProvider 接口

**Files:**
- Create: `blink-web-starter/src/main/java/com/blink/framework/core/exception/ErrMsgProvider.java`

- [ ] **Step 1: 创建 exception 包目录**

```bash
mkdir -p blink-web-starter/src/main/java/com/blink/framework/core/exception
```

- [ ] **Step 2: 创建 ErrMsgProvider 接口**

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
     * @return 错误信息，如果不存在返回默认系统错误消息
     */
    String getErrMsg(String msgCode, String lang);
}
```

- [ ] **Step 3: 提交接口文件**

```bash
git add blink-web-starter/src/main/java/com/blink/framework/core/exception/ErrMsgProvider.java
git commit -m "feat(blink-web-starter): 添加 ErrMsgProvider 接口定义"
```

---

### Task 2: 创建 DefaultErrMsgProvider 默认实现

**Files:**
- Create: `blink-web-starter/src/main/java/com/blink/framework/core/exception/DefaultErrMsgProvider.java`

- [ ] **Step 1: 创建默认实现类**

```java
package com.blink.framework.core.exception;

/**
 * 默认错误信息提供者
 * 不依赖外部资源，直接返回默认消息
 *
 * @author binblink
 */
public class DefaultErrMsgProvider implements ErrMsgProvider {

    /**
     * 系统错误码
     */
    private static final String SYS_ERROR_CODE = "SYS00001";

    /**
     * 中文默认错误消息
     */
    private static final String DEFAULT_MSG_CN = "系统错误，请稍后重试";

    /**
     * 英文默认错误消息
     */
    private static final String DEFAULT_MSG_EN = "System error, please try again later";

    @Override
    public String getErrMsg(String msgCode, String lang) {
        // 统一返回 SYS00001 对应的默认消息
        return getSystemErrorMsg(lang);
    }

    /**
     * 获取系统错误消息
     *
     * @param lang 语言代码
     * @return 对应语言的系统错误消息
     */
    private String getSystemErrorMsg(String lang) {
        // 非中文统一返回英文
        if ("zh_cn".equalsIgnoreCase(lang)) {
            return DEFAULT_MSG_CN;
        }
        return DEFAULT_MSG_EN;
    }
}
```

- [ ] **Step 2: 提交默认实现**

```bash
git add blink-web-starter/src/main/java/com/blink/framework/core/exception/DefaultErrMsgProvider.java
git commit -m "feat(blink-web-starter): 添加 DefaultErrMsgProvider 默认实现"
```

---

### Task 3: 创建 WebStarterAutoConfiguration 自动配置

**Files:**
- Create: `blink-web-starter/src/main/java/com/blink/framework/core/config/WebStarterAutoConfiguration.java`

- [ ] **Step 1: 创建自动配置类**

```java
package com.blink.framework.core.config;

import com.blink.framework.core.exception.DefaultErrMsgProvider;
import com.blink.framework.core.exception.ErrMsgProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web Starter 自动配置类
 *
 * @author binblink
 */
@Configuration
public class WebStarterAutoConfiguration {

    /**
     * 默认错误信息提供者
     * 当业务服务没有提供自己的实现时使用
     *
     * @return DefaultErrMsgProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean(ErrMsgProvider.class)
    public ErrMsgProvider defaultErrMsgProvider() {
        return new DefaultErrMsgProvider();
    }
}
```

- [ ] **Step 2: 提交自动配置类**

```bash
git add blink-web-starter/src/main/java/com/blink/framework/core/config/WebStarterAutoConfiguration.java
git commit -m "feat(blink-web-starter): 添加 WebStarterAutoConfiguration 自动配置"
```

---

## Chunk 2: 重构 GlobalExceptionHandler

### Task 4: 重构 GlobalExceptionHandler

**Files:**
- Modify: `blink-web-starter/src/main/java/com/blink/framework/core/config/GlobalExceptionHandler.java`

- [ ] **Step 1: 读取当前文件内容**

```bash
# 确认当前实现
```

- [ ] **Step 2: 重构 GlobalExceptionHandler**

完整替换为以下内容：

```java
package com.blink.framework.core.config;

import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.data.CoreConstant;
import com.blink.framework.core.exception.ErrMsgProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理
 * 优先处理子类确定声明的异常，然后再处理父类
 *
 * @author binblink
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Autowired
    private ErrMsgProvider errMsgProvider;

    /**
     * 业务异常处理
     *
     * @param exception BlinkException
     * @return ResponseDTO<EmptyBody>
     */
    @ExceptionHandler(value = BlinkException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseDTO<EmptyBody> handleBlinkException(BlinkException exception) {
        log.error(exception.getMessage(), exception);

        ResponseDTO<EmptyBody> rspDto = ResponseDTO.newFailInstance();
        String msgCode = exception.getMessage();
        rspDto.setMsgCode(msgCode);

        String lang = getLanguage();
        String msgInfo = errMsgProvider.getErrMsg(msgCode, lang);
        rspDto.setMsgInfo(msgInfo);

        return rspDto;
    }

    /**
     * 数据校验异常
     *
     * @param exception MethodArgumentNotValidException
     * @return ResponseDTO<EmptyBody>
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseDTO<EmptyBody> handleException(MethodArgumentNotValidException exception) {
        log.error(exception.getMessage(), exception);

        BindingResult bindingResult = exception.getBindingResult();
        // 可能会同时存在多个参数校验失败异常，只取第一个异常信息
        List<ObjectError> objectErrors = bindingResult.getAllErrors();

        ResponseDTO<EmptyBody> rspDto = ResponseDTO.newFailInstance();
        String msgCode = objectErrors.get(0).getDefaultMessage();
        rspDto.setMsgCode(msgCode);

        String lang = getLanguage();
        String msgInfo = errMsgProvider.getErrMsg(msgCode, lang);
        rspDto.setMsgInfo(msgInfo);

        return rspDto;
    }

    /**
     * 资源未找到异常
     *
     * @param exception NoResourceFoundException
     * @return ResponseDTO<EmptyBody>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ResponseDTO<EmptyBody> handleNoResourceFoundException(NoResourceFoundException exception) {
        log.error("资源未找到: {}", exception.getResourcePath(), exception);

        ResponseDTO<EmptyBody> rspDto = ResponseDTO.newFailInstance();
        rspDto.setMsgCode(BlinkErrorCodeEnum.NO_HANDLER_FOUND_ERROR.getCode());

        String lang = getLanguage();
        String msgInfo = errMsgProvider.getErrMsg(BlinkErrorCodeEnum.NO_HANDLER_FOUND_ERROR.getCode(), lang);
        rspDto.setMsgInfo(msgInfo);

        return rspDto;
    }

    /**
     * 处理其他所有未被捕获的异常
     *
     * @param e Exception
     * @return ResponseDTO<EmptyBody>
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseDTO<EmptyBody> handleAllOtherExceptions(Exception e) {
        // 记录详细日志便于排查
        log.error("发生未处理的异常：", e);

        ResponseDTO<EmptyBody> errRsp = ResponseDTO.newFailInstance();
        String msgCode = errRsp.getMsgCode();

        String lang = getLanguage();
        String msgInfo = errMsgProvider.getErrMsg(msgCode, lang);
        errRsp.setMsgInfo(msgInfo);

        return errRsp;
    }

    /**
     * 获取请求中的语言环境
     *
     * @return 语言代码
     */
    private String getLanguage() {
        String lang = BlinkRequestContextHolder.getLanguage();
        if (lang == null || lang.trim().isEmpty()) {
            lang = CoreConstant.LANG_CN;
        }

        // 格式化语言代码，将 '-' 转换为 '_' 以匹配数据库存储格式
        lang = lang.replace('-', '_');

        return lang;
    }
}
```

- [ ] **Step 3: 提交重构**

```bash
git add blink-web-starter/src/main/java/com/blink/framework/core/config/GlobalExceptionHandler.java
git commit -m "refactor(blink-web-starter): 重构 GlobalExceptionHandler 使用 ErrMsgProvider 接口"
```

---

## Chunk 3: 数据库实现

### Task 5: 创建 DbErrMsgProvider 数据库实现

**Files:**
- Create: `blink-base-app/src/main/java/com/blink/gateway/component/DbErrMsgProvider.java`

- [ ] **Step 1: 创建 DbErrMsgProvider 实现类**

```java
package com.blink.gateway.component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blink.framework.core.entity.SysMsgInfoDO;
import com.blink.framework.core.exception.ErrMsgProvider;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.gateway.mapper.SysMsgInfoMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 基于数据库的错误信息提供者
 * 支持多语言，带两级缓存（本地 + Redis）
 *
 * @author binblink
 */
@Slf4j
@Component
public class DbErrMsgProvider implements ErrMsgProvider {

    /**
     * 系统错误码
     */
    private static final String SYS_ERROR_CODE = "SYS00001";

    /**
     * 缓存 key 前缀
     */
    private static final String CACHE_KEY_PREFIX = "system:err:msg:";

    /**
     * 中文默认错误消息
     */
    private static final String DEFAULT_MSG_CN = "系统错误，请稍后重试";

    /**
     * 英文默认错误消息
     */
    private static final String DEFAULT_MSG_EN = "System error, please try again later";

    @Resource
    private CacheComponent cacheComponent;

    @Resource
    private SysMsgInfoMapper sysMsgInfoMapper;

    @Override
    public String getErrMsg(String msgCode, String lang) {
        if (msgCode == null || msgCode.isEmpty()) {
            return getSystemErrorMsg(lang);
        }

        String cacheKey = CACHE_KEY_PREFIX + lang + ":" + msgCode;

        try {
            Supplier<String> dbQuery = () -> queryFromDatabase(msgCode, lang);
            Object result = cacheComponent.getFromCacheOrDB(cacheKey, dbQuery);

            if (result != null && !result.toString().isEmpty()) {
                return result.toString();
            }
        } catch (Exception e) {
            log.error("获取错误信息失败, msgCode: {}, lang: {}", msgCode, lang, e);
        }

        // 查询失败或不存在时返回系统错误消息
        return getSystemErrorMsg(lang);
    }

    /**
     * 从数据库查询错误信息
     *
     * @param msgCode 错误码
     * @param lang    语言
     * @return 错误信息，不存在时返回 null
     */
    private String queryFromDatabase(String msgCode, String lang) {
        SysMsgInfoDO msgInfo = sysMsgInfoMapper.selectOne(
                new QueryWrapper<SysMsgInfoDO>()
                        .lambda()
                        .eq(SysMsgInfoDO::getMsgCode, msgCode)
                        .eq(SysMsgInfoDO::getMsgLang, lang)
        );

        return Objects.isNull(msgInfo) ? null : msgInfo.getMsgInfo();
    }

    /**
     * 获取系统错误消息
     *
     * @param lang 语言代码
     * @return 对应语言的系统错误消息
     */
    private String getSystemErrorMsg(String lang) {
        // 非中文统一返回英文
        if ("zh_cn".equalsIgnoreCase(lang)) {
            return DEFAULT_MSG_CN;
        }
        return DEFAULT_MSG_EN;
    }
}
```

- [ ] **Step 2: 提交数据库实现**

```bash
git add blink-base-app/src/main/java/com/blink/gateway/component/DbErrMsgProvider.java
git commit -m "feat(blink-base-app): 添加 DbErrMsgProvider 数据库实现"
```

---

## Chunk 4: 验证与构建

### Task 6: 构建验证

- [ ] **Step 1: 构建 blink-web-starter 模块**

```bash
./gradlew :blink-web-starter:build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 发布 blink-web-starter 到本地 Maven 仓库**

```bash
./gradlew :blink-web-starter:publishToMavenLocal
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 构建 blink-base-app 模块**

```bash
./gradlew :blink-base:blink-base-app:build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交设计文档**

```bash
git add docs/superpowers/specs/2025-03-11-errmsg-provider-design.md
git commit -m "docs: 添加 ErrMsgProvider 接口抽象设计文档"
```

---

## 完成检查清单

- [ ] ErrMsgProvider 接口创建完成
- [ ] DefaultErrMsgProvider 默认实现完成
- [ ] WebStarterAutoConfiguration 自动配置完成
- [ ] GlobalExceptionHandler 重构完成（移除 CacheComponent 和 sysMsgInfoMapper 依赖）
- [ ] DbErrMsgProvider 数据库实现完成
- [ ] 所有模块构建成功
- [ ] 所有提交完成

---

## 使用说明

### 新服务使用默认实现

只需引入 `blink-web-starter` 依赖，无需额外配置。错误信息将统一返回 `SYS00001` 对应的默认消息。

### 新服务使用数据库实现

1. 确保数据库中存在 `sys_msg_info` 表
2. 实现 `ErrMsgProvider` 接口并注入为 Bean
3. Spring 会自动覆盖默认实现

### 多语言支持

- `zh_cn` → 中文消息
- 其他语言 → 英文消息