package com.blink.base.constans;

public interface CommonConstans {

    //是否为叶子节点 0否 1是
    Integer IS_LEAF = 1;

    Integer NOT_LEAF = 0;

    //用户是否锁定 0未锁定 1 管理员锁定 2密码错误锁定
    Integer USER_LOCKED_ERR_PSW = 2;

    Integer USER_LOCKED_ADM = 1;

    Integer USER_LOCKED_NOT  = 0;

    //导航菜单
    Byte MENU_ORIGIN = 0;

    //功能菜单
    Byte MENU_FUNCTION = 1;

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

    //超级管理员角色 id 1
    String SUPER_ADMIN_CODE = "admin:super";
    //超级管理员 权限标识 代码全部权限

    String SUPER_ADMIN_PERMISSION = "*:**";

    //开启
    Byte SWITCH_OPEN = 0;

    //关闭
    Byte SWITCH_CLOSE = 1;

    //网关配置组 id 5
    Integer GATEWAY_CONFIG_GROUP_ID = 5;

    /**
     *  REDIS 消息状态码 未读
     */
    String REDIS_MSG_STATUS_UNREADED = "0";

    /**
     *  REDIS 消息状态码 已读
     */
    String REDIS_MSG_STATUS_READED = "1";

    /**
     *  REDIS 消息状态码 发送失败
     */
    String REDIS_MSG_STATUS_SEND_FAILED = "2";

    /**
     *  REDIS 消息状态码  确认消费
     */
    String REDIS_MSG_STATUS_ACK = "3";

    /**
     * 密钥文件nacos上的dataid
     */
    String SECRET_CONFIG_DATA_ID= "secretConfig.json";

    /**
     * 密钥文件nacos上的 group
     */
    String SECRET_CONFIG_GROUP= "DEFAULT_GROUP";
}
