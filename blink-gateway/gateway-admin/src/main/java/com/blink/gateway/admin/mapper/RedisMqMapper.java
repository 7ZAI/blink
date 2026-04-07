package com.blink.gateway.admin.mapper;

import com.blink.gateway.admin.entity.RedisMqDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * redis stream消息发送记录表 Mapper 接口
 *
 * @author binblink
 * @since 2025-11-05
 */
@Mapper
public interface RedisMqMapper extends BaseMapper<RedisMqDO> {

    /**
     * 查询失败消息用于重试
     *
     * @param status        消息状态
     * @param maxRetryTimes 最大重试次数
     * @return 失败消息列表
     */
    @Select("SELECT * FROM redis_mq WHERE msg_status = #{status} AND retry_times < #{maxRetryTimes}")
    List<RedisMqDO> selectFailedMessagesForRetry(@Param("status") String status,
                                                   @Param("maxRetryTimes") int maxRetryTimes);

}