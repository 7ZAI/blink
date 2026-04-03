package com.blink.framework.openfeign.util;

import com.blink.framework.common.data.RequestDTO;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

/**
 * 工具类
 * @Author binblink
 */
public class BlinkFeignUtil {

    /**
     * 创建用于feign 调用请求数据
     *
     * @param t 业务数据
     * @param requestId 新的请求id
     * @param original 原始请求
     * @return RequestDTO<T>
     * @param <T> 业务数据类型
     */
    public static  <T> RequestDTO<T> newInstance(T t,String requestId,RequestDTO original){

        RequestDTO<T> requestDTO = new RequestDTO<>();

        BeanUtils.copyProperties(original,requestDTO);

        requestDTO.setBody(t);
        //新请求
        requestDTO.setRequestId(requestId);
        requestDTO.setSource("feign");
        requestDTO.setReqDate(LocalDate.now());

        return requestDTO;
    }



}
