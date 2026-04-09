package com.blink.gateway.base.constants;

public interface CommonConstants {

    //是否为叶子节点 0否 1是
    Integer IS_LEAF = 1;

    Integer NOT_LEAF = 0;

    //用户是否锁定 0未锁定 1 管理员锁定 2密码错误锁定
    Integer USER_LOCKED_ERR_PSW = 2;

    Integer USER_LOCKED_ADM = 1;

    Integer USER_LOCKED_NOT = 0;

    //目录菜单
    Byte MENU_DIRECTORY = 1;

    //页面菜单
    Byte MENU_PAGE = 2;

    //按钮/功能菜单
    Byte MENU_FUNCTION = 3;

    /**
     * 接口权限
     */
    Byte PERMISSION_API_TYPE = 1;

    /**
     * 数据权限
     */
    Byte PERMISSION_DATA_TYPE = 2;

    Long LONG_ZERO = Long.valueOf(0);

    //超级管理员角色 id 1
    Integer SUPER_ADMIN_ID = 1;

    //超级管理员角色代码
    String SUPER_ADMIN_CODE = "admin:super";

    //超级管理员角色标识
    String SUPER_ADMIN_ROLE_CODE = "superAdmin";

    //超级管理员 权限标识 代表全部权限
    String SUPER_ADMIN_PERMISSION = "*:**";

    //超级管理员标志 0-否 1-是
    Integer SUPER_ADMIN_NO = 0;

    Integer SUPER_ADMIN_YES = 1;

    /** 默认最大设备登录数 */
    Integer DEFAULT_MAX_DEVICES = 3;

    //开启
    Byte SWITCH_OPEN = 0;

    //关闭
    Byte SWITCH_CLOSE = 1;

    //网关配置组 id 5
    Integer GATEWAY_CONFIG_GROUP_ID = 5;

    /**
     * REDIS 消息状态码 未读
     */
    String REDIS_MSG_STATUS_UNREADED = "0";

    /**
     * REDIS 消息状态码 已读
     */
    String REDIS_MSG_STATUS_READED = "1";

    /**
     * REDIS 消息状态码 发送失败
     */
    String REDIS_MSG_STATUS_SEND_FAILED = "2";

    /**
     * REDIS 消息状态码  确认消费
     */
    String REDIS_MSG_STATUS_ACK = "3";

    /**
     * 密钥文件nacos上的dataid
     */
    String SECRET_CONFIG_DATA_ID = "secretConfig.json";

    /**
     * 密钥文件nacos上的 group
     */
    String SECRET_CONFIG_GROUP = "DEFAULT_GROUP";

    /**
     * 15分钟
     */
    Long LONG_MINUTES_15_OF_MILL = 900 * 1000L;

    /**
     * 15分钟
     */
    Long LONG_MINUTES_15 = 15L;

    /**
     * 系统默认标题
     */
    String DEFAULT_SYSTEM_TITLE = "Blink Gateway";

    /**
     * 系统默认logo 前端代码形式
     */
    String DEFAULT_SYSTEM_LOGO = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 48 48\"><defs><linearGradient id=\"bgGrad\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\"><stop offset=\"0%\" stop-color=\"#3b82f6\"/><stop offset=\"100%\" stop-color=\"#1d4ed8\"/></linearGradient></defs><circle cx=\"24\" cy=\"24\" r=\"22\" fill=\"url(#bgGrad)\"/><circle cx=\"24\" cy=\"24\" r=\"20\" fill=\"none\" stroke=\"#60a5fa\" stroke-width=\"1\" opacity=\"0.5\"/><path d=\"M14 24 C14 17 19 12 26 12 C33 12 38 17 38 24 C38 31 33 36 26 36 L26 28 L34 28\" fill=\"none\" stroke=\"#fff\" stroke-width=\"4\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/><path d=\"M8 24 L14 20 L14 28 Z\" fill=\"#fff\"/><path d=\"M40 24 L34 28 L34 20 Z\" fill=\"#fff\"/></svg>";

    /**
     * 系统默认页脚
     */
    String DEFAULT_SYSTEM_FOOTER = "© 2026 Blink Gateway Admin";

    /**
     * 用户默认密码
     */
    String DEFAULT_USER_PASSWORD = "123456";

    /**
     * 用户默认头像
     */
    String DEFAULT_USER_AVATAR = "adventurer-neutral";

    /**
     * 系统配置key常量
     */
    interface SysConfigKeys {
        /**
         * 登录验证码开关
         */
        String LOGIN_CAPTCHA_ENABLED = RedisKeyConstants.BLINK_PREFIX + "gate:admin:login:captcha:enabled";

        /**
         * 密码最大重试次数
         */
        String LOGIN_PASSWORD_MAX_RETRY = RedisKeyConstants.BLINK_PREFIX + "gate:admin:login:password:maxRetry";

        /**
         * 账户锁定时间(分钟)
         */
        String LOGIN_PASSWORD_LOCK_TIME = RedisKeyConstants.BLINK_PREFIX + "gate:admin:login:password:lockTime";

        /**
         * 会话超时时间(分钟)
         */
        String SESSION_TIMEOUT = RedisKeyConstants.BLINK_PREFIX + "gate:admin:session:timeout";

        /**
         * 密码最小长度
         */
        String USER_PASSWORD_MIN_LENGTH = RedisKeyConstants.BLINK_PREFIX + "gate:admin:user:passwordMinLength";

        /**
         * 密码需包含数字
         */
        String USER_PASSWORD_REQUIRE_NUMBER = RedisKeyConstants.BLINK_PREFIX + "gate:admin:user:passwordRequireNumber";

        /**
         * 密码需包含大写字母
         */
        String USER_PASSWORD_REQUIRE_UPPERCASE = RedisKeyConstants.BLINK_PREFIX + "gate:admin:user:passwordRequireUppercase";

        /**
         * 密码需包含小写字母
         */
        String USER_PASSWORD_REQUIRE_LOWERCASE = RedisKeyConstants.BLINK_PREFIX + "gate:admin:user:passwordRequireLowercase";

        /**
         * 密码需包含特殊字符
         */
        String USER_PASSWORD_REQUIRE_SPECIAL = RedisKeyConstants.BLINK_PREFIX + "gate:admin:user:passwordRequireSpecial";

        /**
         * 站点名称
         */
        String SITE_NAME = RedisKeyConstants.BLINK_PREFIX + "gate:admin:site:name";

        /**
         * 版权信息
         */
        String SITE_COPYRIGHT = RedisKeyConstants.BLINK_PREFIX + "gate:admin:site:copyright";

        /**
         * 分页大小
         */
        String PAGE_SIZE = RedisKeyConstants.BLINK_PREFIX + "gate:admin:page:size";

        /**
         * 密码复杂度要求
         */
        String PASSWORD_COMPLEXITY = RedisKeyConstants.BLINK_PREFIX + "gate:admin:password:complexity";

        /**
         * 允许上传的文件类型
         */
        String ALLOWED_FILE_TYPES = RedisKeyConstants.BLINK_PREFIX + "gate:admin:allowed:file:types";

        /**
         * 系统标题
         */
        String SYSTEM_TITLE = RedisKeyConstants.BLINK_PREFIX + "gate:admin:system:title";

        /**
         * 系统Logo
         */
        String SYSTEM_LOGO = RedisKeyConstants.BLINK_PREFIX + "gate:admin:system:logo";

        /**
         * 页脚信息
         */
        String SYSTEM_FOOTER = RedisKeyConstants.BLINK_PREFIX + "gate:admin:system:footer";

        /**
         * 上传文件最大大小(MB)
         */
        String UPLOAD_MAX_SIZE = RedisKeyConstants.BLINK_PREFIX + "gate:admin:upload:maxSize";

        /**
         * 允许上传的文件类型
         */
        String UPLOAD_ALLOW_TYPES = RedisKeyConstants.BLINK_PREFIX + "gate:admin:upload:allowTypes";

        /**
         * 用户默认头像
         */
        String USER_DEFAULT_AVATAR = RedisKeyConstants.BLINK_PREFIX + "gate:admin:user:defaultAvatar";

        /**
         * 用户初始密码
         */
        String USER_INIT_PASSWORD = RedisKeyConstants.BLINK_PREFIX + "gate:admin:user:initPassword";

        /**
         * 用户默认头像样式(DiceBear)
         */
        String USER_DEFAULT_AVATAR_STYLE = RedisKeyConstants.BLINK_PREFIX + "gate:admin:user:defaultAvatarStyle";

        /**
         * 最大并发会话数
         */
        String SESSION_MAX_CONCURRENT = RedisKeyConstants.BLINK_PREFIX + "gate:admin:session:maxConcurrent";

        /**
         * 踢出后登录
         */
        String SESSION_KICKOUT_AFTER = RedisKeyConstants.BLINK_PREFIX + "gate:admin:session:kickoutAfter";

        /**
         * 日志总开关
         */
        String LOG_ENABLED = RedisKeyConstants.BLINK_PREFIX + "gate:admin:log:enabled";

        /**
         * 启用操作日志
         */
        String LOG_ENABLE_OPERATION_LOG = RedisKeyConstants.BLINK_PREFIX + "gate:admin:log:enableOperationLog";

        /**
         * 启用登录日志
         */
        String LOG_ENABLE_LOGIN_LOG = RedisKeyConstants.BLINK_PREFIX + "gate:admin:log:enableLoginLog";

        /**
         * 日志保留天数
         */
        String LOG_RETENTION_DAYS = RedisKeyConstants.BLINK_PREFIX + "gate:admin:log:retentionDays";
    }
}
