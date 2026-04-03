package com.blink.gateway.base.service;

import com.blink.gateway.base.dto.req.AddSysConfigGroupReq;
import com.blink.gateway.base.dto.req.DeleteSysConfigGroupReq;
import com.blink.gateway.base.dto.req.UpdateSysConfigGroupReq;
import com.blink.gateway.base.dto.req.QuerySysConfigGroupReq;
import com.blink.gateway.base.dto.rsp.QuerySysConfigGroupRsp;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 *  参数分组表 服务类
 * </p>
 *
 * @author blink
 * @since 2025-10-16
 */
public interface SysConfigGroupService {

    /**
     * 保存 参数分组表
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    void saveSysConfigGroup(AddSysConfigGroupReq saveParam) throws BlinkException;

    /**
     * 删除 参数分组表
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysConfigGroup(DeleteSysConfigGroupReq deleteParam) throws BlinkException;

    /**
     * 更新 参数分组表
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    void modifySysConfigGroup(UpdateSysConfigGroupReq updateParam) throws BlinkException;

    /**
     * 查询 参数分组表 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysConfigGroupRsp getSysConfigGroupList(QuerySysConfigGroupReq queryParam) throws BlinkException;


}
