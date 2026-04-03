package com.blink.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.entity.SysOperationLogDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 操作日志 Mapper 接口
 * </p>
 *
 * @author binblink
 * @since 2024-03-11
 */
public interface SysOperationLogMapper extends BaseMapper<SysOperationLogDO> {

    /**
     * 查询需要归档的日志ID列表
     *
     * @param beforeTime 归档时间点之前的数据
     * @param limit      单次查询数量限制
     * @return 日志ID列表
     */
    List<Long> selectArchiveLogIds(@Param("beforeTime") LocalDateTime beforeTime, @Param("limit") int limit);

    /**
     * 批量归档日志到历史表
     *
     * @param logIds 日志ID列表
     * @return 归档数量
     */
    int batchArchiveToHistory(@Param("logIds") List<Long> logIds);

    /**
     * 批量删除已归档的日志
     *
     * @param logIds 日志ID列表
     * @return 删除数量
     */
    int batchDeleteByIds(@Param("logIds") List<Long> logIds);

}
