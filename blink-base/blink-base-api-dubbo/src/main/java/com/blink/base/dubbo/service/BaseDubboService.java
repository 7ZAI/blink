package com.blink.base.dubbo.service;


import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.base.dto.req.GetAllApiPermissionsReq;
import com.blink.base.dto.req.QueryErrMsgReq;
import com.blink.base.dto.req.QueryOneSysConfigReq;
import com.blink.base.dto.req.QuerySimpleUserReq;
import com.blink.base.dto.req.QueryUserPermissionReq;
import com.blink.base.dto.req.UserIdReq;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.dto.rsp.QuerySimpleUserRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.base.dto.rsp.UserPermissionDetailRsp;

import java.util.concurrent.CompletableFuture;

/**
 * Dubbo 服务接口
 * 服务间调用
 * <p>
 * 提供同步和异步两种调用方式：
 * - 同步方法：直接返回 ResponseDTO
 * - 异步方法（Async后缀）：返回 CompletableFuture，适合响应式场景
 * </p>
 */
public interface BaseDubboService {

    // ==================== 同步方法 ====================

    /**
     * 根据配置key值获取单个配置参数信息
     * @param reqDto 请求参数
     * @return ResponseDTO<SysConfigCacheDO> 配置信息
     */
    ResponseDTO<SysConfigCacheDO> getOneConfig(RequestDTO<QueryOneSysConfigReq> reqDto);

    /**
     * 获取错误提示信息
     * @param reqDto 请求参数
     * @return ResponseDTO<QueryErrMsgRsp> 错误信息
     */
    ResponseDTO<QueryErrMsgRsp> getErrorMsgInfo(RequestDTO<QueryErrMsgReq> reqDto);

    /**
     * 获取用户权限标识
     * @param reqDto 请求参数
     * @return ResponseDTO<QueryUserPermissionRsp> 用户权限
     */
    ResponseDTO<QueryUserPermissionRsp> getUserPermissionsByUerId(RequestDTO<QueryUserPermissionReq> reqDto);

    /**
     * 获取请求路径对应的权限标识
     * @param reqDto 请求参数
     * @return ResponseDTO<QueryUserPermissionRsp> 权限信息
     */
    ResponseDTO<QueryUserPermissionRsp> getUserPermissionsByPath(RequestDTO<QueryUserPermissionReq> reqDto);

    /**
     * 获取所有接口权限
     * @param reqDto 请求参数
     * @return ResponseDTO<GetAllApiPermissionsRsp> 所有接口权限
     */
    ResponseDTO<GetAllApiPermissionsRsp> getAllApiPermissions(RequestDTO<GetAllApiPermissionsReq> reqDto);

    // ==================== 异步方法（原生 CompletableFuture）====================

    /**
     * 根据配置key值获取单个配置参数信息（异步）
     * @param reqDto 请求参数
     * @return CompletableFuture<ResponseDTO<SysConfigCacheDO>> 配置信息
     */
    CompletableFuture<ResponseDTO<SysConfigCacheDO>> getOneConfigAsync(RequestDTO<QueryOneSysConfigReq> reqDto);

    /**
     * 获取错误提示信息（异步）
     * @param reqDto 请求参数
     * @return CompletableFuture<ResponseDTO<QueryErrMsgRsp>> 错误信息
     */
    CompletableFuture<ResponseDTO<QueryErrMsgRsp>> getErrorMsgInfoAsync(RequestDTO<QueryErrMsgReq> reqDto);

    /**
     * 获取用户权限标识（异步）
     * @param reqDto 请求参数
     * @return CompletableFuture<ResponseDTO<QueryUserPermissionRsp>> 用户权限
     */
    CompletableFuture<ResponseDTO<QueryUserPermissionRsp>> getUserPermissionsByUerIdAsync(RequestDTO<QueryUserPermissionReq> reqDto);

    /**
     * 获取请求路径对应的权限标识（异步）
     * @param reqDto 请求参数
     * @return CompletableFuture<ResponseDTO<QueryUserPermissionRsp>> 权限信息
     */
    CompletableFuture<ResponseDTO<QueryUserPermissionRsp>> getUserPermissionsByPathAsync(RequestDTO<QueryUserPermissionReq> reqDto);

    /**
     * 获取所有接口权限（异步）
     * @param reqDto 请求参数
     * @return CompletableFuture<ResponseDTO<GetAllApiPermissionsRsp>> 所有接口权限
     */
    CompletableFuture<ResponseDTO<GetAllApiPermissionsRsp>> getAllApiPermissionsAsync(RequestDTO<GetAllApiPermissionsReq> reqDto);

    // ==================== 渠道关联用户选择 ====================

    /**
     * 查询简化用户列表（用于弹窗选择）
     *
     * @param reqDto 请求参数
     * @return 用户列表
     */
    ResponseDTO<QuerySimpleUserRsp> getSimpleUserList(RequestDTO<QuerySimpleUserReq> reqDto);

    /**
     * 查询用户权限详情（角色、接口权限、数据过滤权限）
     *
     * @param reqDto 请求参数
     * @return 权限详情
     */
    ResponseDTO<UserPermissionDetailRsp> getUserPermissionDetail(RequestDTO<UserIdReq> reqDto);

    // ==================== 异步方法（渠道关联用户选择）====================

    /**
     * 查询简化用户列表（用于弹窗选择）- 异步
     *
     * @param reqDto 请求参数
     * @return 用户列表
     */
    CompletableFuture<ResponseDTO<QuerySimpleUserRsp>> getSimpleUserListAsync(RequestDTO<QuerySimpleUserReq> reqDto);

    /**
     * 查询用户权限详情（角色、接口权限、数据过滤权限）- 异步
     *
     * @param reqDto 请求参数
     * @return 权限详情
     */
    CompletableFuture<ResponseDTO<UserPermissionDetailRsp>> getUserPermissionDetailAsync(RequestDTO<UserIdReq> reqDto);

}
