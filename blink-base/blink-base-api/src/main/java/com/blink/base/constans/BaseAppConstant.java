package com.blink.base.constans;

/**
 * 公共应用 常量
 */
public interface BaseAppConstant {

    /***************************错误码---start ********************************/

    /**
     * 该项超出范围
     */
    String PARAMETER_OUT_RANGE = "INVALID0001";


    /**
     * 该项不为空
     */
    String PARAMETER_NOT_NULL = "INVALID0002";


    /**
     * 密码格式不正确
     */
    String PASSWORD_FORMAT_ERR = "INVALID0003";


    /**
     * 两次密码不一致
     */
    String PASSWORD_CONFIRM_ERR = "INVALID0004";

    /**
     * 登入名重复
     */
    String LOGIN_NAME_REPEAT = "INVALID0005";

    /**
     * 邮箱格式不正确
     */
    String EMAIL_FORMAT_ERR = "INVALID0006";


    /**
     * 用户不存在
     */
    String USER_NOT_EXIST = "BUSS0001";

    /**
     * 组名称重复
     */
    String GROUP_ALREADY_EXIST = "BUSS0002";

    /**
     * 组的父节点不存在
     */
    String GROUP_PARENT_NOT_EXIST = "BUSS0003";

    /**
     * 当前组不存在
     */
    String GROUP_NOT_EXIST = "BUSS0004";

    /**
     * 在关联数据 无法删除
     */
    String HAVE_RELA_DATA = "BUSS0005";

    /**
     * 当前角色不存在
     */
    String ROLE_NOT_EXIST = "BUSS0006";


    /**
     * 当前菜单不存在
     */
    String MENU_NOT_EXIST = "BUSS0007";

    /**
     * 当前父菜单不存在
     */
    String MENU_PARENT_NOT_EXIST = "BUSS0008";

    /**
     * 存在子节点数据 无法删除
     */
    String HAVE_SON_DATA = "BUSS0009";

    /**
     * 权限标识重复
     */
    String PERMISSION_REPEAT = "BUSS0010";

    /**
     * 用户不存在
     */
    String USER_NOT_EXIT = "BUSS0011";

    /**
     * 密码错误
     */
    String INCORRECT_PASSWORD = "BUSS0012";

    /**
     * 用户已被锁定
     */
    String USER_LOCKED = "BUSS0013";

    /**
     * 验证失败
     */
    String INCORRECT_CAPTCHA = "BUSS0014";

    /**
     * 当前用户未分配角色 请分配
     */
    String DONT_HAVE_ANY_ROLE = "BUSS0015";

    /**
     * 当前用户未分配角色 请分配
     */
    String ROLE_ALREADY_EXIT = "BUSS0017";


    /***************************错误码---end ********************************/


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

    Long LONG_ZERO = Long.valueOf(0);

    //超级管理员角色 id 1
    Integer SUPER_ADMIN_ID = 1;
}
