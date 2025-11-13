package com.blink.framework.core.crypt;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.entity.ChannelDO;
import com.blink.framework.core.mapper.SysChannelMapper;
import com.blink.framework.redis.component.CacheComponent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.blink.framework.core.data.CoreConstant.*;

/**
 * 默认 加解密方式
 * ====== 混合加密（AES-GCM + RSA-OAEP） ======
 *
 * @Author binblink
 * @Date 2025/8/29
 */
public class BlinkHybridEncrypted implements HttpServletCrypto {

    private final Logger logger = LoggerFactory.getLogger(BlinkHybridEncrypted.class);


    private final CacheComponent cacheComponent;

    private final SysChannelMapper channelMapper;

    private final Boolean aesAadEnable;

    public BlinkHybridEncrypted(CacheComponent cacheComponent, SysChannelMapper channelMapper, Boolean aesAadEnable) {
        this.cacheComponent = cacheComponent;
        this.channelMapper = channelMapper;
        this.aesAadEnable = aesAadEnable;
    }


    /**
     * 混合加密
     * 过程  AES generate random AESKey and iv ---> AESKey encrypt plaintext ----> cipherText
     * RSA channel publicKey encrypt AESKey ----> key
     *
     * @param response
     * @return
     * @throws BlinkException
     */
    @Override
    public String encrypt(HttpServletResponse response) throws BlinkException {

        try {

            String plaintext = response.toString();

            SecretKey aesKey = AESUtils.generateSecretKey();

            byte[] iv = AESUtils.generateIV();

            AESUtils.encryptWithAad(aesKey, iv, plaintext, null);
            String ivBase64 = AESUtils.encodeToBase64String(iv);


            HybridEncryptedResult result = new HybridEncryptedResult("", ivBase64, "");
            return "";

        } catch (Exception e) {

            throw new RuntimeException(e);

        }
    }

    /**
     * 混合加密
     *
     * @param request
     * @param plaintext
     * @return
     * @throws BlinkException
     */
    @Override
    public HybridEncryptedResult encrypt(HttpServletRequest request, String plaintext) throws BlinkException {
        try {

            SecretKey aesKey = AESUtils.generateSecretKey();

            byte[] iv = AESUtils.generateIV();
            String ciperText = "";

            //是否启用AAD
            if (aesAadEnable) {
                byte[] aad = AESUtils.buildAAD(request.getHeader(X_BLINK_USRID), request.getHeader(X_BLINK_TOKEN), PROTOCOL_VERSION);
                ciperText = AESUtils.encryptWithAad(aesKey, iv, plaintext, aad);
            } else {
                ciperText = AESUtils.encrypt(aesKey, iv, plaintext);
            }

            String keyBase64 = AESUtils.encodeToBase64String(aesKey.getEncoded());
            String ivBase64 = AESUtils.encodeToBase64String(iv);

            String appKey = request.getHeader(X_BLINK_APPKEY);
            Object channelInfo = cacheComponent.getFromCacheOrDB(CHANNEL_INFO_KEY_PREFIX + appKey,
                    () -> channelMapper
                            .selectOne(new QueryWrapper<ChannelDO>()
                                    .lambda()
                                    .eq(ChannelDO::getAppKey, appKey)));
            ChannelDO channelDO = new ChannelDO();
            BeanUtil.copyProperties(channelInfo, channelDO);

            String encryptAesKey = RSAUtils.encryptToBase64(keyBase64, RSAUtils.base64ToPublicKey(channelDO.getChannelPublickey()));

            logger.info("http请求报文加密的结果：{}", ciperText);

            return new HybridEncryptedResult(encryptAesKey, ivBase64, ciperText);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 混合解密
     * 过程 RSA privateKey decrypt key ---> AES KEY ----> AES decrypt cipherText -----> plaintext
     *
     * @param request
     * @return 明文 plaintext
     * @throws BlinkException
     */
    @Override
    public String decrypt(HttpServletRequest request) throws BlinkException {

        try {
            //原始密文
            String cipherText = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            //RSA加密的key
            String key = request.getHeader(X_BLINK_KEY);
            //iv base64字符串
            String iv = request.getHeader(X_BLINK_IV);

            //获取channel信息
            String appKey = request.getHeader(X_BLINK_APPKEY);

            Object channelInfo = cacheComponent.getFromCacheOrDB(CHANNEL_INFO_KEY_PREFIX + appKey,
                    () -> channelMapper
                            .selectOne(new QueryWrapper<ChannelDO>()
                                    .lambda()
                                    .eq(ChannelDO::getAppKey, appKey)));
            ChannelDO channelDO = new ChannelDO();
            BeanUtil.copyProperties(channelInfo, channelDO);

            if (Objects.isNull(channelInfo)) {
                throw new BlinkException("获取渠道信息失败！导致解密失败！");
            }
            //得到系统私钥
            String privateKeyBase64 = channelDO.getSystemPrivatekey();
            String plaintext = "";
            // //RSA 私钥解密key 用系统私钥解密得到 secretkey的base64字符串
            key = RSAUtils.decryptFromBase64(key, RSAUtils.base64ToPrivateKey(privateKeyBase64));
            if (aesAadEnable) {
                //添加 aad GCM特性 可以添加不参与加密解密的认证数据 但解密会验证是否与加密时设置的是否一致
                byte[] aadBytes = AESUtils.buildAAD(request.getHeader(X_BLINK_USRID), request.getHeader(X_BLINK_TOKEN), PROTOCOL_VERSION);
                //AES解密报文 用key和iv 解密密文
                plaintext = AESUtils.decryptWithAad(key, iv, cipherText, aadBytes);
            } else {
                plaintext = AESUtils.decrypt(key, iv, cipherText);
            }


            logger.info("http请求报文解密的结果：{}", plaintext);

            return plaintext;
        } catch (IOException e) {
            logger.error("HttpRequestBody decrypt error! requestId:{}  info:{}", request.getHeader(X_BLINK_REQUEST_ID), e.getMessage());
            throw new BlinkException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 是否需要加解密
     * openfeign调用不需要加解密
     *
     * @param request
     * @return
     */
    @Override
    public boolean shouldEncryptAndDecrypt(HttpServletRequest request) {

        //请求头判断 这里的请求头
        String appKey = request.getHeader(X_BLINK_APPKEY);

        Object channelInfo = cacheComponent.getFromCacheOrDB(CHANNEL_INFO_KEY_PREFIX + appKey,
                () -> channelMapper
                        .selectOne(new QueryWrapper<ChannelDO>()
                                .lambda()
                                .eq(ChannelDO::getAppKey, appKey)));
        if (Objects.isNull(channelInfo)) {
            throw new BlinkException("获取渠道信息失败!");
        }
        ChannelDO channelDO = new ChannelDO();
        BeanUtil.copyProperties(channelInfo, channelDO);

        return SWITCH_ON.equals(channelDO.getEncryptionSwitch());

    }


    /**
     * 混合加密结果封装类
     */
    public static class HybridEncryptedResult {

        public final String encryptedAesKey; // Base64
        public final String iv;              // Base64
        public final String ciphertext;      // Base64

        public HybridEncryptedResult(String encryptedAesKey, String iv, String ciphertext) {
            this.encryptedAesKey = encryptedAesKey;
            this.iv = iv;
            this.ciphertext = ciphertext;
        }
    }
}
