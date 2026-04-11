package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.*;
import com.blink.gateway.admin.dto.rsp.*;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.service.ChannelService;
import com.blink.gateway.dto.req.QueryOneChannelReq;
import com.blink.gateway.dto.vo.ChannelVO;
import com.blink.framework.common.exception.BlinkException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChannelController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelController 单元测试")
class ChannelControllerTest {

    @Mock
    private ChannelService channelService;

    @InjectMocks
    private ChannelController channelController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("getChannelList 测试")
    class GetChannelListTests {

        @Test
        @DisplayName("查询渠道列表 - 正常场景")
        void testGetChannelList_Success() throws BlinkException {
            QueryChannelReq req = new QueryChannelReq();
            req.setPageNum(1);
            req.setPageSize(10);

            RequestDTO<QueryChannelReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            QueryChannelRsp rsp = new QueryChannelRsp();
            when(channelService.getChannelList(any(QueryChannelReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<QueryChannelRsp> response = channelController.getChannelList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(channelService, times(1)).getChannelList(any(QueryChannelReq.class));
        }

        @Test
        @DisplayName("查询渠道列表 - 异常场景")
        void testGetChannelList_Exception() throws BlinkException {
            QueryChannelReq req = new QueryChannelReq();
            RequestDTO<QueryChannelReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(channelService.getChannelList(any(QueryChannelReq.class)))
                    .thenThrow(new BlinkException("查询失败", "GATE0001"));

            assertThrows(BlinkException.class, () -> channelController.getChannelList(requestDTO));
            verify(channelService, times(1)).getChannelList(any(QueryChannelReq.class));
        }
    }

    @Nested
    @DisplayName("getChannel 测试")
    class GetChannelTests {

        @Test
        @DisplayName("获取单个渠道信息 - 正常场景")
        void testGetChannel_Success() throws BlinkException {
            QueryOneChannelReq req = new QueryOneChannelReq();
            req.setChannelId("channel-001");

            RequestDTO<QueryOneChannelReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ChannelVO channelVO = new ChannelVO();
            channelVO.setChannelId("channel-001");
            when(channelService.getChannel(any(QueryOneChannelReq.class))).thenReturn(ResponseDTO.newSuccessInstance(channelVO));

            ResponseDTO<ChannelVO> response = channelController.getChannel(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals("channel-001", response.getBody().getChannelId());
            verify(channelService, times(1)).getChannel(any(QueryOneChannelReq.class));
        }
    }

    @Nested
    @DisplayName("getChannelSecret 测试")
    class GetChannelSecretTests {

        @Test
        @DisplayName("获取渠道密钥信息 - 正常场景")
        void testGetChannelSecret_Success() throws BlinkException {
            GetChannelSecretReq req = new GetChannelSecretReq();
            req.setChannelId("channel-001");
            req.setSecretField("appSecret");

            RequestDTO<GetChannelSecretReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ChannelSecretRsp rsp = new ChannelSecretRsp();
            when(channelService.getChannelSecret(any(GetChannelSecretReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<ChannelSecretRsp> response = channelController.getChannelSecret(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(channelService, times(1)).getChannelSecret(any(GetChannelSecretReq.class));
        }
    }

    @Nested
    @DisplayName("saveChannel 测试")
    class SaveChannelTests {

        @Test
        @DisplayName("新增渠道 - 正常场景")
        void testSaveChannel_Success() throws BlinkException {
            AddChannelReq req = new AddChannelReq();
            req.setChannelName("测试渠道");

            RequestDTO<AddChannelReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(channelService.saveChannel(any(AddChannelReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = channelController.saveChannel(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(channelService, times(1)).saveChannel(any(AddChannelReq.class));
        }

        @Test
        @DisplayName("新增渠道 - 异常场景")
        void testSaveChannel_Exception() throws BlinkException {
            AddChannelReq req = new AddChannelReq();
            RequestDTO<AddChannelReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(channelService.saveChannel(any(AddChannelReq.class)))
                    .thenThrow(new BlinkException("渠道已存在", "GATE0002"));

            assertThrows(BlinkException.class, () -> channelController.saveChannel(requestDTO));
            verify(channelService, times(1)).saveChannel(any(AddChannelReq.class));
        }
    }

    @Nested
    @DisplayName("modifyChannel 测试")
    class ModifyChannelTests {

        @Test
        @DisplayName("更新渠道 - 正常场景")
        void testModifyChannel_Success() throws BlinkException {
            UpdateChannelReq req = new UpdateChannelReq();
            req.setChannelId("channel-001");
            req.setChannelName("新渠道名");

            RequestDTO<UpdateChannelReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ChannelVO channelVO = new ChannelVO();
            channelVO.setChannelId("channel-001");
            channelVO.setChannelName("新渠道名");
            when(channelService.modifyChannel(any(UpdateChannelReq.class))).thenReturn(ResponseDTO.newSuccessInstance(channelVO));

            ResponseDTO<ChannelVO> response = channelController.modifyChannel(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals("新渠道名", response.getBody().getChannelName());
            verify(channelService, times(1)).modifyChannel(any(UpdateChannelReq.class));
        }
    }

    @Nested
    @DisplayName("deleteChannel 测试")
    class DeleteChannelTests {

        @Test
        @DisplayName("删除渠道 - 正常场景")
        void testDeleteChannel_Success() throws BlinkException {
            DeleteChannelReq req = new DeleteChannelReq();
            req.setChannelId("channel-001");

            RequestDTO<DeleteChannelReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(channelService.deleteChannel(any(DeleteChannelReq.class))).thenReturn(ResponseDTO.newSuccessInstance());

            ResponseDTO<EmptyBody> response = channelController.deleteChannel(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(channelService, times(1)).deleteChannel(any(DeleteChannelReq.class));
        }
    }

    @Nested
    @DisplayName("refreshChannelKey 测试")
    class RefreshChannelKeyTests {

        @Test
        @DisplayName("刷新渠道密钥 - 正常场景")
        void testRefreshChannelKey_Success() throws BlinkException {
            RefreshChannelKeyReq req = new RefreshChannelKeyReq();
            req.setChannelId("channel-001");

            RequestDTO<RefreshChannelKeyReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GaChannelDO channelDO = new GaChannelDO();
            channelDO.setChannelId("channel-001");
            when(channelService.refreshChannelKey(any(RefreshChannelKeyReq.class))).thenReturn(ResponseDTO.newSuccessInstance(channelDO));

            ResponseDTO<GaChannelDO> response = channelController.refreshChannelKey(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            verify(channelService, times(1)).refreshChannelKey(any(RefreshChannelKeyReq.class));
        }
    }

    @Nested
    @DisplayName("refreshSystemKey 测试")
    class RefreshSystemKeyTests {

        @Test
        @DisplayName("刷新系统密钥 - 正常场景")
        void testRefreshSystemKey_Success() throws BlinkException {
            RefreshChannelKeyReq req = new RefreshChannelKeyReq();
            req.setChannelId("channel-001");

            RequestDTO<RefreshChannelKeyReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            GaChannelDO channelDO = new GaChannelDO();
            channelDO.setChannelId("channel-001");
            when(channelService.refreshSystemKey(any(RefreshChannelKeyReq.class))).thenReturn(ResponseDTO.newSuccessInstance(channelDO));

            ResponseDTO<GaChannelDO> response = channelController.refreshSystemKey(requestDTO);

            assertNotNull(response);
            assertNotNull(response.getBody());
            verify(channelService, times(1)).refreshSystemKey(any(RefreshChannelKeyReq.class));
        }
    }

    @Nested
    @DisplayName("issueChannelToken 测试")
    class IssueChannelTokenTests {

        @Test
        @DisplayName("签发渠道Token - 正常场景")
        void testIssueChannelToken_Success() throws BlinkException {
            IssueChannelTokenReq req = new IssueChannelTokenReq();
            req.setAppKey("test-app-key");
            req.setAppSecret("test-app-secret");

            RequestDTO<IssueChannelTokenReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ChannelTokenRsp rsp = new ChannelTokenRsp();
            when(channelService.issueChannelToken(any(IssueChannelTokenReq.class))).thenReturn(ResponseDTO.newSuccessInstance(rsp));

            ResponseDTO<ChannelTokenRsp> response = channelController.issueChannelToken(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(channelService, times(1)).issueChannelToken(any(IssueChannelTokenReq.class));
        }
    }
}