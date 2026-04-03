package com.blink.gateway.dubbo;

import com.blink.base.dto.req.GetAllApiPermissionsReq;
import com.blink.base.dto.req.QueryErrMsgReq;
import com.blink.base.dto.req.QueryOneChannelReq;
import com.blink.base.dto.req.QueryUserPermissionReq;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.base.dto.req.QueryOneSysConfigReq;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.vo.ChannelVO;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.dubbo.BaseDubboServiceImpl;
// import com.blink.base.service.BlinkChannelService;  // TODO: Class not found, commented out
import com.blink.base.service.SysConfigService;
import com.blink.base.service.SysPermissionService;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.framework.common.exception.BlinkException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BaseDubboServiceImpl 单元测试类
 * <p>
 * 测试 Dubbo 基础服务实现类的各项功能
 * </p>
 *
 * @author blink
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseDubboServiceImpl 单元测试")
class BaseDubboServiceImplTest {

    @Mock
    private SysConfigService sysConfigService;

    // @Mock
    // private BlinkChannelService blinkChannelService;  // TODO: Class not found, commented out

    @Mock
    private SysPermissionService sysPermissionService;

    @InjectMocks
    private BaseDubboServiceImpl baseDubboServiceImpl;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Test
    @DisplayName("测试获取系统配置 - 正常场景")
    void testGetOneConfig_Success() {
        // 准备测试数据
        QueryOneSysConfigReq req = new QueryOneSysConfigReq();
        req.setConfigKey("site_name");

        RequestDTO<QueryOneSysConfigReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        SysConfigVO configVO = new SysConfigVO();
        configVO.setConfigKey("site_name");
        configVO.setConfigValue("test.value");
        configVO.setConfigName("测试配置");

        // 模拟服务调用
        when(sysConfigService.getOneConfigFromCacheOrDataBase(any())).thenReturn(configVO);

        // 执行测试
        ResponseDTO<SysConfigCacheDO> response = baseDubboServiceImpl.getOneConfig(requestDTO);

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals("test.key", response.getBody().getConfigKey());
        assertEquals("test.value", response.getBody().getConfigValue());
        assertEquals("测试配置", response.getBody().getConfigName());

        // 验证服务调用次数
        verify(sysConfigService, times(1)).getOneConfigFromCacheOrDataBase(any());
    }

    @Test
    @DisplayName("测试获取系统配置 - 空值场景")
    void testGetOneConfig_NullValue() {
        // 准备测试数据
        QueryOneSysConfigReq req = new QueryOneSysConfigReq();
        req.setConfigKey("not.exist.key");

        RequestDTO<QueryOneSysConfigReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        // 模拟返回 null
        when(sysConfigService.getOneConfigFromCacheOrDataBase(any())).thenReturn(null);

        // 执行测试
        ResponseDTO<SysConfigCacheDO> response = baseDubboServiceImpl.getOneConfig(requestDTO);

        // 验证结果
        assertNotNull(response);
        assertNull(response.getBody());

        verify(sysConfigService, times(1)).getOneConfigFromCacheOrDataBase(any());
    }

    @Test
    @DisplayName("测试获取系统配置 - 异常场景")
    void testGetOneConfig_Exception() {
        // 准备测试数据
        QueryOneSysConfigReq req = new QueryOneSysConfigReq();
        req.setConfigKey("test.key");

        RequestDTO<QueryOneSysConfigReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        // 模拟异常
        when(sysConfigService.getOneConfigFromCacheOrDataBase(any()))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // 执行测试并验证异常
        BlinkException exception = assertThrows(BlinkException.class, () -> {
            baseDubboServiceImpl.getOneConfig(requestDTO);
        });

        assertEquals("数据库连接失败", exception.getMessage());
        verify(sysConfigService, times(1)).getOneConfigFromCacheOrDataBase(any());
    }

    // TODO: BlinkChannelService class not found, tests commented out
    // @Test
    // @DisplayName("测试获取渠道信息 - 正常场景")
    // void testGetChannelInfo_Success() {
    //     ...
    // }

    // @Test
    // @DisplayName("测试获取渠道信息 - 空值场景")
    // void testGetChannelInfo_NullValue() {
    //     ...
    // }

    // @Test
    // @DisplayName("测试获取渠道信息 - 异常场景")
    // void testGetChannelInfo_Exception() {
    //     ...
    // }

    @Test
    @DisplayName("测试获取错误消息 - 正常场景")
    void testGetErrorMsgInfo() {
        // 准备测试数据
        QueryErrMsgReq req = new QueryErrMsgReq();
        req.setCode("TEST001");
        req.setLocal("zh_CN");

        RequestDTO<QueryErrMsgReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        // 执行测试
        ResponseDTO<QueryErrMsgRsp> response = baseDubboServiceImpl.getErrorMsgInfo(requestDTO);

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("测试根据用户ID获取权限 - 正常场景")
    void testGetUserPermissionsByUserId_Success() {
        // 准备测试数据
        QueryUserPermissionReq req = new QueryUserPermissionReq();
        req.setUserId(1);

        RequestDTO<QueryUserPermissionReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        QueryUserPermissionRsp rsp = new QueryUserPermissionRsp();
        // 设置权限数据

        // 模拟服务调用
        when(sysPermissionService.getPermissions(any())).thenReturn(rsp);

        // 执行测试
        ResponseDTO<QueryUserPermissionRsp> response = baseDubboServiceImpl.getUserPermissionsByUerId(requestDTO);

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getBody());

        verify(sysPermissionService, times(1)).getPermissions(any());
    }

    @Test
    @DisplayName("测试根据用户ID获取权限 - 异常场景")
    void testGetUserPermissionsByUserId_Exception() {
        // 准备测试数据
        QueryUserPermissionReq req = new QueryUserPermissionReq();
        req.setUserId(1);

        RequestDTO<QueryUserPermissionReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        // 模拟异常
        when(sysPermissionService.getPermissions(any()))
                .thenThrow(new RuntimeException("权限服务异常"));

        // 执行测试并验证异常
        BlinkException exception = assertThrows(BlinkException.class, () -> {
            baseDubboServiceImpl.getUserPermissionsByUerId(requestDTO);
        });

        assertEquals("权限服务异常", exception.getMessage());
        verify(sysPermissionService, times(1)).getPermissions(any());
    }

    @Test
    @DisplayName("测试根据路径获取权限 - 正常场景")
    void testGetUserPermissionsByPath_Success() {
        // 准备测试数据
        QueryUserPermissionReq req = new QueryUserPermissionReq();
        req.setUrl("/api/test");

        RequestDTO<QueryUserPermissionReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        QueryUserPermissionRsp rsp = new QueryUserPermissionRsp();

        // 模拟服务调用
        when(sysPermissionService.getPermissions(any())).thenReturn(rsp);

        // 执行测试
        ResponseDTO<QueryUserPermissionRsp> response = baseDubboServiceImpl.getUserPermissionsByPath(requestDTO);

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getBody());

        verify(sysPermissionService, times(1)).getPermissions(any());
    }

    @Test
    @DisplayName("测试获取所有接口权限 - 正常场景")
    void testGetAllApiPermissions_Success() {
        // 准备测试数据
        GetAllApiPermissionsReq req = new GetAllApiPermissionsReq();

        RequestDTO<GetAllApiPermissionsReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        GetAllApiPermissionsRsp rsp = new GetAllApiPermissionsRsp();

        // 模拟服务调用
        when(sysPermissionService.getAllApiPermission(any())).thenReturn(rsp);

        // 执行测试
        ResponseDTO<GetAllApiPermissionsRsp> response = baseDubboServiceImpl.getAllApiPermissions(requestDTO);

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getBody());

        verify(sysPermissionService, times(1)).getAllApiPermission(any());
    }

    @Test
    @DisplayName("测试获取所有接口权限 - 异常场景")
    void testGetAllApiPermissions_Exception() {
        // 准备测试数据
        GetAllApiPermissionsReq req = new GetAllApiPermissionsReq();

        RequestDTO<GetAllApiPermissionsReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        // 模拟异常
        when(sysPermissionService.getAllApiPermission(any()))
                .thenThrow(new RuntimeException("获取权限列表失败"));

        // 执行测试并验证异常
        BlinkException exception = assertThrows(BlinkException.class, () -> {
            baseDubboServiceImpl.getAllApiPermissions(requestDTO);
        });

        assertEquals("获取权限列表失败", exception.getMessage());
        verify(sysPermissionService, times(1)).getAllApiPermission(any());
    }
}