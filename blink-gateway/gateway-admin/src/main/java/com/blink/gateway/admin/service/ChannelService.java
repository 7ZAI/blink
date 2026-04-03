package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.AddChannelReq;
import com.blink.gateway.admin.dto.req.DeleteChannelReq;
import com.blink.gateway.admin.dto.req.IssueChannelTokenReq;
import com.blink.gateway.admin.dto.req.QueryChannelReq;
import com.blink.gateway.admin.dto.req.RefreshChannelKeyReq;
import com.blink.gateway.admin.dto.req.UpdateChannelReq;
import com.blink.gateway.admin.dto.rsp.ChannelTokenRsp;
import com.blink.gateway.admin.dto.rsp.QueryChannelRsp;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.dto.vo.ChannelVO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.dto.req.QueryOneChannelReq;

/**
 * 渠道管理服务接口
 *
 * @author binblink
 */
public interface ChannelService {

    /**
     * 查询渠道列表
     *
     * @param req 请求参数
     * @return 渠道列表
     * @throws BlinkException 业务异常
     */
    ResponseDTO<QueryChannelRsp> getChannelList(QueryChannelReq req) throws BlinkException;

    /**
     * 获取单个渠道信息
     *
     * @param req 请求参数
     * @return 渠道信息
     * @throws BlinkException 业务异常
     */
    ResponseDTO<ChannelVO> getChannel(QueryOneChannelReq req) throws BlinkException;

    /**
     * 新增渠道
     *
     * @param req 请求参数
     * @return 操作结果
     * @throws BlinkException 业务异常
     */
    ResponseDTO<EmptyBody> saveChannel(AddChannelReq req) throws BlinkException;

    /**
     * 更新渠道
     *
     * @param req 请求参数
     * @return 修改后的渠道数据
     * @throws BlinkException 业务异常
     */
    ResponseDTO<ChannelVO> modifyChannel(UpdateChannelReq req) throws BlinkException;

    /**
     * 删除渠道
     *
     * @param req 请求参数
     * @return 操作结果
     * @throws BlinkException 业务异常
     */
    ResponseDTO<EmptyBody> deleteChannel(DeleteChannelReq req) throws BlinkException;

    /**
     * 刷新渠道密钥
     *
     * @param req 请求参数
     * @return 新密钥信息
     * @throws BlinkException 业务异常
     */
    ResponseDTO<GaChannelDO> refreshChannelKey(RefreshChannelKeyReq req) throws BlinkException;

    /**
     * 刷新系统密钥
     *
     * @param req 请求参数
     * @return 新密钥信息
     * @throws BlinkException 业务异常
     */
    ResponseDTO<GaChannelDO> refreshSystemKey(RefreshChannelKeyReq req) throws BlinkException;

    /**
     * 签发渠道Token
     *
     * @param req 请求参数
     * @return Token信息
     * @throws BlinkException 业务异常
     */
    ResponseDTO<ChannelTokenRsp> issueChannelToken(IssueChannelTokenReq req) throws BlinkException;
}
