package com.blink.gateway.request;

import cn.hutool.core.lang.UUID;
import com.blink.base.dto.rsp.QueryBlinkChannelRsp;

import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.factory.BlinkNamedThreadFactory;
import com.blink.framework.common.utils.AESUtils;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.common.utils.RSAUtils;
import com.blink.gateway.dto.req.QueryBlinkChannelReq;
import com.blink.gateway.signature.HmacSignatureService;
import com.blink.gateway.signature.SignatureServiceFactory;
import com.blink.gateway.util.WebClientUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.blink.gateway.constant.GatewayConstant.*;

/**
 * 发起http请求 对网关和base-app联调测试
 * 启动网关服务 启动base-app服务后测试
 * 运行测试方法 测试
 *
 * @Author binblink
 */
public class WebClientTest {


    private WebClient.Builder webClientBuilder = WebClient.builder();

    private HmacSignatureService signatureService = (HmacSignatureService) new SignatureServiceFactory().getDefaultService();

    private String appSecret = "KLrmMSMBAb3f1bd7euUs8ks6W_mZVmAponfE1WKbjxI";

    private String appKey = "b8366e81a1d21ceb035d09a6c5251587e77c4309";

    private final String loginName = "test2";
    private final String password = "123456";
    private final String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNCIsImlhdCI6MTc3MTQ2ODYyMSwiaXNzIjoiYmFzZS1hcHAiLCJhdWQiOlsiQnJvd3NlciJdLCJleHAiOjE3NzE0Njk1MjEsInR5cGUiOiJhY2Nlc3MifQ.46JtkNsRg7Fe0DwgJQTm88NBEWXFv_yXcRiZBDQDdZA";

    //系统公钥
    private final String systemPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlRqb7VG+lkiTH/8LY4ISQLjD2t+8kZMvthvixlIx57v3d5o994PY0/QFqOPqDdJeXIvxiCA0z/qdMGve3t2lJuUiExNmH+pY46LuNMIyzmiHKliocDCFb1bdVoTWHzJmjDT2TnRxmglVVm4mhlpDS18accZVPXdzESCn32HfmhKkQj+0NTdsPzjlpWsfsXpySToPVa8/U1HupTnRibdsCu80PHCjRwf/3+fj9fBRnNCubJoSlOi4o+koojqQ3vCMc+b+6dW6zYS83g67olT9J77ekOru/+OgWYe3FmBSjhiYAIMSwK1PalyvI9S3V57SdkHkwG72UrnsIP7iE5BSKwIDAQAB";
    //系统私钥
    private final String systemPrivateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCVGpvtUb6WSJMf/wtjghJAuMPa37yRky+2G+LGUjHnu/d3mj33g9jT9AWo4+oN0l5ci/GIIDTP+p0wa97e3aUm5SITE2Yf6ljjou40wjLOaIcqWKhwMIVvVt1WhNYfMmaMNPZOdHGaCVVWbiaGWkNLXxpxxlU9d3MRIKffYd+aEqRCP7Q1N2w/OOWlax+xenJJOg9Vrz9TUe6lOdGJt2wK7zQ8cKNHB//f5+P18FGc0K5smhKU6Lij6SiiOpDe8Ixz5v7p1brNhLzeDruiVP0nvt6Q6u7/46BZh7cWYFKOGJgAgxLArU9qXK8j1LdXntJ2QeTAbvZSuewg/uITkFIrAgMBAAECggEAA60TKNthFYk6TntrfK8EDmlVF0bOMyXy8vcfQxXNCEsk+McGvd0Jo7kFHyWEsqIYeCVtys7wd0g4gFgBYnMKRVaNwS3Be3v0vYY9RwHwCPSEHtIK+mke8tlW9sZx3JG1UYjy9vrhTbAHcXq9TjFYZvKl+dCSqBkTYPZHl+/eeIjUzqzFUemqylG+SACsUU8/FoWOIIMA/nU+YHu9rMdzwPb7FVWlbFLhnP3c0Id7tEmmTgpnjxps9RwYAYEC2yjXVB4vLXSKLQFz7xMdgiZKhtnk4WGVs/hX/QSUn92toIdzTTB7NLvxZ0Ca9F6fSKkr7DQTV0E/PVy6PxXDAodhcQKBgQC4JGitVvDCM8bmWTcklrF0L4sRkQsb4nUTM/pOoCBUhFFmRknfemkuXDCgMyW+CR3WkzFA5O6CH6qheNsWgmt9jZd2lKTNuDzcf10v3EOl72FkGjvXMhzuEuIrf7PipwRb83maziYXkPgH53CwkO1echD7eZXzLv6AhaQQjIz3dwKBgQDPSerQkwUrwSQ7gk7UTslaIvJ0DsBrLNZ1NJC/SGde3MKx/cgcP1n1+a0wyryGzCJlHntC4855fG/+uPeydNyLR5kfwCnoXeduxklOqa+01ZMAnd4ZtIY8iOOalkYGoctYStOtjZ1ytEg+FxEGXJLMxSIXHlXaaaA7J5afWDrP7QKBgQCakR04myi+qr2DL1IFKWTNFPdUCH7dqioBs3ihNPHOX77/7XIm31aYrpO2dRhyOq7MJYOLaF735OLSZWLpsxHNMuP3inAHqmWT5GjxOp+iEpyabRbb3NnB0SH9x1TUpMMY9/eURQRedbZs1A2YeB53T6IHpJiEp2bDtOnxZjkRowKBgBuG5l7iVzpzQJuCd3NsLwMARxAwcU8KOkSAQYSVWdzYFibK28scgpbOnDgxhA2miB7DiHzeganSY5EBASu6pxr73BLCUw5fgf9M56lBkWrDPe1ECZ2CQp1BVHBP6maxD67e6PNQLgtwc3ODXqhonLoCp79IMQB7LGMOo8rvTYgVAoGBAJZXtFcnz5zY5v5N46BVV9oH9KGRfybppmtOjoXWIyk4qZq79lmaAbVv7MOWNl0eIi/lTHi9iuExvOUX8CO6qxTfdXcmCgOfu+R9qrakdU9RHntB4nYfwWq9/DNoRZtJnXW+TVYMCUo6GB6/r0MdZVBReO7dBcPRFa2cgiIcueOU";
    //渠道私钥
    private final String channelPrivateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCso6X97kzmYAU4a3Lt+gaz7ZemQizg4gHG1Z2ktE7zo0BzTfkMoWgnBXeQdUCjjymndcVvVBKrBiIRyydnx6K9KHa6e6rYbmH/ezA+ZaGdQJfB6j8zoGx80Q9JFKX+NjtqonSKFkIkV3bgLEGfaFsFswDJGD9qsVMZJVx5WmflBzdx8y/1AD+1T77h9icHB9mfHRhH5daR8nWU01LV6Qbd4vbtQPqKguucIzSdu4IMSJlp+5Ot3udwx1bkNVGriq2Dds231klJ3dTpORm6JlTQQULaYqS/2eSQ3EkGOrKRyFwhw95DiAZpThqQX9vTtVVijTmYVR+HChcdgNgyxy0xAgMBAAECggEAGgM10ttcvUZQ6GLm+Cz5Dz8TX2r6MyUAFTMruTJX8RsImOHHTxwPmE2ZFJcDlywjn8a9lIbrodcJ5s91umFDcqgNrQ9O5fn1NyxwelO69HmpRCqPvQTaQ+ZbnMHejxLMhMaXXmp4gIb986IyKHBDbXTpfzjG7sRq8STcLbGDFRLv6SP2qBw2uX2KKX8JYDnISlbZNgp9dnoPuOUzNw8rEu2KA80lRE2KM4xfOkn68Ajz8HUoclhgPh/4VlPg/OqBzh5PSPIY+KGqdfKqAdTStV0U0ILBlCE5WoL3GnJfOculZsrX+7ACTlbi/g6yRhxKhSnpP/OlMXDEgr/yB4F43wKBgQDCQIJ4W6ES2DH3qGjRIWLoTitetXUMaZuK+lRbPilLsxarj3FZ7H1/t7Te22wiOCp4eoFYQ0u63ya5Ikgzd6aKFpPEUKuGJ6aBB0svfr8OBJuwtDSWl3G3cb2Od6NpVIq1HBkINTKnY/QruTuXNttx7mXKQjW4FHHXgbnkgeG/WwKBgQDjhGBBjeqgmkaxFrRVZumkYdJhgU247n4ikAh7Ehte6HegYrg2JZBQ7JqcdsGijJct7QrYfzyroiYUr+GrH/j/Xqj0lnBm4lJY5EN+LLD8yyRx7mbLBUweJOFFkhmc12MHyox/UyZ2At7aKI6Uf+wKd4jeuXsiVuDNwZ+sZCsXYwKBgAytUxZxvGhTbadg+T40tJS+jTwIEZR2y+zc+2Zc/yrujBs0KEybD3GnVol4vmzZR4RHUmulMKsIZymL4DRjqZ23bXtRXHBL5CTlifWWivdqO5Ljn874ITa8mIdUrXhxSQAazlNnzV95OXUlCIuMy/N6gHAbtA/IXcmXsL8F7uqjAoGAAUcNA1E4sA4tt3DZMmGRjkq+U63WMeOk8ay9X3OKk83aXhwvzJ4JYWrys043aCJB9xANr4mHXa9bZ2JVchCL5WMyr6zolKtQqw8dEehOVh0N51XfXeR5uPGcEjfvzOGovLJ2d4CQBrmdZrwzkMHnIWfqbNW9y0ORn5Ymv2EQnOECgYBDUjRjpONW2wLuCuD/rxjn8uLGwLFfjL4TyGJqMYxjAG2ZdxexOPE7Xqseot9ETG8uwwZUDkHtRFcU/eq7Y3MqryuhS0fqmsjU9+tnBw/irDv3Tiy6OKkZopdokge4r+n5D+6ga014o3lRzGsIBqgn+7Z5zqYkET8/GI5bBE3P9g==";


    /**
     * 登入
     */
    // @Test
    // void login() {

    //     String base = "http://localhost:8002/base/auth/login";


    //     var loginReqDTO = new SysLoginReq();
    //     loginReqDTO.setUsername(loginName);
    //     loginReqDTO.setPassword(password);

    //     var requestDTO = new RequestDTO<SysLoginReq>();
    //     requestDTO.setBody(loginReqDTO);

    //     this.setHeadersAndSign(webClientBuilder, requestDTO, "");
    //     WebClient webClient = WebClientUtil.getWebClient(webClientBuilder, base);

    //     Mono<ResponseDTO<SysLoginRsp>> mono = WebClientUtil
    //             .webClientPost(webClient, base, requestDTO, new ParameterizedTypeReference<>() {
    //             });
    //     ResponseDTO<SysLoginRsp> rs = mono.block();

    //     System.out.println(rs.toString());
    //     System.out.println("------------------token:" + rs.getBody().getToken());
    // }

    @Test
    void channelGetToken(){
        String base = "http://localhost:8002/channel/issueChannelToken";
    }

    /**
     * 测试获取渠道接口
     */
    @Test
    void gatewayTest() {

        String base = "http://localhost:8002/base/channel/getChannelList";

        var queryBlinkChannelReqDTO = new QueryBlinkChannelReq();
        queryBlinkChannelReqDTO.setChannelName("Browser");

        var requestDTO = new RequestDTO<QueryBlinkChannelReq>();
        requestDTO.setBody(queryBlinkChannelReqDTO);

        this.setHeadersAndSign(webClientBuilder, requestDTO, token);

        WebClient webClient = WebClientUtil.getWebClient(webClientBuilder, base);

        Mono<ResponseDTO<QueryBlinkChannelRsp>> mono = WebClientUtil
                .webClientPost(webClient, base, requestDTO, new ParameterizedTypeReference<ResponseDTO<QueryBlinkChannelRsp>>() {
                });

        ResponseDTO<QueryBlinkChannelRsp> responseDTO = mono.block();

        System.out.println("<=== " + JacksonUtil.toJson(responseDTO));
    }

    /**
     * 登入 加密版
     * 测试 加密发送 响应解密 对应渠道encryptionSwitch 应设置为0
     */
    // @Test
    // void encryptLogin() throws Exception {

    //     String base = "http://localhost:8002/base/auth/login";

    //     var loginReqDTO = new SysLoginReq();
    //     loginReqDTO.setUsername(loginName);
    //     loginReqDTO.setPassword(password);

    //     var requestDTO = new RequestDTO<SysLoginReq>();
    //     requestDTO.setBody(loginReqDTO);

    //     SecretKey key = AESUtils.generateRandomAESKey();
    //     byte[] ivArr = AESUtils.generateIV();
    //     String iv = AESUtils.encodeToBase64(ivArr);
    //     String keyBase64 = AESUtils.encodeToBase64(key.getEncoded());

    //     String plainTxt = JacksonUtil.toJson(requestDTO);
    //     //aes 加密请求体json字符串
    //     String encryptTxt = AESUtils.encrypt(key, ivArr, plainTxt);

    //     PublicKey publicKey = RSAUtils.base64ToPublicKey(systemPublicKey);
    //     //RSA加密 aes密钥
    //     String aesKeyAfterRSA = RSAUtils.encryptToBase64(keyBase64, publicKey);

    //     System.out.println("key:" + aesKeyAfterRSA);

    //     this.setHeadersEncrypt(webClientBuilder, requestDTO, "", aesKeyAfterRSA, iv);
    //     WebClient webClient = WebClientUtil.getWebClient(webClientBuilder, base);
    //     Mono<WebClientUtil.ApiResponse> rsp = WebClientUtil.webClientPost(webClient, base, encryptTxt);

    //     WebClientUtil.ApiResponse apiResponse = rsp.block();
    //     System.out.println("响应状态码：" + apiResponse.getStatusCode().toString());

    //     for (Map.Entry<String, List<String>> entrty : apiResponse.getHeaders().entrySet()) {
    //         System.out.println("响应头：" + entrty.getKey() + ":" + entrty.getValue().get(0));
    //     }

    //     System.out.println("响应body:" + apiResponse.getBody());

    //     System.out.println("body解密：" + decryptResponseBody(apiResponse));
    // }

    @Test
    void encryptGatewayTest() throws Exception {
        String base = "http://localhost:8002/base/channel/getChannelList";

        var queryBlinkChannelReqDTO = new QueryBlinkChannelReq();
        queryBlinkChannelReqDTO.setChannelName("Browser");

        var requestDTO = new RequestDTO<QueryBlinkChannelReq>();
        requestDTO.setBody(queryBlinkChannelReqDTO);

        SecretKey key = AESUtils.generateRandomAESKey();
        byte[] ivArr = AESUtils.generateIV();
        String iv = AESUtils.encodeToBase64(ivArr);
        String keyBase64 = AESUtils.encodeToBase64(key.getEncoded());

        String plainTxt = JacksonUtil.toJson(requestDTO);
        //aes 加密请求体json字符串
        String encryptTxt = AESUtils.encrypt(key, ivArr, plainTxt);

        PublicKey publicKey = RSAUtils.base64ToPublicKey(systemPublicKey);
        //RSA加密 aes密钥
        String aesKeyAfterRSA = RSAUtils.encryptToBase64(keyBase64, publicKey);

        this.setHeadersEncrypt(webClientBuilder, requestDTO, token,aesKeyAfterRSA, iv);
        WebClient webClient = WebClientUtil.getWebClient(webClientBuilder, base);
        Mono<WebClientUtil.ApiResponse> rsp = WebClientUtil.webClientPost(webClient, base, encryptTxt);


        WebClientUtil.ApiResponse apiResponse = rsp.block();
        System.out.println("响应状态码：" + apiResponse.getStatusCode().toString());


        for (Map.Entry<String, List<String>> entrty : apiResponse.getHeaders().entrySet()) {
            System.out.println("响应头：" + entrty.getKey() + ":" + entrty.getValue().get(0));
        }

        System.out.println("响应body:" + apiResponse.getBody());

        System.out.println("body解密：" + decryptResponseBody(apiResponse));
    }

    private String decryptResponseBody(WebClientUtil.ApiResponse apiResponse) throws Exception {

        String iv = apiResponse.getHeaders().getFirst(SysConstant.X_BLINK_IV);
        String key = apiResponse.getHeaders().getFirst(SysConstant.X_BLINK_KEY);
        String keyOriginal = RSAUtils.decryptFromBase64(key, RSAUtils.base64ToPrivateKey(channelPrivateKey));

        return AESUtils.decrypt(AESUtils.keyFromBase64(keyOriginal), AESUtils.ivFromBase64(iv), apiResponse.getBody());
    }


    /**
     * 验证json转换
     */
    @Test
    void test2() {
        var queryBlinkChannelReqDTO = new QueryBlinkChannelReq();
        queryBlinkChannelReqDTO.setChannelName("Browser");

        var requestDTO = new RequestDTO<QueryBlinkChannelReq>();
        requestDTO.setBody(queryBlinkChannelReqDTO);
        String bodyStr = JacksonUtil.toJson(requestDTO);
        System.out.println(bodyStr);

        RequestDTO requestDTO1 = JacksonUtil.fromJson(bodyStr, RequestDTO.class);

        requestDTO1.setReqDate(LocalDate.now());
        System.out.println(requestDTO1.toString());
    }


    /**
     * 组装请求头 未开启加密版本
     *
     * @param builder
     */
    private void setHeadersAndSign(WebClient.Builder builder, RequestDTO requestDTO, String tokenParam) {

        long timestamp = System.currentTimeMillis();
        String nonce = UUID.fastUUID().toString(true);

        String data = JacksonUtil.toJson(requestDTO);
        Map<String, Object> parameMap = new HashMap<>();
        parameMap.put(KEY_TIMESTAMP, timestamp);
        parameMap.put(KEY_NONCE, nonce);
        parameMap.put(KEY_APPKEY, appKey);
        String sign = signatureService.sign(data, appSecret, parameMap);

        builder.defaultHeaders(httpHeaders -> {
            httpHeaders.put(SysConstant.X_BLINK_APPKEY, Collections.singletonList(appKey));
            httpHeaders.put(SysConstant.X_BLINK_TOKEN, Collections.singletonList(tokenParam));
            httpHeaders.put(SysConstant.X_BLINK_NONCE, Collections.singletonList(nonce));
            httpHeaders.put(SysConstant.X_BLINK_SIGN, Collections.singletonList(sign));
            httpHeaders.put(SysConstant.X_BLINK_TIMESTAMP, Collections.singletonList(String.valueOf(timestamp)));
        });
    }

    private void setHeadersEncrypt(WebClient.Builder builder, RequestDTO requestDTO, String tokenParam, String key, String iv) {

        setHeadersAndSign(builder, requestDTO, tokenParam);

        builder.defaultHeaders(httpHeaders -> {
            httpHeaders.put(SysConstant.X_BLINK_IV, Collections.singletonList(iv));
            httpHeaders.put(SysConstant.X_BLINK_KEY, Collections.singletonList(key));
        });
    }


    @Test
    void mutilThread() throws InterruptedException {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(2,
                4,
                6,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new BlinkNamedThreadFactory.Builder("测试线程").build(),
                new ThreadPoolExecutor.DiscardPolicy());

        for (int i = 0; i < 20 ; i++) {
            executor.submit(this::gatewayTest);
        }


        executor.awaitTermination(10, TimeUnit.SECONDS);
        if(executor.isTerminated()){
            executor.shutdown();
        }

    }


}
