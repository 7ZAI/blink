package com.blink.framework.core.config;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.data.CoreConstant;
import com.blink.framework.core.entity.SysMsgInfoDO;
import com.blink.framework.core.mapper.SysMsgInfoMapper;
import com.blink.framework.redis.component.CacheComponent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 全局异常处理 优先处理子类确定声明的异常 然后再处理父类
 * @author binblink
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @Resource
    private SysMsgInfoMapper sysMsgInfoMapper;

    @Resource
    private CacheComponent cacheComponent;

    /**
     * 业务异常
     * @param exception
     * @return
     */
    @ExceptionHandler(value = BlinkException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseDTO<EmptyBody> handleBlinkException(BlinkException exception) {

        log.error(exception.getMessage(), exception);

        ResponseDTO<EmptyBody> rspDto = ResponseDTO.newFailInstance();
        String msgCode = exception.getMessage();
        rspDto.setMsgCode(exception.getMessage());
        String msgInfo = getMsgInfo(msgCode);
        rspDto.setMsgInfo(msgInfo);

        return rspDto;
    }



    /**
     * 数据校验异常
     *
     * @param exception
     * @return
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseDTO<EmptyBody> handleException(MethodArgumentNotValidException exception) {

        log.error(exception.getMessage(), exception);

        BindingResult bindingResult = exception.getBindingResult();
        //可能会同时存在多个参数校验失败异常 只取第一个异常信息
        List<ObjectError> objectErrors = bindingResult.getAllErrors();

        ResponseDTO<EmptyBody> rspDto = ResponseDTO.newFailInstance();

        String msgCode = objectErrors.get(0).getDefaultMessage();
        rspDto.setMsgCode(msgCode);

        String msgInfo = getMsgInfo(msgCode);
        rspDto.setMsgInfo(msgInfo);

        return rspDto;
    }



    /**
     * 处理其他所有未被捕获的异常（覆盖剩余异常）
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class) // 匹配所有 Exception 及其子类（除已被具体处理器处理的）
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseDTO<EmptyBody> handleAllOtherExceptions(Exception e) {
        // 通用异常处理逻辑（如日志记录、返回默认错误信息）
        // 记录详细日志便于排查
        log.error("发生未处理的异常：", e);
        ResponseDTO<EmptyBody> errRsp = ResponseDTO.newFailInstance();
        String msgCode = errRsp.getMsgCode();

        errRsp.setMsgInfo(getMsgInfo(msgCode));
        // 通用错误响应
        return errRsp;
    }


    /**
     * 获取请求中的语言环境
     *
     * @return 语言代码
     */
    private String getLanguage() {
        String lang = BlinkRequestContextHolder.getLanguage();
        if (lang == null || "".equals(lang.trim())) {
            lang = CoreConstant.LANG_CN;
        }

        return lang;
    }

    /**
     * 获取 友好的错误提示
     *
     * @param msgCode 错误码
     * @return 错误提示信息（多语言）
     */
    private String getMsgInfo(String msgCode){

        String lang = getLanguage();
        String cacheKey = CoreConstant.MSG_INFO_KEY_PREFIX + lang + ":" + msgCode;
        return (String) cacheComponent.getFromCacheOrDB(cacheKey, getSupplier(msgCode, lang));
    }

    /**
     * 生成一个执行查询sql的函数作为入参
     *
     * @param msgCode
     * @param lang
     * @return
     */
    private Supplier<String> getSupplier(String msgCode, String lang) {

        return () -> {

            SysMsgInfoDO sysMsgInfoDO = sysMsgInfoMapper.selectOne(new QueryWrapper<SysMsgInfoDO>()
                    .lambda()
                    .eq(SysMsgInfoDO::getMsgCode, msgCode)
                    .eq(SysMsgInfoDO::getMsgLang, lang));

            return Objects.isNull(sysMsgInfoDO) ? "ERROR" : sysMsgInfoDO.getMsgInfo();

        };
    }

}
