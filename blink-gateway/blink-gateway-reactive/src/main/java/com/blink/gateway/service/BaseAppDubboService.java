package com.blink.gateway.service;

import com.blink.base.dubbo.service.BaseDubboService;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

import static com.blink.gateway.constant.RedisConstans.GATEWAY_CONFIG_KEY_PREFIX;

/**
 * Dubbo 调用内部服务
 * <p>
 * 使用 Dubbo 3.x 原生异步接口（CompletableFuture），无需额外线程池
 * Provider 端在 ForkJoinPool.commonPool() 中执行业务逻辑
 * Consumer 端通过 Mono.fromFuture 非阻塞等待结果
 * </p>
 *
 * @Author blink
 * @Date 2026/03/04
 */
@Slf4j
public class BaseAppDubboService implements BaseAppRemoteService {

    private final BaseDubboService baseDubboService;

    public BaseAppDubboService(BaseDubboService baseDubboService) {
        this.baseDubboService = baseDubboService;
    }

    @Override
    public Mono<SysConfigCacheDO> getOneConfig(String configKey) {
        log.info("Dubbo调用获取配置参数信息 key:{}", configKey);

        if (configKey.contains(GATEWAY_CONFIG_KEY_PREFIX)) {
            configKey = configKey.replace(GATEWAY_CONFIG_KEY_PREFIX, "");
        }

        var requestDTO = new RequestDTO<QueryOneSysConfigReq>();
        var param = new QueryOneSysConfigReq();
        param.setConfigKey(configKey);
        requestDTO.setBody(param);

        // 使用原生异步接口 + 自定义线程池
        CompletableFuture<ResponseDTO<SysConfigCacheDO>> future =
                baseDubboService.getOneConfigAsync(requestDTO);

        return Mono.fromFuture(future)
                .mapNotNull(response -> {
                    if (response == null) {
                        log.error("获取配置参数信息失败: response is null");
                        return null;
                    }
                    return response.getBody();
                })
                .doOnError(e -> log.error("获取配置参数信息失败", e))
                .onErrorResume(e -> Mono.empty());
    }

    @Override
    public Mono<QueryErrMsgRsp> getErrorMsgInfo(String code, String local) {
        log.info("Dubbo调用获取错误提示信息 code:{},language:{}", code, local);

        var requestDTO = new RequestDTO<QueryErrMsgReq>();
        var param = new QueryErrMsgReq();
        param.setCode(code);
        param.setLocal(local);
        requestDTO.setBody(param);

        return Mono.fromFuture(baseDubboService.getErrorMsgInfoAsync(requestDTO))
                .mapNotNull(response -> {
                    if (response == null) {
                        log.error("获取错误提示信息失败: response is null");
                        return null;
                    }
                    return response.getBody();
                })
                .doOnError(e -> log.error("获取错误提示信息失败", e))
                .onErrorResume(e -> Mono.empty());
    }

    @Override
    public Mono<QueryUserPermissionRsp> getUserPermissionsByUerId(Integer userId) {
        log.info("Dubbo调用获取用户权限标识 userId:{}", userId);

        var requestDTO = new RequestDTO<QueryUserPermissionReq>();
        var param = new QueryUserPermissionReq();
        param.setUserId(userId);
        requestDTO.setBody(param);

        return Mono.fromFuture(baseDubboService.getUserPermissionsByUerIdAsync(requestDTO))
                .mapNotNull(response -> {
                    if (response == null) {
                        log.error("获取用户权限标识失败: response is null");
                        return null;
                    }
                    return response.getBody();
                })
                .doOnError(e -> log.error("获取用户权限标识失败", e))
                .onErrorResume(e -> Mono.empty());
    }

    @Override
    public Mono<QueryUserPermissionRsp> getUserPermissionsByPath(String requestPath) {
        log.info("Dubbo调用获取请求路径权限标识 requestPath:{}", requestPath);

        var requestDTO = new RequestDTO<QueryUserPermissionReq>();
        var param = new QueryUserPermissionReq();
        param.setUrl(requestPath);
        requestDTO.setBody(param);

        return Mono.fromFuture(baseDubboService.getUserPermissionsByPathAsync(requestDTO))
                .mapNotNull(response -> {
                    if (response == null) {
                        log.error("获取请求路径权限标识失败: response is null");
                        return null;
                    }
                    return response.getBody();
                })
                .doOnError(e -> log.error("获取请求路径权限标识失败", e))
                .onErrorResume(e -> Mono.empty());
    }

    @Override
    public Mono<GetAllApiPermissionsRsp> getAllApiPermissions() {
        log.info("Dubbo调用获取所有接口权限");

        var requestDTO = new RequestDTO<GetAllApiPermissionsReq>();
        requestDTO.setBody(new GetAllApiPermissionsReq());

        return Mono.fromFuture(baseDubboService.getAllApiPermissionsAsync(requestDTO))
                .mapNotNull(response -> {
                    if (response == null) {
                        log.error("获取所有接口权限失败: response is null");
                        return null;
                    }
                    return response.getBody();
                })
                .doOnError(e -> log.error("获取所有接口权限失败", e))
                .onErrorResume(e -> Mono.empty());
    }

}