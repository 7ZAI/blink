package com.blink.framework.common.exception;

import com.blink.framework.common.constrant.SysConstant;

public enum BlinkErrorCodeEnum implements ErrorCode{

    /**
     * 交易成功
     */
    BLINK_SUCCESS(SysConstant.SUCCESS_CODE),
    /**
     * 交易失败
     */
    BLINK_ERROR(SysConstant.FAIL_CODE),
    /**
     * token失效,请重新登陆
     */
    BLINK_TOKEN_INVALID("BLINK0002"),
    /**
     * token必须输入
     */
    BLINK_TOKEN_MUST_INPUT("BLINK0003"),
    /**
     * 用户ID必须输入
     */
    BLINK_USERID_MUST_INPUT("BLINK0004"),
    /**
     * 文件没有找到
     */
    BLINK_FILE_NOT_FOUND_ERROR("BLINK0008"),
    /**
     * 找不到对应的冲正交易
     */
    BLINK_RVS_TXN_NOT_FOUND_ERROR("BLINK0009"),
    /**
     * 属性拷贝失败
     */
    BEAN_COPY_ERROR("SYS00200"),
    /**
     * rabbitmq接收消息处理失败
     */
    RABBITMQ_RECEIVE_HANDLE_ERROR("SYS00201"),
    /**
     * rabbitmq发送消息失败
     */
    RABBITMQ_SEND_ERROR("SYS00202"),
    /**
     * rabbitmq接收消息代码有误
     */
    RABBITMQ_RECEIVE_CODE_ERROR("SYS00203"),

    /**
     * 业务异常消息码
     */
    BUSINESS_ERROR("BUSS00001"),

    /**
     * 系统异常消息码
     */
    SYS_ERROR("SYS00001"),
    /**
     * 访问数据库异常
     */
    ACCESS_DATABASE_ERROR("SYS00002"),
    /**
     * 签名异常
     */
    SIGNATURE_EXCEPTION("SYS00003"),
    /**
     * 401异常
     */
    NO_AUTH_ERROR("SYS00401"),
    /**
     * 403禁止操作
     */
    FORBIDDEN_OPERATION("SYS00403", "FORBIDDEN_OPERATION"),
    /**
     * 404异常
     */
    NO_HANDLER_FOUND_ERROR("SYS00404"),
    /**
     * 405方法不对
     */
    METHOD_NOT_ALLOWED("SYS00405"),
    /**
     * 线程池满异常
     */
    THREAD_POOL_FULL_ERROR("SYS00004"),

    /**
     * 服务端404错误
     */
    SERVER_RESOURCE_NOT_FOUND("SYS00006"),
    /**
     * 服务不可用
     */
    SERVER_NOT_AVAILABLE("SYS00007"),
    /**
     * 累计操作异常
     */
    CUMULATIVE_ERROR("SYS00101"),
    /**
     * bean validation exception
     */
    BEAN_VALIDATION_ERROR("SYS10001"),

    /**
     * UnknownHostException
     */
    CLIENT_EXCEPTION_UNKNOWN_HOST("SYS20001"),
    /**
     * timeout excepiton
     */
    CLIENT_TIMEOUT("SYS20002"),
    /**
     * illegal parameter
     */
    ILLEGAL_PARAMETER("SYS30001"),
    /**
     * validate http header exception
     */
    ILLEGAL_HTTP_REQUEST_HEADER("SYS30002"),
    /**
     * producer of rabbit exception
     */
    PRODUCER_RABBIT_EXCEPTION("SYS40001"),
    /**
     * consumer of rabbit exception
     */
    CONSUMER_RABBIT_EXCEPTION("SYS40021"),
    /**
     * msgCd 不存在
     */
    MSG_CD_NOT_EXISTS("SYS99999");


    private final String code;

    private String desc;

    BlinkErrorCodeEnum(String code, String desc){
        this.code=code;
        this.desc=desc;
    }

    BlinkErrorCodeEnum( String code){
        this.code=code;
    }


    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
