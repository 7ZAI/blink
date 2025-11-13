package com.blink.framework.core.crypt;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 响应包装类 加密用
 * @Author binblink
 * @Date 2025/8/30
 */
public class EncryptResponseWrapper extends HttpServletResponseWrapper {


    private final ByteArrayOutputStream outputStream;
    private final ServletOutputStream servletOutputStream;

    public EncryptResponseWrapper(HttpServletResponse response) {
        super(response);
        this.outputStream = new ByteArrayOutputStream();
        this.servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }


            @Override
            public void setWriteListener(WriteListener listener) {
                // 无需实现
            }
        };
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return servletOutputStream;
    }

    public byte[] getData() {
        return outputStream.toByteArray();
    }

}
