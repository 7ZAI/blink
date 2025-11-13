package com.blink.framework.core.filter;


import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.crypt.BlinkHybridEncrypted;
import com.blink.framework.core.crypt.DecryptedRequestWrapper;
import com.blink.framework.core.crypt.EncryptResponseWrapper;
import com.blink.framework.core.crypt.HttpServletCrypto;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static com.blink.framework.core.data.CoreConstant.*;


/**
 * 请求解密过滤器
 * @Author binblink
 * @Date 2025/8/29
 */

public class CryptoFilter extends HttpFilter {

    private final Logger logger = LoggerFactory.getLogger(CryptoFilter.class);

    private final HttpServletCrypto httpServletCrypto;

    public CryptoFilter(HttpServletCrypto httpServletCrypto){
        this.httpServletCrypto = httpServletCrypto;
    }


    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException , BlinkException {

        // 检查请求是否需要解密 加密
        if (httpServletCrypto.shouldEncryptAndDecrypt(request)) {
            String requestId = request.getHeader(X_BLINK_REQUEST_ID);
            long currentTime = System.currentTimeMillis();
            // 创建包装的请求对象 包含解密后的内容
            String plainText = httpServletCrypto.decrypt(request);
            logger.info("requestId:{},请求解密耗时：{} mills", requestId,Duration.ofMillis(System.currentTimeMillis()-currentTime));
            //包装http 请求和响应 以获取原始body里的值
            DecryptedRequestWrapper requestWrapper = new DecryptedRequestWrapper(request,plainText);
            EncryptResponseWrapper responseWrapper = new EncryptResponseWrapper(response);

            // 继续处理链
            chain.doFilter(requestWrapper, responseWrapper);
            // 获取响应数据
            String responseData = new String(responseWrapper.getData(), StandardCharsets.UTF_8);
            // 响应加密
            BlinkHybridEncrypted.HybridEncryptedResult result  = (BlinkHybridEncrypted.HybridEncryptedResult) httpServletCrypto.encrypt(request,responseData);
            logger.info("requestId:{},的响应加密耗时：{} mills", requestId, Duration.ofMillis(System.currentTimeMillis()-currentTime));

            //设置iv
            responseWrapper.setHeader(X_BLINK_IV,result.iv);
            responseWrapper.setHeader(X_BLINK_KEY,result.encryptedAesKey);
            // 设置 Content-Length
            responseWrapper.setContentLength(result.ciphertext.getBytes(response.getCharacterEncoding()).length);
            // 写回加密后的响应
            responseWrapper.getWriter().write(result.ciphertext);
        } else {
            // 不需要解密的请求直接传递
            chain.doFilter(request, response);
        }
    }



}