package com.blink.gateway.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.dto.req.*;
import com.blink.gateway.admin.dto.rsp.*;
import com.blink.gateway.admin.dto.vo.AlertConditionVO;
import com.blink.gateway.admin.entity.GatewayAlertHistoryDO;
import com.blink.gateway.admin.entity.GatewayAlertRuleDO;
import com.blink.gateway.admin.mapper.GatewayAlertHistoryMapper;
import com.blink.gateway.admin.mapper.GatewayAlertRuleMapper;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.*;

/**
 * 告警管理控制器
 *
 * 提供告警规则配置和告警历史查询 API
 *
 * @author binblink
 * @since 2026-04-15
 */
@RestController
@RequestMapping("/alert")
@Slf4j
public class AlertController {

    @Resource
    private GatewayAlertRuleMapper ruleMapper;

    @Resource
    private GatewayAlertHistoryMapper historyMapper;

    /**
     * 查询告警规则列表
     *
     * @param reqDto 请求参数
     * @return 规则列表
     */
    @PostMapping("/getRules")
    public ResponseDTO<AlertRuleListRsp> getRules(@RequestBody @Valid RequestDTO<QueryAlertRuleReq> reqDto) {
        try {
            QueryAlertRuleReq req = reqDto.getBody();

            LambdaQueryWrapper<GatewayAlertRuleDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(GatewayAlertRuleDO::getCreateTime);

            // 规则类型筛选
            if (StrUtil.isNotBlank(req.getRuleType())) {
                wrapper.eq(GatewayAlertRuleDO::getRuleType, req.getRuleType());
            }

            // 启用状态筛选
            if (req.getEnabled() != null) {
                wrapper.eq(GatewayAlertRuleDO::getEnabled, req.getEnabled());
            }

            // 分页查询
            Page<GatewayAlertRuleDO> page = new Page<>(req.getPageNum(), req.getPageSize());
            Page<GatewayAlertRuleDO> result = ruleMapper.selectPage(page, wrapper);

            // 转换响应
            List<AlertRuleRsp> rules = result.getRecords().stream()
                    .map(this::convertToRuleRsp)
                    .collect(Collectors.toList());

            AlertRuleListRsp rsp = new AlertRuleListRsp();
            rsp.setTotal((int) result.getTotal());
            rsp.setRules(rules);

            log.info("[Alert] 查询告警规则列表成功 | total: {}", result.getTotal());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Alert] 查询告警规则列表失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("查询告警规则列表失败", e, GET_ALERT_RULE_LIST_FAILED);
        }
    }

    /**
     * 新增告警规则
     *
     * @param reqDto 请求参数
     * @return 成功响应
     */
    @PostMapping("/addRule")
    public ResponseDTO<EmptyBody> addRule(@RequestBody @Valid RequestDTO<AddAlertRuleReq> reqDto) {
        try {
            AddAlertRuleReq req = reqDto.getBody();

            // 参数校验
            validateRuleRequest(req);

            // 转换为 Entity
            GatewayAlertRuleDO rule = convertToRuleEntity(req);
            rule.setEnabled((byte) 1);

            ruleMapper.insert(rule);

            log.info("[Alert] 新增告警规则成功 | id: {}, ruleName: {}", rule.getId(), rule.getRuleName());

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Alert] 新增告警规则失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("新增告警规则失败", e, ADD_ALERT_RULE_FAILED);
        }
    }

    /**
     * 更新告警规则
     *
     * @param reqDto 请求参数
     * @return 成功响应
     */
    @PostMapping("/updateRule")
    public ResponseDTO<EmptyBody> updateRule(@RequestBody @Valid RequestDTO<AddAlertRuleReq> reqDto) {
        try {
            AddAlertRuleReq req = reqDto.getBody();

            if (req.getId() == null) {
                BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
            }

            // 查询原规则
            GatewayAlertRuleDO existing = ruleMapper.selectById(req.getId());
            if (existing == null) {
                BlinkException.throwBusinessException(ALERT_RULE_NOT_EXIST);
            }

            // 参数校验
            validateRuleRequest(req);

            // 更新规则
            GatewayAlertRuleDO rule = convertToRuleEntity(req);
            rule.setId(req.getId());

            ruleMapper.updateById(rule);

            log.info("[Alert] 更新告警规则成功 | id: {}, ruleName: {}", rule.getId(), rule.getRuleName());

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Alert] 更新告警规则失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("更新告警规则失败", e, UPDATE_ALERT_RULE_FAILED);
        }
    }

    /**
     * 删除告警规则
     *
     * @param reqDto 请求参数
     * @return 成功响应
     */
    @PostMapping("/deleteRule")
    public ResponseDTO<EmptyBody> deleteRule(@RequestBody @Valid RequestDTO<AlertRuleIdReq> reqDto) {
        try {
            AlertRuleIdReq req = reqDto.getBody();

            if (req.getId() == null) {
                BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
            }

            ruleMapper.deleteById(req.getId());

            log.info("[Alert] 删除告警规则成功 | id: {}", req.getId());

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Alert] 删除告警规则失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("删除告警规则失败", e, DELETE_ALERT_RULE_FAILED);
        }
    }

    /**
     * 切换告警规则启用状态
     *
     * @param reqDto 请求参数
     * @return 成功响应
     */
    @PostMapping("/toggleRule")
    public ResponseDTO<EmptyBody> toggleRule(@RequestBody @Valid RequestDTO<ToggleAlertRuleReq> reqDto) {
        try {
            ToggleAlertRuleReq req = reqDto.getBody();

            if (req.getId() == null) {
                BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
            }

            GatewayAlertRuleDO rule = ruleMapper.selectById(req.getId());
            if (rule == null) {
                BlinkException.throwBusinessException(ALERT_RULE_NOT_EXIST);
            }

            rule.setEnabled(req.getEnabled());
            ruleMapper.updateById(rule);

            log.info("[Alert] 切换告警规则状态成功 | id: {}, enabled: {}", req.getId(), req.getEnabled());

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Alert] 切换告警规则状态失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("切换告警规则状态失败", e, TOGGLE_ALERT_RULE_FAILED);
        }
    }

    /**
     * 查询告警历史
     *
     * @param reqDto 请求参数
     * @return 告警历史列表
     */
    @PostMapping("/getHistory")
    public ResponseDTO<AlertHistoryListRsp> getHistory(@RequestBody @Valid RequestDTO<QueryAlertHistoryReq> reqDto) {
        try {
            QueryAlertHistoryReq req = reqDto.getBody();

            LambdaQueryWrapper<GatewayAlertHistoryDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(GatewayAlertHistoryDO::getFiredTime);

            // 状态筛选
            if (StrUtil.isNotBlank(req.getStatus())) {
                wrapper.eq(GatewayAlertHistoryDO::getStatus, req.getStatus());
            }

            // 严重程度筛选
            if (StrUtil.isNotBlank(req.getSeverity())) {
                wrapper.eq(GatewayAlertHistoryDO::getSeverity, req.getSeverity());
            }

            // 规则 ID 筛选
            if (req.getRuleId() != null) {
                wrapper.eq(GatewayAlertHistoryDO::getRuleId, req.getRuleId());
            }

            // 分页查询
            Page<GatewayAlertHistoryDO> page = new Page<>(req.getPageNum(), req.getPageSize());
            Page<GatewayAlertHistoryDO> result = historyMapper.selectPage(page, wrapper);

            // 转换响应
            List<AlertHistoryRsp> historyList = result.getRecords().stream()
                    .map(this::convertToHistoryRsp)
                    .collect(Collectors.toList());

            AlertHistoryListRsp rsp = new AlertHistoryListRsp();
            rsp.setTotal((int) result.getTotal());
            rsp.setRows(historyList);

            log.info("[Alert] 查询告警历史成功 | total: {}", result.getTotal());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Alert] 查询告警历史失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("查询告警历史失败", e, GET_ALERT_HISTORY_FAILED);
        }
    }

    /**
     * 获取当前触发中的告警
     *
     * @param reqDto 请求参数
     * @return 触发中的告警列表
     */
    @PostMapping("/getFiring")
    public ResponseDTO<List<AlertHistoryRsp>> getFiring(@RequestBody @Valid RequestDTO<Void> reqDto) {
        try {
            List<GatewayAlertHistoryDO> firingAlerts = historyMapper.selectFiringAlerts();

            List<AlertHistoryRsp> rsp = firingAlerts.stream()
                    .map(this::convertToHistoryRsp)
                    .collect(Collectors.toList());

            log.info("[Alert] 获取触发中告警成功 | count: {}", rsp.size());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Alert] 获取触发中告警失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取触发中告警失败", e, GET_FIRING_ALERTS_FAILED);
        }
    }

    /**
     * 确认告警
     *
     * @param reqDto 请求参数
     * @return 成功响应
     */
    @PostMapping("/acknowledge")
    public ResponseDTO<EmptyBody> acknowledge(@RequestBody @Valid RequestDTO<AcknowledgeAlertReq> reqDto) {
        try {
            AcknowledgeAlertReq req = reqDto.getBody();

            if (req.getId() == null) {
                BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
            }

            // 获取当前用户 ID (从 RequestDTO 获取)
            String userIdStr = reqDto.getUserId();
            Integer userId = StrUtil.isNotBlank(userIdStr) ? Integer.parseInt(userIdStr) : 0;

            int updated = historyMapper.acknowledge(req.getId(), userId);

            if (updated == 0) {
                BlinkException.throwBusinessException(ALERT_HISTORY_NOT_EXIST);
            }

            log.info("[Alert] 确认告警成功 | id: {}, userId: {}", req.getId(), userId);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Alert] 确认告警失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("确认告警失败", e, ACKNOWLEDGE_ALERT_FAILED);
        }
    }

    /**
     * 参数校验
     */
    private void validateRuleRequest(AddAlertRuleReq req) {
        if (StrUtil.isBlank(req.getRuleName())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        if (StrUtil.isBlank(req.getRuleType())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        if (CollUtil.isEmpty(req.getConditions())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        if (StrUtil.isBlank(req.getSeverity())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
    }

    /**
     * 转换请求为 Entity
     */
    private GatewayAlertRuleDO convertToRuleEntity(AddAlertRuleReq req) {
        GatewayAlertRuleDO entity = new GatewayAlertRuleDO();
        entity.setRuleName(req.getRuleName());
        entity.setRuleType(req.getRuleType());
        entity.setSeverity(req.getSeverity());
        entity.setNotifyTemplate(req.getNotifyTemplate());
        entity.setSuppressMinutes(req.getSuppressMinutes() != null ? req.getSuppressMinutes() : 5);

        // 条件转为 JSON
        if (CollUtil.isNotEmpty(req.getConditions())) {
            List<AlertConditionVO> conditions = req.getConditions().stream()
                    .map(c -> BeanUtil.copyProperties(c, AlertConditionVO.class))
                    .collect(Collectors.toList());
            entity.setConditions(JacksonUtil.toJson(conditions));
        }

        // 通知渠道转为逗号分隔字符串
        if (CollUtil.isNotEmpty(req.getNotifyChannels())) {
            entity.setNotifyChannels(req.getNotifyChannels().stream()
                    .collect(Collectors.joining(",")));
        } else {
            entity.setNotifyChannels("IN_APP");
        }

        return entity;
    }

    /**
     * 转换 Entity 为响应
     */
    private AlertRuleRsp convertToRuleRsp(GatewayAlertRuleDO entity) {
        AlertRuleRsp rsp = new AlertRuleRsp();
        // 手动复制字段,避免 BeanUtil 尝试转换 conditions 字段导致错误
        rsp.setId(entity.getId());
        rsp.setRuleName(entity.getRuleName());
        rsp.setRuleType(entity.getRuleType());
        rsp.setSeverity(entity.getSeverity());
        rsp.setNotifyTemplate(entity.getNotifyTemplate());
        rsp.setSuppressMinutes(entity.getSuppressMinutes());
        rsp.setEnabled(entity.getEnabled());
        rsp.setCreateTime(entity.getCreateTime());
        rsp.setUpdateTime(entity.getUpdateTime());

        // 条件解析
        if (StrUtil.isNotBlank(entity.getConditions())) {
            List<AlertConditionVO> conditions = JacksonUtil.fromJsonToList(entity.getConditions(), AlertConditionVO.class);
            rsp.setConditions(conditions.stream()
                    .map(c -> BeanUtil.copyProperties(c, AlertRuleRsp.AlertConditionRsp.class))
                    .collect(Collectors.toList()));
        }

        // 通知渠道解析
        if (StrUtil.isNotBlank(entity.getNotifyChannels())) {
            rsp.setNotifyChannels(List.of(entity.getNotifyChannels().split(",")));
        }

        return rsp;
    }

    /**
     * 转换历史 Entity 为响应
     */
    private AlertHistoryRsp convertToHistoryRsp(GatewayAlertHistoryDO entity) {
        return BeanUtil.copyProperties(entity, AlertHistoryRsp.class);
    }
}