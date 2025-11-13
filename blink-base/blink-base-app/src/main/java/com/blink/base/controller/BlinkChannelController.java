package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QueryBlinkChannelRspDTO;
import com.blink.base.dto.vo.ChannelVO;
import com.blink.base.entity.BlinkChannelDO;
import com.blink.base.service.BlinkChannelService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 对接渠道 管理API
 * </p>
 *
 * @author binblink
 * @module blink
 * @since 2024-07-29
 */
@RestController
@RequestMapping("/channel")
public class BlinkChannelController {

    @Resource
    private BlinkChannelService blinkChannelService;



    /**
     * 新增对接渠道
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/saveChannel")
    public ResponseDTO<EmptyBody> saveBlinkChannel(@RequestBody @Validated RequestDTO<AddBlinkChannelReqDTO> reqDto) throws BlinkException {
        blinkChannelService.saveBlinkChannel(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 删除对接渠道
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/deleteChannel")
    public ResponseDTO<EmptyBody> deleteBlinkChannel(@RequestBody @Validated RequestDTO<DeleteBlinkChannelReqDTO> reqDto) throws BlinkException {
        blinkChannelService.deleteBlinkChannel(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新对接渠道
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/modifyChannel")
    public ResponseDTO<EmptyBody> modifyBlinkChannel(@RequestBody @Validated RequestDTO<UpdateBlinkChannelReqDTO> reqDto) throws BlinkException {
        blinkChannelService.modifyBlinkChannel(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 根据查询条件查询对接渠道列表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getChannelList")
    public ResponseDTO<QueryBlinkChannelRspDTO> getBlinkChannelList(@RequestBody @Validated RequestDTO<QueryBlinkChannelReqDTO> reqDto) throws BlinkException {

        return ResponseDTO.newSuccessInstance(blinkChannelService.getBlinkChannelList(reqDto.getBody()));
    }


    /**
     * 根据查询条件查询单个渠道信息
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getChannel")
    public ResponseDTO<ChannelVO> getChannel(@RequestBody @Validated RequestDTO<QueryOneChannelReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(blinkChannelService.getChannel(reqDto.getBody()));
    }

    /**
     * 刷新渠道密钥对 重新生成密钥对
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/refreshChannelKey")
    public ResponseDTO<BlinkChannelDO> refreshChannelKey(@RequestBody @Validated RequestDTO<QueryOneChannelReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(blinkChannelService.refreshChannelKey(reqDto.getBody()));
    }


    /**
     * 刷新系统密钥对 重新生成密钥对
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/refreshSystemKey")
    public ResponseDTO<BlinkChannelDO> refreshSystemKey(@RequestBody @Validated RequestDTO<QueryOneChannelReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(blinkChannelService.refreshSystemKey(reqDto.getBody()));
    }


}
