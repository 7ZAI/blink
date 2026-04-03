package com.blink.gateway;



import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.asymmetric.RSA;
import com.blink.base.dto.req.AddSysUserReq;
import com.blink.base.dto.req.SysLoginReq;
// import com.blink.base.entity.FilterDefinitionDO;  // TODO: Class not found
// import com.blink.base.entity.PredicateDefinitionDO;  // TODO: Class not found
// import com.blink.base.entity.RouteDefinitionDO;  // TODO: Class not found
import com.blink.datasource.code.CodeGenerator;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.utils.AESUtils;
import com.blink.framework.common.utils.EnvReaderUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.common.utils.RSAUtils;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

public class NormalTest {
    // TODO: RouteDefinitionDO class not found, test commented out
    // @Test
    // void testJson(){
    //     String json = ...
    //     RouteDefinitionDO routeDefinitionDO = JacksonUtil.fromJson(json, RouteDefinitionDO.class);
    //     System.out.println(routeDefinitionDO);
    // }

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
    public void test3() throws NoSuchAlgorithmException {

//        System.out.println(AESUtils.encodeToBase64(AESUtils.generateRandomAESKey().getEncoded()));
        System.out.println(EnvReaderUtil.getEnv("BLINK_SECRET_KEY"));
        EnvReaderUtil.printEnvSummary();

//        System.out.println(SecureUtil.sha1().digestHex(RandomUtil.randomString(16)));
//        System.out.println(SecureUtil.hmacSha256().digestBase64(RandomUtil.randomString(32), true));
    }
    @Test
    public void test4(){
        RequestDTO<SysLoginReq> requestDTO = new RequestDTO<>();
        SysLoginReq sysLoginReq = new SysLoginReq();
        sysLoginReq.setPassword("123456");
        sysLoginReq.setUsername("test");
        requestDTO.setChannel("test");
        requestDTO.setVersion("v1");
        requestDTO.setClientIp("192.168.1.3");
        requestDTO.setBody(sysLoginReq);
        System.out.println(JacksonUtil.toJson(requestDTO));
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


    // TODO: AddSysUserReq.setPassword/setConfirmPassword methods not found, test commented out
    // @Test
    // public void test2() throws Exception {
    //     byte[] iv = AESUtils.generateIV();
    //     ...
    // }

    @Test
    public void test8() throws Exception {
        String iv = "FUni5eZ3C5EjVwzR";
        String keyBase64 = "k9uuXhN91pLEAdHIsgEX5eL954gYtsTH+LjNw9QyLPY57RIZs2aoOk7rWNaaJtKF3kGK43Oj+bbe00ufQXPDRk0AdmCJfLLfCjrkk6dMNt4OdRUjMTyqDK/ymhoig1KtLB22Oy60zndfG++0i8DIb3pOOLy2GjOXBWdcOlnc8lEGFRc2St0/kBKhotncWcLFNCMXD/TCQHr1qaFm3DnBDL75vZD6RK1eXb44CzzZ2pWOrciqwGh6pUQkGBLvV/IWrm9Qrvcr1a4o8YDNg/BFf4ggdmQMQLRh1488VhuMUfdpkktJGQfc6BY6u3FBrisJ45I2B0UIjUkGpiH+vWu8Fg==";

        String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCso6X97kzmYAU4a3Lt+gaz7ZemQizg4gHG1Z2ktE7zo0BzTfkMoWgnBXeQdUCjjymndcVvVBKrBiIRyydnx6K9KHa6e6rYbmH/ezA+ZaGdQJfB6j8zoGx80Q9JFKX+NjtqonSKFkIkV3bgLEGfaFsFswDJGD9qsVMZJVx5WmflBzdx8y/1AD+1T77h9icHB9mfHRhH5daR8nWU01LV6Qbd4vbtQPqKguucIzSdu4IMSJlp+5Ot3udwx1bkNVGriq2Dds231klJ3dTpORm6JlTQQULaYqS/2eSQ3EkGOrKRyFwhw95DiAZpThqQX9vTtVVijTmYVR+HChcdgNgyxy0xAgMBAAECggEAGgM10ttcvUZQ6GLm+Cz5Dz8TX2r6MyUAFTMruTJX8RsImOHHTxwPmE2ZFJcDlywjn8a9lIbrodcJ5s91umFDcqgNrQ9O5fn1NyxwelO69HmpRCqPvQTaQ+ZbnMHejxLMhMaXXmp4gIb986IyKHBDbXTpfzjG7sRq8STcLbGDFRLv6SP2qBw2uX2KKX8JYDnISlbZNgp9dnoPuOUzNw8rEu2KA80lRE2KM4xfOkn68Ajz8HUoclhgPh/4VlPg/OqBzh5PSPIY+KGqdfKqAdTStV0U0ILBlCE5WoL3GnJfOculZsrX+7ACTlbi/g6yRhxKhSnpP/OlMXDEgr/yB4F43wKBgQDCQIJ4W6ES2DH3qGjRIWLoTitetXUMaZuK+lRbPilLsxarj3FZ7H1/t7Te22wiOCp4eoFYQ0u63ya5Ikgzd6aKFpPEUKuGJ6aBB0svfr8OBJuwtDSWl3G3cb2Od6NpVIq1HBkINTKnY/QruTuXNttx7mXKQjW4FHHXgbnkgeG/WwKBgQDjhGBBjeqgmkaxFrRVZumkYdJhgU247n4ikAh7Ehte6HegYrg2JZBQ7JqcdsGijJct7QrYfzyroiYUr+GrH/j/Xqj0lnBm4lJY5EN+LLD8yyRx7mbLBUweJOFFkhmc12MHyox/UyZ2At7aKI6Uf+wKd4jeuXsiVuDNwZ+sZCsXYwKBgAytUxZxvGhTbadg+T40tJS+jTwIEZR2y+zc+2Zc/yrujBs0KEybD3GnVol4vmzZR4RHUmulMKsIZymL4DRjqZ23bXtRXHBL5CTlifWWivdqO5Ljn874ITa8mIdUrXhxSQAazlNnzV95OXUlCIuMy/N6gHAbtA/IXcmXsL8F7uqjAoGAAUcNA1E4sA4tt3DZMmGRjkq+U63WMeOk8ay9X3OKk83aXhwvzJ4JYWrys043aCJB9xANr4mHXa9bZ2JVchCL5WMyr6zolKtQqw8dEehOVh0N51XfXeR5uPGcEjfvzOGovLJ2d4CQBrmdZrwzkMHnIWfqbNW9y0ORn5Ymv2EQnOECgYBDUjRjpONW2wLuCuD/rxjn8uLGwLFfjL4TyGJqMYxjAG2ZdxexOPE7Xqseot9ETG8uwwZUDkHtRFcU/eq7Y3MqryuhS0fqmsjU9+tnBw/irDv3Tiy6OKkZopdokge4r+n5D+6ga014o3lRzGsIBqgn+7Z5zqYkET8/GI5bBE3P9g==";

        String keyde = RSAUtils.decryptFromBase64(keyBase64, RSAUtils.base64ToPrivateKey(privateKey));

        String cipertext = "rmefLxwNzTGLPaiaKirzFNMlmM7kLOqLJovKqSg56xZ0YO1sf0aFY1TU6Xa7cishdm1/2Zjgc4sKjZd+c/X9RwojaMc/PeLsD5ReaOB8NEEN0YzNjBvBrlkdB4/4nrCndT2fWKbi9ZoU";

        System.out.println(AESUtils.decrypt(AESUtils.keyFromBase64(keyde), AESUtils.ivFromBase64(iv), cipertext));

    }

    @Test
    public void generetor() throws Exception{


    }

    // TODO: RouteDefinitionDO, PredicateDefinitionDO, FilterDefinitionDO classes not found, test commented out
    // @Test
    // public void test6() throws Exception{
    //     RouteDefinitionDO routeDefinitionDO = new RouteDefinitionDO();
    //     ...
    // }

    @Test
    void colUtilsTest(){

        List<Integer> list1 = new ArrayList<>();
//        list1.add(1);
//        list1.add(2);
//        list1.add(3);
//        list1.add(4);
//        list1.add(5);
//        list1.add(22);
        List<Integer> list2 = new ArrayList<>();
        list2.add(1);
        list2.add(4);
        list2.add(9);
        list2.add(4);
        list2.add(5);
        list2.add(3);

        List<Integer> reuslt1 = CollUtil.subtractToList(list1,list2);
        List<Integer> reuslt2 = CollUtil.subtractToList(list2,list1);

        System.out.println(reuslt1);
        System.out.println(reuslt2);
    }

    public static void main(String[] args) {
        CodeGenerator.generateByCustomTemplate("jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8",
                "root","123456");
    }




}
