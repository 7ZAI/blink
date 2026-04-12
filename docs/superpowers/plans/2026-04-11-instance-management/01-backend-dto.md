# Task 1: 后端 DTO 和常量定义

**依赖:** 无

**目标:** 创建实例管理所需的请求/响应 DTO 和常量类

---

## 文件清单

- 修改: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java`
- 修改: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ConfigValueConstant.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryInstanceReq.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/SaveInstanceReq.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/DeleteInstanceReq.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetInstanceDetailReq.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/QueryInstanceListRsp.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/InstanceDetailRsp.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/InstanceInfoVO.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/JvmMetricsVO.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/HealthDetailVO.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/ComponentHealthVO.java`
- 新增: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/HttpMetricsVO.java`

---

### Task 1.1: 新增错误码常量

- [ ] **Step 1: 在 ErrCodeConstant.java 中新增实例管理错误码**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java`

在文件末尾 `}` 之前添加：

```java
    // ============ 实例管理错误码 GATE0160-GATE0169 ============

    /**
     * 实例已存在（相同 host:port）
     */
    String INSTANCE_ALREADY_EXIST = "GATE0160";

    /**
     * 保存实例失败
     */
    String SAVE_INSTANCE_FAILED = "GATE0161";

    /**
     * 删除实例失败
     */
    String DELETE_INSTANCE_FAILED = "GATE0162";

    /**
     * 查询实例列表失败
     */
    String QUERY_INSTANCE_LIST_FAILED = "GATE0163";

    // 注意：GET_INSTANCE_DETAIL_FAILED = "GATE0011" 已存在，直接复用
    // 注意：GATEWAY_INSTANCE_NOT_EXIST = "GATE0001" 已存在，直接复用

    /**
     * 在线实例不允许删除
     */
    String INSTANCE_ONLINE_CANNOT_DELETE = "GATE0164";
```

- [ ] **Step 2: 在 ConfigValueConstant.java 中新增实例状态常量**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ConfigValueConstant.java`

**注意:** GatewayInstanceServiceImpl 中已有私有常量 `STATUS_ONLINE = 0` 等，应移到 ConfigValueConstant 作为公共常量。

在文件末尾 `}` 之前添加：

```java
    /**
     * 实例状态 - 在线
     */
    Byte INSTANCE_STATUS_ONLINE = 0;

    /**
     * 实例状态 - 离线（注册中心无此实例）
     */
    Byte INSTANCE_STATUS_OFFLINE = 1;

    /**
     * 实例状态 - 下线（手动操作）
     */
    Byte INSTANCE_STATUS_SHUTDOWN = 2;
```

并在 GatewayInstanceServiceImpl.java 中修改为引用公共常量（在 Task 2 中处理）。

---

### Task 1.2: 创建请求 DTO

- [ ] **Step 3: 创建 QueryInstanceReq.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryInstanceReq.java`

```java
package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询实例列表请求参数
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryInstanceReq extends Page {

    /**
     * 服务ID（可选，用于过滤）
     */
    private String serviceId;

    /**
     * 主机地址（可选，用于过滤）
     */
    private String host;

    /**
     * 实例状态（可选，用于过滤）
     * 0-在线，1-离线，2-下线
     */
    private Byte status;
}
```

- [ ] **Step 4: 创建 SaveInstanceReq.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/SaveInstanceReq.java`

```java
package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 保存实例请求参数（新增/编辑）
 *
 * @author binblink
 */
@Data
public class SaveInstanceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID（编辑时必填）
     */
    private Integer id;

    /**
     * 服务ID
     */
    @NotBlank(message = "服务ID不能为空")
    private String serviceId;

    /**
     * 主机地址
     */
    @NotBlank(message = "主机地址不能为空")
    private String host;

    /**
     * 端口
     */
    @NotNull(message = "端口不能为空")
    private Integer port;

    /**
     * 元数据（JSON 格式，可选）
     */
    private String metadata;
}
```

- [ ] **Step 5: 创建 DeleteInstanceReq.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/DeleteInstanceReq.java`

```java
package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除实例请求参数
 *
 * @author binblink
 */
@Data
public class DeleteInstanceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例主键 ID
     */
    @NotNull(message = "实例ID不能为空")
    private Integer id;
}
```

- [ ] **Step 6: 创建 GetInstanceDetailReq.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetInstanceDetailReq.java`

```java
package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取实例详情请求参数
 *
 * @author binblink
 */
@Data
public class GetInstanceDetailReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例主键 ID
     */
    @NotNull(message = "实例ID不能为空")
    private Integer id;
}
```

---

### Task 1.3: 创建响应 DTO 和 VO

- [ ] **Step 7: 创建 QueryInstanceListRsp.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/QueryInstanceListRsp.java`

```java
package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.admin.dto.vo.InstanceInfoVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询实例列表响应
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryInstanceListRsp extends PageDTO<InstanceInfoVO> {
}
```

- [ ] **Step 8: 创建 InstanceDetailRsp.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/InstanceDetailRsp.java`

```java
package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.dto.vo.HealthDetailVO;
import com.blink.gateway.admin.dto.vo.HttpMetricsVO;
import com.blink.gateway.admin.dto.vo.InstanceInfoVO;
import com.blink.gateway.admin.dto.vo.JvmMetricsVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实例详情响应（包含监控指标）
 *
 * @author binblink
 */
@Data
public class InstanceDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例基本信息
     */
    private InstanceInfoVO instanceInfo;

    /**
     * 健康状态详情
     */
    private HealthDetailVO healthDetail;

    /**
     * JVM 监控指标
     */
    private JvmMetricsVO jvmMetrics;

    /**
     * HTTP 请求统计
     */
    private HttpMetricsVO httpMetrics;
}
```

- [ ] **Step 9: 创建 InstanceInfoVO.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/InstanceInfoVO.java`

```java
package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实例基本信息 VO
 *
 * @author binblink
 */
@Data
public class InstanceInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    private Integer id;

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * 服务 ID
     */
    private String serviceId;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * URI
     */
    private String uri;

    /**
     * 元数据
     */
    private String metadata;

    /**
     * 实例状态
     * 0-在线，1-离线，2-下线
     */
    private Byte status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 上线时间
     */
    private LocalDateTime onlineTime;

    /**
     * 下线时间
     */
    private LocalDateTime offlineTime;

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

- [ ] **Step 10: 创建 JvmMetricsVO.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/JvmMetricsVO.java`

```java
package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * JVM 监控指标 VO
 *
 * @author binblink
 */
@Data
public class JvmMetricsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 堆内存使用量 (bytes)
     */
    private Long heapUsed;

    /**
     * 堆内存最大值 (bytes)
     */
    private Long heapMax;

    /**
     * 堆内存使用率 (%)
     */
    private Double heapUsagePercent;

    /**
     * 非堆内存使用量 (bytes)
     */
    private Long nonHeapUsed;

    /**
     * 年轻代 GC 次数
     */
    private Long youngGcCount;

    /**
     * 年轻代 GC 时间 (ms)
     */
    private Long youngGcTime;

    /**
     * 老年代 GC 次数
     */
    private Long oldGcCount;

    /**
     * 老年代 GC 时间 (ms)
     */
    private Long oldGcTime;

    /**
     * 活跃线程数
     */
    private Integer liveThreads;

    /**
     * 峰值线程数
     */
    private Integer peakThreads;

    /**
     * 守护线程数
     */
    private Integer daemonThreads;

    /**
     * 采样时间戳
     */
    private Long timestamp;
}
```

- [ ] **Step 11: 创建 HealthDetailVO.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/HealthDetailVO.java`

```java
package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 健康状态详情 VO
 *
 * @author binblink
 */
@Data
public class HealthDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 整体状态
     */
    private String status;

    /**
     * 各组件健康状态
     */
    private List<ComponentHealthVO> components;
}
```

- [ ] **Step 12: 创建 ComponentHealthVO.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/ComponentHealthVO.java`

```java
package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 组件健康状态 VO
 *
 * @author binblink
 */
@Data
public class ComponentHealthVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 组件名称
     */
    private String name;

    /**
     * 状态
     */
    private String status;

    /**
     * 详情
     */
    private Map<String, Object> details;
}
```

- [ ] **Step 13: 创建 HttpMetricsVO.java**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/HttpMetricsVO.java`

```java
package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * HTTP 请求统计 VO
 *
 * @author binblink
 */
@Data
public class HttpMetricsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总请求数
     */
    private Long totalRequests;

    /**
     * 成功请求数
     */
    private Long successRequests;

    /**
     * 失败请求数
     */
    private Long failedRequests;

    /**
     * 成功率 (%)
     */
    private Double successRate;

    /**
     * 平均响应时间 (ms)
     */
    private Long avgResponseTime;

    /**
     * 采样时间戳
     */
    private Long timestamp;
}
```

---

### Task 1.4: 提交更改

- [ ] **Step 14: Git 提交**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ConfigValueConstant.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryInstanceReq.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/SaveInstanceReq.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/DeleteInstanceReq.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetInstanceDetailReq.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/QueryInstanceListRsp.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/InstanceDetailRsp.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/InstanceInfoVO.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/JvmMetricsVO.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/HealthDetailVO.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/ComponentHealthVO.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/HttpMetricsVO.java
git commit -m "feat(instance): 新增实例管理 DTO 和常量定义

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验收检查

| 检查项 | 状态 |
|--------|------|
| 所有 DTO 文件已创建 | [ ] |
| 错误码已添加到 ErrCodeConstant | [ ] |
| 实例状态常量已添加 | [ ] |
| 代码符合项目命名规范 | [ ] |
| Git 提交成功 | [ ] |