package com.blink.base.service;

import com.blink.base.dto.req.AddSysGroupReqDTO;
import com.blink.base.dto.req.DeleteSysGroupReqDTO;
import com.blink.base.dto.req.UpdateSysGroupReqDTO;
import com.blink.base.dto.req.QuerySysGroupReqDTO;
import com.blink.base.dto.rsp.QuerySysGroupRspDTO;
import com.blink.base.dto.rsp.SysGroupRspDTO;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 *  组 服务类
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
    SysGroupRspDTO saveSysGroup(AddSysGroupReqDTO saveParam) throws BlinkException;

    /**
     * 删除 组
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysGroup(DeleteSysGroupReqDTO deleteParam) throws BlinkException;

    /**
     * 更新 组
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    SysGroupRspDTO modifySysGroup(UpdateSysGroupReqDTO updateParam) throws BlinkException;

    /**
     * 查询 组 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysGroupRspDTO getSysGroupList(QuerySysGroupReqDTO queryParam) throws BlinkException;


}
