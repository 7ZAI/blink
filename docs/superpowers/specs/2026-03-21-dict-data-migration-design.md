# 字典数据迁移设计方案

## 概述

将 blink-base-web 前端硬编码的选项数据迁移到数据库字典表，并提供统一的字典数据获取机制。

## 硬编码数据清单

| 模块 | 字段 | 字典类型编码 | 硬编码选项 |
|------|------|-------------|-----------|
| 用户管理 | 性别 | `sys_sex` | 男(1)、女(2)、不确定(3) |
| 用户管理 | 用户状态 | `sys_user_status` | 正常(0)、管理员锁定(1)、密码锁定(2) |
| 菜单管理 | 菜单类型 | `sys_menu_type` | 目录(1)、菜单(2)、按钮(3) |
| 菜单管理 | 显示状态 | `sys_show_status` | 显示(0)、隐藏(1) |
| 权限管理 | 权限类型 | `sys_permission_type` | API权限(1)、数据权限(2) |
| 角色管理 | 角色类型 | `sys_role_type` | 系统角色(1)、自定义角色(0) |
| 通用 | 状态 | `sys_normal_status` | 启用(0)、禁用(1) |
| 数据范围 | 规则类型 | `sys_data_scope_rule` | FIELD_FILTER、CREATOR_FILTER、DATE_RANGE_FILTER、CUSTOM_SQL |
| 字典管理 | 语言类型 | `sys_locale` | zh_cn(简体中文)、en_us(English) |
| 字典管理 | 是否 | `sys_yes_no` | 是(1)、否(0) |

## 后端设计

### 1. 新增请求DTO

**文件路径**: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/GetDictDataByTypesReq.java`

```java
package com.blink.base.dto.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量获取字典数据请求参数对象
 *
 * @author binblink
 * @since 2026-03-21
 */
@Data
public class GetDictDataByTypesReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典类型编码列表
     */
    @NotEmpty(message = "字典类型编码列表不能为空")
    private List<String> dictTypes;
}
```

### 2. 新增响应DTO

**文件路径**: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/rsp/DictDataMapRsp.java`

```java
package com.blink.base.dto.rsp;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 批量获取字典数据响应对象
 *
 * @author binblink
 * @since 2026-03-21
 */
@Data
public class DictDataMapRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典数据Map，key为dictType，value为该类型下的字典数据列表
     */
    private Map<String, List<DictDataItem>> dictDataMap;

    /**
     * 字典数据项
     */
    @Data
    public static class DictDataItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 字典键值（实际值） */
        private String dictValue;
        /** 字典标签（显示值） */
        private String dictLabel;
        /** 表格回显样式 */
        private String listClass;
        /** 是否默认 */
        private Boolean isDefault;
    }
}
```

### 3. Controller新增接口

**文件路径**: `blink-base/blink-base-app/src/main/java/com/blink/base/controller/SysDictDataController.java`

新增方法：

```java
/**
 * 批量根据字典类型编码获取字典数据
 *
 * @param reqDto 请求参数
 * @return {@link ResponseDTO<DictDataMapRsp>}
 * @throws BlinkException 业务异常
 */
@PostMapping("/getDictDataByTypes")
public ResponseDTO<DictDataMapRsp> getDictDataByTypes(
    @RequestBody @Validated RequestDTO<GetDictDataByTypesReq> reqDto
) throws BlinkException {
    return ResponseDTO.newSuccessInstance(
        sysDictDataService.getDictDataByTypes(reqDto.getBody().getDictTypes())
    );
}
```

### 4. Service层新增方法

**文件路径**: `blink-base/blink-base-app/src/main/java/com/blink/base/service/SysDictDataService.java`

新增方法：

```java
/**
 * 批量根据字典类型编码获取字典数据
 *
 * @param dictTypes 字典类型编码列表
 * @return 字典数据Map
 * @throws BlinkException 业务异常
 */
DictDataMapRsp getDictDataByTypes(List<String> dictTypes) throws BlinkException;
```

### 5. ServiceImpl实现

**文件路径**: `blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysDictDataServiceImpl.java`

新增方法实现，包含：
- 从上下文获取当前语言
- 批量查询字典数据
- 按dictType分组返回
- 记录日志

### 6. Mapper新增方法

**文件路径**: `blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysDictDataMapper.java`

```java
/**
 * 根据字典类型编码列表和语言批量查询字典数据
 *
 * @param dictTypes 字典类型编码列表
 * @param locale 语言标识
 * @return 字典数据列表
 */
List<SysDictDataDO> selectDictDataByTypesAndLocale(
    @Param("dictTypes") List<String> dictTypes,
    @Param("locale") String locale
);
```

### 7. Mapper XML

**文件路径**: `blink-base/blink-base-app/src/main/resources/mapper/SysDictDataMapper.xml`

```xml
<!-- 根据字典类型编码列表和语言批量查询字典数据 -->
<select id="selectDictDataByTypesAndLocale" resultMap="BaseResultMap">
    select
    <include refid="Base_Column_List"/>
    from sys_dict_data
    where dict_type in
    <foreach collection="dictTypes" item="dictType" open="(" separator="," close=")">
        #{dictType}
    </foreach>
    and locale = #{locale}
    and status = 0
    order by dict_type, order_num asc
</select>
```

## 前端设计

### 架构

```
src/
├── stores/
│   └── dict.ts                    # Pinia 字典 Store
├── composables/
│   └── useDict.ts                 # 组合式函数
├── components/
│   └── Dict/
│       ├── DictSelect.vue         # 字典选择器组件
│       ├── DictTag.vue            # 字典标签组件
│       └── index.ts               # 组件导出
└── api/
    └── dict.ts                    # 新增批量获取接口
```

### 核心API

**useDict 组合式函数：**
```typescript
const { options, getLabel, getValue } = useDict('sys_sex')
```

**DictSelect 组件：**
```vue
<DictSelect v-model="form.sex" dict-type="sys_sex" />
```

**DictTag 组件：**
```vue
<DictTag :value="row.sex" dict-type="sys_sex" />
```

## 数据库字典数据

需要插入以下字典数据：

### 字典类型 (sys_dict_type)

| dict_id | dict_name | dict_type | status |
|---------|-----------|-----------|--------|
| 自动生成 | 性别 | sys_sex | 0 |
| 自动生成 | 用户状态 | sys_user_status | 0 |
| 自动生成 | 菜单类型 | sys_menu_type | 0 |
| 自动生成 | 显示状态 | sys_show_status | 0 |
| 自动生成 | 权限类型 | sys_permission_type | 0 |
| 自动生成 | 角色类型 | sys_role_type | 0 |
| 自动生成 | 通用状态 | sys_normal_status | 0 |
| 自动生成 | 数据范围规则类型 | sys_data_scope_rule | 0 |
| 自动生成 | 语言类型 | sys_locale | 0 |
| 自动生成 | 是否 | sys_yes_no | 0 |

### 字典数据 (sys_dict_data)

详见实施阶段的SQL脚本。

## 实施步骤

1. 后端：新增请求/响应DTO、Controller接口、Service方法、Mapper方法和XML
2. 数据库：插入字典类型和字典数据
3. 前端：新增API接口、Pinia Store、组合式函数、全局组件
4. 前端：替换各页面硬编码为字典组件