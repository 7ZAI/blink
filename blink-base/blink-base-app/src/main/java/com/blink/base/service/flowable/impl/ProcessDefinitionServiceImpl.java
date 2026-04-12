package com.blink.base.service.flowable.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.constants.WorkflowConstant;
import com.blink.base.dto.req.DeployProcessReq;
import com.blink.base.dto.req.ImportXmlProcessReq;
import com.blink.base.dto.req.QueryProcessDefinitionReq;
import com.blink.base.dto.rsp.ProcessDefinitionRsp;
import com.blink.base.dto.vo.ProcessDefinitionVO;
import com.blink.base.service.flowable.ProcessDefinitionService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现类
 * <p>
 * 提供流程定义的管理功能：部署、查询、挂起、激活、删除
 * </p>
 *
 * @author binblink
 */
@Service
@Slf4j
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ProcessEngineConfiguration processEngineConfiguration;

    public ProcessDefinitionServiceImpl(RepositoryService repositoryService,
                                         RuntimeService runtimeService,
                                         HistoryService historyService,
                                         ProcessEngineConfiguration processEngineConfiguration) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deployProcess(DeployProcessReq req) throws BlinkException {
        try {
            log.info("[Workflow] 开始部署流程定义 | name: {}, key: {}", req.getProcessName(), req.getProcessKey());

            Deployment deployment = repositoryService.createDeployment()
                    .name(req.getProcessName())
                    .key(req.getProcessKey())
                    .addString(req.getProcessKey() + ".bpmn20.xml", req.getBpmnXmlContent())
                    .deploy();

            log.info("[Workflow] 流程定义部署成功 | deploymentId: {}", deployment.getId());
            return deployment.getId();

        } catch (Exception e) {
            log.error("[Workflow] 部署流程定义失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("部署流程定义失败: " + e.getMessage(), e, BaseErrCodeConstant.DEPLOY_PROCESS_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importProcessFromXml(ImportXmlProcessReq req) throws BlinkException {
        try {
            log.info("[Workflow] 开始导入流程定义 | name: {}", req.getProcessName());

            // 验证XML格式
            validateBpmnXml(req.getBpmnXmlContent());

            // 解析XML获取流程KEY
            String processKey = extractProcessKeyFromXml(req.getBpmnXmlContent());

            Deployment deployment = repositoryService.createDeployment()
                    .name(req.getProcessName())
                    .key(processKey)
                    .addString(processKey + ".bpmn20.xml", req.getBpmnXmlContent())
                    .deploy();

            log.info("[Workflow] 流程定义导入成功 | deploymentId: {}", deployment.getId());
            return deployment.getId();

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Workflow] 导入流程定义失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("导入流程定义失败: " + e.getMessage(), e, BaseErrCodeConstant.XML_IMPORT_ERROR);
        }
    }

    @Override
    public ProcessDefinitionRsp getProcessDefinitionList(QueryProcessDefinitionReq req) throws BlinkException {
        try {
            log.info("[Workflow] 查询流程定义列表 | name: {}, key: {}", req.getName(), req.getKey());

            ProcessDefinitionQuery query = buildProcessDefinitionQuery(req);

            // 获取最新版本映射
            Map<String, String> latestVersionKeys = getLatestVersionKeys();

            ProcessDefinitionRsp rsp = new ProcessDefinitionRsp();

            return PageUtils.queryPage(req, () -> executeProcessDefinitionQuery(query, latestVersionKeys), rsp);

        } catch (Exception e) {
            log.error("[Workflow] 查询流程定义列表失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("查询流程定义列表失败: " + e.getMessage(), e, BaseErrCodeConstant.QUERY_PROCESS_DEF_ERROR);
        }
    }

    @Override
    public String getProcessDiagramXml(String processDefinitionId) throws BlinkException {
        try {
            log.debug("[Workflow] 获取流程图XML | processDefinitionId: {}", processDefinitionId);

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_DEF_NOT_FOUND);
            }

            byte[] xmlBytes = repositoryService.getModelEditorSource(processDefinitionId);
            if (xmlBytes != null) {
                return new String(xmlBytes);
            }

            return null;

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Workflow] 获取流程图XML失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取流程图XML失败: " + e.getMessage(), e, BaseErrCodeConstant.GET_DIAGRAM_XML_ERROR);
        }
    }

    @Override
    public byte[] getProcessDiagramImage(String processInstanceId) throws BlinkException {
        try {
            log.debug("[Workflow] 获取流程图图片 | processInstanceId: {}", processInstanceId);

            org.flowable.engine.runtime.ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            String processDefinitionId;
            List<String> activeActivityIds = Collections.emptyList();

            if (processInstance != null) {
                processDefinitionId = processInstance.getProcessDefinitionId();
                activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
            } else {
                HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (historicProcessInstance == null) {
                    BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_INSTANCE_NOT_FOUND);
                }
                processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_DEF_NOT_FOUND);
            }

            return generateProcessDiagram(bpmnModel, activeActivityIds);

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Workflow] 获取流程图图片失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取流程图图片失败: " + e.getMessage(), e, BaseErrCodeConstant.GET_DIAGRAM_IMAGE_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendProcessDefinition(String processDefinitionId) throws BlinkException {
        try {
            log.info("[Workflow] 挂起流程定义 | processDefinitionId: {}", processDefinitionId);
            repositoryService.suspendProcessDefinitionById(processDefinitionId);
            log.info("[Workflow] 流程定义挂起成功 | processDefinitionId: {}", processDefinitionId);
        } catch (Exception e) {
            log.error("[Workflow] 挂起流程定义失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("挂起流程定义失败: " + e.getMessage(), e, BaseErrCodeConstant.SUSPEND_PROCESS_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateProcessDefinition(String processDefinitionId) throws BlinkException {
        try {
            log.info("[Workflow] 激活流程定义 | processDefinitionId: {}", processDefinitionId);
            repositoryService.activateProcessDefinitionById(processDefinitionId);
            log.info("[Workflow] 流程定义激活成功 | processDefinitionId: {}", processDefinitionId);
        } catch (Exception e) {
            log.error("[Workflow] 激活流程定义失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("激活流程定义失败: " + e.getMessage(), e, BaseErrCodeConstant.ACTIVATE_PROCESS_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessDefinition(String deploymentId, boolean cascade) throws BlinkException {
        try {
            log.info("[Workflow] 删除流程定义 | deploymentId: {}, cascade: {}", deploymentId, cascade);
            repositoryService.deleteDeployment(deploymentId, cascade);
            log.info("[Workflow] 流程定义删除成功 | deploymentId: {}", deploymentId);
        } catch (Exception e) {
            log.error("[Workflow] 删除流程定义失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("删除流程定义失败: " + e.getMessage(), e, BaseErrCodeConstant.DELETE_PROCESS_ERROR);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 验证BPMN XML格式
     */
    private void validateBpmnXml(String xmlContent) throws BlinkException {
        try {
            org.flowable.bpmn.converter.BpmnXMLConverter converter = new org.flowable.bpmn.converter.BpmnXMLConverter();
            javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
            javax.xml.stream.XMLStreamReader reader = factory.createXMLStreamReader(new java.io.StringReader(xmlContent));
            converter.convertToBpmnModel(reader);
        } catch (Exception e) {
            throw new BlinkException("BPMN XML格式验证失败: " + e.getMessage(), e, BaseErrCodeConstant.XML_IMPORT_ERROR);
        }
    }

    /**
     * 从BPMN XML中提取流程KEY
     */
    private String extractProcessKeyFromXml(String xmlContent) throws BlinkException {
        try {
            org.flowable.bpmn.converter.BpmnXMLConverter converter = new org.flowable.bpmn.converter.BpmnXMLConverter();
            javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
            javax.xml.stream.XMLStreamReader reader = factory.createXMLStreamReader(new java.io.StringReader(xmlContent));
            org.flowable.bpmn.model.BpmnModel model = converter.convertToBpmnModel(reader);

            if (CollUtil.isEmpty(model.getProcesses())) {
                throw new BlinkException("BPMN XML中未找到流程定义", BaseErrCodeConstant.XML_IMPORT_ERROR);
            }

            return model.getProcesses().get(0).getId();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            throw new BlinkException("提取流程KEY失败: " + e.getMessage(), e, BaseErrCodeConstant.XML_IMPORT_ERROR);
        }
    }

    /**
     * 构建流程定义查询条件
     */
    private ProcessDefinitionQuery buildProcessDefinitionQuery(QueryProcessDefinitionReq req) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

        if (StrUtil.isNotBlank(req.getName())) {
            query.processDefinitionNameLike(WorkflowConstant.LIKE_PREFIX + req.getName() + WorkflowConstant.LIKE_SUFFIX);
        }
        if (StrUtil.isNotBlank(req.getKey())) {
            query.processDefinitionKey(req.getKey());
        }
        if (Boolean.TRUE.equals(req.getLatestVersion())) {
            query.latestVersion();
        }

        return query;
    }

    /**
     * 获取最新版本映射
     */
    private Map<String, String> getLatestVersionKeys() {
        List<ProcessDefinition> latestVersions = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list();
        return latestVersions.stream()
                .collect(Collectors.toMap(
                        ProcessDefinition::getKey,
                        ProcessDefinition::getId,
                        (a, b) -> a
                ));
    }

    /**
     * 执行流程定义查询并转换为VO列表
     */
    private List<ProcessDefinitionVO> executeProcessDefinitionQuery(
            ProcessDefinitionQuery query, Map<String, String> latestVersionKeys) {
        List<ProcessDefinition> processDefinitions = query
                .orderByProcessDefinitionVersion()
                .desc()
                .list();

        return processDefinitions.stream()
                .map(pd -> convertToProcessDefinitionVO(pd, latestVersionKeys))
                .collect(Collectors.toList());
    }

    /**
     * 生成流程图
     */
    private byte[] generateProcessDiagram(BpmnModel bpmnModel, List<String> activeActivityIds) throws Exception {
        ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
        InputStream inputStream = diagramGenerator.generateDiagram(
                bpmnModel,
                WorkflowConstant.IMAGE_FORMAT_PNG,
                activeActivityIds,
                Collections.emptyList(),
                WorkflowConstant.FONT_NAME_SONGTI,
                WorkflowConstant.FONT_NAME_SONGTI,
                WorkflowConstant.FONT_NAME_SONGTI,
                null,
                1.0,
                true
        );

        if (inputStream == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.GENERATE_DIAGRAM_ERROR);
        }

        return inputStream.readAllBytes();
    }

    /**
     * 转换ProcessDefinition为ProcessDefinitionVO
     */
    private ProcessDefinitionVO convertToProcessDefinitionVO(ProcessDefinition pd, Map<String, String> latestVersionKeys) {
        ProcessDefinitionVO vo = new ProcessDefinitionVO();
        vo.setProcessDefinitionId(pd.getId());
        vo.setProcessDefinitionKey(pd.getKey());
        vo.setProcessDefinitionName(pd.getName());
        vo.setDescription(pd.getDescription());
        vo.setVersion(pd.getVersion());
        vo.setDeploymentId(pd.getDeploymentId());
        vo.setSuspended(pd.isSuspended());
        vo.setLatestVersion(latestVersionKeys.get(pd.getKey()).equals(pd.getId()));
        return vo;
    }
}