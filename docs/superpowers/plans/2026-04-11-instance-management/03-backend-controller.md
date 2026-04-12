# Task 3: GatewayInstanceController 新增接口

**依赖:** Task 2 (GatewayInstanceService 扩展)

**目标:** 在 GatewayInstanceController 中新增实例管理接口

---

## 文件清单

- 修改: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/GatewayInstanceController.java`

---

### Task 3.1: 新增 Controller 接口

- [ ] **Step 1: 在 GatewayInstanceController.java 中引入新的 DTO**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/GatewayInstanceController.java`

在 import 区域添加：

```java
import com.blink.gateway.admin.dto.req.DeleteInstanceReq;
import com.blink.gateway.admin.dto.req.GetInstanceDetailReq;
import com.blink.gateway.admin.dto.req.QueryInstanceReq;
import com.blink.gateway.admin.dto.req.SaveInstanceReq;
import com.blink.gateway.admin.dto.rsp.InstanceDetailRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceListRsp;
```

---

- [ ] **Step 2: 添加 queryInstanceList 接口**

在 Controller 类中添加：

```java
    /**
     * 分页查询实例列表（从数据库）
     *
     * @param reqDto 请求参数
     * @return 实例列表
     */
    @PostMapping("/queryInstanceList")
    public ResponseDTO<QueryInstanceListRsp> queryInstanceList(@RequestBody @Validated RequestDTO<QueryInstanceReq> reqDto) {
        return gatewayInstanceService.queryInstanceList(reqDto.getBody());
    }
```

---

- [ ] **Step 3: 添加 saveInstance 接口**

```java
    /**
     * 保存实例（新增/编辑）
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/saveInstance")
    public ResponseDTO<EmptyBody> saveInstance(@RequestBody @Validated RequestDTO<SaveInstanceReq> reqDto) {
        gatewayInstanceService.saveInstance(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }
```

---

- [ ] **Step 4: 添加 deleteInstance 接口**

```java
    /**
     * 删除实例
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/deleteInstance")
    public ResponseDTO<EmptyBody> deleteInstance(@RequestBody @Validated RequestDTO<DeleteInstanceReq> reqDto) {
        gatewayInstanceService.deleteInstance(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }
```

---

- [ ] **Step 5: 添加 getInstanceDetailWithMetrics 接口**

```java
    /**
     * 获取实例详情（含监控指标）
     *
     * @param reqDto 请求参数
     * @return 实例详情
     */
    @PostMapping("/getInstanceDetailWithMetrics")
    public ResponseDTO<InstanceDetailRsp> getInstanceDetailWithMetrics(@RequestBody @Validated RequestDTO<GetInstanceDetailReq> reqDto) {
        return gatewayInstanceService.getInstanceDetailWithMetrics(reqDto.getBody());
    }
```

---

### Task 3.2: 提交更改

- [ ] **Step 6: Git 提交**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/GatewayInstanceController.java
git commit -m "feat(instance): GatewayInstanceController 新增实例管理接口

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验收检查

| 检查项 | 状态 |
|--------|------|
| queryInstanceList 接口已添加 | [ ] |
| saveInstance 接口已添加 | [ ] |
| deleteInstance 接口已添加 | [ ] |
| getInstanceDetailWithMetrics 接口已添加 | [ ] |
| 所有接口使用 POST 方法 | [ ] |
| 所有接口使用 RequestDTO/ResponseDTO 包裹 | [ ] |
| Git 提交成功 | [ ] |