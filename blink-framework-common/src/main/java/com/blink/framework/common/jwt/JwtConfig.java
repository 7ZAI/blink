package com.blink.framework.common.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import lombok.Data;

import javax.crypto.SecretKey;

/**
 * @Author binblink
 * @Date 2026/2/3
 */
@Data
public class JwtConfig {

    /**
     * 默认过期时间：2小时（单位：毫秒）
     */
    private static final long DEFAULT_EXPIRE_MS = 7200 * 1000L;


    /**
     * 默认过期时间：7天（单位：毫秒）
     */
    private static final long DEFAULT_REFRESH_EXPIRE = 7 * 86400 * 1000L;
    /**
     * 时钟偏移容忍：60秒（解决服务器时间差问题）
     */
    private static final long CLOCK_SKEW_SECONDS = 60L;

    public long getClockSkewSeconds() {
        return CLOCK_SKEW_SECONDS;
    }

    /**
     * 默认签名算法（HS256）
     */
    private static final String DEFAULT_SIGN_ALG = "HS256";

    /**
     * 密钥
     */
    private String jwtSecret;

    /**
     * 算法名称
     */
    private String algorithm ;

    /**
     * 签发者
     */
    private String issuer;

    /**
     * 受众
     */
    private String audience;

    /**
     * 认证token过期时间
     */
    private long accessTokenExpiration;

    /**
     * 刷新toke过期时间
     */
    private long refreshTokenExpiration;


    public JwtConfig(){
        this.jwtSecret = defaultSecretKey();
        this.algorithm = DEFAULT_SIGN_ALG;
        this.accessTokenExpiration = DEFAULT_EXPIRE_MS;
        this.refreshTokenExpiration = DEFAULT_REFRESH_EXPIRE;
    }



    public JwtConfig(String jwtSecret){
        this.jwtSecret = jwtSecret;
        this.algorithm = DEFAULT_SIGN_ALG;
        this.accessTokenExpiration = DEFAULT_EXPIRE_MS;
        this.refreshTokenExpiration = DEFAULT_REFRESH_EXPIRE;
    }

    /**
     * base64的形式
     * @return
     */
    private String defaultSecretKey() {
        SecretKey key = Jwts.SIG.HS256.key().build();
        return Encoders.BASE64.encode(key.getEncoded());
    }
}
