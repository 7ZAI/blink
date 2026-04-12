package com.blink.base.service.flowable;

import com.blink.base.dto.req.DeployProcessReq;
import com.blink.base.dto.req.ImportXmlProcessReq;
import com.blink.base.dto.req.QueryProcessDefinitionReq;
import com.blink.base.dto.rsp.ProcessDefinitionRsp;
import com.blink.framework.common.exception.BlinkException;

/**
 * 流程定义服务接口
 * <p>
 * 提供流程定义的管理功能：部署、查询、挂起、激活、删除
 * </p>
 *
 * @author binblink
 */
public interface ProcessDefinitionService {

    /**
     * 部署流程定义
     *
     * @param req 部署请求
     * @return 部署ID
     * @throws BlinkException 部署失败时抛出
     */
    String deployProcess(DeployProcessReq req) throws BlinkException;

    /**
     * 从BPMN XML导入流程定义
     *
     * @param req 导入请求（包含XML内容）
     * @return 部署ID
     * @throws BlinkException 导入失败时抛出
     */
    String importProcessFromXml(ImportXmlProcessReq req) throws BlinkException;

    /**
     * 分页查询流程定义列表
     *
     * @param req 查询请求
     * @return 流程定义分页响应
     * @throws BlinkException 查询失败时抛出
     */
    ProcessDefinitionRsp getProcessDefinitionList(QueryProcessDefinitionReq req) throws BlinkException;

    /**
     * 根据流程定义ID获取流程图XML
     *
     * @param processDefinitionId 流程定义ID
     * @return 流程图XML
     * @throws BlinkException 获取失败时抛出
     */
    String getProcessDiagramXml(String processDefinitionId) throws BlinkException;

    /**
     * 根据流程实例ID获取流程图（高亮当前节点）
     *
     * @param processInstanceId 流程实例ID
     * @return 流程图图片字节数组
     * @throws BlinkException 获取失败时抛出
     */
    byte[] getProcessDiagramImage(String processInstanceId) throws BlinkException;

    /**
     * 挂起流程定义
     *
     * @param processDefinitionId 流程定义ID
     * @throws BlinkException 挂起失败时抛出
     */
    void suspendProcessDefinition(String processDefinitionId) throws BlinkException;

    /**
     * 激活流程定义
     *
     * @param processDefinitionId 流程定义ID
     * @throws BlinkException 激活失败时抛出
     */
    void activateProcessDefinition(String processDefinitionId) throws BlinkException;

    /**
     * 删除流程定义（删除部署）
     *
     * @param deploymentId 部署ID
     * @param cascade      是否级联删除流程实例
     * @throws BlinkException 删除失败时抛出
     */
    void deleteProcessDefinition(String deploymentId, boolean cascade) throws BlinkException;
}