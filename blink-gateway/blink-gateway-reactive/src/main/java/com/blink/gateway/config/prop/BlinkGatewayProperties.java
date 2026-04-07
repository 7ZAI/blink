package com.blink.gateway.config.prop;

import com.blink.gateway.event.EnableStreamEvent;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.blink.gateway.constant.GatewayConstant.*;
import static com.blink.gateway.constant.RedisConstans.*;

/**
 * gateway配置属性类
 *
 * @author binblink
 */
@RefreshScope
@ConfigurationProperties(prefix = "blink.gateway")
@Validated
public class BlinkGatewayProperties {

    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 配置文件在nacos中的id 用于监听配置文件改动
     */
    private String channelConfigId;

    /**
     * 配置文件在nacos中的组别
     */
    private String channelConfigGroup;

    /**
     * 动态路由配置类
     */
    private DynamicRoute dynamicroute;

    /**
     *  redis stream监听开关 默认关闭 因为长轮询有性能开销 只有需要时开启 所以支持 运行时开启或关闭 通过配置文件nacos监听实现
     * 值： open/close
     */
    private Boolean eventStreamEnable = false;

    /**
     * 实例id 用来区分多实例 取值00 01 02等等
     */
    private String instanceId;


    /**
     * 本地缓存开关 默认开启
     */
    private Boolean localCacheEnable = true;


    public Boolean getLocalCacheEnable() {
        return localCacheEnable;
    }

    public void setLocalCacheEnable(Boolean localCacheEnable) {
        this.localCacheEnable = localCacheEnable;
    }


    public DynamicRoute getDynamicroute() {
        return dynamicroute;
    }

    public void setDynamicroute(DynamicRoute dynamicroute) {
        this.dynamicroute = dynamicroute;
    }

    public String getChannelConfigId() {
        return channelConfigId;
    }

    public void setChannelConfigId(String channelConfigId) {
        this.channelConfigId = channelConfigId;
    }

    public String getChannelConfigGroup() {
        return channelConfigGroup;
    }

    public void setChannelConfigGroup(String channelConfigGroup) {
        this.channelConfigGroup = channelConfigGroup;
    }

    public Boolean getEventStreamEnable() {
        return eventStreamEnable;
    }

    public void setEventStreamEnable(Boolean eventStreamEnable) {
        Boolean oldVal = this.eventStreamEnable;
        this.eventStreamEnable = eventStreamEnable;
        if(!eventStreamEnable.equals(oldVal) ){
            eventPublisher.publishEvent(new EnableStreamEvent(eventStreamEnable));
        }
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(@NotBlank(message = "实例id不能为空") String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * 获取消费者组名称
     * 格式：appName:instanceId
     *
     * @return 消费者组名称
     */
    public String getGroupName() {
        return "gateway-reactive:" + this.instanceId;
    }

    /**
     * 动态路由配置
     *
     */
    public static class DynamicRoute {

        /**
         * 动态路由模式：nacos 或 redis
         */
        private String mode = "nacos";

        /**
         * Nacos配置相关属性
         */
        private Nacos nacos = new Nacos();


        private Redis redis = new Redis();

        // getter和setter方法
        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Nacos getNacos() {
            return nacos;
        }

        public void setNacos(Nacos nacos) {
            this.nacos = nacos;
        }

        public Redis getRedis() {
            return redis;
        }

        public void setRedis(Redis redis) {
            this.redis = redis;
        }

        /**
         * 动态路由 Nacos实现相关配置属性
         *
         */
        public static class Nacos {
            private String dataId = NACOS_GATEWAY_ROUTES_DEFAULT_DATAID;
            private String group = NACOS_GATEWAY_ROUTES_DEFAULT_GROUP;

            // getter和setter
            public String getDataId() {
                return dataId;
            }

            public void setDataId(String dataId) {
                this.dataId = dataId;
            }

            public String getGroup() {
                return group;
            }

            public void setGroup(String group) {
                this.group = group;
            }
        }

        /**
         * 动态路由R edis实现相关配置属性
         *
         */
        public static class Redis {

            private final String ROUTE_PROFIX = GATEWAY_DYNAMIC_ROUTES;

            /**
             * 存放route配置的 key
             * 此属性在多gateway实例中 用来区分路由配置 多个实例routeKey相同则共享路由配置
             * 相当于在Nacos中 GROUP 的作用
             */
            private String routeKey;

            private String routeSuffix = "default";


            public String getRouteSuffix() {
                return routeSuffix;
            }

            public String getRouteKey() {
                return ROUTE_PROFIX + this.routeSuffix;
            }

            public void setRouteSuffix(String routeSuffix) {

                this.routeSuffix = routeSuffix;
            }


        }
    }


}