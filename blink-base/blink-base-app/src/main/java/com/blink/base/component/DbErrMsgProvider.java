package com.blink.base.component;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blink.framework.core.exception.ErrMsgProvider;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.base.entity.SysMsgInfoDO;
import com.blink.base.mapper.SysMsgInfoMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 基于数据库的错误信息提供者
 * 支持多语言，带两级缓存（本地 + Redis）
 * 当错误码查询不到时，根据错误码类型返回友好提示
 *
 * @author binblink
 */
@Slf4j
@Component
public class DbErrMsgProvider implements ErrMsgProvider {

    /**
     * 缓存 key 前缀
     */
    private static final String CACHE_KEY_PREFIX = "system:err:msg:";

    /**
     * 业务错误码前缀
     */
    private static final String BUSINESS_CODE_PREFIX = "BUSS";

    /**
     * 参数校验错误码前缀
     */
    private static final String INVALID_CODE_PREFIX = "INVALID";

    /**
     * 认证授权错误码前缀
     */
    private static final String AUTH_CODE_PREFIX = "AUTH";

    /**
     * 工作流错误码前缀
     */
    private static final String FLOW_CODE_PREFIX = "FLOW";

    /**
     * 系统错误码前缀
     */
    private static final String SYS_CODE_PREFIX = "SYS";

    /**
     * 中文默认业务错误消息模板
     */
    private static final String DEFAULT_BUSINESS_MSG_CN = "操作失败";

    /**
     * 英文默认业务错误消息模板
     */
    private static final String DEFAULT_BUSINESS_MSG_EN = "Operation failed";

    /**
     * 中文默认系统错误消息
     */
    private static final String DEFAULT_SYSTEM_MSG_CN = "系统错误，请稍后重试";

    /**
     * 英文默认系统错误消息
     */
    private static final String DEFAULT_SYSTEM_MSG_EN = "System error, please try again later";

    @Resource
    private CacheComponent cacheComponent;

    @Resource
    private SysMsgInfoMapper sysMsgInfoMapper;

    @Override
    public String getErrMsg(String msgCode, String lang) {
        if (StrUtil.isBlank(msgCode)) {
            return getDefaultSystemMsg(lang);
        }

        String cacheKey = CACHE_KEY_PREFIX + lang + ":" + msgCode;

        try {
            Supplier<String> dbQuery = () -> queryFromDatabase(msgCode, lang);
            Object result = cacheComponent.getFromCacheOrDB(cacheKey, dbQuery);

            if (result != null && !result.toString().isEmpty()) {
                return result.toString();
            }

        } catch (Exception e) {
            log.error("[DbErrMsgProvider] 获取错误信息失败 | msgCode: {}, lang: {}", msgCode, lang, e);
        }

        // 查询失败或不存在时，根据错误码类型返回友好提示
        return buildFallbackMessage(msgCode, lang);
    }

    /**
     * 从数据库查询错误信息
     *
     * @param msgCode 错误码
     * @param lang    语言
     * @return 错误信息，不存在时返回 null
     */
    private String queryFromDatabase(String msgCode, String lang) {
        SysMsgInfoDO msgInfo = sysMsgInfoMapper.selectOne(
                new QueryWrapper<SysMsgInfoDO>()
                        .lambda()
                        .eq(SysMsgInfoDO::getMsgCode, msgCode)
                        .eq(SysMsgInfoDO::getMsgLang, lang)
        );

        return Objects.isNull(msgInfo) ? null : msgInfo.getMsgInfo();
    }

    /**
     * 构建兜底错误消息
     * 当错误码查询不到时，根据错误码类型返回友好提示
     *
     * @param msgCode 错误码
     * @param lang    语言
     * @return 兜底错误消息
     */
    private String buildFallbackMessage(String msgCode, String lang) {
        boolean isChinese = "zh_cn".equalsIgnoreCase(lang);

        if (isBusinessErrorCode(msgCode)) {
            // 业务错误：显示错误码便于用户反馈
            if (isChinese) {
                return DEFAULT_BUSINESS_MSG_CN + "（错误码：" + msgCode + "）";
            } else {
                return DEFAULT_BUSINESS_MSG_EN + " (Error Code: " + msgCode + ")";
            }
        } else {
            // 系统错误：统一提示系统错误
            return getDefaultSystemMsg(lang);
        }
    }

    /**
     * 判断是否为业务错误码
     * 业务错误码包括：BUSS、INVALID、AUTH、FLOW 开头的错误码
     *
     * @param msgCode 错误码
     * @return 是否为业务错误码
     */
    private boolean isBusinessErrorCode(String msgCode) {
        if (StrUtil.isBlank(msgCode)) {
            return false;
        }
        String upperCode = msgCode.toUpperCase();
        return upperCode.startsWith(BUSINESS_CODE_PREFIX)
                || upperCode.startsWith(INVALID_CODE_PREFIX)
                || upperCode.startsWith(AUTH_CODE_PREFIX)
                || upperCode.startsWith(FLOW_CODE_PREFIX);
    }

    /**
     * 获取默认系统错误消息
     *
     * @param lang 语言
     * @return 默认系统错误消息
     */
    private String getDefaultSystemMsg(String lang) {
        if ("zh_cn".equalsIgnoreCase(lang)) {
            return DEFAULT_SYSTEM_MSG_CN;
        }
        return DEFAULT_SYSTEM_MSG_EN;
    }
}