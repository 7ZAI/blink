package com.blink.gateway.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.blink.gateway.constant.GatewayConstant.*;

/**
 * gateway配置属性类
 *
 * @author binblink
 */
@ConfigurationProperties(prefix = "blink.gateway")
public class BlinkGatewayProperties {

    /**
     * 配置文件在nacos中的id 用于监听配置文件改动
     */
    private String configDataId;

    /**
     * 配置文件在nacos中的组别
     */
    private String configGroup;

    /**
     * 动态路由配置类
     */
    private DynamicRoute dynamicroute;

    /**
     * 缓存配置
     */
    private Cache cache;

    /**
     * ip过滤器配置
     */
    private IpFilter ipFilter;


    public DynamicRoute getDynamicroute() {
        return dynamicroute;
    }

    public void setDynamicroute(DynamicRoute dynamicroute) {
        this.dynamicroute = dynamicroute;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    public String getConfigDataId() {
        return configDataId;
    }

    public void setConfigDataId(String configDataId) {
        this.configDataId = configDataId;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public IpFilter getIpFilter() {
        return ipFilter;
    }

    public void setIpFilter(IpFilter ipFilter) {
        this.ipFilter = ipFilter;
    }
    /**
     * 缓存配置
     *
     */
    public static class Cache {

        /**
         * 本地缓存开关 默认开启
         */
        private Boolean localCacheEnable = true;

        /**
         * 本地缓存同步 redis stream监听开关 默认关闭 因为长轮询有性能开销 只有需要时开启 所以支持 运行时开启或关闭 通过配置文件nacos监听实现
         * 值： open/close
         */
        private Boolean syncListenerOpen = false;

        private String streamKey = REDIS_STREAM_CACHE_KEY;

        private String streamGroupName;

        public Boolean getSyncListenerOpen() {
            return syncListenerOpen;
        }

        public String getStreamKey() {
            return streamKey;
        }

        public String getStreamGroupName() {
            return streamGroupName;
        }

        public Boolean getLocalCacheEnable() {
            return localCacheEnable;
        }

        public void setLocalCacheEnable(Boolean localCacheEnable) {
            this.localCacheEnable = localCacheEnable;
        }

        public void setSyncListenerOpen(Boolean syncListenerOpen) {
            this.syncListenerOpen = syncListenerOpen;
        }

        public void setStreamKey(String streamKey) {
            this.streamKey = streamKey;
        }

        public void setStreamGroupName(String streamGroupName) {
            this.streamGroupName = streamGroupName;
        }
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
            private final String routeKey = ROUTE_PROFIX + getRouteSuffix();

            private String routeSuffix = "default";

            /**
             * 同步路由的streamKey
             */
            private String streamkey;

            /**
             * 组id 用于streamKey消费 实现消息广播规则则 多个gateway实例需要配置不同的groupId
             */
            private String groupId;


            public String getRouteSuffix() {
                return routeSuffix;
            }

            public String getRouteKey() {
                return routeKey;
            }

            public String getGroupId() {
                return groupId;
            }

            public String getStreamkey() {
                return streamkey;
            }

            public void setRouteSuffix(String routeSuffix) {
                this.routeSuffix = routeSuffix;
            }

            public void setStreamkey(String streamkey) {
                this.streamkey = streamkey;
            }

            public void setGroupId(String groupId) {
                this.groupId = groupId;
            }
        }
    }

    /**
     * ip过滤器 配置类
     *
     */
    public static class IpFilter {



    }

}