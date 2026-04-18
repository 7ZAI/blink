# 实例分组功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 gateway-admin 添加实例分组管理功能，实现分组的 CRUD 管理，并让实例按分组隔离路由配置。

**Architecture:** 采用标准的分层架构，后端按 Entity → Mapper → DTO → Service → Controller 顺序实现，前端按 API → Views → i18n 顺序实现。修改现有 GatewayInstanceDO 增加字段，修改 RoutePushServiceImpl 实现按分组过滤实例。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus 3.5, Vue 3.5, Element Plus 2.13, TypeScript

---

## 文件结构

### 后端新增文件
```
blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/
├── entity/
│   └── GatewayInstanceGroupDO.java          # 分组实体
├── mapper/
│   └── GatewayInstanceGroupMapper.java      # 分组 Mapper
├── dto/req/
│   ├── QueryInstanceGroupReq.java           # 分页查询请求
│   ├── AddInstanceGroupReq.java             # 新增分组请求
│   ├── UpdateInstanceGroupReq.java          # 更新分组请求
│   ├── GetInstanceGroupReq.java             # 获取详情请求
│   └── DeleteInstanceGroupReq.java          # 删除分组请求
├── dto/rsp/
│   └── InstanceGroupListRsp.java            # 分页列表响应
├── dto/vo/
│   └── InstanceGroupVO.java                 # 分组视图对象
├── service/
│   ├── GatewayInstanceGroupService.java     # 服务接口
│   └── impl/GatewayInstanceGroupServiceImpl.java  # 服务实现
└── controller/
    └── InstanceGroupController.java         # 控制器

blink-gateway/gateway-admin/src/main/resources/db/
└── V20260418__add_instance_group.sql        # 数据库迁移脚本
```

### 后端修改文件
```
blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/
├── entity/GatewayInstanceDO.java            # 新增 groupKey, storageMode 字段
├── dto/req/QueryInstanceReq.java            # 新增 groupKey 筛选字段
├── dto/vo/GatewayInstanceVO.java            # 新增 groupKey, storageMode 字段
├── constants/ErrCodeConstant.java           # 新增分组相关错误码
├── service/impl/GatewayInstanceServiceImpl.java  # 实例注册时读取分组配置
└── service/impl/RoutePushServiceImpl.java   # 推送时按分组过滤实例
```

### 前端新增文件
```
frontend/packages/gateway-admin/src/
├── api/instanceGroup.ts                     # 分组 API
└── views/instanceGroup/index.vue            # 分组管理页面
```

### 前端修改文件
```
frontend/packages/gateway-admin/src/
├── api/instance.ts                          # 新增 groupKey, storageMode 字段
├── views/instance/index.vue                 # 增加分组筛选和显示列
├── locales/zh-cn.ts                         # 中文文案
└── locales/en-us.ts                         # 英文文案
```

---

## Task 1: 数据库迁移脚本

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/resources/db/V20260418__add_instance_group.sql`

- [ ] **Step 1: 创建数据库迁移脚本**

```sql
-- 网关实例分组表
-- 用于管理网关实例的分组信息，支持路由按分组隔离
-- @author binblink
-- @since 2026-04-18

CREATE TABLE IF NOT EXISTS `gateway_instance_group` (
    `group_id` INT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    `group_key` VARCHAR(64) NOT NULL COMMENT '分组标识（业务唯一键）',
    `group_name` VARCHAR(128) NOT NULL COMMENT '分组名称',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注说明',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`group_id`),
    UNIQUE KEY `uk_group_key` (`group_key`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关实例分组表';

-- 实例表新增分组字段
ALTER TABLE `gateway_instance`
ADD COLUMN `group_key` VARCHAR(64) DEFAULT 'default' COMMENT '分组标识' AFTER `instance_id`,
ADD COLUMN `storage_mode` VARCHAR(16) DEFAULT 'redis' COMMENT '存储方式：redis/nacos' AFTER `group_key`,
ADD INDEX `idx_group_key` (`group_key`);

-- 初始化默认分组
INSERT INTO `gateway_instance_group` (`group_key`, `group_name`, `status`, `remark`)
VALUES ('default', '默认分组', 1, '系统默认分组');
```

---

## Task 2: 错误码常量定义

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java`

- [ ] **Step 1: 添加分组相关错误码**

在 `ErrCodeConstant.java` 文件末尾的 `}` 之前添加：

```java
    // ============ 实例分组错误码 GATE0210-GATE0219 ============

    /**
     * 实例分组不存在
     */
    String INSTANCE_GROUP_NOT_EXIST = "GATE0210";

    /**
     * 分组标识已存在
     */
    String INSTANCE_GROUP_KEY_EXISTS = "GATE0211";

    /**
     * 分组下存在实例，无法删除
     */
    String INSTANCE_GROUP_HAS_INSTANCES = "GATE0212";
```

---

## Task 3: 实体类

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/GatewayInstanceGroupDO.java`
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/GatewayInstanceDO.java`

- [ ] **Step 1: 创建分组实体类**

```java
package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 网关实例分组持久化对象
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
@TableName("gateway_instance_group")
public class GatewayInstanceGroupDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @TableId("group_id")
    private Integer groupId;

    /**
     * 分组标识（业务唯一键）
     */
    @TableField("group_key")
    private String groupKey;

    /**
     * 分组名称
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 状态：1启用 0禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 备注说明
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 修改 GatewayInstanceDO 添加字段**

在 `GatewayInstanceDO.java` 中，在 `instanceId` 字段之后添加：

```java
    /**
     * 分组标识
     */
    @TableField("group_key")
    private String groupKey;

    /**
     * 存储方式：redis/nacos
     */
    @TableField("storage_mode")
    private String storageMode;
```

---

## Task 4: Mapper 层

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/mapper/GatewayInstanceGroupMapper.java`

- [ ] **Step 1: 创建分组 Mapper 接口**

```java
package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.GatewayInstanceGroupDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 网关实例分组 Mapper 接口
 *
 * @author binblink
 * @since 2026-04-18
 */
@Mapper
public interface GatewayInstanceGroupMapper extends BaseMapper<GatewayInstanceGroupDO> {
}
```

---

## Task 5: DTO 请求类

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryInstanceGroupReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/AddInstanceGroupReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/UpdateInstanceGroupReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetInstanceGroupReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/DeleteInstanceGroupReq.java`

- [ ] **Step 1: 创建 QueryInstanceGroupReq**

```java
package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询实例分组列表请求参数
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryInstanceGroupReq extends Page {

    /**
     * 分组标识（模糊查询）
     */
    private String groupKey;

    /**
     * 分组名称（模糊查询）
     */
    private String groupName;

    /**
     * 状态：1启用 0禁用
     */
    private Byte status;
}
```

- [ ] **Step 2: 创建 AddInstanceGroupReq**

```java
package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增实例分组请求参数
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
public class AddInstanceGroupReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组标识（业务唯一键）
     */
    @NotBlank(message = "分组标识不能为空")
    private String groupKey;

    /**
     * 分组名称
     */
    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    /**
     * 备注说明
     */
    private String remark;
}
```

- [ ] **Step 3: 创建 UpdateInstanceGroupReq**

```java
package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新实例分组请求参数
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
public class UpdateInstanceGroupReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @NotNull(message = "分组ID不能为空")
    private Integer groupId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 状态：1启用 0禁用
     */
    private Byte status;

    /**
     * 备注说明
     */
    private String remark;
}
```

- [ ] **Step 4: 创建 GetInstanceGroupReq**

```java
package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取实例分组详情请求参数
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
public class GetInstanceGroupReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @NotNull(message = "分组ID不能为空")
    private Integer groupId;
}
```

- [ ] **Step 5: 创建 DeleteInstanceGroupReq**

```java
package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除实例分组请求参数
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
public class DeleteInstanceGroupReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @NotNull(message = "分组ID不能为空")
    private Integer groupId;
}
```

---

## Task 6: DTO 响应类和 VO

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/InstanceGroupVO.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/InstanceGroupListRsp.java`
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/GatewayInstanceVO.java`
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryInstanceReq.java`

- [ ] **Step 1: 创建 InstanceGroupVO**

```java
package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实例分组视图对象
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
public class InstanceGroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    private Integer groupId;

    /**
     * 分组标识
     */
    private String groupKey;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 状态：1启用 0禁用
     */
    private Byte status;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 InstanceGroupListRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.admin.dto.vo.InstanceGroupVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 实例分组列表响应
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InstanceGroupListRsp extends PageDTO<InstanceGroupVO> {
}
```

- [ ] **Step 3: 修改 GatewayInstanceVO 添加字段**

在 `GatewayInstanceVO.java` 中添加：

```java
    /**
     * 分组标识
     */
    private String groupKey;

    /**
     * 存储方式：redis/nacos
     */
    private String storageMode;
```

- [ ] **Step 4: 修改 QueryInstanceReq 添加分组筛选**

在 `QueryInstanceReq.java` 中添加：

```java
    /**
     * 分组标识（可选，用于过滤）
     */
    private String groupKey;
```

---

## Task 7: Service 接口

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/GatewayInstanceGroupService.java`

- [ ] **Step 1: 创建服务接口**

```java
package com.blink.gateway.admin.service;

import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.AddInstanceGroupReq;
import com.blink.gateway.admin.dto.req.DeleteInstanceGroupReq;
import com.blink.gateway.admin.dto.req.GetInstanceGroupReq;
import com.blink.gateway.admin.dto.req.QueryInstanceGroupReq;
import com.blink.gateway.admin.dto.req.UpdateInstanceGroupReq;
import com.blink.gateway.admin.dto.rsp.InstanceGroupListRsp;
import com.blink.gateway.admin.dto.vo.InstanceGroupVO;

import java.util.List;

/**
 * 实例分组服务接口
 *
 * @author binblink
 * @since 2026-04-18
 */
public interface GatewayInstanceGroupService {

    /**
     * 分页查询实例分组列表
     *
     * @param req 查询请求参数
     * @return 分组列表响应
     */
    ResponseDTO<InstanceGroupListRsp> queryInstanceGroupList(QueryInstanceGroupReq req);

    /**
     * 获取实例分组详情
     *
     * @param req 请求参数
     * @return 分组详情
     */
    ResponseDTO<InstanceGroupVO> getInstanceGroupDetail(GetInstanceGroupReq req);

    /**
     * 新增实例分组
     *
     * @param req 新增请求参数
     * @return 操作结果
     */
    ResponseDTO<Void> addInstanceGroup(AddInstanceGroupReq req);

    /**
     * 更新实例分组
     *
     * @param req 更新请求参数
     * @return 操作结果
     */
    ResponseDTO<Void> updateInstanceGroup(UpdateInstanceGroupReq req);

    /**
     * 删除实例分组
     *
     * @param req 删除请求参数
     * @return 操作结果
     */
    ResponseDTO<Void> deleteInstanceGroup(DeleteInstanceGroupReq req);

    /**
     * 获取所有启用的分组列表（用于下拉选择）
     *
     * @return 分组列表
     */
    ResponseDTO<List<InstanceGroupVO>> getEnabledInstanceGroups();
}
```

---

## Task 8: Service 实现

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceGroupServiceImpl.java`

- [ ] **Step 1: 创建服务实现类**

```java
package com.blink.gateway.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.dto.req.AddInstanceGroupReq;
import com.blink.gateway.admin.dto.req.DeleteInstanceGroupReq;
import com.blink.gateway.admin.dto.req.GetInstanceGroupReq;
import com.blink.gateway.admin.dto.req.QueryInstanceGroupReq;
import com.blink.gateway.admin.dto.req.UpdateInstanceGroupReq;
import com.blink.gateway.admin.dto.rsp.InstanceGroupListRsp;
import com.blink.gateway.admin.dto.vo.InstanceGroupVO;
import com.blink.gateway.admin.entity.GatewayInstanceDO;
import com.blink.gateway.admin.entity.GatewayInstanceGroupDO;
import com.blink.gateway.admin.mapper.GatewayInstanceGroupMapper;
import com.blink.gateway.admin.mapper.GatewayInstanceMapper;
import com.blink.gateway.admin.service.GatewayInstanceGroupService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_GROUP_HAS_INSTANCES;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_GROUP_KEY_EXISTS;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_GROUP_NOT_EXIST;

/**
 * 实例分组服务实现
 *
 * @author binblink
 * @since 2026-04-18
 */
@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class GatewayInstanceGroupServiceImpl implements GatewayInstanceGroupService {

    @Resource
    private GatewayInstanceGroupMapper gatewayInstanceGroupMapper;

    @Resource
    private GatewayInstanceMapper gatewayInstanceMapper;

    @Override
    public ResponseDTO<InstanceGroupListRsp> queryInstanceGroupList(QueryInstanceGroupReq req) {
        LambdaQueryWrapper<GatewayInstanceGroupDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(req.getGroupKey()), GatewayInstanceGroupDO::getGroupKey, req.getGroupKey())
                .like(StrUtil.isNotBlank(req.getGroupName()), GatewayInstanceGroupDO::getGroupName, req.getGroupName())
                .eq(req.getStatus() != null, GatewayInstanceGroupDO::getStatus, req.getStatus())
                .orderByDesc(GatewayInstanceGroupDO::getCreateTime);

        InstanceGroupListRsp rsp = new InstanceGroupListRsp();
        return PageUtils.queryPage(req, () -> gatewayInstanceGroupMapper.selectList(wrapper), rsp, this::convertToVO);
    }

    @Override
    public ResponseDTO<InstanceGroupVO> getInstanceGroupDetail(GetInstanceGroupReq req) {
        GatewayInstanceGroupDO groupDO = gatewayInstanceGroupMapper.selectById(req.getGroupId());
        if (groupDO == null) {
            BlinkException.throwBusinessException(INSTANCE_GROUP_NOT_EXIST);
        }
        return ResponseDTO.newSuccessInstance(convertToVO(groupDO));
    }

    @Override
    public ResponseDTO<Void> addInstanceGroup(AddInstanceGroupReq req) {
        // 检查 groupKey 是否已存在
        LambdaQueryWrapper<GatewayInstanceGroupDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GatewayInstanceGroupDO::getGroupKey, req.getGroupKey());
        if (gatewayInstanceGroupMapper.selectCount(wrapper) > 0) {
            BlinkException.throwBusinessException(INSTANCE_GROUP_KEY_EXISTS);
        }

        GatewayInstanceGroupDO groupDO = new GatewayInstanceGroupDO();
        groupDO.setGroupKey(req.getGroupKey());
        groupDO.setGroupName(req.getGroupName());
        groupDO.setRemark(req.getRemark());
        groupDO.setStatus((byte) 1);
        gatewayInstanceGroupMapper.insert(groupDO);

        log.info("[InstanceGroup] 新增实例分组成功 | groupKey: {}, groupName: {}", req.getGroupKey(), req.getGroupName());
        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<Void> updateInstanceGroup(UpdateInstanceGroupReq req) {
        GatewayInstanceGroupDO groupDO = gatewayInstanceGroupMapper.selectById(req.getGroupId());
        if (groupDO == null) {
            BlinkException.throwBusinessException(INSTANCE_GROUP_NOT_EXIST);
        }

        if (req.getGroupName() != null) {
            groupDO.setGroupName(req.getGroupName());
        }
        if (req.getStatus() != null) {
            groupDO.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            groupDO.setRemark(req.getRemark());
        }
        gatewayInstanceGroupMapper.updateById(groupDO);

        log.info("[InstanceGroup] 更新实例分组成功 | groupId: {}", req.getGroupId());
        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<Void> deleteInstanceGroup(DeleteInstanceGroupReq req) {
        GatewayInstanceGroupDO groupDO = gatewayInstanceGroupMapper.selectById(req.getGroupId());
        if (groupDO == null) {
            BlinkException.throwBusinessException(INSTANCE_GROUP_NOT_EXIST);
        }

        // 检查分组下是否有实例
        LambdaQueryWrapper<GatewayInstanceDO> instanceWrapper = new LambdaQueryWrapper<>();
        instanceWrapper.eq(GatewayInstanceDO::getGroupKey, groupDO.getGroupKey());
        if (gatewayInstanceMapper.selectCount(instanceWrapper) > 0) {
            BlinkException.throwBusinessException(INSTANCE_GROUP_HAS_INSTANCES);
        }

        gatewayInstanceGroupMapper.deleteById(req.getGroupId());

        log.info("[InstanceGroup] 删除实例分组成功 | groupId: {}, groupKey: {}", req.getGroupId(), groupDO.getGroupKey());
        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<List<InstanceGroupVO>> getEnabledInstanceGroups() {
        LambdaQueryWrapper<GatewayInstanceGroupDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GatewayInstanceGroupDO::getStatus, 1)
                .orderByAsc(GatewayInstanceGroupDO::getGroupKey);
        List<InstanceGroupVO> list = gatewayInstanceGroupMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return ResponseDTO.newSuccessInstance(list);
    }

    /**
     * 实体转 VO
     */
    private InstanceGroupVO convertToVO(GatewayInstanceGroupDO groupDO) {
        InstanceGroupVO vo = new InstanceGroupVO();
        BeanUtil.copyProperties(groupDO, vo);
        return vo;
    }
}
```

---

## Task 9: Controller 层

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/InstanceGroupController.java`

- [ ] **Step 1: 创建控制器**

```java
package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.AddInstanceGroupReq;
import com.blink.gateway.admin.dto.req.DeleteInstanceGroupReq;
import com.blink.gateway.admin.dto.req.GetInstanceGroupReq;
import com.blink.gateway.admin.dto.req.QueryInstanceGroupReq;
import com.blink.gateway.admin.dto.req.UpdateInstanceGroupReq;
import com.blink.gateway.admin.dto.rsp.InstanceGroupListRsp;
import com.blink.gateway.admin.dto.vo.InstanceGroupVO;
import com.blink.gateway.admin.service.GatewayInstanceGroupService;
import com.blink.log.annotation.RecordLog;
import com.blink.log.constant.LogType;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实例分组管理控制器
 *
 * @author binblink
 * @since 2026-04-18
 */
@RestController
@RequestMapping("/instanceGroup")
public class InstanceGroupController {

    @Resource
    private GatewayInstanceGroupService gatewayInstanceGroupService;

    /**
     * 分页查询实例分组列表
     *
     * @param reqDto 请求参数
     * @return 分组列表
     */
    @PostMapping("/queryInstanceGroupList")
    public ResponseDTO<InstanceGroupListRsp> queryInstanceGroupList(@RequestBody @Validated RequestDTO<QueryInstanceGroupReq> reqDto) {
        return gatewayInstanceGroupService.queryInstanceGroupList(reqDto.getBody());
    }

    /**
     * 获取实例分组详情
     *
     * @param reqDto 请求参数
     * @return 分组详情
     */
    @PostMapping("/getInstanceGroupDetail")
    public ResponseDTO<InstanceGroupVO> getInstanceGroupDetail(@RequestBody @Validated RequestDTO<GetInstanceGroupReq> reqDto) {
        return gatewayInstanceGroupService.getInstanceGroupDetail(reqDto.getBody());
    }

    /**
     * 新增实例分组
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "新增实例分组")
    @PostMapping("/addInstanceGroup")
    public ResponseDTO<Void> addInstanceGroup(@RequestBody @Validated RequestDTO<AddInstanceGroupReq> reqDto) {
        return gatewayInstanceGroupService.addInstanceGroup(reqDto.getBody());
    }

    /**
     * 更新实例分组
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "更新实例分组")
    @PostMapping("/updateInstanceGroup")
    public ResponseDTO<Void> updateInstanceGroup(@RequestBody @Validated RequestDTO<UpdateInstanceGroupReq> reqDto) {
        return gatewayInstanceGroupService.updateInstanceGroup(reqDto.getBody());
    }

    /**
     * 删除实例分组
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "删除实例分组")
    @PostMapping("/deleteInstanceGroup")
    public ResponseDTO<Void> deleteInstanceGroup(@RequestBody @Validated RequestDTO<DeleteInstanceGroupReq> reqDto) {
        return gatewayInstanceGroupService.deleteInstanceGroup(reqDto.getBody());
    }

    /**
     * 获取所有启用的分组列表（用于下拉选择）
     *
     * @param reqDto 请求参数
     * @return 分组列表
     */
    @PostMapping("/getEnabledInstanceGroups")
    public ResponseDTO<List<InstanceGroupVO>> getEnabledInstanceGroups(@RequestBody RequestDTO<Void> reqDto) {
        return gatewayInstanceGroupService.getEnabledInstanceGroups();
    }
}
```

---

## Task 10: 修改 GatewayInstanceServiceImpl

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceServiceImpl.java`

- [ ] **Step 1: 添加配置读取**

在类中添加配置属性（在其他 `@Value` 注解附近）：

```java
    /**
     * 默认分组标识
     */
    @Value("${blink.gateway.instance.default-group-key:default}")
    private String defaultGroupKey;

    /**
     * 默认存储方式
     */
    @Value("${blink.gateway.instance.default-storage-mode:redis}")
    private String defaultStorageMode;
```

- [ ] **Step 2: 修改实例同步逻辑**

在同步实例状态的方法中，当插入新实例时，从元数据读取 groupKey 和 storageMode。找到创建 `GatewayInstanceDO` 并设置属性的代码位置，添加：

```java
        // 从元数据读取分组和存储方式配置
        Map<String, String> metadata = instance.getMetadata();
        String groupKey = metadata != null ? metadata.getOrDefault("groupKey", defaultGroupKey) : defaultGroupKey;
        String storageMode = metadata != null ? metadata.getOrDefault("storageMode", defaultStorageMode) : defaultStorageMode;
        instanceDO.setGroupKey(groupKey);
        instanceDO.setStorageMode(storageMode);
```

- [ ] **Step 3: 修改查询结果转换**

在 `queryInstanceList` 方法中，确保返回的 VO 包含新字段。找到转换逻辑，添加：

```java
        vo.setGroupKey(doct.getGroupKey());
        vo.setStorageMode(doct.getStorageMode());
```

---

## Task 11: 修改 RoutePushServiceImpl

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/RoutePushServiceImpl.java`

- [ ] **Step 1: 修改广播模式下的实例过滤逻辑**

找到 `pushRoutes` 方法中获取在线实例的代码（约 167-175 行），修改为按分组过滤：

```java
        } else {
            // 广播模式：获取指定分组的在线实例
            String routesGroup = req.getRoutesGroup();
            if (StrUtil.isBlank(routesGroup)) {
                routesGroup = RouteConstant.DEFAULT_ROUTES_GROUP;
            }
            final String finalRoutesGroup = routesGroup;
            ResponseDTO<GatewayInstanceListRsp> instancesRsp = gatewayInstanceService.getGatewayInstances();
            if (instancesRsp.getBody() != null && instancesRsp.getBody().getInstances() != null) {
                targetInstanceIds = instancesRsp.getBody().getInstances().stream()
                    .filter(inst -> inst.getStatus().equals(INSTANCE_STATUS_ONLINE))
                    .filter(inst -> StrUtil.equals(inst.getGroupKey(), finalRoutesGroup)
                        || (StrUtil.isBlank(inst.getGroupKey()) && StrUtil.equals(finalRoutesGroup, RouteConstant.DEFAULT_ROUTES_GROUP)))
                    .map(GatewayInstanceVO::getInstanceId)
                    .toList();
            }
        }
```

---

## Task 12: 前端 API 定义

**Files:**
- Create: `frontend/packages/gateway-admin/src/api/instanceGroup.ts`
- Modify: `frontend/packages/gateway-admin/src/api/instance.ts`

- [ ] **Step 1: 创建分组 API 文件**

```typescript
import request from '@/utils/request'

// ==================== 类型定义 ====================

/**
 * 实例分组
 */
export interface InstanceGroup {
  groupId: number
  groupKey: string
  groupName: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 分页查询分组请求参数
 */
export interface QueryInstanceGroupParams {
  pageNum?: number
  pageSize?: number
  groupKey?: string
  groupName?: string
  status?: number
}

/**
 * 新增分组请求参数
 */
export interface AddInstanceGroupParams {
  groupKey: string
  groupName: string
  remark?: string
}

/**
 * 更新分组请求参数
 */
export interface UpdateInstanceGroupParams {
  groupId: number
  groupName?: string
  status?: number
  remark?: string
}

/**
 * 分页查询分组列表响应
 */
export interface QueryInstanceGroupListResult {
  rows: InstanceGroup[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

// ==================== API 函数 ====================

/**
 * 分页查询实例分组列表
 */
export const queryInstanceGroupList = (params: QueryInstanceGroupParams = {}): Promise<QueryInstanceGroupListResult> => {
  return request.post('/instanceGroup/queryInstanceGroupList', { body: params })
}

/**
 * 获取实例分组详情
 */
export const getInstanceGroupDetail = (params: { groupId: number }): Promise<InstanceGroup> => {
  return request.post('/instanceGroup/getInstanceGroupDetail', { body: params })
}

/**
 * 新增实例分组
 */
export const addInstanceGroup = (params: AddInstanceGroupParams): Promise<void> => {
  return request.post('/instanceGroup/addInstanceGroup', { body: params })
}

/**
 * 更新实例分组
 */
export const updateInstanceGroup = (params: UpdateInstanceGroupParams): Promise<void> => {
  return request.post('/instanceGroup/updateInstanceGroup', { body: params })
}

/**
 * 删除实例分组
 */
export const deleteInstanceGroup = (params: { groupId: number }): Promise<void> => {
  return request.post('/instanceGroup/deleteInstanceGroup', { body: params })
}

/**
 * 获取所有启用的分组列表（用于下拉选择）
 */
export const getEnabledInstanceGroups = (): Promise<InstanceGroup[]> => {
  return request.post('/instanceGroup/getEnabledInstanceGroups', { body: {} })
}

// ==================== API 对象导出 ====================

export const instanceGroupApi = {
  queryInstanceGroupList,
  getInstanceGroupDetail,
  addInstanceGroup,
  updateInstanceGroup,
  deleteInstanceGroup,
  getEnabledInstanceGroups,
}
```

- [ ] **Step 2: 修改 instance.ts 添加字段**

在 `InstanceInfo` 接口中添加：

```typescript
export interface InstanceInfo {
  // ... 现有字段保持不变
  groupKey?: string
  storageMode?: string
}
```

在 `QueryInstanceParams` 接口中添加：

```typescript
export interface QueryInstanceParams {
  // ... 现有字段保持不变
  groupKey?: string
}
```

在 `instanceApi` 对象中添加：

```typescript
  getEnabledInstanceGroups,
```

并导入：

```typescript
import { getEnabledInstanceGroups } from './instanceGroup'
```

---

## Task 13: 前端国际化文案

**Files:**
- Modify: `frontend/packages/gateway-admin/src/locales/zh-cn.ts`
- Modify: `frontend/packages/gateway-admin/src/locales/en-us.ts`

- [ ] **Step 1: 添加中文文案**

在 `zh-cn.ts` 中添加 `instanceGroup` 配置块（在 `instance` 配置块之后）：

```typescript
  instanceGroup: {
    title: '实例分组',
    groupKey: '分组标识',
    groupName: '分组名称',
    groupKeyPlaceholder: '请输入分组标识',
    groupNamePlaceholder: '请输入分组名称',
    remarkPlaceholder: '请输入备注说明',
    addGroup: '新增分组',
    editGroup: '编辑分组',
    deleteConfirm: '确定要删除该分组吗？删除后不可恢复',
    groupKeyRequired: '分组标识不能为空',
    groupNameRequired: '分组名称不能为空',
    groupKeyExists: '分组标识已存在',
    hasInstances: '该分组下存在实例，无法删除',
  },
```

- [ ] **Step 2: 添加英文文案**

在 `en-us.ts` 中添加 `instanceGroup` 配置块：

```typescript
  instanceGroup: {
    title: 'Instance Group',
    groupKey: 'Group Key',
    groupName: 'Group Name',
    groupKeyPlaceholder: 'Enter group key',
    groupNamePlaceholder: 'Enter group name',
    remarkPlaceholder: 'Enter remark',
    addGroup: 'Add Group',
    editGroup: 'Edit Group',
    deleteConfirm: 'Are you sure you want to delete this group? This action cannot be undone',
    groupKeyRequired: 'Group key is required',
    groupNameRequired: 'Group name is required',
    groupKeyExists: 'Group key already exists',
    hasInstances: 'Cannot delete group with instances',
  },
```

- [ ] **Step 3: 在 instance 中添加新字段文案**

在 `zh-cn.ts` 的 `instance` 配置块中添加：

```typescript
    groupKey: '分组',
    storageMode: '存储方式',
    groupKeyPlaceholder: '请选择分组',
    storageModeRedis: 'Redis',
    storageModeNacos: 'Nacos',
```

在 `en-us.ts` 的 `instance` 配置块中添加：

```typescript
    groupKey: 'Group',
    storageMode: 'Storage Mode',
    groupKeyPlaceholder: 'Select group',
    storageModeRedis: 'Redis',
    storageModeNacos: 'Nacos',
```

---

## Task 14: 前端分组管理页面

**Files:**
- Create: `frontend/packages/gateway-admin/src/views/instanceGroup/index.vue`

- [ ] **Step 1: 创建分组管理页面**

```vue
<template>
  <div class="instance-group-page table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card shrink-0" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('instanceGroup.groupKey')">
          <el-input
            v-model.trim="searchForm.groupKey"
            :placeholder="t('instanceGroup.groupKeyPlaceholder')"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item :label="t('instanceGroup.groupName')">
          <el-input
            v-model.trim="searchForm.groupName"
            :placeholder="t('instanceGroup.groupNamePlaceholder')"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="searchForm.status" :placeholder="t('common.pleaseSelect')" clearable style="width: 120px">
            <el-option :label="t('common.statusEnable')" :value="1" />
            <el-option :label="t('common.statusDisable')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card flex-1 flex flex-col overflow-hidden" shadow="never">
      <template #header>
        <div class="table-header">
          <el-button type="primary" style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            {{ t('instanceGroup.addGroup') }}
          </el-button>
        </div>
      </template>

      <!-- 表格区域 -->
      <div class="table-wrapper">
        <el-table v-loading="loading" :data="tableData" height="100%" stripe>
          <el-table-column prop="groupKey" :label="t('instanceGroup.groupKey')" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag type="info" effect="plain" size="small">{{ row.groupKey }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="groupName" :label="t('instanceGroup.groupName')" min-width="140" show-overflow-tooltip />
          <el-table-column :label="t('common.status')" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain" size="small">
                {{ row.status === 1 ? t('common.statusEnable') : t('common.statusDisable') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="t('common.remark')" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.remark || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('common.createTime')" width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ formatTime(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
                {{ t('common.edit') }}
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)" :disabled="row.groupKey === 'default'">
                <el-icon><Delete /></el-icon>
                {{ t('common.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-area">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('instanceGroup.editGroup') : t('instanceGroup.addGroup')"
      width="500px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item :label="t('instanceGroup.groupKey')" prop="groupKey">
          <el-input
            v-model.trim="formData.groupKey"
            :placeholder="t('instanceGroup.groupKeyPlaceholder')"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item :label="t('instanceGroup.groupName')" prop="groupName">
          <el-input v-model.trim="formData.groupName" :placeholder="t('instanceGroup.groupNamePlaceholder')" />
        </el-form-item>
        <el-form-item v-if="isEdit" :label="t('common.status')">
          <el-switch v-model="statusSwitch" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model.trim="formData.remark" type="textarea" :rows="3" :placeholder="t('instanceGroup.remarkPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  queryInstanceGroupList,
  addInstanceGroup,
  updateInstanceGroup,
  deleteInstanceGroup,
  type InstanceGroup,
} from '@/api/instanceGroup'

defineOptions({ name: 'InstanceGroupManagement' })

const { t } = useI18n()

// ==================== 数据状态 ====================

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<InstanceGroup[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const searchForm = reactive({
  groupKey: '',
  groupName: '',
  status: undefined as number | undefined,
})

const formData = reactive({
  groupId: 0,
  groupKey: '',
  groupName: '',
  remark: '',
})

const statusSwitch = ref(1)

// ==================== 表单校验 ====================

const formRules = computed<FormRules>(() => ({
  groupKey: [{ required: true, message: t('instanceGroup.groupKeyRequired'), trigger: 'blur' }],
  groupName: [{ required: true, message: t('instanceGroup.groupNameRequired'), trigger: 'blur' }],
}))

// ==================== 辅助方法 ====================

const formatTime = (time: string): string => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const resetFormData = () => {
  formData.groupId = 0
  formData.groupKey = ''
  formData.groupName = ''
  formData.remark = ''
  statusSwitch.value = 1
}

// ==================== 数据加载 ====================

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm,
    }
    const result = await queryInstanceGroupList(params)
    tableData.value = result?.rows || []
    pagination.total = result?.total || 0
  } catch (error) {
    console.error('Load data error:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ==================== 搜索 ====================

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.groupKey = ''
  searchForm.groupName = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  loadData()
}

// ==================== 分页 ====================

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadData()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  loadData()
}

// ==================== 新增/编辑 ====================

const handleAdd = () => {
  isEdit.value = false
  resetFormData()
  dialogVisible.value = true
}

const handleEdit = (row: InstanceGroup) => {
  isEdit.value = true
  formData.groupId = row.groupId
  formData.groupKey = row.groupKey
  formData.groupName = row.groupName
  formData.remark = row.remark || ''
  statusSwitch.value = row.status
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateInstanceGroup({
        groupId: formData.groupId,
        groupName: formData.groupName,
        status: statusSwitch.value,
        remark: formData.remark,
      })
    } else {
      await addInstanceGroup({
        groupKey: formData.groupKey,
        groupName: formData.groupName,
        remark: formData.remark,
      })
    }
    ElMessage.success(t('common.success'))
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('Submit error:', error)
  } finally {
    submitting.value = false
  }
}

// ==================== 删除 ====================

const handleDelete = (row: InstanceGroup) => {
  ElMessageBox.confirm(t('instanceGroup.deleteConfirm'), t('common.tips'), { type: 'warning' })
    .then(async () => {
      try {
        await deleteInstanceGroup({ groupId: row.groupId })
        ElMessage.success(t('common.success'))
        loadData()
      } catch (error) {
        console.error('Delete error:', error)
      }
    })
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.instance-group-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .search-card {
    flex-shrink: 0;
  }

  .table-card {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      overflow: auto;
      display: flex;
      flex-direction: column;
    }
  }

  .table-header {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .table-wrapper {
    flex: 1;
    min-height: 0;
  }

  .pagination-area {
    flex-shrink: 0;
    padding-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
```

---

## Task 15: 修改实例管理页面

**Files:**
- Modify: `frontend/packages/gateway-admin/src/views/instance/index.vue`

- [ ] **Step 1: 导入分组 API**

在文件顶部的 import 区域添加：

```typescript
import { getEnabledInstanceGroups, type InstanceGroup } from '@/api/instanceGroup'
```

- [ ] **Step 2: 添加分组数据状态**

在数据状态区域添加：

```typescript
const groupOptions = ref<InstanceGroup[]>([])
```

- [ ] **Step 3: 添加分组筛选下拉框**

在搜索区域的 `el-form` 中，在状态筛选之后添加：

```vue
          <el-form-item :label="t('instance.groupKey')">
            <el-select v-model="searchForm.groupKey" :placeholder="t('instance.groupKeyPlaceholder')" clearable style="width: 140px">
              <el-option
                v-for="group in groupOptions"
                :key="group.groupKey"
                :label="group.groupName"
                :value="group.groupKey"
              />
            </el-select>
          </el-form-item>
```

- [ ] **Step 4: 修改搜索表单数据**

在 `searchForm` reactive 对象中添加：

```typescript
  groupKey: '',
```

- [ ] **Step 5: 添加表格列**

在表格中，在 `port` 列之后添加：

```vue
        <el-table-column :label="t('instance.groupKey')" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.groupKey" type="info" effect="plain" size="small">{{ row.groupKey }}</el-tag>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('instance.storageMode')" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.storageMode" :type="row.storageMode === 'redis' ? 'success' : 'warning'" effect="plain" size="small">
              {{ row.storageMode === 'redis' ? t('instance.storageModeRedis') : t('instance.storageModeNacos') }}
            </el-tag>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
```

- [ ] **Step 6: 修改 handleReset 方法**

在 `handleReset` 方法中添加：

```typescript
  searchForm.groupKey = ''
```

- [ ] **Step 7: 添加加载分组列表方法**

```typescript
const loadGroupOptions = async () => {
  try {
    const result = await getEnabledInstanceGroups()
    groupOptions.value = result || []
  } catch (error) {
    console.error('Load group options error:', error)
  }
}
```

- [ ] **Step 8: 在 onMounted 中调用**

修改 `onMounted`：

```typescript
onMounted(() => {
  loadData()
  loadGroupOptions()
})
```

---

## Task 16: 菜单数据配置

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/resources/db/V20260418_2__add_instance_group_menu.sql`

- [ ] **Step 1: 创建菜单数据脚本**

需要先查询「监控中心」菜单的 ID。假设监控中心菜单 ID 为变量，实际执行时需替换：

```sql
-- 实例分组菜单
-- 放置在监控中心下，实例管理之前
-- @author binblink
-- @since 2026-04-18

-- 查询监控中心菜单ID
SET @monitor_parent_id = (SELECT menu_id FROM sys_menu WHERE route_path = 'monitor' LIMIT 1);

-- 插入实例分组菜单
INSERT INTO sys_menu (
    parent_id, menu_name, menu_type, route_path, component_path,
    perms, order_num, status, create_time, update_time
) VALUES (
    @monitor_parent_id, '实例分组', 'MENU', 'instanceGroup', 'instanceGroup/index',
    'monitor:instanceGroup:list', 1, 1, NOW(), NOW()
);

-- 更新实例管理的排序，让实例分组排在其前面
UPDATE sys_menu SET order_num = 2 WHERE parent_id = @monitor_parent_id AND route_path = 'instance';
```

---

## 自检清单

**1. Spec 覆盖检查：**
- ✅ 数据库设计：Task 1
- ✅ 实体类：Task 3
- ✅ DTO：Task 5, Task 6
- ✅ Mapper：Task 4
- ✅ Service：Task 7, Task 8
- ✅ Controller：Task 9
- ✅ 修改 GatewayInstanceDO：Task 3
- ✅ 修改 GatewayInstanceServiceImpl：Task 10
- ✅ 修改 RoutePushServiceImpl：Task 11
- ✅ 前端 API：Task 12
- ✅ 前端页面：Task 14
- ✅ 修改实例管理页面：Task 15
- ✅ 国际化：Task 13
- ✅ 菜单数据：Task 16

**2. 占位符扫描：** 无 TBD、TODO、implement later 等占位符

**3. 类型一致性：**
- 所有 DTO 字段类型与实体类一致
- 前端 TypeScript 接口与后端 DTO 对应
