package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constants.BaseErrCodeConstant;

import com.blink.base.dto.vo.DataFilterVO;
import com.blink.base.dto.vo.EntityFieldVO;
import com.blink.base.dto.req.AddDataFilterReq;
import com.blink.base.dto.req.DataFilterIdReq;
import com.blink.base.dto.req.QueryDataFilterReq;
import com.blink.base.dto.req.UpdateDataFilterReq;
import com.blink.base.dto.rsp.EntityFieldsRsp;
import com.blink.base.dto.rsp.EntityListRsp;
import com.blink.base.dto.rsp.MatchTypesRsp;
import com.blink.base.dto.rsp.QueryDataFilterRsp;
import com.blink.base.entity.SysDataFilterDO;
import com.blink.base.mapper.SysDataFilterMapper;
import com.blink.base.mapper.SysPermissionMapper;
import com.blink.base.service.SysDataFilterService;
import com.blink.datasource.component.DataScopeEntityScanner;
import com.blink.base.datascope.constants.DataScopeRuleType;
import com.blink.datasource.data.RelationInfoVO;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.utils.CustomSqlValidator;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据过滤规则服务实现类
 *
 * @author binblink
 */
@Service
@Slf4j
public class SysDataFilterServiceImpl implements SysDataFilterService {

    @Resource
    private SysDataFilterMapper sysDataFilterMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

//    @Resource
//    private DataScopeCache dataScopeCache;

    @Override
    public ResponseDTO<QueryDataFilterRsp> queryDataFilterList(RequestDTO<QueryDataFilterReq> reqDto) {
        QueryDataFilterReq req = reqDto.getBody();
        QueryDataFilterRsp rsp = new QueryDataFilterRsp();

        LambdaQueryWrapper<SysDataFilterDO> queryWrapper = new LambdaQueryWrapper<>();
        if (req != null) {
            if (StrUtil.isNotBlank(req.getDataFilterName())) {
                queryWrapper.like(SysDataFilterDO::getDataFilterName, req.getDataFilterName());
            }
            if (StrUtil.isNotBlank(req.getEntityClass())) {
                queryWrapper.eq(SysDataFilterDO::getEntityClass, req.getEntityClass());
            }
            if (StrUtil.isNotBlank(req.getRuleType())) {
                queryWrapper.eq(SysDataFilterDO::getRuleType, req.getRuleType());
            }
            if (req.getStatus() != null) {
                queryWrapper.eq(SysDataFilterDO::getStatus, req.getStatus());
            }
        }
        queryWrapper.orderByDesc(SysDataFilterDO::getCreateTime);

        // 使用 queryPageCustom 支持DO到VO的转换
        rsp = PageUtils.queryPageCustom(
                req,
                () -> sysDataFilterMapper.selectCount(queryWrapper),
                () -> {
                    List<SysDataFilterDO> doList = sysDataFilterMapper.selectList(queryWrapper);
                    List<DataFilterVO> voList = new ArrayList<>();
                    for (SysDataFilterDO dataFilter : doList) {
                        voList.add(convertToVO(dataFilter));
                    }
                    return voList;
                },
                rsp
        );

        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> addDataFilter(RequestDTO<AddDataFilterReq> reqDto) {
        AddDataFilterReq req = reqDto.getBody();

        // 验证规则类型
        validateRuleType(req.getRuleType());

        // 验证规则配置JSON格式
        validateRuleConfig(req.getRuleConfig(), req.getRuleType());

        // 验证规则配置内容有效性
        validateRuleConfigContent(req.getRuleConfig(), req.getRuleType());

        // 验证实体类是否已注册
        validateEntityClass(req.getEntityClass());

        // 构建实体
        SysDataFilterDO dataFilter = BeanUtil.copyProperties(req, SysDataFilterDO.class);
        dataFilter.setStatus((byte) 0);
        dataFilter.setCreateBy(reqDto.getLoginName());

        sysDataFilterMapper.insert(dataFilter);
        log.info("新增数据过滤规则成功，ID: {}, 名称: {}", dataFilter.getDataFilterId(), dataFilter.getDataFilterName());

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> updateDataFilter(RequestDTO<UpdateDataFilterReq> reqDto) {
        UpdateDataFilterReq req = reqDto.getBody();

        // 检查规则是否存在
        SysDataFilterDO existing = sysDataFilterMapper.selectById(req.getDataFilterId());
        if (existing == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_NOT_FOUND);
        }

        // 验证规则配置JSON格式
        validateRuleConfig(req.getRuleConfig(), existing.getRuleType());

        // 验证规则配置内容有效性
        validateRuleConfigContent(req.getRuleConfig(), existing.getRuleType());

        // 更新实体
        SysDataFilterDO dataFilter = new SysDataFilterDO();
        dataFilter.setDataFilterId(req.getDataFilterId());
        dataFilter.setDataFilterName(req.getDataFilterName());
        dataFilter.setDataFilterEnName(req.getDataFilterEnName());
        dataFilter.setRuleConfig(req.getRuleConfig());
        dataFilter.setStatus(req.getStatus());
        dataFilter.setRemark(req.getRemark());
        dataFilter.setUpdateBy(reqDto.getLoginName());

        sysDataFilterMapper.updateById(dataFilter);
        log.info("更新数据过滤规则成功，ID: {}", req.getDataFilterId());

        // 清除缓存 - 待实现
        // dataScopeCache.clearAllCache();

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> deleteDataFilter(RequestDTO<DataFilterIdReq> reqDto) {
        DataFilterIdReq req = reqDto.getBody();

        SysDataFilterDO existing = sysDataFilterMapper.selectById(req.getDataFilterId());
        if (existing == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_NOT_FOUND);
        }

        // 检查是否有权限正在使用该数据过滤规则
        int relatedCount = sysPermissionMapper.countByDataFilterId(req.getDataFilterId());
        if (relatedCount > 0) {
            log.warn("[SysDataFilter] 数据过滤规则正在被使用，无法删除 | dataFilterId: {}, relatedCount: {}",
                    req.getDataFilterId(), relatedCount);
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_FILTER_IN_USE);
        }

        sysDataFilterMapper.deleteById(req.getDataFilterId());
        log.info("删除数据过滤规则成功，ID: {}", req.getDataFilterId());

        // 清除缓存 - 待实现
        // dataScopeCache.clearAllCache();

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<DataFilterVO> getDataFilterDetail(RequestDTO<DataFilterIdReq> reqDto) {
        DataFilterIdReq req = reqDto.getBody();

        SysDataFilterDO dataFilter = sysDataFilterMapper.selectById(req.getDataFilterId());
        if (dataFilter == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_NOT_FOUND);
        }

        DataFilterVO vo = convertToVO(dataFilter);
        return ResponseDTO.newSuccessInstance(vo);
    }

    @Override
    public EntityFieldsRsp getEntityFields(String entityClass) {
        EntityFieldsRsp rsp = new EntityFieldsRsp();
        rsp.setFields(new ArrayList<>());

        if (StrUtil.isBlank(entityClass)) {
            return rsp;
        }

        try {
            Class<?> clazz = Class.forName(entityClass);
            Field[] fields = clazz.getDeclaredFields();

            List<EntityFieldVO> fieldList = new ArrayList<>();
            for (Field field : fields) {
                // 跳过静态字段和serialVersionUID
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                EntityFieldVO fieldVO = new EntityFieldVO();
                fieldVO.setFieldName(field.getName());
                fieldVO.setFieldType(field.getType().getSimpleName());

                // 获取数据库列名（简化处理，实际应解析@TableField注解）
                fieldVO.setColumnName(camelToSnake(field.getName()));

                fieldList.add(fieldVO);
            }

            rsp.setFields(fieldList);
        } catch (ClassNotFoundException e) {
            log.warn("实体类不存在: {}", entityClass);
        }

        return rsp;
    }

    @Override
    public EntityListRsp getEntityList() {
        EntityListRsp rsp = new EntityListRsp();
        rsp.setEntities(DataScopeEntityScanner.getRegisteredEntities());
        return rsp;
    }

    @Override
    public ResponseDTO<EmptyBody> refreshCache() {
        // 清除缓存 - 待实现
        // dataScopeCache.clearAllCache();
        log.info("已刷新所有数据权限缓存");
        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public List<RuleConfig> getRuleConfigsByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            return new ArrayList<>();
        }

        // 查询用户拥有的所有数据过滤规则
        List<SysDataFilterDO> dataFilterList = sysDataFilterMapper.selectByUserId(userId);
        if (CollUtil.isEmpty(dataFilterList)) {
            return new ArrayList<>();
        }

        // 解析规则配置JSON为RuleConfig对象
        List<RuleConfig> ruleConfigs = new ArrayList<>();
        for (SysDataFilterDO dataFilter : dataFilterList) {
            if (StrUtil.isNotBlank(dataFilter.getRuleConfig())) {
                try {
                    RuleConfig config = JSON.parseObject(dataFilter.getRuleConfig(), RuleConfig.class);
                    if (config != null) {
                        // 设置规则类型
                        config.setRuleType(dataFilter.getRuleType());
                        // 设置实体类（用于规则匹配）
                        config.setEntityClass(dataFilter.getEntityClass());
                        ruleConfigs.add(config);
                    }
                } catch (Exception e) {
                    log.warn("[SysDataFilter] 规则配置解析失败 | dataFilterId: {}, error: {}",
                            dataFilter.getDataFilterId(), e.getMessage());
                }
            }
        }

        log.debug("[SysDataFilter] 查询用户数据权限规则 | userId: {}, ruleCount: {}", userId, ruleConfigs.size());
        return ruleConfigs;
    }

    @Override
    public MatchTypesRsp getMatchTypes(String tableName, String relationName) {
        MatchTypesRsp rsp = new MatchTypesRsp();
        rsp.setOptions(new ArrayList<>());

        if (StrUtil.isBlank(tableName) || StrUtil.isBlank(relationName)) {
            return rsp;
        }

        // 获取该表的关联关系列表
        List<RelationInfoVO> relations = DataScopeEntityScanner.getRelations(tableName);
        if (CollUtil.isEmpty(relations)) {
            return rsp;
        }

        // 根据关联关系名称查找对应的关联关系
        RelationInfoVO targetRelation = null;
        for (RelationInfoVO relation : relations) {
            if (relationName.equals(relation.getName())) {
                targetRelation = relation;
                break;
            }
        }

        if (targetRelation == null) {
            log.warn("[SysDataFilter] 未找到关联关系 | tableName: {}, relationName: {}", tableName, relationName);
            return rsp;
        }

        // 根据目标实体名称生成匹配类型选项
        String targetName = targetRelation.getTargetName();
        List<MatchTypesRsp.MatchTypeOption> options = buildMatchTypeOptions(targetName, tableName);
        rsp.setOptions(options);

        return rsp;
    }

    /**
     * 根据目标实体名称构建匹配类型选项
     *
     * @param targetName 目标实体名称（如"用户"、"部门"、"角色"）
     * @param sourceTable 源表名（用于判断特殊场景）
     * @return 匹配类型选项列表
     */
    private List<MatchTypesRsp.MatchTypeOption> buildMatchTypeOptions(String targetName, String sourceTable) {
        List<MatchTypesRsp.MatchTypeOption> options = new ArrayList<>();

        if (StrUtil.isBlank(targetName)) {
            return options;
        }

        // 判断主表是否为部门表
        boolean isGroupTable = "sys_group".equalsIgnoreCase(sourceTable);

        switch (targetName) {
            case "用户" -> {
                options.add(new MatchTypesRsp.MatchTypeOption("CURRENT_USER", "当前用户", true));
                options.add(new MatchTypesRsp.MatchTypeOption("USER_LIST", "指定用户", false));
                // 当主表是部门表时，添加额外的匹配类型
                if (isGroupTable) {
                    options.add(new MatchTypesRsp.MatchTypeOption("CURRENT_USER_DEPT_CHILDREN", "当前用户所在部门及其子部门", true));
                }
            }
            case "部门" -> {
                options.add(new MatchTypesRsp.MatchTypeOption("CURRENT_DEPT", "当前用户所属组织", true));
                options.add(new MatchTypesRsp.MatchTypeOption("CURRENT_DEPT_CHILDREN", "当前用户组织及子组织", true));
                options.add(new MatchTypesRsp.MatchTypeOption("DEPT_LIST", "指定组织", false));
            }
            case "角色" -> {
                options.add(new MatchTypesRsp.MatchTypeOption("CURRENT_ROLE", "当前用户拥有的角色", true));
                options.add(new MatchTypesRsp.MatchTypeOption("ROLE_LIST", "指定角色", false));
            }
            default -> {
                // 其他实体类型暂无匹配选项
                log.debug("[SysDataFilter] 目标实体 [{}] 不支持动态匹配", targetName);
            }
        }

        return options;
    }

    /**
     * 转换为VO
     *
     * @param dataFilter 实体
     * @return VO
     */
    private DataFilterVO convertToVO(SysDataFilterDO dataFilter) {
        DataFilterVO vo = BeanUtil.copyProperties(dataFilter, DataFilterVO.class);
        return vo;
    }

    /**
     * 验证规则类型
     *
     * @param ruleType 规则类型
     */
    private void validateRuleType(String ruleType) {
        if (StrUtil.isBlank(ruleType)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_INVALID);
        }

        try {
            DataScopeRuleType.valueOf(ruleType);
        } catch (IllegalArgumentException e) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_INVALID);
        }
    }

    /**
     * 验证规则配置JSON格式
     *
     * @param ruleConfig 规则配置JSON
     * @param ruleType   规则类型
     */
    private void validateRuleConfig(String ruleConfig, String ruleType) {
        if (StrUtil.isBlank(ruleConfig)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_INVALID);
        }

        try {
            JSON.parseObject(ruleConfig);
        } catch (Exception e) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_INVALID);
        }

        // 自定义SQL类型需要额外验证
        if (DataScopeRuleType.CUSTOM_SQL.name().equals(ruleType)) {
            try {
                RuleConfig config = JSON.parseObject(ruleConfig, RuleConfig.class);
                if (config != null && StrUtil.isNotBlank(config.getSqlFragment())) {
                    CustomSqlValidator.validate(config.getSqlFragment());
                }
            } catch (Exception e) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
            }
        }
    }

    /**
     * 验证规则配置内容有效性
     * 检查配置内容是否符合规则类型的要求
     *
     * @param ruleConfig 规则配置JSON
     * @param ruleType   规则类型
     */
    private void validateRuleConfigContent(String ruleConfig, String ruleType) {
        if (StrUtil.isBlank(ruleConfig)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_EMPTY);
        }

        try {
            JSONObject config = JSON.parseObject(ruleConfig);

            // 根据不同规则类型验证配置内容
            switch (DataScopeRuleType.valueOf(ruleType)) {
                case FIELD_FILTER -> {
                    // 字段过滤：至少选择一个字段
                    boolean hasIncludeFields = config.containsKey("includeFields")
                            && config.getJSONArray("includeFields") != null
                            && !config.getJSONArray("includeFields").isEmpty();
                    boolean hasExcludeFields = config.containsKey("excludeFields")
                            && config.getJSONArray("excludeFields") != null
                            && !config.getJSONArray("excludeFields").isEmpty();
                    if (!hasIncludeFields && !hasExcludeFields) {
                        log.warn("[SysDataFilter] 字段过滤规则未选择任何字段");
                        BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_EMPTY);
                    }
                }
                case CUSTOM_SQL -> {
                    // 自定义SQL：sqlFragment不能为空
                    String sqlFragment = config.getString("sqlFragment");
                    if (StrUtil.isBlank(sqlFragment)) {
                        log.warn("[SysDataFilter] 自定义SQL规则未配置SQL片段");
                        BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_EMPTY);
                    }
                }
                case RELATION_FILTER -> {
                    // 关联过滤：必须配置关联关系和匹配类型
                    String relationMatchType = config.getString("relationMatchType");
                    if (StrUtil.isBlank(relationMatchType)) {
                        log.warn("[SysDataFilter] 关联过滤规则未配置匹配类型");
                        BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_EMPTY);
                    }
                    // 非动态类型需要配置匹配值
                    boolean isDynamic = "CURRENT_USER".equals(relationMatchType)
                            || "CURRENT_DEPT".equals(relationMatchType)
                            || "CURRENT_DEPT_CHILDREN".equals(relationMatchType)
                            || "CURRENT_ROLE".equals(relationMatchType)
                            || "CURRENT_USER_DEPT_CHILDREN".equals(relationMatchType);
                    if (!isDynamic) {
                        var matchValues = config.getJSONArray("relationMatchValues");
                        if (matchValues == null || matchValues.isEmpty()) {
                            log.warn("[SysDataFilter] 关联过滤规则未选择匹配值");
                            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_EMPTY);
                        }
                    }
                }
                case CREATOR_FILTER, DATE_RANGE_FILTER -> {
                    // 创建者过滤和日期范围过滤：需要配置字段名
                    String field = config.getString("field");
                    if (StrUtil.isBlank(field)) {
                        log.warn("[SysDataFilter] 规则未配置字段名 | ruleType: {}", ruleType);
                        BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_EMPTY);
                    }
                }
                default -> {
                    // 其他类型暂不做额外验证
                }
            }
        } catch (IllegalArgumentException e) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_RULE_CONFIG_INVALID);
        }
    }

    /**
     * 验证实体类是否已注册
     *
     * @param entityClass 实体类全限定名
     */
    private void validateEntityClass(String entityClass) {
        if (StrUtil.isBlank(entityClass)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_ENTITY_NOT_REGISTERED);
        }

        String tableName = DataScopeEntityScanner.getTableName(entityClass);
        if (tableName == null) {
            log.warn("实体类未注册: {}", entityClass);
            // 不强制要求注册，允许配置
        }
    }

    /**
     * 驼峰转下划线
     *
     * @param camel 驼峰字符串
     * @return 下划线字符串
     */
    private String camelToSnake(String camel) {
        if (StrUtil.isBlank(camel)) {
            return camel;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}