package com.blink.framework.common.exception;


import java.io.Serial;

/**
 * 自定义的异常类
 *
 * @author binblink
 */
public class BlinkException extends RuntimeException {

    @Serial
    static final long serialVersionUID = -7034897190745766938L;

    /**
     * 默认系统错误码
     */
    protected static final String SYS_ERROR = BlinkErrorCodeEnum.SYS_ERROR.getCode();

    /**
     * 默认业务错误码
     */
    protected static final String BUSINESS_ERROR = BlinkErrorCodeEnum.BUSINESS_ERROR.getCode();

    /**
     * 异常代码
     */
    private final String code;

    private String errMessage;

    private Throwable cause;

    /**
     * 是否业务异常
     */
    private Boolean isBusinessException = false;


    //无参构造 默认错误代码
    public BlinkException(){
        super(SYS_ERROR);
        this.code = SYS_ERROR;
    }

    /**
     *  这里一般为new出来的异常使用
     *  内部用code覆盖父类的错误message
     *  如果是要将其他异常转为BlinkException 不能使用这个API
     *  原则上要保留原始错误信息，请使用其他重载方法
     *
     * @param code 错误代码
     */
    public BlinkException(String code) {
        super(code);
        this.code = code;
    }

    public BlinkException(String message, String code) {
        super(message);
        this.code = code;
        this.errMessage = message;
    }

    public BlinkException(String message, Throwable cause, String code) {
        super(message, cause);
        this.code = code;
        this.errMessage = message;
        this.cause = cause;
    }

    public BlinkException(String code, Boolean isBusinessException) {
        super(code);
        this.code = code;
        this.isBusinessException = isBusinessException;
    }

    public BlinkException(Throwable cause, String code) {
        super(cause);
        this.code = code;
    }


    public static void throwException(String code){

        throw new BlinkException(code);
    }

    public static void throwException(){

        throw new BlinkException();
    }

    public static void throwBusinessException(){

        throw new BlinkException(BUSINESS_ERROR,true);
    }

    public static void throwBusinessException(String code){

        throw new BlinkException(code,true);
    }

    public Boolean isBusinessException() {
        return isBusinessException;
    }

    public String getCode() {
        return code;
    }

    public Boolean getBusinessException() {
        return isBusinessException;
    }

    public String getErrMessage() {
        return errMessage;
    }
}
