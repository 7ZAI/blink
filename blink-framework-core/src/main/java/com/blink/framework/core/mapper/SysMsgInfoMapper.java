package com.blink.framework.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.framework.core.entity.SysMsgInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 消息码信息表 Mapper 接口
 * </p>
 *
 * @author binblink
 */
@Mapper
public interface SysMsgInfoMapper extends BaseMapper<SysMsgInfoDO> {

    @Select("SELECT msg_Code,msg_info,msg_lang FROM sys_msg_info")
    List<SysMsgInfoDO> findAllMsgInfo();
}
