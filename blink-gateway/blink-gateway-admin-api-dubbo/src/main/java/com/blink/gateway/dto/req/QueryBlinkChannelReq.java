package com.blink.gateway.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * QueryBlinkChannelReqDTO 查询列表对接渠道请求参数对象
 * </p>
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryBlinkChannelReq extends PageDTO implements Serializable {

  private static final long serialVersionUID = 1L;


    /**
     * 渠道ID
     */
    private String channelId;


    /**
     * 渠道名
     */
    private String channelName;


    /**
     * 应用key值
     */
    private String appKey;


    /**
     * 应用秘钥
     */
    private String appSecret;


    /**
     * 关联用户
     */
    private String relaUserId;


    /**
     * 认证token
     */
    private String accessToken;


    /**
     * 系统公钥
     */
    private String systemPublickey;


    /**
     * 系统私钥
     */
    private String systemPrivatekey;


    /**
     * 渠道公钥
     */
    private String channelPublickey;


    /**
     * 渠道私钥
     */
    private String channelPrivatekey;


    /**
     * 渠道开关 0 开启 1关闭
     */
    private Byte enable;


    /**
     * 加密开关 0 开启 1关闭
     */
    private Byte encryptionSwitch;


    /**
     * 认证方式 0带状态的token  1 jwt
     */
    private Byte tokenType;


    /**
     * 权限校验开关 0 开启 1关闭
     */
    private Byte authoritySwitch;


    /**
     * 备注
     */
    private String remark;


    /**
     * 创建者
     */
    private String createBy;


    /**
     * 更新者
     */
    private String updateBy;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}