package com.blink.base.service;

import com.blink.base.dto.req.AddSysUserReq;
import com.blink.base.dto.req.DeleteSysUserReq;
import com.blink.base.dto.req.QuerySysUserReq;
import com.blink.base.dto.req.UpdateSysUserReq;
import com.blink.base.dto.rsp.SysUserRsp;
import com.blink.base.dto.vo.SysUserVO;
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
    void saveSysUser(AddSysUserReq saveParam) throws BlinkException;

    /**
     * 删除 系统用户
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysUser(DeleteSysUserReq deleteParam) throws BlinkException;

    /**
     * 更新 系统用户
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    void modifySysUser(UpdateSysUserReq updateParam) throws BlinkException;

    /**
     * 查询 系统用户 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    SysUserRsp getSysUserList(QuerySysUserReq queryParam) throws BlinkException;


    /**
     * 查询 系统用户 详情
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    SysUserVO getSysUserDetail(QuerySysUserReq queryParam) throws BlinkException;
}
