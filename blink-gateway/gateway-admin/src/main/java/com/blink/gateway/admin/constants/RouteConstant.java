package com.blink.gateway.admin.constants;

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
}