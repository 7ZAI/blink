package com.blink.base.dto.req;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * DeleteBlinkChannelReqDTO删除对接渠道请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-07-29
 */
@Data
public class DeleteBlinkChannelReq implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 渠道ID
     */
    private String deleteId;


}
