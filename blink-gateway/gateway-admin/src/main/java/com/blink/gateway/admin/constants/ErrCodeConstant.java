package com.blink.gateway.admin.constants;

/**
 * 错误码常量
 *
 * @author binblink
 */
public interface ErrCodeConstant {

    // ============ 业务错误码 ============

    /**
     * 渠道名称已存在
     */
    String CHANNEL_NAME_ALREADY_EXIT = "GATE0018";

    /**
     * 启用的渠道无法删除
     */
    String CHANNEL_NOT_ALLOW_DELETE = "GATE0019";

    /**
     * 数据不存在
     */
    String DATA_NOT_EXIST = "GATE0020";

    /**
     * 参数key命名重复
     */
    String CONFIG_NAME_REPEAT = "GATE0023";

    /**
     * 不存在该参数
     */
    String CONFIG_NOT_EXIST = "GATE0025";

    /**
     * 渠道信息不存在
     */
    String CHANNEL_NOT_EXIST = "GATE0022";

    /**
     * 参数不能为空
     */
    String PARAMETER_NOT_NULL = "PARAM0001";

    /**
     * 密钥不匹配
     */
    String ERR_APP_SECRET = "GATE0028";

    // ============ 网关相关错误码 ============

    /**
     * 网关实例不存在
     */
    String GATEWAY_INSTANCE_NOT_EXIST = "GATE0001";

    /**
     * 网关实例已下线
     */
    String GATEWAY_INSTANCE_SHUTDOWN = "GATE0002";

    /**
     * 配置推送失败
     */
    String CONFIG_PUSH_FAILED = "GATE0003";

    /**
     * 数据同步失败
     */
    String DATA_SYNC_FAILED = "GATE0004";

    /**
     * 路由组不能为空
     */
    String ROUTE_GROUP_EMPTY = "GATE0005";

    /**
     * 保存路由失败
     */
    String SAVE_ROUTE_FAILED = "GATE0006";

    /**
     * 删除路由失败
     */
    String DELETE_ROUTE_FAILED = "GATE0007";

    /**
     * 线程中断
     */
    String THREAD_INTERRUPTED = "GATE0008";

    /**
     * 获取网关指标失败
     */
    String GET_METRICS_FAILED = "GATE0009";

    /**
     * 获取网关实例列表失败
     */
    String GET_INSTANCE_LIST_FAILED = "GATE0010";

    /**
     * 获取网关实例详情失败
     */
    String GET_INSTANCE_DETAIL_FAILED = "GATE0011";

    /**
     * 网关实例下线失败
     */
    String OFFLINE_INSTANCE_FAILED = "GATE0012";

    /**
     * 网关实例上线失败
     */
    String ONLINE_INSTANCE_FAILED = "GATE0013";

    // ============ 参数校验错误码 ============

    /**
     * DataId不能为空
     */
    String DATA_ID_EMPTY = "GATE0014";

    /**
     * 配置内容不能为空
     */
    String CONFIG_CONTENT_EMPTY = "GATE0015";

    /**
     * 历史ID不能为空
     */
    String HISTORY_ID_EMPTY = "GATE0016";

    // ============ 用户模块错误码 GATE0100-GATE0109 ============

    /**
     * 用户不存在
     */
    String USER_NOT_EXIST = "GATE0100";

    /**
     * 用户密码错误
     */
    String USER_PASSWORD_ERROR = "GATE0101";

    /**
     * 用户已锁定
     */
    String USER_LOCKED = "GATE0102";

    /**
     * 用户已禁用
     */
    String USER_DISABLED = "GATE0103";

    /**
     * 登录名已存在
     */
    String LOGIN_NAME_EXISTS = "GATE0104";

    /**
     * 原密码错误
     */
    String OLD_PASSWORD_ERROR = "GATE0105";

    /**
     * 密码长度不足
     */
    String PWD_TOO_SHORT = "GATE0106";

    /**
     * 密码复杂度不满足
     */
    String PWD_COMPLEXITY_FAIL = "GATE0107";

    // ============ 角色模块错误码 GATE0110-GATE0119 ============

    /**
     * 角色不存在
     */
    String ROLE_NOT_EXIST = "GATE0110";

    /**
     * 角色名称已存在
     */
    String ROLE_NAME_EXISTS = "GATE0111";

    /**
     * 角色下存在用户
     */
    String ROLE_HAS_USERS = "GATE0112";

    // ============ 菜单模块错误码 GATE0120-GATE0129 ============

    /**
     * 菜单不存在
     */
    String MENU_NOT_EXIST = "GATE0120";

    /**
     * 菜单存在子菜单
     */
    String MENU_HAS_CHILDREN = "GATE0121";

    /**
     * 菜单名称已存在
     */
    String MENU_NAME_EXISTS = "GATE0122";

    // ============ 认证模块错误码 GATE0130-GATE0139 ============

    /**
     * Token无效
     */
    String TOKEN_INVALID = "GATE0130";

    /**
     * Token已过期
     */
    String TOKEN_EXPIRED = "GATE0131";

    /**
     * 权限不足
     */
    String PERMISSION_DENIED = "GATE0132";

    // ============ 密钥配置错误码 GATE0140-GATE0149 ============

    /**
     * 获取密钥配置文件失败
     */
    String SECRET_CONFIG_GET_FAILED = "GATE0140";

    /**
     * 渠道密钥配置不存在
     */
    String CHANNEL_SECRET_NOT_EXIST = "GATE0141";

    /**
     * 密钥配置文件为空
     */
    String SECRET_CONFIG_EMPTY = "GATE0142";

    /**
     * 刷新密钥失败
     */
    String REFRESH_SECRET_FAILED = "GATE0143";

    // ============ 消息通知错误码 GATE0150-GATE0159 ============

    /**
     * 消息通知不存在
     */
    String NOTIFICATION_NOT_EXIST = "GATE0150";

    /**
     * 消息已阅读
     */
    String NOTIFICATION_ALREADY_READ = "GATE0151";

    /**
     * SSE连接失败
     */
    String SSE_CONNECTION_FAILED = "GATE0152";
}
