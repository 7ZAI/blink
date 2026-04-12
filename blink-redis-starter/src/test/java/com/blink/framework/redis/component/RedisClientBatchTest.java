package com.blink.framework.redis.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisClient 批量操作与Lua脚本单元测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisClientBatchTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisTemplate<String, Object> streamRedisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedisSerializer<?> keySerializer;

    @Mock
    private RedisSerializer<?> valueSerializer;

    private RedisClient redisClient;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(redisTemplate.getKeySerializer()).thenReturn((RedisSerializer) keySerializer);
        when(redisTemplate.getValueSerializer()).thenReturn((RedisSerializer) valueSerializer);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisClient = new RedisClient(redisTemplate, streamRedisTemplate);
    }

    // ==================== 批量操作测试 ====================

    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTests {

        @Test
        @DisplayName("05-01: 批量获取多个key")
        void testMultiGet() {
            // Given
            List<String> keys = Arrays.asList("key1", "key2", "key3");
            List<Object> expectedValues = Arrays.asList("value1", "value2", "value3");
            when(valueOperations.multiGet(keys)).thenReturn(expectedValues);

            // When
            List<Object> result = redisClient.multiGet(keys);

            // Then
            assertEquals(expectedValues, result);
            verify(valueOperations).multiGet(keys);
        }

        @Test
        @DisplayName("05-02: 批量获取并转为Map")
        void testBatchGet() {
            // Given
            List<String> keys = Arrays.asList("key1", "key2");
            List<Object> values = Arrays.asList("value1", "value2");
            when(valueOperations.multiGet(keys)).thenReturn(values);

            // When
            Map<String, Object> result = redisClient.batchGet(keys);

            // Then
            assertEquals(2, result.size());
            assertEquals("value1", result.get("key1"));
            assertEquals("value2", result.get("key2"));
        }

        @Test
        @DisplayName("05-03: 空key列表返回空Map")
        void testBatchGet_EmptyKeys() {
            // Given
            List<String> keys = Collections.emptyList();

            // When
            Map<String, Object> result = redisClient.batchGet(keys);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("05-04: 批量设置多个key")
        void testBatchSet() {
            // Given
            Map<String, Object> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", "value2");

            // When
            redisClient.batchSet(map);

            // Then
            verify(valueOperations).multiSet(map);
        }

        @Test
        @DisplayName("05-05: 批量设置带过期时间")
        @SuppressWarnings("unchecked")
        void testBatchSetWithExpire() {
            // Given
            Map<String, Object> keyValueMap = new HashMap<>();
            keyValueMap.put("key1", "value1");
            keyValueMap.put("key2", "value2");
            long expire = 60L;
            TimeUnit timeUnit = TimeUnit.SECONDS;

            // Mock keySerializer to return byte array
            when(((RedisSerializer<String>) keySerializer).serialize(anyString())).thenReturn(new byte[0]);
            when(valueSerializer.serialize(any())).thenReturn(new byte[0]);
            when(redisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(Collections.emptyList());

            // When
            Boolean result = redisClient.batchSetWithExpire(keyValueMap, expire, timeUnit);

            // Then
            assertTrue(result);
            verify(redisTemplate).executePipelined(any(RedisCallback.class));
        }

        @Test
        @DisplayName("05-06: 所有key不存在时设置成功")
        void testMultiSetIfAbsent_Success() {
            // Given
            Map<String, Object> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", "value2");
            when(valueOperations.multiSetIfAbsent(map)).thenReturn(true);

            // When
            Boolean result = redisClient.multiSetIfAbsent(map);

            // Then
            assertTrue(result);
            verify(valueOperations).multiSetIfAbsent(map);
        }

        @Test
        @DisplayName("05-07: 有key存在时设置失败")
        void testMultiSetIfAbsent_Fail() {
            // Given
            Map<String, Object> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", "value2");
            when(valueOperations.multiSetIfAbsent(map)).thenReturn(false);

            // When
            Boolean result = redisClient.multiSetIfAbsent(map);

            // Then
            assertFalse(result);
        }
    }

    // ==================== 批量删除测试 ====================

    @Nested
    @DisplayName("批量删除测试")
    class BatchDeleteTests {

        @Test
        @DisplayName("05-08: 按前缀删除（已废弃方法）")
        void testDeleteByPrefix_Deprecated() {
            // Given
            String pattern = "test-prefix";
            Set<String> keys = new HashSet<>(Arrays.asList("test-prefix-1", "test-prefix-2"));
            when(redisTemplate.keys("test-prefix*")).thenReturn(keys);
            when(redisTemplate.delete(keys)).thenReturn(2L);

            // When
            Long result = redisClient.deleteByPrefix(pattern);

            // Then
            assertEquals(2L, result);
        }

        @Test
        @DisplayName("05-09: 按前缀扫描删除")
        void testDeleteByPrefixScan() {
            // Given
            String pattern = "test-prefix";

            // Mock the scan operation
            Cursor<String> mockCursor = mock(Cursor.class);
            when(mockCursor.hasNext()).thenReturn(false);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mockCursor);

            // When
            Long result = redisClient.deleteByPrefixScan(pattern);

            // Then
            assertEquals(0L, result);
            verify(redisTemplate).scan(any(ScanOptions.class));
        }

        @Test
        @DisplayName("05-10: 按前缀管道删除")
        void testDeleteByPrefixPipeline() {
            // Given
            String pattern = "test-prefix";

            // Mock pipeline execution
            when(redisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(Collections.emptyList());

            // When
            Long result = redisClient.deleteByPrefixPipeline(pattern);

            // Then
            assertEquals(0L, result);
            verify(redisTemplate).executePipelined(any(RedisCallback.class));
        }

        @Test
        @DisplayName("05-11: 按前缀Lua删除")
        void testDeleteByPrefixLua() {
            // Given
            String pattern = "test-prefix";
            when(redisTemplate.execute(any(RedisScript.class), eq(null), eq("test-prefix*"))).thenReturn(5L);

            // When
            Long result = redisClient.deleteByPrefixLua(pattern);

            // Then
            assertEquals(5L, result);
        }

        @Test
        @DisplayName("05-12: 安全删除-成功")
        void testDeleteByPrefixSafely_Success() {
            // Given
            String pattern = "test-prefix";

            // Mock pipeline execution
            when(redisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(Collections.emptyList());

            // When
            Boolean result = redisClient.deleteByPrefixSafely(pattern, 3);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("05-14: 删除指定key列表")
        void testDeleteKeys() {
            // Given
            List<String> keys = Arrays.asList("key1", "key2", "key3");
            when(redisTemplate.delete(keys)).thenReturn(3L);

            // When
            Long result = redisClient.deleteKeys(keys);

            // Then
            assertEquals(3L, result);
            verify(redisTemplate).delete(keys);
        }

        @Test
        @DisplayName("05-15: 统计前缀匹配数量")
        void testCountByPrefix() {
            // Given
            String pattern = "test-prefix";

            // Mock cursor with 5 items
            Cursor<String> mockCursor = mock(Cursor.class);
            when(mockCursor.hasNext()).thenReturn(true, true, true, true, true, false);
            when(mockCursor.next()).thenReturn("key1", "key2", "key3", "key4", "key5");
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mockCursor);

            // When
            Long result = redisClient.countByPrefix(pattern);

            // Then
            assertEquals(5L, result);
        }

        @Test
        @DisplayName("05-16: 分页获取前缀匹配key")
        void testGetKeysByPrefix() {
            // Given
            String pattern = "test-prefix";
            int pageSize = 10;
            int page = 0;

            Cursor<String> mockCursor = mock(Cursor.class);
            when(mockCursor.hasNext()).thenReturn(true, true, false);
            when(mockCursor.next()).thenReturn("key1", "key2");
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mockCursor);

            // When
            List<String> result = redisClient.getKeysByPrefix(pattern, pageSize, page);

            // Then
            assertEquals(2, result.size());
        }
    }

    // ==================== Lua脚本测试 ====================

    @Nested
    @DisplayName("Lua脚本测试")
    class LuaScriptTests {

        @Test
        @DisplayName("05-17: 执行RedisScript对象")
        @SuppressWarnings("unchecked")
        void testExecute_WithRedisScript() {
            // Given
            RedisScript<Long> script = mock(RedisScript.class);
            List<String> keys = Arrays.asList("key1");
            Object[] args = {"arg1"};
            when(redisTemplate.execute(eq(script), eq(keys), eq(args))).thenReturn(1L);

            // When
            Long result = redisClient.execute(script, keys, args);

            // Then
            assertEquals(1L, result);
            verify(redisTemplate).execute(eq(script), eq(keys), eq(args));
        }

        @Test
        @DisplayName("05-18: 执行Lua脚本字符串")
        @SuppressWarnings("unchecked")
        void testExecute_WithScriptString() {
            // Given
            String scriptStr = "return redis.call('get', KEYS[1])";
            List<String> keys = Arrays.asList("key1");
            List<Object> args = Collections.emptyList();
            when(redisTemplate.execute(any(RedisScript.class), eq(keys), any(Object[].class))).thenReturn("value1");

            // When
            Object result = redisClient.execute(scriptStr, keys, args);

            // Then
            assertEquals("value1", result);
        }

        @Test
        @DisplayName("05-19: 带序列化器执行脚本")
        @SuppressWarnings("unchecked")
        void testExecute_WithSerializer() {
            // Given
            RedisScript<Long> script = mock(RedisScript.class);
            RedisSerializer<Long> resultSerializer = mock(RedisSerializer.class);
            List<String> keys = Arrays.asList("key1");
            Object[] args = {"arg1"};

            when(redisTemplate.execute(eq(script), any(RedisSerializer.class), any(RedisSerializer.class), eq(keys), eq(args)))
                    .thenReturn(1L);

            // When
            Long result = redisClient.execute(script, resultSerializer, keys, args);

            // Then
            assertEquals(1L, result);
        }

        @Test
        @DisplayName("05-20: 使用String序列化器")
        @SuppressWarnings("unchecked")
        void testExecuteWithStringSerializer() {
            // Given
            RedisScript<?> script = mock(RedisScript.class);
            List<String> keys = Arrays.asList("key1");
            Object[] args = {"arg1"};

            // Mock keySerializer
            when(((RedisSerializer<String>) keySerializer).serialize(anyString())).thenReturn(new byte[0]);

            // Mock RedisCallback execution
            when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(1L);

            // When
            Object result = redisClient.executeWithStringSerializer(script, keys, args);

            // Then
            assertNotNull(result);
            verify(redisTemplate).execute(any(RedisCallback.class));
        }
    }

    // ==================== 管道操作测试 ====================

    @Nested
    @DisplayName("管道操作测试")
    class PipelineTests {

        @Test
        @DisplayName("05-21: 管道执行RedisCallback")
        void testExecutePipelined_WithRedisCallback() {
            // Given
            RedisCallback<Object> callback = connection -> null;
            when(redisTemplate.executePipelined(callback)).thenReturn(Collections.singletonList("result"));

            // When
            Object result = redisClient.executePipelined(callback);

            // Then
            assertNotNull(result);
            verify(redisTemplate).executePipelined(callback);
        }

        @Test
        @DisplayName("05-22: 管道执行SessionCallback")
        void testExecutePipelined_WithSessionCallback() {
            // Given
            SessionCallback<Object> callback = mock(SessionCallback.class);
            List<Object> expectedResults = Arrays.asList("result1", "result2");
            when(redisTemplate.executePipelined(callback)).thenReturn(expectedResults);

            // When
            List<Object> result = redisClient.executePipelined(callback);

            // Then
            assertEquals(expectedResults, result);
            verify(redisTemplate).executePipelined(callback);
        }
    }

    // ==================== 扫描操作测试 ====================

    @Nested
    @DisplayName("扫描操作测试")
    class ScanTests {

        @Test
        @DisplayName("05-23: 扫描匹配key")
        void testScan() {
            // Given
            String pattern = "test-*";
            long count = 100;

            Cursor<String> mockCursor = mock(Cursor.class);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mockCursor);

            // When
            Cursor<String> result = redisClient.scan(pattern, count);

            // Then
            assertNotNull(result);
            verify(redisTemplate).scan(any(ScanOptions.class));
        }
    }
}
