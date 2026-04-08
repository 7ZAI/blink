package com.blink.base.controller;

import com.blink.base.dto.rsp.DashboardRsp;
import com.blink.base.service.DashboardService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DashboardController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController 单元测试")
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getDashboardData 测试")
    class GetDashboardDataTests {

        @Test
        @DisplayName("获取Dashboard统计数据 - 正常场景")
        void testGetDashboardData_Success() throws Exception {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            DashboardRsp rsp = new DashboardRsp();
            when(dashboardService.getDashboardData()).thenReturn(rsp);

            ResponseDTO<DashboardRsp> response = dashboardController.getDashboardData(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(dashboardService, times(1)).getDashboardData();
        }

        @Test
        @DisplayName("获取Dashboard统计数据 - 异常场景")
        void testGetDashboardData_Exception() throws Exception {
            RequestDTO<EmptyBody> requestDTO = new RequestDTO<>();

            when(dashboardService.getDashboardData())
                    .thenThrow(new RuntimeException("获取统计数据失败"));

            assertThrows(RuntimeException.class, () -> dashboardController.getDashboardData(requestDTO));
            verify(dashboardService, times(1)).getDashboardData();
        }
    }
}