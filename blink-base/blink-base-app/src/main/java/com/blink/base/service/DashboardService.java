package com.blink.base.service;

import com.blink.base.dto.rsp.DashboardRsp;
import com.blink.framework.common.exception.BlinkException;

/**
 * Dashboard 服务接口
 *
 * @author binblink
 */
public interface DashboardService {

    /**
     * 获取 Dashboard 统计数据
     *
     * @return Dashboard 统计数据
     * @throws BlinkException 异常
     */
    DashboardRsp getDashboardData() throws BlinkException;
}