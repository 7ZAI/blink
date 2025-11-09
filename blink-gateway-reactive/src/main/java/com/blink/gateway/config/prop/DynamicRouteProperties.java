package com.blink.gateway.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.blink.gateway.constant.GatewayConstant.*;

@ConfigurationProperties(prefix = "blink.gateway.dynamicroute")
public class DynamicRouteProperties {

    /**
     * 动态路由模式：nacos 或 redis
     */
    private String mode = "nacos";

    /**
     * Nacos配置相关属性
     */
    private Nacos nacos = new Nacos();

    /**
     * Redis配置相关属性
     */
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

    public static class Redis {

        private final String ROUTE_PROFIX = GATEWAY_DYNAMIC_ROUTES;

        /**
         * 存放route配置的 key
         * 此属性在多gateway实例中 用来区分路由配置 多个实例routeKey相同则共享路由配置
         * 相当于在Nacos中 GROUP 的作用
         */
        private final String routeKey = ROUTE_PROFIX + getRouteSuffix();

        private String routeSuffix = "default";

        /**
         * 同步路由的streamKey
         */
        private String streamkey;

        /**
         * 组id 用于streamKey消费 实现消息广播则 多个gateway实例需要配置不同的groupId
         */
        private String groupId;



        // getter和setter

        public String getRouteSuffix() {
            return routeSuffix;
        }

        public void setRouteSuffix(String routeSuffix) {
            this.routeSuffix = routeSuffix;
        }

        public String getRouteKey() {
            return routeKey;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getStreamkey() {
            return streamkey;
        }

        public void setStreamkey(String streamkey) {
            this.streamkey = streamkey;
        }
    }
}