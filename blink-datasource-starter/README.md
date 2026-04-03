# blink-datasource-starter

数据库基础设施模块，封装 MyBatis-Plus、Druid、PageHelper，提供代码生成、分页查询、自动配置等能力。

## 功能特性

| 功能模块 | 说明 |
|---------|------|
| MyBatis-Plus 集成 | 自动配置，支持 CRUD、逻辑删除、自动填充 |
| Druid 连接池 | 数据库连接池管理 |
| PageHelper 分页 | 物理分页支持 |
| 代码生成器 | 基于自定义模板生成 Entity、Mapper、Service、Controller、DTO |
| 自动字段填充 | create_time、update_time 自动填充 |

## 快速开始

### 引入依赖

```gradle
implementation 'com.blink:blink-datasource-starter:1.0.0-SNAPSHOT'
```

### 配置

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: root
    password: 123456
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
```

---

## MyBatis-Plus 配置

### 自动配置项

模块自动配置以下内容：

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 一级缓存 | STATEMENT | 关闭（微服务场景） |
| 二级缓存 | false | 关闭（使用 Redis 替代） |
| 驼峰映射 | true | 自动下划线转驼峰 |
| 空值映射 | true | 调用 setters on nulls |
| Mapper 扫描 | `com.blink.**.mapper` | 自动扫描 |
| 逻辑删除字段 | delFlag | 自动处理 |

### 实体类示例

```java
@Data
@TableName("sys_user")
public class SysUserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
```

### Mapper 示例

```java
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserDO> {

    // 自定义查询
    List<SysUserDO> selectByCondition(@Param("username") String username);
}
```

---

## 分页查询

### PageUtils 工具类

```java
@Service
public class UserService {

    @Resource
    private SysUserMapper userMapper;

    /**
     * 分页查询
     */
    public QueryUserRspDTO queryPage(QueryUserReqDTO queryParam) {
        QueryUserRspDTO pageRsp = new QueryUserRspDTO();
        return PageUtils.queryPage(
            queryParam,
            () -> userMapper.selectByCondition(queryParam.getUsername()),
            pageRsp
        );
    }
}
```

### 分页 DTO

```java
// 请求 DTO
@Data
public class QueryUserReqDTO extends PageDTO {
    private String username;
    private Integer status;
}

// 响应 DTO
@Data
public class QueryUserRspDTO extends PageDTO<SysUserDO> {
    // 继承分页字段：pageNum, pageSize, total, pages, rows
}
```

### 排序支持

```java
QueryUserReqDTO queryParam = new QueryUserReqDTO();
queryParam.setPageNum(1);
queryParam.setPageSize(10);
queryParam.setOrderBy("create_time desc, id asc");  // 支持多字段排序
```

---

## 代码生成器

### 使用配置文件生成

创建 `src/main/resources/datasource-dev.yml`：

```yaml
generator:
  url: jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
  username: root
  password: 123456
```

```java
// 使用默认配置生成
CodeGenerator.generate();

// 指定 profile 生成
CodeGenerator.generate("dev", null);

// 指定路径生成
CodeGenerator.generate("dev", "/path/to/output");
```

### 使用自定义模板生成

```java
String url = "jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
String username = "root";
String password = "123456";

CodeGenerator.generateByCustomTemplate(url, username, password);
```

交互式输入：
- 作者名称
- 应用包名（已有前缀 com.blink）
- 表名（多个用逗号分隔，all 表示全部）
- 表前缀过滤（none 表示不过滤）

### 生成文件结构

```
com.blink.{appName}
├── controller
│   └── {Entity}Controller.java
├── dto
│   ├── req
│   │   ├── Add{Entity}Req.java
│   │   ├── Delete{Entity}Req.java
│   │   ├── Update{Entity}Req.java
│   │   └── Query{Entity}Req.java
│   └── rsp
│       └── Query{Entity}Rsp.java
├── entity
│   └── {Entity}DO.java
├── mapper
│   └── {Entity}Mapper.java
├── service
│   └── impl
│       └── {Entity}ServiceImpl.java
└── test
    └── {Entity}ControllerTest.java
```

### 生成文件示例

**Entity（DO）**
```java
@Data
@TableName("sys_user")
public class SysUserDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

**AddReq**
```java
@Data
public class AddSysUserReq {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

**Controller**
```java
@RestController
@RequestMapping("/sysUser")
public class SysUserController {

    @Resource
    private SysUserServiceImpl sysUserService;

    @PostMapping("/add")
    public ResponseDTO<EmptyBody> add(@RequestBody RequestDTO<AddSysUserReq> request) {
        // 实现...
    }

    @PostMapping("/query")
    public ResponseDTO<QuerySysUserRsp> query(@RequestBody RequestDTO<QuerySysUserReq> request) {
        // 实现...
    }
}
```

---

## 自定义模板

模板文件位于 `src/main/resources/codeTemplate/`：

| 模板文件 | 说明 |
|---------|------|
| `controller.java.vm` | Controller 模板 |
| `serviceImpl.java.vm` | Service 实现模板 |
| `mapper.java.vm` | Mapper 接口模板 |
| `mapper.xml.vm` | Mapper XML 模板 |
| `entity.java.vm` | Entity 模板 |
| `dto/AddReq.java.vm` | 新增请求 DTO 模板 |
| `dto/DeleteReq.java.vm` | 删除请求 DTO 模板 |
| `dto/UpdateReq.java.vm` | 更新请求 DTO 模板 |
| `dto/QueryReq.java.vm` | 查询请求 DTO 模板 |
| `dto/QueryRsp.java.vm` | 查询响应 DTO 模板 |
| `test.java.vm` | 测试类模板 |

---

## 数据库连接池

### Druid 配置

```yaml
spring:
  datasource:
    druid:
      # 初始化大小
      initial-size: 5
      # 最小空闲连接数
      min-idle: 5
      # 最大连接数
      max-active: 20
      # 获取连接等待超时时间
      max-wait: 60000
      # 检测间隔
      time-between-eviction-runs-millis: 60000
      # 连接最小生存时间
      min-evictable-idle-time-millis: 300000
      # 验证 SQL
      validation-query: SELECT 1 FROM DUAL
      # 申请连接时检测
      test-while-idle: true
      # 申请连接时检测
      test-on-borrow: false
      # 归还连接时检测
      test-on-return: false
```

---

## 最佳实践

### 1. 实体类命名

- 数据库表：`sys_user`
- 实体类：`SysUserDO`（后缀 DO 表示 Data Object）

### 2. Mapper 接口

```java
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserDO> {
    // 简单 CRUD 继承 BaseMapper 即可
    // 复杂查询自定义方法
}
```

### 3. Service 层

```java
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUserDO> {
    
    // 使用 IService 提供的方法
    public boolean saveUser(SysUserDO user) {
        return save(user);
    }
    
    // 自定义业务方法
    public SysUserDO getByUsername(String username) {
        return lambdaQuery()
            .eq(SysUserDO::getUsername, username)
            .one();
    }
}
```

### 4. 分页查询

```java
// 推荐：使用 PageUtils 封装
public PageDTO<UserVO> queryUserPage(QueryUserReqDTO req) {
    return PageUtils.queryPage(
        req,
        () -> userMapper.selectUserList(req),
        new QueryUserRspDTO()
    );
}
```

---

## 注意事项

1. **缓存策略**
   - 微服务场景下关闭 MyBatis 一二级缓存
   - 热点数据使用 Redis 缓存

2. **分页插件**
   - 确保分页查询在 PageHelper 之后执行
   - 避免在分页查询中嵌套复杂子查询

3. **代码生成**
   - 首次生成后建议 review 生成的代码
   - 自定义模板可根据项目需求调整

4. **逻辑删除**
   - 默认字段名：`delFlag`
   - 已删除：1，未删除：0
