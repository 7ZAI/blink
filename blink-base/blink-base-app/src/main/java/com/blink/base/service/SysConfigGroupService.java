package com.blink.base.service;

import com.blink.base.dto.req.AddSysConfigGroupReqDTO;
import com.blink.base.dto.req.DeleteSysConfigGroupReqDTO;
import com.blink.base.dto.req.UpdateSysConfigGroupReqDTO;
import com.blink.base.dto.req.QuerySysConfigGroupReqDTO;
import com.blink.base.dto.rsp.QuerySysConfigGroupRspDTO;
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
    void saveSysConfigGroup(AddSysConfigGroupReqDTO saveParam) throws BlinkException;

    /**
     * 删除 参数分组表
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysConfigGroup(DeleteSysConfigGroupReqDTO deleteParam) throws BlinkException;

    /**
     * 更新 参数分组表
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    void modifySysConfigGroup(UpdateSysConfigGroupReqDTO updateParam) throws BlinkException;

    /**
     * 查询 参数分组表 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysConfigGroupRspDTO getSysConfigGroupList(QuerySysConfigGroupReqDTO queryParam) throws BlinkException;


}
