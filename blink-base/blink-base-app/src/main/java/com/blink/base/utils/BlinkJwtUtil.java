package com.blink.base.utils;

import cn.hutool.crypto.KeyUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.signers.AlgorithmUtil;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.blink.datasource.code.CodeGenerator;


/**
 * @Author binblink
 * @Date 2025/8/20
 */
public class BlinkJwtUtil {

    /**
     * 过期时间1小时
     */
    private static final long EXPIRATION = 1000 * 60 * 60;
    /**
     * 签名算法
     */
    private static final String ALGORITHM_ID = "ES256";
    /**
     *  签名器 signer – 签名算法
     */
    private static final JWTSigner SIGNER = JWTSignerUtil.createSigner(ALGORITHM_ID, KeyUtil.generateKeyPair(AlgorithmUtil.getAlgorithm(ALGORITHM_ID)));


    public static String generateToken(String username,String userId){

        return JWT.create().setPayload("username",username).setPayload("uid",userId).sign(SIGNER);
    }

    public static boolean validateToken(String token) {
        try {
            JWT.of(token).verify();
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "123456";
        CodeGenerator.generateByCustomTemplate(url, username, password);

//        System.out.println(validateToken("eyJ0eXAiOiJKV1QiLCJhGciOiJQUzI1NiJ9.eyJ1c2VybmFtZSI6IjEyMzEyMyJ9.JD-nTUY8PUtBWv7L3BLGLMKkiCWTgc56gga1EDOxAfUDp2W6UwxPKHzRn_Mmkj9W_5rxDjcbQSKgbuSTmkLdiElYtPt30Zik1KIeXvrM_YdfYx10MW9KPsIjQGUdIBTIfxC8OLj7eLhJ2fNgCWRc0IGYmqbM9C2PhKTA_VueXnc"));
//        System.out.println(jwt.getPayload("username"));
    }

    public static String getUsernameFromToken(String token) {
        return (String) JWT.of(token).getPayload("username");
    }

    public static String getUseIdFromToken(String token) {
        return (String) JWT.of(token).getPayload("uid");
    }
}
