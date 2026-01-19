package com.blink.base.service;

import com.blink.base.dto.req.AddSysUserReqDTO;
import com.blink.base.dto.req.DeleteSysUserReqDTO;
import com.blink.base.dto.req.QuerySysUserReqDTO;
import com.blink.base.dto.req.UpdateSysUserReqDTO;
import com.blink.base.dto.rsp.SysUserRspDTO;
import com.blink.base.entity.SysUserDO;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 * 系统用户 服务类
 * </p>
 *
 * @author binblink
 * @since 2023-12-26
 */
public interface SysUserService {

    /**
     * 保存 系统用户
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    void saveSysUser(AddSysUserReqDTO saveParam) throws BlinkException;

    /**
     * 删除 系统用户
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysUser(DeleteSysUserReqDTO deleteParam) throws BlinkException;

    /**
     * 更新 系统用户
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    void modifySysUser(UpdateSysUserReqDTO updateParam) throws BlinkException;

    /**
     * 查询 系统用户 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    SysUserRspDTO getSysUserList(QuerySysUserReqDTO queryParam) throws BlinkException;


    /**
     * 查询 系统用户 详情
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    SysUserDO getSysUserDetail(QuerySysUserReqDTO queryParam) throws BlinkException;
}
