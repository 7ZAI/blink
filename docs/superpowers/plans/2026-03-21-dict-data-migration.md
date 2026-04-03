# 字典数据迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将前端硬编码选项数据迁移到数据库字典表，提供统一的字典数据获取和展示机制。

**Architecture:** 后端新增批量获取字典数据API，前端使用Pinia Store缓存字典数据，通过useDict组合式函数和DictSelect/DictTag全局组件统一使用。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, Vue 3, TypeScript, Pinia, Element Plus

---

## 文件结构

### 后端文件

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `blink-base-app/.../dto/req/GetDictDataByTypesReq.java` | 批量获取字典请求DTO |
| 新增 | `blink-base-app/.../dto/rsp/DictDataMapRsp.java` | 字典数据Map响应DTO |
| 修改 | `blink-base-app/.../controller/SysDictDataController.java` | 新增批量获取接口 |
| 修改 | `blink-base-app/.../service/SysDictDataService.java` | 新增Service方法 |
| 修改 | `blink-base-app/.../service/impl/SysDictDataServiceImpl.java` | 实现批量获取逻辑 |
| 修改 | `blink-base-app/.../mapper/SysDictDataMapper.java` | 新增Mapper方法 |
| 修改 | `blink-base-app/.../resources/mapper/SysDictDataMapper.xml` | 新增SQL |
| 新增 | `blink-base-app/.../resources/db/migration/dict_data.sql` | 字典数据初始化SQL |

### 前端文件

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `blink-base-web/src/api/dict.ts` | 新增批量获取API |
| 新增 | `blink-base-web/src/stores/dict.ts` | Pinia字典Store |
| 新增 | `blink-base-web/src/composables/useDict.ts` | 字典组合式函数 |
| 新增 | `blink-base-web/src/components/Dict/DictSelect.vue` | 字典选择器组件 |
| 新增 | `blink-base-web/src/components/Dict/DictTag.vue` | 字典标签组件 |
| 新增 | `blink-base-web/src/components/Dict/index.ts` | 组件导出 |
| 修改 | `blink-base-web/src/main.ts` | 注册全局组件 |

---

## Task 1: 后端 - 新增请求DTO

**Files:**
- Create: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/GetDictDataByTypesReq.java`

- [ ] **Step 1: 创建GetDictDataByTypesReq.java**

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

---

## Task 2: 后端 - 新增响应DTO

**Files:**
- Create: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/rsp/DictDataMapRsp.java`

- [ ] **Step 1: 创建DictDataMapRsp.java**

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

        /**
         * 字典键值（实际值）
         */
        private String dictValue;

        /**
         * 字典标签（显示值）
         */
        private String dictLabel;

        /**
         * 表格回显样式
         */
        private String listClass;

        /**
         * 是否默认
         */
        private Boolean isDefault;
    }
}
```

---

## Task 3: 后端 - 新增Mapper方法和XML

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysDictDataMapper.java`
- Modify: `blink-base/blink-base-app/src/main/resources/mapper/SysDictDataMapper.xml`

- [ ] **Step 1: 在SysDictDataMapper.java中新增方法**

在接口末尾添加：

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

需要在文件顶部添加import:
```java
import org.apache.ibatis.annotations.Param;
```

- [ ] **Step 2: 在SysDictDataMapper.xml中新增SQL**

在文件末尾 `</mapper>` 标签前添加：

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

---

## Task 4: 后端 - 新增Service方法

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/service/SysDictDataService.java`

- [ ] **Step 1: 在SysDictDataService.java中新增方法**

在接口末尾添加：

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

需要在文件顶部添加import:
```java
import com.blink.rsp.dto.com.blink.base.DictDataMapRsp;
```

---

## Task 5: 后端 - 实现ServiceImpl方法

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysDictDataServiceImpl.java`

- [ ] **Step 1: 在SysDictDataServiceImpl.java中新增实现方法**

在类末尾添加：

```java
/**
 * 批量根据字典类型编码获取字典数据
 *
 * @param dictTypes 字典类型编码列表
 * @return 字典数据Map
 * @throws BlinkException 业务异常
 */
@Override
public DictDataMapRsp getDictDataByTypes(List<String> dictTypes) throws BlinkException {
    // 从上下文获取当前语言
    String locale = BlinkRequestContextHolder.getContext().getLanguage();
    if (StrUtil.isBlank(locale)) {
        locale = "zh_cn";
    }

    // 查询所有字典数据
    List<SysDictDataDO> dictDataList = sysDictDataMapper.selectDictDataByTypesAndLocale(dictTypes, locale);

    // 按dictType分组
    Map<String, List<DictDataMapRsp.DictDataItem>> dictDataMap = dictDataList.stream()
        .collect(Collectors.groupingBy(
            SysDictDataDO::getDictType,
            Collectors.mapping(this::convertToDictDataItem, Collectors.toList())
        ));

    DictDataMapRsp rsp = new DictDataMapRsp();
    rsp.setDictDataMap(dictDataMap);

    log.info("[SysDictData] 批量获取字典数据成功 | dictTypes: {}, locale: {}", dictTypes, locale);
    return rsp;
}

/**
 * 将DO转换为DictDataItem
 *
 * @param dictDataDO 字典数据DO
 * @return DictDataItem
 */
private DictDataMapRsp.DictDataItem convertToDictDataItem(SysDictDataDO dictDataDO) {
    DictDataMapRsp.DictDataItem item = new DictDataMapRsp.DictDataItem();
    item.setDictValue(dictDataDO.getDictValue());
    item.setDictLabel(dictDataDO.getDictLabel());
    item.setListClass(dictDataDO.getListClass());
    item.setIsDefault(dictDataDO.getIsDefault());
    return item;
}
```

需要在文件顶部添加import:
```java
import com.blink.rsp.dto.com.blink.base.DictDataMapRsp;
import java.util.stream.Collectors;
```

---

## Task 6: 后端 - 新增Controller接口

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/controller/SysDictDataController.java`

- [ ] **Step 1: 在SysDictDataController.java中新增接口**

在类末尾添加：

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

需要在文件顶部添加import:
```java
import com.blink.req.dto.com.blink.base.GetDictDataByTypesReq;
import com.blink.rsp.dto.com.blink.base.DictDataMapRsp;
```

- [ ] **Step 2: 编译验证后端代码**

Run: `./gradlew :blink-base:blink-base-app:compileJava`
Expected: BUILD SUCCESSFUL

---

## Task 7: 数据库 - 插入字典数据

**Files:**
- Create: `blink-base/blink-base-app/src/main/resources/db/migration/V20260321__init_dict_data.sql`

- [ ] **Step 1: 创建字典数据初始化SQL**

```sql
-- ============================================
-- 字典数据迁移初始化脚本
-- @author binblink
-- @since 2026-03-21
-- ============================================

-- ============================================
-- 1. 插入字典类型
-- ============================================

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark, locale) VALUES
('性别', 'sys_sex', 0, 'admin', NOW(), '用户性别字典', 'zh_cn'),
('用户状态', 'sys_user_status', 0, 'admin', NOW(), '用户锁定状态', 'zh_cn'),
('菜单类型', 'sys_menu_type', 0, 'admin', NOW(), '菜单类型字典', 'zh_cn'),
('显示状态', 'sys_show_status', 0, 'admin', NOW(), '显示隐藏状态', 'zh_cn'),
('权限类型', 'sys_permission_type', 0, 'admin', NOW(), '权限类型字典', 'zh_cn'),
('角色类型', 'sys_role_type', 0, 'admin', NOW(), '角色类型字典', 'zh_cn'),
('通用状态', 'sys_normal_status', 0, 'admin', NOW(), '通用启用禁用状态', 'zh_cn'),
('数据范围规则类型', 'sys_data_scope_rule', 0, 'admin', NOW(), '数据范围规则类型', 'zh_cn'),
('语言类型', 'sys_locale', 0, 'admin', NOW(), '语言类型字典', 'zh_cn'),
('是否', 'sys_yes_no', 0, 'admin', NOW(), '是否字典', 'zh_cn');

-- 英文版本
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark, locale) VALUES
('Gender', 'sys_sex', 0, 'admin', NOW(), 'User gender dictionary', 'en_us'),
('User Status', 'sys_user_status', 0, 'admin', NOW(), 'User lock status', 'en_us'),
('Menu Type', 'sys_menu_type', 0, 'admin', NOW(), 'Menu type dictionary', 'en_us'),
('Show Status', 'sys_show_status', 0, 'admin', NOW(), 'Show/Hide status', 'en_us'),
('Permission Type', 'sys_permission_type', 0, 'admin', NOW(), 'Permission type dictionary', 'en_us'),
('Role Type', 'sys_role_type', 0, 'admin', NOW(), 'Role type dictionary', 'en_us'),
('Normal Status', 'sys_normal_status', 0, 'admin', NOW(), 'Enable/Disable status', 'en_us'),
('Data Scope Rule Type', 'sys_data_scope_rule', 0, 'admin', NOW(), 'Data scope rule type', 'en_us'),
('Locale', 'sys_locale', 0, 'admin', NOW(), 'Locale dictionary', 'en_us'),
('Yes/No', 'sys_yes_no', 0, 'admin', NOW(), 'Yes/No dictionary', 'en_us');

-- ============================================
-- 2. 插入字典数据 - 中文
-- ============================================

-- 性别 (sys_sex)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_sex', '男', '1', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_sex', '女', '2', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_sex', '不确定', '3', NULL, 'info', 0, 0, 3, 'admin', NOW(), 'zh_cn');

-- 用户状态 (sys_user_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_user_status', '正常', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_user_status', '管理员锁定', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_user_status', '密码锁定', '2', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'zh_cn');

-- 菜单类型 (sys_menu_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_menu_type', '目录', '1', NULL, 'primary', 0, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_menu_type', '菜单', '2', NULL, 'success', 1, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_menu_type', '按钮', '3', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'zh_cn');

-- 显示状态 (sys_show_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_show_status', '显示', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_show_status', '隐藏', '1', NULL, 'info', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 权限类型 (sys_permission_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_permission_type', 'API权限', '1', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_permission_type', '数据权限', '2', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 角色类型 (sys_role_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_role_type', '自定义角色', '0', NULL, 'info', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_role_type', '系统角色', '1', NULL, 'primary', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 通用状态 (sys_normal_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_normal_status', '启用', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_normal_status', '禁用', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 数据范围规则类型 (sys_data_scope_rule)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_data_scope_rule', '字段过滤', 'FIELD_FILTER', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_data_scope_rule', '创建者过滤', 'CREATOR_FILTER', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_data_scope_rule', '日期范围过滤', 'DATE_RANGE_FILTER', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'zh_cn'),
('sys_data_scope_rule', '自定义SQL', 'CUSTOM_SQL', NULL, 'info', 0, 0, 4, 'admin', NOW(), 'zh_cn');

-- 语言类型 (sys_locale)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_locale', '简体中文', 'zh_cn', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_locale', 'English', 'en_us', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 是否 (sys_yes_no)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_yes_no', '是', '1', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_yes_no', '否', '0', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- ============================================
-- 3. 插入字典数据 - 英文
-- ============================================

-- Gender (sys_sex)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_sex', 'Male', '1', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_sex', 'Female', '2', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'en_us'),
('sys_sex', 'Unknown', '3', NULL, 'info', 0, 0, 3, 'admin', NOW(), 'en_us');

-- User Status (sys_user_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_user_status', 'Normal', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_user_status', 'Admin Locked', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'en_us'),
('sys_user_status', 'Password Locked', '2', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'en_us');

-- Menu Type (sys_menu_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_menu_type', 'Directory', '1', NULL, 'primary', 0, 0, 1, 'admin', NOW(), 'en_us'),
('sys_menu_type', 'Menu', '2', NULL, 'success', 1, 0, 2, 'admin', NOW(), 'en_us'),
('sys_menu_type', 'Button', '3', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'en_us');

-- Show Status (sys_show_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_show_status', 'Show', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_show_status', 'Hide', '1', NULL, 'info', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Permission Type (sys_permission_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_permission_type', 'API Permission', '1', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_permission_type', 'Data Permission', '2', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Role Type (sys_role_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_role_type', 'Custom Role', '0', NULL, 'info', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_role_type', 'System Role', '1', NULL, 'primary', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Normal Status (sys_normal_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_normal_status', 'Enabled', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_normal_status', 'Disabled', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Data Scope Rule Type (sys_data_scope_rule)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_data_scope_rule', 'Field Filter', 'FIELD_FILTER', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_data_scope_rule', 'Creator Filter', 'CREATOR_FILTER', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'en_us'),
('sys_data_scope_rule', 'Date Range Filter', 'DATE_RANGE_FILTER', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'en_us'),
('sys_data_scope_rule', 'Custom SQL', 'CUSTOM_SQL', NULL, 'info', 0, 0, 4, 'admin', NOW(), 'en_us');

-- Locale (sys_locale)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_locale', 'Simplified Chinese', 'zh_cn', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_locale', 'English', 'en_us', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Yes/No (sys_yes_no)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_yes_no', 'Yes', '1', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_yes_no', 'No', '0', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'en_us');
```

---

## Task 8: 前端 - 新增API接口

**Files:**
- Modify: `blink-base/blink-base-web/src/api/dict.ts`

- [ ] **Step 1: 在dict.ts末尾新增批量获取接口**

```typescript
/**
 * 批量获取字典数据参数
 */
export interface GetDictDataByTypesParams {
  dictTypes: string[]
}

/**
 * 字典数据项
 */
export interface DictDataItem {
  dictValue: string
  dictLabel: string
  listClass: string
  isDefault: boolean
}

/**
 * 批量获取字典数据响应
 */
export interface DictDataMapRsp {
  dictDataMap: Record<string, DictDataItem[]>
}

/**
 * 批量根据字典类型编码获取字典数据
 * @param params 包含dictTypes列表的参数
 * @returns 字典数据Map
 */
export const getDictDataByTypes = (params: GetDictDataByTypesParams): Promise<DictDataMapRsp> => {
  return request.post('/sysDictData/getDictDataByTypes', { body: params }) as Promise<DictDataMapRsp>
}
```

---

## Task 9: 前端 - 创建Pinia字典Store

**Files:**
- Create: `blink-base/blink-base-web/src/stores/dict.ts`

- [ ] **Step 1: 创建dict.ts Store**

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDictDataByTypes, type DictDataItem } from '@/api/dict'

/**
 * 字典数据Store
 * 管理系统中所有字典数据的缓存和获取
 */
export const useDictStore = defineStore('dict', () => {
  // 字典数据缓存
  const dictDataMap = ref<Record<string, DictDataItem[]>>({})

  // 已加载的字典类型
  const loadedTypes = ref<Set<string>>(new Set())

  // 加载状态
  const loading = ref(false)

  /**
   * 加载指定类型的字典数据
   * @param dictTypes 字典类型列表
   */
  const loadDictData = async (dictTypes: string[]) => {
    // 过滤出未加载的类型
    const typesToLoad = dictTypes.filter(type => !loadedTypes.value.has(type))

    if (typesToLoad.length === 0) {
      return
    }

    loading.value = true
    try {
      const response = await getDictDataByTypes({ dictTypes: typesToLoad })

      // 合并到缓存
      if (response?.dictDataMap) {
        Object.assign(dictDataMap.value, response.dictDataMap)

        // 标记为已加载
        typesToLoad.forEach(type => loadedTypes.value.add(type))
      }
    } catch (error) {
      console.error('[DictStore] 加载字典数据失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取指定类型的字典数据
   * @param dictType 字典类型
   * @returns 字典数据列表
   */
  const getDictData = (dictType: string): DictDataItem[] => {
    return dictDataMap.value[dictType] || []
  }

  /**
   * 根据字典值获取标签
   * @param dictType 字典类型
   * @param value 字典值
   * @returns 字典标签
   */
  const getLabelByValue = (dictType: string, value: string | number): string => {
    const items = dictDataMap.value[dictType]
    if (!items) return String(value)

    const item = items.find(item => item.dictValue === String(value))
    return item?.dictLabel || String(value)
  }

  /**
   * 根据字典标签获取值
   * @param dictType 字典类型
   * @param label 字典标签
   * @returns 字典值
   */
  const getValueByLabel = (dictType: string, label: string): string => {
    const items = dictDataMap.value[dictType]
    if (!items) return ''

    const item = items.find(item => item.dictLabel === label)
    return item?.dictValue || ''
  }

  /**
   * 获取字典数据的listClass
   * @param dictType 字典类型
   * @param value 字典值
   * @returns listClass
   */
  const getListClass = (dictType: string, value: string | number): string => {
    const items = dictDataMap.value[dictType]
    if (!items) return ''

    const item = items.find(item => item.dictValue === String(value))
    return item?.listClass || ''
  }

  /**
   * 清除字典缓存
   */
  const clearCache = () => {
    dictDataMap.value = {}
    loadedTypes.value.clear()
  }

  /**
   * 刷新指定类型的字典数据
   * @param dictTypes 字典类型列表
   */
  const refreshDictData = async (dictTypes: string[]) => {
    // 从已加载集合中移除
    dictTypes.forEach(type => loadedTypes.value.delete(type))

    // 重新加载
    await loadDictData(dictTypes)
  }

  return {
    dictDataMap,
    loading,
    loadDictData,
    getDictData,
    getLabelByValue,
    getValueByLabel,
    getListClass,
    clearCache,
    refreshDictData
  }
})
```

---

## Task 10: 前端 - 创建useDict组合式函数

**Files:**
- Create: `blink-base/blink-base-web/src/composables/useDict.ts`

- [ ] **Step 1: 创建composables目录和useDict.ts**

```typescript
import { computed, onMounted, type Ref } from 'vue'
import { useDictStore, type DictDataItem } from '@/stores/dict'

export interface DictOption {
  label: string
  value: string
  listClass?: string
  isDefault?: boolean
}

export interface UseDictReturn {
  /** 字典选项列表，格式为 { label, value } */
  options: Ref<DictOption[]>
  /** 原始字典数据列表 */
  dictData: Ref<DictDataItem[]>
  /** 根据值获取标签 */
  getLabel: (value: string | number) => string
  /** 根据标签获取值 */
  getValue: (label: string) => string
  /** 根据值获取listClass */
  getListClass: (value: string | number) => string
  /** 加载状态 */
  loading: Ref<boolean>
}

/**
 * 字典组合式函数
 * 提供统一的字典数据访问接口
 *
 * @param dictType 字典类型编码
 * @param autoLoad 是否自动加载，默认true
 * @returns 字典数据和相关方法
 *
 * @example
 * ```ts
 * const { options, getLabel, getListClass } = useDict('sys_sex')
 *
 * // options.value = [{ label: '男', value: '1' }, { label: '女', value: '2' }, ...]
 * // getLabel(1) => '男'
 * // getListClass(1) => 'primary'
 * ```
 */
export function useDict(dictType: string, autoLoad: boolean = true): UseDictReturn {
  const dictStore = useDictStore()

  // 字典数据
  const dictData = computed(() => dictStore.getDictData(dictType))

  // 转换为选项格式
  const options = computed<DictOption[]>(() => {
    return dictData.value.map(item => ({
      label: item.dictLabel,
      value: item.dictValue,
      listClass: item.listClass,
      isDefault: item.isDefault
    }))
  })

  // 根据值获取标签
  const getLabel = (value: string | number): string => {
    return dictStore.getLabelByValue(dictType, value)
  }

  // 根据标签获取值
  const getValue = (label: string): string => {
    return dictStore.getValueByLabel(dictType, label)
  }

  // 根据值获取listClass
  const getListClass = (value: string | number): string => {
    return dictStore.getListClass(dictType, value)
  }

  // 自动加载
  if (autoLoad) {
    onMounted(() => {
      dictStore.loadDictData([dictType])
    })
  }

  return {
    options,
    dictData,
    getLabel,
    getValue,
    getListClass,
    loading: dictStore.loading
  }
}

/**
 * 批量加载多个字典类型
 *
 * @param dictTypes 字典类型编码列表
 *
 * @example
 * ```ts
 * // 在应用初始化或路由守卫中预加载
 * await loadDicts(['sys_sex', 'sys_normal_status', 'sys_menu_type'])
 * ```
 */
export async function loadDicts(dictTypes: string[]): Promise<void> {
  const dictStore = useDictStore()
  await dictStore.loadDictData(dictTypes)
}
```

---

## Task 11: 前端 - 创建DictSelect组件

**Files:**
- Create: `blink-base/blink-base-web/src/components/Dict/DictSelect.vue`

- [ ] **Step 1: 创建DictSelect.vue组件**

```vue
<template>
  <el-select
    v-model="modelValue"
    :placeholder="placeholder || t('common.pleaseSelect')"
    :clearable="clearable"
    :disabled="disabled"
    :loading="loading"
    v-bind="$attrs"
  >
    <el-option
      v-for="item in options"
      :key="item.value"
      :label="item.label"
      :value="item.value"
    />
  </el-select>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDict, type DictOption } from '@/composables/useDict'

/**
 * 字典选择器组件
 * 根据字典类型自动加载字典数据并渲染下拉选择框
 */

interface Props {
  /** v-model绑定值 */
  modelValue?: string | number | null
  /** 字典类型编码 */
  dictType: string
  /** 占位符文本 */
  placeholder?: string
  /** 是否可清空 */
  clearable?: boolean
  /** 是否禁用 */
  disabled?: boolean
  /** 值类型是否为数字 */
  valueType?: 'string' | 'number'
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  clearable: true,
  disabled: false,
  valueType: 'string'
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number | null]
}>()

const { t } = useI18n()
const { options: dictOptions, loading } = useDict(props.dictType)

// 转换选项值类型
const options = computed<DictOption[]>(() => {
  return dictOptions.value.map(item => ({
    ...item,
    value: props.valueType === 'number' ? Number(item.value) : item.value
  }))
})

// 双向绑定值
const modelValue = computed({
  get: () => {
    if (props.modelValue === undefined || props.modelValue === null) {
      return undefined
    }
    return props.valueType === 'number' ? Number(props.modelValue) : String(props.modelValue)
  },
  set: (val) => {
    emit('update:modelValue', val ?? null)
  }
})
</script>
```

---

## Task 12: 前端 - 创建DictTag组件

**Files:**
- Create: `blink-base/blink-base-web/src/components/Dict/DictTag.vue`

- [ ] **Step 1: 创建DictTag.vue组件**

```vue
<template>
  <el-tag :type="tagType" v-bind="$attrs">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useDict } from '@/composables/useDict'

/**
 * 字典标签组件
 * 根据字典类型和值自动显示对应的标签
 */

interface Props {
  /** 字典值 */
  value?: string | number | null
  /** 字典类型编码 */
  dictType: string
}

const props = defineProps<Props>()

const { getLabel, getListClass } = useDict(props.dictType)

// 获取显示标签
const label = computed(() => {
  if (props.value === undefined || props.value === null) {
    return '-'
  }
  return getLabel(props.value)
})

// 将listClass映射为el-tag的type
const tagType = computed(() => {
  if (props.value === undefined || props.value === null) {
    return 'info'
  }

  const listClass = getListClass(props.value)

  // 映射关系：primary -> primary, success -> success, warning -> warning, danger -> danger, info -> info
  const typeMap: Record<string, string> = {
    primary: 'primary',
    success: 'success',
    warning: 'warning',
    danger: 'danger',
    info: 'info'
  }

  return typeMap[listClass] || 'info'
})
</script>
```

---

## Task 13: 前端 - 创建组件导出文件

**Files:**
- Create: `blink-base/blink-base-web/src/components/Dict/index.ts`

- [ ] **Step 1: 创建index.ts导出文件**

```typescript
import DictSelect from './DictSelect.vue'
import DictTag from './DictTag.vue'

export { DictSelect, DictTag }

// 默认导出
export default {
  DictSelect,
  DictTag
}
```

---

## Task 14: 前端 - 注册全局组件

**Files:**
- Modify: `blink-base/blink-base-web/src/main.ts`

- [ ] **Step 1: 在main.ts中注册全局组件**

在现有import语句后添加：

```typescript
import { DictSelect, DictTag } from '@/components/Dict'
```

在 `app.use(pinia)` 等语句后添加：

```typescript
// 注册字典组件为全局组件
app.component('DictSelect', DictSelect)
app.component('DictTag', DictTag)
```

- [ ] **Step 2: 验证前端编译**

Run: `cd blink-base/blink-base-web && npm run build`
Expected: BUILD SUCCESSFUL

---

## Task 15: 提交代码

- [ ] **Step 1: 提交后端代码**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/GetDictDataByTypesReq.java
git add blink-base/blink-base-app/src/main/java/com/blink/base/dto/rsp/DictDataMapRsp.java
git add blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysDictDataMapper.java
git add blink-base/blink-base-app/src/main/java/com/blink/base/service/SysDictDataService.java
git add blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysDictDataServiceImpl.java
git add blink-base/blink-base-app/src/main/java/com/blink/base/controller/SysDictDataController.java
git add blink-base/blink-base-app/src/main/resources/mapper/SysDictDataMapper.xml
git commit -m "feat(base-app): 新增批量获取字典数据API接口

- 新增GetDictDataByTypesReq请求DTO
- 新增DictDataMapRsp响应DTO
- 新增Mapper方法selectDictDataByTypesAndLocale
- 新增Service方法getDictDataByTypes
- 新增Controller接口/sysDictData/getDictDataByTypes

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

- [ ] **Step 2: 提交前端代码**

```bash
git add blink-base/blink-base-web/src/api/dict.ts
git add blink-base/blink-base-web/src/stores/dict.ts
git add blink-base/blink-base-web/src/composables/useDict.ts
git add blink-base/blink-base-web/src/components/Dict/
git add blink-base/blink-base-web/src/main.ts
git commit -m "feat(base-web): 新增字典数据管理模块

- 新增getDictDataByTypes批量获取API
- 新增Pinia字典Store
- 新增useDict组合式函数
- 新增DictSelect/DictTag全局组件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

- [ ] **Step 3: 提交设计文档**

```bash
git add docs/superpowers/specs/2026-03-21-dict-data-migration-design.md
git add docs/superpowers/plans/2026-03-21-dict-data-migration.md
git commit -m "docs: 新增字典数据迁移设计文档和实施计划

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 后续任务（可选）

替换各页面硬编码为字典组件的工作可根据需要单独实施，主要涉及：

1. 用户管理页面 - 性别、用户状态
2. 菜单管理页面 - 菜单类型、显示状态
3. 权限管理页面 - 权限类型
4. 角色管理页面 - 角色类型、状态
5. 数据范围页面 - 规则类型
6. 字典数据页面 - 语言、是否默认