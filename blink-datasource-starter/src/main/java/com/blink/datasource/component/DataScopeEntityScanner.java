package com.blink.datasource.component;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.blink.datasource.annotation.DataScopeEntity;
import com.blink.datasource.annotation.DataScopeRelation;
import com.blink.datasource.annotation.RelationEndpoint;
import com.blink.datasource.data.RegisteredEntityVO;
import com.blink.datasource.data.RelationInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据范围实体扫描器
 * 启动时扫描带有 @DataScopeEntity 注解的实体类，建立表名到实体类的映射
 * 同时扫描带有 @DataScopeRelation 注解的关联表DO，构建关联关系缓存
 *
 * @author binblink
 */
@Slf4j
public class DataScopeEntityScanner implements ApplicationRunner {

    /**
     * 表名到实体类的映射（线程安全）
     * 只包含标记了 @DataScopeEntity 注解的实体
     */
    private static final Map<String, Class<?>> TABLE_ENTITY_MAP = new ConcurrentHashMap<>();

    /**
     * 已注册的数据范围实体列表（带 @DataScopeEntity 注解）
     */
    private static final List<RegisteredEntityVO> REGISTERED_ENTITIES = new ArrayList<>();

    /**
     * 实体表 -> 关联关系列表 映射
     * Key: 实体表名 (如 sys_user)
     * Value: 该实体可用的关联关系列表
     */
    private static final Map<String, List<RelationInfoVO>> TABLE_RELATIONS_MAP = new ConcurrentHashMap<>();

    /**
     * 应用启动时执行扫描
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始扫描数据范围实体类...");

        // 清空旧数据（防止热重载时重复添加）
        TABLE_ENTITY_MAP.clear();
        REGISTERED_ENTITIES.clear();
        TABLE_RELATIONS_MAP.clear();

        // 扫描带有 @DataScopeEntity 注解的实体类
        // 注意：ClassUtil.scanPackageByAnnotation 不支持 ** 通配符，需要指定基础包名
        Set<Class<?>> dataScopeEntities = ClassUtil.scanPackageByAnnotation(
                "com.blink", DataScopeEntity.class
        );

        for (Class<?> clazz : dataScopeEntities) {
            DataScopeEntity annotation = clazz.getAnnotation(DataScopeEntity.class);
            TableName tableNameAnnotation = clazz.getAnnotation(TableName.class);

            if (annotation == null) {
                continue;
            }

            // 构建注册实体VO
            RegisteredEntityVO vo = new RegisteredEntityVO();
            vo.setEntityClass(clazz.getName());
            vo.setEntityName(annotation.name());

            // 英文名称：优先使用注解配置，否则使用类名
            String enName = annotation.enName();
            if (StrUtil.isBlank(enName)) {
                enName = clazz.getSimpleName();
            }
            vo.setEntityEnName(enName);

            // 表名
            if (tableNameAnnotation != null && tableNameAnnotation.value() != null) {
                String tableName = tableNameAnnotation.value();
                vo.setTableName(tableName);

                // 建立表名到实体类的映射
                TABLE_ENTITY_MAP.put(tableName, clazz);
                log.debug("映射表名 [{}] -> 实体类 [{}]", tableName, clazz.getName());
            }

            REGISTERED_ENTITIES.add(vo);
            log.debug("注册数据范围实体: {} -> {}", annotation.name(), clazz.getName());
        }

        log.info("数据范围实体类扫描完成，共注册 {} 个实体，映射 {} 个表",
                REGISTERED_ENTITIES.size(), TABLE_ENTITY_MAP.size());

        // 扫描关联关系注解
        scanRelationAnnotations();
    }

    /**
     * 扫描带有 @DataScopeRelation 注解的关联表DO，构建双向关联关系缓存
     */
    private void scanRelationAnnotations() {
        log.info("开始扫描数据范围关联关系...");

        Set<Class<?>> relationClasses = ClassUtil.scanPackageByAnnotation(
                "com.blink", DataScopeRelation.class
        );

        int relationCount = 0;
        for (Class<?> clazz : relationClasses) {
            DataScopeRelation relation = clazz.getAnnotation(DataScopeRelation.class);
            TableName tableNameAnnotation = clazz.getAnnotation(TableName.class);

            if (relation == null || tableNameAnnotation == null) {
                continue;
            }

            String relationTable = tableNameAnnotation.value();
            RelationEndpoint endpointA = relation.endpointA();
            RelationEndpoint endpointB = relation.endpointB();

            // 获取端点A的关联字段（relationField为空时默认使用field）
            String endpointARelationField = StrUtil.isNotBlank(endpointA.relationField())
                    ? endpointA.relationField()
                    : endpointA.field();
            // 获取端点B的关联字段（relationField为空时默认使用field）
            String endpointBRelationField = StrUtil.isNotBlank(endpointB.relationField())
                    ? endpointB.relationField()
                    : endpointB.field();

            // 为端点A构建关联关系（A视角：关联到B）
            // 匹配类型根据目标实体B的名称自动推断
            List<String> supportMatchTypesForA = inferMatchTypes(endpointB.name());
            Map<String, String> matchTypeLabelsForA = buildMatchTypeLabels(endpointB.name());
            RelationInfoVO relationForA = buildRelationInfo(
                    endpointB.name(),
                    endpointB.enName(),
                    relationTable,
                    endpointA.field(),
                    endpointARelationField,
                    endpointBRelationField,
                    endpointB.table(),
                    endpointB.field(),
                    supportMatchTypesForA,
                    matchTypeLabelsForA
            );
            addToRelationMap(endpointA.table(), relationForA);

            // 为端点B构建关联关系（B视角：关联到A）
            // 匹配类型根据目标实体A的名称自动推断
            List<String> supportMatchTypesForB = inferMatchTypes(endpointA.name());
            Map<String, String> matchTypeLabelsForB = buildMatchTypeLabels(endpointA.name());
            RelationInfoVO relationForB = buildRelationInfo(
                    endpointA.name(),
                    endpointA.enName(),
                    relationTable,
                    endpointB.field(),
                    endpointBRelationField,
                    endpointARelationField,
                    endpointA.table(),
                    endpointA.field(),
                    supportMatchTypesForB,
                    matchTypeLabelsForB
            );
            addToRelationMap(endpointB.table(), relationForB);

            relationCount++;
            log.debug("注册关联关系: {} <-> {} (表: {})",
                    endpointA.table(), endpointB.table(), relationTable);
        }

        log.info("数据范围关联关系扫描完成，共注册 {} 个关联表，{} 个关联关系",
                relationCount, TABLE_RELATIONS_MAP.values().stream().mapToInt(List::size).sum());
    }

    /**
     * 根据目标实体名称推断支持的匹配类型
     * <p>
     * 匹配类型说明：
     * - 目标"用户"：通过用户进行匹配，可匹配当前用户或指定用户
     * - 目标"部门"：通过部门进行匹配，可匹配当前用户所属部门、当前用户部门层级、或指定部门
     * - 目标"角色"：通过角色进行匹配，可匹配当前用户角色或指定角色
     * - 其他实体：无动态匹配，暂不支持关联过滤
     *
     * @param targetName 目标实体名称（如"用户"、"部门"、"角色"）
     * @return 支持的匹配类型列表
     */
    private List<String> inferMatchTypes(String targetName) {
        return switch (targetName) {
            case "用户" -> List.of("CURRENT_USER", "USER_LIST");
            case "部门" -> List.of("CURRENT_DEPT", "CURRENT_DEPT_CHILDREN", "DEPT_LIST");
            case "角色" -> List.of("CURRENT_ROLE", "ROLE_LIST");
            default -> {
                log.debug("[DataScope] 目标实体 [{}] 不支持动态匹配", targetName);
                yield List.of();
            }
        };
    }

    /**
     * 构建匹配类型的描述
     * 根据目标实体类型生成对应的描述文本
     *
     * @param targetName 目标实体名称
     * @return 匹配类型 -> 描述 的映射
     */
    private Map<String, String> buildMatchTypeLabels(String targetName) {
        Map<String, String> labels = new LinkedHashMap<>();

        switch (targetName) {
            case "用户" -> {
                labels.put("CURRENT_USER", "当前用户");
                labels.put("USER_LIST", "指定用户");
            }
            case "部门" -> {
                labels.put("CURRENT_DEPT", "当前用户所属组织");
                labels.put("CURRENT_DEPT_CHILDREN", "当前用户组织及子组织");
                labels.put("DEPT_LIST", "指定组织");
            }
            case "角色" -> {
                labels.put("CURRENT_ROLE", "当前用户拥有的角色");
                labels.put("ROLE_LIST", "指定角色");
            }
            default -> {
                // 其他实体类型暂无描述
            }
        }

        return labels;
    }

    /**
     * 构建关联关系VO
     */
    private RelationInfoVO buildRelationInfo(
            String targetName, String targetEnName,
            String relationTable,
            String sourceField, String relationSourceField, String relationTargetField,
            String targetTable, String targetField,
            List<String> supportMatchTypes,
            Map<String, String> matchTypeLabels) {

        RelationInfoVO vo = new RelationInfoVO();
        vo.setName(targetName + "关联");
        vo.setEnName(StrUtil.isNotBlank(targetEnName) ? targetEnName + "Relation" : targetName + "Relation");
        vo.setRelationTable(relationTable);
        vo.setSourceField(sourceField);
        vo.setRelationSourceField(relationSourceField);
        vo.setRelationTargetField(relationTargetField);
        vo.setTargetTable(targetTable);
        vo.setTargetField(targetField);
        vo.setTargetName(targetName);
        vo.setSupportMatchTypes(supportMatchTypes);
        vo.setMatchTypeLabels(matchTypeLabels);
        return vo;
    }

    /**
     * 添加到关联关系缓存
     */
    private void addToRelationMap(String table, RelationInfoVO relation) {
        TABLE_RELATIONS_MAP.computeIfAbsent(table, k -> new ArrayList<>()).add(relation);
    }

    /**
     * 根据表名获取实体类
     *
     * @param tableName 表名
     * @return 实体类，未找到返回null
     */
    public static Class<?> getEntityClass(String tableName) {
        if (tableName == null) {
            return null;
        }
        return TABLE_ENTITY_MAP.get(tableName);
    }

    /**
     * 根据实体类名获取表名
     *
     * @param entityClassName 实体类全限定名
     * @return 表名，未找到返回null
     */
    public static String getTableName(String entityClassName) {
        for (Map.Entry<String, Class<?>> entry : TABLE_ENTITY_MAP.entrySet()) {
            if (entry.getValue().getName().equals(entityClassName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 检查表名是否已注册
     *
     * @param tableName 表名
     * @return true=已注册
     */
    public static boolean isRegistered(String tableName) {
        return TABLE_ENTITY_MAP.containsKey(tableName);
    }

    /**
     * 获取所有已注册的表名
     *
     * @return 表名集合
     */
    public static Set<String> getAllTableNames() {
        return TABLE_ENTITY_MAP.keySet();
    }

    /**
     * 获取已注册的数据范围实体列表（包含关联关系）
     * 每次调用都会创建新的VO对象，避免修改静态缓存
     *
     * @return 已注册实体列表
     */
    public static List<RegisteredEntityVO> getRegisteredEntities() {
        List<RegisteredEntityVO> result = new ArrayList<>();
        for (RegisteredEntityVO entity : REGISTERED_ENTITIES) {
            // 创建新的VO对象，避免修改静态列表中的对象
            RegisteredEntityVO vo = new RegisteredEntityVO();
            vo.setEntityClass(entity.getEntityClass());
            vo.setEntityName(entity.getEntityName());
            vo.setEntityEnName(entity.getEntityEnName());
            vo.setTableName(entity.getTableName());

            // 设置关联关系（创建副本）
            String tableName = entity.getTableName();
            if (tableName != null) {
                List<RelationInfoVO> relations = TABLE_RELATIONS_MAP.get(tableName);
                vo.setRelations(relations != null ? new ArrayList<>(relations) : Collections.emptyList());
            } else {
                vo.setRelations(Collections.emptyList());
            }

            result.add(vo);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 获取指定实体表的关联关系列表
     *
     * @param tableName 实体表名
     * @return 关联关系列表，无关联关系返回空列表
     */
    public static List<RelationInfoVO> getRelations(String tableName) {
        List<RelationInfoVO> relations = TABLE_RELATIONS_MAP.get(tableName);
        return relations != null ? new ArrayList<>(relations) : Collections.emptyList();
    }

    /**
     * 检查实体类是否标记了 @DataScopeEntity 注解
     *
     * @param entityClass 实体类
     * @return true=已标记，需要进行数据权限校验
     */
    public static boolean isDataScopeEntity(Class<?> entityClass) {
        if (entityClass == null) {
            return false;
        }
        return entityClass.isAnnotationPresent(DataScopeEntity.class);
    }

    /**
     * 检查表名对应的实体类是否标记了 @DataScopeEntity 注解
     *
     * @param tableName 表名
     * @return true=已标记，需要进行数据权限校验
     */
    public static boolean isDataScopeEntityByTable(String tableName) {
        // TABLE_ENTITY_MAP 中只包含 @DataScopeEntity 标记的实体
        return TABLE_ENTITY_MAP.containsKey(tableName);
    }

    /**
     * 检查实体表是否有关联关系
     *
     * @param tableName 实体表名
     * @return true=有关联关系
     */
    public static boolean hasRelation(String tableName) {
        List<RelationInfoVO> relations = TABLE_RELATIONS_MAP.get(tableName);
        return relations != null && !relations.isEmpty();
    }
}