package com.blink.framework.core.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * web 应用配置属性
 * @Author binblink
 */

@Data
@ConfigurationProperties(prefix = "blink.web")
public class BlinkWebAppConfigProperties {

    /**
     * 是否使用ContextHolder
     */
    private Boolean enableContextHolder = true;


    private PreCache preCache = new PreCache();

    private Log log = new Log();

    @Data
    public static class Log{
        /**
         * 是否开启controller切面日志 开启则com.blink包下的所有controller生效
         */
        private Boolean enableControllerLog = true;

        /**
         * 入参日志长度 超过1000个字符省略 ....
         */
        private Integer upperLimit = 1000;

        /**
         * 日志入参超过长度自动省略 true 为省略 默认不省略
         */
        private Boolean autoSkip = false;

        /**
         * 是否开启敏感数据脱敏 默认关闭
         */
        private Boolean enableSensitive = false;
    }


    /**
     * 预加载配置
     */
    @Data
    public static class PreCache{

        /**
         *  全局预加载 默认预加载 数据字典
         */
        private Boolean enable = true;

        /**
         * 预加载缓存字典
         */
        private Boolean dictionary = true;


        /**
         * 预加载缓存提示消息
         */
        private Boolean errMsgInfo = true;

    }


}
