package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.AddChannelReq;
import com.blink.gateway.admin.dto.req.DeleteChannelReq;
import com.blink.gateway.admin.dto.req.GetChannelSecretReq;
import com.blink.gateway.admin.dto.req.IssueChannelTokenReq;
import com.blink.gateway.admin.dto.req.QueryChannelReq;
import com.blink.gateway.admin.dto.req.RefreshChannelKeyReq;
import com.blink.gateway.admin.dto.req.UpdateChannelReq;
import com.blink.gateway.admin.dto.rsp.ChannelSecretRsp;
import com.blink.gateway.admin.dto.rsp.ChannelTokenRsp;
import com.blink.gateway.admin.dto.rsp.QueryChannelRsp;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.service.ChannelService;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.dto.req.QueryOneChannelReq;
import com.blink.gateway.dto.vo.ChannelVO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 渠道管理控制器
 *
 * @author binblink
 */
@RestController
@RequestMapping("/channel")
public class ChannelController {

    @Resource
    private ChannelService channelService;

    /**
     * 查询渠道列表
     *
     * @param reqDto 请求参数
     * @return 渠道列表
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getChannelList")
    public ResponseDTO<QueryChannelRsp> getChannelList(@RequestBody @Validated RequestDTO<QueryChannelReq> reqDto) throws BlinkException {
        return channelService.getChannelList(reqDto.getBody());
    }

    /**
     * 获取单个渠道信息
     *
     * @param reqDto 请求参数
     * @return 渠道信息
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getChannel")
    public ResponseDTO<ChannelVO> getChannel(@RequestBody @Validated RequestDTO<QueryOneChannelReq> reqDto) throws BlinkException {
        return channelService.getChannel(reqDto.getBody());
    }

    /**
     * 获取渠道密钥信息
     *
     * @param reqDto 请求参数
     * @return 渠道密钥信息
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getChannelSecret")
    public ResponseDTO<ChannelSecretRsp> getChannelSecret(@RequestBody @Validated RequestDTO<GetChannelSecretReq> reqDto) throws BlinkException {
        return channelService.getChannelSecret(reqDto.getBody());
    }

    /**
     * 新增渠道
     *
     * @param reqDto 请求参数
     * @return 操作结果
     * @throws BlinkException 业务异常
     */
    @PostMapping("/saveChannel")
    public ResponseDTO<EmptyBody> saveChannel(@RequestBody @Validated RequestDTO<AddChannelReq> reqDto) throws BlinkException {
        return channelService.saveChannel(reqDto.getBody());
    }

    /**
     * 更新渠道
     *
     * @param reqDto 请求参数
     * @return 修改后的渠道数据
     * @throws BlinkException 业务异常
     */
    @PostMapping("/modifyChannel")
    public ResponseDTO<ChannelVO> modifyChannel(@RequestBody @Validated RequestDTO<UpdateChannelReq> reqDto) throws BlinkException {
        return channelService.modifyChannel(reqDto.getBody());
    }

    /**
     * 删除渠道
     *
     * @param reqDto 请求参数
     * @return 操作结果
     * @throws BlinkException 业务异常
     */
    @PostMapping("/deleteChannel")
    public ResponseDTO<EmptyBody> deleteChannel(@RequestBody @Validated RequestDTO<DeleteChannelReq> reqDto) throws BlinkException {
        return channelService.deleteChannel(reqDto.getBody());
    }

    /**
     * 刷新渠道密钥
     *
     * @param reqDto 请求参数
     * @return 新密钥信息
     * @throws BlinkException 业务异常
     */
    @PostMapping("/refreshChannelKey")
    public ResponseDTO<GaChannelDO> refreshChannelKey(@RequestBody @Validated RequestDTO<RefreshChannelKeyReq> reqDto) throws BlinkException {
        return channelService.refreshChannelKey(reqDto.getBody());
    }

    /**
     * 刷新系统密钥
     *
     * @param reqDto 请求参数
     * @return 新密钥信息
     * @throws BlinkException 业务异常
     */
    @PostMapping("/refreshSystemKey")
    public ResponseDTO<GaChannelDO> refreshSystemKey(@RequestBody @Validated RequestDTO<RefreshChannelKeyReq> reqDto) throws BlinkException {
        return channelService.refreshSystemKey(reqDto.getBody());
    }

    /**
     * 签发渠道Token
     *
     * @param reqDto 请求参数
     * @return Token信息
     * @throws BlinkException 业务异常
     */
    @PostMapping("/issueChannelToken")
    public ResponseDTO<ChannelTokenRsp> issueChannelToken(@RequestBody @Validated RequestDTO<IssueChannelTokenReq> reqDto) throws BlinkException {
        return channelService.issueChannelToken(reqDto.getBody());
    }
}
