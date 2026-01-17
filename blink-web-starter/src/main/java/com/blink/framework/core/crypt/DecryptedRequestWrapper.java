package com.blink.framework.core.crypt;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * http request 包装类
 * 自定义请求包装器，相当与重新set body
 * @Author binblink
 * @Date 2025/8/29
 */
public class DecryptedRequestWrapper extends HttpServletRequestWrapper {

    private final String decryptedBody;

    public DecryptedRequestWrapper(HttpServletRequest request,String plaintext) throws IOException {
        super(request);
        this.decryptedBody = plaintext;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 返回解密后的内容作为输入流
        final ByteArrayInputStream byteArrayInputStream =
                new ByteArrayInputStream(decryptedBody.getBytes(StandardCharsets.UTF_8));

        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }


}
