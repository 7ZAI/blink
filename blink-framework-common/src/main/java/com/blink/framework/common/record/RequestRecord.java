package com.blink.framework.common.record;

import com.blink.framework.common.data.EmptyBody;
import jakarta.validation.Valid;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通用请求数据传输对象 (General Request Data Transfer Object)
 *
 * <p>用于封装所有类型的请求数据，包含请求标识、跟踪信息、用户信息、扩展字段等通用属性。
 * 采用 Record 类型实现不可变性，并结合构建器模式提供灵活的创建和修改方式。</p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * // 简洁创建
 * GeneralRequestDTO<String> request1 = GeneralRequestDTO.of("data");
 *
 * // 使用构建器
 * GeneralRequestDTO<String> request2 = GeneralRequestDTO.<String>builder()
 *     .requestId("req-123")
 *     .traceId("trace-456")
 *     .body("request data")
 *     .build();
 *
 * // 修改现有实例
 * GeneralRequestDTO<String> updated = request2.toBuilder()
 *     .source("web")
 *     .build();
 * }</pre>
 *
 * @param <T> 业务数据类型
 * @author binblink
 * @version 1.0
 */
public record RequestRecord<T>(
        /**
         * 请求唯一标识
         */
        String requestId,

        /**
         * 调用链唯一标识，用于分布式系统跟踪
         */
        String traceId,

        /**
         * API 版本号
         */
        String version,

        /**
         * 调用链 Span 唯一标识，用于性能分析和调用链追踪
         */
        String spanId,

        /**
         * 当前 Span 的父 ID，用于构建调用链层级关系
         */
        String parentSpanId,

        /**
         * 请求日期
         */
        LocalDate reqDate,

        /**
         * 交易发起时间戳
         */
        LocalDateTime startDateTime,

        /**
         * 交易结束时间戳
         */
        LocalDateTime endDateTime,

        /**
         * 登录用户 ID
         */
        String userId,

        /**
         * 客户端 IP 地址
         */
        String clientIp,

        /**
         * 请求来源系统或应用标识
         */
        String source,

        /**
         * 请求渠道（如：web, mobile, desktop 等）
         */
        String channel,

        /**
         * 请求的 URI 路径
         */
        String uri,

        /**
         * 客户端期望的超时时间（毫秒）
         */
        Integer timeout,

        /**
         * 用户认证令牌
         */
        String token,

        /**
         * 用户登录名
         */
        String loginName,

        /**
         * 扩展字段，用于存储自定义的键值对数据
         */
        Map<String, Object> extensions,

        /**
         * 业务数据负载，使用 {@link Valid} 注解支持数据验证
         */
        @Valid T body
) implements Serializable {

    /**
     * 创建包含业务数据的请求对象
     *
     * <p>此方法创建一个只包含业务数据，其他字段为空的请求对象。</p>
     *
     * @param <T>  业务数据类型
     * @param body 业务数据
     * @return 包含指定业务数据的请求对象
     */
    public static <T> RequestRecord<T> of(T body) {
        return RequestRecord.<T>builder()
                .body(body)
                .build();
    }

    /**
     * 创建空的请求对象
     *
     * <p>此方法创建一个不包含任何数据的空请求对象，业务数据类型为 {@link EmptyBody}。</p>
     *
     * @return 空的请求对象
     */
    public static RequestRecord<Object> empty() {
        return builder().build();
    }




    /**
     * 创建构建器实例
     *
     * @param <T> 业务数据类型
     * @return 构建器实例
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * 创建构建器实例（带类型参数）
     *
     * @param <T>      业务数据类型
     * @param bodyType 业务数据类型的Class对象
     * @return 构建器实例
     */
    public static <T> Builder<T> builder(Class<T> bodyType) {
        return new Builder<>();
    }

    /**
     * 转换为构建器，用于基于当前实例创建新的修改版本
     *
     * @return 包含当前实例所有字段值的构建器
     */
    public Builder<T> toBuilder() {
        return new Builder<T>()
                .requestId(this.requestId)
                .traceId(this.traceId)
                .version(this.version)
                .spanId(this.spanId)
                .parentSpanId(this.parentSpanId)
                .reqDate(this.reqDate)
                .startDateTime(this.startDateTime)
                .endDateTime(this.endDateTime)
                .userId(this.userId)
                .clientIp(this.clientIp)
                .source(this.source)
                .channel(this.channel)
                .uri(this.uri)
                .timeout(this.timeout)
                .token(this.token)
                .loginName(this.loginName)
                .extensions(this.extensions)
                .body(this.body);
    }

    /**
     * 检查是否包含业务数据
     *
     * @return 如果包含业务数据则返回 true，否则返回 false
     */
    public boolean hasBody() {
        return body != null;
    }

    /**
     * 检查是否包含扩展字段
     *
     * @return 如果包含非空的扩展字段则返回 true，否则返回 false
     */
    public boolean hasExtensions() {
        return extensions != null && !extensions.isEmpty();
    }

    /**
     * 检查是否包含跟踪信息
     *
     * @return 如果包含非空的跟踪ID则返回 true，否则返回 false
     */
    public boolean hasTraceInfo() {
        return traceId != null && !traceId.trim().isEmpty();
    }

    /**
     * 安全获取扩展字段的值
     *
     * @param <V>       期望的返回值类型
     * @param key       扩展字段的键
     * @param valueType 期望的返回值类型的Class对象
     * @return 如果找到指定键的值且类型匹配则返回值，否则返回 null
     */
    @SuppressWarnings("unchecked")
    public <V> V getExtensionValue(String key, Class<V> valueType) {
        if (extensions == null) return null;
        Object value = extensions.get(key);
        return valueType.isInstance(value) ? (V) value : null;
    }

    /**
     * 安全获取字符串类型的扩展字段值
     *
     * @param key 扩展字段的键
     * @return 如果找到指定键的值且为字符串类型则返回值，否则返回 null
     */
    public String getExtensionValueAsString(String key) {
        return getExtensionValue(key, String.class);
    }

    /**
     * 安全获取整数类型的扩展字段值
     *
     * @param key 扩展字段的键
     * @return 如果找到指定键的值且为整数类型则返回值，否则返回 null
     */
    public Integer getExtensionValueAsInteger(String key) {
        return getExtensionValue(key, Integer.class);
    }

    /**
     * 返回对象的字符串表示形式
     *
     * <p>此方法重写了默认的 toString 方法，提供了更友好的输出格式，
     * 同时避免在日志中输出敏感信息或过长的内容。</p>
     *
     * @return 对象的字符串表示
     */
//    @Override
//    public String toString() {
//        return "GeneralRequestDTO{" +
//                "requestId='" + requestId + '\'' +
//                ", traceId='" + traceId + '\'' +
//                ", version='" + version + '\'' +
//                ", spanId='" + spanId + '\'' +
//                ", parentSpanId='" + parentSpanId + '\'' +
//                ", reqDate=" + reqDate +
//                ", startDateTime=" + startDateTime +
//                ", endDateTime=" + endDateTime +
//                ", userId='" + userId + '\'' +
//                ", clientIp='" + clientIp + '\'' +
//                ", source='" + source + '\'' +
//                ", channel='" + channel + '\'' +
//                ", uri='" + uri + '\'' +
//                ", timeout=" + timeout +
//                ", token='" + (token != null ? "***" : "null") + '\'' + // 隐藏敏感信息
//                ", loginName='" + (loginName != null ? "***" : "null") + '\'' + // 隐藏敏感信息
//                ", extensions=" + (extensions != null ? extensions.size() + " items" : "null") +
//                ", body=" + (body != null ? body.getClass().getSimpleName() : "null") +
//                '}';
//    }

    /**
     * 通用请求数据传输对象的构建器
     *
     * <p>提供链式 API 用于创建 {@link RequestRecord} 实例。
     * 支持所有字段的设置，并提供一些便捷方法。</p>
     *
     * @param <T> 业务数据类型
     */
    public static class Builder<T> {
        private String requestId;
        private String traceId;
        private String version;
        private String spanId;
        private String parentSpanId;
        private LocalDate reqDate;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private String userId;
        private String clientIp;
        private String source;
        private String channel;
        private String uri;
        private Integer timeout;
        private String token;
        private String loginName;
        private Map<String, Object> extensions;
        private T body;

        /**
         * 创建空的构建器实例
         */
        public Builder() {}

        /**
         * 设置请求ID
         *
         * @param requestId 请求唯一标识
         * @return 当前构建器实例
         */
        public Builder<T> requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * 设置调用链跟踪ID
         *
         * @param traceId 调用链唯一标识
         * @return 当前构建器实例
         */
        public Builder<T> traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /**
         * 设置API版本号
         *
         * @param version API版本
         * @return 当前构建器实例
         */
        public Builder<T> version(String version) {
            this.version = version;
            return this;
        }

        /**
         * 设置调用链Span ID
         *
         * @param spanId Span唯一标识
         * @return 当前构建器实例
         */
        public Builder<T> spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        /**
         * 设置父Span ID
         *
         * @param parentSpanId 父Span标识
         * @return 当前构建器实例
         */
        public Builder<T> parentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }

        /**
         * 设置请求日期
         *
         * @param reqDate 请求日期
         * @return 当前构建器实例
         */
        public Builder<T> reqDate(LocalDate reqDate) {
            this.reqDate = reqDate;
            return this;
        }

        /**
         * 设置交易开始时间
         *
         * @param startDateTime 开始时间戳
         * @return 当前构建器实例
         */
        public Builder<T> startDateTime(LocalDateTime startDateTime) {
            this.startDateTime = startDateTime;
            return this;
        }

        /**
         * 设置交易结束时间
         *
         * @param endDateTime 结束时间戳
         * @return 当前构建器实例
         */
        public Builder<T> endDateTime(LocalDateTime endDateTime) {
            this.endDateTime = endDateTime;
            return this;
        }

        /**
         * 设置用户ID
         *
         * @param userId 用户标识
         * @return 当前构建器实例
         */
        public Builder<T> userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 设置客户端IP地址
         *
         * @param clientIp 客户端IP
         * @return 当前构建器实例
         */
        public Builder<T> clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        /**
         * 设置请求来源
         *
         * @param source 来源系统标识
         * @return 当前构建器实例
         */
        public Builder<T> source(String source) {
            this.source = source;
            return this;
        }

        /**
         * 设置请求渠道
         *
         * @param channel 渠道标识
         * @return 当前构建器实例
         */
        public Builder<T> channel(String channel) {
            this.channel = channel;
            return this;
        }

        /**
         * 设置请求URI
         *
         * @param uri 请求路径
         * @return 当前构建器实例
         */
        public Builder<T> uri(String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * 设置超时时间
         *
         * @param timeout 超时时间（毫秒）
         * @return 当前构建器实例
         */
        public Builder<T> timeout(Integer timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * 设置用户认证令牌
         *
         * @param token 认证令牌
         * @return 当前构建器实例
         */
        public Builder<T> token(String token) {
            this.token = token;
            return this;
        }

        /**
         * 设置用户登录名
         *
         * @param loginName 登录名
         * @return 当前构建器实例
         */
        public Builder<T> loginName(String loginName) {
            this.loginName = loginName;
            return this;
        }

        /**
         * 设置扩展字段映射
         *
         * @param extensions 扩展字段键值对
         * @return 当前构建器实例
         */
        public Builder<T> extensions(Map<String, Object> extensions) {
            this.extensions = extensions;
            return this;
        }

        /**
         * 设置业务数据
         *
         * @param body 业务数据对象
         * @return 当前构建器实例
         */
        public Builder<T> body(T body) {
            this.body = body;
            return this;
        }

        /**
         * 设置当前时间为交易开始时间
         *
         * @return 当前构建器实例
         */
        public Builder<T> currentTimeAsStart() {
            this.startDateTime = LocalDateTime.now();
            return this;
        }

        /**
         * 设置当前日期为请求日期
         *
         * @return 当前构建器实例
         */
        public Builder<T> currentDateAsReqDate() {
            this.reqDate = LocalDate.now();
            return this;
        }

        /**
         * 添加单个扩展字段
         *
         * @param key   扩展字段键
         * @param value 扩展字段值
         * @return 当前构建器实例
         */
        public Builder<T> addExtension(String key, Object value) {
            if (this.extensions == null) {
                this.extensions = new java.util.HashMap<>();
            }
            this.extensions.put(key, value);
            return this;
        }

        /**
         * 构建 {@link RequestRecord} 实例
         *
         * <p>此方法会创建扩展字段的防御性拷贝，确保构建的实例不可变。</p>
         *
         * @return 配置完成的请求数据传输对象
         */
        public RequestRecord<T> build() {
            return new RequestRecord<>(
                    requestId,
                    traceId,
                    version,
                    spanId,
                    parentSpanId,
                    reqDate,
                    startDateTime,
                    endDateTime,
                    userId,
                    clientIp,
                    source,
                    channel,
                    uri,
                    timeout,
                    token,
                    loginName,
                    extensions != null ? Map.copyOf(extensions) : null,
                    body
            );
        }
    }


}