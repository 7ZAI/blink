package com.blink.gateway;

import cn.hutool.core.lang.UUID;
import com.blink.base.dto.CacheMsg;
import com.blink.framework.common.utils.RSAUtils;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.signature.HmacSignatureService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author binblink
 * @Date 2025/8/27
 */

public class TestReactive {
    @Test
    public void test(){
       Mono<Object> mono   =  Mono.empty().switchIfEmpty(Mono.error(new RuntimeException("未登录或 Token 已过期"))).flatMap(v->{
            v = "fffff";
            return Mono.just(v);
        });

        System.out.println(mono.block());
    }

    @Test
    public void test2(){
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.fastUUID().toString(true);

        timestamp = Long.valueOf("1761222398047");
        nonce = "50a91b9c-4da3-4478-9aec-4c3b2b91aa0d";
        System.out.println(timestamp);
        System.out.println(nonce);
        String appKey = "073c25c6a554ee93675f2c4f3919ed49d921ad35";

        HmacSignatureService signatureService = new HmacSignatureService();
        Map<String, Object> parameMap = new HashMap<>();
        parameMap.put("timeStamp", timestamp);
        parameMap.put("nonce", nonce);
        parameMap.put("loginName", "test1");
        parameMap.put("appKey", appKey);

        String json = "{\n" +
                "  \"body\": {\n" +
                "    \"password\": \"123456\",\n" +
                "    \"username\": \"test1\"\n" +
                "  },\n" +
                "  \"channel\": \"test\",\n" +
                "  \"clientIp\": \"192.168.1.3\",\n" +
                "  \"version\": \"v1\"\n" +
                "}";

        String json2 = "\"{\"body\":{\"password\":\"123456\",\"username\":\"test1\"},\"channel\":\"test\",\"clientIp\":\"192.168.1.3\",\"version\":\"v1\"}";

       String signStr = signatureService.sign(json2, "FL5ibnYjuh9hkDa_BLJ9FzNdCe0e8TOZ1cfeCchz-x8",parameMap);

        System.out.println(signStr);

    }

    @Test
    public void test3(){

        String encodetxt ="gpjn1BJku1svIpyi+jKGtfuJFOlQZWhyvPlpd3HSQ2wbfmp+lA/xMIikSDglbN7ij8P/yGuy+S1+r3yfCKdmgbIUBGfgYqlQJGih5UlaIREqyt+yNCdwSR7RnaNYoKrvd8uExKurNTOxBLUmtFqgNyuvq5yeBT7zxYyUdRlfcjAC6SxfDvVQbLrJ+tH2/jeiK/WRwbansCnn2pqMoMUX+3Wj5pXHH+/rjopMcN2MoSn1TjFGFAtmKyuhFBeEudZHRahkpKmIi5zQ+E9xJ+GB3PPXWnrQsKo8xuA7Ax+tW8/pBklMTtYjrylkvcRm3ZvD8LoEegnhmozVnr98t85v1A==";
        String pubkeypem ="-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn9L+mIynGMHs4F6ZYRP+\n" +
                "cdr7MRgFZecF27qM4Ejl57U/RDxDwIXHoC6qum14wgO39hARaWxdpKTI7pgbHEsC\n" +
                "yXqssctzsmiwsZv+FmnJxZ8ril3QmNSx+GWjOBbT2FCkV7TfwJ1LRLR6wiwrQ5UD\n" +
                "SmnpSe7ie7amtf8wATj2KmSjd32oQb9rgJ6EC2n6yPHFw113ZKS6kyXoeHxChiEo\n" +
                "Ad9yXJ1s550IOAfXre/FFhUfoAs+s+jTAov4FHhYe3gO+fn7wXwtAb+HhjGqPAma\n" +
                "gGfSu8aIYIiZ3GD/eeUcdZliuod1mrwtpbM2yHqsAdlS64MQ92bV5up2hfTdZVOg\n" +
                "2QIDAQAB\n" +
                "-----END PUBLIC KEY-----";

        String pubkey = pubkeypem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", ""); // 移除所有空白字符

        String palin = "apifox收到JFK附件是打开风格和*&……36823";

        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCTnlcG4OlC+SGz\n" +
                "kKoLMODz6dir2nuRvM6HJDVPG6N2Eyix7Svm6Lu2vgIUxQxpVTIupZlWqFCsXceu\n" +
                "/OpEyCGfR2ZSxflUdbxvXUpqbmSck9zdUlsilMP7ZKoyPO1i/IKzRcqw4oKAnkLN\n" +
                "vnn4ieDD1221rb9+0PYhZhXqEcteatJK9XmsY7TgOBeBAvKm4Bra9u61+sZbwloU\n" +
                "5qusaLIHhnhhnA8lWHkp2zLn33FCDL7Ql1sZlqrGKz2D4Zl7KZ+crAqdA2tIJEp4\n" +
                "wLh+5rXA/wtbtYeAG2o1JQbJ/XrSzsRHSOZrSn3BkvB4XW1qrhA48CyeOe4tLwOj\n" +
                "k7QHzJ4NAgMBAAECggEASlb3JLOnRN8JpdA4kiIvZyZgw0rn5+DZcR0+63PTweas\n" +
                "G86AyWxy+/h8O9FwCquo9ezXZ9ijp+oiYXFk2PagWAIw+YZRpcMU/lBQ5YBqkp+0\n" +
                "HpEW7DdW6wcq3tnoAm1/tKB1DtdDOVp58fr2mb91vaqDyXaSdV7uVKuAZbxIWmdV\n" +
                "Se4DWgZ7vyDJMBp9PWrN8KtRnzLELQrMeYqiSQRKOfoXV1fp6yVNleJMRSH9lYY+\n" +
                "e+u5fHHL02a3onb3NiTqqq0sI1Szy903SyJVshBlOJIPwwJL8TVCEKRWYCCeVp7b\n" +
                "tsq3VQBRTL0GYvsL03mdzduoV710qQKvh6NQOHM7UQKBgQDJruF1V85mRmCbKQzO\n" +
                "Wj2siEExyyErHYFdMCoGoxtjdKdL2g3664m5ltv+rgQ/KosgLXux/dz+65F0OqNt\n" +
                "vRB9wfIwAu+apN4ToEm7ovvS6kUKCwiKzpleGt1LWutfhrcYYxX7P0GYsO9wBS43\n" +
                "0ftA/2hDQlQwA56rzdjFWymnpwKBgQC7X/RETozC0FnrOb6AM2DoaqcTXmu/DJ6N\n" +
                "WbbAP9Ogb2ftwG6zje018l+WNlUrxCOr1nCZr9IOWpi1M+hOs2yOa197mD2FfLNP\n" +
                "YoUxAva2dpymxXmFre4+ElmMjJWzvI3tMIFeSF1cuebGKqIcIpmrBVU6Z6xvBoo6\n" +
                "CAh/8UWDKwKBgE/chvxvM5S+I3ztSVvg0fEi1yX/eHfZLCOZcdrE9ZBxXdmkU8rb\n" +
                "P+MUgLKdd6MHlqW8PPHjQ4JSB3vNyG8o4YHarasjTnue8y00WF4LkMoY6BOtAMnu\n" +
                "Oz7PRneTXgaqko/+lHKzvqbW0Uk7U+zYojQ/rqqLdki49jizvzWO/rSbAoGAC0vS\n" +
                "nxeZNtl2wk6d5H60de6QGgPUPRVWrbDd6qMub/qnalbylZKV3W26Q4UNB3FwsRhh\n" +
                "e3J1GHZvxGTXuX7pqYBhpkLcWqLU8JFA9F54XzGxagB3vg5pUYarrWSAVfClIcqN\n" +
                "0hBMyJ1Qg1vooAeZ+gx9QeMkKyFazfrUAFqcVTcCgYBnd4UrRa7R+Eyp22uhILtS\n" +
                "6exlcNZ1fuEAQzsJ0RtNLFx3cqLOPYd1neom1kLvK4w29LHuq1J89B9MS5xHYBeB\n" +
                "QW3gDKQth+ZQ0PEvsidD5KwKLnCi7l+XxBSvckQVHlIhqhbhRK1Mzh3akLurpZVo\n" +
                "jAFibj+L6yUV6W/fQtyM1g==\n" +
                "-----END PRIVATE KEY-----";
        String privateKeyContent = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", ""); // 移除所有空白字符

//        String encode = RSAUtils.encryptToBase64(palin, RSAUtils.base64ToPublicKey(pubkey));
        System.out.println(privateKeyContent);

        String msg = RSAUtils.decryptFromBase64(encodetxt, RSAUtils.base64ToPrivateKey(privateKeyContent));
        System.out.println(new String(Base64.getDecoder().decode(msg)));
//        String pubkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu/fTua2t/kSKeMQFbUB8T0k1KhB+ffDVr2OrpPXvt2XJZS3IkUkSGKQiPKuT6qeNyn2wexg5vNKXGAWZ4U5thUKezhHUUDtbWpEv7Og/SSsqOQp7tJLuKd93w9Jk8byVWWqTuQK4Bsw52LANr6DN1XK5m5iCXUU2+cxFNqLXr5OR0kO4AEoUnG84kXQ3fDPZ0E5r7d0LR02sXEgh2XBwHXAyA2zPcNoaVq9jeL1LLviQ1woSmC8xlA9PwtrRqEtH6C1K2//eCtnMVCquofjNqf3BxbqGpJ9sJep9nw28s/mOVF89h83TB73YubCjKGsC594QZDbLdyKoq5oYgJjHJQIDAQAB";
//        RSAUtils.base64ToPublicKey(pubkey);
//
//        String plaintxt = "你好发到付顺丰";
//        String encryptedTxt = RSAUtils.encryptToBase64(plaintxt,RSAUtils.base64ToPublicKey(pubkey));
//
//        System.out.println(RSAUtils.decryptFromBase64(encryptedTxt, RSAUtils.base64ToPrivateKey(privateKeyBase64)));
        System.out.println(msg);
    }

    @Test
    public void encrypt(){
        String txt = "撒旦金克拉 打发士大夫就哦啊sddgdfgdasdop134eu90sajdfj&%^**&Y()";

        String pub = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArKOl/e5M5mAFOGty7foGs+2XpkIs4OIBxtWdpLRO86NAc035DKFoJwV3kHVAo48pp3XFb1QSqwYiEcsnZ8eivSh2unuq2G5h/3swPmWhnUCXweo/M6BsfNEPSRSl/jY7aqJ0ihZCJFd24CxBn2hbBbMAyRg/arFTGSVceVpn5Qc3cfMv9QA/tU++4fYnBwfZnx0YR+XWkfJ1lNNS1ekG3eL27UD6ioLrnCM0nbuCDEiZafuTrd7ncMdW5DVRq4qtg3bNt9ZJSd3U6TkZuiZU0EFC2mKkv9nkkNxJBjqykchcIcPeQ4gGaU4akF/b07VVYo05mFUfhwoXHYDYMsctMQIDAQAB";

        String encode = RSAUtils.encryptToBase64(txt,RSAUtils.base64ToPublicKey(pub));

        String secret = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCso6X97kzmYAU4a3Lt+gaz7ZemQizg4gHG1Z2ktE7zo0BzTfkMoWgnBXeQdUCjjymndcVvVBKrBiIRyydnx6K9KHa6e6rYbmH/ezA+ZaGdQJfB6j8zoGx80Q9JFKX+NjtqonSKFkIkV3bgLEGfaFsFswDJGD9qsVMZJVx5WmflBzdx8y/1AD+1T77h9icHB9mfHRhH5daR8nWU01LV6Qbd4vbtQPqKguucIzSdu4IMSJlp+5Ot3udwx1bkNVGriq2Dds231klJ3dTpORm6JlTQQULaYqS/2eSQ3EkGOrKRyFwhw95DiAZpThqQX9vTtVVijTmYVR+HChcdgNgyxy0xAgMBAAECggEAGgM10ttcvUZQ6GLm+Cz5Dz8TX2r6MyUAFTMruTJX8RsImOHHTxwPmE2ZFJcDlywjn8a9lIbrodcJ5s91umFDcqgNrQ9O5fn1NyxwelO69HmpRCqPvQTaQ+ZbnMHejxLMhMaXXmp4gIb986IyKHBDbXTpfzjG7sRq8STcLbGDFRLv6SP2qBw2uX2KKX8JYDnISlbZNgp9dnoPuOUzNw8rEu2KA80lRE2KM4xfOkn68Ajz8HUoclhgPh/4VlPg/OqBzh5PSPIY+KGqdfKqAdTStV0U0ILBlCE5WoL3GnJfOculZsrX+7ACTlbi/g6yRhxKhSnpP/OlMXDEgr/yB4F43wKBgQDCQIJ4W6ES2DH3qGjRIWLoTitetXUMaZuK+lRbPilLsxarj3FZ7H1/t7Te22wiOCp4eoFYQ0u63ya5Ikgzd6aKFpPEUKuGJ6aBB0svfr8OBJuwtDSWl3G3cb2Od6NpVIq1HBkINTKnY/QruTuXNttx7mXKQjW4FHHXgbnkgeG/WwKBgQDjhGBBjeqgmkaxFrRVZumkYdJhgU247n4ikAh7Ehte6HegYrg2JZBQ7JqcdsGijJct7QrYfzyroiYUr+GrH/j/Xqj0lnBm4lJY5EN+LLD8yyRx7mbLBUweJOFFkhmc12MHyox/UyZ2At7aKI6Uf+wKd4jeuXsiVuDNwZ+sZCsXYwKBgAytUxZxvGhTbadg+T40tJS+jTwIEZR2y+zc+2Zc/yrujBs0KEybD3GnVol4vmzZR4RHUmulMKsIZymL4DRjqZ23bXtRXHBL5CTlifWWivdqO5Ljn874ITa8mIdUrXhxSQAazlNnzV95OXUlCIuMy/N6gHAbtA/IXcmXsL8F7uqjAoGAAUcNA1E4sA4tt3DZMmGRjkq+U63WMeOk8ay9X3OKk83aXhwvzJ4JYWrys043aCJB9xANr4mHXa9bZ2JVchCL5WMyr6zolKtQqw8dEehOVh0N51XfXeR5uPGcEjfvzOGovLJ2d4CQBrmdZrwzkMHnIWfqbNW9y0ORn5Ymv2EQnOECgYBDUjRjpONW2wLuCuD/rxjn8uLGwLFfjL4TyGJqMYxjAG2ZdxexOPE7Xqseot9ETG8uwwZUDkHtRFcU/eq7Y3MqryuhS0fqmsjU9+tnBw/irDv3Tiy6OKkZopdokge4r+n5D+6ga014o3lRzGsIBqgn+7Z5zqYkET8/GI5bBE3P9g==";


        String decode = RSAUtils.decryptFromBase64(encode,RSAUtils.base64ToPrivateKey(secret));

        System.out.println(encode);
        System.out.println(decode);
    }

    @Test
    public void jsontest(){
        CacheMsg cacheMsg = new CacheMsg();
        cacheMsg.setKey("asdasd:gdsgf:4tt:rrr");
        StreamMessage<CacheMsg> m = StreamMessage.of("dasd", "rs", cacheMsg);

        Map<String,Object> map = StreamMessage.convertMessageToMap(m);

        StreamMessage<CacheMsg> k = (StreamMessage<CacheMsg>) StreamMessage.convertMapToMessage(map,CacheMsg.class);

        System.out.println(k);
    }

}
