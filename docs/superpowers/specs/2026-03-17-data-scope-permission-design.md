# 数据范围权限设计文档

## 一、需求概述

在现有 base-app 权限体系基础上，设计实现动态数据范围权限系统。支持通过规则配置对查询接口进行字段过滤和条件过滤，而非传统的部门关联数据范围。

### 核心需求

1. **规则类型固定**：字段过滤、创建人过滤、部门过滤、时间范围过滤、状态过滤、自定义SQL
2. **动态配置**：按角色配置过滤规则，规则间为AND关系
3. **多角色合并**：用户拥有多个角色时，规则取并集（最宽松）
4. **超管绕过**：超级管理员自动跳过所有过滤规则
5. **按实体配置**：过滤规则基于数据实体（Entity/DO）配置，所有查询该实体的接口自动应用规则
6. **SQL层面过滤**：字段过滤在SELECT时排除字段，条件过滤动态追加WHERE条件

## 二、现有架构分析

### 已有相关表结构

| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `sys_permission` | 权限表 | `ac_type=2` 表示数据权限，`data_filter_id` 关联过滤规则 |
| `sys_data_filter` | 数据过滤规则表 | 存在但结构需调整 |
| `sys_role_perm_rela` | 角色-权限关系表 | 角色分配权限 |
| `sys_group` | 组织架构（部门） | 支持层级结构 |
| `sys_user_group_rela` | 用户-部门关系 | 用户所属部门 |

### 已有实体类

- `SysPermissionDO`：权限实体，含 `acType` 和 `dataFilterId` 字段
- `SysDataFilterDO`：数据过滤规则实体，需调整字段

## 三、数据库设计

### 3.1 sys_data_filter 表结构调整

```sql
CREATE TABLE `sys_data_filter` (
    `data_filter_id`        INT NOT NULL AUTO_INCREMENT COMMENT '数据过滤ID',
    `data_filter_name`      VARCHAR(64) NOT NULL COMMENT '过滤规则名称',
    `data_filter_en_name`   VARCHAR(64) COMMENT '过滤规则英文名称',
    `entity_class`          VARCHAR(255) NOT NULL COMMENT '实体类全限定名（如 com.blink.entity.com.blink.base.SysUserDO）',
    `table_name`            VARCHAR(64) NOT NULL COMMENT '对应表名（如 sys_user）',
    `rule_type`             VARCHAR(32) NOT NULL COMMENT '规则类型：FIELD_FILTER/CREATOR_FILTER/DEPT_FILTER/DATE_RANGE_FILTER/STATUS_FILTER/CUSTOM_SQL',
    `rule_config`           TEXT NOT NULL COMMENT '规则配置JSON',
    `status`                TINYINT DEFAULT 0 COMMENT '状态 0启用 1禁用',
    `remark`                VARCHAR(500) COMMENT '备注',
    `create_by`             VARCHAR(30),
    `create_time`           TIMESTAMP,
    `update_by`             VARCHAR(30),
    `update_time`           TIMESTAMP,
    PRIMARY KEY (`data_filter_id`),
    KEY `idx_entity_class` (`entity_class`),
    KEY `idx_table_name` (`table_name`)
) COMMENT '数据权限过滤规则表';
```

**说明：** 主键命名为 `data_filter_id`，与 `sys_permission.data_filter_id` 外键关联保持一致。

### 3.2 表结构迁移策略

现有 `sys_data_filter` 表结构与新设计不兼容，采用以下迁移策略：

**方案：删除重建**

由于现有表无数据或数据不重要，直接删除重建：

```sql
-- 1. 备份现有表（可选）
CREATE TABLE `sys_data_filter_backup` AS SELECT * FROM `sys_data_filter`;

-- 2. 删除旧表
DROP TABLE IF EXISTS `sys_data_filter`;

-- 3. 创建新表（使用上文定义的新结构）
CREATE TABLE `sys_data_filter` (
    -- 新表结构...
);
```

### 3.3 sys_permission 表复用

现有表结构：
- `ac_type = 2` 表示数据权限（根据 `CommonConstans.PERMISSION_DATA_TYPE`）
- `data_filter_id` 关联 `sys_data_filter.data_filter_id`

### 3.4 sys_role_perm_rela 表复用

现有表结构，无需修改。通过此表建立角色与数据权限的关系。

### 3.5 错误码定义

在 `BaseErrCodeConstant` 中新增数据范围相关错误码：

```java
public interface BaseErrCodeConstant {
    // ... 现有错误码

    // 数据范围权限错误码
    String DATA_SCOPE_RULE_NOT_FOUND = "BUSS0050";
    String DATA_SCOPE_RULE_CONFIG_INVALID = "BUSS0051";
    String DATA_SCOPE_SQL_FRAGMENT_INVALID = "BUSS0052";
    String DATA_SCOPE_ENTITY_NOT_REGISTERED = "BUSS0053";
}
```

## 四、规则配置JSON格式

### 4.1 FIELD_FILTER - 字段过滤

```json
{
    "excludeFields": ["password", "salt", "id_card"],
    "includeFields": []
}
```

| 字段 | 说明 |
|------|------|
| `excludeFields` | 排除的字段列表 |
| `includeFields` | 只包含的字段（与exclude互斥） |

### 4.2 CREATOR_FILTER - 创建人过滤

```json
{
    "field": "create_by",
    "matchType": "CURRENT_USER",
    "userIds": [1, 2, 3],
    "roleIds": [1, 2]
}
```

| 字段 | 说明 |
|------|------|
| `field` | 创建人字段名 |
| `matchType` | 匹配类型，见下表 |
| `userIds` | 指定用户ID列表（matchType=USER_LIST时使用） |
| `roleIds` | 指定角色ID列表（matchType=ROLE_USER时使用） |

**matchType 取值说明：**

| 值 | 说明 |
|------|------|
| `CURRENT_USER` | 当前登录用户 |
| `USER_LIST` | 指定用户列表（userIds） |
| `ROLE_USER` | 指定角色下的所有用户（roleIds） |

### 4.3 DEPT_FILTER - 部门过滤

```json
{
    "field": "dept_id",
    "matchType": "CURRENT_DEPT",
    "deptIds": [10, 20]
}
```

| 字段 | 说明 |
|------|------|
| `field` | 部门字段名 |
| `matchType` | 匹配类型：`CURRENT_DEPT`/`DEPT_LIST`/`DEPT_AND_CHILDREN` |
| `deptIds` | 指定部门ID列表（matchType=DEPT_LIST时使用） |

### 4.4 DATE_RANGE_FILTER - 时间范围过滤

```json
{
    "field": "create_time",
    "rangeType": "RELATIVE",
    "relativeValue": -90,
    "relativeUnit": "DAY",
    "startTime": "2024-01-01",
    "endTime": "2024-12-31"
}
```

| 字段 | 说明 |
|------|------|
| `field` | 时间字段名 |
| `rangeType` | 范围类型：`RELATIVE`（相对）/ `ABSOLUTE`（绝对） |
| `relativeValue` | 相对值（负数表示过去） |
| `relativeUnit` | 单位：`DAY`/`MONTH`/`YEAR` |
| `startTime` | 绝对开始时间 |
| `endTime` | 绝对结束时间 |

### 4.5 STATUS_FILTER - 状态过滤

```json
{
    "field": "status",
    "allowedValues": [0, 1]
}
```

| 字段 | 说明 |
|------|------|
| `field` | 状态字段名 |
| `allowedValues` | 允许的状态值列表 |

### 4.6 CUSTOM_SQL - 自定义SQL片段

```json
{
    "sqlFragment": "org_id = #{currentOrgId} AND type = 1"
}
```

| 字段 | 说明 |
|------|------|
| `sqlFragment` | 自定义WHERE条件片段 |

**安全验证规则：**

为防止SQL注入，`sqlFragment` 需满足以下规则：

1. 禁止包含分号 `;`
2. 禁止包含子查询关键字 `SELECT`、`INSERT`、`UPDATE`、`DELETE`、`DROP`、`TRUNCATE`
3. 禁止包含注释符号 `--`、`/*`、`*/`
4. 允许使用占位符：`#{currentUserId}`、`#{loginName}`

```java
/**
 * 自定义SQL片段验证器
 *
 * @author binblink
 */
public class CustomSqlValidator {

    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
        ";|SELECT|INSERT|UPDATE|DELETE|DROP|TRUNCATE|--|/\\*|\\*/",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 验证SQL片段是否安全
     *
     * @param sqlFragment SQL片段
     * @throws BlinkException 包含非法字符时抛出业务异常
     */
    public static void validate(String sqlFragment) {
        if (FORBIDDEN_PATTERN.matcher(sqlFragment).find()) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_SCOPE_SQL_FRAGMENT_INVALID);
        }
    }
}
```

### 4.7 RuleConfig 配置类定义

规则配置统一使用 `RuleConfig` 类承载，支持JSON序列化：

```java
@Data
public class RuleConfig implements Serializable {

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 字段名（条件过滤使用）
     */
    private String field;

    // ========== FIELD_FILTER ==========
    private List<String> excludeFields;
    private List<String> includeFields;

    // ========== CREATOR_FILTER / DEPT_FILTER ==========
    private String matchType;
    private List<Integer> userIds;
    private List<Integer> roleIds;
    private List<Integer> deptIds;

    // ========== DATE_RANGE_FILTER ==========
    private String rangeType;
    private Integer relativeValue;
    private String relativeUnit;
    private String startTime;
    private String endTime;

    // ========== STATUS_FILTER ==========
    private List<Object> allowedValues;

    // ========== CUSTOM_SQL ==========
    private String sqlFragment;

    /**
     * 创建副本
     */
    public RuleConfig copy() {
        return BeanUtil.copyProperties(this, RuleConfig.class);
    }
}

## 五、代码架构设计

### 5.1 包结构

```
com.blink.base.datascope
├── annotation
│   └── DataScope.java                    # 标记注解（用于跳过过滤）
├── constant
│   └── DataScopeRuleType.java            # 规则类型枚举
├── entity
│   └── SysDataFilterDO.java              # 实体类（调整后）
├── dto
│   ├── req/
│   │   ├── AddDataFilterReq.java         # 新增请求DTO
│   │   ├── UpdateDataFilterReq.java      # 更新请求DTO
│   │   ├── QueryDataFilterReq.java       # 查询请求DTO（继承PageDTO）
│   │   ├── DataFilterIdReq.java          # ID请求DTO
│   │   └── GetEntityFieldsReq.java       # 获取实体字段请求DTO
│   └── rsp/
│       └── QueryDataFilterRsp.java       # 查询响应DTO（继承PageDTO<DataFilterVO>）
├── mapper
│   └── SysDataFilterMapper.java
├── service
│   ├── SysDataFilterService.java
│   └── impl/SysDataFilterServiceImpl.java
├── controller
│   └── SysDataFilterController.java
├── interceptor
│   └── DataScopeInterceptor.java         # MyBatis拦截器
├── handler
│   ├── RuleHandler.java                  # 规则处理器接口
│   ├── FieldFilterHandler.java           # 字段过滤处理器
│   ├── CreatorFilterHandler.java         # 创建人过滤处理器
│   ├── DeptFilterHandler.java            # 部门过滤处理器
│   ├── DateRangeFilterHandler.java       # 时间范围处理器
│   ├── StatusFilterHandler.java          # 状态过滤处理器
│   └── CustomSqlHandler.java             # 自定义SQL处理器
├── context
│   └── DataScopeContext.java             # 上下文（当前用户、角色等）
├── merge
│   └── RuleMergeStrategy.java            # 规则合并策略
├── cache
│   └── DataScopeCache.java               # 规则缓存
└── util
    └── DataScopeSqlUtil.java             # SQL处理工具类
```

**DTO 定义：**

```java
/**
 * 新增数据过滤规则请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class AddDataFilterReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 过滤规则名称
     */
    @NotBlank(message = "过滤规则名称不能为空")
    private String dataFilterName;

    /**
     * 过滤规则英文名称
     */
    private String dataFilterEnName;

    /**
     * 实体类全限定名
     */
    @NotBlank(message = "实体类不能为空")
    private String entityClass;

    /**
     * 对应表名
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;

    /**
     * 规则类型
     */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    /**
     * 规则配置JSON
     */
    @NotBlank(message = "规则配置不能为空")
    private String ruleConfig;

    /**
     * 备注
     */
    private String remark;
}

/**
 * 更新数据过滤规则请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class UpdateDataFilterReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据过滤ID
     */
    @NotNull(message = "数据过滤ID不能为空")
    private Integer dataFilterId;

    /**
     * 过滤规则名称
     */
    @NotBlank(message = "过滤规则名称不能为空")
    private String dataFilterName;

    /**
     * 过滤规则英文名称
     */
    private String dataFilterEnName;

    /**
     * 规则配置JSON
     */
    @NotBlank(message = "规则配置不能为空")
    private String ruleConfig;

    /**
     * 状态 0启用 1禁用
     */
    private Byte status;

    /**
     * 备注
     */
    private String remark;
}

/**
 * 查询数据过滤规则请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class QueryDataFilterReq extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 过滤规则名称（模糊查询）
     */
    private String dataFilterName;

    /**
     * 实体类全限定名
     */
    private String entityClass;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 状态 0启用 1禁用
     */
    private Byte status;
}

/**
 * 数据过滤规则响应DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class QueryDataFilterRsp extends PageDTO<DataFilterVO> implements Serializable {

    private static final long serialVersionUID = 1L;
}

/**
 * 数据过滤规则VO
 *
 * @author binblink
 */
@Getter
@Setter
public class DataFilterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据过滤ID
     */
    private Integer dataFilterId;

    /**
     * 过滤规则名称
     */
    private String dataFilterName;

    /**
     * 过滤规则英文名称
     */
    private String dataFilterEnName;

    /**
     * 实体类全限定名
     */
    private String entityClass;

    /**
     * 对应表名
     */
    private String tableName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 规则配置JSON
     */
    private String ruleConfig;

    /**
     * 状态 0启用 1禁用
     */
    private Byte status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

/**
 * 数据过滤ID请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class DataFilterIdReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据过滤ID
     */
    @NotNull(message = "数据过滤ID不能为空")
    private Integer dataFilterId;
}

/**
 * 获取实体字段请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class GetEntityFieldsReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实体类全限定名
     */
    @NotBlank(message = "实体类不能为空")
    private String entityClass;
}

/**
 * 实体字段VO
 *
 * @author binblink
 */
@Getter
@Setter
public class EntityFieldVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段名（Java属性名）
     */
    private String fieldName;

    /**
     * 列名（数据库列名）
     */
    private String columnName;

    /**
     * 字段类型
     */
    private String fieldType;
}

/**
 * 实体字段列表响应DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class EntityFieldsRsp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段列表
     */
    private List<EntityFieldVO> fields;
}
```

### 5.2 @DataScope 注解定义

```java
/**
 * 数据范围权限注解
 * 用于标记Mapper方法，控制是否启用数据过滤
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 是否启用数据过滤，默认启用
     */
    boolean enabled() default true;

    /**
     * 指定实体类（用于明确指定，不解析SQL）
     */
    Class<?> entity() default Void.class;

    /**
     * 指定表别名（用于JOIN场景）
     */
    String tableAlias() default "";
}
```

**使用示例：**

```java
// 跳过数据过滤
@DataScope(enabled = false)
List<SysUserDO> selectAllUsers();

// 指定实体和别名（用于JOIN场景）
@DataScope(entity = SysUserDO.class, tableAlias = "u")
List<UserVO> selectUserWithDept();
```

### 5.3 核心类设计

#### 5.3.1 DataScopeRuleType 枚举

```java
/**
 * 数据范围规则类型枚举
 *
 * @author binblink
 */
@Getter
@AllArgsConstructor
public enum DataScopeRuleType {

    FIELD_FILTER("字段过滤"),
    CREATOR_FILTER("创建人过滤"),
    DEPT_FILTER("部门过滤"),
    DATE_RANGE_FILTER("时间范围过滤"),
    STATUS_FILTER("状态过滤"),
    CUSTOM_SQL("自定义SQL");

    private final String description;
}
```

#### 5.3.2 RuleHandler 接口

```java
/**
 * 规则处理器接口
 * 定义数据过滤规则的处理逻辑
 *
 * @author binblink
 */
public interface RuleHandler {

    /**
     * 处理规则，修改SQL
     *
     * @param sql     原SQL
     * @param config  规则配置
     * @param context 上下文
     */
    void apply(StringBuilder sql, RuleConfig config, DataScopeContext context);

    /**
     * 获取支持的规则类型
     *
     * @return 规则类型枚举
     */
    DataScopeRuleType getRuleType();
}
```

#### 5.3.3 DataScopeInterceptor 拦截器

```java
/**
 * 数据范围权限拦截器
 * 拦截MyBatis查询，自动应用数据过滤规则
 *
 * @author binblink
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataScopeInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DataScopeInterceptor.class);

    @Resource
    private DataScopeCache dataScopeCache;

    @Resource
    private List<RuleHandler> ruleHandlers;

    @Resource
    private RedisClient redisClient;

    @Resource
    private SysUserGroupRelaMapper sysUserGroupRelaMapper;

    @Resource
    private SysUserRoleRelaMapper sysUserRoleRelaMapper;

    @Resource
    private SysGroupMapper sysGroupMapper;

    /**
     * 拦截SQL执行，应用数据过滤规则
     *
     * @param invocation 调用信息
     * @return 执行结果
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 获取StatementHandler和原始SQL
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();
        StringBuilder sqlBuilder = new StringBuilder(originalSql);

        // 2. 获取用户信息：从 BlinkRequestContextHolder 获取 userId，再从 Redis 获取 UserInfoRedisDO
        BlinkRequestContext requestContext = BlinkRequestContextHolder.getContext();
        if (requestContext == null || StrUtil.isBlank(requestContext.getUserId())) {
            return invocation.proceed();
        }

        // 3. 从 Redis 获取用户信息（使用 USER_INFO + userId）
        UserInfoRedisDO userInfo = JacksonUtil.convert(
            redisClient.get(RedisKeyConstans.USER_INFO + requestContext.getUserId()),
            UserInfoRedisDO.class
        );
        if (userInfo == null) {
            return invocation.proceed();
        }

        // 4. 快速判断：超管跳过所有过滤
        if (CommonConstans.SUPER_ADMIN_YES.equals(userInfo.getSuperFlag())) {
            return invocation.proceed();
        }

        // 5. 获取Mapper方法上的@DataScope注解
        DataScope dataScope = getDataScopeAnnotation(invocation);
        if (dataScope != null && !dataScope.enabled()) {
            return invocation.proceed();
        }

        // 6. 解析SQL获取表名
        Set<String> tableNames = DataScopeSqlUtil.extractTableNames(originalSql);

        // 7. 根据表数量决定处理策略
        Class<?> entityClass = null;
        String tableAlias = null;

        if (tableNames.size() > 1) {
            // JOIN查询：必须有@DataScope注解指定实体和别名
            if (dataScope == null || dataScope.entity() == Void.class) {
                log.warn("JOIN查询未指定@DataScope注解，跳过数据过滤: {}", originalSql);
                return invocation.proceed();
            }
            entityClass = dataScope.entity();
            tableAlias = dataScope.tableAlias();
        } else if (tableNames.size() == 1) {
            // 单表查询：自动识别
            String tableName = tableNames.iterator().next();
            entityClass = DataScopeEntityScanner.getEntityClass(tableName);
        } else {
            // 无表名（异常情况），跳过
            return invocation.proceed();
        }

        // 8. 实体类未注册，跳过过滤
        if (entityClass == null) {
            log.debug("实体类未注册映射，跳过数据过滤: {}", tableNames);
            return invocation.proceed();
        }

        // 9. 获取当前用户角色列表
        List<Integer> roleIds = userInfo.getRoleIds();
        if (CollUtil.isEmpty(roleIds)) {
            return invocation.proceed();
        }

        // 10. 获取并合并过滤规则
        List<RuleConfig> mergedRules = dataScopeCache.getMergedRules(roleIds, entityClass.getName());
        if (CollUtil.isEmpty(mergedRules)) {
            return invocation.proceed();
        }

        // 11. 构建上下文（注入所需的 Mapper）
        DataScopeContext dsContext = new DataScopeContext(
            userInfo, tableAlias,
            sysUserGroupRelaMapper, sysUserRoleRelaMapper, sysGroupMapper
        );

        // 12. 依次执行规则处理器修改SQL
        for (RuleConfig rule : mergedRules) {
            RuleHandler handler = getHandler(rule.getRuleType());
            if (handler != null) {
                handler.apply(sqlBuilder, rule, dsContext);
            }
        }

        // 13. 替换原SQL
        if (!sqlBuilder.toString().equals(originalSql)) {
            ReflectUtil.setFieldValue(boundSql, "sql", sqlBuilder.toString());
        }

        return invocation.proceed();
    }

    /**
     * 获取指定规则类型的处理器
     *
     * @param ruleType 规则类型
     * @return 处理器，未找到返回null
     */
    private RuleHandler getHandler(String ruleType) {
        RuleHandler handler = ruleHandlers.stream()
            .filter(h -> h.getRuleType().name().equals(ruleType))
            .findFirst()
            .orElse(null);

        if (handler == null) {
            log.warn("未找到规则类型 [{}] 对应的处理器，请检查是否已实现该处理器", ruleType);
        }

        return handler;
    }

    /**
     * 获取Mapper方法上的@DataScope注解
     *
     * @param invocation 调用信息
     * @return DataScope注解，未找到返回null
     */
    private DataScope getDataScopeAnnotation(Invocation invocation) {
        try {
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
            MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

            // 获取Mapper类名和方法名
            String id = mappedStatement.getId();
            String className = id.substring(0, id.lastIndexOf("."));
            String methodName = id.substring(id.lastIndexOf(".") + 1);

            // 通过反射获取方法上的注解
            Class<?> mapperClass = Class.forName(className);
            for (Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    return method.getAnnotation(DataScope.class);
                }
            }
        } catch (Exception e) {
            log.debug("获取@DataScope注解失败: {}", e.getMessage());
        }
        return null;
    }
}
```

### 5.4 用户上下文获取

**设计原则：** 复用现有的 `UserInfoRedisDO` 结构，通过 Redis 获取用户信息，避免修改 `BlinkRequestContext`。

**获取流程：**

1. 从 `BlinkRequestContextHolder` 获取 `userId`
2. 通过 `userId` 从 Redis 获取 `UserInfoRedisDO`（使用 `USER_INFO + userId` 键）
3. `UserInfoRedisDO` 已包含 `superFlag` 和 `roleIds`

**依赖关系：**

`UserInfoRedisDO` 结构（位于 `blink-framework-common`）：

```java
public class UserInfoRedisDO implements Serializable {
    private Integer userId;
    private String loginName;
    private String username;
    private LocalDateTime loginDateTime;
    private String token;
    private Set<String> permissions;
    private Integer superFlag;        // 超级管理员标志
    private List<Integer> roleIds;    // 角色ID列表
}
```

**注意：** 超级管理员在查询时直接跳过所有数据过滤规则，返回全部数据。

### 5.4.1 DataScopeContext 上下文类

`DataScopeContext` 作为数据过滤的上下文对象，需要访问数据库查询用户-部门关系等数据。
采用**构造函数注入**方式接收必要的 Mapper 依赖。

```java
/**
 * 数据范围上下文
 * 封装当前用户的上下文信息，用于规则处理
 *
 * @author binblink
 */
@Getter
public class DataScopeContext {

    /**
     * 用户信息（从Redis获取）
     */
    private final UserInfoRedisDO userInfo;

    /**
     * 表别名
     */
    private final String tableAlias;

    /**
     * 用户-组织关联Mapper
     */
    private final SysUserGroupRelaMapper sysUserGroupRelaMapper;

    /**
     * 用户-角色关联Mapper
     */
    private final SysUserRoleRelaMapper sysUserRoleRelaMapper;

    /**
     * 组织Mapper
     */
    private final SysGroupMapper sysGroupMapper;

    /**
     * 构造函数
     *
     * @param userInfo                 用户信息
     * @param tableAlias               表别名
     * @param sysUserGroupRelaMapper   用户-组织关联Mapper
     * @param sysUserRoleRelaMapper    用户-角色关联Mapper
     * @param sysGroupMapper           组织Mapper
     */
    public DataScopeContext(UserInfoRedisDO userInfo, String tableAlias,
                            SysUserGroupRelaMapper sysUserGroupRelaMapper,
                            SysUserRoleRelaMapper sysUserRoleRelaMapper,
                            SysGroupMapper sysGroupMapper) {
        this.userInfo = userInfo;
        this.tableAlias = tableAlias;
        this.sysUserGroupRelaMapper = sysUserGroupRelaMapper;
        this.sysUserRoleRelaMapper = sysUserRoleRelaMapper;
        this.sysGroupMapper = sysGroupMapper;
    }

    /**
     * 判断当前用户是否为超级管理员
     *
     * @return true=超管，跳过过滤
     */
    public boolean isSuperAdmin() {
        return CommonConstans.SUPER_ADMIN_YES.equals(userInfo.getSuperFlag());
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public Integer getUserId() {
        return userInfo.getUserId();
    }

    /**
     * 获取当前用户登录名
     *
     * @return 登录名
     */
    public String getLoginName() {
        return userInfo.getLoginName();
    }

    /**
     * 获取当前用户角色ID列表
     *
     * @return 角色ID列表
     */
    public List<Integer> getRoleIds() {
        return userInfo.getRoleIds();
    }

    /**
     * 获取表别名
     *
     * @return 表别名
     */
    public String getTableAlias() {
        return tableAlias;
    }

    /**
     * 获取当前用户所属部门ID
     *
     * @return 部门ID
     */
    public Integer getCurrentDeptId() {
        return sysUserGroupRelaMapper.selectDeptIdByUserId(getUserId());
    }

    /**
     * 根据角色ID列表获取用户ID列表
     *
     * @param roleIds 角色ID列表
     * @return 用户ID列表
     */
    public List<Integer> getUserIdsByRoleIds(List<Integer> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return sysUserRoleRelaMapper.selectUserIdsByRoleIds(roleIds);
    }

    /**
     * 获取部门及其所有子部门ID列表
     *
     * @param deptId 部门ID
     * @return 部门ID列表（包含子部门）
     */
    public List<Integer> getDeptAndChildren(Integer deptId) {
        return sysGroupMapper.selectDeptAndChildrenById(deptId);
    }
}

### 5.5 表名-实体映射机制

启动时扫描所有带 `@TableName` 注解的实体类，建立表名到实体类的映射：

```java
/**
 * 数据范围实体扫描器
 * 启动时扫描实体类，建立表名到实体类的映射
 *
 * @author binblink
 */
@Component
public class DataScopeEntityScanner implements ApplicationRunner {

    private static final Map<String, Class<?>> TABLE_ENTITY_MAP = new ConcurrentHashMap<>();

    /**
     * 应用启动时执行扫描
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        Set<Class<?>> entityClasses = ClassUtil.scanPackageByAnnotation(
            "com.blink.base.entity", TableName.class
        );

        for (Class<?> clazz : entityClasses) {
            TableName tableName = clazz.getAnnotation(TableName.class);
            TABLE_ENTITY_MAP.put(tableName.value(), clazz);
        }
    }

    /**
     * 根据表名获取实体类
     *
     * @param tableName 表名
     * @return 实体类，未找到返回null
     */
    public static Class<?> getEntityClass(String tableName) {
        return TABLE_ENTITY_MAP.get(tableName);
    }
}
```

### 5.6 JOIN查询处理策略

**范围限制：** 第一期仅支持单表查询的自动过滤。JOIN查询需要通过 `@DataScope` 注解显式指定。

**处理逻辑：**

```java
/**
 * JOIN查询处理逻辑（DataScopeInterceptor内部方法）
 *
 * @param invocation 调用信息
 * @return 执行结果
 */
public Object intercept(Invocation invocation) throws Throwable {
    // 获取Mapper方法上的@DataScope注解
    DataScope dataScope = getDataScopeAnnotation(invocation);

    if (dataScope != null && !dataScope.enabled()) {
        // 注解明确禁用过滤
        return invocation.proceed();
    }

    // 解析SQL中的表
    Set<String> tableNames = extractTableNames(sql);

    if (tableNames.size() > 1) {
        // JOIN查询：必须有@DataScope注解指定实体和别名
        if (dataScope == null || dataScope.entity() == Void.class) {
            // 无注解或未指定实体，跳过过滤（记录警告日志）
            log.warn("JOIN查询未指定@DataScope注解，跳过数据过滤: {}", sql);
            return invocation.proceed();
        }
        // 使用注解指定的实体和别名
        applyRules(sql, dataScope.entity(), dataScope.tableAlias());
    } else {
        // 单表查询：自动识别
        String tableName = tableNames.iterator().next();
        Class<?> entityClass = DataScopeEntityScanner.getEntityClass(tableName);
        applyRules(sql, entityClass, null);
    }
}
```

### 5.7 规则缓存设计

**Redis Key 常量定义：**

在 `RedisKeyConstant` 中新增：

```java
public interface RedisKeyConstant {
    // ... 现有常量

    // 数据范围权限缓存
    String DATA_SCOPE_ROLE = "blink:data_scope:role:";
}
```

**缓存结构：**

```
Redis Key: blink:data_scope:role:{roleId}
Value: Map<entityClass, List<RuleConfig>>
过期时间: 30分钟
```

**缓存更新时机：**

1. 角色权限变更时（分配/取消数据权限）
2. 数据权限规则变更时（增删改）
3. 手动刷新缓存接口

**DataScopeCache 实现：**

```java
/**
 * 数据范围权限缓存服务
 * 提供角色规则的缓存获取和合并
 *
 * @author binblink
 */
@Service
public class DataScopeCache {

    private static final Logger log = LoggerFactory.getLogger(DataScopeCache.class);

    @Resource
    private StringRedisTemplate redisTemplate;

    @Resource
    private SysRolePermRelaMapper sysRolePermRelaMapper;

    @Resource
    private SysDataFilterMapper sysDataFilterMapper;

    @Resource
    private RuleMergeStrategy ruleMergeStrategy;

    private static final long CACHE_EXPIRE_MINUTES = 30;

    /**
     * 获取角色的过滤规则列表
     *
     * @param roleId      角色ID
     * @param entityClass 实体类名
     * @return 规则配置列表
     */
    public List<RuleConfig> getRoleRules(Integer roleId, String entityClass) {
        String cacheKey = RedisKeyConstant.DATA_SCOPE_ROLE + roleId;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (StrUtil.isNotBlank(cachedValue)) {
            // 从缓存获取
            Map<String, List<RuleConfig>> roleRules = JSON.parseObject(cachedValue,
                new TypeReference<Map<String, List<RuleConfig>>>() {});
            return roleRules.get(entityClass);
        }

        // 缓存未命中，从数据库加载
        Map<String, List<RuleConfig>> allRules = loadRoleRulesFromDb(roleId);
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(allRules),
            CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        return allRules.get(entityClass);
    }

    /**
     * 获取并合并多角色的过滤规则
     *
     * @param roleIds     角色ID列表
     * @param entityClass 实体类名
     * @return 合并后的规则列表
     */
    public List<RuleConfig> getMergedRules(List<Integer> roleIds, String entityClass) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }

        // 收集所有角色的规则
        Map<String, List<RuleConfig>> rulesByType = new HashMap<>();

        for (Integer roleId : roleIds) {
            List<RuleConfig> roleRules = getRoleRules(roleId, entityClass);
            if (CollUtil.isEmpty(roleRules)) {
                continue;
            }

            for (RuleConfig rule : roleRules) {
                rulesByType.computeIfAbsent(rule.getRuleType(), k -> new ArrayList<>())
                    .add(rule);
            }
        }

        // 按类型合并规则
        List<RuleConfig> mergedRules = new ArrayList<>();
        for (List<RuleConfig> sameTypeRules : rulesByType.values()) {
            if (sameTypeRules.size() == 1) {
                mergedRules.add(sameTypeRules.get(0));
            } else {
                mergedRules.add(ruleMergeStrategy.merge(sameTypeRules));
            }
        }

        return mergedRules;
    }

    /**
     * 从数据库加载角色的过滤规则
     *
     * @param roleId 角色ID
     * @return 实体类 -> 规则列表映射
     */
    private Map<String, List<RuleConfig>> loadRoleRulesFromDb(Integer roleId) {
        Map<String, List<RuleConfig>> result = new HashMap<>();

        // 查询角色的数据权限ID列表
        List<Integer> dataFilterIds = sysRolePermRelaMapper.selectDataFilterIdsByRoleId(roleId);
        if (CollUtil.isEmpty(dataFilterIds)) {
            return result;
        }

        // 查询过滤规则
        List<SysDataFilterDO> filters = sysDataFilterMapper.selectBatchIds(dataFilterIds);
        for (SysDataFilterDO filter : filters) {
            if (filter.getStatus() != 0) {
                continue;  // 跳过禁用的规则
            }

            RuleConfig config = JSON.parseObject(filter.getRuleConfig(), RuleConfig.class);
            config.setRuleType(filter.getRuleType());

            result.computeIfAbsent(filter.getEntityClass(), k -> new ArrayList<>())
                .add(config);
        }

        return result;
    }

    /**
     * 清除角色缓存
     *
     * @param roleId 角色ID
     */
    public void clearRoleCache(Integer roleId) {
        String cacheKey = RedisKeyConstant.DATA_SCOPE_ROLE + roleId;
        redisTemplate.delete(cacheKey);
        log.info("已清除角色 [{}] 的数据权限缓存", roleId);
    }

    /**
     * 清除所有数据权限缓存
     */
    public void clearAllCache() {
        Set<String> keys = redisTemplate.keys(RedisKeyConstant.DATA_SCOPE_ROLE + "*");
        if (CollUtil.isNotEmpty(keys)) {
            redisTemplate.delete(keys);
            log.info("已清除所有数据权限缓存，共 {} 个角色", keys.size());
        }
    }
}
```

### 5.8 规则合并策略详细实现

**合并原则：** 同类型规则取并集（最宽松）

```java
/**
 * 规则合并策略
 * 处理多角色场景下的规则合并逻辑
 *
 * @author binblink
 */
public class RuleMergeStrategy {

    /**
     * 合并多个角色的规则配置
     *
     * @param roleRules 每个角色的规则列表
     * @return 合并后的规则列表
     */
    public static List<RuleConfig> merge(List<List<RuleConfig>> roleRules) {
        Map<String, RuleConfig> mergedMap = new HashMap<>();

        for (List<RuleConfig> rules : roleRules) {
            for (RuleConfig rule : rules) {
                String key = rule.getRuleType() + "_" + rule.getField();
                RuleConfig existing = mergedMap.get(key);

                if (existing == null) {
                    mergedMap.put(key, rule.copy());
                } else {
                    mergeSameTypeRule(existing, rule);
                }
            }
        }

        return new ArrayList<>(mergedMap.values());
    }

    /**
     * 合并同类型规则（并集）
     *
     * @param target 目标规则配置
     * @param source 源规则配置
     */
    private static void mergeSameTypeRule(RuleConfig target, RuleConfig source) {
        switch (target.getRuleType()) {
            case FIELD_FILTER:
                // 字段过滤：取排除字段的交集（更宽松）
                Set<String> excludeFields = new HashSet<>(target.getExcludeFields());
                excludeFields.retainAll(source.getExcludeFields());
                target.setExcludeFields(new ArrayList<>(excludeFields));
                break;

            case CREATOR_FILTER:
                // 创建人过滤：合并用户列表
                if (!target.getMatchType().equals(source.getMatchType())) {
                    // matchType不同，转换为USER_LIST模式合并
                    target.setMatchType("USER_LIST");
                    Set<Integer> userIds = new HashSet<>(target.getUserIds());
                    userIds.addAll(source.getUserIds());
                    target.setUserIds(new ArrayList<>(userIds));
                } else if ("USER_LIST".equals(target.getMatchType())) {
                    Set<Integer> userIds = new HashSet<>(target.getUserIds());
                    userIds.addAll(source.getUserIds());
                    target.setUserIds(new ArrayList<>(userIds));
                }
                break;

            case DEPT_FILTER:
                // 部门过滤：合并部门列表
                if (!target.getMatchType().equals(source.getMatchType())) {
                    target.setMatchType("DEPT_LIST");
                    Set<Integer> deptIds = new HashSet<>(target.getDeptIds());
                    deptIds.addAll(source.getDeptIds());
                    target.setDeptIds(new ArrayList<>(deptIds));
                } else if ("DEPT_LIST".equals(target.getMatchType())) {
                    Set<Integer> deptIds = new HashSet<>(target.getDeptIds());
                    deptIds.addAll(source.getDeptIds());
                    target.setDeptIds(new ArrayList<>(deptIds));
                }
                break;

            case STATUS_FILTER:
                // 状态过滤：合并允许的状态值
                Set<Object> allowedValues = new HashSet<>(target.getAllowedValues());
                allowedValues.addAll(source.getAllowedValues());
                target.setAllowedValues(new ArrayList<>(allowedValues));
                break;

            case DATE_RANGE_FILTER:
                // 时间范围：取更大的范围
                mergeDateRange(target, source);
                break;

            case CUSTOM_SQL:
                // 自定义SQL：用OR连接
                target.setSqlFragment("(" + target.getSqlFragment() + " OR " + source.getSqlFragment() + ")");
                break;
        }
    }

    /**
     * 合并时间范围规则，取更大的范围（更宽松）
     *
     * @param target 目标规则配置
     * @param source 源规则配置
     */
    private void mergeDateRange(RuleConfig target, RuleConfig source) {
        // 相对时间范围：取较大的相对值
        if ("RELATIVE".equals(target.getRangeType()) && "RELATIVE".equals(source.getRangeType())) {
            // 相同单位时取较大的值
            if (target.getRelativeUnit().equals(source.getRelativeUnit())) {
                target.setRelativeValue(Math.max(target.getRelativeValue(), source.getRelativeValue()));
            } else {
                // 不同单位时转换为天数比较
                int targetDays = convertToDays(target.getRelativeValue(), target.getRelativeUnit());
                int sourceDays = convertToDays(source.getRelativeValue(), source.getRelativeUnit());
                if (sourceDays > targetDays) {
                    target.setRelativeValue(source.getRelativeValue());
                    target.setRelativeUnit(source.getRelativeUnit());
                }
            }
        } else {
            // 绝对时间范围或有混合类型：合并日期范围
            LocalDateTime targetStart = target.getStartDate();
            LocalDateTime targetEnd = target.getEndDate();
            LocalDateTime sourceStart = source.getStartDate();
            LocalDateTime sourceEnd = source.getEndDate();

            if (sourceStart != null && (targetStart == null || sourceStart.isBefore(targetStart))) {
                target.setStartDate(sourceStart);
            }
            if (sourceEnd != null && (targetEnd == null || sourceEnd.isAfter(targetEnd))) {
                target.setEndDate(sourceEnd);
            }
        }
    }

    /**
     * 将相对时间转换为天数
     *
     * @param value 相对值
     * @param unit  单位（DAY/WEEK/MONTH/YEAR）
     * @return 天数
     */
    private int convertToDays(int value, String unit) {
        return switch (unit) {
            case "DAY" -> value;
            case "WEEK" -> value * 7;
            case "MONTH" -> value * 30;
            case "YEAR" -> value * 365;
            default -> value;
        };
    }
}
```

### 5.9 SQL处理工具类

所有SQL处理相关的辅助方法统一放在 `DataScopeSqlUtil` 工具类中：

```java
/**
 * 数据范围SQL处理工具类
 * 提供SQL解析、字段过滤、条件追加等通用方法
 *
 * @author binblink
 */
public class DataScopeSqlUtil {

    /**
     * 从SQL中提取表名集合
     *
     * @param sql SQL语句
     * @return 表名集合
     */
    public static Set<String> extractTableNames(String sql) {
        Set<String> tableNames = new HashSet<>();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder() {
                @Override
                public void visit(Table tableName) {
                    tableNames.add(tableName.getName());
                }
            };
            tablesNamesFinder.getTableList(statement);
        } catch (JSQLParserException e) {
            log.warn("SQL解析失败: {}", e.getMessage());
        }
        return tableNames;
    }

    /**
     * 提取SELECT部分
     *
     * @param sql 完整SQL
     * @return SELECT字段部分字符串
     */
    public static String extractSelectPart(String sql) {
        try {
            Select select = (Select) CCJSqlParserUtil.parse(sql);
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
            return plainSelect.getSelectItems().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        } catch (JSQLParserException e) {
            log.warn("提取SELECT部分失败: {}", e.getMessage());
            return "*";
        }
    }

    /**
     * 提取FROM及之后部分
     *
     * @param sql 完整SQL
     * @return FROM及之后的字符串
     */
    public static String extractFromPart(String sql) {
        int fromIndex = sql.toUpperCase().indexOf(" FROM ");
        if (fromIndex > 0) {
            return sql.substring(fromIndex);
        }
        return "";
    }

    /**
     * 过滤SELECT字段，排除指定字段
     *
     * @param selectPart    SELECT部分字符串
     * @param excludeFields 需要排除的字段列表
     * @param tableAlias    表别名（可为空）
     * @return 过滤后的SELECT字段字符串
     */
    public static String filterFields(String selectPart, List<String> excludeFields, String tableAlias) {
        if (CollUtil.isEmpty(excludeFields)) {
            return selectPart;
        }

        List<String> originalFields = parseSelectFields(selectPart);
        List<String> filteredFields = new ArrayList<>();

        for (String field : originalFields) {
            String fieldName = extractFieldName(field);
            String fullName = StrUtil.isNotBlank(tableAlias)
                ? tableAlias + "." + fieldName
                : fieldName;

            if (!excludeFields.contains(fieldName) && !excludeFields.contains(fullName)) {
                filteredFields.add(field);
            }
        }

        return String.join(", ", filteredFields);
    }

    /**
     * 解析SELECT字段列表
     *
     * @param selectPart SELECT部分字符串
     * @return 字段列表
     */
    public static List<String> parseSelectFields(String selectPart) {
        if (StrUtil.isBlank(selectPart) || "*".equals(selectPart.trim())) {
            return Collections.singletonList("*");
        }

        return Arrays.stream(selectPart.split(","))
            .map(String::trim)
            .collect(Collectors.toList());
    }

    /**
     * 从字段表达式中提取字段名
     *
     * @param fieldExpression 字段表达式（如 "u.user_id", "user_id AS id", "COUNT(*)"）
     * @return 字段名
     */
    public static String extractFieldName(String fieldExpression) {
        String trimmed = fieldExpression.trim();

        // 处理别名：user_id AS id -> user_id
        if (trimmed.toUpperCase().contains(" AS ")) {
            trimmed = trimmed.split("(?i) AS ")[0].trim();
        }

        // 处理表别名：u.user_id -> user_id
        if (trimmed.contains(".")) {
            trimmed = trimmed.substring(trimmed.lastIndexOf(".") + 1);
        }

        return trimmed;
    }

    /**
     * 追加WHERE条件到SQL中
     *
     * @param sql       SQL字符串
     * @param condition 条件表达式
     */
    public static void appendWhereCondition(StringBuilder sql, String condition) {
        String upperSql = sql.toString().toUpperCase();

        if (upperSql.contains(" WHERE ")) {
            // 已有WHERE，追加AND
            int insertIndex = findWhereInsertPosition(sql);
            sql.insert(insertIndex, " AND " + condition);
        } else {
            // 无WHERE，添加WHERE
            int insertIndex = findFromEndPosition(sql);
            sql.insert(insertIndex, " WHERE " + condition);
        }
    }

    /**
     * 查找WHERE条件插入位置
     */
    private static int findWhereInsertPosition(StringBuilder sql) {
        String upperSql = sql.toString().toUpperCase();
        int groupByIndex = upperSql.indexOf(" GROUP BY ");
        int havingIndex = upperSql.indexOf(" HAVING ");
        int orderByIndex = upperSql.indexOf(" ORDER BY ");
        int limitIndex = upperSql.indexOf(" LIMIT ");

        int minIndex = sql.length();
        if (groupByIndex > 0) { minIndex = Math.min(minIndex, groupByIndex); }
        if (havingIndex > 0) { minIndex = Math.min(minIndex, havingIndex); }
        if (orderByIndex > 0) { minIndex = Math.min(minIndex, orderByIndex); }
        if (limitIndex > 0) { minIndex = Math.min(minIndex, limitIndex); }

        return minIndex;
    }

    /**
     * 查找FROM子句结束位置
     */
    private static int findFromEndPosition(StringBuilder sql) {
        String upperSql = sql.toString().toUpperCase();
        int groupByIndex = upperSql.indexOf(" GROUP BY ");
        int orderByIndex = upperSql.indexOf(" ORDER BY ");
        int limitIndex = upperSql.indexOf(" LIMIT ");

        int minIndex = sql.length();
        if (groupByIndex > 0) { minIndex = Math.min(minIndex, groupByIndex); }
        if (orderByIndex > 0) { minIndex = Math.min(minIndex, orderByIndex); }
        if (limitIndex > 0) { minIndex = Math.min(minIndex, limitIndex); }

        return minIndex;
    }
}
```

**说明：** 该工具类提供SQL解析和修改的通用方法，所有 `RuleHandler` 实现类都应调用此工具类的方法，而非自行实现。

## 六、SQL修改实现

### 6.1 字段过滤处理

```java
/**
 * 字段过滤处理器
 * 从SELECT中排除指定字段
 *
 * @author binblink
 */
@Component
public class FieldFilterHandler implements RuleHandler {

    /**
     * 应用字段过滤规则
     *
     * @param sql     原SQL
     * @param config  规则配置
     * @param context 上下文
     */
    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeContext context) {
        List<String> excludeFields = config.getExcludeFields();
        if (CollUtil.isEmpty(excludeFields)) {
            return;
        }

        // 解析原SQL的SELECT字段
        String originalSql = sql.toString();
        String selectPart = DataScopeSqlUtil.extractSelectPart(originalSql);
        String fromPart = DataScopeSqlUtil.extractFromPart(originalSql);

        // 过滤掉排除的字段
        String newSelectPart = DataScopeSqlUtil.filterFields(
            selectPart, excludeFields, context.getTableAlias()
        );

        // 重构SQL
        sql.setLength(0);
        sql.append("SELECT ").append(newSelectPart).append(" ").append(fromPart);
    }

    @Override
    public DataScopeRuleType getRuleType() {
        return DataScopeRuleType.FIELD_FILTER;
    }
}
```

### 6.2 条件过滤处理

```java
/**
 * 创建人过滤处理器
 * 根据创建人条件过滤数据
 *
 * @author binblink
 */
@Component
public class CreatorFilterHandler implements RuleHandler {

    /**
     * 应用创建人过滤规则
     *
     * @param sql     原SQL
     * @param config  规则配置
     * @param context 上下文
     */
    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeContext context) {
        String field = config.getField();
        String matchType = config.getMatchType();
        String tableAlias = context.getTableAlias();

        StringBuilder condition = new StringBuilder();
        String column = StrUtil.isNotBlank(tableAlias)
            ? tableAlias + "." + field
            : field;

        switch (matchType) {
            case "CURRENT_USER":
                condition.append(column).append(" = '").append(context.getLoginName()).append("'");
                break;
            case "USER_LIST":
                List<Integer> userIds = config.getUserIds();
                condition.append(column).append(" IN (")
                    .append(CollUtil.join(userIds, ",")).append(")");
                break;
            case "ROLE_USER":
                // 根据角色ID查询用户ID列表
                List<Integer> roleIds = config.getRoleIds();
                List<Integer> usersByRole = context.getUserIdsByRoleIds(roleIds);
                condition.append(column).append(" IN (")
                    .append(CollUtil.join(usersByRole, ",")).append(")");
                break;
        }

        // 使用工具类追加WHERE条件
        DataScopeSqlUtil.appendWhereCondition(sql, condition.toString());
    }

    @Override
    public DataScopeRuleType getRuleType() {
        return DataScopeRuleType.CREATOR_FILTER;
    }
}
```

### 6.3 其他条件过滤处理器

其余条件过滤处理器遵循相同的模式，使用 `DataScopeSqlUtil.appendWhereCondition()` 追加条件。

**DeptFilterHandler（部门过滤处理器）：**

```java
@Component
public class DeptFilterHandler implements RuleHandler {

    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeContext context) {
        String field = config.getField();
        String matchType = config.getMatchType();
        String tableAlias = context.getTableAlias();

        String column = StrUtil.isNotBlank(tableAlias) ? tableAlias + "." + field : field;
        StringBuilder condition = new StringBuilder();

        switch (matchType) {
            case "CURRENT_DEPT":
                condition.append(column).append(" = ").append(context.getCurrentDeptId());
                break;
            case "DEPT_LIST":
                condition.append(column).append(" IN (")
                    .append(CollUtil.join(config.getDeptIds(), ",")).append(")");
                break;
            case "DEPT_AND_CHILDREN":
                List<Integer> deptIds = context.getDeptAndChildren(context.getCurrentDeptId());
                condition.append(column).append(" IN (")
                    .append(CollUtil.join(deptIds, ",")).append(")");
                break;
        }

        DataScopeSqlUtil.appendWhereCondition(sql, condition.toString());
    }

    @Override
    public DataScopeRuleType getRuleType() {
        return DataScopeRuleType.DEPT_FILTER;
    }
}
```

**DateRangeFilterHandler（时间范围过滤处理器）：**

```java
/**
 * 时间范围过滤处理器
 * 根据配置的时间范围过滤数据
 *
 * @author binblink
 */
@Component
public class DateRangeFilterHandler implements RuleHandler {

    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeContext context) {
        String field = config.getField();
        String tableAlias = context.getTableAlias();
        String column = StrUtil.isNotBlank(tableAlias) ? tableAlias + "." + field : field;

        StringBuilder condition = new StringBuilder();

        if ("RELATIVE".equals(config.getRangeType())) {
            // 相对时间范围
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(endTime, config.getRelativeValue(), config.getRelativeUnit());
            condition.append(column).append(" BETWEEN '")
                .append(startTime).append("' AND '").append(endTime).append("'");
        } else {
            // 绝对时间范围
            condition.append(column).append(" BETWEEN '")
                .append(config.getStartTime()).append("' AND '").append(config.getEndTime()).append("'");
        }

        DataScopeSqlUtil.appendWhereCondition(sql, condition.toString());
    }

    /**
     * 根据相对值计算起始时间
     *
     * @param endTime 结束时间（当前时间）
     * @param value   相对值
     * @param unit    单位（DAY/WEEK/MONTH/YEAR）
     * @return 起始时间
     */
    private LocalDateTime calculateStartTime(LocalDateTime endTime, int value, String unit) {
        return switch (unit) {
            case "DAY" -> endTime.minusDays(value);
            case "WEEK" -> endTime.minusWeeks(value);
            case "MONTH" -> endTime.minusMonths(value);
            case "YEAR" -> endTime.minusYears(value);
            default -> endTime.minusDays(value);
        };
    }

    @Override
    public DataScopeRuleType getRuleType() {
        return DataScopeRuleType.DATE_RANGE_FILTER;
    }
}
```

**StatusFilterHandler（状态过滤处理器）：**

```java
@Component
public class StatusFilterHandler implements RuleHandler {

    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeContext context) {
        String field = config.getField();
        String tableAlias = context.getTableAlias();
        String column = StrUtil.isNotBlank(tableAlias) ? tableAlias + "." + field : field;

        String condition = column + " IN (" + CollUtil.join(config.getAllowedValues(), ",") + ")";
        DataScopeSqlUtil.appendWhereCondition(sql, condition);
    }

    @Override
    public DataScopeRuleType getRuleType() {
        return DataScopeRuleType.STATUS_FILTER;
    }
}
```

**CustomSqlHandler（自定义SQL处理器）：**

```java
@Component
public class CustomSqlHandler implements RuleHandler {

    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeContext context) {
        // 验证SQL片段安全性
        CustomSqlValidator.validate(config.getSqlFragment());

        // 替换占位符
        String sqlFragment = config.getSqlFragment()
            .replace("#{currentUserId}", String.valueOf(context.getUserId()))
            .replace("#{loginName}", context.getLoginName());

        DataScopeSqlUtil.appendWhereCondition(sql, sqlFragment);
    }

    @Override
    public DataScopeRuleType getRuleType() {
        return DataScopeRuleType.CUSTOM_SQL;
    }
}
```

## 七、前端管理界面设计

### 7.1 数据权限管理页面

**菜单路径：** 系统管理 → 权限管理 → 数据权限

**页面功能：**

- 列表展示所有数据权限过滤规则
- 支持按规则名称、规则类型、状态搜索
- 支持新增、编辑、删除操作

**列表字段：**

| 字段 | 说明 |
|------|------|
| 规则名称 | 过滤规则名称 |
| 实体类 | 关联的实体类 |
| 规则类型 | 字段过滤/创建人过滤/部门过滤等 |
| 状态 | 启用/禁用 |
| 操作 | 编辑、删除 |

### 7.2 新增/编辑表单

**表单字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| 规则名称 | 输入框 | 中文名称 |
| 英文名称 | 输入框 | 英文名称 |
| 实体类 | 下拉框 | 选择已注册的实体类 |
| 规则类型 | 下拉框 | 选择规则类型 |
| 规则配置 | 动态表单 | 根据规则类型显示不同配置项 |
| 状态 | 单选 | 启用/禁用 |
| 备注 | 文本域 | 备注信息 |

**动态配置表单：**

- **字段过滤**：多选框，选择要排除的字段
- **创建人过滤**：下拉选择匹配类型，可选填用户列表
- **部门过滤**：下拉选择匹配类型，可选择部门
- **时间范围过滤**：下拉选择范围类型，输入相对值或选择绝对时间
- **状态过滤**：多选框，选择允许的状态值
- **自定义SQL**：文本域，输入SQL片段

### 7.3 权限分配集成

在现有角色管理的"分配权限"功能中，数据权限作为 `ac_type = 2` 的权限项展示，与菜单权限、接口权限并列分配。

### 7.4 后端 Controller 定义

**文件位置：** `com.blink.controller.com.blink.base.SysDataFilterController`

```java
/**
 * 数据权限过滤规则控制器
 *
 * @author binblink
 */
@RestController
@RequestMapping("/sysDataFilter")
public class SysDataFilterController {

    @Resource
    private SysDataFilterService sysDataFilterService;

    /**
     * 查询数据过滤规则列表（分页）
     *
     * @param reqDto 请求参数
     * @return 分页结果
     */
    @PostMapping("/getDataFilterList")
    public ResponseDTO<QueryDataFilterRsp> getDataFilterList(
            @RequestBody @Validated RequestDTO<QueryDataFilterReq> reqDto) {
        QueryDataFilterRsp rsp = sysDataFilterService.getDataFilterList(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 新增数据过滤规则
     *
     * @param reqDto 请求参数
     * @return 空响应
     */
    @PostMapping("/addDataFilter")
    public ResponseDTO<EmptyBody> addDataFilter(
            @RequestBody @Validated RequestDTO<AddDataFilterReq> reqDto) {
        sysDataFilterService.addDataFilter(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新数据过滤规则
     *
     * @param reqDto 请求参数
     * @return 空响应
     */
    @PostMapping("/updateDataFilter")
    public ResponseDTO<EmptyBody> updateDataFilter(
            @RequestBody @Validated RequestDTO<UpdateDataFilterReq> reqDto) {
        sysDataFilterService.updateDataFilter(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 删除数据过滤规则
     *
     * @param reqDto 请求参数（body中包含dataFilterId）
     * @return 空响应
     */
    @PostMapping("/deleteDataFilter")
    public ResponseDTO<EmptyBody> deleteDataFilter(
            @RequestBody RequestDTO<DataFilterIdReq> reqDto) {
        sysDataFilterService.deleteDataFilter(reqDto.getBody().getDataFilterId());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 获取实体类的字段列表
     *
     * @param reqDto 请求参数
     * @return 字段列表
     */
    @PostMapping("/getEntityFields")
    public ResponseDTO<EntityFieldsRsp> getEntityFields(
            @RequestBody RequestDTO<GetEntityFieldsReq> reqDto) {
        List<EntityFieldVO> fields = sysDataFilterService.getEntityFields(reqDto.getBody().getEntityClass());
        EntityFieldsRsp rsp = new EntityFieldsRsp();
        rsp.setFields(fields);
        return ResponseDTO.newSuccessInstance(rsp);
    }
}
```

**说明：**
- 所有接口统一使用 `POST` 方法
- 入参使用 `RequestDTO<T>` 包裹
- 出参使用 `ResponseDTO<T>` 包裹
- 无返回数据的接口使用 `ResponseDTO<EmptyBody>`
- 分页查询请求继承 `PageDTO`，响应继承 `PageDTO<T>`

### 7.5 前端API接口定义

**文件位置：** `blink-base-web/src/api/dataScope.ts`

```typescript
// 数据权限过滤规则 API

/**
 * 规则配置（对应后端 RuleConfig 类）
 */
export interface RuleConfig {
  ruleType: string
  field?: string
  excludeFields?: string[]
  matchType?: string
  rangeType?: string
  relativeValue?: number
  relativeUnit?: string
  startDate?: string
  endDate?: string
  allowedValues?: (string | number)[]
  sqlFragment?: string
  deptList?: number[]
}

/**
 * 数据过滤规则信息
 */
export interface DataFilterInfo {
  dataFilterId: number
  dataFilterName: string
  dataFilterEnName: string
  entityClass: string
  tableName: string
  ruleType: string
  ruleConfig: RuleConfig
  status: number
  remark: string
}

/**
 * 查询数据过滤规则请求参数（继承分页参数）
 */
export interface QueryDataFilterParams extends PageParams {
  dataFilterName?: string
  ruleType?: string
  status?: number
}

// 查询列表
export const getDataFilterList = (params: QueryDataFilterParams): Promise<PageResult<DataFilterInfo>> => {
  return request.post('/sysDataFilter/getDataFilterList', { body: params })
}

// 新增
export const addDataFilter = (params: Omit<DataFilterInfo, 'dataFilterId'>): Promise<void> => {
  return request.post('/sysDataFilter/addDataFilter', { body: params })
}

// 更新
export const updateDataFilter = (params: DataFilterInfo): Promise<void> => {
  return request.post('/sysDataFilter/updateDataFilter', { body: params })
}

// 删除（注意：后端返回 ResponseDTO<EmptyBody>，前端统一处理）
export const deleteDataFilter = (dataFilterId: number): Promise<void> => {
  return request.post('/sysDataFilter/deleteDataFilter', { body: { dataFilterId } })
}

// 获取实体字段列表（用于字段过滤配置）
export const getEntityFields = (entityClass: string): Promise<EntityFieldsRsp> => {
  return request.post('/sysDataFilter/getEntityFields', { body: { entityClass } })
}

/**
 * 实体字段列表响应
 */
export interface EntityFieldsRsp {
  fields: EntityFieldInfo[]
}

export interface EntityFieldInfo {
  fieldName: string
  columnName: string
  fieldType: string
}
```

### 7.6 国际化配置

**文件位置：** `blink-base-web/src/locales/zh-cn.ts` 和 `en-us.ts`

```typescript
// zh-cn.ts
dataScope: {
  title: '数据权限',
  dataFilterName: '规则名称',
  entityClass: '实体类',
  ruleType: '规则类型',
  fieldFilter: '字段过滤',
  creatorFilter: '创建人过滤',
  deptFilter: '部门过滤',
  dateRangeFilter: '时间范围过滤',
  statusFilter: '状态过滤',
  customSql: '自定义SQL',
  excludeFields: '排除字段',
  matchType: '匹配类型',
  currentUser: '当前用户',
  userList: '指定用户',
  currentDept: '当前部门',
  deptList: '指定部门',
  deptAndChildren: '本部门及以下',
}

// en-us.ts
dataScope: {
  title: 'Data Permission',
  dataFilterName: 'Rule Name',
  // ...
}
```

### 7.7 组件文件结构

```
blink-base-web/src/views/system/dataScope/
├── index.vue                    # 列表页面
└── components/
    └── DataFilterFormDialog.vue # 新增/编辑弹窗
```

## 八、业务流程

### 8.1 配置流程

```
管理员登录
    ↓
进入 数据权限管理 页面
    ↓
新增过滤规则：
    ├── 选择实体类（自动加载表字段供选择）
    ├── 选择规则类型
    ├── 配置具体条件（动态表单）
    └── 保存到 sys_data_filter
    ↓
进入 权限管理 页面
    ↓
新增数据权限：
    ├── 权限类型选择"数据权限"
    ├── 关联刚创建的过滤规则
    └── 保存到 sys_permission
    ↓
进入 角色管理 → 分配权限
    ↓
为角色勾选数据权限
    ↓
保存到 sys_role_perm_rela（自动清除缓存）
```

### 8.2 运行时流程

```
用户请求查询接口
    ↓
MyBatis拦截器拦截SQL
    ↓
判断：用户是否超管？
    ├── 是 → 放行，执行原SQL
    └── 否 → 继续
    ↓
解析SQL获取表名
    ↓
表名映射获取实体类
    ↓
从缓存获取用户角色的过滤规则
    ↓
合并多角色规则（并集）
    ↓
依次执行规则处理器修改SQL
    ↓
执行修改后的SQL
    ↓
返回过滤后的数据
```

## 九、异常处理

| 场景 | 处理方式 |
|------|---------|
| 实体类未注册映射 | 跳过过滤，执行原SQL |
| 规则配置JSON解析失败 | 记录日志，跳过该规则 |
| 缓存获取失败 | 回退查询数据库 |
| SQL解析失败 | 记录日志，执行原SQL |

## 十、技术要点

### 10.1 MyBatis拦截器配置

在 `blink-datasource-starter` 中注册拦截器：

```java
/**
 * 数据范围权限自动配置类
 *
 * @author binblink
 */
@Configuration
public class DataScopeAutoConfiguration {

    /**
     * 注册数据范围权限拦截器
     * Spring 会自动注入 @Resource 标注的依赖
     *
     * @return 拦截器实例
     */
    @Bean
    public DataScopeInterceptor dataScopeInterceptor(
            DataScopeCache dataScopeCache,
            List<RuleHandler> ruleHandlers,
            RedisClient redisClient,
            SysUserGroupRelaMapper sysUserGroupRelaMapper,
            SysUserRoleRelaMapper sysUserRoleRelaMapper,
            SysGroupMapper sysGroupMapper
    ) {
        DataScopeInterceptor interceptor = new DataScopeInterceptor();
        // 通过 setter 注入依赖（或在拦截器中使用 @Resource 自动注入）
        interceptor.setDataScopeCache(dataScopeCache);
        interceptor.setRuleHandlers(ruleHandlers);
        interceptor.setRedisClient(redisClient);
        interceptor.setSysUserGroupRelaMapper(sysUserGroupRelaMapper);
        interceptor.setSysUserRoleRelaMapper(sysUserRoleRelaMapper);
        interceptor.setSysGroupMapper(sysGroupMapper);
        return interceptor;
    }
}
```

**说明：** 由于 MyBatis 拦截器通过 `@Bean` 方法创建，建议在拦截器中添加 setter 方法配合构造器注入，或使用 `@PostConstruct` 从 `ApplicationContext` 获取依赖。
```

### 10.2 实体字段发现API

前端需要获取实体类的字段列表，用于字段过滤配置。

**Controller接口：**

```java
/**
 * 数据范围权限控制器
 *
 * @author binblink
 */
@RestController
@RequestMapping("/sysDataFilter")
public class SysDataFilterController {

    @Resource
    private SysDataFilterService sysDataFilterService;

    /**
     * 获取实体类的字段列表
     *
     * @param reqDto 请求参数
     * @return 字段列表
     */
    @PostMapping("/getEntityFields")
    public ResponseDTO<EntityFieldsRsp> getEntityFields(
            @RequestBody @Validated RequestDTO<GetEntityFieldsReq> reqDto) {
        List<EntityFieldVO> fields = sysDataFilterService.getEntityFields(reqDto.getBody().getEntityClass());
        EntityFieldsRsp rsp = new EntityFieldsRsp();
        rsp.setFields(fields);
        return ResponseDTO.newSuccessInstance(rsp);
    }
}
```

**Service实现逻辑：**

```java
/**
 * 获取实体类的字段信息列表
 *
 * @param entityClass 实体类全限定名
 * @return 字段信息列表
 */
public List<EntityFieldVO> getEntityFields(String entityClass) {
    Class<?> clazz = Class.forName(entityClass);
    List<EntityFieldVO> fields = new ArrayList<>();

    for (Field field : clazz.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers())) {
            continue;  // 跳过静态字段
        }
        EntityFieldVO vo = new EntityFieldVO();
        vo.setFieldName(field.getName());
        vo.setFieldType(field.getType().getSimpleName());

        // 获取TableField注解中的列名
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && StrUtil.isNotBlank(tableField.value())) {
            vo.setColumnName(tableField.value());
        } else {
            // 驼峰转下划线
            vo.setColumnName(StrUtil.toUnderlineCase(field.getName()));
        }

        fields.add(vo);
    }
    return fields;
}
```

### 10.3 性能优化策略

**1. 规则缓存**

- Redis缓存角色规则，30分钟过期
- 配置变更时主动清除缓存
- 缓存未命中时回退查询数据库

**2. SQL解析优化**

```java
/**
 * SQL解析工具类
 * 使用JSqlParser解析SQL（比正则更可靠）
 *
 * @author binblink
 */
public class SqlParserUtil {

    private static final Map<String, Statement> SQL_CACHE = new ConcurrentHashMap<>();

    /**
     * 解析SQL语句
     *
     * @param sql SQL字符串
     * @return 解析后的Statement对象
     */
    public static Statement parse(String sql) {
        return SQL_CACHE.computeIfAbsent(sql, CCJSqlParserUtil::parse);
    }
}
```

**3. 拦截器快速跳过**

```java
/**
 * 拦截SQL执行，快速判断是否跳过过滤
 *
 * @param invocation 调用信息
 * @return 执行结果
 */
@Override
public Object intercept(Invocation invocation) throws Throwable {
    // 获取用户上下文
    BlinkRequestContext requestContext = BlinkRequestContextHolder.getContext();
    if (requestContext == null || StrUtil.isBlank(requestContext.getUserId())) {
        return invocation.proceed();
    }

    // 从 Redis 获取用户信息判断是否超管
    UserInfoRedisDO userInfo = JacksonUtil.convert(
        redisClient.get(RedisKeyConstans.USER_INFO + requestContext.getUserId()),
        UserInfoRedisDO.class
    );
    if (userInfo != null && CommonConstans.SUPER_ADMIN_YES.equals(userInfo.getSuperFlag())) {
        return invocation.proceed();
    }

    // ... 后续处理
}
```

**4. 批量查询优化**

对于批量查询场景，规则只解析一次，复用于多条SQL：

```java
/**
 * 数据范围请求级缓存
 * 在一次请求中缓存解析结果，避免重复解析
 *
 * @author binblink
 */
public class DataScopeRequestCache {
    private static final ThreadLocal<Map<String, List<RuleConfig>>> CACHE = new ThreadLocal<>();

    /**
     * 初始化请求级缓存
     * 应在请求开始时调用
     */
    public static void init() {
        CACHE.set(new HashMap<>());
    }

    /**
     * 清理请求级缓存
     * 必须在请求结束时调用，防止内存泄漏
     */
    public static void clear() {
        CACHE.remove();
    }

    /**
     * 获取缓存的规则列表
     *
     * @param entityClass 实体类名
     * @return 规则列表
     */
    public static List<RuleConfig> getRules(String entityClass) {
        Map<String, List<RuleConfig>> cache = CACHE.get();
        return cache != null ? cache.get(entityClass) : null;
    }

    /**
     * 缓存规则列表
     *
     * @param entityClass 实体类名
     * @param rules       规则列表
     */
    public static void setRules(String entityClass, List<RuleConfig> rules) {
        Map<String, List<RuleConfig>> cache = CACHE.get();
        if (cache != null) {
            cache.put(entityClass, rules);
        }
    }
}
```

### 10.4 字段过滤SQL重写

```java
/**
 * 过滤SELECT字段，排除指定字段
 *
 * @param selectPart    SELECT部分字符串
 * @param excludeFields 需要排除的字段列表
 * @param tableAlias    表别名（可为空）
 * @return 过滤后的SELECT字段字符串
 */
private String filterFields(String selectPart, List<String> excludeFields, String tableAlias) {
    List<String> originalFields = parseSelectFields(selectPart);
    List<String> filteredFields = new ArrayList<>();

    for (String field : originalFields) {
        String fieldName = extractFieldName(field);
        if (!excludeFields.contains(fieldName)) {
            filteredFields.add(field);
        }
    }

    return String.join(", ", filteredFields);
}
```

## 十一、测试要点

1. **字段过滤测试**：验证排除字段后SQL正确，敏感数据不返回
2. **条件过滤测试**：验证WHERE条件正确追加，数据范围正确
3. **多角色合并测试**：验证多角色规则取并集正确
4. **超管绕过测试**：验证超级管理员不受过滤限制
5. **缓存测试**：验证缓存命中、更新、清除正确
6. **异常测试**：验证各种异常场景下的降级处理