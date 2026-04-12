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
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisClient List/Set/ZSet 操作单元测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisClientCollectionTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisTemplate<String, Object> streamRedisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

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
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        redisClient = new RedisClient(redisTemplate, streamRedisTemplate);
    }

    // ==================== List 操作测试 ====================

    @Nested
    @DisplayName("List 操作测试")
    class ListTests {

        @Test
        @DisplayName("03-01: 左侧推入多个值")
        void testLPush() {
            // Given
            String key = "list-key";
            Object[] values = {"value1", "value2", "value3"};
            when(listOperations.leftPushAll(key, values)).thenReturn(3L);

            // When
            Long result = redisClient.lPush(key, values);

            // Then
            assertEquals(3L, result);
            verify(listOperations).leftPushAll(key, values);
        }

        @Test
        @DisplayName("03-02: 右侧推入多个值")
        void testRPush() {
            // Given
            String key = "list-key";
            Object[] values = {"value1", "value2", "value3"};
            when(listOperations.rightPushAll(key, values)).thenReturn(3L);

            // When
            Long result = redisClient.rPush(key, values);

            // Then
            assertEquals(3L, result);
            verify(listOperations).rightPushAll(key, values);
        }

        @Test
        @DisplayName("03-03: 左侧弹出")
        void testLPop() {
            // Given
            String key = "list-key";
            Object expectedValue = "value1";
            when(listOperations.leftPop(key)).thenReturn(expectedValue);

            // When
            Object result = redisClient.lPop(key);

            // Then
            assertEquals(expectedValue, result);
            verify(listOperations).leftPop(key);
        }

        @Test
        @DisplayName("03-04: 右侧弹出")
        void testRPop() {
            // Given
            String key = "list-key";
            Object expectedValue = "value3";
            when(listOperations.rightPop(key)).thenReturn(expectedValue);

            // When
            Object result = redisClient.rPop(key);

            // Then
            assertEquals(expectedValue, result);
            verify(listOperations).rightPop(key);
        }

        @Test
        @DisplayName("03-05: 带超时的左侧弹出")
        void testLPopWithTimeout() {
            // Given
            String key = "list-key";
            long timeout = 5L;
            TimeUnit unit = TimeUnit.SECONDS;
            Object expectedValue = "value1";
            when(listOperations.leftPop(key, timeout, unit)).thenReturn(expectedValue);

            // When
            Object result = redisClient.lPop(key, timeout, unit);

            // Then
            assertEquals(expectedValue, result);
            verify(listOperations).leftPop(key, timeout, unit);
        }

        @Test
        @DisplayName("03-06: 带超时的右侧弹出")
        void testRPopWithTimeout() {
            // Given
            String key = "list-key";
            long timeout = 5L;
            TimeUnit unit = TimeUnit.SECONDS;
            Object expectedValue = "value3";
            when(listOperations.rightPop(key, timeout, unit)).thenReturn(expectedValue);

            // When
            Object result = redisClient.rPop(key, timeout, unit);

            // Then
            assertEquals(expectedValue, result);
            verify(listOperations).rightPop(key, timeout, unit);
        }

        @Test
        @DisplayName("03-07: 获取范围内元素")
        void testLRange() {
            // Given
            String key = "list-key";
            List<Object> expectedList = Arrays.asList("value1", "value2", "value3");
            when(listOperations.range(key, 0, -1)).thenReturn(expectedList);

            // When
            List<Object> result = redisClient.lRange(key, 0, -1);

            // Then
            assertEquals(expectedList, result);
            verify(listOperations).range(key, 0, -1);
        }

        @Test
        @DisplayName("03-08: 获取列表长度")
        void testLLen() {
            // Given
            String key = "list-key";
            when(listOperations.size(key)).thenReturn(5L);

            // When
            Long result = redisClient.lLen(key);

            // Then
            assertEquals(5L, result);
            verify(listOperations).size(key);
        }

        @Test
        @DisplayName("03-09: 按索引获取元素")
        void testLIndex() {
            // Given
            String key = "list-key";
            long index = 0;
            Object expectedValue = "value1";
            when(listOperations.index(key, index)).thenReturn(expectedValue);

            // When
            Object result = redisClient.lIndex(key, index);

            // Then
            assertEquals(expectedValue, result);
            verify(listOperations).index(key, index);
        }

        @Test
        @DisplayName("03-10: 设置指定索引元素")
        void testLSet() {
            // Given
            String key = "list-key";
            long index = 0;
            Object value = "new-value";

            // When
            redisClient.lSet(key, index, value);

            // Then
            verify(listOperations).set(key, index, value);
        }

        @Test
        @DisplayName("03-11: 移除指定数量元素")
        void testLRemove() {
            // Given
            String key = "list-key";
            long count = 2;
            Object value = "value1";
            when(listOperations.remove(key, count, value)).thenReturn(2L);

            // When
            Long result = redisClient.lRemove(key, count, value);

            // Then
            assertEquals(2L, result);
            verify(listOperations).remove(key, count, value);
        }

        @Test
        @DisplayName("03-12: 修剪列表")
        void testLTrim() {
            // Given
            String key = "list-key";
            long start = 0;
            long end = 10;

            // When
            redisClient.lTrim(key, start, end);

            // Then
            verify(listOperations).trim(key, start, end);
        }
    }

    // ==================== Set 操作测试 ====================

    @Nested
    @DisplayName("Set 操作测试")
    class SetTests {

        @Test
        @DisplayName("03-13: 添加集合成员")
        void testSAdd() {
            // Given
            String key = "set-key";
            Object[] values = {"member1", "member2"};
            when(setOperations.add(key, values)).thenReturn(2L);

            // When
            Long result = redisClient.sAdd(key, values);

            // Then
            assertEquals(2L, result);
            verify(setOperations).add(key, values);
        }

        @Test
        @DisplayName("03-14: 获取所有成员")
        void testSMembers() {
            // Given
            String key = "set-key";
            Set<Object> expectedSet = new HashSet<>(Arrays.asList("member1", "member2"));
            when(setOperations.members(key)).thenReturn(expectedSet);

            // When
            Set<Object> result = redisClient.sMembers(key);

            // Then
            assertEquals(expectedSet, result);
            verify(setOperations).members(key);
        }

        @Test
        @DisplayName("03-15: 成员存在判断-存在")
        void testSIsMember_True() {
            // Given
            String key = "set-key";
            Object value = "member1";
            when(setOperations.isMember(key, value)).thenReturn(true);

            // When
            Boolean result = redisClient.sIsMember(key, value);

            // Then
            assertTrue(result);
            verify(setOperations).isMember(key, value);
        }

        @Test
        @DisplayName("03-16: 成员存在判断-不存在")
        void testSIsMember_False() {
            // Given
            String key = "set-key";
            Object value = "non-existent-member";
            when(setOperations.isMember(key, value)).thenReturn(false);

            // When
            Boolean result = redisClient.sIsMember(key, value);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("03-17: 获取集合大小")
        void testSSize() {
            // Given
            String key = "set-key";
            when(setOperations.size(key)).thenReturn(3L);

            // When
            Long result = redisClient.sSize(key);

            // Then
            assertEquals(3L, result);
            verify(setOperations).size(key);
        }

        @Test
        @DisplayName("03-18: 移除成员")
        void testSRemove() {
            // Given
            String key = "set-key";
            Object[] values = {"member1"};
            when(setOperations.remove(key, values)).thenReturn(1L);

            // When
            Long result = redisClient.sRemove(key, values);

            // Then
            assertEquals(1L, result);
            verify(setOperations).remove(key, values);
        }

        @Test
        @DisplayName("03-19: 随机弹出成员")
        void testSPop() {
            // Given
            String key = "set-key";
            Object expectedValue = "member1";
            when(setOperations.pop(key)).thenReturn(expectedValue);

            // When
            Object result = redisClient.sPop(key);

            // Then
            assertEquals(expectedValue, result);
            verify(setOperations).pop(key);
        }

        @Test
        @DisplayName("03-20: 随机获取多个成员")
        void testSRandomMembers() {
            // Given
            String key = "set-key";
            long count = 2;
            List<Object> expectedList = Arrays.asList("member1", "member2");
            when(setOperations.randomMembers(key, count)).thenReturn(expectedList);

            // When
            List<Object> result = redisClient.sRandomMembers(key, count);

            // Then
            assertEquals(expectedList, result);
            verify(setOperations).randomMembers(key, count);
        }

        @Test
        @DisplayName("03-21: 集合并集")
        void testSUnion() {
            // Given
            String key1 = "set-key1";
            String key2 = "set-key2";
            Set<Object> expectedSet = new HashSet<>(Arrays.asList("member1", "member2", "member3"));
            when(setOperations.union(key1, Collections.singletonList(key2))).thenReturn(expectedSet);

            // When
            Set<Object> result = redisClient.sUnion(Arrays.asList(key1, key2));

            // Then
            assertEquals(expectedSet, result);
        }

        @Test
        @DisplayName("03-22: 集合交集")
        void testSIntersect() {
            // Given
            String key1 = "set-key1";
            String key2 = "set-key2";
            Set<Object> expectedSet = new HashSet<>(Arrays.asList("member1"));
            when(setOperations.intersect(key1, Collections.singletonList(key2))).thenReturn(expectedSet);

            // When
            Set<Object> result = redisClient.sIntersect(Arrays.asList(key1, key2));

            // Then
            assertEquals(expectedSet, result);
        }

        @Test
        @DisplayName("03-23: 集合差集")
        void testSDifference() {
            // Given
            String key1 = "set-key1";
            String key2 = "set-key2";
            Set<Object> expectedSet = new HashSet<>(Arrays.asList("member1"));
            when(setOperations.difference(key1, Collections.singletonList(key2))).thenReturn(expectedSet);

            // When
            Set<Object> result = redisClient.sDifference(Arrays.asList(key1, key2));

            // Then
            assertEquals(expectedSet, result);
        }
    }

    // ==================== ZSet 操作测试 ====================

    @Nested
    @DisplayName("ZSet 操作测试")
    class ZSetTests {

        @Test
        @DisplayName("03-24: 添加有序集合成员")
        void testZAdd() {
            // Given
            String key = "zset-key";
            Object value = "member1";
            double score = 1.0;
            when(zSetOperations.add(key, value, score)).thenReturn(true);

            // When
            Boolean result = redisClient.zAdd(key, value, score);

            // Then
            assertTrue(result);
            verify(zSetOperations).add(key, value, score);
        }

        @Test
        @DisplayName("03-25: 批量添加有序集合成员")
        void testZAddBatch() {
            // Given
            String key = "zset-key";
            Map<Object, Double> valueScores = new HashMap<>();
            valueScores.put("member1", 1.0);
            valueScores.put("member2", 2.0);
            when(zSetOperations.add(eq(key), any(Set.class))).thenReturn(2L);

            // When
            Long result = redisClient.zAdd(key, valueScores);

            // Then
            assertEquals(2L, result);
            verify(zSetOperations).add(eq(key), any(Set.class));
        }

        @Test
        @DisplayName("03-26: 按索引范围获取成员")
        void testZRange() {
            // Given
            String key = "zset-key";
            Set<Object> expectedSet = new LinkedHashSet<>(Arrays.asList("member1", "member2"));
            when(zSetOperations.range(key, 0, -1)).thenReturn(expectedSet);

            // When
            Set<Object> result = redisClient.zRange(key, 0, -1);

            // Then
            assertEquals(expectedSet, result);
            verify(zSetOperations).range(key, 0, -1);
        }

        @Test
        @DisplayName("03-27: 按分数范围获取成员")
        void testZRangeByScore() {
            // Given
            String key = "zset-key";
            double min = 0.0;
            double max = 10.0;
            Set<Object> expectedSet = new LinkedHashSet<>(Arrays.asList("member1", "member2"));
            when(zSetOperations.rangeByScore(key, min, max)).thenReturn(expectedSet);

            // When
            Set<Object> result = redisClient.zRangeByScore(key, min, max);

            // Then
            assertEquals(expectedSet, result);
            verify(zSetOperations).rangeByScore(key, min, max);
        }

        @Test
        @DisplayName("03-28: 带偏移量和数量限制")
        void testZRangeByScoreWithOffset() {
            // Given
            String key = "zset-key";
            double min = 0.0;
            double max = 10.0;
            long offset = 0;
            long count = 10;
            Set<Object> expectedSet = new LinkedHashSet<>(Arrays.asList("member1", "member2"));
            when(zSetOperations.rangeByScore(key, min, max, offset, count)).thenReturn(expectedSet);

            // When
            Set<Object> result = redisClient.zRangeByScore(key, min, max, offset, count);

            // Then
            assertEquals(expectedSet, result);
            verify(zSetOperations).rangeByScore(key, min, max, offset, count);
        }

        @Test
        @DisplayName("03-29: 获取成员分数")
        void testZScore() {
            // Given
            String key = "zset-key";
            Object value = "member1";
            when(zSetOperations.score(key, value)).thenReturn(5.0);

            // When
            Double result = redisClient.zScore(key, value);

            // Then
            assertEquals(5.0, result);
            verify(zSetOperations).score(key, value);
        }

        @Test
        @DisplayName("03-30: 获取成员数量")
        void testZSize() {
            // Given
            String key = "zset-key";
            when(zSetOperations.size(key)).thenReturn(10L);

            // When
            Long result = redisClient.zSize(key);

            // Then
            assertEquals(10L, result);
            verify(zSetOperations).size(key);
        }

        @Test
        @DisplayName("03-31: 按分数范围统计数量")
        void testZCount() {
            // Given
            String key = "zset-key";
            double min = 0.0;
            double max = 10.0;
            when(zSetOperations.count(key, min, max)).thenReturn(5L);

            // When
            Long result = redisClient.zCount(key, min, max);

            // Then
            assertEquals(5L, result);
            verify(zSetOperations).count(key, min, max);
        }

        @Test
        @DisplayName("03-32: 获取成员排名（升序）")
        void testZRank() {
            // Given
            String key = "zset-key";
            Object value = "member1";
            when(zSetOperations.rank(key, value)).thenReturn(0L);

            // When
            Long result = redisClient.zRank(key, value);

            // Then
            assertEquals(0L, result);
            verify(zSetOperations).rank(key, value);
        }

        @Test
        @DisplayName("03-33: 获取成员排名（降序）")
        void testZReverseRank() {
            // Given
            String key = "zset-key";
            Object value = "member1";
            when(zSetOperations.reverseRank(key, value)).thenReturn(5L);

            // When
            Long result = redisClient.zReverseRank(key, value);

            // Then
            assertEquals(5L, result);
            verify(zSetOperations).reverseRank(key, value);
        }

        @Test
        @DisplayName("03-34: 移除成员")
        void testZRemove() {
            // Given
            String key = "zset-key";
            Object[] values = {"member1"};
            when(zSetOperations.remove(key, values)).thenReturn(1L);

            // When
            Long result = redisClient.zRemove(key, values);

            // Then
            assertEquals(1L, result);
            verify(zSetOperations).remove(key, values);
        }

        @Test
        @DisplayName("03-35: 按排名范围移除")
        void testZRemoveRange() {
            // Given
            String key = "zset-key";
            long start = 0;
            long end = 5;
            when(zSetOperations.removeRange(key, start, end)).thenReturn(6L);

            // When
            Long result = redisClient.zRemoveRange(key, start, end);

            // Then
            assertEquals(6L, result);
            verify(zSetOperations).removeRange(key, start, end);
        }

        @Test
        @DisplayName("03-36: 按分数范围移除")
        void testZRemoveRangeByScore() {
            // Given
            String key = "zset-key";
            double min = 0.0;
            double max = 5.0;
            when(zSetOperations.removeRangeByScore(key, min, max)).thenReturn(3L);

            // When
            Long result = redisClient.zRemoveRangeByScore(key, min, max);

            // Then
            assertEquals(3L, result);
            verify(zSetOperations).removeRangeByScore(key, min, max);
        }

        @Test
        @DisplayName("03-37: 增加成员分数")
        void testZIncrementScore() {
            // Given
            String key = "zset-key";
            Object value = "member1";
            double delta = 5.0;
            when(zSetOperations.incrementScore(key, value, delta)).thenReturn(10.0);

            // When
            Double result = redisClient.zIncrementScore(key, value, delta);

            // Then
            assertEquals(10.0, result);
            verify(zSetOperations).incrementScore(key, value, delta);
        }
    }
}
