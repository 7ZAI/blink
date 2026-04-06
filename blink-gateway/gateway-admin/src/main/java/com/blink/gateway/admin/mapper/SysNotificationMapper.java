package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.SysNotificationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息通知Mapper
 *
 * @author binblink
 * @since 2026-04-06
 */
@Mapper
public interface SysNotificationMapper extends BaseMapper<SysNotificationDO> {

    /**
     * 查询用户未读消息列表
     */
    @Select("SELECT n.* FROM sys_notification n " +
            "WHERE (n.target_type = 'ALL' OR n.target_user_id = #{userId}) " +
            "AND n.notification_id NOT IN " +
            "(SELECT nr.notification_id FROM sys_notification_read nr WHERE nr.user_id = #{userId}) " +
            "AND (n.expire_time IS NULL OR n.expire_time > NOW()) " +
            "ORDER BY n.created_time DESC LIMIT #{limit}")
    List<SysNotificationDO> selectUnreadByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * 查询用户未读消息数量
     */
    @Select("SELECT COUNT(*) FROM sys_notification n " +
            "WHERE (n.target_type = 'ALL' OR n.target_user_id = #{userId}) " +
            "AND n.notification_id NOT IN " +
            "(SELECT nr.notification_id FROM sys_notification_read nr WHERE nr.user_id = #{userId}) " +
            "AND (n.expire_time IS NULL OR n.expire_time > NOW())")
    Integer countUnreadByUserId(@Param("userId") Integer userId);
}