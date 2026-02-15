package com.blink.base.service;

import com.blink.base.dto.req.AddSysGroupReq;
import com.blink.base.dto.req.DeleteSysGroupReq;
import com.blink.base.dto.req.QuerySysGroupReq;
import com.blink.base.dto.req.UpdateSysGroupReq;
import com.blink.base.dto.rsp.QuerySysGroupRsp;
import com.blink.base.dto.rsp.SysGroupRsp;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 * 组 服务类
 * </p>
 *
 * @author blink
 * @since 2025-10-16
 */
public interface SysGroupService {

    /**
     * 保存 组
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    SysGroupRsp saveSysGroup(AddSysGroupReq saveParam) throws BlinkException;

    /**
     * 删除 组
     *
     * @param deleteParam
     * @throws BlinkException
     */
    void deleteSysGroup(DeleteSysGroupReq deleteParam) throws BlinkException;

    /**
     * 更新 组
     *
     * @param updateParam
     * @return SysGroupRspDTO
     * @throws BlinkException
     */
    SysGroupRsp modifySysGroup(UpdateSysGroupReq updateParam) throws BlinkException;

    /**
     * 查询 组 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysGroupRsp getSysGroupList(QuerySysGroupReq queryParam) throws BlinkException;


}
