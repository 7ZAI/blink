package com.blink.gateway.filter;

import cn.hutool.core.lang.UUID;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.crypt.AESUtils;
import com.blink.gateway.crypt.RSAUtils;
import com.blink.gateway.signature.HmacSignatureService;
import com.blink.gateway.signature.SignatureServiceFactory;
import com.blink.gateway.util.GateWayUtil;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.blink.gateway.constant.GatewayConstant.*;

/**
 * 请求解密
 * 响应加密
 * chain.filter(exchange) 之前执行的代码相当于传统filter的 pre 方法 之
 * 后加.then() 执行的代码相当于 post方法
 * 假如有 AFilter BFilter 加载顺序 A----> B 最终执行顺序 A.pre ---->  B.pre ----> B.post ----> A.post
 * 所以在CryptFilter  chain.filter(exchange)之前 完成请求报文解密
 * chain.filter(exchange)之后 完成响应报文加密
 *
 * @author binblink
 */
public class CryptFilter implements GlobalFilter, Ordered {


    private final Logger logger = LoggerFactory.getLogger(CryptFilter.class);

    private final SignatureServiceFactory signatureServiceFactory;

    public CryptFilter(SignatureServiceFactory signatureServiceFactory) {
        this.signatureServiceFactory = signatureServiceFactory;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ChannelInfoRedisDO channelInfo = exchange.getAttribute(CHANNEL_INFO);
        if (Objects.isNull(channelInfo)) {
            return Mono.error(new BlinkException("系统错误!"));
        }

        // 检查是否需要处理加密解密
        if (shouldProcessCrypt(exchange, channelInfo)) {
            return processCryptExchange(exchange, chain, channelInfo);
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
        if (!GateWayUtil.shouldCacheRequestBody(exchange.getRequest())) {
            return false;
        }
        return true;
    }

    private Mono<Void> processCryptExchange(ServerWebExchange exchange, GatewayFilterChain chain, ChannelInfoRedisDO channelInfo) {
        // 处理请求解密
        Mono<Boolean> decryptedRequest = decryptRequest(exchange, channelInfo.getSystemPrivatekey());
        Map<String,Object> signParams =  getSignParams(exchange,channelInfo);

        return decryptedRequest.flatMap(result -> {
            // 处理响应加密
            return chain.filter(
                    exchange.mutate().response(createEncryptedResponseDecorator(exchange.getResponse(), channelInfo,signParams))
                            .build());
        });
    }

    /**
     * 签名参数
     * @param exchange
     * @param channelInfo
     * @return
     */
    private Map<String, Object> getSignParams(ServerWebExchange exchange, ChannelInfoRedisDO channelInfo) {
        Map<String, Object> params = new HashMap<>();
        params.put("timeStamp", System.currentTimeMillis());
        params.put("nonce", UUID.fastUUID().toString(true));
        params.put("appKey", channelInfo.getAppKey());
        params.put("loginName", exchange.getRequest().getHeaders().getFirst(X_BLINK_LOGINNAME));

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
            return Mono.error(new BlinkException("系统错误!"));
        }
        logger.debug("请求body 字符串：{}", bodyStr);

        String key = exchange.getRequest().getHeaders().getFirst(X_BLINK_KEY);
        String iv = exchange.getRequest().getHeaders().getFirst(X_BLINK_IV);

        logger.debug("iv base64字符串：{}", iv);
        long start = System.currentTimeMillis();
        //RSA 解密还原 ase 密钥
        String aesKey = RSAUtils.decryptFromBase64(key, RSAUtils.base64ToPrivateKey(systemPrivateKey));
        logger.debug("原始 aesKey：{}", aesKey);
        try {
            //aes 解密
            String plainBodyStr = AESUtils.decrypt(AESUtils.keyFromBase64(aesKey), AESUtils.ivFromBase64(iv), bodyStr);
            //替换缓存body
            exchange.getAttributes().put(CACHED_REQUEST_BODY_ATTR, plainBodyStr);
            logger.debug("请求解密 耗时：{} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            return Mono.error(new BlinkException(e, "AES解密错误"));
        }
        return Mono.just(true);
    }

    /**
     * 创建Response装饰类 读取响应 加密 加签
     * @param originalResponse
     * @param channelInfo
     * @param signParams
     * @return
     */
    private ServerHttpResponseDecorator createEncryptedResponseDecorator(ServerHttpResponse originalResponse, ChannelInfoRedisDO channelInfo,Map<String,Object> signParams) {
        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<DataBuffer> fluxBody = (Flux<DataBuffer>) body;

                    return super.writeWith(fluxBody.collectList().flatMap(dataBuffers -> {
                        try {

                            long start = System.currentTimeMillis();
                            // 生成新的AES密钥和IV用于响应加密
                            SecretKey aesKey = AESUtils.generateRandomAESKey();
                            byte[] iv = AESUtils.generateIV();

                            String plainKey = AESUtils.encodeToBase64(aesKey.getEncoded());
                            String base64IV = AESUtils.encodeToBase64(iv);

                            logger.debug("响应加密 aesKeyBase64:{}",plainKey);

                            // 使用RSA加密AES密钥和IV
                            String encryptedKey = RSAUtils.encryptToBase64(plainKey, RSAUtils.base64ToPublicKey(channelInfo.getChannelPublickey()));


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
                                logger.debug("响应加密 耗时：{} ms", System.currentTimeMillis() - start);
                                //签名
                                String sign = getSignStr(encryptedResponse,channelInfo.getAppSecret(), signParams);
                                // 设置响应头
                                setHttpHeaders(headers,encryptedKey, base64IV, sign, signParams);

                                // 设置内容长度并写入加密后的数据
                                headers.setContentLength(encryptedBytes.length);
                                DataBuffer buffer = bufferFactory().wrap(encryptedBytes);


                                return Mono.just(buffer);
                            }

                            return Mono.empty();

                        } catch (Exception e) {
                            logger.error("Response encryption failed", e);
                            return Mono.error(new RuntimeException("Response encryption failed", e));
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

    private void setHttpHeaders(HttpHeaders headers,String encryptedKey, String base64IV, String sign, Map<String, Object> signParams) {
        headers.set(X_BLINK_SIGN, sign);
        headers.set(X_BLINK_KEY, encryptedKey);
        headers.set(X_BLINK_IV, base64IV);
        headers.set(X_BLINK_TIMESTAMP,signParams.get("timeStamp").toString());
        headers.set(X_BLINK_NONCE,signParams.get("nonce").toString());

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
     * @param data 签名数据
     * @param appSecret 签名密钥
     * @param signParams 签名参数
     * @return
     */
    private String getSignStr(String data,String appSecret,Map<String,Object> signParams) {
        HmacSignatureService hmacSignatureService = (HmacSignatureService) signatureServiceFactory.getDefaultService();
        String sign = hmacSignatureService.sign(data, appSecret, signParams);
        logger.debug("加密响应数字签名 sign：{}", sign);
        return sign;
    }


    @Override
    public int getOrder() {
        return GatewayConstant.ORDER_LOWEST_ADD_THREE;
    }
}
