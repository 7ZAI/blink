package com.blink.framework.core.config;

import com.blink.framework.common.constrant.ResponseMsgType;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.data.CoreConstant;
import com.blink.framework.core.exception.ErrMsgProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理
 * 优先处理子类确定声明的异常，然后再处理父类
 *
 * @author binblink
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Resource
    private ErrMsgProvider errMsgProvider;

    /**
     * 业务异常处理
     *
     * @param exception BlinkException
     * @return ResponseDTO<EmptyBody>
     */
    @ExceptionHandler(value = BlinkException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseDTO<EmptyBody> handleBlinkException(BlinkException exception) {
        log.error(exception.getMessage(), exception);

        ResponseDTO<EmptyBody> rspDto = ResponseDTO.newFailInstance();
        String msgCode = exception.getMessage();
        rspDto.setMsgCode(msgCode);

        String lang = getLanguage();
        String msgInfo = errMsgProvider.getErrMsg(msgCode, lang);
        rspDto.setMsgInfo(msgInfo);
        //设置错误类型
        if(exception.isBusinessException()){
            rspDto.setMsgType(ResponseMsgType.BUSINESS_ERR.getType());
        }

        return rspDto;
    }

    /**
     * 数据校验异常
     *
     * @param exception MethodArgumentNotValidException
     * @return ResponseDTO<EmptyBody>
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseDTO<EmptyBody> handleException(MethodArgumentNotValidException exception) {
        log.error(exception.getMessage(), exception);

        BindingResult bindingResult = exception.getBindingResult();
        // 可能会同时存在多个参数校验失败异常，只取第一个异常信息
        List<ObjectError> objectErrors = bindingResult.getAllErrors();

        ResponseDTO<EmptyBody> rspDto = ResponseDTO.newFailInstance();
        String msgCode = objectErrors.get(0).getDefaultMessage();
        rspDto.setMsgCode(msgCode);

        String lang = getLanguage();
        String msgInfo = errMsgProvider.getErrMsg(msgCode, lang);
        rspDto.setMsgInfo(msgInfo);
        //数据校验默认属于业务异常
        rspDto.setMsgType(ResponseMsgType.BUSINESS_ERR.getType());

        return rspDto;
    }

    /**
     * 资源未找到异常
     *
     * @param exception NoResourceFoundException
     * @return ResponseDTO<EmptyBody>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ResponseDTO<EmptyBody> handleNoResourceFoundException(NoResourceFoundException exception) {
        log.error("资源未找到: {}", exception.getResourcePath(), exception);

        ResponseDTO<EmptyBody> rspDto = ResponseDTO.newFailInstance();
        rspDto.setMsgCode(BlinkErrorCodeEnum.NO_HANDLER_FOUND_ERROR.getCode());

        String lang = getLanguage();
        String msgInfo = errMsgProvider.getErrMsg(BlinkErrorCodeEnum.NO_HANDLER_FOUND_ERROR.getCode(), lang);
        rspDto.setMsgInfo(msgInfo);

        return rspDto;
    }

    /**
     * 处理其他所有未被捕获的异常
     *
     * @param e Exception
     * @return ResponseDTO<EmptyBody>
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseDTO<EmptyBody> handleAllOtherExceptions(Exception e) {
        // 记录详细日志便于排查
        log.error("发生未处理的异常：", e);

        ResponseDTO<EmptyBody> errRsp = ResponseDTO.newFailInstance();
        String msgCode = errRsp.getMsgCode();

        String lang = getLanguage();
        String msgInfo = errMsgProvider.getErrMsg(msgCode, lang);
        errRsp.setMsgInfo(msgInfo);

        return errRsp;
    }

    /**
     * 获取请求中的语言环境
     *
     * @return 语言代码
     */
    private String getLanguage() {
        String lang = BlinkRequestContextHolder.getLanguage();
        if (lang == null || lang.trim().isEmpty()) {
            lang = CoreConstant.LANG_CN;
        }
        //前端传错- 格式化语言代码，将 '-' 转换为 '_' 以匹配数据库存储格式
        lang = lang.replace('-', '_');

        return lang;
    }
}