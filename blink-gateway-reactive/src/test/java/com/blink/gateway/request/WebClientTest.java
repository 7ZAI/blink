package com.blink.gateway.request;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.blink.base.dto.req.QueryBlinkChannelReqDTO;
import com.blink.base.dto.req.SysLoginReqDTO;
import com.blink.base.dto.rsp.QueryBlinkChannelRspDTO;
import com.blink.base.dto.rsp.SysLoginRspDTO;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.signature.HmacSignatureService;
import com.blink.gateway.signature.SignatureServiceFactory;
import com.blink.gateway.util.WebClientUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.blink.gateway.constant.GatewayConstant.*;
import static com.blink.gateway.constant.GatewayConstant.KEY_APPKEY;

/**
 * 启动网关服务后 运行测试方法 测试
 * 测试
 * @Author binblink
 */
public class WebClientTest {


    private WebClient.Builder webClientBuilder = WebClient.builder();

    private HmacSignatureService signatureService = (HmacSignatureService) new SignatureServiceFactory().getDefaultService();

    private String appSecret = "FL5ibnYjuh9hkDa_BLJ9FzNdCe0e8TOZ1cfeCchz-x8";

    private String appKey = "073c25c6a554ee93675f2c4f3919ed49d921ad35";

    private final String loginName = "test2";
    private final String password = "123456";
    private final String token = "08df01da6921441696d9040784e179f2";

    @Test
    void gatewayTest() throws InterruptedException, ExecutionException {

        String base = "http://localhost:8002/base/channel/getChannelList";

        var queryBlinkChannelReqDTO = new QueryBlinkChannelReqDTO();
        queryBlinkChannelReqDTO.setChannelName("Browser");

        var requestDTO = new RequestDTO<QueryBlinkChannelReqDTO>();
        requestDTO.setBody(queryBlinkChannelReqDTO);

        QueryBlinkChannelRspDTO me = new QueryBlinkChannelRspDTO();
        this.setHeadersAndSign(webClientBuilder,requestDTO,loginName,token);

        WebClient webClient = WebClientUtil.getWebClient(webClientBuilder,base);

        Mono<ResponseDTO<QueryBlinkChannelRspDTO>> mono = WebClientUtil
                .webClientPost(webClient,base,requestDTO,new ParameterizedTypeReference<ResponseDTO<QueryBlinkChannelRspDTO>>(){});

        ResponseDTO<QueryBlinkChannelRspDTO> responseDTO = mono.block();

        System.out.println(responseDTO.toString());
    }

    @Test
    void test2(){
        var queryBlinkChannelReqDTO = new QueryBlinkChannelReqDTO();
        queryBlinkChannelReqDTO.setChannelName("Browser");

        var requestDTO = new RequestDTO<QueryBlinkChannelReqDTO>();
        requestDTO.setBody(queryBlinkChannelReqDTO);
        String bodyStr = JSON.toJSONString(requestDTO);
        System.out.println(bodyStr);

        RequestDTO requestDTO1 = JSON.parseObject(bodyStr, RequestDTO.class);

        requestDTO1.setReqDate(LocalDate.now());
        System.out.println(requestDTO1.toString());
    }

    @Test
    void login(){

        String base = "http://localhost:8002/base/auth/login";

        var loginReqDTO = new SysLoginReqDTO();
        loginReqDTO.setUsername(loginName);
        loginReqDTO.setPassword(password);

        var requestDTO = new RequestDTO<SysLoginReqDTO>();
        requestDTO.setBody(loginReqDTO);

        this.setHeadersAndSign(webClientBuilder,requestDTO,loginName,"");
        WebClient webClient = WebClientUtil.getWebClient(webClientBuilder,base);
        Mono<ResponseDTO<SysLoginRspDTO>> mono = WebClientUtil
                .webClientPost(webClient,base,requestDTO,new ParameterizedTypeReference<>(){});
        ResponseDTO<SysLoginRspDTO> rs = mono.block();

        System.out.println(rs.toString());
        System.out.println("------------------token:"+rs.getBody().getToken());
    }

    /**
     * 组装请求头 未开启加密版本
     * @param builder
     */
    private void setHeadersAndSign(WebClient.Builder builder,RequestDTO requestDTO,String logName,String token) {

        long timestamp = System.currentTimeMillis();
        String nonce = UUID.fastUUID().toString(true);

        String data = JSON.toJSONString(requestDTO);
        Map<String, Object> parameMap = new HashMap<>();
        parameMap.put(KEY_TIMESTAMP, timestamp);
        parameMap.put(KEY_NONCE, nonce);
        parameMap.put(KEY_APPKEY, appKey);
        String sign = signatureService.sign(data,appSecret,parameMap);

        builder.defaultHeaders(httpHeaders -> {
            httpHeaders.put(SysConstant.X_BLINK_APPKEY, Collections.singletonList(appKey));
            httpHeaders.put(SysConstant.X_BLINK_TOKEN, Collections.singletonList(token));
            httpHeaders.put(SysConstant.X_BLINK_NONCE, Collections.singletonList(nonce));
            httpHeaders.put(SysConstant.X_BLINK_SIGN, Collections.singletonList(sign));
            httpHeaders.put(SysConstant.X_BLINK_TIMESTAMP, Collections.singletonList(String.valueOf(timestamp)));
        });

    }
}
