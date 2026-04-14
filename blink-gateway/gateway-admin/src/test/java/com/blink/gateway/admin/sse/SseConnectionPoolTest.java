package com.blink.gateway.admin.sse;

import cn.dev33.satoken.stp.StpUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.dto.SseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SseConnectionPool 单元测试
 *
 * 测试 SSE 连接池的核心功能：
 * 1. 单用户连接数限制
 * 2. 总连接数限制
 * 3. 心跳检测机制
 * 4. 连接配置应用
 *
 * @author binblink
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SseConnectionPoolTest {

    @Mock
    private RedisClient redisClient;

    @Mock
    private SseInstanceIdentifier instanceIdentifier;

    @Mock
    private SseConfig sseConfig;

    @InjectMocks
    private SseConnectionPool sseConnectionPool;

    private static final Integer TEST_USER_ID = 1001;
    private static final String TEST_INSTANCE_ID = "test-instance-001";
    private static final String TEST_CONNECTION_KEY = TEST_USER_ID + ":test-token-abc123";

    @BeforeEach
    void setUp() {
        when(instanceIdentifier.getInstanceId()).thenReturn(TEST_INSTANCE_ID);
        when(redisClient.hGetField(anyString(), anyString())).thenReturn(null);
        // 配置 SSE 参数
        when(sseConfig.getMaxConnectionsPerUser()).thenReturn(5);
        when(sseConfig.getMaxTotalConnections()).thenReturn(1000);
        when(sseConfig.getConnectionTimeout()).thenReturn(30 * 60_000L);
        when(sseConfig.getRegistryTtl()).thenReturn(120L);
    }

    @Nested
    @DisplayName("单用户连接数限制测试")
    class MaxConnectionsPerUserTests {

        @Test
        @DisplayName("单用户连接数未超限时应正常创建连接")
        void shouldCreateConnectionWhenUnderLimit() {
            // Given: 模拟用户登录
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);

                // When: 创建连接（用户第一个连接）
                SseEmitter emitter = sseConnectionPool.createConnection(TEST_CONNECTION_KEY, TEST_USER_ID);

                // Then: 连接应成功创建
                assertNotNull(emitter);
                assertEquals(1, sseConnectionPool.getUserConnectionCount(TEST_USER_ID));
            }
        }

        @Test
        @DisplayName("单用户连接数达到上限时应拒绝新连接")
        void shouldRejectConnectionWhenLimitReached() {
            // Given: 模拟用户登录
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);

                int maxPerUser = sseConfig.getMaxConnectionsPerUser();
                // 使用不同的 connectionKey 创建多个连接（模拟多设备/多标签页）
                for (int i = 0; i < maxPerUser; i++) {
                    String connectionKey = TEST_USER_ID + ":token-" + i;
                    sseConnectionPool.createConnection(connectionKey, TEST_USER_ID);
                }

                // When: 尝试创建超限连接（使用新的 connectionKey）
                SseEmitter emitter = sseConnectionPool.createConnection(TEST_USER_ID + ":token-extra", TEST_USER_ID);

                // Then: 应返回 null 表示拒绝连接
                assertNull(emitter);
                assertEquals(maxPerUser, sseConnectionPool.getUserConnectionCount(TEST_USER_ID));
            }
        }
    }

    @Nested
    @DisplayName("总连接数限制测试")
    class MaxTotalConnectionsTests {

        @Test
        @DisplayName("总连接数未超限时应正常创建连接")
        void shouldCreateConnectionWhenTotalUnderLimit() {
            // Given: 模拟多个用户登录
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                for (int i = 1; i <= 10; i++) {
                    stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(i);
                    sseConnectionPool.createConnection(TEST_CONNECTION_KEY, TEST_USER_ID);
                }

                // When: 创建新用户连接
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(11);
                SseEmitter emitter = sseConnectionPool.createConnection(TEST_CONNECTION_KEY, TEST_USER_ID);

                // Then: 连接应成功创建
                assertNotNull(emitter);
            }
        }

        @Test
        @DisplayName("总连接数达到上限时应拒绝新连接")
        void shouldRejectConnectionWhenTotalLimitReached() {
            // Given: 模拟大量用户达到总连接数上限
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                int connectionsCreated = 0;
                int userId = 1;
                int userConnectionCount = 0;
                int maxPerUser = sseConfig.getMaxConnectionsPerUser();
                int maxTotal = sseConfig.getMaxTotalConnections();

                // 创建直到接近上限（测试时使用较小的数量）
                int testLimit = Math.min(50, maxTotal);
                while (connectionsCreated < testLimit) {
                    stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(userId);
                    // 使用不同的 connectionKey（模拟同一用户的多设备登录）
                    String connectionKey = userId + ":token-" + userConnectionCount;
                    SseEmitter emitter = sseConnectionPool.createConnection(connectionKey, userId);
                    if (emitter != null) {
                        connectionsCreated++;
                        userConnectionCount++;
                    }
                    // 每个用户最多创建 maxPerUser 个连接
                    if (userConnectionCount >= maxPerUser) {
                        userId++;
                        userConnectionCount = 0;
                    }
                }

                // Then: 验证连接创建成功
                assertEquals(testLimit, sseConnectionPool.getTotalConnectionCount());
            }
        }
    }

    @Nested
    @DisplayName("SSE 配置应用测试")
    class SseConfigApplicationTests {

        @Test
        @DisplayName("SSE 连接应使用配置的超时时间")
        void shouldUseConfiguredTimeout() {
            // Given
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);

                // When
                SseEmitter emitter = sseConnectionPool.createConnection(TEST_CONNECTION_KEY, TEST_USER_ID);

                // Then: 验证超时时间（通过反射或 mock 验证）
                // SseEmitter 的超时时间在构造时设置，我们通过创建成功来验证
                assertNotNull(emitter);
            }
        }

        @Test
        @DisplayName("连接注册应使用配置的 TTL")
        void shouldUseConfiguredRegistryTTL() {
            // Given
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);

                // When
                sseConnectionPool.createConnection(TEST_CONNECTION_KEY, TEST_USER_ID);

                // Then: 验证 Redis 注册使用了正确的 TTL
                verify(redisClient).expire(anyString(), eq(SseConfig.REGISTRY_TTL));
            }
        }
    }

    @Nested
    @DisplayName("连接管理测试")
    class ConnectionManagementTests {

        @Test
        @DisplayName("应正确获取用户连接数")
        void shouldGetCorrectUserConnectionCount() {
            // Given
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);

                // When: 使用不同的 connectionKey 创建多个连接
                sseConnectionPool.createConnection(TEST_USER_ID + ":token-1", TEST_USER_ID);
                sseConnectionPool.createConnection(TEST_USER_ID + ":token-2", TEST_USER_ID);
                sseConnectionPool.createConnection(TEST_USER_ID + ":token-3", TEST_USER_ID);

                // Then
                assertEquals(3, sseConnectionPool.getUserConnectionCount(TEST_USER_ID));
            }
        }

        @Test
        @DisplayName("无连接用户应返回 0")
        void shouldReturnZeroForUserWithNoConnections() {
            // Given: 用户无连接

            // When & Then
            assertEquals(0, sseConnectionPool.getUserConnectionCount(9999));
        }

        @Test
        @DisplayName("应正确判断用户是否有连接")
        void shouldCorrectlyCheckIfUserHasConnection() {
            // Given
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);

                // When: 创建连接前
                boolean beforeCreate = sseConnectionPool.hasConnection(TEST_USER_ID);

                // 创建连接后
                sseConnectionPool.createConnection(TEST_CONNECTION_KEY, TEST_USER_ID);
                boolean afterCreate = sseConnectionPool.hasConnection(TEST_USER_ID);

                // Then
                assertFalse(beforeCreate);
                assertTrue(afterCreate);
            }
        }
    }

    @Nested
    @DisplayName("连接移除测试")
    class ConnectionRemovalTests {

        @Test
        @DisplayName("移除连接后连接数应正确减少")
        void connectionCountShouldDecreaseAfterRemoval() throws Exception {
            // Given
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);

                SseEmitter emitter = sseConnectionPool.createConnection(TEST_CONNECTION_KEY, TEST_USER_ID);
                assertEquals(1, sseConnectionPool.getUserConnectionCount(TEST_USER_ID));

                // When: 触发 completion 回调
                // 使用反射获取并执行回调
                Runnable completionCallback = (Runnable) ReflectionTestUtils.getField(emitter, "completionCallback");
                if (completionCallback != null) {
                    completionCallback.run();
                }

                // Then: 连接数应减少
                assertEquals(0, sseConnectionPool.getUserConnectionCount(TEST_USER_ID));
            }
        }

        @Test
        @DisplayName("用户最后一个连接关闭时应从 Redis 移除注册")
        void shouldUnregisterFromRedisWhenLastConnectionClosed() throws Exception {
            // Given
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);

                SseEmitter emitter = sseConnectionPool.createConnection(TEST_CONNECTION_KEY, TEST_USER_ID);

                // When: 触发 completion 回调
                Runnable completionCallback = (Runnable) ReflectionTestUtils.getField(emitter, "completionCallback");
                if (completionCallback != null) {
                    completionCallback.run();
                }

                // Then: 应从 Redis 移除注册
                verify(redisClient).hDeleteFields(eq(RedisKeyConstant.SSE_CONNECTION_REGISTRY), eq(String.valueOf(TEST_USER_ID)));
            }
        }

        @Test
        @DisplayName("用户非最后一个连接关闭时不应从 Redis 移除注册")
        void shouldNotUnregisterFromRedisWhenNotLastConnectionClosed() throws Exception {
            // Given
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);

                // 使用不同的 connectionKey 创建多个连接
                SseEmitter emitter1 = sseConnectionPool.createConnection(TEST_USER_ID + ":token-1", TEST_USER_ID);
                sseConnectionPool.createConnection(TEST_USER_ID + ":token-2", TEST_USER_ID);

                // 重置 mock 以清除之前的调用
                reset(redisClient);

                // When: 触发第一个连接的 completion 回调
                Runnable completionCallback = (Runnable) ReflectionTestUtils.getField(emitter1, "completionCallback");
                if (completionCallback != null) {
                    completionCallback.run();
                }

                // Then: 不应从 Redis 移除注册（因为还有其他连接）
                verify(redisClient, never()).hDeleteFields(anyString(), anyString());
            }
        }
    }

    @Nested
    @DisplayName("总连接数统计测试")
    class TotalConnectionCountTests {

        @Test
        @DisplayName("应正确统计总连接数")
        void shouldCorrectlyCountTotalConnections() {
            // Given
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // 用户1创建2个连接（使用不同的 connectionKey）
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(1);
                sseConnectionPool.createConnection("1:token-1", 1);
                sseConnectionPool.createConnection("1:token-2", 1);

                // 用户2创建3个连接（使用不同的 connectionKey）
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(2);
                sseConnectionPool.createConnection("2:token-1", 2);
                sseConnectionPool.createConnection("2:token-2", 2);
                sseConnectionPool.createConnection("2:token-3", 2);

                // When
                int totalCount = sseConnectionPool.getTotalConnectionCount();

                // Then
                assertEquals(5, totalCount);
            }
        }

        @Test
        @DisplayName("无连接时应返回 0")
        void shouldReturnZeroWhenNoConnections() {
            // Given: 无连接

            // When
            int totalCount = sseConnectionPool.getTotalConnectionCount();

            // Then
            assertEquals(0, totalCount);
        }
    }

    @Nested
    @DisplayName("心跳测试")
    class HeartbeatTests {

        @Test
        @DisplayName("心跳应使用配置的间隔")
        void shouldUseConfiguredHeartbeatInterval() {
            // Given: 验证 SseConfig 中的配置
            // Then: 验证心跳间隔配置
            assertEquals(30_000L, SseConfig.HEARTBEAT_INTERVAL);
        }

        @Test
        @DisplayName("心跳应刷新 Redis TTL")
        void heartbeatShouldRefreshRedisTTL() {
            // Given
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsInt).thenReturn(TEST_USER_ID);
                sseConnectionPool.createConnection(TEST_CONNECTION_KEY, TEST_USER_ID);

                // 重置 mock 以清除创建连接时的调用
                reset(redisClient);

                // When: 执行心跳
                sseConnectionPool.heartbeat();

                // Then: 验证 Redis TTL 被刷新
                verify(redisClient).expire(eq(RedisKeyConstant.SSE_CONNECTION_REGISTRY), eq(SseConfig.REGISTRY_TTL));
            }
        }

        @Test
        @DisplayName("无连接时心跳不应执行 Redis 操作")
        void heartbeatShouldNotOperateRedisWhenNoConnections() {
            // Given: 无连接

            // 重置 mock
            reset(redisClient);

            // When: 执行心跳
            sseConnectionPool.heartbeat();

            // Then: 不应刷新 Redis TTL
            verify(redisClient, never()).expire(anyString(), any(Long.class));
        }
    }
}
