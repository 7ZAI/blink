# 流程模型管理功能设计

## 1. 概述

### 1.1 背景
当前流程设计器的"保存流程"只存储到浏览器 localStorage，无法在流程列表中查看。"部署流程"才存入 Flowable 数据库。需要新增流程模型管理功能，支持草稿保存、发布、部署的完整生命周期。

### 1.2 目标
- 新增"流程模型"页面，支持模型的增删改查
- 新增"模型编辑"页面，复用 LogicFlow 设计器
- 支持模型状态管理：草稿 → 已发布 → 已部署
- 支持双格式存储：BPMN XML（部署用）+ LogicFlow JSON（编辑用）

### 1.3 范围
- 后端：新增模型管理接口
- 前端：新增模型列表页、模型编辑页
- 数据库：使用 Flowable 原生 ACT_RE_MODEL 表

---

## 2. 整体架构

### 2.1 模块结构

```
后端 (blink-base-app)
├── controller/
│   └── FlowableModelController.java      ← 新增：模型管理接口
├── service/
│   ├── FlowableModelService.java         ← 新增：模型服务接口
│   └── impl/FlowableModelServiceImpl.java ← 新增：模型服务实现
├── dto/
│   ├── req/
│   │   ├── SaveModelReq.java             ← 保存模型请求
│   │   ├── QueryModelReq.java            ← 查询模型请求
│   │   └── ModelIdReq.java               ← 模型ID请求
│   └── rsp/
│       └── ModelVO.java                  ← 模型响应VO
└── constants/
    └── ModelStatusConstant.java          ← 模型状态常量

前端 (blink-base-web)
├── views/workflow/
│   ├── model/
│   │   ├── index.vue                     ← 新增：模型列表页面
│   │   └── editor.vue                    ← 新增：模型编辑页面
│   ├── designer/index.vue                ← 保留：独立设计器
│   ├── process/index.vue                 ← 保留：流程列表
│   └── task/index.vue                    ← 保留：我的待办
├── api/
│   └── workflow.ts                       ← 新增模型相关API
└── router/index.ts                       ← 新增路由配置
```

### 2.2 数据流

```
模型编辑页
    │
    │ 保存
    ▼
ACT_RE_MODEL
├── EDITOR_SOURCE: BPMN XML (用于部署)
├── EDITOR_SOURCE_EXTRA: LogicFlow JSON (用于编辑还原)
└── STATUS: draft/published
    │
    │ 部署
    ▼
ACT_RE_PROCDEF (已部署流程定义)
    │
    ▼
流程实例执行
```

---

## 3. 数据库设计

### 3.1 使用 Flowable 原生表 ACT_RE_MODEL

| 字段 | 用途 |
|------|------|
| `ID_` | 模型ID |
| `REV_` | 版本号（乐观锁） |
| `NAME_` | 模型名称 |
| `KEY_` | 模型KEY（流程定义KEY） |
| `CATEGORY_` | 分类 |
| `CREATE_TIME_` | 创建时间 |
| `LAST_UPDATE_TIME_` | 最后更新时间 |
| `VERSION_` | 模型版本 |
| `META_INFO_` | 元信息（JSON格式，存储状态、描述等） |
| `DEPLOYMENT_ID_` | 部署ID（部署后关联） |
| `EDITOR_SOURCE_VALUE_ID_` | BPMN XML 存储 |
| `EDITOR_SOURCE_EXTRA_VALUE_ID_` | LogicFlow JSON 存储 |

### 3.2 META_INFO 扩展字段

```json
{
  "status": "draft",
  "description": "请假审批流程",
  "createUser": "admin",
  "updateUser": "admin"
}
```

### 3.3 状态流转

```
draft (草稿) ──发布──→ published (已发布) ──部署──→ 已部署
    │                      │
    │ 可编辑/删除          │ 可部署/删除
    ▼                      ▼
```

---

## 4. 后端接口设计

### 4.1 接口列表

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 新建模型 | POST | `/workflow/createModel` | 创建空白模型，返回模型ID |
| 保存模型 | POST | `/workflow/saveModel` | 保存模型内容（BPMN XML + LogicFlow JSON） |
| 分页查询模型 | POST | `/workflow/getModelList` | 按状态、名称筛选 |
| 获取模型详情 | POST | `/workflow/getModelDetail` | 用于编辑时加载数据 |
| 发布模型 | POST | `/workflow/publishModel` | 草稿 → 已发布 |
| 部署模型 | POST | `/workflow/deployModel` | 已发布 → 部署到 Flowable |
| 复制模型 | POST | `/workflow/copyModel` | 复制为新草稿 |
| 删除模型 | POST | `/workflow/deleteModel` | 删除模型 |
| 导出模型 | POST | `/workflow/exportModelXml` | 导出 BPMN XML 文件 |

### 4.2 请求/响应 DTO

#### SaveModelReq - 保存模型请求
```java
@Getter
@Setter
public class SaveModelReq implements Serializable {
    private String modelId;           // 模型ID（新建时为空）
    private String name;              // 模型名称
    private String key;               // 模型KEY
    private String category;          // 分类
    private String description;       // 描述
    private String bpmnXml;           // BPMN XML
    private String editorJson;        // LogicFlow JSON
}
```

#### QueryModelReq - 查询模型请求
```java
@Getter
@Setter
public class QueryModelReq extends PageDTO implements Serializable {
    private String name;              // 模型名称（模糊查询）
    private String key;               // 模型KEY
    private String status;            // 状态：draft/published/all
}
```

#### ModelIdReq - 模型ID请求
```java
@Getter
@Setter
public class ModelIdReq implements Serializable {
    private String modelId;
}
```

#### ModelVO - 模型响应
```java
@Getter
@Setter
public class ModelVO implements Serializable {
    private String modelId;
    private String name;
    private String key;
    private String category;
    private String description;
    private String status;            // draft | published
    private Integer version;
    private String deploymentId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### ModelRsp - 分页响应
```java
@Getter
@Setter
public class ModelRsp extends PageDTO<ModelVO> implements Serializable {
}
```

### 4.3 状态常量

```java
public interface ModelStatusConstant {
    String DRAFT = "draft";           // 草稿
    String PUBLISHED = "published";   // 已发布
}
```

---

## 5. 前端页面设计

### 5.1 模型列表页面

**布局：**
- 顶部：筛选条件（状态、名称搜索）+ 新建按钮
- 主体：数据表格，显示模型信息和操作按钮
- 底部：分页组件

**操作按钮：**

| 状态 | 可用操作 |
|------|---------|
| 草稿 | 编辑、发布、复制、删除、导出 |
| 已发布 | 部署、复制、删除、导出 |

### 5.2 模型编辑页面

**布局：**
- 顶部：返回按钮 + 基本信息（名称、KEY、分类、描述）
- 左侧：节点面板
- 中间：LogicFlow 画布
- 右侧：属性面板
- 底部：保存、保存并发布、导出XML 按钮

**功能：**
- 加载模型：根据 modelId 从后端获取数据，还原画布
- 保存：调用 saveModel 接口
- 保存并发布：保存后调用 publishModel 接口
- 导出XML：生成 BPMN XML 并下载

### 5.3 路由配置

```typescript
{
  path: 'workflow',
  name: 'Workflow',
  children: [
    {
      path: 'model',
      name: 'WorkflowModel',
      component: () => import('@/views/workflow/model/index.vue'),
      meta: { title: '流程模型', icon: 'Files' }
    },
    {
      path: 'model/edit/:modelId?',
      name: 'WorkflowModelEdit',
      component: () => import('@/views/workflow/model/editor.vue'),
      meta: { title: '编辑模型', hidden: true }
    },
    {
      path: 'designer',
      name: 'WorkflowDesigner',
      component: () => import('@/views/workflow/designer/index.vue'),
      meta: { title: '流程设计', icon: 'Edit' }
    },
    {
      path: 'process',
      name: 'WorkflowProcess',
      component: () => import('@/views/workflow/process/index.vue'),
      meta: { title: '流程列表', icon: 'List' }
    },
    {
      path: 'task',
      name: 'WorkflowTask',
      component: () => import('@/views/workflow/task/index.vue'),
      meta: { title: '我的待办', icon: 'Document' }
    }
  ]
}
```

---

## 6. 错误处理

### 6.1 业务异常

| 场景 | 错误码 | 提示信息 |
|------|--------|---------|
| 模型KEY已存在 | `MODEL_KEY_EXISTS` | 模型KEY已存在 |
| 模型不存在 | `MODEL_NOT_FOUND` | 模型不存在 |
| 状态错误（编辑） | `MODEL_STATUS_ERROR` | 只有草稿状态才能编辑 |
| 状态错误（部署） | `MODEL_STATUS_ERROR` | 只有已发布状态才能部署 |
| 部署失败 | `DEPLOY_FAILED` | 部署失败：{原因} |

### 6.2 并发处理

- 使用乐观锁（`REV_` 字段）防止并发修改冲突
- 保存时校验版本号，不一致则提示"数据已被修改，请刷新后重试"

### 6.3 删除逻辑

| 状态 | 删除行为 |
|------|---------|
| 草稿 | 直接删除，清理关联数据 |
| 已发布（未部署） | 直接删除 |
| 已发布（已部署） | 提示确认，级联删除部署记录 |

---

## 7. 测试要点

### 7.1 后端测试

- 模型 CRUD 操作
- 状态流转（发布、部署）
- 并发修改冲突
- KEY 唯一性校验
- 部署失败处理

### 7.2 前端测试

- 模型列表分页、筛选
- 模型编辑保存、还原
- 操作按钮权限控制
- 路由跳转参数传递