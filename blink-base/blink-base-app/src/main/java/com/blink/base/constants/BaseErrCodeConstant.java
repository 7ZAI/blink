package com.blink.base.constants;

/**
 * 公共应用 常量
 *
 * @author binblink
 */
public interface BaseErrCodeConstant {

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
     * 角色不存在
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
     * 权限标识已存在
     */
    String PERMISSION_IDENTITY_REPEAT = "BUSS0056";

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

    /**
     * 角色名称已存在
     */
    String ROLE_NAME_ALREADY_EXIT = "BUSS0050";

    /**
     * 名称已存在 名称重复
     */
    String CHANNEL_NAME_ALREADY_EXIT = "BUSS0018";

    /**
     * 启用的渠道无法删除 请关闭再删除
     */
    String CHANNEL_NOT_ALLOW_DELETE = "BUSS0019";

    /**
     * 数据不存在
     */
    String DATA_NOT_EXIST = "BUSS0020";


    /**
     * 所有参数为空
     */
    String ALL_PARAME_IS_NULL = "BUSS0021";

    /**
     * 渠道信息不存在
     */
    String CHANNEL_NOT_EXIST = "BUSS0022";

    /**
     * 参数key命名重复
     */
    String CONFIG_NAME_REPEAT = "BUSS0023";


    /**
     * 权限不存在
     */
    String PERMISSION_NOT_EXIST = "BUSS0024";

    /**
     * 不存在该参数
     */
    String CONFIG_NOT_EXIST = "BUSS0025";

    /**
     * 该错误消息不存在
     */
    String ERR_MSG_NOT_EXIST = "BUSS0026";

    /**
     * 超级管理员无法删除
     */
    String NOT_ALLOW_DELETE = "BUSS0027";

    /**
     * 密钥不匹配
     */
    String ERR_APP_SECRET = "BUSS0028";

    /**
     * 首页菜单不可删除
     */
    String HOME_MENU_NOT_ALLOW_DELETE = "BUSS0029";

    /**
     * 超级管理员角色无法修改
     */
    String SUPER_ADMIN_ROLE_NOT_ALLOW_UPDATE = "BUSS0030";

    /**
     * 超级管理员角色无法分配
     */
    String SUPER_ADMIN_ROLE_NOT_ALLOW_ASSIGN = "BUSS0031";

    /**
     * 只有超级管理员才能分配超级管理员角色
     */
    String ONLY_SUPER_ADMIN_CAN_ASSIGN = "BUSS0032";

    /**
     * 字典数据不存在
     */
    String DICT_DATA_NOT_EXIST = "BUSS0033";

    /**
     * 字典标签重复
     */
    String DICT_LABEL_REPEAT = "BUSS0034";

    /**
     * 字典值重复
     */
    String DICT_VALUE_REPEAT = "BUSS0035";

    /**
     * 字典类型不存在
     */
    String DICT_TYPE_NOT_EXIST = "BUSS0036";

    /**
     * 字典类型编码重复
     */
    String DICT_TYPE_REPEAT = "BUSS0037";

    /**
     * 用户已下线或token无效
     */
    String TOKEN_EXPIRED = "BUSS0038";

    /**
     * 超级管理员密码无法重置
     */
    String SUPER_ADMIN_NOT_ALLOW_RESET = "BUSS1038";

    // ==================== 登入授权错误码 ====================
    /**
     * 请完成验证码验证
     */
    String CAPTCHA_NOT_VALID = "AUTH0001";

    /**
     * 验证码已失效，请重新验证
     */
    String CAPTCHA_EXPIRED = "AUTH0002";

    // ==================== 工作流相关错误码 ====================

    /**
     * 流程定义不存在
     */
    String PROCESS_DEF_NOT_FOUND = "FLOW0001";

    /**
     * 流程实例不存在
     */
    String PROCESS_INSTANCE_NOT_FOUND = "FLOW0002";

    /**
     * 任务不存在
     */
    String TASK_NOT_FOUND = "FLOW0003";

    /**
     * 无权限处理该任务
     */
    String NO_TASK_PERMISSION = "FLOW0004";

    /**
     * 部署流程定义失败
     */
    String DEPLOY_PROCESS_ERROR = "FLOW0005";

    /**
     * 查询流程定义列表失败
     */
    String QUERY_PROCESS_DEF_ERROR = "FLOW0006";

    /**
     * 获取流程图XML失败
     */
    String GET_DIAGRAM_XML_ERROR = "FLOW0007";

    /**
     * 获取流程图图片失败
     */
    String GET_DIAGRAM_IMAGE_ERROR = "FLOW0008";

    /**
     * 启动流程实例失败
     */
    String START_PROCESS_ERROR = "FLOW0009";

    /**
     * 查询流程实例列表失败
     */
    String QUERY_PROCESS_INSTANCE_ERROR = "FLOW0010";

    /**
     * 查询用户发起的流程实例失败
     */
    String QUERY_MY_PROCESS_ERROR = "FLOW0011";

    /**
     * 获取流程实例详情失败
     */
    String GET_PROCESS_INSTANCE_ERROR = "FLOW0012";

    /**
     * 删除流程实例失败
     */
    String DELETE_PROCESS_ERROR = "FLOW0013";

    /**
     * 查询用户待办任务失败
     */
    String QUERY_TASK_ERROR = "FLOW0014";

    /**
     * 分页查询待办任务失败
     */
    String QUERY_PENDING_TASK_ERROR = "FLOW0015";

    /**
     * 分页查询已办任务失败
     */
    String QUERY_COMPLETED_TASK_ERROR = "FLOW0016";

    /**
     * 完成任务失败
     */
    String COMPLETE_TASK_ERROR = "FLOW0017";

    /**
     * 委托任务失败
     */
    String DELEGATE_TASK_ERROR = "FLOW0018";

    /**
     * 认领任务失败
     */
    String CLAIM_TASK_ERROR = "FLOW0019";

    /**
     * 取消认领任务失败
     */
    String UNCLAIM_TASK_ERROR = "FLOW0020";

    /**
     * 查询流程历史失败
     */
    String QUERY_HISTORY_ERROR = "FLOW0021";

    /**
     * 撤回任务失败
     */
    String WITHDRAW_TASK_ERROR = "FLOW0022";

    /**
     * 回退流程失败
     */
    String ROLLBACK_PROCESS_ERROR = "FLOW0023";

    /**
     * 导入BPMN XML失败
     */
    String XML_IMPORT_ERROR = "FLOW0024";

    /**
     * 流程图生成失败
     */
    String GENERATE_DIAGRAM_ERROR = "FLOW0025";

    /**
     * 挂起流程定义失败
     */
    String SUSPEND_PROCESS_ERROR = "FLOW0026";

    /**
     * 激活流程定义失败
     */
    String ACTIVATE_PROCESS_ERROR = "FLOW0027";

    // ==================== Dubbo服务相关错误码 ====================

    /**
     * 用户权限不存在
     */
    String USER_PERMISSION_NOT_EXIST = "BUSS0038";

    /**
     * 路径权限不存在
     */
    String PATH_PERMISSION_NOT_EXIST = "BUSS0039";

    /**
     * 接口权限不存在
     */
    String API_PERMISSION_NOT_EXIST = "BUSS0045";

    /**
     * 操作日志不存在
     */
    String OPERATION_LOG_NOT_EXIST = "BUSS0046";

    /**
     * 用户无需重置密码
     */
    String PASSWORD_RESET_NOT_REQUIRED = "BUSS0048";

    /**
     * 只有超级管理员才能修改超级管理员信息
     */
    String ONLY_SUPER_ADMIN_CAN_MODIFY = "BUSS0049";

    // ==================== 数据范围权限错误码 ====================

    /**
     * 数据范围规则不存在
     */
    String DATA_SCOPE_RULE_NOT_FOUND = "BUSS0050";

    /**
     * 数据范围规则配置无效
     */
    String DATA_SCOPE_RULE_CONFIG_INVALID = "BUSS0051";

    /**
     * 数据范围SQL片段无效（包含非法字符）
     */
    String DATA_SCOPE_SQL_FRAGMENT_INVALID = "BUSS0052";

    /**
     * 实体类未注册数据范围映射
     */
    String DATA_SCOPE_ENTITY_NOT_REGISTERED = "BUSS0053";

    /**
     * 数据过滤规则正在被权限使用，无法删除
     */
    String DATA_FILTER_IN_USE = "BUSS0054";

    /**
     * 规则配置内容无效（如字段过滤未选择任何字段）
     */
    String DATA_SCOPE_RULE_CONFIG_EMPTY = "BUSS0055";

    /**
     * 数据过滤规则不存在
     */
    String DATA_FILTER_NOT_EXIST = "BUSS0057";

    /**
     * 查询简化用户列表失败
     */
    String SIMPLE_USER_LIST_QUERY_FAILED = "BUSS0060";

    /**
     * 查询用户权限详情失败
     */
    String USER_PERMISSION_DETAIL_QUERY_FAILED = "BUSS0061";

    // ==================== 请假管理错误码 ====================

    /**
     * 请假申请不存在
     */
    String LEAVE_REQUEST_NOT_EXIST = "LEAVE0001";

    /**
     * 请假申请已处理，无法再次审批
     */
    String LEAVE_ALREADY_PROCESSED = "LEAVE0002";

    /**
     * 无权审批该请假申请
     */
    String NO_LEAVE_APPROVAL_PERMISSION = "LEAVE0003";

    /**
     * 只有申请人可以取消请假申请
     */
    String ONLY_APPLICANT_CAN_CANCEL = "LEAVE0004";

    /**
     * 只有待审批状态的请假申请可以取消
     */
    String LEAVE_CANNOT_CANCEL = "LEAVE0005";


    /***************************错误码---end ********************************/



}
