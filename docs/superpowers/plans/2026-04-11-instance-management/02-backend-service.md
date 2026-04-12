# Task 2: GatewayInstanceService 扩展

**依赖:** Task 1 (后端 DTO 和常量定义)

**目标:** 扩展 GatewayInstanceService 接口和实现类，新增实例 CRUD 和监控指标获取方法

---

## 文件清单

- 修改: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/GatewayInstanceService.java`
- 修改: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceServiceImpl.java`

---

### Task 2.1: 扩展 Service 接口

- [ ] **Step 1: 在 GatewayInstanceService.java 中新增方法定义**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/GatewayInstanceService.java`

在现有方法之后添加以下新方法：

```java
import com.blink.gateway.admin.dto.req.DeleteInstanceReq;
import com.blink.gateway.admin.dto.req.GetInstanceDetailReq;
import com.blink.gateway.admin.dto.req.QueryInstanceReq;
import com.blink.gateway.admin.dto.req.SaveInstanceReq;
import com.blink.gateway.admin.dto.rsp.InstanceDetailRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceListRsp;

    /**
     * 分页查询实例列表（从数据库）
     *
     * @param req 查询请求参数
     * @return 实例列表响应
     */
    ResponseDTO<QueryInstanceListRsp> queryInstanceList(QueryInstanceReq req);

    /**
     * 保存实例（新增/编辑）
     *
     * @param req 保存请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> saveInstance(SaveInstanceReq req);

    /**
     * 删除实例
     *
     * @param req 删除请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> deleteInstance(DeleteInstanceReq req);

    /**
     * 获取实例详情（含监控指标）
     *
     * @param req 请求参数
     * @return 实例详情响应
     */
    ResponseDTO<InstanceDetailRsp> getInstanceDetailWithMetrics(GetInstanceDetailReq req);
```

---

### Task 2.2: 实现 Service 新方法

- [ ] **Step 2: 在 GatewayInstanceServiceImpl.java 中引入新的依赖和 DTO**

文件: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceServiceImpl.java`

在 import 区域添加：

```java
import cn.hutool.core.collection.CollUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.dto.req.DeleteInstanceReq;
import com.blink.gateway.admin.dto.req.GetInstanceDetailReq;
import com.blink.gateway.admin.dto.req.QueryInstanceReq;
import com.blink.gateway.admin.dto.req.SaveInstanceReq;
import com.blink.gateway.admin.dto.rsp.InstanceDetailRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceListRsp;
import com.blink.gateway.admin.dto.vo.ComponentHealthVO;
import com.blink.gateway.admin.dto.vo.HealthDetailVO;
import com.blink.gateway.admin.dto.vo.HttpMetricsVO;
import com.blink.gateway.admin.dto.vo.InstanceInfoVO;
import com.blink.gateway.admin.dto.vo.JvmMetricsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.blink.gateway.admin.constants.ErrCodeConstant.*;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_ONLINE;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_OFFLINE;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_SHUTDOWN;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_PREFIX;
```

在类中注入新的依赖：

```java
    @Resource
    private RedisClient redisClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
```

---

- [ ] **Step 3: 实现 queryInstanceList 方法**

在 GatewayInstanceServiceImpl.java 中添加：

```java
    @Override
    public ResponseDTO<QueryInstanceListRsp> queryInstanceList(QueryInstanceReq req) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
            
            if (StrUtil.isNotBlank(req.getServiceId())) {
                queryWrapper.like(GatewayInstanceDO::getServiceId, req.getServiceId());
            }
            if (StrUtil.isNotBlank(req.getHost())) {
                queryWrapper.like(GatewayInstanceDO::getHost, req.getHost());
            }
            if (ObjectUtil.isNotNull(req.getStatus())) {
                queryWrapper.eq(GatewayInstanceDO::getStatus, req.getStatus());
            }
            
            // 排序：按更新时间降序
            queryWrapper.orderByDesc(GatewayInstanceDO::getUpdateTime);

            // 分页查询
            Page<GatewayInstanceDO> page = new Page<>(req.getPageNum(), req.getPageSize());
            Page<GatewayInstanceDO> resultPage = gatewayInstanceMapper.selectPage(page, queryWrapper);

            // 转换为 VO
            List<InstanceInfoVO> voList = resultPage.getRecords().stream()
                    .map(this::convertToInstanceInfoVO)
                    .collect(Collectors.toList());

            // 构建响应
            QueryInstanceListRsp rsp = new QueryInstanceListRsp();
            rsp.setRows(voList);
            rsp.setTotal((int) resultPage.getTotal());
            rsp.setPageNum(req.getPageNum());
            rsp.setPageSize(req.getPageSize());
            rsp.setPages((int) resultPage.getPages());

            log.info("[GatewayInstance] 分页查询实例列表成功 | total: {}, pageNum: {}, pageSize: {}",
                    resultPage.getTotal(), req.getPageNum(), req.getPageSize());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 查询实例列表失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("查询实例列表失败：" + e.getMessage(), e, QUERY_INSTANCE_LIST_FAILED);
        }
    }
```

---

- [ ] **Step 4: 实现 saveInstance 方法**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> saveInstance(SaveInstanceReq req) {
        try {
            GatewayInstanceDO instanceDO;
            
            if (ObjectUtil.isNull(req.getId())) {
                // 新增实例
                instanceDO = new GatewayInstanceDO();
                
                // 检查是否已存在相同 host:port 的实例
                LambdaQueryWrapper<GatewayInstanceDO> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(GatewayInstanceDO::getHost, req.getHost())
                        .eq(GatewayInstanceDO::getPort, req.getPort());
                GatewayInstanceDO existInstance = gatewayInstanceMapper.selectOne(checkWrapper);
                if (ObjectUtil.isNotNull(existInstance)) {
                    BlinkException.throwBusinessException(INSTANCE_ALREADY_EXIST);
                }
                
                // 生成 instanceId
                String instanceId = req.getServiceId() + ":" + req.getHost() + ":" + req.getPort();
                instanceDO.setInstanceId(instanceId);
                instanceDO.setStatus(INSTANCE_STATUS_OFFLINE);
                
                log.info("[GatewayInstance] 新增实例 | instanceId: {}", instanceId);
            } else {
                // 编辑实例
                instanceDO = gatewayInstanceMapper.selectById(req.getId());
                if (ObjectUtil.isNull(instanceDO)) {
                    BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
                }
                
                // 检查 host:port 是否与其他实例冲突
                LambdaQueryWrapper<GatewayInstanceDO> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(GatewayInstanceDO::getHost, req.getHost())
                        .eq(GatewayInstanceDO::getPort, req.getPort())
                        .ne(GatewayInstanceDO::getId, req.getId());
                GatewayInstanceDO conflictInstance = gatewayInstanceMapper.selectOne(checkWrapper);
                if (ObjectUtil.isNotNull(conflictInstance)) {
                    BlinkException.throwBusinessException(INSTANCE_ALREADY_EXIST);
                }
                
                log.info("[GatewayInstance] 编辑实例 | id: {}", req.getId());
            }
            
            // 设置通用字段
            instanceDO.setServiceId(req.getServiceId());
            instanceDO.setHost(req.getHost());
            instanceDO.setPort(req.getPort());
            instanceDO.setUri("http://" + req.getHost() + ":" + req.getPort());
            instanceDO.setMetadata(req.getMetadata());
            
            // 保存或更新
            if (ObjectUtil.isNull(req.getId())) {
                gatewayInstanceMapper.insert(instanceDO);
            } else {
                gatewayInstanceMapper.updateById(instanceDO);
            }
            
            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 保存实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("保存实例失败：" + e.getMessage(), e, SAVE_INSTANCE_FAILED);
        }
    }
```

---

- [ ] **Step 5: 实现 deleteInstance 方法**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> deleteInstance(DeleteInstanceReq req) {
        try {
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectById(req.getId());
            if (ObjectUtil.isNull(instanceDO)) {
                BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
            }
            
            // 不允许删除在线实例
            if (INSTANCE_STATUS_ONLINE.equals(instanceDO.getStatus())) {
                BlinkException.throwBusinessException(INSTANCE_ONLINE_CANNOT_DELETE);
            }
            
            gatewayInstanceMapper.deleteById(req.getId());
            
            log.info("[GatewayInstance] 删除实例成功 | id: {}, instanceId: {}", req.getId(), instanceDO.getInstanceId());
            
            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 删除实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("删除实例失败：" + e.getMessage(), e, DELETE_INSTANCE_FAILED);
        }
    }
```

---

- [ ] **Step 6: 实现 getInstanceDetailWithMetrics 方法**

```java
    @Override
    public ResponseDTO<InstanceDetailRsp> getInstanceDetailWithMetrics(GetInstanceDetailReq req) {
        try {
            // 查询实例基本信息
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectById(req.getId());
            if (ObjectUtil.isNull(instanceDO)) {
                BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
            }
            
            InstanceDetailRsp rsp = new InstanceDetailRsp();
            
            // 基本信息
            InstanceInfoVO instanceInfo = convertToInstanceInfoVO(instanceDO);
            rsp.setInstanceInfo(instanceInfo);
            
            // 如果实例在线，从 Redis 获取实时监控指标
            if (INSTANCE_STATUS_ONLINE.equals(instanceDO.getStatus())) {
                // 获取 JVM 指标
                JvmMetricsVO jvmMetrics = getJvmMetricsFromRedis(instanceDO.getInstanceId());
                rsp.setJvmMetrics(jvmMetrics);
                
                // 获取 HTTP 指标
                HttpMetricsVO httpMetrics = getHttpMetricsFromRedis(instanceDO.getInstanceId());
                rsp.setHttpMetrics(httpMetrics);
                
                // 获取健康状态详情（从 Actuator 实时获取）
                HealthDetailVO healthDetail = fetchHealthDetail(instanceDO.getUri());
                rsp.setHealthDetail(healthDetail);
            } else {
                // 离线实例，返回最近一次采集的指标（如果有）
                JvmMetricsVO jvmMetrics = getJvmMetricsFromRedis(instanceDO.getInstanceId());
                rsp.setJvmMetrics(jvmMetrics);
                
                HttpMetricsVO httpMetrics = getHttpMetricsFromRedis(instanceDO.getInstanceId());
                rsp.setHttpMetrics(httpMetrics);
                
                // 健康状态标记为离线
                HealthDetailVO healthDetail = new HealthDetailVO();
                healthDetail.setStatus("OFFLINE");
                rsp.setHealthDetail(healthDetail);
            }
            
            log.info("[GatewayInstance] 获取实例详情成功 | id: {}", req.getId());
            
            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 获取实例详情失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取实例详情失败：" + e.getMessage(), e, GET_INSTANCE_DETAIL_FAILED);
        }
    }
```

---

- [ ] **Step 7: 添加辅助方法**

在类末尾添加以下辅助方法：

```java
    /**
     * 将 DO 转换为 InstanceInfoVO
     */
    private InstanceInfoVO convertToInstanceInfoVO(GatewayInstanceDO instanceDO) {
        InstanceInfoVO vo = BeanUtil.copyProperties(instanceDO, InstanceInfoVO.class);
        vo.setStatusDesc(getStatusDesc(instanceDO.getStatus()));
        return vo;
    }

    /**
     * 从 Redis 获取 JVM 监控指标
     */
    private JvmMetricsVO getJvmMetricsFromRedis(String instanceId) {
        try {
            String key = GATEWAY_METRICS_PREFIX + instanceId;
            Map<String, Object> metrics = redisClient.hGetAll(key);
            
            if (CollUtil.isEmpty(metrics)) {
                return null;
            }
            
            JvmMetricsVO vo = new JvmMetricsVO();
            
            // 堆内存
            Object heapUsed = metrics.get("heapUsed");
            Object heapMax = metrics.get("heapMax");
            if (ObjectUtil.isNotNull(heapUsed) && ObjectUtil.isNotNull(heapMax)) {
                vo.setHeapUsed(((Number) heapUsed).longValue());
                vo.setHeapMax(((Number) heapMax).longValue());
                if (vo.getHeapMax() > 0) {
                    double percent = (double) vo.getHeapUsed() / vo.getHeapMax() * 100;
                    vo.setHeapUsagePercent(BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP).doubleValue());
                }
            }
            
            // 非堆内存
            Object nonHeapUsed = metrics.get("nonHeapUsed");
            if (ObjectUtil.isNotNull(nonHeapUsed)) {
                vo.setNonHeapUsed(((Number) nonHeapUsed).longValue());
            }
            
            // GC 指标
            Object youngGcCount = metrics.get("youngGcCount");
            Object youngGcTime = metrics.get("youngGcTime");
            Object oldGcCount = metrics.get("oldGcCount");
            Object oldGcTime = metrics.get("oldGcTime");
            if (ObjectUtil.isNotNull(youngGcCount)) {
                vo.setYoungGcCount(((Number) youngGcCount).longValue());
            }
            if (ObjectUtil.isNotNull(youngGcTime)) {
                vo.setYoungGcTime(((Number) youngGcTime).longValue());
            }
            if (ObjectUtil.isNotNull(oldGcCount)) {
                vo.setOldGcCount(((Number) oldGcCount).longValue());
            }
            if (ObjectUtil.isNotNull(oldGcTime)) {
                vo.setOldGcTime(((Number) oldGcTime).longValue());
            }
            
            // 线程指标
            Object liveThreads = metrics.get("liveThreads");
            Object peakThreads = metrics.get("peakThreads");
            Object daemonThreads = metrics.get("daemonThreads");
            if (ObjectUtil.isNotNull(liveThreads)) {
                vo.setLiveThreads(((Number) liveThreads).intValue());
            }
            if (ObjectUtil.isNotNull(peakThreads)) {
                vo.setPeakThreads(((Number) peakThreads).intValue());
            }
            if (ObjectUtil.isNotNull(daemonThreads)) {
                vo.setDaemonThreads(((Number) daemonThreads).intValue());
            }
            
            // 时间戳
            Object timestamp = metrics.get("timestamp");
            if (ObjectUtil.isNotNull(timestamp)) {
                vo.setTimestamp(((Number) timestamp).longValue());
            }
            
            return vo;
        } catch (Exception e) {
            log.warn("[GatewayInstance] 从 Redis 获取 JVM 指标失败 | instanceId: {}, error: {}", instanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 从 Redis 获取 HTTP 监控指标
     */
    private HttpMetricsVO getHttpMetricsFromRedis(String instanceId) {
        try {
            String key = GATEWAY_METRICS_PREFIX + instanceId;
            Map<String, Object> metrics = redisClient.hGetAll(key);
            
            if (CollUtil.isEmpty(metrics)) {
                return null;
            }
            
            HttpMetricsVO vo = new HttpMetricsVO();
            
            Object totalRequests = metrics.get("totalRequests");
            Object successRequests = metrics.get("successRequests");
            Object failedRequests = metrics.get("failedRequests");
            Object avgResponseTime = metrics.get("avgResponseTime");
            
            if (ObjectUtil.isNotNull(totalRequests)) {
                vo.setTotalRequests(((Number) totalRequests).longValue());
            }
            if (ObjectUtil.isNotNull(successRequests)) {
                vo.setSuccessRequests(((Number) successRequests).longValue());
            }
            if (ObjectUtil.isNotNull(failedRequests)) {
                vo.setFailedRequests(((Number) failedRequests).longValue());
            }
            if (ObjectUtil.isNotNull(avgResponseTime)) {
                vo.setAvgResponseTime(((Number) avgResponseTime).longValue());
            }
            
            // 计算成功率
            if (ObjectUtil.isNotNull(vo.getTotalRequests()) && vo.getTotalRequests() > 0) {
                long success = ObjectUtil.isNotNull(vo.getSuccessRequests()) ? vo.getSuccessRequests() : 0;
                double rate = (double) success / vo.getTotalRequests() * 100;
                vo.setSuccessRate(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            }
            
            // 时间戳
            Object timestamp = metrics.get("timestamp");
            if (ObjectUtil.isNotNull(timestamp)) {
                vo.setTimestamp(((Number) timestamp).longValue());
            }
            
            return vo;
        } catch (Exception e) {
            log.warn("[GatewayInstance] 从 Redis 获取 HTTP 指标失败 | instanceId: {}, error: {}", instanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 从 Actuator 获取健康状态详情
     */
    private HealthDetailVO fetchHealthDetail(String uri) {
        try {
            String healthUrl = uri + "/actuator/health";
            String response = webClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (StrUtil.isBlank(response)) {
                return null;
            }
            
            Map<String, Object> healthMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            
            HealthDetailVO vo = new HealthDetailVO();
            vo.setStatus((String) healthMap.get("status"));
            
            // 解析各组件健康状态
            Object componentsObj = healthMap.get("components");
            if (ObjectUtil.isNotNull(componentsObj) && componentsObj instanceof Map) {
                Map<String, Object> components = (Map<String, Object>) componentsObj;
                List<ComponentHealthVO> componentList = new ArrayList<>();
                
                for (Map.Entry<String, Object> entry : components.entrySet()) {
                    ComponentHealthVO componentVO = new ComponentHealthVO();
                    componentVO.setName(entry.getKey());
                    
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> componentData = (Map<String, Object>) entry.getValue();
                        componentVO.setStatus((String) componentData.get("status"));
                        componentVO.setDetails((Map<String, Object>) componentData.get("details"));
                    }
                    
                    componentList.add(componentVO);
                }
                
                vo.setComponents(componentList);
            }
            
            return vo;
        } catch (Exception e) {
            log.warn("[GatewayInstance] 获取健康状态详情失败 | uri: {}, error: {}", uri, e.getMessage());
            return null;
        }
    }
```

---

### Task 2.3: 提交更改

- [ ] **Step 8: Git 提交**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/GatewayInstanceService.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceServiceImpl.java
git commit -m "feat(instance): 扩展 GatewayInstanceService 实例 CRUD 和监控方法

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验收检查

| 检查项 | 状态 |
|--------|------|
| Service 接口新增 4 个方法 | [ ] |
| ServiceImpl 实现所有新方法 | [ ] |
| queryInstanceList 支持分页和筛选 | [ ] |
| saveInstance 支持新增和编辑 | [ ] |
| deleteInstance 检查实例状态 | [ ] |
| getInstanceDetailWithMetrics 返回完整指标 | [ ] |
| 辅助方法正确实现 | [ ] |
| Git 提交成功 | [ ] |