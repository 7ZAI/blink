package com.blink.base.controller;

import com.blink.base.dto.rsp.DashboardRsp;
import com.blink.base.service.DashboardService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard 前端控制器
 *
 * @author binblink
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    /**
     * 获取 Dashboard 统计数据
     *
     * @param reqDto 请求参数
     * @return Dashboard 统计数据
     * @throws BlinkException 异常
     */
    @PostMapping("/getData")
    public ResponseDTO<DashboardRsp> getDashboardData(@RequestBody RequestDTO<EmptyBody> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(dashboardService.getDashboardData());
    }
}