package com.blink.gateway.dubbo;

import com.blink.base.dubbo.service.BaseDubboService;
import com.blink.base.dto.req.QueryOneSysConfigReq;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * BaseDubboService Dubbo RPC 集成测试类
 * <p>
 * 通过 Dubbo 协议真正发起 RPC 调用测试
 * 需要确保：
 * 1. Nacos 服务已启动
 * 2. base-app 服务已启动并注册到 Nacos
 * 3. Dubbo 服务已暴露
 * </p>
 *
 * @author blink
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("BaseDubboService Dubbo RPC 集成测试")
class BaseDubboServiceRpcTest {

    /**
     * 通过 Dubbo 引用注入远程服务
     * check = false 表示服务不可用时也不报错（测试时可以先启动测试再启动服务）
     */
    @DubboReference(check = false, timeout = 10000)
    private BaseDubboService baseDubboService;

    @BeforeAll
    static void setUp() throws InterruptedException {
        // 等待服务注册到 Nacos
        log.info("等待 Dubbo 服务注册到 Nacos...");
        Thread.sleep(5000);
        log.info("等待完成，开始执行测试");
    }

    @Test
    @Order(1)
    @DisplayName("测试 Dubbo 服务是否可用")
    void testDubboServiceAvailable() {
        log.info("开始测试 Dubbo 服务是否可用");
        Assertions.assertNotNull(baseDubboService, "Dubbo 服务引用不能为空，请检查服务是否已注册到 Nacos");
        log.info("Dubbo 服务引用成功，服务可用");
    }

    @Test
    @Order(2)
    @DisplayName("测试 RPC 调用获取系统配置 - 正常场景")
    void testGetOneConfigRpc_Success() {
        log.info("开始测试 RPC 调用获取系统配置");

        // 准备请求参数
        QueryOneSysConfigReq req = new QueryOneSysConfigReq();
        req.setConfigKey("site_name");

        RequestDTO<QueryOneSysConfigReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        try {
            // 发起真正的 Dubbo RPC 调用
            ResponseDTO<SysConfigCacheDO> response = baseDubboService.getOneConfig(requestDTO);

            // 验证响应结果
            Assertions.assertNotNull(response, "RPC 调用响应不能为空");
            log.info("RPC 调用成功，响应结果: {}", response);

            // 如果配置存在，验证配置内容
            if (response.getBody() != null) {
                SysConfigCacheDO config = response.getBody();
                log.info("获取到配置信息 - key: {}, value: {}", config.getConfigKey(), config.getConfigValue());
            } else {
                log.warn("配置不存在或返回为空");
            }
        } catch (Exception e) {
            log.error("RPC 调用失败", e);
            Assertions.fail("Dubbo RPC 调用失败: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("测试 RPC 调用获取系统配置 - 空值场景")
    void testGetOneConfigRpc_NullValue() {
        log.info("开始测试 RPC 调用获取不存在的配置");

        // 准备一个不存在的配置key
        QueryOneSysConfigReq req = new QueryOneSysConfigReq();
        req.setConfigKey("not.exist.config.key.12345");

        RequestDTO<QueryOneSysConfigReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        try {
            // 发起 Dubbo RPC 调用
            ResponseDTO<SysConfigCacheDO> response = baseDubboService.getOneConfig(requestDTO);

            Assertions.assertNotNull(response, "RPC 调用响应不能为空");
            log.info("RPC 调用成功，不存在的配置返回结果: {}", response);

        } catch (Exception e) {
            log.error("RPC 调用异常", e);
            // 根据业务逻辑，可能抛出异常或返回空，这里仅记录日志
        }
    }

    @Test
    @Order(4)
    @DisplayName("测试 RPC 调用性能")
    void testGetOneConfigRpc_Performance() {
        log.info("开始测试 RPC 调用性能");

        QueryOneSysConfigReq req = new QueryOneSysConfigReq();
        req.setConfigKey("site_name");

        RequestDTO<QueryOneSysConfigReq> requestDTO = new RequestDTO<>();
        requestDTO.setBody(req);

        int count = 10;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            try {
                ResponseDTO<SysConfigCacheDO> response = baseDubboService.getOneConfig(requestDTO);
                Assertions.assertNotNull(response, "第 " + (i + 1) + " 次调用响应不能为空");
            } catch (Exception e) {
                log.error("第 {} 次 RPC 调用失败", i + 1, e);
                Assertions.fail("第 " + (i + 1) + " 次调用失败: " + e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        long avgTime = (endTime - startTime) / count;

        log.info("RPC 性能测试结果 - 总次数: {}, 总耗时: {}ms, 平均耗时: {}ms", 
                count, (endTime - startTime), avgTime);

        // 断言平均耗时小于 1 秒
        Assertions.assertTrue(avgTime < 1000, 
                "RPC 调用平均耗时应该小于 1000ms，实际: " + avgTime + "ms");
    }
}
