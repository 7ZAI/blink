package com.blink.framework.common.data;

import jakarta.validation.Valid;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * RequestMessageDTO 通用数据传输对象
 *
 */
public class RequestDTO<T> implements Serializable {

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 调用链唯一标识
     */
    private String traceId;

    /**
     * API版本
     */
    private String version;

    /**
     * 调用链Span唯一标识 一次调用或操作的单个组件 用于性能分析
     */
    private String spanId;

    /**
     * 当前Span的父ID
     */
    private String parentSpanId;
    /**
     * 请求日期
     */
    private LocalDate reqDate;

    /**
     * 交易发起时间
     */
    private LocalDateTime startDateTime;

    /**
     * 交易结束时间
     */
    private LocalDateTime endDateTime;

    /**
     * 登录用户ID
     */
    private String userId;

    /**
     * 客户端ip
     */
    private String clientIp;

    /**
     * 请求来源
     */
    private String source;

    /**
     * 渠道
     */
    private String channel;

    /**
     * uri
     */
    private String uri;

    /**
     * 客户端期望的超时时间（毫秒）
     */
    private Integer timeout;

    /**
     * 用户凭证
     */
    private String token;

    /**
     * 登入名
     */
    private String loginName;

    /**
     * 扩展字段
     */
    private Map<String, Object> extensions;


    /**
     * 封装业务请求数据 javabean
     */
    @Valid
    private T body;


    public static <T> RequestDTO<T> newInstance(T body) {
        RequestDTO<T> requestDTO = new RequestDTO<>();
        requestDTO.setBody(body);
        return requestDTO;
    }


    public static RequestDTO<EmptyBody> newInstance() {

        return new RequestDTO<EmptyBody>();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }


    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public LocalDate getReqDate() {
        return reqDate;
    }

    public void setReqDate(LocalDate reqDate) {
        this.reqDate = reqDate;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        return "RequestDTO{" +
                "requestId='" + requestId + '\'' +
                ", traceId='" + traceId + '\'' +
                ", version='" + version + '\'' +
                ", spanId='" + spanId + '\'' +
                ", parentSpanId='" + parentSpanId + '\'' +
                ", reqDate=" + reqDate +
                ", startDateTime=" + startDateTime +
                ", endDateTime=" + endDateTime +
                ", userId='" + userId + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", source='" + source + '\'' +
                ", channel='" + channel + '\'' +
                ", uri='" + uri + '\'' +
                ", timeout=" + timeout +
                ", token='" + token + '\'' +
                ", loginName='" + loginName + '\'' +
                ", extensions=" + extensions +
                ", body=" + body +
                '}';
    }




}

