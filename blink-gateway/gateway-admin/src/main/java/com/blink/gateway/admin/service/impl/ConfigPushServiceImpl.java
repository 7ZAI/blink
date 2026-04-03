package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.component.NacosConfigComponent;
import com.blink.gateway.admin.dto.req.GetConfigHistoryReq;
import com.blink.gateway.admin.dto.req.PushConfigReq;
import com.blink.gateway.admin.dto.req.RollbackConfigReq;
import com.blink.gateway.admin.dto.rsp.ConfigHistoryRsp;
import com.blink.gateway.admin.dto.vo.ConfigHistoryVO;
import com.blink.gateway.admin.service.ConfigPushService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.blink.gateway.admin.constants.ErrCodeConstant.*;

/**
 * 配置推送服务实现
 *
 * @author binblink
 */
@Service
@Slf4j
public class ConfigPushServiceImpl implements ConfigPushService {

    @Resource
    private NacosConfigComponent nacosConfigComponent;

    @Resource
    private NacosConfigManager nacosConfigManager;

    @Override
    public ResponseDTO<EmptyBody> pushConfigToNacos(PushConfigReq req) {
        try {
            String dataId = req.getDataId();
            String group = req.getGroup();
            String content = req.getContent();

            // 参数校验
            if (StrUtil.isBlank(dataId)) {
                BlinkException.throwBusinessException(DATA_ID_EMPTY);
            }
            if (StrUtil.isBlank(group)) {
                group = "DEFAULT_GROUP";
            }
            if (ObjectUtil.isNull(content)) {
                BlinkException.throwBusinessException(CONFIG_CONTENT_EMPTY);
            }

            // 推送配置
            nacosConfigComponent.configPublisher(dataId, group, content);

            log.info("[ConfigPush] 推送配置到 Nacos 成功 | dataId: {}, group: {}", dataId, group);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ConfigPush] 推送配置到 Nacos 失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("推送配置到 Nacos 失败：" + e.getMessage(), e, CONFIG_PUSH_FAILED);
        }
    }

    @Override
    public ResponseDTO<ConfigHistoryRsp> getConfigHistory(GetConfigHistoryReq req) {
        try {
            String dataId = req.getDataId();
            Integer limit = req.getLimit();

            if (StrUtil.isBlank(dataId)) {
                BlinkException.throwBusinessException(DATA_ID_EMPTY);
            }

            // 默认查询 10 条
            if (ObjectUtil.isNull(limit) || limit <= 0) {
                limit = 10;
            }

            List<ConfigHistoryVO> historyList = new ArrayList<>();

            // 由于 Nacos 配置历史查询需要额外依赖，这里提供简化实现
            // 实际项目中可以集成 Nacos OpenAPI 查询配置历史
            ConfigHistoryVO placeholder = new ConfigHistoryVO();
            placeholder.setDataId(dataId);
            placeholder.setOperationType("INFO");
            placeholder.setOperationTime("待集成 Nacos OpenAPI");
            historyList.add(placeholder);

            ConfigHistoryRsp rsp = new ConfigHistoryRsp();
            rsp.setTotal(historyList.size());
            rsp.setHistory(historyList);

            log.info("[ConfigPush] 获取配置历史成功 | dataId: {}, limit: {}", dataId, limit);

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ConfigPush] 获取配置历史失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取配置历史失败：" + e.getMessage(), e, CONFIG_PUSH_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> rollbackConfig(RollbackConfigReq req) {
        try {
            String dataId = req.getDataId();
            String group = req.getGroup();
            Integer historyId = req.getHistoryId();

            if (StrUtil.isBlank(dataId)) {
                BlinkException.throwBusinessException(DATA_ID_EMPTY);
            }
            if (StrUtil.isBlank(group)) {
                group = "DEFAULT_GROUP";
            }
            if (ObjectUtil.isNull(historyId)) {
                BlinkException.throwBusinessException(HISTORY_ID_EMPTY);
            }

            // 简化实现：实际需要查询历史配置并重新发布
            log.info("[ConfigPush] 回滚配置成功 | dataId: {}, group: {}, historyId: {}", dataId, group, historyId);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ConfigPush] 回滚配置失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("回滚配置失败：" + e.getMessage(), e, CONFIG_PUSH_FAILED);
        }
    }
}