package com.blink.gateway.admin.constants;

import java.util.Set;

/**
 * 路由相关常量
 *
 * @author binblink
 * @since 2026-04-11
 */
public interface RouteConstant {

    /**
     * 存储方式：Redis
     */
    String STORAGE_MODE_REDIS = "redis";

    /**
     * 存储方式：Nacos
     */
    String STORAGE_MODE_NACOS = "nacos";

    /**
     * 默认路由分组
     */
    String DEFAULT_ROUTES_GROUP = "default";

    /**
     * 默认 Nacos Group
     */
    String DEFAULT_NACOS_GROUP = "DEFAULT_GROUP";

    /**
     * 默认 Nacos Data ID
     */
    String DEFAULT_NACOS_DATA_ID = "gateway-routes.json";

    // ==================== Nacos 路由配置常量 ====================

    /**
     * Nacos 路由配置文件前缀
     */
    String NACOS_ROUTE_CONFIG_PREFIX = "gateway-routes";

    /**
     * Nacos 路由配置文件后缀
     */
    String NACOS_ROUTE_CONFIG_SUFFIX = ".json";

    /**
     * Nacos 路由配置文件 Group
     */
    String NACOS_ROUTE_CONFIG_GROUP = "DEFAULT_GROUP";

    /**
     * 状态：启用
     */
    Byte STATUS_ENABLE = 1;

    /**
     * 状态：禁用
     */
    Byte STATUS_DISABLE = 0;

    /**
     * 操作类型：新增
     */
    String OPERATION_ADD = "A";

    /**
     * 操作类型：修改
     */
    String OPERATION_MODIFY = "M";

    /**
     * 操作类型：删除
     */
    String OPERATION_DELETE = "D";

    /**
     * 推送模式：广播
     */
    String PUSH_MODE_BROADCAST = "broadcast";

    /**
     * 推送模式：指定实例
     */
    String PUSH_MODE_SPECIFIED = "specified";

    /**
     * 推送结果：成功
     */
    Byte PUSH_RESULT_SUCCESS = 0;

    /**
     * 推送结果：部分失败
     */
    Byte PUSH_RESULT_PARTIAL_FAILED = 1;

    /**
     * 推送结果：失败
     */
    Byte PUSH_RESULT_FAILED = 2;

    // ==================== URI前缀常量 ====================

    /**
     * URI前缀：负载均衡
     */
    String URI_PREFIX_LB = "lb://";

    /**
     * URI前缀：HTTP
     */
    String URI_PREFIX_HTTP = "http://";

    /**
     * URI前缀：HTTPS
     */
    String URI_PREFIX_HTTPS = "https://";

    // ==================== 推送状态常量 ====================

    /**
     * 推送状态：未推送
     */
    Byte PUSH_STATUS_NOT_PUSHED = 0;

    /**
     * 推送状态：已推送
     */
    Byte PUSH_STATUS_PUSHED = 1;

    /**
     * 推送状态：推送失败
     */
    Byte PUSH_STATUS_PUSH_FAILED = 2;

    // ==================== 确认状态常量 ====================

    /**
     * 确认状态：待确认
     */
    Byte CONFIRM_STATUS_PENDING = 0;

    /**
     * 确认状态：已确认
     */
    Byte CONFIRM_STATUS_CONFIRMED = 1;

    /**
     * 确认状态：超时
     */
    Byte CONFIRM_STATUS_TIMEOUT = 2;

    // ==================== 重试配置常量 ====================

    /**
     * 最大推送重试次数
     */
    Integer MAX_PUSH_RETRY_TIMES = 3;

    /**
     * 推送重试间隔（毫秒）
     */
    Long PUSH_RETRY_INTERVAL_MS = 100L;

    // ==================== 支持的断言类型 ====================

    /**
     * 支持的断言类型集合
     */
    Set<String> SUPPORTED_PREDICATES = Set.of(
        "Path", "Method", "Header", "Query", "Host", "Weight",
        "After", "Before", "Between", "Cookie", "RemoteAddr"
    );

    // ==================== 支持的过滤器类型 ====================

    /**
     * 支持的过滤器类型集合
     */
    Set<String> SUPPORTED_FILTERS = Set.of(
        "StripPrefix", "AddRequestHeader", "RemoveRequestHeader",
        "AddResponseHeader", "RemoveResponseHeader", "RewritePath",
        "SetPath", "RedirectTo", "Retry", "RequestRateLimiter",
        "CircuitBreaker", "FallbackHeaders", "PrefixPath"
    );

    // ==================== 字段名常量（用于历史记录变更字段） ====================

    /**
     * 字段名：路由名称
     */
    String FIELD_ROUTE_NAME = "routeName";

    /**
     * 字段名：目标URI
     */
    String FIELD_URI = "uri";

    /**
     * 字段名：断言配置
     */
    String FIELD_PREDICATES = "predicates";

    /**
     * 字段名：过滤器配置
     */
    String FIELD_FILTERS = "filters";

    /**
     * 字段名：路由顺序
     */
    String FIELD_ORDER_NUM = "orderNum";

    /**
     * 字段名：元数据
     */
    String FIELD_METADATA = "metadata";

    /**
     * 字段名：路由分组
     */
    String FIELD_ROUTES_GROUP = "routesGroup";

    /**
     * 字段名：存储方式
     */
    String FIELD_STORAGE_MODE = "storageMode";

    /**
     * 字段名：状态
     */
    String FIELD_STATUS = "status";

    // ==================== 推送状态描述常量 ====================

    /**
     * 推送状态描述：未知
     */
    String PUSH_STATUS_DESC_UNKNOWN = "未知";

    /**
     * 推送状态描述：未推送
     */
    String PUSH_STATUS_DESC_NOT_PUSHED = "未推送";

    /**
     * 推送状态描述：已推送
     */
    String PUSH_STATUS_DESC_PUSHED = "已推送";

    /**
     * 推送状态描述：推送失败
     */
    String PUSH_STATUS_DESC_PUSH_FAILED = "推送失败";

    // ==================== 备注常量 ====================

    /**
     * 克隆路由名称后缀
     */
    String CLONED_ROUTE_NAME_SUFFIX = "-克隆";

    /**
     * 回滚推送备注前缀
     */
    String REMARK_ROLLBACK_PUSH_PREFIX = "回滚推送，原 pushId: ";

    /**
     * 全量推送备注
     */
    String REMARK_FULL_PUSH = "全量推送";

    // ==================== 验证结果常量 ====================

    /**
     * 验证结果：一致
     */
    Byte VERIFY_RESULT_CONSISTENT = 0;

    /**
     * 验证结果：部分不一致
     */
    Byte VERIFY_RESULT_PARTIAL_MISMATCH = 1;

    /**
     * 验证结果：完全不一致
     */
    Byte VERIFY_RESULT_MISMATCH = 2;

    // ==================== 实例配置文件常量 ====================

    /**
     * 实例配置文件前缀
     * 命名规则：gateway-instance-{instanceId}.yaml
     */
    String INSTANCE_CONFIG_PREFIX = "gateway-instance-";

    /**
     * 实例配置文件后缀
     */
    String INSTANCE_CONFIG_SUFFIX = ".yaml";

    /**
     * 实例配置文件中的配置路径：blink.gateway
     */
    String INSTANCE_CONFIG_PATH = "blink.gateway";

    /**
     * 实例配置字段：动态路由配置
     */
    String INSTANCE_CONFIG_DYNAMIC_ROUTE = "dynamicRoute";

    /**
     * 实例配置字段：路由模式（redis/nacos）
     */
    String INSTANCE_CONFIG_ROUTE_MODE = "mode";

    /**
     * 实例配置字段：路由分组
     */
    String INSTANCE_CONFIG_ROUTE_GROUP = "group";
}