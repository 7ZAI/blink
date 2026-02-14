package com.blink.base.service;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QueryBlinkChannelRspDTO;
import com.blink.base.dto.vo.ChannelVO;
import com.blink.base.entity.BlinkChannelDO;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 * 对接渠道 服务类
 * </p>
 *
 * @author binblink
 * @since 2024-07-29
 */
public interface BlinkChannelService {

    /**
     * 保存 对接渠道
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    void saveBlinkChannel(AddBlinkChannelReqDTO saveParam) throws BlinkException;

    /**
     * 删除 对接渠道
     *
     * @param deleteParam
     * @throws BlinkException
     */
    void deleteBlinkChannel(DeleteBlinkChannelReqDTO deleteParam) throws BlinkException;

    /**
     * 更新 对接渠道
     *
     * @param updateParam
     * @throws BlinkException
     */
    void modifyBlinkChannel(UpdateBlinkChannelReqDTO updateParam) throws BlinkException;

    /**
     * 查询 对接渠道 列表
     *
     * @param queryParam
     * @return QueryBlinkChannelRspDTO
     * @throws BlinkException
     */
    QueryBlinkChannelRspDTO getBlinkChannelList(QueryBlinkChannelReqDTO queryParam) throws BlinkException;

    /**
     * 根据查询条件查询单个渠道信息
     *
     * @param queryParam
     * @return {@link BlinkChannelDO}
     * @throws Throwable
     */
    ChannelVO getChannel(QueryOneChannelReqDTO queryParam) throws BlinkException;

    /**
     * 刷新渠道密钥对 重新生成密钥对
     *
     * @param queryParam
     * @return {@link BlinkChannelDO}
     * @throws Throwable
     */
    BlinkChannelDO refreshChannelKey(QueryOneChannelReqDTO queryParam) throws BlinkException;

    /**
     * 刷新系统密钥对 重新生成密钥对
     *
     * @param queryParam
     * @return {@link BlinkChannelDO}
     * @throws Throwable
     */
    BlinkChannelDO refreshSystemKey(QueryOneChannelReqDTO queryParam) throws BlinkException;


}
