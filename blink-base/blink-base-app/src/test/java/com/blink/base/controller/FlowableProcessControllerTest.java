package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.*;
import com.blink.base.dto.vo.ProcessInstanceVO;
import com.blink.base.service.FlowableProcessService;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * FlowableProcessController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlowableProcessController 单元测试")
class FlowableProcessControllerTest {

    @Mock
    private FlowableProcessService flowableProcessService;

    @InjectMocks
    private FlowableProcessController flowableProcessController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    // ==================== 流程定义管理测试 ====================

    @Nested
    @DisplayName("deployProcess 测试")
    class DeployProcessTests {

        @Test
        @DisplayName("部署流程定义 - 正常场景")
        void testDeployProcess_Success() throws Exception {
            DeployProcessReq req = new DeployProcessReq();

            RequestDTO<DeployProcessReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(flowableProcessService.deployProcess(any(DeployProcessReq.class))).thenReturn("deployment_123");

            ResponseDTO<String> response = flowableProcessController.deployProcess(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertEquals("deployment_123", response.getBody());
            verify(flowableProcessService, times(1)).deployProcess(any(DeployProcessReq.class));
        }
    }

    @Nested
    @DisplayName("getProcessDefinitionList 测试")
    class GetProcessDefinitionListTests {

        @Test
        @DisplayName("分页查询流程定义列表 - 正常场景")
        void testGetProcessDefinitionList_Success() throws Exception {
            QueryProcessDefinitionReq req = new QueryProcessDefinitionReq();

            RequestDTO<QueryProcessDefinitionReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ProcessDefinitionRsp rsp = new ProcessDefinitionRsp();
            when(flowableProcessService.getProcessDefinitionList(any(QueryProcessDefinitionReq.class))).thenReturn(rsp);

            ResponseDTO<ProcessDefinitionRsp> response = flowableProcessController.getProcessDefinitionList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getProcessDefinitionList(any(QueryProcessDefinitionReq.class));
        }
    }

    @Nested
    @DisplayName("getProcessDiagramXml 测试")
    class GetProcessDiagramXmlTests {

        @Test
        @DisplayName("获取流程图XML - 正常场景")
        void testGetProcessDiagramXml_Success() throws Exception {
            ProcessDefinitionIdReq req = new ProcessDefinitionIdReq();
            req.setProcessDefinitionId("process_123");

            RequestDTO<ProcessDefinitionIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            when(flowableProcessService.getProcessDiagramXml(anyString())).thenReturn("<xml>test</xml>");

            ResponseDTO<String> response = flowableProcessController.getProcessDiagramXml(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getProcessDiagramXml(anyString());
        }
    }

    @Nested
    @DisplayName("getProcessDiagramImage 测试")
    class GetProcessDiagramImageTests {

        @Test
        @DisplayName("获取流程图图片 - 正常场景")
        void testGetProcessDiagramImage_Success() throws Exception {
            ProcessInstanceIdReq req = new ProcessInstanceIdReq();
            req.setProcessInstanceId("instance_123");

            RequestDTO<ProcessInstanceIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            byte[] imageBytes = new byte[]{1, 2, 3};
            when(flowableProcessService.getProcessDiagramImage(anyString())).thenReturn(imageBytes);

            ResponseDTO<byte[]> response = flowableProcessController.getProcessDiagramImage(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getProcessDiagramImage(anyString());
        }
    }

    @Nested
    @DisplayName("suspendProcessDefinition 测试")
    class SuspendProcessDefinitionTests {

        @Test
        @DisplayName("挂起流程定义 - 正常场景")
        void testSuspendProcessDefinition_Success() throws Exception {
            ProcessDefinitionIdReq req = new ProcessDefinitionIdReq();
            req.setProcessDefinitionId("process_123");

            RequestDTO<ProcessDefinitionIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(flowableProcessService).suspendProcessDefinition(anyString());

            ResponseDTO<EmptyBody> response = flowableProcessController.suspendProcessDefinition(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(flowableProcessService, times(1)).suspendProcessDefinition(anyString());
        }
    }

    @Nested
    @DisplayName("activateProcessDefinition 测试")
    class ActivateProcessDefinitionTests {

        @Test
        @DisplayName("激活流程定义 - 正常场景")
        void testActivateProcessDefinition_Success() throws Exception {
            ProcessDefinitionIdReq req = new ProcessDefinitionIdReq();
            req.setProcessDefinitionId("process_123");

            RequestDTO<ProcessDefinitionIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(flowableProcessService).activateProcessDefinition(anyString());

            ResponseDTO<EmptyBody> response = flowableProcessController.activateProcessDefinition(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(flowableProcessService, times(1)).activateProcessDefinition(anyString());
        }
    }

    @Nested
    @DisplayName("deleteProcessDefinition 测试")
    class DeleteProcessDefinitionTests {

        @Test
        @DisplayName("删除流程定义 - 正常场景")
        void testDeleteProcessDefinition_Success() throws Exception {
            DeploymentIdReq req = new DeploymentIdReq();
            req.setDeploymentId("deployment_123");
            req.setCascade(false);

            RequestDTO<DeploymentIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(flowableProcessService).deleteProcessDefinition(anyString(), anyBoolean());

            ResponseDTO<EmptyBody> response = flowableProcessController.deleteProcessDefinition(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(flowableProcessService, times(1)).deleteProcessDefinition(anyString(), anyBoolean());
        }

        @Test
        @DisplayName("删除流程定义 - 级联删除")
        void testDeleteProcessDefinition_Cascade() throws Exception {
            DeploymentIdReq req = new DeploymentIdReq();
            req.setDeploymentId("deployment_123");
            req.setCascade(true);

            RequestDTO<DeploymentIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(flowableProcessService).deleteProcessDefinition(anyString(), anyBoolean());

            ResponseDTO<EmptyBody> response = flowableProcessController.deleteProcessDefinition(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(flowableProcessService, times(1)).deleteProcessDefinition(anyString(), anyBoolean());
        }
    }

    // ==================== 流程实例管理测试 ====================

    @Nested
    @DisplayName("startProcess 测试")
    class StartProcessTests {

        @Test
        @DisplayName("启动流程实例 - 正常场景")
        void testStartProcess_Success() throws Exception {
            StartProcessReq req = new StartProcessReq();

            RequestDTO<StartProcessReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ProcessInstanceVO vo = new ProcessInstanceVO();
            when(flowableProcessService.startProcess(any(StartProcessReq.class))).thenReturn(vo);

            ResponseDTO<ProcessInstanceVO> response = flowableProcessController.startProcess(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).startProcess(any(StartProcessReq.class));
        }
    }

    @Nested
    @DisplayName("startLeaveProcess 测试")
    class StartLeaveProcessTests {

        @Test
        @DisplayName("启动请假审批流程 - 正常场景")
        void testStartLeaveProcess_Success() throws Exception {
            LeaveApprovalReq req = new LeaveApprovalReq();

            RequestDTO<LeaveApprovalReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ProcessInstanceVO vo = new ProcessInstanceVO();
            when(flowableProcessService.startLeaveProcess(any(LeaveApprovalReq.class))).thenReturn(vo);

            ResponseDTO<ProcessInstanceVO> response = flowableProcessController.startLeaveProcess(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).startLeaveProcess(any(LeaveApprovalReq.class));
        }
    }

    @Nested
    @DisplayName("getProcessInstanceList 测试")
    class GetProcessInstanceListTests {

        @Test
        @DisplayName("分页查询流程实例列表 - 正常场景")
        void testGetProcessInstanceList_Success() throws Exception {
            QueryProcessInstanceReq req = new QueryProcessInstanceReq();

            RequestDTO<QueryProcessInstanceReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ProcessInstanceRsp rsp = new ProcessInstanceRsp();
            when(flowableProcessService.getProcessInstanceList(any(QueryProcessInstanceReq.class))).thenReturn(rsp);

            ResponseDTO<ProcessInstanceRsp> response = flowableProcessController.getProcessInstanceList(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getProcessInstanceList(any(QueryProcessInstanceReq.class));
        }
    }

    @Nested
    @DisplayName("getMyProcessInstances 测试")
    class GetMyProcessInstancesTests {

        @Test
        @DisplayName("查询用户发起的流程实例 - 正常场景")
        void testGetMyProcessInstances_Success() throws Exception {
            QueryMyProcessReq req = new QueryMyProcessReq();
            req.setUserId("1");
            req.setStatus("running");

            RequestDTO<QueryMyProcessReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            List<ProcessInstanceDetailRsp> list = new ArrayList<>();
            when(flowableProcessService.getMyProcessInstances(anyString(), anyString())).thenReturn(list);

            ResponseDTO<List<ProcessInstanceDetailRsp>> response = flowableProcessController.getMyProcessInstances(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getMyProcessInstances(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("getProcessInstanceDetail 测试")
    class GetProcessInstanceDetailTests {

        @Test
        @DisplayName("查询流程实例详情 - 正常场景")
        void testGetProcessInstanceDetail_Success() throws Exception {
            ProcessInstanceIdReq req = new ProcessInstanceIdReq();
            req.setProcessInstanceId("instance_123");

            RequestDTO<ProcessInstanceIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ProcessInstanceDetailRsp rsp = new ProcessInstanceDetailRsp();
            when(flowableProcessService.getProcessInstanceDetail(anyString())).thenReturn(rsp);

            ResponseDTO<ProcessInstanceDetailRsp> response = flowableProcessController.getProcessInstanceDetail(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getProcessInstanceDetail(anyString());
        }
    }

    @Nested
    @DisplayName("deleteProcessInstance 测试")
    class DeleteProcessInstanceTests {

        @Test
        @DisplayName("删除流程实例 - 正常场景")
        void testDeleteProcessInstance_Success() throws Exception {
            DeleteProcessInstanceReq req = new DeleteProcessInstanceReq();
            req.setProcessInstanceId("instance_123");
            req.setReason("测试删除");

            RequestDTO<DeleteProcessInstanceReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(flowableProcessService).deleteProcessInstance(anyString(), anyString());

            ResponseDTO<EmptyBody> response = flowableProcessController.deleteProcessInstance(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(flowableProcessService, times(1)).deleteProcessInstance(anyString(), anyString());
        }
    }

    // ==================== 任务管理测试 ====================

    @Nested
    @DisplayName("getUserTasks 测试")
    class GetUserTasksTests {

        @Test
        @DisplayName("查询用户待办任务 - 正常场景")
        void testGetUserTasks_Success() throws Exception {
            UserIdReq req = new UserIdReq();
            req.setUserId(1);

            RequestDTO<UserIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            TaskRsp rsp = new TaskRsp();
            when(flowableProcessService.getUserTasks(anyString())).thenReturn(rsp);

            ResponseDTO<TaskRsp> response = flowableProcessController.getUserTasks(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getUserTasks(anyString());
        }
    }

    @Nested
    @DisplayName("getPendingTasks 测试")
    class GetPendingTasksTests {

        @Test
        @DisplayName("分页查询待办任务 - 正常场景")
        void testGetPendingTasks_Success() throws Exception {
            QueryTaskReq req = new QueryTaskReq();

            RequestDTO<QueryTaskReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            TaskRsp rsp = new TaskRsp();
            when(flowableProcessService.getPendingTasks(any(QueryTaskReq.class))).thenReturn(rsp);

            ResponseDTO<TaskRsp> response = flowableProcessController.getPendingTasks(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getPendingTasks(any(QueryTaskReq.class));
        }
    }

    @Nested
    @DisplayName("getCompletedTasks 测试")
    class GetCompletedTasksTests {

        @Test
        @DisplayName("分页查询已办任务 - 正常场景")
        void testGetCompletedTasks_Success() throws Exception {
            QueryTaskReq req = new QueryTaskReq();

            RequestDTO<QueryTaskReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            HistoricTaskRsp rsp = new HistoricTaskRsp();
            when(flowableProcessService.getCompletedTasks(any(QueryTaskReq.class))).thenReturn(rsp);

            ResponseDTO<HistoricTaskRsp> response = flowableProcessController.getCompletedTasks(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getCompletedTasks(any(QueryTaskReq.class));
        }
    }

    @Nested
    @DisplayName("completeTask 测试")
    class CompleteTaskTests {

        @Test
        @DisplayName("完成任务 - 正常场景")
        void testCompleteTask_Success() throws Exception {
            CompleteTaskReq req = new CompleteTaskReq();
            req.setTaskId("task_123");

            RequestDTO<CompleteTaskReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(flowableProcessService).completeTask(any(CompleteTaskReq.class));

            ResponseDTO<EmptyBody> response = flowableProcessController.completeTask(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(flowableProcessService, times(1)).completeTask(any(CompleteTaskReq.class));
        }
    }

    @Nested
    @DisplayName("delegateTask 测试")
    class DelegateTaskTests {

        @Test
        @DisplayName("委托任务 - 正常场景")
        void testDelegateTask_Success() throws Exception {
            DelegateTaskReq req = new DelegateTaskReq();
            req.setTaskId("task_123");
            req.setTargetUserId("2");

            RequestDTO<DelegateTaskReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(flowableProcessService).delegateTask(any(DelegateTaskReq.class));

            ResponseDTO<EmptyBody> response = flowableProcessController.delegateTask(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(flowableProcessService, times(1)).delegateTask(any(DelegateTaskReq.class));
        }
    }

    @Nested
    @DisplayName("claimTask 测试")
    class ClaimTaskTests {

        @Test
        @DisplayName("认领任务 - 正常场景")
        void testClaimTask_Success() throws Exception {
            ClaimTaskReq req = new ClaimTaskReq();
            req.setTaskId("task_123");
            req.setUserId("1");

            RequestDTO<ClaimTaskReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(flowableProcessService).claimTask(anyString(), anyString());

            ResponseDTO<EmptyBody> response = flowableProcessController.claimTask(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(flowableProcessService, times(1)).claimTask(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("unclaimTask 测试")
    class UnclaimTaskTests {

        @Test
        @DisplayName("取消认领任务 - 正常场景")
        void testUnclaimTask_Success() throws Exception {
            TaskIdReq req = new TaskIdReq();
            req.setTaskId("task_123");

            RequestDTO<TaskIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            doNothing().when(flowableProcessService).unclaimTask(anyString());

            ResponseDTO<EmptyBody> response = flowableProcessController.unclaimTask(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(flowableProcessService, times(1)).unclaimTask(anyString());
        }
    }

    // ==================== 流程历史测试 ====================

    @Nested
    @DisplayName("getProcessHistory 测试")
    class GetProcessHistoryTests {

        @Test
        @DisplayName("查询流程历史 - 正常场景")
        void testGetProcessHistory_Success() throws Exception {
            ProcessInstanceIdReq req = new ProcessInstanceIdReq();
            req.setProcessInstanceId("instance_123");

            RequestDTO<ProcessInstanceIdReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            List<ProcessHistoryRsp> list = new ArrayList<>();
            when(flowableProcessService.getProcessHistory(anyString())).thenReturn(list);

            ResponseDTO<List<ProcessHistoryRsp>> response = flowableProcessController.getProcessHistory(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(flowableProcessService, times(1)).getProcessHistory(anyString());
        }
    }
}