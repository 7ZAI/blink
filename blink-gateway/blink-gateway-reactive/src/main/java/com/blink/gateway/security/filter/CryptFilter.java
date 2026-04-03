package com.blink.gateway.security.filter;

import cn.hutool.core.lang.UUID;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.ChannelSecretKey;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.AESUtils;
import com.blink.framework.common.utils.RSAUtils;
import com.blink.gateway.component.ChannelSecretCache;
import com.blink.gateway.signature.HmacSignatureService;
import com.blink.gateway.signature.SignatureServiceFactory;
import com.blink.gateway.util.GateWayUtil;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.blink.framework.common.constrant.SysConstant.*;
import static com.blink.gateway.constant.GatewayConstant.*;

/**
 * 请求解密
 * 响应加密
 * chain.filter(exchange) 之前执行的代码相当于传统filter的 pre 方法 之
 * 后加.then() 执行的代码相当于 post方法
 * 假如有 AFilter BFilter 加载顺序 A----> B 最终执行顺序 A.pre ---->  B.pre ----> B.post ----> A.post
 * 所以在CryptFilter中  chain.filter(exchange)之前 完成请求报文解密
 * chain.filter(exchange)之后 完成响应报文加密
 *
 * @author binblink
 */
@Slf4j
public class CryptFilter implements WebFilter {


    private final SignatureServiceFactory signatureServiceFactory;

    private final ChannelSecretCache channelSecretCache;

    public CryptFilter(SignatureServiceFactory signatureServiceFactory, ChannelSecretCache channelSecretCache) {
        this.signatureServiceFactory = signatureServiceFactory;
        this.channelSecretCache = channelSecretCache;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ChannelInfoRedisDO channelInfo = exchange.getAttribute(CHANNEL_INFO);
        if (Objects.isNull(channelInfo)) {
            return Mono.error(new BlinkException(BlinkErrorCodeEnum.SYS_ERROR.getCode()));
        }
        ChannelSecretKey secretKeyInfo = channelSecretCache.getChannelSecretConfigs().get(channelInfo.getAppKey());
        // 检查是否需要处理加密解密
        if (shouldProcessCrypt(exchange, channelInfo)) {

            return decryptRequest(exchange,secretKeyInfo.getSystemPrivatekey())
                    .filter(isValid->isValid)
                    .switchIfEmpty(Mono.error(new BlinkException(BlinkErrorCodeEnum.ENCRYPT_DECRYPT_ERROR.getCode())))
                    .flatMap(result -> {
                        // 处理响应加密
                        return chain.filter(exchange.mutate()
                                .response(createEncryptedResponseDecorator(exchange.getResponse(), channelInfo,secretKeyInfo)).build());
                    });
        }

        return chain.filter(exchange);
    }

    /**
     * 是否要加密解密
     *
     * @param exchange
     * @return
     */
    private boolean shouldProcessCrypt(ServerWebExchange exchange, ChannelInfoRedisDO channelInfo) {

        //当前渠道未开启加密解密
        if (SWITCH_OFF.equals(channelInfo.getEncryptionSwitch())) {
            return false;
        }
        // 请求不是 post application/json
        return GateWayUtil.shouldCacheRequestBody(exchange.getRequest());
    }

    /**
     * 签名参数
     *
     * @param channelInfo
     * @return
     */
    private Map<String, Object> getSignParams(ChannelInfoRedisDO channelInfo) {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_TIMESTAMP, System.currentTimeMillis());
        params.put(KEY_NONCE, UUID.fastUUID().toString(true));
        params.put(KEY_APPKEY, channelInfo.getAppKey());

        return params;

    }

    /**
     * 请求解密
     *
     * @param exchange
     * @param systemPrivateKey 系统私钥
     * @return
     */
    private Mono<Boolean> decryptRequest(ServerWebExchange exchange, String systemPrivateKey) {

        String bodyStr = exchange.getAttribute(CACHED_REQUEST_BODY_ATTR);
        if (Objects.isNull(bodyStr)) {
            log.warn("系统错误! 无法拿到Attribute中缓存的body json字符串");
            return Mono.just(false);
        }
        log.debug("请求body 字符串：{}", bodyStr);

        String key = exchange.getRequest().getHeaders().getFirst(X_BLINK_KEY);
        String iv = exchange.getRequest().getHeaders().getFirst(X_BLINK_IV);

        log.debug("iv base64字符串：{}", iv);
        long start = System.currentTimeMillis();

        // 去除body字符串可能存在的引号
        String encryptedBody = bodyStr;
        if (encryptedBody.startsWith("\"") && encryptedBody.endsWith("\"")) {
            encryptedBody = encryptedBody.substring(1, encryptedBody.length() - 1);
        }

        // 将URL安全Base64转换为标准Base64
        String standardKey = urlSafeToStandardBase64(key);
        String standardIv = urlSafeToStandardBase64(iv);

        //RSA 解密还原 ase 密钥
        String aesKey = RSAUtils.decryptFromBase64(standardKey, RSAUtils.base64ToPrivateKey(systemPrivateKey));
        log.debug("原始 aesKey：{}", aesKey);
        try {
            //aes 解密
            String plainBodyStr = AESUtils.decrypt(AESUtils.keyFromBase64(aesKey), AESUtils.ivFromBase64(standardIv), encryptedBody);
            //替换缓存body
            exchange.getAttributes().put(CACHED_REQUEST_BODY_ATTR, plainBodyStr);
            log.debug("请求解密 耗时：{} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("AES解密错误 {}",e.getMessage(),e);
            return Mono.just(false);
        }
        return Mono.just(true);
    }

    /**
     * 将URL安全Base64转换为标准Base64
     * 前端使用URL安全Base64编码（+ -> -, / -> _）
     * 后端需要转换为标准Base64才能正确解码
     *
     * @param urlSafeBase64 URL安全Base64字符串
     * @return 标准Base64字符串
     */
    private String urlSafeToStandardBase64(String urlSafeBase64) {
        if (urlSafeBase64 == null || urlSafeBase64.isEmpty()) {
            return urlSafeBase64;
        }
        // 去除可能存在的引号
        String result = urlSafeBase64;
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        // - -> +, _ -> /
        String standardBase64 = result.replace("-", "+").replace("_", "/");
        // 补齐=填充
        int pad = standardBase64.length() % 4;
        if (pad > 0) {
            standardBase64 += "=".repeat(4 - pad);
        }
        return standardBase64;
    }

    /**
     * 创建Response装饰类 读取响应 加密 加签
     *
     * @param originalResponse
     * @param channelInfo
     * @return
     */
    private ServerHttpResponseDecorator createEncryptedResponseDecorator(ServerHttpResponse originalResponse, ChannelInfoRedisDO channelInfo,ChannelSecretKey secretKeyInfo) {
        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {

                    Flux<DataBuffer> fluxBody = ((Flux<?>) body).cast(DataBuffer.class);

                    return super.writeWith(fluxBody.collectList().flatMap(dataBuffers -> {
                        try {

                            long start = System.currentTimeMillis();
                            // 生成新的AES密钥和IV用于响应加密
                            SecretKey aesKey = AESUtils.generateRandomAESKey();
                            byte[] iv = AESUtils.generateIV();

                            String plainKey = AESUtils.encodeToBase64(aesKey.getEncoded());
                            String base64IV = AESUtils.encodeToBase64(iv);

                            log.debug("响应加密 aesKeyBase64:{}", plainKey);

                            // 使用RSA加密AES密钥和IV
                            String encryptedKey = RSAUtils.encryptToBase64(plainKey, RSAUtils.base64ToPublicKey(secretKeyInfo.getChannelPublicKey()));


                            HttpHeaders headers = getDelegate().getHeaders();
                            // 处理响应体
                            if (dataBuffers.isEmpty()) {
                                headers.setContentLength(0);
                                return Mono.empty();
                            }
                            // 合并数据缓冲区
                            byte[] combinedBytes = combineDataBuffers(dataBuffers);

                            if (combinedBytes.length > 0) {
                                // 加密响应体
                                String responseBody = new String(combinedBytes, StandardCharsets.UTF_8);
                                String encryptedResponse = AESUtils.encrypt(aesKey, iv, responseBody);
                                byte[] encryptedBytes = encryptedResponse.getBytes(StandardCharsets.UTF_8);
                                log.debug("响应加密 耗时：{} ms", System.currentTimeMillis() - start);
                                //签名
                                // 处理请求解密
                                Map<String, Object> signParams = getSignParams(channelInfo);
                                String sign = getSignStr(encryptedResponse, secretKeyInfo.getAppSecret(), signParams);
                                // 设置响应头
                                setHttpHeaders(headers, encryptedKey, base64IV, sign, signParams);

                                // 设置内容长度并写入加密后的数据
                                headers.setContentLength(encryptedBytes.length);
                                DataBuffer buffer = bufferFactory().wrap(encryptedBytes);

                                return Mono.just(buffer);
                            }

                            return Mono.empty();

                        } catch (Exception e) {
                            log.error("Response encryption failed", e);
                            return Mono.error(new BlinkException("Response encryption failed: " + e.getMessage(), e, BlinkErrorCodeEnum.ENCRYPT_DECRYPT_ERROR.getCode()));
                        }
                    }));
                }
                return super.writeWith(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                return headers;
            }
        };
    }


    private void setHttpHeaders(HttpHeaders headers, String encryptedKey, String base64IV, String sign, Map<String, Object> signParams) {
        headers.set(X_BLINK_SIGN, sign);
        headers.set(X_BLINK_KEY, encryptedKey);
        headers.set(X_BLINK_IV, base64IV);
        headers.set(X_BLINK_TIMESTAMP, signParams.get("timeStamp").toString());
        headers.set(X_BLINK_NONCE, signParams.get("nonce").toString());

    }

    private byte[] combineDataBuffers(List<DataBuffer> dataBuffers) {
        int totalSize = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
        byte[] result = new byte[totalSize];
        int offset = 0;

        for (DataBuffer buffer : dataBuffers) {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            System.arraycopy(bytes, 0, result, offset, bytes.length);
            offset += bytes.length;
            DataBufferUtils.release(buffer);
        }

        return result;
    }

    /**
     * 获取数据签名字符串
     *
     * @param data       签名数据
     * @param appSecret  签名密钥
     * @param signParams 签名参数
     * @return
     */
    private String getSignStr(String data, String appSecret, Map<String, Object> signParams) {
        HmacSignatureService hmacSignatureService = (HmacSignatureService) signatureServiceFactory.getDefaultService();
        String sign = hmacSignatureService.sign(data, appSecret, signParams);
        log.debug("加密响应数字签名 sign：{}", sign);
        return sign;
    }

}
