package com.blink.base.service.flowable.impl;

import cn.hutool.core.collection.CollUtil;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.dto.rsp.ProcessHistoryRsp;
import com.blink.base.service.flowable.FlowableQueryHelper;
import com.blink.base.service.flowable.ProcessHistoryService;
import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程历史服务实现类
 * <p>
 * 提供流程历史的查询功能
 * </p>
 *
 * @author binblink
 */
@Service
@Slf4j
public class ProcessHistoryServiceImpl implements ProcessHistoryService {

    private final HistoryService historyService;
    private final FlowableQueryHelper flowableQueryHelper;

    public ProcessHistoryServiceImpl(HistoryService historyService,
                                      FlowableQueryHelper flowableQueryHelper) {
        this.historyService = historyService;
        this.flowableQueryHelper = flowableQueryHelper;
    }

    @Override
    public List<ProcessHistoryRsp> getProcessHistory(String processInstanceId) throws BlinkException {
        try {
            log.debug("[Workflow] 查询流程实例历史 | processInstanceId: {}", processInstanceId);

            // 校验流程实例是否存在
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (historicProcessInstance == null) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_INSTANCE_NOT_FOUND);
            }

            // 查询流程历史活动节点（包含任务、网关等）
            List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceStartTime()
                    .asc()
                    .list();

            if (CollUtil.isEmpty(activities)) {
                return new ArrayList<>();
            }

            List<ProcessHistoryRsp> result = new ArrayList<>();
            for (HistoricActivityInstance activity : activities) {
                // 跳过部分类型的事件
                String activityType = activity.getActivityType();
                if (shouldSkipActivityType(activityType)) {
                    continue;
                }

                ProcessHistoryRsp rsp = convertToProcessHistoryRsp(activity);
                result.add(rsp);
            }

            log.debug("[Workflow] 查询流程历史成功 | processInstanceId: {}, 历史节点数: {}",
                    processInstanceId, result.size());

            return result;

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Workflow] 查询流程历史失败 | processInstanceId: {}, error: {}",
                    processInstanceId, e.getMessage(), e);
            throw new BlinkException("查询流程历史失败: " + e.getMessage(), e,
                    BaseErrCodeConstant.QUERY_HISTORY_ERROR);
        }
    }

    /**
     * 判断是否跳过该活动类型
     *
     * @param activityType 活动类型
     * @return 是否跳过
     */
    private boolean shouldSkipActivityType(String activityType) {
        // 跳过开始事件、结束事件、序列流等不需要展示的类型
        return "startEvent".equals(activityType)
                || "endEvent".equals(activityType)
                || "sequenceFlow".equals(activityType)
                || "boundaryEvent".equals(activityType);
    }

    /**
     * 转换 HistoricActivityInstance 为 ProcessHistoryRsp
     *
     * @param activity 历史活动实例
     * @return 流程历史响应DTO
     */
    private ProcessHistoryRsp convertToProcessHistoryRsp(HistoricActivityInstance activity) {
        String activityType = activity.getActivityType();

        // 判断状态
        String status = activity.getEndTime() != null ? "completed" : "pending";

        // 获取审批意见（如果是任务类型）
        String comment = null;
        if ("userTask".equals(activityType) || "task".equals(activityType)) {
            comment = flowableQueryHelper.getTaskComment(activity.getTaskId());
        }

        return ProcessHistoryRsp.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .activityType(activityType)
                .taskId(activity.getTaskId())
                .assignee(activity.getAssignee())
                .startTime(activity.getStartTime() != null ?
                        LocalDateTime.ofInstant(activity.getStartTime().toInstant(), ZoneId.systemDefault()) : null)
                .endTime(activity.getEndTime() != null ?
                        LocalDateTime.ofInstant(activity.getEndTime().toInstant(), ZoneId.systemDefault()) : null)
                .durationInMillis(activity.getDurationInMillis())
                .comment(comment)
                .status(status)
                .build();
    }
}
