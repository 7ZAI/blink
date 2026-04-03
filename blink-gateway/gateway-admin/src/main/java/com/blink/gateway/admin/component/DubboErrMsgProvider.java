package com.blink.gateway.admin.component;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.base.dto.req.QueryErrMsgReq;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.dubbo.service.BaseDubboService;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.core.exception.DefaultErrMsgProvider;
import com.blink.framework.core.exception.ErrMsgProvider;
import com.blink.framework.redis.component.CacheComponent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Dubbo 错误信息提供者
 * 通过 Dubbo 调用远程服务获取错误信息
 *
 * @author binblink
 */
@Component
@Slf4j
public class DubboErrMsgProvider implements ErrMsgProvider {

    /**
     * 缓存 key 前缀
     */
    private static final String CACHE_KEY_PREFIX = "system:err:msg:";

    private final DefaultErrMsgProvider defaultErrMsgProvider = new DefaultErrMsgProvider();

    private final BaseDubboService baseDubboService;

    public DubboErrMsgProvider(BaseDubboService baseDubboService) {
        this.baseDubboService = baseDubboService;
    }

    @Resource
    private CacheComponent cacheComponent;

    @Override
    public String getErrMsg(String msgCode, String lang) {
        if (StrUtil.isBlank(msgCode)) {
            return defaultErrMsgProvider.getErrMsg(msgCode, lang);
        }

        String cacheKey = CACHE_KEY_PREFIX + lang + ":" + msgCode;

        try {
            Supplier<String> dbQuery = () -> rpcCall(msgCode, lang);
            Object result = cacheComponent.getFromCacheOrDB(cacheKey, dbQuery);

            if (ObjectUtil.isNotNull(result) && StrUtil.isNotBlank(result.toString())) {
                return result.toString();
            }
        } catch (Exception e) {
            log.error("[DubboErrMsg] 获取错误信息失败 | msgCode: {}, lang: {}", msgCode, lang, e);
            return defaultErrMsgProvider.getErrMsg(msgCode, lang);
        }
        // 查询失败或不存在时返回系统错误消息
        return defaultErrMsgProvider.getErrMsg(msgCode, lang);
    }


    /**
     * 从数据库查询错误信息
     *
     * @param msgCode 错误码
     * @param lang    语言
     * @return 错误信息，不存在时返回 null
     */
    private String rpcCall(String msgCode, String lang) {
        RequestDTO<QueryErrMsgReq> reqDto = new RequestDTO<>();
        QueryErrMsgReq req = new QueryErrMsgReq();
        req.setCode(msgCode);
        req.setLocal(lang);
        reqDto.setBody(req);

        ResponseDTO<QueryErrMsgRsp> rspDto = baseDubboService.getErrorMsgInfo(reqDto);

        if (ObjectUtil.isNull(rspDto)) {
            log.error("[DubboErrMsg] Dubbo调用获取错误码信息失败 | msgCode: {}, lang: {}", msgCode, lang);
            return null;
        }
        QueryErrMsgRsp rsp = rspDto.getBody();
        return ObjectUtil.isNull(rsp) ? null : rsp.getMsgInfo();
    }
}