# blink-web-starter 单元测试计划

## 测试概览

| 指标 | 目标值 |
|------|--------|
| 总任务数 | 10 个任务 |
| 总测试类 | 9 个 |
| 预计总用例数 | ~85 个 |
| 行覆盖率目标 | ≥ 80% |
| 分支覆盖率目标 | ≥ 70% |

---

## 任务列表

### 任务 1: GlobalExceptionHandler 测试
- **优先级**: P0
- **测试类**: `GlobalExceptionHandlerTest`
- **预计用例数**: 10 个
- **状态**: ✅ 已完成
- **完成日期**: 2026-04-13
- **实际用例数**: 10 个
- **测试要点**:
  - BlinkException 业务异常处理
  - BlinkException 系统异常处理
  - MethodArgumentNotValidException 参数校验异常
  - NoResourceFoundException 资源未找到异常
  - 通用异常处理
  - 多语言消息获取逻辑
- **依赖**: 需要 mock `ErrMsgProvider`

---

### 任务 2: CircuitBreakerAspect 测试
- **优先级**: P0
- **测试类**: `CircuitBreakerAspectTest`
- **预计用例数**: 12 个
- **状态**: ✅ 已完成
- **完成日期**: 2026-04-13
- **实际用例数**: 10 个
- **测试要点**:
  - 正常调用返回结果
  - 熔断器触发抛出异常
  - 熔断器触发执行降级方法
  - 熔断器实例缓存
  - default/strict/lenient 配置模板
  - 降级方法参数传递
  - 降级方法不存在场景
- **依赖**: 需要 mock `CircuitBreakerRegistry`、`ResilienceProperties`、`ProceedingJoinPoint`

---

### 任务 3: RateLimitAspect 测试
- **优先级**: P0
- **测试类**: `RateLimitAspectTest`
- **预计用例数**: 11 个
- **状态**: ✅ 已完成
- **完成日期**: 2026-04-13
- **实际用例数**: 11 个
- **测试要点**:
  - 正常调用返回结果
  - 限流触发抛出异常
  - 限流触发执行降级方法
  - 限流器实例缓存
  - 注解参数优先级
  - default/strict/lenient 配置模板
  - 降级方法查找与执行
- **依赖**: 需要 mock `RateLimiterRegistry`、`ResilienceProperties`、`ProceedingJoinPoint`

---

### 任务 4: IpRateLimitAspect 测试
- **优先级**: P0
- **测试类**: `IpRateLimitAspectTest`
- **预计用例数**: 9 个
- **状态**: ✅ 已完成
- **完成日期**: 2026-04-13
- **实际用例数**: 9 个
- **测试要点**:
  - 正常调用返回结果
  - 按IP限流触发
  - 不同IP独立限流
  - 从Context获取IP
  - Context为空返回unknown
  - 降级方法执行
- **依赖**: 需要 mock `RateLimiterRegistry`、`BlinkRequestContextHolder`、`ProceedingJoinPoint`

---

### 任务 5: RetryAspect 测试
- **优先级**: P0
- **测试类**: `RetryAspectTest`
- **预计用例数**: 11 个
- **状态**: ✅ 已完成
- **完成日期**: 2026-04-13
- **实际用例数**: 9 个
- **测试要点**:
  - 正常调用直接返回
  - 重试成功场景
  - 重试失败抛出异常
  - 重试失败执行降级方法
  - 注解参数优先级
  - quick/slow/default 配置模板
  - BlinkException 不重试
  - IOException/TimeoutException 重试
- **依赖**: 需要 mock `RetryRegistry`、`ResilienceProperties`、`ProceedingJoinPoint`

---

### 任务 6: DefaultErrMsgProvider 测试
- **优先级**: P1
- **测试类**: `DefaultErrMsgProviderTest`
- **预计用例数**: 10 个
- **状态**: ✅ 已完成
- **完成日期**: 2026-04-13
- **实际用例数**: 15 个
- **测试要点**:
  - 中文业务错误消息
  - 英文业务错误消息
  - 中文系统错误消息
  - 英文系统错误消息
  - BUSS/INVALID/AUTH/FLOW 前缀判断
  - 其他前缀判断
  - 空值处理
- **依赖**: 无外部依赖

---

### 任务 7: BlinkRequestContextInterceptor 测试
- **优先级**: P1
- **测试类**: `BlinkRequestContextInterceptorTest`
- **预计用例数**: 5 个
- **状态**: ⏳ 待开始
- **测试要点**:
  - 设置完整上下文
  - 从Header读取各字段
  - 设置AppName
  - 清理上下文
- **依赖**: 需要 mock `HttpServletRequest`、`HttpServletResponse`

---

### 任务 8: LogMdcInterceptor 测试
- **优先级**: P1
- **测试类**: `LogMdcInterceptorTest`
- **预计用例数**: 5 个
- **状态**: ⏳ 待开始
- **测试要点**:
  - 从Header获取traceId
  - Header无traceId自动生成
  - 设置userName到MDC
  - 清空MDC
- **依赖**: 需要 mock `HttpServletRequest`、`HttpServletResponse`

---

### 任务 9: MdcThreadPoolTaskExecutor 测试
- **优先级**: P1
- **测试类**: `MdcThreadPoolTaskExecutorTest`
- **预计用例数**: 6 个
- **状态**: ⏳ 待开始
- **测试要点**:
  - Callable任务MDC传递
  - Callable任务执行后清空MDC
  - Runnable任务MDC传递
  - Runnable任务执行后清空MDC
  - submit(Runnable) MDC传递
  - 空MDC上下文场景
- **依赖**: 无外部依赖

---

### 任务 10: ScanClassUtil 测试
- **优先级**: P2
- **测试类**: `ScanClassUtilTest`
- **预计用例数**: 3 个
- **状态**: ⏳ 待开始
- **测试要点**:
  - 扫描指定注解的类
  - 空包路径返回空列表
  - 无匹配注解返回空列表
- **依赖**: 需要创建测试用注解和类

---

## 进度跟踪

| 任务 | 状态 | 完成日期 | 备注 |
|------|------|----------|------|
| 任务 1 | ✅ 已完成 | 2026-04-13 | 10个用例全部通过 |
| 任务 2 | ✅ 已完成 | 2026-04-13 | 10个用例全部通过 |
| 任务 3 | ✅ 已完成 | 2026-04-13 | 11个用例全部通过 |
| 任务 4 | ✅ 已完成 | 2026-04-13 | 9个用例全部通过 |
| 任务 5 | ✅ 已完成 | 2026-04-13 | 9个用例全部通过 |
| 任务 6 | ✅ 已完成 | 2026-04-13 | 15个用例全部通过 |
| 任务 7 | ✅ 已完成 | 2026-04-13 | 8个用例全部通过 |
| 任务 8 | ✅ 已完成 | 2026-04-13 | 10个用例全部通过 |
| 任务 9 | ✅ 已完成 | 2026-04-13 | 10个用例全部通过 |
| 任务 10 | ⏳ 待开始 | - | - |

---

## 技术规范

### 测试框架
- JUnit 5
- Mockito
- Spring Boot Test (部分集成测试场景)

### 命名规范
```java
// 测试类命名: 原类名 + Test
public class GlobalExceptionHandlerTest { }

// 测试方法命名: test + 方法名 + 场景描述
@Test
void testHandleBlinkException_业务异常返回正确响应() { }
```

### 测试结构 (AAA模式)
```java
@Test
void testMethodName_场景描述() {
    // Arrange - 准备测试数据
    // Act - 执行被测方法
    // Assert - 验证结果
}
```

### Mock 规范
- 所有外部依赖必须通过 `@Mock` 或 `@MockBean` 进行模拟
- 不依赖真实数据库、网络、文件系统
- 使用 `ArgumentCaptor` 验证方法调用参数

---

## 执行顺序

建议按以下顺序执行测试任务：

1. **第一批 (P0核心)**: 任务 1 → 任务 6 (无外部依赖先行)
2. **第二批 (P0切面)**: 任务 2 → 任务 3 → 任务 4 → 任务 5
3. **第三批 (P1拦截器)**: 任务 7 → 任务 8 → 任务 9
4. **第四批 (P2工具)**: 任务 10

---

## 验收标准

- [ ] 所有测试用例通过
- [ ] 行覆盖率 ≥ 80%
- [ ] 分支覆盖率 ≥ 70%
- [ ] 无 SonarQube 代码异味
- [ ] 测试代码符合项目规范
