package com.blink.base;



import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.RSA;
import com.alibaba.fastjson2.JSON;
import com.blink.base.dto.req.AddSysUserReqDTO;
import com.blink.base.dto.req.SysLoginReqDTO;
import com.blink.base.entity.FilterDefinitionDO;
import com.blink.base.entity.PredicateDefinitionDO;
import com.blink.base.entity.RouteDefinitionDO;
import com.blink.datasource.code.CodeGenerator;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.core.crypt.AESUtils;
import com.blink.framework.core.crypt.RSAUtils;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

public class NormalTest {



    @Test
    public void test() throws Exception {

//        System.out.println(UUID.fromString("dasdasd"));

//        System.out.println(UUID.fromString("dasdasd"));

        System.out.println(cn.hutool.core.lang.UUID.fastUUID());
        System.out.println(UUID.randomUUID());

        RSA rsa = new RSA();

        System.out.println(rsa.getPublicKeyBase64());
        System.out.println(rsa.getPublicKeyBase64());



        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[16]; // 生成16字节的随机密钥
        secureRandom.nextBytes(keyBytes);
        System.out.println(Base64.getEncoder().encodeToString(keyBytes)); // 转换为Base64字符串


        int first = new Random(10).nextInt(8) + 1;
        int hashCodeV = UUID.randomUUID().toString().hashCode();
        if (hashCodeV < 0) {// 有可能是负数
            hashCodeV = -hashCodeV;
        }
        // 0 代表前面补充0
        // 4 代表长度为4
        // d 代表参数为正数型
        System.out.println(first + String.format("%015d", hashCodeV));


        String appKey = generateAppKey();
        String secretInfo = "userId123456" + System.currentTimeMillis(); // 假设的secret信息
        String appSecret = encryptAppSecret(appKey, secretInfo);
        System.out.println("AppKey: " + appKey);
        System.out.println("AppSecret: " + appSecret);
        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));

        System.out.println(RandomUtil.randomString(16));


    }

    @Test
    public void test3(){
        System.out.println(SecureUtil.sha1().digestHex(RandomUtil.randomString(16)));
        System.out.println(SecureUtil.hmacSha256().digestBase64(RandomUtil.randomString(32), true));
    }
    @Test
    public void test4(){
        RequestDTO<SysLoginReqDTO> requestDTO = new RequestDTO<>();
        SysLoginReqDTO sysLoginReqDTO = new SysLoginReqDTO();
        sysLoginReqDTO.setPassword("123456");
        sysLoginReqDTO.setUsername("test");
        requestDTO.setChannel("test");
        requestDTO.setVersion("v1");
        requestDTO.setClientIp("192.168.1.3");
        requestDTO.setBody(sysLoginReqDTO);
        System.out.println(JSON.toJSONString(requestDTO));
    }


    public static String generateAppKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[16]; // 生成16字节的随机密钥
        secureRandom.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes); // 转换为Base64字符串
    }

    public static String encryptAppSecret(String baseKey, String secretInfo) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        SecretKeySpec secretKeySpec = new SecretKeySpec(baseKey.getBytes(), algorithm);
        mac.init(secretKeySpec);
        byte[] secretBytes = secretInfo.getBytes();
        byte[] encryptedBytes = mac.doFinal(secretBytes);
        return Base64.getEncoder().encodeToString(encryptedBytes); // 加密后转换为Base64字符串
    }


    @Test
    public void test2() throws Exception {
        byte[] iv = AESUtils.generateIV();
        String iv64 = AESUtils.encodeToBase64String(iv);
        System.out.println("iv:  "+iv64);
        SecretKey secretKey = AESUtils.generateSecretKey();

        String key = AESUtils.encodeToBase64String(secretKey.getEncoded());

        System.out.println("keypalin:  "+key);

        RequestDTO<AddSysUserReqDTO> reqDTO = new RequestDTO<>();
        AddSysUserReqDTO req = new AddSysUserReqDTO();

        req.setPassword("123456");
        req.setLoginName("bllink");
        req.setUsername("h5");
        req.setConfirmPassword("123456");
        req.setEmail("3423ff24@qq.com");
        req.setPhone("12312313");
        req.setSex(1);

        reqDTO.setBody(req);
        reqDTO.setReqDate(LocalDate.now());

        String jsonStr = JSON.toJSONString(reqDTO);
        System.out.println(jsonStr);
        String cipertext = AESUtils.encrypt(secretKey, iv, jsonStr);
        System.out.println("cipertext: "+cipertext);

        String syspublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu/fTua2t/kSKeMQFbUB8T0k1KhB+ffDVr2OrpPXvt2XJZS3IkUkSGKQiPKuT6qeNyn2wexg5vNKXGAWZ4U5thUKezhHUUDtbWpEv7Og/SSsqOQp7tJLuKd93w9Jk8byVWWqTuQK4Bsw52LANr6DN1XK5m5iCXUU2+cxFNqLXr5OR0kO4AEoUnG84kXQ3fDPZ0E5r7d0LR02sXEgh2XBwHXAyA2zPcNoaVq9jeL1LLviQ1woSmC8xlA9PwtrRqEtH6C1K2//eCtnMVCquofjNqf3BxbqGpJ9sJep9nw28s/mOVF89h83TB73YubCjKGsC594QZDbLdyKoq5oYgJjHJQIDAQAB";

        PublicKey publicKey = RSAUtils.base64ToPublicKey(syspublicKey);

        String encryptKey = RSAUtils.encryptToBase64(key,publicKey);

        System.out.println("keybase64: "+encryptKey);


        String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC799O5ra3+RIp4xAVtQHxPSTUqEH598NWvY6uk9e+3ZcllLciRSRIYpCI8q5Pqp43KfbB7GDm80pcYBZnhTm2FQp7OEdRQO1takS/s6D9JKyo5Cnu0ku4p33fD0mTxvJVZapO5ArgGzDnYsA2voM3VcrmbmIJdRTb5zEU2otevk5HSQ7gAShScbziRdDd8M9nQTmvt3QtHTaxcSCHZcHAdcDIDbM9w2hpWr2N4vUsu+JDXChKYLzGUD0/C2tGoS0foLUrb/94K2cxUKq6h+M2p/cHFuoakn2wl6n2fDbyz+Y5UXz2HzdMHvdi5sKMoawLn3hBkNst3IqirmhiAmMclAgMBAAECggEAAnP1PiDTWrRbx0AtnvOeu60lpbch43RbYYbQNF6mQnMwWeLrUWQN+abYsIh/rWoC2m/h9/VEuZ9Y6+4UyEEtAjZwmplWdzaTFl87LCEKxGJtEmH948kgFQlOx44tFY+Dtm9C3sD3lFUhcx+3CgHz9bNh0zM+DkgLM97Tz2b4IBkr36ADIFkdSknmArpxdzL6ZGUu5N+9Y2FdNxMNIt9LKhfV7Ifz5Lgc4E1PZTH27J0s6VAqLedGXT7p6eO94gC0hk386DLIs4sTdN+P2kHqblvUj4Fqvwi90pH60nN105UaNuS4E1giLURkZBmedMSU4P4xn1xNXLBbEuOkGZxhkQKBgQD8XLoAV02X4WJsV3ZmchLv/mwxmKTZ1QIoFTcTcXZ/unUlUa4ELcmZNI5yR5XcR5dGrIhvnJ72arTRXfKciSbQVOSRT9Ej8Djq2fhMc0Uk0qU3KlBECASHu12WqOmlm9DU3VhlcFyXN+0I/oc7AipfxoRjfaUHB/L6g4Mi7xwXkQKBgQC+rXi663CVwOe9vvtKAAsI/OO2dkFRqb6nirWywotAana93ILgUCh/NpOs3m6QB+bukz+P9HfUorj1b4+/3JctHVhgSswNvjPs8Rllmpqb9nj3wtu0chqsWT+iD9+cW3RKamZp64c6WEnXlz1LmPVjIG44OK68R8JsAbYhRQO0VQKBgQDQm13ttj0f1WFkqY/n81gQJsIM5V8v6dJUi+TxH5OS1fDMHo64SZkiGb2+XcpVQqCb/Tby/AazL1W00/Iez4jUIbinHdGF4adLC08i8w4u+Ck2RrX+5pnIEWu1hH9PNFQWAbp2b8E6BzLbgnsKnimk7ha1n0DxZPLHfjrCf+E64QKBgQCNn8ENbLQptqcpqwFwPEn1+geUTY9EfYSLYp6EULUnOixJ5tAalc8OddYHzwpl1kizKRFL9fNPLzGbsqodglWS/7nVg5i+GGju80C+069EajP9KnEPPLUNEBkQYSPgOZlNcGvy/ipproviIoY3cTvJzZuwDIHUfKQunrLNUTpoJQKBgH7ZR1PCl++R5zbawnQdXflnYKTuyqlH5zy4KqaIFLq2RhwRFn57VVo0Plra7zelK18+WQ10sp5y1tTh0eDvHjmW+ANh1T3nrBaQwUjigWgQg45uCzXsTPqPcSoBmj1yKQZ/VKRE9BzG5UuVET3gDDcRr9zyMsAmw/rU6JwIkgUH";

        String keyde = RSAUtils.decryptFromBase64(encryptKey, RSAUtils.base64ToPrivateKey(privateKey));

        System.out.println("还原的key:" + keyde);


        String jsono = AESUtils.decrypt(keyde, iv64, cipertext);

        System.out.println(jsono);



    }

    @Test
    public void test8() throws Exception {
        String iv = "FUni5eZ3C5EjVwzR";
        String keyBase64 = "k9uuXhN91pLEAdHIsgEX5eL954gYtsTH+LjNw9QyLPY57RIZs2aoOk7rWNaaJtKF3kGK43Oj+bbe00ufQXPDRk0AdmCJfLLfCjrkk6dMNt4OdRUjMTyqDK/ymhoig1KtLB22Oy60zndfG++0i8DIb3pOOLy2GjOXBWdcOlnc8lEGFRc2St0/kBKhotncWcLFNCMXD/TCQHr1qaFm3DnBDL75vZD6RK1eXb44CzzZ2pWOrciqwGh6pUQkGBLvV/IWrm9Qrvcr1a4o8YDNg/BFf4ggdmQMQLRh1488VhuMUfdpkktJGQfc6BY6u3FBrisJ45I2B0UIjUkGpiH+vWu8Fg==";

        String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCso6X97kzmYAU4a3Lt+gaz7ZemQizg4gHG1Z2ktE7zo0BzTfkMoWgnBXeQdUCjjymndcVvVBKrBiIRyydnx6K9KHa6e6rYbmH/ezA+ZaGdQJfB6j8zoGx80Q9JFKX+NjtqonSKFkIkV3bgLEGfaFsFswDJGD9qsVMZJVx5WmflBzdx8y/1AD+1T77h9icHB9mfHRhH5daR8nWU01LV6Qbd4vbtQPqKguucIzSdu4IMSJlp+5Ot3udwx1bkNVGriq2Dds231klJ3dTpORm6JlTQQULaYqS/2eSQ3EkGOrKRyFwhw95DiAZpThqQX9vTtVVijTmYVR+HChcdgNgyxy0xAgMBAAECggEAGgM10ttcvUZQ6GLm+Cz5Dz8TX2r6MyUAFTMruTJX8RsImOHHTxwPmE2ZFJcDlywjn8a9lIbrodcJ5s91umFDcqgNrQ9O5fn1NyxwelO69HmpRCqPvQTaQ+ZbnMHejxLMhMaXXmp4gIb986IyKHBDbXTpfzjG7sRq8STcLbGDFRLv6SP2qBw2uX2KKX8JYDnISlbZNgp9dnoPuOUzNw8rEu2KA80lRE2KM4xfOkn68Ajz8HUoclhgPh/4VlPg/OqBzh5PSPIY+KGqdfKqAdTStV0U0ILBlCE5WoL3GnJfOculZsrX+7ACTlbi/g6yRhxKhSnpP/OlMXDEgr/yB4F43wKBgQDCQIJ4W6ES2DH3qGjRIWLoTitetXUMaZuK+lRbPilLsxarj3FZ7H1/t7Te22wiOCp4eoFYQ0u63ya5Ikgzd6aKFpPEUKuGJ6aBB0svfr8OBJuwtDSWl3G3cb2Od6NpVIq1HBkINTKnY/QruTuXNttx7mXKQjW4FHHXgbnkgeG/WwKBgQDjhGBBjeqgmkaxFrRVZumkYdJhgU247n4ikAh7Ehte6HegYrg2JZBQ7JqcdsGijJct7QrYfzyroiYUr+GrH/j/Xqj0lnBm4lJY5EN+LLD8yyRx7mbLBUweJOFFkhmc12MHyox/UyZ2At7aKI6Uf+wKd4jeuXsiVuDNwZ+sZCsXYwKBgAytUxZxvGhTbadg+T40tJS+jTwIEZR2y+zc+2Zc/yrujBs0KEybD3GnVol4vmzZR4RHUmulMKsIZymL4DRjqZ23bXtRXHBL5CTlifWWivdqO5Ljn874ITa8mIdUrXhxSQAazlNnzV95OXUlCIuMy/N6gHAbtA/IXcmXsL8F7uqjAoGAAUcNA1E4sA4tt3DZMmGRjkq+U63WMeOk8ay9X3OKk83aXhwvzJ4JYWrys043aCJB9xANr4mHXa9bZ2JVchCL5WMyr6zolKtQqw8dEehOVh0N51XfXeR5uPGcEjfvzOGovLJ2d4CQBrmdZrwzkMHnIWfqbNW9y0ORn5Ymv2EQnOECgYBDUjRjpONW2wLuCuD/rxjn8uLGwLFfjL4TyGJqMYxjAG2ZdxexOPE7Xqseot9ETG8uwwZUDkHtRFcU/eq7Y3MqryuhS0fqmsjU9+tnBw/irDv3Tiy6OKkZopdokge4r+n5D+6ga014o3lRzGsIBqgn+7Z5zqYkET8/GI5bBE3P9g==";

        String keyde = RSAUtils.decryptFromBase64(keyBase64, RSAUtils.base64ToPrivateKey(privateKey));

        String cipertext = "rmefLxwNzTGLPaiaKirzFNMlmM7kLOqLJovKqSg56xZ0YO1sf0aFY1TU6Xa7cishdm1/2Zjgc4sKjZd+c/X9RwojaMc/PeLsD5ReaOB8NEEN0YzNjBvBrlkdB4/4nrCndT2fWKbi9ZoU";

        System.out.println(AESUtils.decrypt(keyde, iv, cipertext));

    }

    @Test
    public void generetor() throws Exception{


    }

    @Test
    public void test6() throws Exception{
        RouteDefinitionDO routeDefinitionDO = new RouteDefinitionDO();
        routeDefinitionDO.setId("base-app");
        routeDefinitionDO.setOrder(1);
        routeDefinitionDO.setUri(new URI("lb://base-service"));

        PredicateDefinitionDO predicateDefinitionDO = new PredicateDefinitionDO();
        predicateDefinitionDO.setName("Path");

        FilterDefinitionDO filterDefinitionDO = new FilterDefinitionDO();
        filterDefinitionDO.setName("Header");

        routeDefinitionDO.setPredicates(Collections.singletonList(predicateDefinitionDO));
        routeDefinitionDO.setFilters(Collections.singletonList(filterDefinitionDO));

        System.out.println(JSON.toJSONString(routeDefinitionDO));
    }

    public static void main(String[] args) {
        CodeGenerator.generateByCustomTemplate("jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8",
                "root","123456");
    }


}
