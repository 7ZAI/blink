package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerDetailReq;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerHistoryReq;
import com.blink.gateway.admin.dto.req.GetCircuitBreakerOverviewReq;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerDetailRsp;
import com.blink.gateway.admin.dto.rsp.CircuitBreakerOverviewRsp;
import com.blink.gateway.admin.dto.rsp.InstanceSummaryRsp;
import com.blink.gateway.admin.dto.rsp.StateTransitionHistoryRsp;
import com.blink.gateway.admin.service.CircuitBreakerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 熔断器监控控制器
 *
 * 提供熔断器配置和状态监控，从 Redis 读取真实数据
 *
 * @author binblink
 * @since 2026-04-15
 */
@RestController
@RequestMapping("/circuitBreaker")
@Slf4j
public class CircuitBreakerController {

    @Resource
    private CircuitBreakerService circuitBreakerService;

    /**
     * 获取实例列表及熔断器汇总
     *
     * @param reqDto 请求参数
     * @return 实例列表
     */
    @PostMapping("/getInstanceList")
    public ResponseDTO<List<InstanceSummaryRsp>> getInstanceList(@RequestBody RequestDTO<Void> reqDto) {
        try {
            List<InstanceSummaryRsp> instances = circuitBreakerService.getInstanceList();

            log.info("[CircuitBreaker] 获取实例列表成功 | count: {}", instances.size());

            return ResponseDTO.newSuccessInstance(instances);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取实例列表失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(List.of());
        }
    }

    /**
     * 获取熔断器监控总览
     *
     * @param reqDto 请求参数（可选 instanceId）
     * @return 熔断器总览
     */
    @PostMapping("/getOverview")
    public ResponseDTO<CircuitBreakerOverviewRsp> getOverview(
            @RequestBody RequestDTO<GetCircuitBreakerOverviewReq> reqDto) {
        try {
            GetCircuitBreakerOverviewReq req = reqDto.getBody();
            if (req == null) {
                req = new GetCircuitBreakerOverviewReq();
            }

            CircuitBreakerOverviewRsp overview = circuitBreakerService.getOverview(req);

            log.info("[CircuitBreaker] 获取熔断器总览成功 | total: {}, instances: {}",
                    overview.getTotalCircuitBreakers(), overview.getTotalInstances());

            return ResponseDTO.newSuccessInstance(overview);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取熔断器总览失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(new CircuitBreakerOverviewRsp());
        }
    }

    /**
     * 获取熔断器详情
     *
     * @param reqDto 请求参数（name 必填，instanceId 可选）
     * @return 熔断器详情
     */
    @PostMapping("/getDetail")
    public ResponseDTO<CircuitBreakerDetailRsp> getDetail(
            @RequestBody RequestDTO<GetCircuitBreakerDetailReq> reqDto) {
        try {
            GetCircuitBreakerDetailReq req = reqDto.getBody();
            if (req == null || req.getName() == null) {
                log.warn("[CircuitBreaker] 获取熔断器详情失败：缺少 name 参数");
                return ResponseDTO.newSuccessInstance(new CircuitBreakerDetailRsp());
            }

            CircuitBreakerDetailRsp detail = circuitBreakerService.getDetail(req);

            log.info("[CircuitBreaker] 获取熔断器详情成功 | name: {}, instanceCount: {}",
                    req.getName(), detail.getInstances() != null ? detail.getInstances().size() : 0);

            return ResponseDTO.newSuccessInstance(detail);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取熔断器详情失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(new CircuitBreakerDetailRsp());
        }
    }

    /**
     * 获取状态转换历史
     *
     * @param reqDto 请求参数（instanceId 和 name 必填）
     * @return 状态转换历史列表
     */
    @PostMapping("/getHistory")
    public ResponseDTO<List<StateTransitionHistoryRsp>> getHistory(
            @RequestBody RequestDTO<GetCircuitBreakerHistoryReq> reqDto) {
        try {
            GetCircuitBreakerHistoryReq req = reqDto.getBody();
            if (req == null || req.getInstanceId() == null || req.getName() == null) {
                log.warn("[CircuitBreaker] 获取状态转换历史失败：缺少必要参数");
                return ResponseDTO.newSuccessInstance(List.of());
            }

            List<StateTransitionHistoryRsp> history = circuitBreakerService.getHistory(req);

            log.info("[CircuitBreaker] 获取状态转换历史成功 | instanceId: {}, name: {}, count: {}",
                    req.getInstanceId(), req.getName(), history.size());

            return ResponseDTO.newSuccessInstance(history);
        } catch (Exception e) {
            log.error("[CircuitBreaker] 获取状态转换历史失败 | error: {}", e.getMessage(), e);
            return ResponseDTO.newSuccessInstance(List.of());
        }
    }
}
