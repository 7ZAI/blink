package com.blink.log.function;

import com.blink.log.model.OperationLogRecord;

/**
 * 日志转换函数式接口
 * <p>
 * 将通用的 {@link OperationLogRecord} 转换为业务模块的日志实体类型。
 * <p>
 * 使用示例：
 * <pre>
 * &#064;Bean
 * public LogConverter&lt;SysOperationLogDO&gt; logConverter() {
 *     return record -&gt; {
 *         SysOperationLogDO entity = new SysOperationLogDO();
 *         BeanUtil.copyProperties(record, entity);
 *         // 设置业务特定字段
 *         entity.setCreateTime(LocalDateTime.now());
 *         return entity;
 *     };
 * }
 * </pre>
 *
 * @param <T> 目标日志实体类型
 * @author binblink
 */
@FunctionalInterface
public interface LogConverter<T> {

    /**
     * 将通用日志记录转换为业务实体
     *
     * @param record 通用日志记录
     * @return 业务日志实体
     */
    T convert(OperationLogRecord record);
}