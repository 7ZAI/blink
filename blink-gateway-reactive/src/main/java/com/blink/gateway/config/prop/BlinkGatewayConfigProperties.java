package com.blink.gateway.config.prop;

import com.blink.framework.validate.annotation.ValidIPAddress;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * gateway 系统参数 配置属性类
 *
 * @Author binblink
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "blink.gateway.config")
@Validated
public class BlinkGatewayConfigProperties {

    /**
     * 报文签名开关
     */
    private boolean signatureEnable = true;

    /**
     * 防止请求重放开关
     */
    private boolean requestReplayDefendEnable = false;

    /**
     * 请求有效时间（毫秒）
     */
    private long requestReplayDefendEffectTime = 6000L;

    /**
     * 请求随机值过期时间（毫秒）
     */
    private long requestReplayDefendNonceExpireTime = 6000L;

    /**
     * api下线过滤开关
     */
    private boolean apiDisableSwitch = false;

    /**
     * 临时下线API集合（url）
     */
    private List<String> apiDisableList = new ArrayList<>();

    private IPFilter ipFilter = new IPFilter();


    /**
     * IP过滤配置类
     * 支持：单个IP、CIDR网段（如192.168.1.0/24）、IP段（如192.168.1.1-192.168.1.100）
     */
    @Getter
    @Setter
    @Validated
    public static class IPFilter {
        /**
         * IP过滤总开关
         * 默认：关闭
         */
        private boolean ipFilterEnable = false;

        private boolean whiteListEnable = false;

        private boolean blackListEnable = false;

        /**
         * 白名单-单个IP地址集合
         * 格式示例：["192.168.1.1", "10.0.0.1"]
         * 默认：空列表
         */
        @ValidIPAddress(type = ValidIPAddress.IPType.ALL,targetType = ValidIPAddress.TargetType.MULTIPLE,message = "配置白名单ip格式错误")
        private List<String> whiteListIps = new ArrayList<>();

        /**
         * 白名单-IP范围集合（支持CIDR网段/IP段）
         * 格式示例：
         * - CIDR网段：192.168.1.0/24（表示192.168.1.0-192.168.1.255）
         * - IP段：192.168.2.1-192.168.2.100（表示从起始IP到结束IP的连续范围）
         * 默认：空列表
         */
        private List<String> whiteListIpRanges = new ArrayList<>();

        /**
         * 黑名单-单个IP地址集合
         * 格式示例：["192.168.3.1", "172.17.0.1"]
         * 默认：空列表
         */
        @ValidIPAddress(type = ValidIPAddress.IPType.ALL,targetType = ValidIPAddress.TargetType.MULTIPLE,message = "配置黑名单ip格式错误")
        private List<String> blackListIps = new ArrayList<>();

        /**
         * 黑名单-IP范围集合（支持CIDR网段/IP段）
         * 格式示例：
         * - CIDR网段：10.0.0.0/8（表示10.0.0.0-10.255.255.255）
         * - IP段：172.18.0.1-172.18.0.255
         * 默认：空列表
         */
        private List<String> blackListIpRanges = new ArrayList<>();

    }
}

