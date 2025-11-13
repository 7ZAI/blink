package com.blink.framework.common.exception;


/**
 * 自定义的异常类
 */
public class BlinkException extends RuntimeException {

    static final long serialVersionUID = -7034897190745766938L;

    /**
     * 默认错误码
     */
    protected static final String SYS_ERROR = BlinkErrorCodeEnum.SYS_ERROR.getCode();

    /**
     * 默认错误码
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


}
