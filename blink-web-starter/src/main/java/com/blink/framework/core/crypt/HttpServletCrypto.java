package com.blink.framework.core.crypt;

import com.blink.framework.common.exception.BlinkException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 加密解密方式 和判断方式抽象成接口 可以后续改变加解密方式时方便更换
 * @author binblink
 */
public interface HttpServletCrypto {



    /**
     * 加密 可能需要request中的参数参数 加密
     * @param plainText
     * @return 不一定值返回一个字符串 所以设置为Object
     */
    Object encrypt(HttpServletRequest request,String plainText) throws BlinkException;

    /**
     * 解密
     * @param request
     * @return
     */
    String decrypt(HttpServletRequest request) throws BlinkException;

    /**
     * 是否需要加解密
     * @return
     */
    boolean shouldEncryptAndDecrypt(HttpServletRequest request);

    /**
     * 默认 空实现
     * @param response
     * @return
     * @throws BlinkException
     */
    default String encrypt(HttpServletResponse response) throws BlinkException{

        return "";
    }
}
