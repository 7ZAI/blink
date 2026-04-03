package com.blink.log.function;

/**
 * 日志持久化函数式接口
 * <p>
 * 由业务模块实现，负责将日志持久化到存储（数据库、ES、文件等）。
 * <p>
 * 使用示例：
 * <pre>
 * &#064;Bean
 * public LogPersistFunction&lt;SysOperationLogDO&gt; logPersistFunction(SysOperationLogService service) {
 *     return logRecord -&gt; {
 *         SysOperationLogDO entity = convertToEntity(logRecord);
 *         service.asyncSaveLog(entity);
 *     };
 * }
 * </pre>
 *
 * @param <T> 日志实体类型
 * @author binblink
 */
@FunctionalInterface
public interface LogPersistFunction<T> {

    /**
     * 异步持久化日志
     * <p>
     * 实现类应使用异步方式（如 @Async、线程池等）进行持久化，
     * 避免影响主业务流程性能。
     *
     * @param logRecord 日志记录，由业务模块通过 LogConverter 转换后的实体
     */
    void persist(T logRecord);
}